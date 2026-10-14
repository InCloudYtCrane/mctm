package com.mctaskmanager.client.page;

import com.mctaskmanager.client.MctmConfig;
import com.mctaskmanager.client.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 绘图工具与配色入口。
 *
 * <p>配色不再写死：{@code COL_*} 是从当前 {@link Theme} 动态取值的方法，
 * 所以 {@link MctmConfig#getTheme()} 变化后下一帧就生效，调用方写法保持不变。
 */
public class PageDraw {

    private static final Minecraft mc = Minecraft.getInstance();

    /** 当前生效的主题。 */
    public static Theme theme() {
        return MctmConfig.getTheme();
    }

    // ===== 结构色 =====
    public static int COL_BG()            { return theme().bg; }
    public static int COL_BG_MENU()       { return theme().bgMenu; }
    public static int COL_MENU_HOVER()    { return theme().menuHover; }
    public static int COL_MENU_SELECTED() { return theme().menuSelected; }
    public static int COL_SLOT()          { return theme().slot; }
    public static int COL_BORDER()        { return theme().border; }
    public static int COL_SEP()           { return theme().sep; }
    public static int COL_PANEL()         { return theme().panel; }

    // ===== 文字 =====
    public static int COL_TEXT()          { return theme().text; }
    public static int COL_TEXT_SECOND()   { return theme().textSecond; }

    // ===== 强调色 =====
    public static int COL_ACCENT()        { return theme().accent; }
    public static int COL_ACCENT2()       { return theme().accent2; }

    // ===== 语义色 =====
    public static int COL_GREEN()         { return theme().green; }
    public static int COL_YELLOW()        { return theme().yellow; }
    public static int COL_RED()           { return theme().red; }
    public static int COL_BLUE()          { return theme().blue; }
    public static int COL_GOLD()          { return theme().gold; }
    public static int COL_GRAY()          { return theme().gray; }

    // ===== 派生色（斑马纹 / 选中行 / 滚动条）=====
    /** 表格斑马纹。 */
    public static int COL_ROW_ALT()       { return theme().rowAlt; }
    /** 表格行悬停。 */
    public static int COL_ROW_HOVER()     { return theme().rowHover; }
    /** 表格行选中。 */
    public static int COL_ROW_SELECTED()  { return theme().rowSelected; }
    /** 滚动条轨道。 */
    public static int COL_SCROLL_TRACK()  { return theme().scrollTrack; }
    /** 滚动条滑块。 */
    public static int COL_SCROLL_THUMB()  { return theme().scrollThumb; }

    // ===== 文字 =====

    public static void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(mc.font, s, x, y, color, false);
    }

    public static void textCentered(GuiGraphics g, String s, int cx, int y, int color) {
        g.drawString(mc.font, s, cx - mc.font.width(s) / 2, y, color, false);
    }

    public static int textWidth(String s) {
        return mc.font.width(s);
    }

    // ===== 形状 =====

    public static void drawBox(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
    }

    /** 内容卡片：仅在主题启用卡片层次（panel != bg）时绘制底色。 */
    public static void drawCard(GuiGraphics g, int x, int y, int w, int h) {
        Theme t = theme();
        if (!t.hasPanel()) return;
        g.fill(x, y, x + w, y + h, t.panel);
        g.fill(x, y, x + w, y + 1, t.border);
        g.fill(x, y + h - 1, x + w, y + h, t.border);
        g.fill(x, y, x + 1, y + h, t.border);
        g.fill(x + w - 1, y, x + w, y + h, t.border);
    }

    /** 区块标题下的横线（视觉规范统一，替代各页面手写的 fill）。 */
    public static void drawSectionLine(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, COL_SEP());
    }

    public static void drawFlatButton(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        drawBox(g, x, y, w, h, hovered ? COL_ROW_HOVER() : COL_PANEL(), COL_BORDER());
    }

    public static void drawProgressBar(GuiGraphics g, int x, int y, int w, int h,
                                       double ratio, int color) {
        drawBox(g, x, y, w, h, COL_SLOT(), COL_BORDER());
        int fillW = (int) (ratio * (w - 4));
        if (fillW > 0) {
            g.fill(x + 2, y + 2, x + 2 + fillW, y + h - 2, color);
        }
    }

    public static void drawSepH(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, COL_SEP());
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
        return String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0);
    }
}
