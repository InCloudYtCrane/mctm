package com.mctaskmanager.client.page;

import com.mctaskmanager.client.DataCache;
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

    private static final int[] LEGEND_COLORS = {
            0xFFEDEDED,
            0xFFFFF4A0,
            0xFFFFD060,
            0xFFFF9020,
            0xFFFF6030,
            0xFFD03030,
    };
    private static final int COL_UNLOADED = 0xFF505050;

    @Override
    public void onEnter() {
        DataCache.scanBlockEntities();
    }

    @Override
    public int render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        if (mc.level == null || mc.player == null) {
            PageDraw.text(g, "请先进入一个世界", x, y, PageDraw.COL_TEXT_SECOND);
            y += 11;
            return y;
        }

        int radius = MctmConfig.get().scanRadius;
        int gridSize = radius * 2 + 1;

        int availWidth = parent.width - x - 40;
        int cellSize = Math.min(12, Math.max(4, availWidth / gridSize));
        int gridPixels = gridSize * cellSize;

        // 顶部：标题 + 数据源切换
        PageDraw.text(g, "=== 热力图 ===", x, y, PageDraw.COL_TEXT);

        String sourceLabel = dataSource == 0 ? "数据源: 方块实体" : "数据源: 实体";
        int labelW = PageDraw.textWidth(sourceLabel) + 16;
        switchBtnX = parent.width - labelW - 20;
        switchBtnY = y - 2;
        switchBtnW = labelW;
        switchBtnH = 14;

        boolean btnHovered = mouseX >= switchBtnX && mouseX <= switchBtnX + switchBtnW
                && mouseY >= switchBtnY && mouseY <= switchBtnY + switchBtnH;
        PageDraw.drawFlatButton(g, switchBtnX, switchBtnY, switchBtnW, switchBtnH, btnHovered);
        PageDraw.textCentered(g, sourceLabel, switchBtnX + switchBtnW / 2, switchBtnY + 3, PageDraw.COL_TEXT);

        y += 14;
        PageDraw.text(g, "每个格子 = 1 个区块，颜色越深数量越多", x, y, PageDraw.COL_TEXT_SECOND);
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
        g.fill(gridX, gridY, gridX + gridPixels, gridY + gridPixels, COL_UNLOADED);

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

        // 外边框
        g.fill(gridX - 1, gridY - 1, gridX + gridPixels + 1, gridY, 0xFF000000);
        g.fill(gridX - 1, gridY + gridPixels, gridX + gridPixels + 1, gridY + gridPixels + 1, 0xFF000000);
        g.fill(gridX - 1, gridY, gridX, gridY + gridPixels, 0xFF000000);
        g.fill(gridX + gridPixels, gridY, gridX + gridPixels + 1, gridY + gridPixels, 0xFF000000);

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
            g.fill(hx, hy, hx + cellSize, hy + 1, 0xFFFFFFFF);
            g.fill(hx, hy + cellSize - 1, hx + cellSize, hy + cellSize, 0xFFFFFFFF);
            g.fill(hx, hy, hx + 1, hy + cellSize, 0xFFFFFFFF);
            g.fill(hx + cellSize - 1, hy, hx + cellSize, hy + cellSize, 0xFFFFFFFF);
        }

        y = gridY + gridPixels + 10;

        // ===== 图例 =====
        PageDraw.text(g, "图例：", x, y, PageDraw.COL_TEXT);
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
            g.fill(lx - 1, y - 1, lx + 11, y + 11, 0xFF000000);
            g.fill(lx, y, lx + 10, y + 10, color);
            PageDraw.text(g, label, lx + 14, y + 1, PageDraw.COL_TEXT_SECOND);
            lx += 14 + PageDraw.textWidth(label) + 10;
        }
        y += 16;

        g.fill(x - 1, y - 1, x + 11, y + 11, 0xFF000000);
        g.fill(x, y, x + 10, y + 10, COL_UNLOADED);
        PageDraw.text(g, "= 未加载", x + 14, y + 1, PageDraw.COL_TEXT_SECOND);
        y += 16;

        g.fill(x + 1, y + 1, x + 9, y + 3, 0xFFFF2020);
        g.fill(x + 1, y + 7, x + 9, y + 9, 0xFFFF2020);
        g.fill(x + 1, y + 1, x + 3, y + 9, 0xFFFF2020);
        g.fill(x + 7, y + 1, x + 9, y + 9, 0xFFFF2020);
        PageDraw.text(g, "= 你的位置", x + 14, y + 1, PageDraw.COL_TEXT_SECOND);
        y += 16;

        // 悬停信息
        if (hoverDx != -9999) {
            int hx = centerCX + hoverDx;
            int hz = centerCZ + hoverDz;
            long key = ((long) hx << 32) | (hz & 0xFFFFFFFFL);
            Integer countObj = chunkData.get(key);
            String unit = dataSource == 0 ? "个方块实体" : "个实体";

            PageDraw.text(g, "区块", x, y, PageDraw.COL_TEXT_SECOND);
            PageDraw.text(g, "(" + hx + ", " + hz + ")", x + 40, y, PageDraw.COL_TEXT);

            if (countObj == null) {
                PageDraw.text(g, "· 未加载", x + 140, y, PageDraw.COL_TEXT_SECOND);
            } else if (countObj == 0) {
                PageDraw.text(g, "· 0 " + unit, x + 140, y, PageDraw.COL_TEXT_SECOND);
            } else {
                PageDraw.text(g, "· " + countObj + " " + unit, x + 140, y, PageDraw.COL_YELLOW);
            }
            y += 12;
        }

        // ===== 统计 =====
        y += 6;
        PageDraw.text(g, "当前区域统计：", x, y, PageDraw.COL_TEXT);
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

        String unit = dataSource == 0 ? "方块实体" : "实体";

        PageDraw.text(g, "已加载区块:", x, y, PageDraw.COL_TEXT_SECOND);
        PageDraw.text(g, loadedChunks + " / " + (gridSize * gridSize), x + 90, y, PageDraw.COL_TEXT);
        y += 11;

        PageDraw.text(g, unit + "总数:", x, y, PageDraw.COL_TEXT_SECOND);
        PageDraw.text(g, String.valueOf(total), x + 90, y, PageDraw.COL_TEXT);
        y += 11;

        PageDraw.text(g, "高密度区块 (≥6):", x, y, PageDraw.COL_TEXT_SECOND);
        PageDraw.text(g, String.valueOf(hotChunks), x + 130, y,
                hotChunks > 0 ? PageDraw.COL_YELLOW : PageDraw.COL_TEXT);
        y += 11;

        if (maxCount > 0) {
            PageDraw.text(g, "最高密度区块:", x, y, PageDraw.COL_TEXT_SECOND);
            PageDraw.text(g, maxCount + " 个  (" + maxChunkX + ", " + maxChunkZ + ")",
                    x + 100, y, PageDraw.COL_TEXT);
        } else {
            PageDraw.text(g, "最高密度区块: 0 个", x, y, PageDraw.COL_TEXT);
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