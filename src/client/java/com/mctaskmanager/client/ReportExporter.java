package com.mctaskmanager.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportExporter {

    public static File export(Minecraft mc,
                              Map<String, Integer> blockEntityCounts,
                              List<DiagnosisEngine.DiagItem> diagItems) {
        try {
            File gameDir = mc.gameDirectory;
            File reportsDir = new File(gameDir, "mctm-reports");
            if (!reportsDir.exists()) reportsDir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File outFile = new File(reportsDir, "mctm_report_" + timestamp + ".json");

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            root.put("modVersion", modVersion());

            // 硬件信息
            Map<String, Object> hw = new LinkedHashMap<>();
            hw.put("cpu", ManualHardwareStore.getEffectiveCpu());
            hw.put("gpu", ManualHardwareStore.getEffectiveGpu());
            hw.put("ramGB", HardwareInfoCollector.getTotalRamGB());
            hw.put("cpuManualOverride", ManualHardwareStore.isCpuOverridden());
            hw.put("gpuManualOverride", ManualHardwareStore.isGpuOverridden());
            root.put("hardware", hw);

            // 游戏环境
            Map<String, Object> env = new LinkedHashMap<>();
            env.put("loader", FabricLoader.getInstance().getModContainer("fabricloader")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));
            env.put("resolution", mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight());
            env.put("renderDistance", mc.options.renderDistance().get());
            env.put("vsync", mc.options.enableVsync().get());
            env.put("fpsLimit", mc.options.framerateLimit().get());
            env.put("resourcePacks", new ArrayList<>(mc.options.resourcePacks));
            env.put("activeShaderPack", readActiveShaderPack());
            root.put("environment", env);

            // 模组列表
            List<Map<String, Object>> mods = new ArrayList<>();
            for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", mod.getMetadata().getId());
                m.put("name", mod.getMetadata().getName());
                m.put("version", mod.getMetadata().getVersion().getFriendlyString());
                try {
                    var paths = mod.getOrigin().getPaths();
                    if (!paths.isEmpty()) {
                        Path p = paths.get(0);
                        if (Files.exists(p)) m.put("sizeBytes", Files.size(p));
                    }
                } catch (Exception ignored) {}
                mods.add(m);
            }
            root.put("mods", mods);

            // 帧率数据
            FpsMonitor fm = MinecraftTaskManagerClient.getFpsMonitor();
            Map<String, Object> perf = new LinkedHashMap<>();
            perf.put("current", mc.getFps());
            perf.put("average", fm.getAverageFps());
            perf.put("min", fm.getMinFps());
            perf.put("low1Percent", fm.get1PercentLow());
            perf.put("low01Percent", fm.get01PercentLow());
            perf.put("averageFrameTimeMs", fm.getAverageFrameTime());
            perf.put("averageMemoryMB", fm.getAverageMemoryMB());
            perf.put("gcTotalCount", fm.getTotalGcCount());
            perf.put("gcTotalTimeMs", fm.getTotalGcTime());
            perf.put("gcTimeLast10sMs", fm.getGcTimeLast10s());
            root.put("performance", perf);

            // 方块实体排行
            List<Map<String, Object>> beList = new ArrayList<>();
            blockEntityCounts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .forEach(e -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("type", e.getKey());
                        item.put("count", e.getValue());
                        beList.add(item);
                    });
            root.put("blockEntities", beList);

            // 卡顿快照
            List<Map<String, Object>> snapList = new ArrayList<>();
            for (FpsMonitor.LagSnapshot snap : fm.getSnapshots()) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("time", snap.timeText());
                s.put("dimension", snap.dimShort());
                s.put("x", snap.x);
                s.put("y", snap.y);
                s.put("z", snap.z);
                s.put("fps", snap.fps);
                s.put("entityCount", snap.entityCount);
                snapList.add(s);
            }
            root.put("lagSnapshots", snapList);

            // 诊断建议
            List<Map<String, Object>> diagList = new ArrayList<>();
            for (DiagnosisEngine.DiagItem item : diagItems) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("level", item.level);
                d.put("text", item.text);
                if (item.detail != null) d.put("detail", item.detail);
                diagList.add(d);
            }
            root.put("diagnosis", diagList);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter w = new FileWriter(outFile)) {
                gson.toJson(root, w);
            }
            return outFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String modVersion() {
        return FabricLoader.getInstance().getModContainer("tkmanager")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static String readActiveShaderPack() {
        try {
            File f = new File(Minecraft.getInstance().gameDirectory, "config/iris.properties");
            if (!f.exists()) return null;
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("shaderPack=")) {
                        String v = line.substring("shaderPack=".length()).trim();
                        if (v.isEmpty() || v.equalsIgnoreCase("OFF")) return null;
                        return v;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}