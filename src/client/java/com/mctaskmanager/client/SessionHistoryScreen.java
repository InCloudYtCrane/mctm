package com.mctaskmanager.client;

import com.mctaskmanager.client.page.PageDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SessionHistoryScreen extends Screen {

    private final Screen parent;
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int viewportTop = 0;
    private int viewportBottom = 0;

    private static final int ROW_HEIGHT = 50;
    private static final int TOP_BAR_HEIGHT = 30;

    // 每行的删除按钮位置缓存
    // 每项：[x, y, w, h, sessionIndex]
    private final java.util.List<int[]> deleteButtons = new java.util.ArrayList<>();

    // 悬停时高亮
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    public SessionHistoryScreen(Screen parent) {
        super(Component.translatable("mctm.session.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.back"),
                b -> {
                    if (this.minecraft != null) this.minecraft.setScreen(parent);
                }
        ).bounds(this.width / 2 - 60, this.height - 28, 120, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("mctm.session.clear"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new ConfirmScreen(
                                confirm -> {
                                    if (confirm) {
                                        SessionManager.clearAll();
                                        scrollOffset = 0;
                                    }
                                    if (this.minecraft != null) {
                                        this.minecraft.setScreen(this);
                                    }
                                },
                                Component.translatable("mctm.session.clear.confirm.title"),
                                Component.translatable("mctm.session.clear.confirm.body")
                        ));
                    }
                }
        ).bounds(this.width - 110, this.height - 28, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        g.fill(0, 0, this.width, this.height, PageDraw.COL_BG());
        g.fill(0, 0, this.width, TOP_BAR_HEIGHT, PageDraw.COL_BG_MENU());
        g.fill(0, TOP_BAR_HEIGHT, this.width, TOP_BAR_HEIGHT + 1, PageDraw.COL_SEP());

        PageDraw.text(g, this.title.getString(), 10, 8, PageDraw.COL_TEXT());

        List<SessionManager.SessionRecord> sessions = SessionManager.getSessions();

        PageDraw.text(g, Lang.get("mctm.session.count", sessions.size()),
                this.width - 130, 8, PageDraw.COL_TEXT_SECOND());

        deleteButtons.clear();

        if (sessions.isEmpty()) {
            PageDraw.text(g, Lang.get("mctm.session.empty"), 20, TOP_BAR_HEIGHT + 20, PageDraw.COL_TEXT());
            PageDraw.text(g, Lang.get("mctm.session.empty_hint"),
                    20, TOP_BAR_HEIGHT + 36, PageDraw.COL_TEXT_SECOND());
            super.render(g, mouseX, mouseY, delta);
            return;
        }

        viewportTop = TOP_BAR_HEIGHT + 6;
        viewportBottom = this.height - 36;

        g.enableScissor(0, viewportTop, this.width, viewportBottom);

        int y = viewportTop - scrollOffset;
        int rowWidth = this.width - 40;
        int rowX = 20;

        for (int i = sessions.size() - 1; i >= 0; i--) {
            SessionManager.SessionRecord r = sessions.get(i);
            int realIndex = i; // 在 sessions 里的真实索引

            boolean hovered = mouseX >= rowX && mouseX <= rowX + rowWidth
                    && mouseY >= y && mouseY < y + ROW_HEIGHT - 4
                    && mouseY >= viewportTop && mouseY <= viewportBottom;

            int bg = hovered ? PageDraw.COL_ROW_HOVER() : PageDraw.COL_ROW_ALT();
            g.fill(rowX, y, rowX + rowWidth, y + ROW_HEIGHT - 4, bg);

            int accent = r.avgFps < 30 ? PageDraw.COL_RED()
                    : r.avgFps < 60 ? PageDraw.COL_YELLOW()
                    : PageDraw.COL_GREEN();
            g.fill(rowX, y, rowX + 3, y + ROW_HEIGHT - 4, accent);

            // 世界名 + 日期
            PageDraw.text(g, r.worldName, rowX + 10, y + 5, PageDraw.COL_TEXT());
            PageDraw.text(g, r.dateText(), rowX + rowWidth - 190, y + 5, PageDraw.COL_TEXT_SECOND());

            // 时长 + 帧率
            PageDraw.text(g, Lang.get("mctm.session.duration"), rowX + 10, y + 19, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, r.durationText(), rowX + 45, y + 19, PageDraw.COL_TEXT());

            PageDraw.text(g, Lang.get("mctm.session.avg"), rowX + 130, y + 19, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, Lang.get("mctm.unit.fps", r.avgFps), rowX + 165, y + 19, accent);

            PageDraw.text(g, Lang.get("mctm.session.min"), rowX + 230, y + 19, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, String.valueOf(r.minFps), rowX + 265, y + 19, PageDraw.COL_TEXT());

            PageDraw.text(g, Lang.get("mctm.session.low1"), rowX + 300, y + 19, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, String.valueOf(r.low1Percent), rowX + 350, y + 19, PageDraw.COL_TEXT());

            PageDraw.text(g, Lang.get("mctm.session.lag"), rowX + 400, y + 19, PageDraw.COL_TEXT_SECOND());
            int lagColor = r.lagCount == 0 ? PageDraw.COL_TEXT() : PageDraw.COL_YELLOW();
            PageDraw.text(g, r.lagText(), rowX + 435, y + 19, lagColor);

            // 峰值
            PageDraw.text(g, Lang.get("mctm.session.peak.memory"), rowX + 10, y + 33, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, r.peakMemoryMB + " MB", rowX + 70, y + 33, PageDraw.COL_TEXT());

            PageDraw.text(g, Lang.get("mctm.session.peak.chunks"), rowX + 160, y + 33, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, String.valueOf(r.peakChunks), rowX + 220, y + 33, PageDraw.COL_TEXT());

            PageDraw.text(g, Lang.get("mctm.session.peak.entities"), rowX + 270, y + 33, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, String.valueOf(r.peakEntities), rowX + 330, y + 33, PageDraw.COL_TEXT());

            // ===== 删除按钮 =====
            int btnW = 50;
            int btnH = 14;
            int btnX = rowX + rowWidth - btnW - 8;
            int btnY = y + 5;

            boolean delHovered = mouseX >= btnX && mouseX <= btnX + btnW
                    && mouseY >= btnY && mouseY <= btnY + btnH
                    && mouseY >= viewportTop && mouseY <= viewportBottom;

            // 悬浮时按钮才显眼一点
            if (hovered || delHovered) {
                PageDraw.drawFlatButton(g, btnX, btnY, btnW, btnH, delHovered);
            } else {
                // 平时半透明
                g.fill(btnX, btnY, btnX + btnW, btnY + btnH, PageDraw.COL_ROW_ALT());
                g.fill(btnX, btnY, btnX + btnW, btnY + 1, PageDraw.COL_BORDER());
                g.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, PageDraw.COL_BORDER());
                g.fill(btnX, btnY, btnX + 1, btnY + btnH, PageDraw.COL_BORDER());
                g.fill(btnX + btnW - 1, btnY, btnX + btnW, btnY + btnH, PageDraw.COL_BORDER());
            }

            int delTextColor = delHovered ? PageDraw.COL_RED() : PageDraw.COL_TEXT_SECOND();
            PageDraw.textCentered(g, Lang.get("mctm.session.delete"), btnX + btnW / 2, btnY + 3, delTextColor);

            // 记录按钮区域，供 mouseClicked 用
            deleteButtons.add(new int[]{btnX, btnY, btnW, btnH, realIndex});

            y += ROW_HEIGHT;
        }

        contentHeight = y - (viewportTop - scrollOffset);

        g.disableScissor();

        // 滚动条
        int viewHeight = viewportBottom - viewportTop;
        if (contentHeight > viewHeight) {
            int barX = this.width - 8;
            g.fill(barX, viewportTop, barX + 4, viewportBottom, PageDraw.COL_SCROLL_TRACK());
            double ratio = (double) viewHeight / contentHeight;
            int thumbH = Math.max(20, (int) (viewHeight * ratio));
            double scrollRatio = (double) scrollOffset / Math.max(1, contentHeight - viewHeight);
            int thumbY = viewportTop + (int) ((viewHeight - thumbH) * scrollRatio);
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, PageDraw.COL_SCROLL_THUMB());
        }

        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;

        // 检查删除按钮
        for (int[] b : deleteButtons) {
            if (mx >= b[0] && mx <= b[0] + b[2]
                    && my >= b[1] && my <= b[1] + b[3]) {
                int index = b[4];
                SessionManager.SessionRecord rec = SessionManager.getSessions().get(index);
                String label = rec.worldName + "  " + rec.dateText();

                // 弹二次确认
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new ConfirmScreen(
                            confirm -> {
                                if (confirm) {
                                    SessionManager.removeSession(index);
                                    // 调整滚动偏移，避免越界
                                    scrollOffset = Math.max(0, scrollOffset - ROW_HEIGHT);
                                }
                                if (this.minecraft != null) {
                                    this.minecraft.setScreen(this);
                                }
                            },
                            Component.translatable("mctm.session.delete.confirm.title"),
                            Component.translatable("mctm.session.delete.confirm.body", label)
                    ));
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int viewHeight = viewportBottom - viewportTop;
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        scrollOffset -= scrollY * 30;
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // 禁用模糊
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}