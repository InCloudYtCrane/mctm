package com.mctaskmanager.client;

import com.mctaskmanager.client.page.PageDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HardwareEditScreen extends Screen {

    private final Screen parent;
    private final boolean isCpu;
    private EditBox editBox;

    public HardwareEditScreen(Screen parent, boolean isCpu) {
        super(Component.literal(isCpu ? "CPU" : "GPU"));
        this.parent = parent;
        this.isCpu = isCpu;
    }

    @Override
    protected void init() {
        super.init();

        int boxWidth = 400;
        int boxX = this.width / 2 - boxWidth / 2;
        int boxY = this.height / 2 - 20;

        editBox = new EditBox(this.font, boxX, boxY, boxWidth, 20, Component.literal(""));

        // ⚠ 必须先设 maxLength，再设 value
        editBox.setMaxLength(512);

        String current = isCpu
                ? ManualHardwareStore.getEffectiveCpu()
                : ManualHardwareStore.getEffectiveGpu();
        if (current != null && !current.equals(Lang.get("mctm.hardware.reading"))) {
            editBox.setValue(current);
        } else {
            editBox.setValue("");
        }

        this.addRenderableWidget(editBox);
        this.setFocused(editBox);

        this.addRenderableWidget(Button.builder(
                Component.translatable("mctm.button.yes"),
                b -> {
                    if (isCpu) ManualHardwareStore.setManualCpu(editBox.getValue());
                    else ManualHardwareStore.setManualGpu(editBox.getValue());
                    if (this.minecraft != null) this.minecraft.setScreen(parent);
                }
        ).bounds(this.width / 2 - 85, boxY + 30, 80, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("mctm.button.no"),
                b -> {
                    if (this.minecraft != null) this.minecraft.setScreen(parent);
                }
        ).bounds(this.width / 2 + 5, boxY + 30, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        this.renderBackground(g, mouseX, mouseY, delta);
        super.render(g, mouseX, mouseY, delta);

        g.drawCenteredString(this.font,
                Lang.get("mctm.hardware.input_title") + " · " + (isCpu ? "CPU" : "GPU"),
                this.width / 2, this.height / 2 - 50, PageDraw.COL_TEXT());
        g.drawCenteredString(this.font,
                Lang.get("mctm.hardware.input_hint"),
                this.width / 2, this.height / 2 - 36, PageDraw.COL_TEXT_SECOND());
    }

    @Override
    public boolean isPauseScreen() { return false; }
}