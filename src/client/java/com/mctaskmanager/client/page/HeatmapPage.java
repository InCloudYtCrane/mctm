package com.mctaskmanager.client.page;

import com.mctaskmanager.client.DataCache;
import com.mctaskmanager.client.Lang;
import com.mctaskmanager.client.MctmConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

import java.util.Map;

public class HeatmapPage extends BasePage {

    private int lastMouseX = 0;
    private int lastMouseY = 0;

    private int dataSource = 0;

    private int switchBtnX = 0;
    private int switchBtnY = 0;
    private int switchBtnW = 0;
    private int switchBtnH = 14;

    // 热力渐变属于数据语义色，不随主题变化（保证各主题下判读一致）
    private static final int[] LEGEND_COLORS = {
            0xFFEDEDED,
            0xFFFFF4A0,
            0xFFFFD060,
            0xFFFF9020,
            0xFFFF6030,
            0xFFD03030,
    };
    /** 未加载区块的底色，取当前主题的灰色，避免深浅主题下都不协调。 */
    private static int colUnloaded() {
        return PageDraw.COL_GRAY();
    }

    @Override
    public void onEnter() {
        DataCache.scanBlockEntities();
    }

    @Override
    public int render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        if (mc.level == null || mc.player == null) {
            PageDraw.text(g, Lang.get("mctm.not_in_world"), x, y, PageDraw.COL_TEXT_SECOND());
            y += 11;
            return y;
        }

        int radius = MctmConfig.get().scanRadius;
        int gridSize = radius * 2 + 1;

        int availWidth = parent.width - x - 40;
        int cellSize = Math.min(12, Math.max(4, availWidth / gridSize));
        int gridPixels = gridSize * cellSize;

        // 顶部：标题 + 数据源切换
        PageDraw.text(g, Lang.get("mctm.heatmap.title"), x, y, PageDraw.COL_TEXT());

        String sourceLabel = dataSource == 0
                ? Lang.get("mctm.heatmap.source.blockentities")
                : Lang.get("mctm.heatmap.source.entities");
        int labelW = PageDraw.textWidth(sourceLabel) + 16;
        switchBtnX = parent.width - labelW - 20;
        switchBtnY = y - 2;
        switchBtnW = labelW;
        switchBtnH = 14;

        boolean btnHovered = mouseX >= switchBtnX && mouseX <= switchBtnX + switchBtnW
                && mouseY >= switchBtnY && mouseY <= switchBtnY + switchBtnH;
        PageDraw.drawFlatButton(g, switchBtnX, switchBtnY, switchBtnW, switchBtnH, btnHovered);
        PageDraw.textCentered(g, sourceLabel, switchBtnX + switchBtnW / 2, switchBtnY + 3, PageDraw.COL_TEXT());

        y += 14;
        PageDraw.text(g, Lang.get("mctm.heatmap.hint"), x, y, PageDraw.COL_TEXT_SECOND());
        y += 14;

        BlockPos playerPos = mc.player.blockPosition();
        int centerCX = playerPos.getX() >> 4;
        int centerCZ = playerPos.getZ() >> 4;

        Map<Long, Integer> chunkData = dataSource == 0
                ? DataCache.getBlockEntityCountPerChunk()
                : DataCache.getEntityCountPerChunk();

        int maxCount = 0;
        for (int v : chunkData.values()) if (v > maxCount) maxCount = v;

        int gridX = x;
        int gridY = y;

        // 未加载底色
        g.fill(gridX, gridY, gridX + gridPixels, gridY + gridPixels, colUnloaded());

        // 画格子
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int cx = centerCX + dx;
                int cz = centerCZ + dz;
                long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
                Integer countObj = chunkData.get(key);

                int px = gridX + (dx + radius) * cellSize;
                int py = gridY + (dz + radius) * cellSize;

                if (countObj == null) continue;

                int color = colorFor(countObj);
                g.fill(px, py, px + cellSize, py + cellSize, color);

