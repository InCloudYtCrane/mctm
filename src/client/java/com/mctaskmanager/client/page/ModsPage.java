package com.mctaskmanager.client.page;

import com.mctaskmanager.client.DataCache;
import com.mctaskmanager.client.Lang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ModsPage extends BasePage {

    private static final int TOP_BAR_HEIGHT = 30;
    private static final int CONTENT_ROW_HEIGHT = 12;

    private EditBox searchBox;
    private String filter = "";

    @Override
    public void onEnter() {
        if (parent == null) return;

        int boxW = 150;
        int boxH = 16;
        int boxX = parent.width - boxW - 12;
        int boxY = TOP_BAR_HEIGHT + 4;

        searchBox = new EditBox(mc.font, boxX, boxY, boxW, boxH, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setValue(filter);
        searchBox.setResponder(s -> this.filter = s);
        parent.mctmAddWidget(searchBox);
    }

    @Override
    public void onExit() {
        if (parent != null && searchBox != null) {
            parent.mctmRemoveWidget(searchBox);
            searchBox = null;
        }
    }

    @Override
    public int render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        List<DataCache.ModRow> all = DataCache.getModRows();
        long totalSize = DataCache.getTotalModSizeBytes();
        long largestSize = DataCache.getLargestModSizeBytes();

        List<DataCache.ModRow> list;
        if (filter.isEmpty()) {
            list = all;
        } else {
            String f = filter.toLowerCase();
            list = new ArrayList<>();
            for (DataCache.ModRow r : all) {
                if (r.name.toLowerCase().contains(f) || r.id.toLowerCase().contains(f)) {
                    list.add(r);
                }
            }
        }

        // 左侧标题
        PageDraw.text(g, Lang.get("mctm.mods.title"), x, y, PageDraw.COL_TEXT());

        String summary = filter.isEmpty()
                ? Lang.get("mctm.mods.summary", list.size(), PageDraw.formatSize(totalSize))
                : "§7" + Lang.get("mctm.mods.filtered", list.size(), all.size());
        PageDraw.text(g, summary, x + 90, y, PageDraw.COL_TEXT_SECOND());

        y += 15;

        // 表头
        int colName = x;
        int colSize = x + 240;
        int colPercent = x + 310;
        int colBar = x + 370;

        PageDraw.text(g, Lang.get("mctm.mods.col.name"), colName, y, PageDraw.COL_TEXT());
        PageDraw.text(g, Lang.get("mctm.mods.col.size"), colSize, y, PageDraw.COL_TEXT());
        PageDraw.text(g, Lang.get("mctm.mods.col.percent"), colPercent, y, PageDraw.COL_TEXT());
        PageDraw.text(g, Lang.get("mctm.mods.col.bar"), colBar, y, PageDraw.COL_TEXT());
        y += 10;
        PageDraw.drawSepH(g, x, y, parent.width - x - 15);
        y += 3;

        int barWidth = Math.max(50, parent.width - colBar - 20);

        int rowIndex = 0;
        for (DataCache.ModRow row : list) {
            int argb;
            switch (row.color) {
                case "green" -> argb = PageDraw.COL_GREEN();
                case "yellow" -> argb = PageDraw.COL_YELLOW();
                default -> argb = PageDraw.COL_GRAY();
            }

            if (rowIndex % 2 == 1) {
                g.fill(x - 2, y, parent.width - 15, y + CONTENT_ROW_HEIGHT, PageDraw.COL_ROW_ALT());
            }
            rowIndex++;

            String displayName = row.name;
            if (displayName.length() > 30) {
                displayName = displayName.substring(0, 28) + "..";
            }
            g.fill(colName, y + 3, colName + 5, y + 8, argb);
            PageDraw.text(g, displayName, colName + 8, y + 1, PageDraw.COL_TEXT());
            PageDraw.text(g, row.sizeText(), colSize, y + 1, PageDraw.COL_TEXT_SECOND());

            double percent = totalSize > 0 ? (row.sizeBytes * 100.0 / totalSize) : 0;
            PageDraw.text(g, String.format("%.1f%%", percent), colPercent, y + 1, PageDraw.COL_TEXT());

            double ratio = largestSize > 0 ? (double) row.sizeBytes / largestSize : 0;
            PageDraw.drawProgressBar(g, colBar, y + 1, barWidth, 9, ratio, argb);

            y += CONTENT_ROW_HEIGHT;
        }

        y += 8;
        return y;
    }
}