package com.mctaskmanager.client.page;

import com.mctaskmanager.client.Lang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ResourcePacksPage extends BasePage {

    private static final int TOP_BAR_HEIGHT = 30;

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
        PageDraw.text(g, Lang.get("mctm.packs.title"), x, y, PageDraw.COL_TEXT);
        y += 15;

        File rpDir = new File(mc.gameDirectory, "resourcepacks");

        List<String> packs = new ArrayList<>(mc.options.resourcePacks);

        if (!filter.isEmpty()) {
            String f = filter.toLowerCase();
            packs.removeIf(p -> !p.toLowerCase().contains(f));
        }

        if (packs.isEmpty()) {
            String msg = filter.isEmpty()
                    ? Lang.get("mctm.packs.none")
                    : "§7(无匹配结果)";
            PageDraw.text(g, msg, x, y, PageDraw.COL_TEXT_SECOND);
            y += 11;
            return y;
        }

        for (String packName : packs) {
            String sizeText = "?";
            if (rpDir.exists()) {
                String clean = packName;
                if (clean.startsWith("file/")) clean = clean.substring("file/".length());
                File[] candidates = rpDir.listFiles();
                if (candidates != null) {
                    for (File f : candidates) {
                        if (f.getName().equals(clean) || f.getName().equals(packName)) {
                            sizeText = PageDraw.formatSize(f.length());
                            break;
                        }
                    }
                }
            }
            PageDraw.text(g, "- " + packName + "  (" + sizeText + ")", x, y, PageDraw.COL_TEXT);
            y += 11;
        }

        y += 8;
        return y;
    }
}