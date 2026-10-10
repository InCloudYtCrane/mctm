package com.mctaskmanager.client;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class ManualHardwareStore {

    private static String manualCpu = null;
    private static String manualGpu = null;
    private static boolean loaded = false;

    private static File getFile() {
        File cfg = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!cfg.exists()) cfg.mkdirs();
        return new File(cfg, "mctm-manual.properties");
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        File f = getFile();
        if (!f.exists()) return;
        try {
            Properties p = new Properties();
            try (var r = Files.newInputStream(f.toPath())) {
                p.load(r);
            }
            manualCpu = p.getProperty("cpu");
            manualGpu = p.getProperty("gpu");
        } catch (IOException ignored) {}
    }

    public static void save() {
        File f = getFile();
        try {
            Properties p = new Properties();
            if (manualCpu != null) p.setProperty("cpu", manualCpu);
            if (manualGpu != null) p.setProperty("gpu", manualGpu);
            try (var w = Files.newOutputStream(f.toPath())) {
                p.store(w, "Minecraft Task Manager manual hardware override");
            }
        } catch (IOException ignored) {}
    }

    public static String getManualCpu() { load(); return manualCpu; }
    public static String getManualGpu() { load(); return manualGpu; }

    public static void setManualCpu(String cpu) {
        load();
        manualCpu = (cpu == null || cpu.isBlank()) ? null : cpu.trim();
        save();
    }

    public static void setManualGpu(String gpu) {
        load();
        manualGpu = (gpu == null || gpu.isBlank()) ? null : gpu.trim();
        save();
    }

    public static String getEffectiveCpu() {
        load();
        return manualCpu != null ? manualCpu : HardwareInfoCollector.getCpuName();
    }

    public static String getEffectiveGpu() {
        load();
        return manualGpu != null ? manualGpu : HardwareInfoCollector.getGpuName();
    }

    public static boolean isCpuOverridden() { load(); return manualCpu != null; }
    public static boolean isGpuOverridden() { load(); return manualGpu != null; }
}