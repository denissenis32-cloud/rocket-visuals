package com.rocketvisuals.fx;

import com.rocketvisuals.config.RocketConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * All world effects. Everything here is client-side: it only spawns local particles, plays local sounds
 * and sets a few HUD timers. Nothing is sent to the server and no game state is modified.
 */
public final class EffectsManager {
    // HUD-visible state
    public static int flashTicks;
    public static int xpPopupTicks;
    public static int xpPopupAmount;

    private static int tick;
    private static boolean wasAttackDown;
    private static boolean xpInit;
    private static int lastXp;
    private static int lastLevel;
    private static final Map<Integer, Integer> hurtMap = new HashMap<>();
    private static final Set<Integer> dying = new HashSet<>();
    private static final Set<Integer> bolts = new HashSet<>();
    private static final Map<String, Integer> failures = new HashMap<>();

    private static final ParticleOptions FIREFLY = Compat.particle("FIREFLY", ParticleTypes.END_ROD);

    private EffectsManager() {}

    public static void reset() {
        tick = 0;
        xpInit = false;
        flashTicks = 0;
        xpPopupTicks = 0;
        hurtMap.clear();
        dying.clear();
        bolts.clear();
    }

    private static void run(String name, Runnable r) {
        if (failures.getOrDefault(name, 0) >= 3) return; // disable a feature that keeps failing
        try {
            r.run();
        } catch (RuntimeException | LinkageError e) {
            int n = failures.merge(name, 1, Integer::sum);
            System.err.println("[ROCKET VISUALS] effect '" + name + "' failed (" + n + "/3): " + e);
        }
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        LocalPlayer p = mc.player;
        if (level == null || p == null) {
            reset();
            return;
        }
        if (mc.isPaused()) return;

        RocketConfig c = RocketConfig.get();
        if (flashTicks > 0) flashTicks--;
        if (xpPopupTicks > 0) xpPopupTicks--;
        Fx.beginTick(mc);
        tick++;
        int every = c.lowLoadMode ? 4 : 2;

        if (c.miningParticles) run("mining", () -> mining(mc, level));
        run("swing", () -> toolSwing(mc, level, p, c));
        if (c.xpEffects) run("xp", () -> xp(level, p)); else xpInit = false;
        if (c.rainEffects) run("rain", () -> rain(level, p, c));
        if (tick % every == 0 && (c.mobAnimations || c.fireEffects || c.rainEffects || c.enchantGlow)) {
            run("entities", () -> scanEntities(level, p, c));
        }
        if (c.enchantGlow && tick % (every * 3) == 0) run("enchant", () -> enchantGlow(level, p));
        if (c.atmosphere && tick % (every * 2) == 0) run("atmosphere", () -> atmosphere(level, p, c));
    }

    // ---------------------------------------------------------------- mining

    private static void mining(Minecraft mc, ClientLevel level) {
        if (mc.gameMode == null || !mc.gameMode.isDestroying() || tick % 3 != 0) return;
        if (!(mc.hitResult instanceof BlockHitResult hit)) return;
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (state.isAir()) return;
        Vec3 loc = hit.getLocation();
        double nx = hit.getDirection().getStepX(), ny = hit.getDirection().getStepY(), nz = hit.getDirection().getStepZ();
        int n = Fx.count(2f);
        for (int i = 0; i < n; i++) {
            Fx.spawn(level, new BlockParticleOption(ParticleTypes.BLOCK, state),
                    loc.x + nx * 0.05 + Fx.rnd2() * 0.2, loc.y + ny * 0.05 + Fx.rnd2() * 0.2, loc.z + nz * 0.05 + Fx.rnd2() * 0.2,
                    nx * 0.08 + Fx.rnd2() * 0.04, ny * 0.08 + Fx.rnd2() * 0.04, nz * 0.08 + Fx.rnd2() * 0.04);
        }
        if (RocketConfig.get().quality >= 2 && Fx.rnd() < 0.25) {
            Fx.spawn(level, ParticleTypes.ELECTRIC_SPARK, loc.x + nx * 0.1, loc.y + ny * 0.1, loc.z + nz * 0.1,
                    nx * 0.1, ny * 0.1, nz * 0.1);
        }
    }

