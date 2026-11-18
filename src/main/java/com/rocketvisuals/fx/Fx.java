package com.rocketvisuals.fx;

import com.rocketvisuals.config.RocketConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;

import java.util.concurrent.ThreadLocalRandom;

/** Particle budget + scaling shared by all effects. Keeps FPS cost bounded. */
public final class Fx {
    private static int budget;
    private static boolean particlesOff;

    private Fx() {}

    public static double rnd() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /** Random value in [-1, 1]. */
    public static double rnd2() {
        return rnd() * 2.0 - 1.0;
    }

    public static float qualityMul(RocketConfig c) {
        return switch (c.quality) {
            case 0 -> 0.5f;
            case 1 -> 0.8f;
            case 3 -> 1.5f;
            default -> 1.0f;
        };
    }

    static void beginTick(Minecraft mc) {
        RocketConfig c = RocketConfig.get();
        budget = (int) ((c.lowLoadMode ? 40 : 140) * qualityMul(c));
        String status = "ALL";
        try {
            status = mc.options.particles().get().name();
        } catch (RuntimeException | LinkageError ignored) { }
        particlesOff = "MINIMAL".equals(status);
        if ("DECREASED".equals(status)) budget /= 2;
    }

    /** Stochastic rounding of base * intensity * quality * (low-load factor). */
    public static int count(float base) {
        RocketConfig c = RocketConfig.get();
        float v = base * (c.particleIntensity / 100f) * qualityMul(c) * (c.lowLoadMode ? 0.35f : 1f);
        int n = (int) v;
        if (rnd() < v - n) n++;
        return n;
    }

    public static void spawn(ClientLevel level, ParticleOptions type, double x, double y, double z,
                             double dx, double dy, double dz) {
        if (particlesOff || budget <= 0) return;
        budget--;
        level.addParticle(type, x, y, z, dx, dy, dz);
    }
}
