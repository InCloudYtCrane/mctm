package com.mctaskmanager.client;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class FpsMonitor {

    private static final int SAMPLE_SIZE = 100;
    private static final int GRAPH_POINTS = 120;
    private static final int MAX_SNAPSHOTS = 8;
    private static final long SNAPSHOT_COOLDOWN_MS = 15_000L;

    // FPS 原始样本
    private final Deque<Double> fpsSamples = new ArrayDeque<>();
    private final Deque<Double> frameTimeSamples = new ArrayDeque<>();
    private final Deque<Double> memorySamples = new ArrayDeque<>();

    // 图表历史
    private final List<Double> fpsGraphHistory = new LinkedList<>();
    private final List<Double> frameTimeGraphHistory = new LinkedList<>();
    private final List<Double> memoryGraphHistory = new LinkedList<>();
    private final List<Double> gcTimeGraphHistory = new LinkedList<>();

    // 卡顿快照
    private final List<LagSnapshot> snapshots = new ArrayList<>();

    private long lastToastTime = 0;
    private long lastSnapshotTime = 0;

    // GC 统计
    private long lastGcCount = 0;
    private long lastGcTime = 0;
    private long totalGcCount = 0;
    private long totalGcTime = 0;
    private long lastGcCheckTime = System.currentTimeMillis();

    private long gcTimeWindow = 0;
    private long gcCheckWindowStart = System.currentTimeMillis();

    public void tick(int currentFps, long usedMemoryMB) {
        double frameTime = currentFps > 0 ? 1000.0 / currentFps : 1000.0;

        addToDeque(fpsSamples, currentFps);
        addToDeque(frameTimeSamples, frameTime);
        addToDeque(memorySamples, usedMemoryMB);

        addToGraph(fpsGraphHistory, currentFps);
        addToGraph(frameTimeGraphHistory, frameTime);
        addToGraph(memoryGraphHistory, usedMemoryMB);

        updateGcStats();
    }

    private void updateGcStats() {
        long now = System.currentTimeMillis();
        if (now - lastGcCheckTime < 1000) return;
        lastGcCheckTime = now;

        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long c = bean.getCollectionCount();
            long t = bean.getCollectionTime();
            if (c > 0) gcCount += c;
            if (t > 0) gcTime += t;
        }

        long timeDelta = gcTime - lastGcTime;
        lastGcCount = gcCount;
        lastGcTime = gcTime;
        totalGcCount = gcCount;
        totalGcTime = gcTime;

        if (now - gcCheckWindowStart > 10_000) {
            gcTimeWindow = 0;
            gcCheckWindowStart = now;
        }
        gcTimeWindow += timeDelta;

        addToGraph(gcTimeGraphHistory, timeDelta);
    }

    private void addToDeque(Deque<Double> deque, double v) {
        deque.addLast(v);
        while (deque.size() > SAMPLE_SIZE) deque.removeFirst();
    }

    private void addToGraph(List<Double> list, double v) {
        list.add(v);
        while (list.size() > GRAPH_POINTS) list.remove(0);
    }

    // ---------- 平均值 ----------

    public int getAverageFps() {
        if (fpsSamples.isEmpty()) return 0;
        double sum = 0;
        for (double d : fpsSamples) sum += d;
        return (int) (sum / fpsSamples.size());
    }

    public int getMinFps() {
        if (fpsSamples.isEmpty()) return 0;
        int min = Integer.MAX_VALUE;
        for (double d : fpsSamples) if (d < min) min = (int) d;
        return min;
    }

    public double getAverageFrameTime() {
        if (frameTimeSamples.isEmpty()) return 0;
        double sum = 0;
        for (double d : frameTimeSamples) sum += d;
        return sum / frameTimeSamples.size();
    }

    public long getAverageMemoryMB() {
        if (memorySamples.isEmpty()) return 0;
        double sum = 0;
        for (double d : memorySamples) sum += d;
        return (long) (sum / memorySamples.size());
    }

    // ---------- 1% Low / 0.1% Low ----------

    public int get1PercentLow() {
        return computeLowFps(0.01);
    }

    public int get01PercentLow() {
        return computeLowFps(0.001);
    }

    private int computeLowFps(double ratio) {
        if (fpsSamples.size() < 10) return 0;
        List<Double> sorted = new LinkedList<>(fpsSamples);
        sorted.sort(Double::compareTo);
        int n = Math.max(1, (int) (sorted.size() * ratio));
        double sum = 0;
        for (int i = 0; i < n; i++) sum += sorted.get(i);
        return (int) (sum / n);
    }

    // ---------- GC ----------

    public long getGcCountLast10s() {
        if (gcTimeGraphHistory.size() < 10) return totalGcCount;
        long sum = 0;
        int n = Math.min(10, gcTimeGraphHistory.size());
        for (int i = gcTimeGraphHistory.size() - n; i < gcTimeGraphHistory.size(); i++) {
            if (gcTimeGraphHistory.get(i) > 0) sum++;
        }
        return sum;
    }

    public long getGcTimeLast10s() {
        long sum = 0;
        int n = Math.min(10, gcTimeGraphHistory.size());
        for (int i = gcTimeGraphHistory.size() - n; i < gcTimeGraphHistory.size(); i++) {
            sum += gcTimeGraphHistory.get(i).longValue();
        }
        return sum;
    }

    public long getTotalGcCount() { return totalGcCount; }
    public long getTotalGcTime() { return totalGcTime; }
    public List<Double> getGcTimeGraphHistory() { return gcTimeGraphHistory; }

    // ---------- 状态（读配置） ----------

    public boolean hasEnoughSamples() {
        return fpsSamples.size() >= 40;
    }

    public boolean isLowFps() {
        int threshold = MctmConfig.get().lowFpsThreshold;
        return hasEnoughSamples() && getAverageFps() < threshold;
    }

    public boolean canShowToast() {
        long cooldown = MctmConfig.get().toastCooldownSec * 1000L;
        long now = System.currentTimeMillis();
        if (now - lastToastTime < cooldown) return false;
        lastToastTime = now;
        return true;
    }

    public int getThreshold() {
        return MctmConfig.get().lowFpsThreshold;
    }

    // ---------- 图表数据 ----------

    public List<Double> getFpsGraphHistory() { return fpsGraphHistory; }
    public List<Double> getFrameTimeGraphHistory() { return frameTimeGraphHistory; }
    public List<Double> getMemoryGraphHistory() { return memoryGraphHistory; }

    // ---------- 卡顿快照 ----------

    public boolean canRecordSnapshot() {
        long now = System.currentTimeMillis();
        if (now - lastSnapshotTime < SNAPSHOT_COOLDOWN_MS) return false;
        lastSnapshotTime = now;
        return true;
    }

    public void recordSnapshot(int x, int y, int z, int entityCount, String dimension) {
        snapshots.add(new LagSnapshot(
                System.currentTimeMillis(),
                x, y, z,
                getAverageFps(),
                entityCount,
                dimension
        ));
        while (snapshots.size() > MAX_SNAPSHOTS) {
            snapshots.remove(0);
        }
    }

    public List<LagSnapshot> getSnapshots() { return snapshots; }

    // ---------- 重置（进入新世界时调用） ----------

    public void reset() {
        fpsSamples.clear();
        frameTimeSamples.clear();
        memorySamples.clear();
        fpsGraphHistory.clear();
        frameTimeGraphHistory.clear();
        memoryGraphHistory.clear();
        gcTimeGraphHistory.clear();
        snapshots.clear();
    }

    public static class LagSnapshot {
        public final long timestamp;
        public final int x, y, z;
        public final int fps;
        public final int entityCount;
        public final String dimension;

        public LagSnapshot(long timestamp, int x, int y, int z, int fps,
                           int entityCount, String dimension) {
            this.timestamp = timestamp;
            this.x = x;
            this.y = y;
            this.z = z;
            this.fps = fps;
            this.entityCount = entityCount;
            this.dimension = dimension;
        }

        public String timeText() {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("HH:mm:ss");
            return fmt.format(new java.util.Date(timestamp));
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