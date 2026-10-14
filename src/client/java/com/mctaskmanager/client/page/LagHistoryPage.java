package com.mctaskmanager.client.page;

import com.mctaskmanager.client.LagTracker;
import com.mctaskmanager.client.Lang;
import com.mctaskmanager.client.MinecraftTaskManagerClient;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class LagHistoryPage extends BasePage {

    private static final int ROW_HEIGHT = 12;
    private int selectedIndex = -1;

    private int listStartY = 0;
    private int listCount = 0;

    @Override
    public void onEnter() {
        MinecraftTaskManagerClient.getLagTracker().markRead();
    }

    @Override
    public int render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        LagTracker tracker = MinecraftTaskManagerClient.getLagTracker();
        List<LagTracker.LagRecord> records = tracker.getRecords();

        PageDraw.text(g, Lang.get("mctm.lag.title"), x, y, PageDraw.COL_TEXT());
        PageDraw.text(g, Lang.get("mctm.lag.summary", records.size()),
                x + 130, y, PageDraw.COL_TEXT_SECOND());
        y += 14;
        PageDraw.text(g, Lang.get("mctm.lag.hint"),
                x, y, PageDraw.COL_TEXT_SECOND());
        y += 14;

        if (records.isEmpty()) {
            PageDraw.text(g, Lang.get("mctm.lag.empty"), x, y, PageDraw.COL_TEXT());
            y += 12;
            PageDraw.text(g, Lang.get("mctm.lag.empty_hint"),
                    x, y, PageDraw.COL_TEXT_SECOND());
            y += 12;
            return y;
        }

        // 表头
        PageDraw.text(g, Lang.get("mctm.lag.col.time"), x, y, PageDraw.COL_TEXT());
        PageDraw.text(g, Lang.get("mctm.lag.col.duration"), x + 70, y, PageDraw.COL_TEXT());
        PageDraw.text(g, Lang.get("mctm.lag.col.minfps"), x + 130, y, PageDraw.COL_TEXT());
        PageDraw.text(g, Lang.get("mctm.lag.col.reason"), x + 200, y, PageDraw.COL_TEXT());
        y += 10;
        PageDraw.drawSepH(g, x, y, parent.width - x - 15);
        y += 3;

        listStartY = y;
        listCount = records.size();

        for (int i = records.size() - 1; i >= 0; i--) {
            LagTracker.LagRecord rec = records.get(i);
            boolean selected = (i == selectedIndex);
            boolean hovered = mouseX >= x && mouseX <= parent.width - 15
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;

            if (selected) {
                g.fill(x - 2, y - 1, parent.width - 15, y + ROW_HEIGHT - 1, PageDraw.COL_ROW_SELECTED());
            } else if (hovered) {
                g.fill(x - 2, y - 1, parent.width - 15, y + ROW_HEIGHT - 1, PageDraw.COL_ROW_HOVER());
            }

            int fpsColor = rec.minFps < 15 ? PageDraw.COL_RED()
                    : rec.minFps < 25 ? PageDraw.COL_YELLOW()
                    : PageDraw.COL_TEXT();

            PageDraw.text(g, rec.timeText(), x, y, PageDraw.COL_TEXT());
            PageDraw.text(g, rec.durationText(), x + 70, y, PageDraw.COL_TEXT());
            PageDraw.text(g, String.valueOf(rec.minFps), x + 130, y, fpsColor);

            int reasonColor = switch (rec.reason) {
                case "gc" -> PageDraw.COL_BLUE();
                case "entities", "blockentities", "memory" -> PageDraw.COL_RED();
                case "chunks" -> PageDraw.COL_GOLD();
                default -> PageDraw.COL_TEXT_SECOND();
            };
            PageDraw.text(g, rec.reasonText(), x + 200, y, reasonColor);

            y += ROW_HEIGHT;
        }

        y += 8;

        // ===== 详情 =====
        if (selectedIndex >= 0 && selectedIndex < records.size()) {
            LagTracker.LagRecord rec = records.get(selectedIndex);
            PageDraw.drawSepH(g, x, y, parent.width - x - 15);
            y += 6;

            PageDraw.text(g, Lang.get("mctm.lag.detail.title"), x, y, PageDraw.COL_TEXT());
            y += 12;

            PageDraw.text(g, Lang.get("mctm.lag.time"), x, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, rec.timeText(), x + 50, y, PageDraw.COL_TEXT());
            y += 11;

            PageDraw.text(g, Lang.get("mctm.lag.pos"), x, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, rec.coordText(), x + 50, y, PageDraw.COL_TEXT());
            PageDraw.text(g, Lang.get("mctm.lag.dim"), x + 220, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, rec.dimShort(), x + 260, y, PageDraw.COL_TEXT());
            y += 11;

            PageDraw.text(g, Lang.get("mctm.lag.duration"), x, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, rec.durationText(), x + 50, y, PageDraw.COL_TEXT());
            PageDraw.text(g, Lang.get("mctm.lag.min"), x + 130, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, Lang.get("mctm.unit.fps", rec.minFps), x + 170, y, PageDraw.COL_TEXT());
            PageDraw.text(g, Lang.get("mctm.lag.avg"), x + 230, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, Lang.get("mctm.unit.fps", rec.avgFps), x + 270, y, PageDraw.COL_TEXT());
            y += 11;

            PageDraw.text(g, Lang.get("mctm.lag.entities"), x, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, Lang.get("mctm.unit.count", rec.entityCount), x + 50, y, PageDraw.COL_TEXT());
            PageDraw.text(g, Lang.get("mctm.lag.chunks"), x + 130, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, Lang.get("mctm.unit.count", rec.chunkCount), x + 170, y, PageDraw.COL_TEXT());
            y += 11;

            PageDraw.text(g, Lang.get("mctm.lag.memory"), x, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, rec.memoryUsedMB + " / " + rec.memoryMaxMB + " MB",
                    x + 50, y, PageDraw.COL_TEXT());
            PageDraw.text(g, Lang.get("mctm.lag.gc"), x + 220, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, rec.gcTimeDuringLag + " ms", x + 250, y, PageDraw.COL_TEXT());
            y += 14;

            // 诊断
            PageDraw.text(g, Lang.get("mctm.lag.diagnosis"), x, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, rec.reasonText(), x + 50, y, PageDraw.COL_RED());
            y += 11;
            PageDraw.text(g, rec.reasonDetail, x + 20, y, PageDraw.COL_TEXT_SECOND());
            y += 14;

            if (!rec.topEntities.isEmpty()) {
                PageDraw.text(g, Lang.get("mctm.lag.top_entities"), x, y, PageDraw.COL_TEXT());
                y += 11;
                for (String s : rec.topEntities) {
                    PageDraw.text(g, "  " + s, x, y, PageDraw.COL_TEXT_SECOND());
                    y += 10;
                }
                y += 3;
            }

            if (!rec.topBlockEntities.isEmpty()) {
                PageDraw.text(g, Lang.get("mctm.lag.top_block_entities"), x, y, PageDraw.COL_TEXT());
                y += 11;
                for (String s : rec.topBlockEntities) {
                    PageDraw.text(g, "  " + s, x, y, PageDraw.COL_TEXT_SECOND());
                    y += 10;
                }
                y += 3;
            }
        }

        return y;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx < 96 || mx > parent.width - 15) return false;
        if (my < listStartY) return false;

        int idx = (int) ((my - listStartY) / ROW_HEIGHT);
        if (idx < 0 || idx >= listCount) return false;

        LagTracker tracker = MinecraftTaskManagerClient.getLagTracker();
        List<LagTracker.LagRecord> records = tracker.getRecords();
        if (records.isEmpty()) return false;

        int realIndex = records.size() - 1 - idx;
        if (realIndex < 0 || realIndex >= records.size()) return false;

        if (selectedIndex == realIndex) {
            selectedIndex = -1;
        } else {
            selectedIndex = realIndex;
        }
        return true;
    }
}