package com.mctaskmanager.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiagnosisEngine {

    public static class DiagItem {
        public final String level;   // warn / info / good
        public final String text;
        public final String detail;
        public final Action action;

        public DiagItem(String level, String text, String detail, Action action) {
            this.level = level;
            this.text = text;
            this.detail = detail;
            this.action = action;
        }

        public DiagItem(String level, String text, String detail) {
            this(level, text, detail, null);
        }
    }

    public interface Action {
        String label();
        void run();
    }

    public static List<DiagItem> diagnose(Minecraft mc,
                                          Map<String, Integer> blockEntityCounts) {
        List<DiagItem> items = new ArrayList<>();
        FpsMonitor monitor = MinecraftTaskManagerClient.getFpsMonitor();

        int avgFps = monitor.getAverageFps();
        double avgFrameTime = monitor.getAverageFrameTime();
        int low1 = monitor.get1PercentLow();
        int renderDist = mc.options.renderDistance().get();
        boolean vsync = mc.options.enableVsync().get();

        if (renderDist >= 20 && avgFps > 0 && avgFps < 60) {
            items.add(new DiagItem("warn",
                    String.format("视距 %d 偏高，平均帧率只有 %d", renderDist, avgFps),
                    "建议降到 12-16，帧率通常能翻倍",
                    new Action() {
                        public String label() { return "降到 12"; }
                        public void run() { mc.options.renderDistance().set(12); }
                    }));
        }

        if (vsync) {
            long refreshRate = 60;
            try {
                refreshRate = mc.getWindow().getRefreshRate();
                if (refreshRate <= 0) refreshRate = 60;
            } catch (Exception ignored) {}

            if (avgFps >= refreshRate - 3) {
                items.add(new DiagItem("info",
                        String.format("垂直同步锁定在 %d FPS", refreshRate),
                        "真实性能可能远高于这个数字，关掉可看真实帧率",
                        new Action() {
                            public String label() { return "关闭"; }
                            public void run() { mc.options.enableVsync().set(false); }
                        }));
            } else if (avgFps < refreshRate) {
                items.add(new DiagItem("warn",
                        "垂直同步开启，但帧率达不到刷新率",
                        "说明硬件确实跑不到刷新率，不只是被锁帧"));
            }
        }

        var loader = FabricLoader.getInstance();
        boolean hasSodium = loader.isModLoaded("sodium") || loader.isModLoaded("embeddium");
        boolean hasLithium = loader.isModLoaded("lithium");

        if (!hasSodium) {
            items.add(new DiagItem("warn",
                    "未检测到 Sodium / Embeddium",
                    "这两个渲染优化模组通常能提升 30%+ 的帧率，强烈建议安装"));
        } else {
            items.add(new DiagItem("good", "已安装渲染优化模组 Sodium", null));
        }

        if (!hasLithium) {
            items.add(new DiagItem("info",
                    "未检测到 Lithium",
                    "Lithium 优化服务端逻辑，单人游戏也能受益"));
        }

        if (avgFrameTime > 33 && avgFps > 0) {
            items.add(new DiagItem("warn",
                    String.format("平均帧时间 %.1f ms，超过 33ms", avgFrameTime),
                    "每帧处理时间偏长，尝试降低视距或关闭光影"));
        }

        if (avgFps > 0 && low1 > 0 && avgFps - low1 > 20) {
            items.add(new DiagItem("warn",
                    String.format("1%% Low 只有 %d，与平均值 %d 差距较大", low1, avgFps),
                    "存在间歇性卡顿，可能是 GC 或区块加载导致"));
        }

        for (var entry : blockEntityCounts.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();

            if (type.equals("hopper") && count > 100) {
                items.add(new DiagItem("warn",
                        String.format("检测到 %d 个漏斗", count),
                        "漏斗是性能杀手，考虑用漏斗矿车或合并"));
            } else if (type.equals("chest") && count > 500) {
                items.add(new DiagItem("info",
                        String.format("检测到 %d 个箱子", count),
                        "大量箱子占用内存，考虑用存储模组"));
            } else if (type.equals("furnace") && count > 100) {
                items.add(new DiagItem("info",
                        String.format("检测到 %d 个熔炉", count),
                        "熔炉持续 tick，考虑用烟熏炉"));
            }
        }

        if (mc.level != null) {
            int entityCount = mc.level.getEntityCount();
            if (entityCount > 500) {
                items.add(new DiagItem("warn",
                        String.format("附近有 %d 个实体", entityCount),
                        "实体多会拖慢 CPU，检查刷怪塔、掉落物"));
            }
        }

        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long max  = Runtime.getRuntime().maxMemory();
        double memRatio = (double) used / max;
        if (memRatio > 0.85) {
            items.add(new DiagItem("warn",
                    String.format("堆内存使用率 %.0f%%", memRatio * 100),
                    "内存压力大，考虑调大 -Xmx 或减少模组"));
        }

        long gcTime10s = monitor.getGcTimeLast10s();
        if (gcTime10s > 2000) {
            items.add(new DiagItem("warn",
                    String.format("最近 10 秒 GC 耗时 %d ms", gcTime10s),
                    "GC 太频繁，通常是内存压力大，建议调大 -Xmx"));
        }

        if (isShaderActive() && avgFps > 0 && avgFps < 60) {
            items.add(new DiagItem("info",
                    "检测到光影已开启，帧率偏低",
                    "关掉光影或换成轻量光影能明显提升帧率"));
        }

        return items;
    }

    private static boolean isShaderActive() {
        try {
            File f = new File(Minecraft.getInstance().gameDirectory, "config/iris.properties");
            if (!f.exists()) return false;
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("shaderPack=")) {
                        String v = line.substring("shaderPack=".length()).trim();
                        return !v.isEmpty()
                                && !v.equalsIgnoreCase("OFF")
                                && !v.equalsIgnoreCase("(internal)")
                                && !v.equalsIgnoreCase("internal");
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}