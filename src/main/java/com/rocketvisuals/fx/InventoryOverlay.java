package com.rocketvisuals.fx;

import com.rocketvisuals.config.RocketConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/** Animated themed backdrop drawn behind inventory/container screens. Uses only fill() and text(). */
public final class InventoryOverlay {
    private InventoryOverlay() {}

    public static void render(GuiGraphicsExtractor g, Screen screen) {
        RocketConfig c = RocketConfig.get();
        if (!c.inventoryUi) return;
        int w = screen.width, h = screen.height;
        long t = System.currentTimeMillis();
        float hue = (t % 14000L) / 14000f;
        int accent = HudRenderer.hue(hue);
        int accent2 = HudRenderer.hue((hue + 0.35f) % 1f);

        int strips = c.lowLoadMode ? 4 : (c.quality >= 2 ? 24 : 12);
        int band = h / 2;
        int sh = Math.max(1, band / strips);
        for (int i = 0; i < strips; i++) {
            float k = 1f - i / (float) strips;
            int a = (int) (70 * k * k);
            g.fill(0, i * sh, w, (i + 1) * sh, (a << 24) | accent);
            g.fill(0, h - (i + 1) * sh, w, h - i * sh, (a << 24) | accent2);
        }
        g.fill(0, 0, w, 2, (0xC8 << 24) | accent);
        g.fill(0, h - 2, w, h, (0xC8 << 24) | accent2);

        int len = 16, th = 2, m = 6;
        int col = (0xE0 << 24) | 0xFFFFFF;
        g.fill(m, m, m + len, m + th, col);            g.fill(m, m, m + th, m + len, col);
        g.fill(w - m - len, m, w - m, m + th, col);    g.fill(w - m - th, m, w - m, m + len, col);
        g.fill(m, h - m - th, m + len, h - m, col);    g.fill(m, h - m - len, m + th, h - m, col);
        g.fill(w - m - len, h - m - th, w - m, h - m, col); g.fill(w - m - th, h - m - len, w - m, h - m, col);

        if (!c.lowLoadMode) {
            int sweep = (int) ((t / 6) % (w + 120)) - 60;
            g.fill(sweep, 0, sweep + 40, 1, (0xFF << 24) | 0xFFFFFF);
        }
        Minecraft mc = Minecraft.getInstance();
        g.text(mc.font, "ROCKET VISUALS", 12, h - 14, (0xA0 << 24) | 0xFFFFFF, true);
    }
}
