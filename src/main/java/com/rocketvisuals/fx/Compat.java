package com.rocketvisuals.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.lang.reflect.Method;

/** Small reflection helpers so a renamed vanilla member degrades gracefully instead of breaking. */
final class Compat {
    private static Method nightMethod;
    private static Method timeMethod;
    private static boolean probed;

    private Compat() {}

    private static void probe(ClientLevel level) {
        if (probed) return;
        probed = true;
        for (String n : new String[]{"isDarkOutside", "isNight"}) {
            try {
                Method m = level.getClass().getMethod(n);
                if (m.getReturnType() == boolean.class) { nightMethod = m; return; }
            } catch (ReflectiveOperationException ignored) { }
        }
        for (String n : new String[]{"getDayTime", "getOverworldClockTime", "getGameTime"}) {
            try {
                Method m = level.getClass().getMethod(n);
                if (m.getReturnType() == long.class) { timeMethod = m; return; }
            } catch (ReflectiveOperationException ignored) { }
        }
    }

    static boolean isNight(ClientLevel level) {
        probe(level);
        try {
            if (nightMethod != null) return (boolean) nightMethod.invoke(level);
            if (timeMethod != null) {
                long t = Math.floorMod((long) timeMethod.invoke(level), 24000L);
                return t >= 13000L && t <= 23000L;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
        return false;
    }

    /** Looks up a ParticleTypes constant by name, with a fallback if it does not exist in this version. */
    static ParticleOptions particle(String field, ParticleOptions fallback) {
        try {
            Object o = ParticleTypes.class.getField(field).get(null);
            if (o instanceof ParticleOptions po) return po;
        } catch (ReflectiveOperationException ignored) { }
        return fallback;
    }
}
