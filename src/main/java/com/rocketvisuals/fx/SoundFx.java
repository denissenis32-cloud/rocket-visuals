package com.rocketvisuals.fx;

import com.rocketvisuals.config.RocketConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

/** Layered vanilla sounds (client only, no new audio assets). */
public final class SoundFx {
    private SoundFx() {}

    public static void play(Object event, float pitch, float volume) {
        RocketConfig c = RocketConfig.get();
        if (!c.soundEffects || c.soundVolume <= 0) return;
        SoundEvent se;
        if (event instanceof Holder<?> h) se = (SoundEvent) h.value();
        else if (event instanceof SoundEvent s) se = s;
        else return;
        float v = volume * (c.soundVolume / 100f);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(se, pitch, v));
    }
}
