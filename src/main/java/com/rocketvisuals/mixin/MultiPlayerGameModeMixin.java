package com.rocketvisuals.mixin;

import com.rocketvisuals.fx.EffectsManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks the client-side block break to spawn extra particles. Purely cosmetic; never alters the result. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "destroyBlock", at = @At("HEAD"), require = 0)
    private void rocketvisuals$onDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        EffectsManager.onBlockBroken(pos);
    }
}
