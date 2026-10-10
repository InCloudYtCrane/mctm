package com.mctaskmanager.client.page;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class PageDraw {

    // 扁平化配色
    public static final int COL_BG            = 0xFFC6C6C6;
    public static final int COL_BG_MENU       = 0xFFB8B8B8;
    public static final int COL_MENU_HOVER    = 0xFFD0D0D0;
    public static final int COL_MENU_SELECTED = 0xFFD8D8D8;
    public static final int COL_SLOT          = 0xFFB8B8B8;
    public static final int COL_BORDER        = 0xFF9A9A9A;
    public static final int COL_SEP           = 0xFFA8A8A8;
    public static final int COL_TEXT          = 0xFF202020;
    public static final int COL_TEXT_SECOND   = 0xFF606060;
    public static final int COL_GREEN         = 0xFF3FAA3F;
    public static final int COL_YELLOW        = 0xFFB08020;
    public static final int COL_RED           = 0xFFAA3030;
    public static final int COL_BLUE          = 0xFF3060B0;
    public static final int COL_GOLD          = 0xFF9A6A00;
    public static final int COL_GRAY          = 0xFF808080;
    public static final int COL_ACCENT        = 0xFF3F7FBF;

    private static final Minecraft mc = Minecraft.getInstance();

    public static void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(mc.font, s, x, y, color, false);
    }

    public static void textCentered(GuiGraphics g, String s, int cx, int y, int color) {
        g.drawString(mc.font, s, cx - mc.font.width(s) / 2, y, color, false);
    }

    public static int textWidth(String s) {
        return mc.font.width(s);
    }

    public static void drawBox(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
    }

    public static void drawFlatButton(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        int bg = hovered ? COL_MENU_HOVER : COL_BG;
        drawBox(g, x, y, w, h, bg, COL_BORDER);
    }

    public static void drawProgressBar(GuiGraphics g, int x, int y, int w, int h,
                                       double ratio, int color) {
        drawBox(g, x, y, w, h, COL_SLOT, COL_BORDER);
        int fillW = (int) (ratio * (w - 4));
        if (fillW > 0) {
            g.fill(x + 2, y + 2, x + 2 + fillW, y + h - 2, color);
        }
    }

    public static void drawSepH(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, COL_SEP);
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
        return String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0);
    }
}