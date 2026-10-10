package com.mctaskmanager.client.page;

import com.mctaskmanager.client.HardwareEditScreen;
import com.mctaskmanager.client.HardwareInfoCollector;
import com.mctaskmanager.client.Lang;
import com.mctaskmanager.client.ManualHardwareStore;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class HardwarePage extends BasePage {

    private final List<int[]> buttonBounds = new ArrayList<>();
    private final List<Runnable> buttonActions = new ArrayList<>();

    private int lastMouseX = 0;
    private int lastMouseY = 0;

    @Override
    public int render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        buttonBounds.clear();
        buttonActions.clear();

        PageDraw.text(g, Lang.get("mctm.hardware.title"), x, y, PageDraw.COL_TEXT);
        y += 16;

        String cpu = ManualHardwareStore.getEffectiveCpu();
        String gpu = ManualHardwareStore.getEffectiveGpu();
        long ram = HardwareInfoCollector.getTotalRamGB();
        String ramText = (ram < 0) ? "读取中..." : Lang.get("mctm.hardware.ram_value", ram);

        // CPU
        PageDraw.text(g, Lang.get("mctm.hardware.cpu") + ":", x, y, PageDraw.COL_TEXT);
        PageDraw.text(g, cpu, x + 50, y, PageDraw.COL_TEXT);
        addSmallButton(g, x + 50 + Math.max(200, PageDraw.textWidth(cpu) + 30), y - 1,
                Lang.get("mctm.hardware.edit"),
                () -> {
                    if (mc != null) mc.setScreen(new HardwareEditScreen(parent, true));
                });
        y += 15;

        // GPU
        PageDraw.text(g, Lang.get("mctm.hardware.gpu") + ":", x, y, PageDraw.COL_TEXT);
        PageDraw.text(g, gpu, x + 50, y, PageDraw.COL_TEXT);
        addSmallButton(g, x + 50 + Math.max(200, PageDraw.textWidth(gpu) + 30), y - 1,
                Lang.get("mctm.hardware.edit"),
                () -> {
                    if (mc != null) mc.setScreen(new HardwareEditScreen(parent, false));
                });
        y += 15;

        // RAM
        PageDraw.text(g, Lang.get("mctm.hardware.ram") + ":", x, y, PageDraw.COL_TEXT);
        PageDraw.text(g, ramText, x + 50, y, PageDraw.COL_TEXT);
        y += 18;

        PageDraw.text(g, Lang.get("mctm.hardware.edit_hint"), x, y, PageDraw.COL_TEXT_SECOND);
        y += 12;

        return y;
    }

    private void addSmallButton(GuiGraphics g, int btnX, int y, String label, Runnable onClick) {
        int btnW = PageDraw.textWidth(label) + 12;
        int btnH = 12;

        boolean hovered = lastMouseX >= btnX && lastMouseX <= btnX + btnW
                && lastMouseY >= y && lastMouseY <= y + btnH;
        PageDraw.drawFlatButton(g, btnX, y, btnW, btnH, hovered);
        PageDraw.textCentered(g, label, btnX + btnW / 2, y + 2, PageDraw.COL_TEXT);

        buttonBounds.add(new int[]{btnX, y, btnW, btnH});
        buttonActions.add(onClick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < buttonBounds.size(); i++) {
            int[] b = buttonBounds.get(i);
            if (mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3]) {
                buttonActions.get(i).run();
                return true;
            }
        }
        return false;
    }
}