package com.mctaskmanager.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SessionManager {

    private static final int MAX_SESSIONS = 20;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static List<SessionRecord> sessions = null;

    // 当前会话状态
    private static long sessionStartTime = 0;
    private static String worldName = "";
    private static boolean active = false;
    private static int lagCountStart = 0;

    private static int peakMemoryMB = 0;
    private static int peakChunks = 0;
    private static int peakEntities = 0;

    public static boolean isActive() { return active; }

    // ===== 会话生命周期 =====

    public static void startSession(Minecraft mc) {
        if (active) return;
        active = true;
        sessionStartTime = System.currentTimeMillis();
        peakMemoryMB = 0;
        peakChunks = 0;
        peakEntities = 0;

        // 重置 FPS 采样，让统计数据只包含本次会话
        MinecraftTaskManagerClient.getFpsMonitor().reset();

        // 记录起始的卡顿次数
        lagCountStart = MinecraftTaskManagerClient.getLagTracker().getRecords().size();

        // 世界名
        try {
            if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
                String name = mc.getSingleplayerServer().getWorldData().getLevelName();
                worldName = "单人 · " + name;
            } else if (mc.getCurrentServer() != null) {
                worldName = "多人 · " + mc.getCurrentServer().ip;
            } else {
                worldName = "未知世界";
            }
        } catch (Exception e) {
            worldName = "未知世界";
        }
    }

    public static void tick(Minecraft mc) {
        if (!active || mc.level == null) return;

        // 内存峰值
        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        int usedMB = (int) (used / 1024 / 1024);
        if (usedMB > peakMemoryMB) peakMemoryMB = usedMB;

        // 区块峰值
        try {
            int chunks = mc.level.getChunkSource().getLoadedChunksCount();
            if (chunks > peakChunks) peakChunks = chunks;
        } catch (Exception ignored) {}

        // 实体峰值
        int entities = mc.level.getEntityCount();
        if (entities > peakEntities) peakEntities = entities;
    }

    public static SessionRecord endSession() {
        if (!active) return null;
        active = false;

        long endTime = System.currentTimeMillis();

        FpsMonitor fps = MinecraftTaskManagerClient.getFpsMonitor();
        LagTracker lag = MinecraftTaskManagerClient.getLagTracker();

        SessionRecord rec = new SessionRecord();
        rec.startTime = sessionStartTime;
        rec.endTime = endTime;
        rec.durationMs = endTime - sessionStartTime;
        rec.worldName = worldName;
        rec.avgFps = fps.getAverageFps();
        rec.minFps = fps.getMinFps();
        rec.low1Percent = fps.get1PercentLow();
        rec.peakMemoryMB = peakMemoryMB;
        rec.peakChunks = peakChunks;
        rec.peakEntities = peakEntities;

        // 卡顿统计（只统计本次会话期间新增的）
        List<LagTracker.LagRecord> allLags = lag.getRecords();
        int totalLags = allLags.size();
        int sessionLags = Math.max(0, totalLags - lagCountStart);
        rec.lagCount = sessionLags;

        long lagTotalMs = 0;
        for (int i = Math.max(0, totalLags - sessionLags); i < totalLags; i++) {
            lagTotalMs += allLags.get(i).durationMs;
        }
        rec.totalLagMs = lagTotalMs;

        load();
        sessions.add(rec);
        while (sessions.size() > MAX_SESSIONS) sessions.remove(0);
        save();

        return rec;
    }

    // ===== 存储 =====

    private static File getFile() {
        File cfg = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!cfg.exists()) cfg.mkdirs();
        return new File(cfg, "mctm-sessions.json");
    }

    private static void load() {
        if (sessions != null) return;
        File f = getFile();
        sessions = new ArrayList<>();
        if (f.exists()) {
            try {
                String json = Files.readString(f.toPath());
                SessionRecord[] arr = GSON.fromJson(json, SessionRecord[].class);
                if (arr != null) {
                    for (SessionRecord r : arr) sessions.add(r);
                }
            } catch (Exception ignored) {}
        }
    }

    private static void save() {
        try {
            Files.writeString(getFile().toPath(), GSON.toJson(sessions));
        } catch (Exception ignored) {}
    }

    public static List<SessionRecord> getSessions() {
        load();
        return sessions;
    }

    // 删除单条记录
    public static void removeSession(int index) {
        load();
        if (index >= 0 && index < sessions.size()) {
            sessions.remove(index);
            save();
        }
    }

    // 删除全部
    public static void clearAll() {
        sessions = new ArrayList<>();
        save();
    }

    // ===== 数据类 =====

    public static class SessionRecord {
        public long startTime;
        public long endTime;
        public long durationMs;
        public String worldName;
        public int avgFps;
        public int minFps;
        public int low1Percent;
        public int peakMemoryMB;
        public int peakChunks;
        public int peakEntities;
        public int lagCount;
        public long totalLagMs;

        public String dateText() {
            SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm");
            return fmt.format(new Date(startTime));
        }

        public String durationText() {
            long sec = durationMs / 1000;
            long h = sec / 3600;
            long m = (sec % 3600) / 60;
            long s = sec % 60;
            if (h > 0) return String.format("%d时%d分", h, m);
            if (m > 0) return String.format("%d分%d秒", m, s);
            return s + "秒";
        }

        public String lagText() {
            if (lagCount == 0) return "无";
            return lagCount + " 次 · " + String.format("%.1f", totalLagMs / 1000.0) + "s";
        }
    }
}