    /** Called by MultiPlayerGameModeMixin right before the block is destroyed. */
    public static void onBlockBroken(BlockPos pos) {
        RocketConfig c = RocketConfig.get();
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (!c.miningParticles || level == null) return;
        run("blockbreak", () -> {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) return;
            int n = Fx.count(8f);
            for (int i = 0; i < n; i++) {
                Fx.spawn(level, new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + Fx.rnd(), pos.getY() + Fx.rnd(), pos.getZ() + Fx.rnd(),
                        Fx.rnd2() * 0.15, 0.08 + Fx.rnd() * 0.12, Fx.rnd2() * 0.15);
            }
            int sparks = Fx.count(3f);
            for (int i = 0; i < sparks; i++) {
                Fx.spawn(level, ParticleTypes.ELECTRIC_SPARK, pos.getX() + Fx.rnd(), pos.getY() + Fx.rnd(), pos.getZ() + Fx.rnd(),
                        Fx.rnd2() * 0.2, Fx.rnd2() * 0.2, Fx.rnd2() * 0.2);
            }
            if (c.quality >= 2 && Fx.rnd() < 0.5) {
                Fx.spawn(level, ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0.02, 0);
            }
            SoundFx.play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f + (float) Fx.rnd() * 0.5f, 0.12f);
        });
    }

    // ------------------------------------------------------------ tool swing

    private static void toolSwing(Minecraft mc, ClientLevel level, LocalPlayer p, RocketConfig c) {
        boolean down = mc.options.keyAttack.isDown();
        boolean rising = down && !wasAttackDown;
        wasAttackDown = down;
        if (!c.toolAnimations || !rising) return;

        ItemStack st = p.getMainHandItem();
        ParticleOptions type;
        boolean sword = st.is(ItemTags.SWORDS);
        if (sword) type = ParticleTypes.END_ROD;
        else if (st.is(ItemTags.AXES)) type = ParticleTypes.CRIT;
        else if (st.is(ItemTags.PICKAXES)) type = ParticleTypes.ELECTRIC_SPARK;
        else if (st.is(ItemTags.SHOVELS) || st.is(ItemTags.HOES)) type = ParticleTypes.POOF;
        else return;

        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();

        int n = Math.max(2, Fx.count(sword ? 12f : 7f));
        for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0.5 : i / (double) (n - 1);
            double ang = Mth.lerp(t, 0.95, -0.95);
            Vec3 dir = look.scale(Math.cos(ang)).add(right.scale(Math.sin(ang)));
            Vec3 pos = eye.add(dir.scale(1.5)).add(up.scale(0.3 - 0.55 * t));
            Vec3 vel = right.scale(-0.04).add(up.scale(-0.01));
            Fx.spawn(level, type, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z);
        }
    }

    // --------------------------------------------------------- enchant glow

    private static void enchantGlow(ClientLevel level, LocalPlayer self) {
        for (Player pl : level.players()) {
            if (pl.distanceToSqr(self) > 24 * 24) continue;
            ItemStack st = pl.getMainHandItem();
            if (st.isEmpty() || !st.isEnchanted()) continue;
            Vec3 look = pl.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0));
            right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 base = pl.getEyePosition().add(look.scale(0.7)).add(right.scale(0.35)).add(0, -0.35, 0);
            int n = Fx.count(2f);
            for (int i = 0; i < n; i++) {
                Fx.spawn(level, ParticleTypes.ENCHANT, base.x, base.y, base.z,
                        Fx.rnd2() * 0.6, Fx.rnd() * 0.6, Fx.rnd2() * 0.6);
            }
            if (RocketConfig.get().quality >= 2 && Fx.rnd() < 0.3) {
                Fx.spawn(level, ParticleTypes.END_ROD, base.x + Fx.rnd2() * 0.15, base.y + Fx.rnd2() * 0.15, base.z + Fx.rnd2() * 0.15,
                        0, 0.01, 0);
            }
        }
    }

    // ------------------------------------------------------- entity scanning

    private static void scanEntities(ClientLevel level, LocalPlayer p, RocketConfig c) {
        double maxD = c.lowLoadMode ? 20 : 36;
        double max2 = maxD * maxD;
        int processed = 0;
        Set<Integer> seen = new HashSet<>();

        for (Entity e : level.entitiesForRendering()) {
            if (processed >= 64) break;
            if (e.distanceToSqr(p) > max2) continue;
            processed++;
            int id = e.getId();
            seen.add(id);

            if (e instanceof LightningBolt) {
                if (c.rainEffects && bolts.add(id)) onLightning(level, p, e);
                continue;
            }
            if (e instanceof ItemEntity ie) {
                if (c.enchantGlow && ie.getItem().isEnchanted() && Fx.rnd() < 0.5) {
                    Fx.spawn(level, ParticleTypes.ENCHANT, e.getX(), e.getY() + 0.4, e.getZ(),
                            Fx.rnd2() * 0.5, Fx.rnd() * 0.5, Fx.rnd2() * 0.5);
                }
                continue;
            }
            if (c.fireEffects && e.isOnFire()) fire(level, e, c);
            if (c.mobAnimations && e instanceof LivingEntity le && le != p) mobEffects(level, le, id);
        }
        hurtMap.keySet().retainAll(seen);
        dying.retainAll(seen);
        bolts.retainAll(seen);
    }

    private static void fire(ClientLevel level, Entity e, RocketConfig c) {
        int n = Fx.count(2f);
        for (int i = 0; i < n; i++) {
            Fx.spawn(level, i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
                    e.getRandomX(0.6), e.getY() + Fx.rnd() * e.getBbHeight(), e.getRandomZ(0.6),
                    Fx.rnd2() * 0.01, 0.04 + Fx.rnd() * 0.03, Fx.rnd2() * 0.01);
        }
        if (c.quality >= 2 && Fx.rnd() < 0.35) {
            Fx.spawn(level, ParticleTypes.LAVA, e.getRandomX(0.5), e.getY() + e.getBbHeight() * 0.8, e.getRandomZ(0.5), 0, 0, 0);
        }
        if (Fx.rnd() < 0.2) {
            Fx.spawn(level, ParticleTypes.LARGE_SMOKE, e.getRandomX(0.5), e.getY() + e.getBbHeight() + 0.1, e.getRandomZ(0.5), 0, 0.03, 0);
        }
    }

    private static void mobEffects(ClientLevel level, LivingEntity le, int id) {
        Integer prev = hurtMap.put(id, le.hurtTime);
        double cy = le.getY(0.6);

        if (prev == null) {
            if (le.tickCount < 6) { // spawn "pop"
                int n = Fx.count(6f);
                for (int i = 0; i < n; i++) {
                    double a = Fx.rnd() * Math.PI * 2;
                    Fx.spawn(level, ParticleTypes.POOF, le.getX(), le.getY() + 0.1, le.getZ(),
                            Math.cos(a) * 0.08, 0.02, Math.sin(a) * 0.08);
                }
            }
        } else if (prev == 0 && le.hurtTime > 0 && le.deathTime == 0) { // hurt burst
            int n = Fx.count(2f);
            for (int i = 0; i < n; i++) {
                Fx.spawn(level, ParticleTypes.DAMAGE_INDICATOR, le.getRandomX(0.6), cy, le.getRandomZ(0.6),
                        Fx.rnd2() * 0.1, 0.1, Fx.rnd2() * 0.1);
            }
            int m = Fx.count(3f);
            for (int i = 0; i < m; i++) {
                Fx.spawn(level, ParticleTypes.ENCHANTED_HIT, le.getRandomX(0.6), cy, le.getRandomZ(0.6),
                        Fx.rnd2() * 0.15, 0.05 + Fx.rnd() * 0.1, Fx.rnd2() * 0.15);
            }
        }

        if (le.deathTime > 0 && dying.add(id)) { // death puff
            int n = Fx.count(14f);
            for (int i = 0; i < n; i++) {
                Fx.spawn(level, i % 3 == 0 ? ParticleTypes.SOUL : ParticleTypes.POOF,
                        le.getRandomX(0.8), le.getY() + Fx.rnd() * le.getBbHeight(), le.getRandomZ(0.8),
                        Fx.rnd2() * 0.06, 0.03 + Fx.rnd() * 0.05, Fx.rnd2() * 0.06);
            }
        }
    }

    private static void onLightning(ClientLevel level, LocalPlayer p, Entity bolt) {
        double d = Math.sqrt(bolt.distanceToSqr(p));
        if (d < 80) flashTicks = 8;
        int n = Fx.count(24f);
        for (int i = 0; i < n; i++) {
            double a = Fx.rnd() * Math.PI * 2, r = Fx.rnd() * 1.5;
            Fx.spawn(level, ParticleTypes.ELECTRIC_SPARK, bolt.getX() + Math.cos(a) * r, bolt.getY() + Fx.rnd() * 3, bolt.getZ() + Math.sin(a) * r,
                    Math.cos(a) * 0.3, 0.1 + Fx.rnd() * 0.2, Math.sin(a) * 0.3);
        }
        SoundFx.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, (float) Mth.clamp(0.6 - d / 200.0, 0.1, 0.6));
    }

    // ------------------------------------------------------------------ rain

    private static void rain(ClientLevel level, LocalPlayer p, RocketConfig c) {
        if (!level.isRaining()) return;
        int radius = c.lowLoadMode ? 7 : 12;
        int n = Fx.count(level.isThundering() ? 7f : 4f);
        for (int i = 0; i < n; i++) {
            int x = Mth.floor(p.getX() + Fx.rnd2() * radius);
            int z = Mth.floor(p.getZ() + Fx.rnd2() * radius);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
            if (!level.hasChunkAt(top) || Math.abs(top.getY() - p.getY()) > 12 || !level.isRainingAt(top)) continue;
            Fx.spawn(level, ParticleTypes.RAIN, x + Fx.rnd(), top.getY() + 0.05, z + Fx.rnd(), 0, 0, 0);
            if (c.quality >= 2 && Fx.rnd() < 0.15) {
                Fx.spawn(level, ParticleTypes.SPLASH, x + Fx.rnd(), top.getY() + 0.05, z + Fx.rnd(), 0, 0.1, 0);
            }
        }
    }

    // -------------------------------------------------------------------- xp

    private static void xp(ClientLevel level, LocalPlayer p) {
        int xp = p.totalExperience;
        int lvl = p.experienceLevel;
        if (!xpInit) {
            lastXp = xp;
            lastLevel = lvl;
            xpInit = true;
            return;
        }
        if (xp > lastXp) {
            int gain = xp - lastXp;
            xpPopupAmount = xpPopupTicks > 0 ? xpPopupAmount + gain : gain;
            xpPopupTicks = 40;
            int n = Fx.count(Math.min(14f, 3f + gain / 2f));
            for (int i = 0; i < n; i++) {
                Fx.spawn(level, ParticleTypes.HAPPY_VILLAGER, p.getX() + Fx.rnd2() * 0.5, p.getY() + Fx.rnd() * 1.8, p.getZ() + Fx.rnd2() * 0.5,
                        0, 0.05, 0);
            }
        }
        if (lvl > lastLevel) {
            int n = Fx.count(26f);
            for (int i = 0; i < n; i++) {
                double a = i / (double) Math.max(1, n) * Math.PI * 2;
                Fx.spawn(level, i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.FIREWORK,
                        p.getX() + Math.cos(a) * 0.8, p.getY() + 0.2, p.getZ() + Math.sin(a) * 0.8,
                        Math.cos(a) * 0.03, 0.12, Math.sin(a) * 0.03);
            }
            SoundFx.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6f, 0.5f);
        }
        lastXp = xp;
        lastLevel = lvl;
    }

    // ------------------------------------------------------------------ crit

    /** Called from the client attack callback for the local player. */
    public static void onAttack(Player player, Entity target) {
        RocketConfig c = RocketConfig.get();
        ClientLevel level = Minecraft.getInstance().level;
        if (!c.critEffects || level == null || !(target instanceof LivingEntity)) return;
        run("crit", () -> {
            boolean crit = player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable()
                    && !player.isInWater() && !player.isPassenger() && !player.isSprinting()
                    && player.getAttackStrengthScale(0.5F) > 0.9F;
            if (!crit) return;
            double y = target.getY(0.6);
            int n = Fx.count(14f);
            for (int i = 0; i < n; i++) {
                double a = Fx.rnd() * Math.PI * 2, up = Fx.rnd2() * 0.2;
                Fx.spawn(level, ParticleTypes.CRIT, target.getX(), y, target.getZ(),
                        Math.cos(a) * 0.35, 0.1 + up, Math.sin(a) * 0.35);
            }
            int m = Fx.count(8f);
            for (int i = 0; i < m; i++) {
                Fx.spawn(level, ParticleTypes.ENCHANTED_HIT, target.getRandomX(0.8), y, target.getRandomZ(0.8),
                        Fx.rnd2() * 0.3, 0.1 + Fx.rnd() * 0.2, Fx.rnd2() * 0.3);
            }
            if (c.quality >= 2) {
                int k = Fx.count(5f);
                for (int i = 0; i < k; i++) {
                    Fx.spawn(level, ParticleTypes.END_ROD, target.getX(), y, target.getZ(),
                            Fx.rnd2() * 0.2, 0.05 + Fx.rnd() * 0.15, Fx.rnd2() * 0.2);
                }
            }
            SoundFx.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.9f, 0.3f);
        });
    }

    // ------------------------------------------------------------ atmosphere

    private static void atmosphere(ClientLevel level, LocalPlayer p, RocketConfig c) {
        int n = Fx.count(c.lowLoadMode ? 0.6f : 1.6f);
        boolean night = Compat.isNight(level);
        boolean raining = level.isRaining();
        boolean end = level.dimension() == Level.END;
        boolean nether = level.dimension() == Level.NETHER;
        if (nether) return; // the Nether already has plenty of vanilla ambience

        for (int i = 0; i < n; i++) {
            double x = p.getX() + Fx.rnd2() * 10, y = p.getY() + Fx.rnd() * 5 - 1, z = p.getZ() + Fx.rnd2() * 10;
            BlockPos bp = BlockPos.containing(x, y, z);
            if (!level.getBlockState(bp).isAir()) continue;
            boolean sky = level.canSeeSky(bp);
            if (end) {
                Fx.spawn(level, ParticleTypes.PORTAL, x, y, z, Fx.rnd2() * 0.2, Fx.rnd2() * 0.1, Fx.rnd2() * 0.2);
            } else if (sky && night && !raining) {
                Fx.spawn(level, FIREFLY, x, y, z, Fx.rnd2() * 0.01, Fx.rnd2() * 0.01, Fx.rnd2() * 0.01);
            } else if (sky && !night && !raining) {
                Fx.spawn(level, ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 0.01, -0.005, 0.01);
            } else if (!sky && p.getY() < 50) {
                Fx.spawn(level, ParticleTypes.WHITE_ASH, x, y, z, Fx.rnd2() * 0.01, -0.005, Fx.rnd2() * 0.01);
            }
        }
    }
}
