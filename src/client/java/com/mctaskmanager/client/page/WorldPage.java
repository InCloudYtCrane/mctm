package com.mctaskmanager.client.page;

import com.mctaskmanager.client.DataCache;
import com.mctaskmanager.client.Lang;
import com.mctaskmanager.client.MctmConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldPage extends BasePage {

    private static final int TOP_BAR_HEIGHT = 30;

    private EditBox searchBox;
    private String filter = "";

    @Override
    public void onEnter() {
        if (parent == null) return;
        DataCache.scanBlockEntities();

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
        if (mc.level == null) {
            PageDraw.text(g, Lang.get("mctm.not_in_world"), x, y, PageDraw.COL_TEXT_SECOND);
            y += 11;
            return y;
        }

        String f = filter.toLowerCase();

        // ===== 实体 =====
        Map<String, Integer> entityCounts = new HashMap<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            String typeName = entity.getType().getDescription().getString();
            entityCounts.merge(typeName, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> entitySorted = new ArrayList<>(entityCounts.entrySet());
        entitySorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        if (!f.isEmpty()) {
            entitySorted.removeIf(e -> !e.getKey().toLowerCase().contains(f));
        }

        PageDraw.text(g, Lang.get("mctm.world.entity.title"), x, y, PageDraw.COL_TEXT);
        y += 13;
        PageDraw.text(g, Lang.get("mctm.world.col.type"), x, y, PageDraw.COL_TEXT);
        PageDraw.text(g, Lang.get("mctm.world.col.count"), x + 200, y, PageDraw.COL_TEXT);
        y += 10;
        PageDraw.drawSepH(g, x, y, parent.width - x - 15);
        y += 3;

        if (entitySorted.isEmpty()) {
            PageDraw.text(g, Lang.get("mctm.world.entity.none"), x, y, PageDraw.COL_TEXT_SECOND);
            y += 11;
        } else {
            int ri = 0;
            for (Map.Entry<String, Integer> entry : entitySorted) {
                if (ri % 2 == 1) {
                    g.fill(x - 2, y, parent.width - 15, y + 11, 0x18FFFFFF);
                }
                ri++;

                String typeName = entry.getKey();
                int amount = entry.getValue();
                int color = amount > 100 ? PageDraw.COL_RED : PageDraw.COL_TEXT;
                if (typeName.length() > 28) typeName = typeName.substring(0, 26) + "..";
                PageDraw.text(g, typeName, x, y, color);
                PageDraw.text(g, String.valueOf(amount), x + 200, y, color);
                y += 11;
            }
        }

        y += 10;

        // ===== 方块实体 =====
        Map<String, Integer> counts = DataCache.getBlockEntityCounts();
        Map<String, long[]> positions = DataCache.getBlockEntityPositions();

        List<Map.Entry<String, Integer>> beSorted = new ArrayList<>(counts.entrySet());
        beSorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        if (!f.isEmpty()) {
            beSorted.removeIf(e -> !e.getKey().toLowerCase().contains(f));
        }

        int scanRadius = MctmConfig.get().scanRadius;
        PageDraw.text(g, Lang.get("mctm.world.be.title", scanRadius), x, y, PageDraw.COL_TEXT);
        y += 13;
        PageDraw.text(g, Lang.get("mctm.world.col.type"), x, y, PageDraw.COL_TEXT);
        PageDraw.text(g, Lang.get("mctm.world.col.count"), x + 180, y, PageDraw.COL_TEXT);
        PageDraw.text(g, Lang.get("mctm.world.col.offset"), x + 250, y, PageDraw.COL_TEXT);
        y += 10;
        PageDraw.drawSepH(g, x, y, parent.width - x - 15);
        y += 3;

        if (beSorted.isEmpty()) {
            PageDraw.text(g, Lang.get("mctm.world.be.none"), x, y, PageDraw.COL_TEXT_SECOND);
            y += 11;
            return y;
        }

        BlockPos playerPos = mc.player != null ? mc.player.blockPosition() : BlockPos.ZERO;

        int ri = 0;
        for (Map.Entry<String, Integer> entry : beSorted) {
            if (ri % 2 == 1) {
                g.fill(x - 2, y, parent.width - 15, y + 11, 0x18FFFFFF);
            }
            ri++;

            String typeName = entry.getKey();
            int amount = entry.getValue();
            int color = amount > 200 ? PageDraw.COL_RED : PageDraw.COL_TEXT;
            if (typeName.length() > 24) typeName = typeName.substring(0, 22) + "..";

            PageDraw.text(g, typeName, x, y, color);
            PageDraw.text(g, String.valueOf(amount), x + 180, y, color);

            long[] posSum = positions.get(entry.getKey());
            if (posSum != null && amount > 0) {
                int avgX = (int) (posSum[0] / amount);
                int avgY = (int) (posSum[1] / amount);
                int avgZ = (int) (posSum[2] / amount);
                int dx = avgX - playerPos.getX();
                int dy = avgY - playerPos.getY();
                int dz = avgZ - playerPos.getZ();
                PageDraw.text(g, String.format("(%+d, %+d, %+d)", dx, dy, dz),
                        x + 250, y, PageDraw.COL_TEXT_SECOND);
            }

            y += 11;
        }

        y += 8;
        PageDraw.text(g, Lang.get("mctm.world.offset_hint"), x, y, PageDraw.COL_TEXT_SECOND);
        y += 11;

        return y;
    }
}