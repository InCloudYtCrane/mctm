package com.mctaskmanager.client.page;

import com.mctaskmanager.client.TaskManagerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public abstract class BasePage {

    protected final Minecraft mc = Minecraft.getInstance();
    protected TaskManagerScreen parent;

    public void attach(TaskManagerScreen parent) {
        this.parent = parent;
    }

    public void onEnter() {}

    public void onExit() {}

    public abstract int render(GuiGraphics g, int x, int y, int mouseX, int mouseY);

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char c, int modifiers) {
        return false;
    }

    public void tick() {}

    protected boolean isMouseOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}