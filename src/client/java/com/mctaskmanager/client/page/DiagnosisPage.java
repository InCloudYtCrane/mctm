package com.mctaskmanager.client.page;

import com.mctaskmanager.client.DiagnosisEngine;
import com.mctaskmanager.client.DataCache;
import com.mctaskmanager.client.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class DiagnosisPage extends BasePage {

    private final List<int[]> buttonBounds = new ArrayList<>();
    private final List<DiagnosisEngine.Action> buttonActions = new ArrayList<>();

    private int lastMouseX = 0;
    private int lastMouseY = 0;

    @Override
    public int render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        buttonBounds.clear();
        buttonActions.clear();

        PageDraw.text(g, Lang.get("mctm.diag.title"), x, y, PageDraw.COL_TEXT);
        y += 16;

        List<DiagnosisEngine.DiagItem> items =
                DiagnosisEngine.diagnose(Minecraft.getInstance(), DataCache.getBlockEntityCounts());

        if (items.isEmpty()) {
            PageDraw.text(g, Lang.get("mctm.diag.all_good"), x, y, PageDraw.COL_GREEN);
            y += 14;
        } else {
            for (DiagnosisEngine.DiagItem item : items) {
                String prefix;
                int textColor;
                switch (item.level) {
                    case "warn" -> { prefix = "[!] "; textColor = PageDraw.COL_RED; }
                    case "info" -> { prefix = "[i] "; textColor = PageDraw.COL_YELLOW; }
                    case "good" -> { prefix = "[✓] "; textColor = PageDraw.COL_GREEN; }
                    default     -> { prefix = ""; textColor = PageDraw.COL_TEXT; }
                }
                PageDraw.text(g, prefix + item.text, x, y, textColor);

                if (item.action != null) {
                    String label = item.action.label();
                    int btnW = PageDraw.textWidth(label) + 12;
                    int btnH = 12;
                    int btnX = parent.width - btnW - 20;
                    int btnY = y - 1;

                    boolean hovered = mouseX >= btnX && mouseX <= btnX + btnW
                            && mouseY >= btnY && mouseY <= btnY + btnH;
                    PageDraw.drawFlatButton(g, btnX, btnY, btnW, btnH, hovered);
                    PageDraw.textCentered(g, label, btnX + btnW / 2, btnY + 2, PageDraw.COL_TEXT);

                    buttonBounds.add(new int[]{btnX, btnY, btnW, btnH});
                    buttonActions.add(item.action);
                }

                y += 11;

                if (item.detail != null && !item.detail.isEmpty()) {
                    PageDraw.text(g, "     " + item.detail, x, y, PageDraw.COL_TEXT_SECOND);
                    y += 10;
                }
                y += 3;
            }
        }

        y += 8;
        PageDraw.text(g, Lang.get("mctm.diag.footer1"), x, y, PageDraw.COL_TEXT_SECOND);
        y += 11;
        PageDraw.text(g, Lang.get("mctm.diag.footer2"), x, y, PageDraw.COL_TEXT_SECOND);
        y += 11;
        return y;
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