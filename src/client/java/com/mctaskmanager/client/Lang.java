package com.mctaskmanager.client;

import net.minecraft.network.chat.Component;

public class Lang {

    public static String get(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    // 布尔转 "开启/关闭"
    public static String onOff(boolean b) {
        return b ? get("mctm.state.on") : get("mctm.state.off");
    }
}