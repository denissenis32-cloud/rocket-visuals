package com.rocketvisuals.mixin;

import com.rocketvisuals.gui.RocketConfigScreen;
import com.rocketvisuals.gui.ScreenUtil;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a ROCKET VISUALS shortcut button to Options > Controls (string target: no compile-time dependency on the package). */
@Mixin(targets = "net.minecraft.client.gui.screens.options.OptionsSubScreen")
public abstract class OptionsSubScreenMixin extends Screen {
    protected OptionsSubScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"), require = 0)
    private void rocketvisuals$addControlsButton(CallbackInfo ci) {
        String name = this.getClass().getSimpleName();
        if (!name.equals("ControlsScreen")) return;
        this.addRenderableWidget(Button.builder(Component.translatable("rocketvisuals.button.main_menu"),
                b -> ScreenUtil.open(new RocketConfigScreen((Screen) (Object) this)))
                .bounds(4, 4, 112, 20).build());
    }
}
