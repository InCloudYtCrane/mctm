package com.mctaskmanager.client.page;

import com.mctaskmanager.client.FpsMonitor;
import com.mctaskmanager.client.Lang;
import com.mctaskmanager.client.MinecraftTaskManagerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class OverviewPage extends BasePage {

    @Override
    public int render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        FpsMonitor monitor = MinecraftTaskManagerClient.getFpsMonitor();

        PageDraw.text(g, Lang.get("mctm.overview.realtime"), x, y, PageDraw.COL_TEXT);
        y += 14;

        int avgFps = monitor.getAverageFps();
        int minFps = monitor.getMinFps();
        int low1 = monitor.get1PercentLow();
        int low01 = monitor.get01PercentLow();
        double avgFrameTime = monitor.getAverageFrameTime();
        long avgMem = monitor.getAverageMemoryMB();

        int fpsColor = avgFps < monitor.getThreshold() ? PageDraw.COL_RED : PageDraw.COL_TEXT;

        PageDraw.text(g, Lang.get("mctm.overview.current", mc.getFps()), x, y, PageDraw.COL_TEXT);
        PageDraw.text(g, Lang.get("mctm.overview.average", avgFps), x + 100, y, fpsColor);
        PageDraw.text(g, Lang.get("mctm.overview.minimum", minFps), x + 200, y, PageDraw.COL_TEXT);
        PageDraw.text(g, Lang.get("mctm.overview.low1", low1), x + 300, y, PageDraw.COL_GOLD);
        PageDraw.text(g, Lang.get("mctm.overview.low01", low01), x + 400, y, PageDraw.COL_RED);
        y += 12;

        PageDraw.text(g, Lang.get("mctm.overview.frametime", String.format("%.2f", avgFrameTime)),
                x, y, PageDraw.COL_TEXT);
        PageDraw.text(g, Lang.get("mctm.overview.memoryused", avgMem), x + 120, y, PageDraw.COL_TEXT);
        y += 16;

        PageDraw.text(g, Lang.get("mctm.overview.trend"), x, y, PageDraw.COL_TEXT);
        y += 12;

        int graphTotalWidth = parent.width - x - 20;
        int labelWidth = 70;
        int graphHeight = 26;
        int rowGap = 4;

        List<Double> fpsData = monitor.getFpsGraphHistory();
        List<Double> ftData  = monitor.getFrameTimeGraphHistory();
        List<Double> memData = monitor.getMemoryGraphHistory();

        double fpsMax = Math.max(getMax(fpsData) * 1.15, 60);
        renderGraphRow(g, x, y, graphTotalWidth, labelWidth, graphHeight,
                Lang.get("mctm.overview.graph.fps"), String.format("%d", mc.getFps()),
                fpsData, PageDraw.COL_GREEN, fpsMax, String.format("%.0f", fpsMax));
        y += graphHeight + rowGap;

        double ftMax = Math.max(getMax(ftData) * 1.5, 20);
        renderGraphRow(g, x, y, graphTotalWidth, labelWidth, graphHeight,
                Lang.get("mctm.overview.graph.frametime"), String.format("%.2f ms", avgFrameTime),
                ftData, PageDraw.COL_YELLOW, ftMax, String.format("%.1f", ftMax));
        y += graphHeight + rowGap;

        long maxHeapMB = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        double memMax = Math.max(maxHeapMB, 1);
        renderGraphRow(g, x, y, graphTotalWidth, labelWidth, graphHeight,
                Lang.get("mctm.overview.graph.memory"), avgMem + " MB",
                memData, PageDraw.COL_BLUE, memMax, String.format("%d", maxHeapMB));
        y += graphHeight + 12;

        PageDraw.text(g, Lang.get("mctm.overview.gcstats"), x, y, PageDraw.COL_TEXT);
        y += 12;
        PageDraw.text(g, Lang.get("mctm.overview.gc.total",
                monitor.getTotalGcCount(), monitor.getTotalGcTime()), x, y, PageDraw.COL_TEXT_SECOND);
        y += 12;
        long gcTime10s = monitor.getGcTimeLast10s();
        int gcColor = gcTime10s > 2000 ? PageDraw.COL_RED : PageDraw.COL_TEXT_SECOND;
        PageDraw.text(g, Lang.get("mctm.overview.gc.last10s",
                monitor.getGcCountLast10s(), gcTime10s), x, y, gcColor);
        y += 16;

        PageDraw.text(g, Lang.get("mctm.overview.snapshots"), x, y, PageDraw.COL_TEXT);
        y += 14;

        var snapshots = monitor.getSnapshots();
        if (snapshots.isEmpty()) {
            PageDraw.text(g, Lang.get("mctm.overview.snapshot.none"), x, y, PageDraw.COL_TEXT_SECOND);
            y += 12;
        } else {
            PageDraw.text(g, Lang.get("mctm.overview.snapshot.header"), x, y, PageDraw.COL_TEXT_SECOND);
            y += 10;
            PageDraw.drawSepH(g, x, y, parent.width - x - 15);
            y += 3;

            for (int i = snapshots.size() - 1; i >= 0; i--) {
                var snap = snapshots.get(i);
                PageDraw.text(g, String.format("%s  %d  %-12s  %s",
                                snap.timeText(), snap.fps, snap.dimShort(), snap.coordText()),
                        x, y, PageDraw.COL_TEXT);
                y += 11;
            }
        }
        y += 8;

        PageDraw.text(g, Lang.get("mctm.overview.settings"), x, y, PageDraw.COL_TEXT);
        y += 14;

        PageDraw.text(g, Lang.get("mctm.overview.resolution",
                        mc.getWindow().getWidth() + " x " + mc.getWindow().getHeight()),
                x, y, PageDraw.COL_TEXT);
        y += 11;
        PageDraw.text(g, Lang.get("mctm.overview.renderdistance",
                mc.options.renderDistance().get()), x, y, PageDraw.COL_TEXT);
        y += 11;
        PageDraw.text(g, Lang.get("mctm.overview.vsync",
                Lang.onOff(mc.options.enableVsync().get())), x, y, PageDraw.COL_TEXT);
        y += 11;
        PageDraw.text(g, Lang.get("mctm.overview.fpslimit",
                mc.options.framerateLimit().get()), x, y, PageDraw.COL_TEXT);
        y += 14;

        PageDraw.text(g, Lang.get("mctm.overview.stats"), x, y, PageDraw.COL_TEXT);
        y += 14;
        PageDraw.text(g, Lang.get("mctm.overview.modcount",
                        com.mctaskmanager.client.DataCache.getModRows().size()),
                x, y, PageDraw.COL_TEXT);
        y += 11;
        PageDraw.text(g, Lang.get("mctm.overview.packcount",
                mc.options.resourcePacks.size()), x, y, PageDraw.COL_TEXT);
        y += 12;

        return y;
    }

    private void renderGraphRow(GuiGraphics g, int x, int y,
                                int totalWidth, int labelWidth, int height,
                                String label, String currentValue,
                                List<Double> data, int lineColor, double maxValue,
                                String maxLabel) {
        PageDraw.text(g, label, x, y + 1, PageDraw.COL_TEXT);
        PageDraw.text(g, currentValue, x, y + 11, PageDraw.COL_TEXT_SECOND);

        int gx = x + labelWidth;
        int gw = totalWidth - labelWidth;

        PageDraw.drawBox(g, gx, y, gw, height, PageDraw.COL_SLOT, PageDraw.COL_BORDER);
        drawGraphLine(g, data, gx + 2, y + 2, gw - 4, height - 4, lineColor, maxValue);

        String scaleText = Lang.get("mctm.overview.graph.max", maxLabel);
        PageDraw.text(g, scaleText,
                gx + gw - PageDraw.textWidth(scaleText) - 4, y + 1, PageDraw.COL_TEXT_SECOND);
    }

    private double getMax(List<Double> data) {
        if (data == null || data.isEmpty()) return 0;
        double m = 0;
        for (double d : data) if (d > m) m = d;
        return m;
    }

    private void drawGraphLine(GuiGraphics g, List<Double> data, int x, int y,
                               int width, int height, int argbColor, double maxValue) {
        if (data == null || data.size() < 2 || maxValue <= 0) return;

        int n = data.size();
        double stepX = (double) width / (n - 1);

        int prevX = -1;
        int prevY = -1;

        for (int i = 0; i < n; i++) {
            double v = Math.min(data.get(i), maxValue);
            int px = (int) (x + i * stepX);
            int py = (int) (y + height - (v / maxValue) * height);

            if (prevX >= 0) {
                drawLineSegment(g, prevX, prevY, px, py, argbColor);
            }
            prevX = px;
            prevY = py;
        }
    }

    private void drawLineSegment(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            g.fill(x1, y1, x1 + 1, y1 + 2, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int px = x1 + (x2 - x1) * i / steps;
            int py = y1 + (y2 - y1) * i / steps;
            g.fill(px, py, px + 1, py + 2, color);
        }
    }
}