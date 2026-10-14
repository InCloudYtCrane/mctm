package com.mctaskmanager.client;

import com.mctaskmanager.client.page.BasePage;
import com.mctaskmanager.client.page.DiagnosisPage;
import com.mctaskmanager.client.page.HardwarePage;
import com.mctaskmanager.client.page.HeatmapPage;
import com.mctaskmanager.client.page.LagHistoryPage;
import com.mctaskmanager.client.page.ModsPage;
import com.mctaskmanager.client.page.OverviewPage;
import com.mctaskmanager.client.page.PageDraw;
import com.mctaskmanager.client.page.ResourcePacksPage;
import com.mctaskmanager.client.page.ShadersPage;
import com.mctaskmanager.client.page.WorldPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.List;

public class TaskManagerScreen extends Screen {

    private static final int SIDEBAR_WIDTH = 96;
    private static final int TOP_BAR_HEIGHT = 30;
    private static final int MENU_ROW_HEIGHT = 16;

    private int currentPageIndex = 0;

    private final BasePage[] pages = new BasePage[9];
    private final double[] scrollOffsets = new double[9];
    private double currentContentHeight = 0;

    private static final String[] PAGE_KEYS = {
            "mctm.page.overview",
            "mctm.page.diagnosis",
            "mctm.page.hardware",
            "mctm.page.mods",
            "mctm.page.resourcepacks",
            "mctm.page.shaders",
            "mctm.page.world",
            "mctm.page.heatmap",
            "mctm.page.laghistory"
    };

