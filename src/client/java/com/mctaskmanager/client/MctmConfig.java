package com.mctaskmanager.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class MctmConfig {

    // ===== 用户可配置项 =====
    public int lowFpsThreshold = 30;
    public int scanRadius = 16;
    public boolean showLowFpsToast = true;
    public int toastCooldownSec = 30;
    public boolean includeModListInReport = true;

    /** 界面配色主题 id，见 {@link Theme#PRESETS}。 */
    public String theme = Theme.DEFAULT.id;

    // 卡顿追踪器
    public int lagDetectThreshold = 30;
    public int lagMinDurationMs = 1000;

    private static MctmConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static MctmConfig get() {
        if (instance == null) load();
        return instance;
    }

    /** 当前生效的主题；配置缺失或 id 无效时回退到默认主题。 */
    public static Theme getTheme() {
        return Theme.byId(get().theme);
    }

    private static File getFile() {
        File cfg = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!cfg.exists()) cfg.mkdirs();
        return new File(cfg, "mctm.json");
    }

    public static void load() {
        File f = getFile();
        if (f.exists()) {
            try {
                String json = Files.readString(f.toPath());
                instance = GSON.fromJson(json, MctmConfig.class);
            } catch (Exception e) {
                instance = new MctmConfig();
            }
        } else {
            instance = new MctmConfig();
        }
        if (instance == null) instance = new MctmConfig();
    }

    public void save() {
        try {
            Files.writeString(getFile().toPath(), GSON.toJson(this));
        } catch (IOException ignored) {}
    }
}