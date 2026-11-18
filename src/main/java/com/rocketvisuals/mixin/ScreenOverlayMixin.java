package com.rocketvisuals.mixin;

import com.rocketvisuals.fx.InventoryOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the ROCKET VISUALS backdrop behind inventory / container screens. */
@Mixin(Screen.class)
public abstract class ScreenOverlayMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
    private void rocketvisuals$overlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object) this instanceof AbstractContainerScreen<?>) {
            InventoryOverlay.render(graphics, (Screen) (Object) this);
        }
    }
}
