package com.rocketvisuals;

import com.mojang.blaze3d.platform.InputConstants;
import com.rocketvisuals.config.RocketConfig;
import com.rocketvisuals.fx.EffectsManager;
import com.rocketvisuals.fx.HudRenderer;
import com.rocketvisuals.gui.RocketConfigScreen;
import com.rocketvisuals.gui.ScreenUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

/** ROCKET VISUALS - client-only entrypoint. */
public final class RocketVisualsClient implements ClientModInitializer {
    public static final String MOD_ID = "rocketvisuals";
    public static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        RocketConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.rocketvisuals.open_menu",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RSHIFT,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openMenuKey.consumeClick()) {
                ScreenUtil.open(new RocketConfigScreen(null));
            }
            EffectsManager.tick(mc);
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "overlay"), HudRenderer::extract);

        // Fires on both logical sides in singleplayer; only react to the local client player.
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (player == Minecraft.getInstance().player) {
                EffectsManager.onAttack(player, entity);
            }
            return InteractionResult.PASS;
        });
    }
}
