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
                    Lang.get("mctm.diag.renderdist.text", renderDist, avgFps),
                    Lang.get("mctm.diag.renderdist.detail"),
                    new Action() {
                        public String label() { return Lang.get("mctm.diag.renderdist.action"); }
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
                        Lang.get("mctm.diag.vsync.locked.text", refreshRate),
                        Lang.get("mctm.diag.vsync.locked.detail"),
                        new Action() {
                            public String label() { return Lang.get("mctm.diag.vsync.locked.action"); }
                            public void run() { mc.options.enableVsync().set(false); }
                        }));
            } else if (avgFps < refreshRate) {
                items.add(new DiagItem("warn",
                        Lang.get("mctm.diag.vsync.slow.title"),
                        Lang.get("mctm.diag.vsync.slow.detail")));
            }
        }

        var loader = FabricLoader.getInstance();
        boolean hasSodium = loader.isModLoaded("sodium") || loader.isModLoaded("embeddium");
        boolean hasLithium = loader.isModLoaded("lithium");

        if (!hasSodium) {
            items.add(new DiagItem("warn",
                    Lang.get("mctm.diag.sodium.missing.title"),
                    Lang.get("mctm.diag.sodium.missing.detail")));
        } else {
            items.add(new DiagItem("good", Lang.get("mctm.diag.sodium.ok"), null));
        }

        if (!hasLithium) {
            items.add(new DiagItem("info",
                    Lang.get("mctm.diag.lithium.missing.title"),
                    Lang.get("mctm.diag.lithium.missing.detail")));
        }

        if (avgFrameTime > 33 && avgFps > 0) {
            items.add(new DiagItem("warn",
                    Lang.get("mctm.diag.frametime.title", String.format("%.1f", avgFrameTime)),
                    Lang.get("mctm.diag.frametime.detail")));
        }

        if (avgFps > 0 && low1 > 0 && avgFps - low1 > 20) {
            items.add(new DiagItem("warn",
                    Lang.get("mctm.diag.low1.title", low1, avgFps),
                    Lang.get("mctm.diag.low1.detail")));
        }

        for (var entry : blockEntityCounts.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();

            if (type.equals("hopper") && count > 100) {
                items.add(new DiagItem("warn",
                        Lang.get("mctm.diag.hopper.title", count),
                        Lang.get("mctm.diag.hopper.detail")));
            } else if (type.equals("chest") && count > 500) {
                items.add(new DiagItem("info",
                        Lang.get("mctm.diag.chest.title", count),
                        Lang.get("mctm.diag.chest.detail")));
            } else if (type.equals("furnace") && count > 100) {
                items.add(new DiagItem("info",
                        Lang.get("mctm.diag.furnace.title", count),
                        Lang.get("mctm.diag.furnace.detail")));
            }
        }

        if (mc.level != null) {
            int entityCount = mc.level.getEntityCount();
            if (entityCount > 500) {
                items.add(new DiagItem("warn",
                        Lang.get("mctm.diag.entities.title", entityCount),
                        Lang.get("mctm.diag.entities.detail")));
            }
        }

        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long max  = Runtime.getRuntime().maxMemory();
        double memRatio = (double) used / max;
        if (memRatio > 0.85) {
            items.add(new DiagItem("warn",
                    Lang.get("mctm.diag.memory.title", String.format("%.0f", memRatio * 100)),
                    Lang.get("mctm.diag.memory.detail")));
        }

        long gcTime10s = monitor.getGcTimeLast10s();
        if (gcTime10s > 2000) {
            items.add(new DiagItem("warn",
                    Lang.get("mctm.diag.gc.title", gcTime10s),
                    Lang.get("mctm.diag.gc.detail")));
        }

        if (isShaderActive() && avgFps > 0 && avgFps < 60) {
            items.add(new DiagItem("info",
                    Lang.get("mctm.diag.shader.title"),
                    Lang.get("mctm.diag.shader.detail")));
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