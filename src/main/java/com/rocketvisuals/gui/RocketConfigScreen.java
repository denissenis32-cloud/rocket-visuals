package com.rocketvisuals.gui;

import com.rocketvisuals.config.RocketConfig;
import com.rocketvisuals.fx.HudRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** The ROCKET VISUALS settings menu: two pages, every change is saved immediately. */
public class RocketConfigScreen extends Screen {
    private record Opt(String key, BooleanSupplier get, Consumer<Boolean> set) {}

    private static final int PAGES = 2;
    private final Screen parent;
    private int page;
    private final List<Opt> toggles = List.of(
            new Opt("rocketvisuals.opt.mining", () -> RocketConfig.get().miningParticles, v -> RocketConfig.get().miningParticles = v),
            new Opt("rocketvisuals.opt.tools", () -> RocketConfig.get().toolAnimations, v -> RocketConfig.get().toolAnimations = v),
            new Opt("rocketvisuals.opt.enchant", () -> RocketConfig.get().enchantGlow, v -> RocketConfig.get().enchantGlow = v),
            new Opt("rocketvisuals.opt.mobs", () -> RocketConfig.get().mobAnimations, v -> RocketConfig.get().mobAnimations = v),
            new Opt("rocketvisuals.opt.rain", () -> RocketConfig.get().rainEffects, v -> RocketConfig.get().rainEffects = v),
            new Opt("rocketvisuals.opt.fire", () -> RocketConfig.get().fireEffects, v -> RocketConfig.get().fireEffects = v),
            new Opt("rocketvisuals.opt.health", () -> RocketConfig.get().healthIndicators, v -> RocketConfig.get().healthIndicators = v),
            new Opt("rocketvisuals.opt.inventory", () -> RocketConfig.get().inventoryUi, v -> RocketConfig.get().inventoryUi = v),
            new Opt("rocketvisuals.opt.xp", () -> RocketConfig.get().xpEffects, v -> RocketConfig.get().xpEffects = v),
            new Opt("rocketvisuals.opt.crit", () -> RocketConfig.get().critEffects, v -> RocketConfig.get().critEffects = v),
            new Opt("rocketvisuals.opt.atmosphere", () -> RocketConfig.get().atmosphere, v -> RocketConfig.get().atmosphere = v),
            new Opt("rocketvisuals.opt.sounds", () -> RocketConfig.get().soundEffects, v -> RocketConfig.get().soundEffects = v)
    );

    public RocketConfigScreen(Screen parent) {
        super(Component.translatable("rocketvisuals.screen.title"));
        this.parent = parent;
    }

    private static Component onOff(boolean on) {
        return on ? Component.translatable("rocketvisuals.on").withStyle(ChatFormatting.GREEN)
                  : Component.translatable("rocketvisuals.off").withStyle(ChatFormatting.RED);
    }

    private static MutableComponent label(String key, Component value) {
        return Component.translatable(key).append(": ").append(value);
    }

    private static Component toggleLabel(Opt o) {
        return label(o.key(), onOff(o.get().getAsBoolean()));
    }

