package com.mctaskmanager.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class HardwareInfoCollector {

    private static String cachedCpu = null;
    private static String cachedGpu = null;
    private static long cachedRamGB = -1;

    // 加载状态
    private static volatile boolean loading = false;
    private static volatile boolean loaded = false;

    public static boolean isLoading() { return loading && !loaded; }
    public static boolean isLoaded() { return loaded; }

    // 主入口启动时调用一次，后台线程默默读
    public static void preloadAsync() {
        if (loading || loaded) return;
        loading = true;
        Thread t = new Thread(() -> {
            try {
                // 依次调用，结果会进缓存
                getCpuNameInternal();
                getGpuNameInternal();
                getTotalRamGBInternal();
            } finally {
                loaded = true;
                loading = false;
            }
        }, "MCTM-Hardware-Preload");
        t.setDaemon(true);
        t.start();
    }

    // ---- 对外接口：返回缓存，没加载完就返回"读取中..." ----

    public static String getCpuName() {
        if (cachedCpu == null) {
            if (isLoading()) return Lang.get("mctm.hardware.reading");
            // 没预加载就同步跑（兼容旧调用）
            getCpuNameInternal();
        }
        return cachedCpu;
    }

    public static String getGpuName() {
        if (cachedGpu == null) {
            if (isLoading()) return Lang.get("mctm.hardware.reading");
            getGpuNameInternal();
        }
        return cachedGpu;
    }

    public static long getTotalRamGB() {
        if (cachedRamGB < 0) {
            if (isLoading()) return -1;
            getTotalRamGBInternal();
        }
        return cachedRamGB;
    }

    // ---- 内部实际读取，不走 loading 检查 ----

    private static void getCpuNameInternal() {
        if (cachedCpu != null) return;
        String os = System.getProperty("os.name").toLowerCase();
        String result;
        if (os.contains("win")) {
            result = run("wmic cpu get name");
            if (isEmptyOrFail(result)) {
                result = run("powershell -Command \"(Get-CimInstance Win32_Processor).Name\"");
            }
        } else if (os.contains("mac")) {
            result = run("sysctl -n machdep.cpu.brand_string");
        } else {
            result = readLinuxCpu();
        }
        cachedCpu = isEmptyOrFail(result) ? Lang.get("mctm.hardware.unknown.cpu") : result;
    }

    private static void getGpuNameInternal() {
        if (cachedGpu != null) return;
        String os = System.getProperty("os.name").toLowerCase();
        String raw;
        if (os.contains("win")) {
            raw = run("wmic path win32_VideoController get name");
            if (isEmptyOrFail(raw)) {
                raw = run("powershell -Command \"(Get-CimInstance Win32_VideoController).Name\"");
            }
        } else if (os.contains("mac")) {
            raw = run("system_profiler SPDisplaysDataType");
        } else {
            raw = run("lspci");
        }
        String picked = pickMainGpu(raw);
        cachedGpu = isEmptyOrFail(picked) ? Lang.get("mctm.hardware.unknown.gpu") : picked;
    }

    private static void getTotalRamGBInternal() {
        if (cachedRamGB >= 0) return;
        try {
            com.sun.management.OperatingSystemMXBean bean =
                    (com.sun.management.OperatingSystemMXBean)
                            java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            cachedRamGB = bean.getTotalMemorySize() / 1024 / 1024 / 1024;
        } catch (Exception e) {
            cachedRamGB = 0;
        }
    }

    // ---- 工具方法 ----

    private static String readLinuxCpu() {
        try (BufferedReader r = new BufferedReader(
                new java.io.FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("model name")) {
                    int idx = line.indexOf(':');
                    if (idx >= 0) return line.substring(idx + 1).trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String pickMainGpu(String rawAll) {
        if (rawAll == null || rawAll.isEmpty()) return null;

        if (rawAll.contains("Chipset Model")) {
            String[] lines = rawAll.split("\n");
            for (String l : lines) {
                if (l.contains("Chipset Model")) {
                    int idx = l.indexOf(':');
                    if (idx >= 0) return l.substring(idx + 1).trim();
                }
            }
        }

        if (rawAll.contains("VGA compatible") || rawAll.contains("3D controller")) {
            String[] lines = rawAll.split("\n");
            for (String l : lines) {
                if (l.contains("VGA compatible") || l.contains("3D controller")) {
                    int idx = l.indexOf(": ");
                    if (idx > 0) {
                        String rest = l.substring(idx + 2);
                        int idx2 = rest.indexOf(": ");
                        if (idx2 > 0) return rest.substring(idx2 + 2).trim();
                        return rest.trim();
                    }
                }
            }
        }

        String[] priority = {"NVIDIA GeForce", "NVIDIA", "GeForce", "AMD Radeon", "Radeon"};
        for (String kw : priority) {
            int idx = rawAll.indexOf(kw);
            if (idx < 0) continue;
            String result = rawAll.substring(idx);
            for (String kw2 : priority) {
                int idx2 = result.indexOf(kw2, kw.length());
                if (idx2 > 0) result = result.substring(0, idx2);
            }
            int intelIdx = result.indexOf("Intel", 1);
            if (intelIdx > 0) result = result.substring(0, intelIdx);
            return result.trim();
        }
        return rawAll.trim();
    }

    private static boolean isEmptyOrFail(String s) {
        return s == null || s.isEmpty() || s.equals("读取失败");
    }
    private static String run(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("Name")) {
                    sb.append(trimmed).append("\n");
                }
            }
            p.waitFor(3, TimeUnit.SECONDS);
            return sb.toString().trim();
        } catch (Exception e) {
            return "读取失败";
        }
    }
}