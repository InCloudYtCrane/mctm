package com.mctaskmanager.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

public class MctmConfigScreen extends Screen {

    private final Screen parent;
    private final MctmConfig cfg;

    public MctmConfigScreen(Screen parent) {
        super(Component.translatable("mctm.config.title"));
        this.parent = parent;
        this.cfg = MctmConfig.get();
    }

    @Override
    protected void init() {
        super.init();

        int cx = this.width / 2;
        int y = 30;
        int w = 240;
        int h = 18;
        int gap = 22;

        // 低帧率提示阈值
        addRenderableWidget(new IntSlider(cx - w / 2, y, w, h,
                "mctm.config.low_fps", 10, 120, cfg.lowFpsThreshold,
                v -> cfg.lowFpsThreshold = v));
        y += gap;

        // 方块实体扫描半径
        addRenderableWidget(new IntSlider(cx - w / 2, y, w, h,
                "mctm.config.scan_radius", 4, 32, cfg.scanRadius,
                v -> cfg.scanRadius = v));
        y += gap;

        // 是否显示低帧率 toast
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showLowFpsToast)
                .create(cx - w / 2, y, w, h,
                        Component.translatable("mctm.config.show_toast"),
                        (btn, val) -> cfg.showLowFpsToast = val));
        y += gap;

        // 提示冷却秒数
        addRenderableWidget(new IntSlider(cx - w / 2, y, w, h,
                "mctm.config.toast_cooldown", 5, 300, cfg.toastCooldownSec,
                v -> cfg.toastCooldownSec = v));
        y += gap;

        // 卡顿检测阈值
        addRenderableWidget(new IntSlider(cx - w / 2, y, w, h,
                "mctm.config.lag_threshold", 10, 120, cfg.lagDetectThreshold,
                v -> cfg.lagDetectThreshold = v));
        y += gap;

        // 卡顿持续时长
        addRenderableWidget(new IntSlider(cx - w / 2, y, w, h,
                "mctm.config.lag_duration", 200, 5000, cfg.lagMinDurationMs,
                v -> cfg.lagMinDurationMs = v));
        y += gap;

        // 报告是否包含模组列表
        addRenderableWidget(CycleButton.onOffBuilder(cfg.includeModListInReport)
                .create(cx - w / 2, y, w, h,
                        Component.translatable("mctm.config.include_mods"),
                        (btn, val) -> cfg.includeModListInReport = val));
        y += gap + 10;

        // 完成按钮
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> {
                    cfg.save();
                    if (minecraft != null) minecraft.setScreen(parent);
                }
        ).bounds(cx - w / 2, y, w, h).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        this.renderBackground(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        cfg.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    // ===== 内部类：整数滑块 =====
    static class IntSlider extends AbstractSliderButton {
        private final String labelKey;
        private final int min, max;
        private final IntConsumer onChange;

        IntSlider(int x, int y, int w, int h, String labelKey,
                  int min, int max, int init, IntConsumer onChange) {
            super(x, y, w, h,
                    Component.translatable(labelKey, init),
                    (init - min) / (double) (max - min));
            this.labelKey = labelKey;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(labelKey, getIntValue()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(getIntValue());
        }

        private int getIntValue() {
            return min + (int) Math.round(value * (max - min));
        }
    }
}