                if (cellSize >= 5) {
                    g.fill(px, py, px + cellSize, py + 1, 0x30000000);
                    g.fill(px, py, px + 1, py + cellSize, 0x30000000);
                }
            }
        }

        // 外边框（跟随主题文字色，深浅主题下都清晰）
        int gridEdge = PageDraw.COL_TEXT();
        g.fill(gridX - 1, gridY - 1, gridX + gridPixels + 1, gridY, gridEdge);
        g.fill(gridX - 1, gridY + gridPixels, gridX + gridPixels + 1, gridY + gridPixels + 1, gridEdge);
        g.fill(gridX - 1, gridY, gridX, gridY + gridPixels, gridEdge);
        g.fill(gridX + gridPixels, gridY, gridX + gridPixels + 1, gridY + gridPixels, gridEdge);

        // 玩家位置
        int playerGridX = gridX + radius * cellSize;
        int playerGridY = gridY + radius * cellSize;

        int boxX1 = playerGridX - 1;
        int boxY1 = playerGridY - 1;
        int boxX2 = playerGridX + cellSize + 1;
        int boxY2 = playerGridY + cellSize + 1;

        g.fill(boxX1, boxY1, boxX2, boxY1 + 2, 0xFFFF2020);
        g.fill(boxX1, boxY2 - 2, boxX2, boxY2, 0xFFFF2020);
        g.fill(boxX1, boxY1, boxX1 + 2, boxY2, 0xFFFF2020);
        g.fill(boxX2 - 2, boxY1, boxX2, boxY2, 0xFFFF2020);

        if (cellSize >= 4) {
            int cxm = playerGridX + cellSize / 2;
            int cym = playerGridY + cellSize / 2;
            g.fill(cxm - 1, cym - 1, cxm + 1, cym + 1, 0xFFFF2020);
        }

        // 悬停
        int hoverDx = -9999, hoverDz = -9999;
        if (mouseX >= gridX && mouseX < gridX + gridPixels
                && mouseY >= gridY && mouseY < gridY + gridPixels) {
            hoverDx = (mouseX - gridX) / cellSize - radius;
            hoverDz = (mouseY - gridY) / cellSize - radius;
        }

        if (hoverDx != -9999) {
            int hx = gridX + (hoverDx + radius) * cellSize;
            int hy = gridY + (hoverDz + radius) * cellSize;
            int hi = PageDraw.COL_TEXT();
            g.fill(hx, hy, hx + cellSize, hy + 1, hi);
            g.fill(hx, hy + cellSize - 1, hx + cellSize, hy + cellSize, hi);
            g.fill(hx, hy, hx + 1, hy + cellSize, hi);
            g.fill(hx + cellSize - 1, hy, hx + cellSize, hy + cellSize, hi);
        }

        y = gridY + gridPixels + 10;

        // ===== 图例 =====
        PageDraw.text(g, Lang.get("mctm.heatmap.legend"), x, y, PageDraw.COL_TEXT());
        y += 12;

        int lx = x;
        for (int i = 0; i < LEGEND_COLORS.length; i++) {
            int color = LEGEND_COLORS[i];
            String label = i == 0 ? "0"
                    : i == 1 ? "1-2"
                    : i == 2 ? "3-5"
                    : i == 3 ? "6-15"
                    : i == 4 ? "16-30"
                    : "31+";
            g.fill(lx - 1, y - 1, lx + 11, y + 11, PageDraw.COL_TEXT());
            g.fill(lx, y, lx + 10, y + 10, color);
            PageDraw.text(g, label, lx + 14, y + 1, PageDraw.COL_TEXT_SECOND());
            lx += 14 + PageDraw.textWidth(label) + 10;
        }
        y += 16;

        g.fill(x - 1, y - 1, x + 11, y + 11, PageDraw.COL_TEXT());
        g.fill(x, y, x + 10, y + 10, colUnloaded());
        PageDraw.text(g, Lang.get("mctm.heatmap.unloaded"), x + 14, y + 1, PageDraw.COL_TEXT_SECOND());
        y += 16;

        g.fill(x + 1, y + 1, x + 9, y + 3, 0xFFFF2020);
        g.fill(x + 1, y + 7, x + 9, y + 9, 0xFFFF2020);
        g.fill(x + 1, y + 1, x + 3, y + 9, 0xFFFF2020);
        g.fill(x + 7, y + 1, x + 9, y + 9, 0xFFFF2020);
        PageDraw.text(g, Lang.get("mctm.heatmap.you"), x + 14, y + 1, PageDraw.COL_TEXT_SECOND());
        y += 16;

        // 悬停信息
        if (hoverDx != -9999) {
            int hx = centerCX + hoverDx;
            int hz = centerCZ + hoverDz;
            long key = ((long) hx << 32) | (hz & 0xFFFFFFFFL);
            Integer countObj = chunkData.get(key);
            String unit = dataSource == 0
                    ? Lang.get("mctm.heatmap.unit.blockentities")
                    : Lang.get("mctm.heatmap.unit.entities");

            PageDraw.text(g, Lang.get("mctm.heatmap.chunk"), x, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, "(" + hx + ", " + hz + ")", x + 40, y, PageDraw.COL_TEXT());

            if (countObj == null) {
                PageDraw.text(g, Lang.get("mctm.heatmap.cell.unloaded"), x + 140, y, PageDraw.COL_TEXT_SECOND());
            } else if (countObj == 0) {
                PageDraw.text(g, "· " + Lang.get("mctm.heatmap.cell.count", 0, unit),
                        x + 140, y, PageDraw.COL_TEXT_SECOND());
            } else {
                PageDraw.text(g, "· " + Lang.get("mctm.heatmap.cell.count", countObj, unit),
                        x + 140, y, PageDraw.COL_YELLOW());
            }
            y += 12;
        }

        // ===== 统计 =====
        y += 6;
        PageDraw.text(g, Lang.get("mctm.heatmap.stats"), x, y, PageDraw.COL_TEXT());
        y += 12;

        int total = 0;
        int loadedChunks = 0;
        int hotChunks = 0;
        int maxChunkX = 0, maxChunkZ = 0;

        for (Map.Entry<Long, Integer> e : chunkData.entrySet()) {
            int v = e.getValue();
            total += v;
            loadedChunks++;
            if (v >= 6) hotChunks++;
            if (v == maxCount && maxCount > 0) {
                long key = e.getKey();
                maxChunkX = (int) (key >> 32);
                maxChunkZ = (int) key;
            }
        }

        String unit = dataSource == 0
                ? Lang.get("mctm.heatmap.unit.short.blockentities")
                : Lang.get("mctm.heatmap.unit.short.entities");

        PageDraw.text(g, Lang.get("mctm.heatmap.loaded"), x, y, PageDraw.COL_TEXT_SECOND());
        PageDraw.text(g, loadedChunks + " / " + (gridSize * gridSize), x + 90, y, PageDraw.COL_TEXT());
        y += 11;

        PageDraw.text(g, Lang.get("mctm.heatmap.total_unit", unit), x, y, PageDraw.COL_TEXT_SECOND());
        PageDraw.text(g, String.valueOf(total), x + 90, y, PageDraw.COL_TEXT());
        y += 11;

        PageDraw.text(g, Lang.get("mctm.heatmap.hot"), x, y, PageDraw.COL_TEXT_SECOND());
        PageDraw.text(g, String.valueOf(hotChunks), x + 130, y,
                hotChunks > 0 ? PageDraw.COL_YELLOW() : PageDraw.COL_TEXT());
        y += 11;

        if (maxCount > 0) {
            PageDraw.text(g, Lang.get("mctm.heatmap.peak"), x, y, PageDraw.COL_TEXT_SECOND());
            PageDraw.text(g, Lang.get("mctm.heatmap.peak.value", maxCount, maxChunkX, maxChunkZ),
                    x + 100, y, PageDraw.COL_TEXT());
        } else {
            PageDraw.text(g, Lang.get("mctm.heatmap.peak.none"), x, y, PageDraw.COL_TEXT());
        }
        y += 11;

        return y;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx >= switchBtnX && mx <= switchBtnX + switchBtnW
                && my >= switchBtnY && my <= switchBtnY + switchBtnH) {
            dataSource = 1 - dataSource;
            return true;
        }
        return false;
    }

    private int colorFor(int count) {
        if (count == 0) return LEGEND_COLORS[0];
        if (count <= 2) return LEGEND_COLORS[1];
        if (count <= 5) return LEGEND_COLORS[2];
        if (count <= 15) return LEGEND_COLORS[3];
        if (count <= 30) return LEGEND_COLORS[4];
        return LEGEND_COLORS[5];
    }
}