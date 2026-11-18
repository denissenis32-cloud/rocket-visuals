package com.rocketvisuals.fx;

import com.rocketvisuals.config.RocketConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** HUD layer: targeted-mob health bar, XP popup and thunder flash. Registered through Fabric's HudElementRegistry. */
public final class HudRenderer {
    private static LivingEntity tracked;
    private static long lastSeenMs;
    private static float shown = -1f;
    private static float ghost = -1f;

    private HudRenderer() {}

    public static int argb(int a, int r, int g, int b) {
        return (clamp255(a) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** Hue (0..1) to opaque-ish RGB without alpha. */
    public static int hue(float h) {
        float r = Math.max(0f, Math.min(1f, Math.abs(h * 6f - 3f) - 1f));
        float g = Math.max(0f, Math.min(1f, 2f - Math.abs(h * 6f - 2f)));
        float b = Math.max(0f, Math.min(1f, 2f - Math.abs(h * 6f - 4f)));
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static void extract(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getDebugOverlay().showDebugScreen()) return;
        RocketConfig c = RocketConfig.get();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        if (c.healthIndicators) health(g, mc, w);
        if (c.xpEffects && EffectsManager.xpPopupTicks > 0) xpPopup(g, mc, w, h);
        if (c.rainEffects && EffectsManager.flashTicks > 0) {
            int a = Math.min(90, EffectsManager.flashTicks * 12);
            g.fill(0, 0, w, h, argb(a, 255, 255, 255));
        }
    }

    private static void health(GuiGraphicsExtractor g, Minecraft mc, int w) {
        long now = System.currentTimeMillis();
        Entity t = mc.crosshairPickEntity;
        if (t instanceof LivingEntity le && le.isAlive() && le != mc.player) {
            if (le != tracked) {
                tracked = le;
                shown = ghost = le.getHealth();
            }
            lastSeenMs = now;
        }
        if (tracked == null || !tracked.isAlive() || tracked.isRemoved()) return;
        long age = now - lastSeenMs;
        if (age > 2500) return;
        float alpha = age < 2000 ? 1f : 1f - (age - 2000) / 500f;

        float hp = Math.max(0f, tracked.getHealth());
        float max = Math.max(1f, tracked.getMaxHealth());
        shown += (hp - shown) * 0.25f;
        if (ghost > shown) ghost += (shown - ghost) * 0.05f; else ghost = shown;
        float ratio = Math.min(1f, shown / max);
        float ghostRatio = Math.min(1f, ghost / max);

        int pw = 140, ph = 25;
        int x = w / 2 - pw / 2, y = 14;
        int bg = argb((int) (140 * alpha), 10, 10, 20);
        g.fill(x, y, x + pw, y + ph, bg);
        g.fill(x, y, x + pw, y + 1, argb((int) (230 * alpha), 120, 90, 255));

        String name = tracked.getName().getString();
        g.text(mc.font, name, x + 6, y + 5, argb((int) (255 * alpha), 255, 255, 255), true);
        String hpText = String.format("%.1f / %.1f", hp, max);
        g.text(mc.font, hpText, x + pw - 6 - mc.font.width(hpText), y + 5, argb((int) (255 * alpha), 220, 220, 230), true);

        int bx = x + 6, by = y + 16, bw = pw - 12, bh = 5;
        g.fill(bx, by, bx + bw, by + bh, argb((int) (200 * alpha), 30, 30, 40));
        g.fill(bx, by, bx + (int) (bw * ghostRatio), by + bh, argb((int) (200 * alpha), 255, 255, 255));
        int r, gr;
        if (ratio > 0.5f) { r = (int) (255 * (1f - ratio) * 2f); gr = 220; }
        else { r = 255; gr = (int) (220 * ratio * 2f); }
        g.fill(bx, by, bx + (int) (bw * ratio), by + bh, argb((int) (255 * alpha), r, gr, 70));
    }

    private static void xpPopup(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
        int ticks = EffectsManager.xpPopupTicks;
        float a = Math.min(1f, ticks / 10f);
        String s = Component.translatable("rocketvisuals.hud.xp", EffectsManager.xpPopupAmount).getString();
        int y = h - 70 - (40 - ticks) / 3;
        g.text(mc.font, s, w / 2 - mc.font.width(s) / 2, y, argb((int) (255 * a), 124, 255, 107), true);
    }
}
