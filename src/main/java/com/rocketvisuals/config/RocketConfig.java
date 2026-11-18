package com.rocketvisuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Client-side settings, persisted to config/rocketvisuals.json. */
public final class RocketConfig {
    public static final int[] INTENSITY_STEPS = {25, 50, 75, 100, 150, 200};
    public static final int[] VOLUME_STEPS = {0, 25, 50, 75, 100};

    // Effect toggles
    public boolean miningParticles = true;
    public boolean toolAnimations = true;
    public boolean enchantGlow = true;
    public boolean mobAnimations = true;
    public boolean rainEffects = true;
    public boolean fireEffects = true;
    public boolean healthIndicators = true;
    public boolean inventoryUi = true;
    public boolean xpEffects = true;
    public boolean critEffects = true;
    public boolean atmosphere = true;
    public boolean soundEffects = true;

    // Tuning
    public int particleIntensity = 100; // percent, 25..200
    public int quality = 2;             // 0 low, 1 medium, 2 high, 3 ultra
    public boolean lowLoadMode = false;
    public int soundVolume = 100;       // percent, 0..100

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static RocketConfig instance = new RocketConfig();

    public static RocketConfig get() {
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("rocketvisuals.json");
    }

    public static void load() {
        Path f = file();
        if (Files.isRegularFile(f)) {
            try {
                RocketConfig loaded = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), RocketConfig.class);
                if (loaded != null) instance = loaded;
            } catch (IOException | RuntimeException e) {
                System.err.println("[ROCKET VISUALS] Could not read config, using defaults: " + e);
                instance = new RocketConfig();
            }
        }
        instance.sanitize();
        save();
    }

    public static void save() {
        instance.sanitize();
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[ROCKET VISUALS] Could not save config: " + e);
        }
    }

    public static void resetToDefaults() {
        instance = new RocketConfig();
        save();
    }

    public static void setAllEffects(boolean value) {
        RocketConfig c = instance;
        c.miningParticles = c.toolAnimations = c.enchantGlow = c.mobAnimations = value;
        c.rainEffects = c.fireEffects = c.healthIndicators = c.inventoryUi = value;
        c.xpEffects = c.critEffects = c.atmosphere = c.soundEffects = value;
        save();
    }

    private void sanitize() {
        particleIntensity = Math.max(10, Math.min(300, particleIntensity));
        quality = Math.max(0, Math.min(3, quality));
        soundVolume = Math.max(0, Math.min(100, soundVolume));
    }

    public static int nextIntensity(int current) {
        for (int step : INTENSITY_STEPS) if (step > current) return step;
        return INTENSITY_STEPS[0];
    }

    public static int nextVolume(int current) {
        for (int step : VOLUME_STEPS) if (step > current) return step;
        return VOLUME_STEPS[0];
    }
}