    @Override
    protected void init() {
        int cw = 150, gap = 8;
        int left = this.width / 2 - cw - gap / 2;
        int right = this.width / 2 + gap / 2;
        int y0 = 40, rh = 24;

        if (page == 0) {
            for (int i = 0; i < toggles.size(); i++) {
                Opt o = toggles.get(i);
                int x = (i % 2 == 0) ? left : right;
                int y = y0 + (i / 2) * rh;
                this.addRenderableWidget(Button.builder(toggleLabel(o), b -> {
                    o.set().accept(!o.get().getAsBoolean());
                    RocketConfig.save();
                    b.setMessage(toggleLabel(o));
                }).bounds(x, y, cw, 20).build());
            }
        } else {
            RocketConfig c = RocketConfig.get();
            this.addRenderableWidget(Button.builder(label("rocketvisuals.opt.intensity", Component.literal(c.particleIntensity + "%")), b -> {
                RocketConfig.get().particleIntensity = RocketConfig.nextIntensity(RocketConfig.get().particleIntensity);
                RocketConfig.save();
                b.setMessage(label("rocketvisuals.opt.intensity", Component.literal(RocketConfig.get().particleIntensity + "%")));
            }).bounds(left, y0, cw, 20).build());

            this.addRenderableWidget(Button.builder(label("rocketvisuals.opt.quality", Component.translatable("rocketvisuals.quality." + c.quality)), b -> {
                RocketConfig.get().quality = (RocketConfig.get().quality + 1) % 4;
                RocketConfig.save();
                b.setMessage(label("rocketvisuals.opt.quality", Component.translatable("rocketvisuals.quality." + RocketConfig.get().quality)));
            }).bounds(right, y0, cw, 20).build());

            this.addRenderableWidget(Button.builder(label("rocketvisuals.opt.lowload", onOff(c.lowLoadMode)), b -> {
                RocketConfig.get().lowLoadMode = !RocketConfig.get().lowLoadMode;
                RocketConfig.save();
                b.setMessage(label("rocketvisuals.opt.lowload", onOff(RocketConfig.get().lowLoadMode)));
            }).bounds(left, y0 + rh, cw, 20).build());

            this.addRenderableWidget(Button.builder(label("rocketvisuals.opt.volume", Component.literal(c.soundVolume + "%")), b -> {
                RocketConfig.get().soundVolume = RocketConfig.nextVolume(RocketConfig.get().soundVolume);
                RocketConfig.save();
                b.setMessage(label("rocketvisuals.opt.volume", Component.literal(RocketConfig.get().soundVolume + "%")));
            }).bounds(right, y0 + rh, cw, 20).build());

            this.addRenderableWidget(Button.builder(Component.translatable("rocketvisuals.opt.keybind"),
                    b -> ScreenUtil.openKeyBinds(this)).bounds(left, y0 + rh * 2, cw * 2 + gap, 20).build());

            this.addRenderableWidget(Button.builder(Component.translatable("rocketvisuals.opt.enable_all"), b -> {
                RocketConfig.setAllEffects(true);
                this.rebuildWidgets();
            }).bounds(left, y0 + rh * 3, cw, 20).build());

            this.addRenderableWidget(Button.builder(Component.translatable("rocketvisuals.opt.disable_all"), b -> {
                RocketConfig.setAllEffects(false);
                this.rebuildWidgets();
            }).bounds(right, y0 + rh * 3, cw, 20).build());

            this.addRenderableWidget(Button.builder(Component.translatable("rocketvisuals.opt.reset"), b -> {
                RocketConfig.resetToDefaults();
                this.rebuildWidgets();
            }).bounds(left, y0 + rh * 4, cw * 2 + gap, 20).build());
        }

        int by = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("rocketvisuals.prev"), b -> {
            page = (page + PAGES - 1) % PAGES;
            this.rebuildWidgets();
        }).bounds(this.width / 2 - 154, by, 90, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("rocketvisuals.done"), b -> this.onClose())
                .bounds(this.width / 2 - 60, by, 120, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("rocketvisuals.next"), b -> {
            page = (page + 1) % PAGES;
            this.rebuildWidgets();
        }).bounds(this.width / 2 + 64, by, 90, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Minecraft mc = Minecraft.getInstance();
        float hue = (System.currentTimeMillis() % 6000L) / 6000f;
        int rgb = HudRenderer.hue(hue);
        String title = "ROCKET VISUALS";
        g.text(mc.font, title, this.width / 2 - mc.font.width(title) / 2, 14, (0xFF << 24) | rgb, true);
        String pageText = Component.translatable("rocketvisuals.page", page + 1, PAGES).getString();
        g.text(mc.font, pageText, this.width / 2 - mc.font.width(pageText) / 2, 26, 0xFFAAAAAA, true);
        String hint = Component.translatable("rocketvisuals.hint.key").getString();
        g.text(mc.font, hint, this.width / 2 - mc.font.width(hint) / 2, this.height - 42, 0xFF8888AA, true);
    }

    @Override
    public void onClose() {
        RocketConfig.save();
        ScreenUtil.open(parent);
    }
}
