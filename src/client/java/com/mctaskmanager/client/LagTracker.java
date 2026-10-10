package com.mctaskmanager.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LagTracker {

    private static final int MAX_RECORDS = 20;
    private static final int TOP_N = 3;

    // 当前卡顿状态
    private long lagStartTime = 0;
    private boolean inLag = false;
    private int minFpsDuringLag = Integer.MAX_VALUE;
    private long fpsSumDuringLag = 0;
    private int fpsSampleCount = 0;
    private long gcTimeAtLagStart = 0;
    private int chunkCountAtLagStart = 0;

    // 记录列表
    private final List<LagRecord> records = new ArrayList<>();

    // 新记录待通知
    private boolean pendingNotification = false;

    // 未读标记
    private boolean hasUnread = false;

    // 通知冷却
    private long lastNotificationTime = 0;
    private static final long NOTIFICATION_COOLDOWN_MS = 10_000L;

    public List<LagRecord> getRecords() {
        return records;
    }

    public boolean hasUnread() {
        return hasUnread;
    }

    public void markRead() {
        hasUnread = false;
    }

    public boolean consumePendingNotification() {
        if (pendingNotification) {
            pendingNotification = false;
            long now = System.currentTimeMillis();
            if (now - lastNotificationTime < NOTIFICATION_COOLDOWN_MS) {
                return false;
            }
            lastNotificationTime = now;
            return true;
        }
        return false;
    }

    public LagRecord getLatestRecord() {
        if (records.isEmpty()) return null;
        return records.get(records.size() - 1);
    }

    public void tick(int currentFps) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            inLag = false;
            return;
        }

        MctmConfig cfg = MctmConfig.get();
        long now = System.currentTimeMillis();

        if (currentFps < cfg.lagDetectThreshold) {
            if (!inLag) {
                inLag = true;
                lagStartTime = now;
                minFpsDuringLag = currentFps;
                fpsSumDuringLag = 0;
                fpsSampleCount = 0;
                gcTimeAtLagStart = getTotalGcTime();
                chunkCountAtLagStart = getChunkCount(mc);
            } else {
                if (currentFps < minFpsDuringLag) minFpsDuringLag = currentFps;
                fpsSumDuringLag += currentFps;
                fpsSampleCount++;
            }
        } else {
            if (inLag) {
                long duration = now - lagStartTime;
                if (duration >= cfg.lagMinDurationMs) {
                    recordLag(duration, mc);
                }
                inLag = false;
            }
        }
    }

    private void recordLag(long durationMs, Minecraft mc) {
        BlockPos pos = mc.player.blockPosition();
        int entityCount = mc.level.getEntityCount();
        int chunkCount = getChunkCount(mc);

        long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long maxMem = Runtime.getRuntime().maxMemory();
        long gcTime = getTotalGcTime() - gcTimeAtLagStart;

        // 实体类型分布
        Map<String, Integer> entityTypes = new LinkedHashMap<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            String name = e.getType().getDescription().getString();
            entityTypes.merge(name, 1, Integer::sum);
        }

        // 方块实体类型
        Map<String, Integer> beTypes = DataCache.getBlockEntityCounts();

        LagRecord rec = new LagRecord();
        rec.timestamp = lagStartTime;
        rec.durationMs = durationMs;
        rec.minFps = minFpsDuringLag;
        rec.avgFps = fpsSampleCount > 0 ? (int) (fpsSumDuringLag / fpsSampleCount) : minFpsDuringLag;
        rec.x = pos.getX();
        rec.y = pos.getY();
        rec.z = pos.getZ();
        rec.dimension = mc.level.dimension().location().toString();
        rec.entityCount = entityCount;
        rec.chunkCount = chunkCount;
        rec.chunkCountAtStart = chunkCountAtLagStart;
        rec.memoryUsedMB = usedMem / 1024 / 1024;
        rec.memoryMaxMB = maxMem / 1024 / 1024;
        rec.gcTimeDuringLag = gcTime;

        List<Map.Entry<String, Integer>> entitySorted = new ArrayList<>(entityTypes.entrySet());
        entitySorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < Math.min(TOP_N, entitySorted.size()); i++) {
            Map.Entry<String, Integer> e = entitySorted.get(i);
            rec.topEntities.add(e.getKey() + " × " + e.getValue());
        }

        List<Map.Entry<String, Integer>> beSorted = new ArrayList<>(beTypes.entrySet());
        beSorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < Math.min(TOP_N, beSorted.size()); i++) {
            Map.Entry<String, Integer> e = beSorted.get(i);
            rec.topBlockEntities.add(e.getKey() + " × " + e.getValue());
        }

        analyze(rec);

        records.add(rec);
        while (records.size() > MAX_RECORDS) records.remove(0);

        hasUnread = true;
        pendingNotification = true;
    }

    private void analyze(LagRecord rec) {
        if (rec.gcTimeDuringLag > rec.durationMs * 0.3 && rec.gcTimeDuringLag > 100) {
            rec.reason = "GC 停顿";
            rec.reasonDetail = String.format("卡顿期间 GC 总计 %d ms，占 %.0f%%",
                    rec.gcTimeDuringLag, rec.gcTimeDuringLag * 100.0 / rec.durationMs);
            return;
        }

        if (rec.entityCount > 200) {
            rec.reason = "实体过多";
            String top = rec.topEntities.isEmpty() ? "" : rec.topEntities.get(0);
            rec.reasonDetail = "附近有 " + rec.entityCount + " 个实体，最多的是 " + top;
            return;
        }

        int totalBe = 0;
        for (int v : DataCache.getBlockEntityCounts().values()) totalBe += v;
        if (totalBe > 200) {
            rec.reason = "方块实体过多";
            String top = rec.topBlockEntities.isEmpty() ? "" : rec.topBlockEntities.get(0);
            rec.reasonDetail = "附近有 " + totalBe + " 个方块实体，最多的是 " + top;
            return;
        }

        int chunkDelta = rec.chunkCount - rec.chunkCountAtStart;
        if (chunkDelta > 30) {
            rec.reason = "区块加载";
            rec.reasonDetail = "卡顿期间新增加载了 " + chunkDelta + " 个区块";
            return;
        }

        double memRatio = (double) rec.memoryUsedMB / rec.memoryMaxMB;
        if (memRatio > 0.85) {
            rec.reason = "内存压力";
            rec.reasonDetail = String.format("内存使用率 %.0f%%，建议调大 -Xmx", memRatio * 100);
            return;
        }

        rec.reason = "未知原因";
        rec.reasonDetail = "可能是某个模组的临时负载，或客户端瞬时卡顿";
    }

    private long getTotalGcTime() {
        long t = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long v = bean.getCollectionTime();
            if (v > 0) t += v;
        }
        return t;
    }

    private int getChunkCount(Minecraft mc) {
        try {
            return mc.level.getChunkSource().getLoadedChunksCount();
        } catch (Exception e) {
            return 0;
        }
    }

    public void clearAll() {
        records.clear();
        hasUnread = false;
    }

    public static class LagRecord {
        public long timestamp;
        public long durationMs;
        public int minFps;
        public int avgFps;
        public int x, y, z;
        public String dimension;
        public int entityCount;
        public int chunkCount;
        public int chunkCountAtStart;
        public long memoryUsedMB;
        public long memoryMaxMB;
        public long gcTimeDuringLag;
        public List<String> topEntities = new ArrayList<>();
        public List<String> topBlockEntities = new ArrayList<>();
        public String reason = "";
        public String reasonDetail = "";

        public String timeText() {
            SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
            return fmt.format(new Date(timestamp));
        }

        public String durationText() {
            return String.format("%.1f 秒", durationMs / 1000.0);
        }

        public String coordText() {
            return "(" + x + ", " + y + ", " + z + ")";
        }

        public String dimShort() {
            if (dimension == null) return "?";
            int idx = dimension.indexOf(':');
            return idx >= 0 ? dimension.substring(idx + 1) : dimension;
        }
    }
}