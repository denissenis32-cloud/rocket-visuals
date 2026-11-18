package com.rocketvisuals.mixin;

import com.rocketvisuals.gui.RocketConfigScreen;
import com.rocketvisuals.gui.ScreenUtil;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the ROCKET VISUALS button to the main menu. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"), require = 0)
    private void rocketvisuals$addButton(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.translatable("rocketvisuals.button.main_menu"),
                b -> ScreenUtil.open(new RocketConfigScreen((Screen) (Object) this)))
                .bounds(4, 4, 112, 20).build());
    }
}