    public TaskManagerScreen() {
        super(Component.translatable("mctm.title"));
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(Button.builder(
                Component.translatable("mctm.config.open"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new MctmConfigScreen(this));
                    }
                }
        ).bounds(this.width - 290, this.height - 26, 90, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("mctm.export.title"),
                b -> exportReport()
        ).bounds(this.width - 190, this.height - 26, 90, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("mctm.button.close"),
                b -> this.onClose()
        ).bounds(this.width - 90, this.height - 26, 80, 18).build());

        if (pages[0] == null) {
            pages[0] = new OverviewPage();
            pages[1] = new DiagnosisPage();
            pages[2] = new HardwarePage();
            pages[3] = new ModsPage();
            pages[4] = new ResourcePacksPage();
            pages[5] = new ShadersPage();
            pages[6] = new WorldPage();
            pages[7] = new HeatmapPage();
            pages[8] = new LagHistoryPage();
        }

        for (BasePage p : pages) {
            if (p != null) p.attach(this);
        }

        if (currentPageIndex == 6 || currentPageIndex == 7) {
            DataCache.scanBlockEntities();
        }
        if (pages[currentPageIndex] != null) {
            pages[currentPageIndex].onEnter();
        }
    }

    private void switchPage(int newIndex) {
        if (newIndex == currentPageIndex) return;
        if (pages[currentPageIndex] != null) {
            pages[currentPageIndex].onExit();
        }
        currentPageIndex = newIndex;
        if (newIndex == 6 || newIndex == 7) {
            DataCache.scanBlockEntities();
        }
        if (pages[newIndex] != null) {
            pages[newIndex].onEnter();
        }
    }

    private void exportReport() {
        Minecraft mc = Minecraft.getInstance();
        List<DiagnosisEngine.DiagItem> diagItems =
                DiagnosisEngine.diagnose(mc, DataCache.getBlockEntityCounts());
        File out = ReportExporter.export(mc, DataCache.getBlockEntityCounts(), diagItems);

        if (out != null) {
            mc.getToasts().addToast(new net.minecraft.client.gui.components.toasts.SystemToast(
                    net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.translatable("mctm.export.title"),
                    Component.literal(out.getAbsolutePath())
            ));
        } else {
            mc.getToasts().addToast(new net.minecraft.client.gui.components.toasts.SystemToast(
                    net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.translatable("mctm.export.title"),
                    Component.translatable("mctm.export.fail", "unknown")
            ));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < SIDEBAR_WIDTH) return false;
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;

        double viewHeight = this.height - TOP_BAR_HEIGHT - 4;
        double maxScroll = Math.max(0, currentContentHeight - viewHeight);

        scrollOffsets[currentPageIndex] -= scrollY * 30;

        if (scrollOffsets[currentPageIndex] < 0) scrollOffsets[currentPageIndex] = 0;
        if (scrollOffsets[currentPageIndex] > maxScroll) scrollOffsets[currentPageIndex] = maxScroll;

        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;

        if (mx < SIDEBAR_WIDTH && my >= TOP_BAR_HEIGHT) {
            int rowY = TOP_BAR_HEIGHT + 4;
            for (int i = 0; i < PAGE_KEYS.length; i++) {
                if (my >= rowY && my < rowY + MENU_ROW_HEIGHT) {
                    switchPage(i);
                    return true;
                }
                rowY += MENU_ROW_HEIGHT;
            }
        }

        if (pages[currentPageIndex] != null) {
            return pages[currentPageIndex].mouseClicked(mx, my, button);
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (pages[currentPageIndex] != null) {
            return pages[currentPageIndex].keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (super.charTyped(c, modifiers)) return true;
        if (pages[currentPageIndex] != null) {
            return pages[currentPageIndex].charTyped(c, modifiers);
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, PageDraw.COL_BG());
        // 卡片式内容区（仅启用卡片层次的主题可见）
        PageDraw.drawCard(g, SIDEBAR_WIDTH + 4, TOP_BAR_HEIGHT + 4, this.width - SIDEBAR_WIDTH - 8, this.height - TOP_BAR_HEIGHT - 12);
        g.fill(0, 0, this.width, TOP_BAR_HEIGHT, PageDraw.COL_BG_MENU());
        g.fill(0, TOP_BAR_HEIGHT, SIDEBAR_WIDTH, this.height, PageDraw.COL_BG_MENU());
        g.fill(0, TOP_BAR_HEIGHT, this.width, TOP_BAR_HEIGHT + 1, PageDraw.COL_SEP());
        g.fill(SIDEBAR_WIDTH, TOP_BAR_HEIGHT, SIDEBAR_WIDTH + 1, this.height, PageDraw.COL_SEP());

        PageDraw.text(g, this.title.getString(), 8, 6, PageDraw.COL_TEXT());
        renderTopBar(g);
        renderSidebar(g, mouseX, mouseY);

        g.enableScissor(0, TOP_BAR_HEIGHT + 1, this.width, this.height);

        int contentX = SIDEBAR_WIDTH + 10;
        int baseY = TOP_BAR_HEIGHT + 8 - (int) scrollOffsets[currentPageIndex];

        currentContentHeight = 0;
        int endY;
        BasePage cur = pages[currentPageIndex];
        if (cur == null) {
            PageDraw.text(g, "§7施工中...", contentX, baseY, PageDraw.COL_TEXT_SECOND());
            endY = baseY + 14;
        } else {
            endY = cur.render(g, contentX, baseY, mouseX, mouseY);
        }
        currentContentHeight = endY - baseY;

        g.disableScissor();

        super.render(g, mouseX, mouseY, delta);
        renderScrollbar(g);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // 禁用 1.21 模糊背景
    }

    public <T extends GuiEventListener & net.minecraft.client.gui.components.Renderable & NarratableEntry>
    T mctmAddWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    public void mctmRemoveWidget(GuiEventListener widget) {
        this.removeWidget(widget);
    }

    private void renderTopBar(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        int y = 18;

        PageDraw.text(g, Lang.get("mctm.topbar.fps", mc.getFps()), 8, y, PageDraw.COL_TEXT());

        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long max  = Runtime.getRuntime().maxMemory();
        double usedGB = used / 1024.0 / 1024.0 / 1024.0;
        double maxGB  = max  / 1024.0 / 1024.0 / 1024.0;
        int memColor = (usedGB > maxGB * 0.8) ? PageDraw.COL_RED() : PageDraw.COL_TEXT();
        PageDraw.text(g, Lang.get("mctm.topbar.memory",
                String.format("%.2f", usedGB), String.format("%.2f", maxGB)), 70, y, memColor);

        int chunks = 0;
        if (mc.level != null) {
            try {
                chunks = mc.level.getChunkSource().getLoadedChunksCount();
            } catch (Exception ignored) {}
        }
        PageDraw.text(g, Lang.get("mctm.topbar.chunks", chunks), 220, y, PageDraw.COL_TEXT());

        int entities = mc.level != null ? mc.level.getEntityCount() : 0;
        PageDraw.text(g, Lang.get("mctm.topbar.entities", entities), 300, y, PageDraw.COL_TEXT());
    }

    private void renderSidebar(GuiGraphics g, int mouseX, int mouseY) {
        int rowY = TOP_BAR_HEIGHT + 4;
        for (int i = 0; i < PAGE_KEYS.length; i++) {
            boolean selected = (i == currentPageIndex);
            boolean hovered = !selected
                    && mouseX < SIDEBAR_WIDTH && mouseX >= 0
                    && mouseY >= rowY && mouseY < rowY + MENU_ROW_HEIGHT;

            if (selected) {
                g.fill(0, rowY, SIDEBAR_WIDTH, rowY + MENU_ROW_HEIGHT, PageDraw.COL_MENU_SELECTED());
                g.fill(0, rowY, 3, rowY + MENU_ROW_HEIGHT, PageDraw.COL_ACCENT());
            } else if (hovered) {
                g.fill(0, rowY, SIDEBAR_WIDTH, rowY + MENU_ROW_HEIGHT, PageDraw.COL_MENU_HOVER());
            }

            // 卡顿历史页：如果有未读记录，画一个小红点
            String label = Component.translatable(PAGE_KEYS[i]).getString();
            if (i == 8 && MinecraftTaskManagerClient.getLagTracker().hasUnread()) {
                label = "§c● §r" + label;
            }

            int textColor = selected ? PageDraw.COL_TEXT() : PageDraw.COL_TEXT_SECOND();
            PageDraw.text(g, label, 8, rowY + 4, textColor);

            rowY += MENU_ROW_HEIGHT;
        }
    }

    private void renderScrollbar(GuiGraphics g) {
        double viewHeight = this.height - TOP_BAR_HEIGHT - 4;
        if (currentContentHeight <= viewHeight) return;

        int barX = this.width - 8;
        int barTop = TOP_BAR_HEIGHT + 4;
        int barBottom = this.height - 4;
        int barHeight = barBottom - barTop;

        g.fill(barX, barTop, barX + 4, barBottom, PageDraw.COL_SCROLL_TRACK());

        double ratio = viewHeight / currentContentHeight;
        int thumbHeight = Math.max(20, (int) (barHeight * ratio));
        double scrollRatio = scrollOffsets[currentPageIndex] / Math.max(1, currentContentHeight - viewHeight);
        int thumbY = barTop + (int) ((barHeight - thumbHeight) * scrollRatio);

        g.fill(barX, thumbY, barX + 4, thumbY + thumbHeight, PageDraw.COL_SCROLL_THUMB());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}