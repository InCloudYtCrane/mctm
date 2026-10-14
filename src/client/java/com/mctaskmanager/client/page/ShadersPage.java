package com.mctaskmanager.client.page;

import com.mctaskmanager.client.Lang;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ShadersPage extends BasePage {

    @Override
    public int render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        PageDraw.text(g, Lang.get("mctm.shaders.title"), x, y, PageDraw.COL_TEXT());
        y += 15;

        String combo = detectOptimizationCombo();
        PageDraw.text(g, Lang.get("mctm.shaders.current", combo), x, y, PageDraw.COL_TEXT());
        y += 11;

        boolean hasIris = FabricLoader.getInstance().isModLoaded("iris")
                || FabricLoader.getInstance().isModLoaded("oculus");
        boolean hasOptifine = FabricLoader.getInstance().isModLoaded("optifine");
        boolean hasSodium = FabricLoader.getInstance().isModLoaded("sodium")
                || FabricLoader.getInstance().isModLoaded("embeddium");

        PageDraw.text(g, "Iris: " + (hasIris ? "✓" : "✗") +
                        "   OptiFine: " + (hasOptifine ? "✓" : "✗") +
                        "   Sodium: " + (hasSodium ? "✓" : "✗"),
                x, y, PageDraw.COL_TEXT());
        y += 15;

        PageDraw.text(g, Lang.get("mctm.shaders.active"), x, y, PageDraw.COL_TEXT());
        y += 13;

        String activePack = readActiveShaderPack();
        if (activePack == null) {
            PageDraw.text(g, Lang.get("mctm.shaders.active_none"), x, y, PageDraw.COL_TEXT_SECOND());
        } else {
            PageDraw.text(g, "▶ " + activePack, x, y, PageDraw.COL_GREEN());
        }
        y += 16;

        PageDraw.text(g, Lang.get("mctm.shaders.installed"), x, y, PageDraw.COL_TEXT());
        y += 13;

        File gameDir = mc.gameDirectory;
        File shaderDir = new File(gameDir, "shaderpacks");

        if (!shaderDir.exists() || !shaderDir.isDirectory()) {
            PageDraw.text(g, Lang.get("mctm.shaders.folder_missing"), x, y, PageDraw.COL_TEXT_SECOND());
            y += 11;
            return y;
        }

        File[] files = shaderDir.listFiles();
        if (files == null || files.length == 0) {
            PageDraw.text(g, Lang.get("mctm.shaders.folder_empty"), x, y, PageDraw.COL_TEXT_SECOND());
            y += 11;
            return y;
        }

        for (File f : files) {
            String name = f.getName();
            if (!name.endsWith(".zip") && !f.isDirectory()) continue;

            String sizeText = f.isDirectory()
                    ? Lang.get("mctm.shaders.is_folder")
                    : PageDraw.formatSize(f.length());

            boolean isActive = activePack != null &&
                    (name.equals(activePack)
                            || name.equals(activePack + ".zip")
                            || name.replace(".zip", "").equals(activePack));

            String prefix = isActive ? "▶ " : "- ";
            int textColor = isActive ? PageDraw.COL_GREEN() : PageDraw.COL_TEXT();

            PageDraw.text(g, prefix + name + " (" + sizeText + ")", x, y, textColor);
            y += 11;
        }

        y += 8;
        return y;
    }

    private String detectOptimizationCombo() {
        var loader = FabricLoader.getInstance();
        boolean hasIris = loader.isModLoaded("iris") || loader.isModLoaded("oculus");
        boolean hasSodium = loader.isModLoaded("sodium") || loader.isModLoaded("embeddium");
        boolean hasOptifine = loader.isModLoaded("optifine");

        if (hasIris && hasSodium) return Lang.get("mctm.shaders.combo.iris_sodium");
        if (hasIris) return Lang.get("mctm.shaders.combo.iris");
        if (hasSodium) return Lang.get("mctm.shaders.combo.sodium");
        if (hasOptifine) return Lang.get("mctm.shaders.combo.optifine");
        return Lang.get("mctm.shaders.combo.vanilla");
    }

    private String readActiveShaderPack() {
        File configDir = new File(mc.gameDirectory, "config");
        File irisConfig = new File(configDir, "iris.properties");
        String irisPack = readShaderPackFromFile(irisConfig);
        if (irisPack != null) return irisPack;

        File gameDir = mc.gameDirectory;
        String ofPack = readShaderPackFromFile(new File(gameDir, "optionsshaders.txt"));
        if (ofPack != null) return ofPack;
        ofPack = readShaderPackFromFile(new File(gameDir, "options.txt"));
        if (ofPack != null) return ofPack;

        return null;
    }

    private String readShaderPackFromFile(File f) {
        if (f == null || !f.exists()) return null;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("shaderPack=")) {
                    String pack = line.substring("shaderPack=".length()).trim();
                    if (pack.isEmpty()
                            || pack.equalsIgnoreCase("OFF")
                            || pack.equalsIgnoreCase("(internal)")
                            || pack.equalsIgnoreCase("internal")) {
                        return null;
                    }
                    return pack;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}