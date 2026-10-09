package com.josebieco.dirtcheapgolems.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * COMMON config (spec §8). Every balancing number of the mod lives here.
 * Getters follow the convention {@code <golem><Key>()}; the "general" section has no prefix.
 */
public final class GolemConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // general
    private static final ForgeConfigSpec.IntValue PASSIVE_DECAY_TICKS;
    private static final ForgeConfigSpec.IntValue HOME_RADIUS;
    // dirt
    private static final ForgeConfigSpec.IntValue DIRT_HARVEST_COST;
    private static final ForgeConfigSpec.IntValue DIRT_HARVEST_SCAN_INTERVAL;
    private static final ForgeConfigSpec.IntValue DIRT_HARVEST_SCAN_RADIUS;
    private static final ForgeConfigSpec.IntValue DIRT_RAIN_DECAY_PER_SECOND;
    private static final ForgeConfigSpec.DoubleValue DIRT_DROP_CHANCE;
    // sand
    private static final ForgeConfigSpec.IntValue SAND_TURRET_RANGE;
    private static final ForgeConfigSpec.IntValue SAND_TURRET_COOLDOWN;
    private static final ForgeConfigSpec.IntValue SAND_SHOT_COST;
    private static final ForgeConfigSpec.DoubleValue SAND_DAMAGE;
    private static final ForgeConfigSpec.IntValue SAND_SLOWNESS_TICKS;
    private static final ForgeConfigSpec.IntValue SAND_IN_EYES_TICKS;
    private static final ForgeConfigSpec.DoubleValue SAND_DROP_CHANCE;
    // netherrack
    private static final ForgeConfigSpec.IntValue NETHERRACK_LIGHT_LEVEL;
    private static final ForgeConfigSpec.IntValue NETHERRACK_KAMIKAZE_RANGE;
    private static final ForgeConfigSpec.IntValue NETHERRACK_FUSE_TICKS;
    private static final ForgeConfigSpec.DoubleValue NETHERRACK_BLAST_RADIUS;
    private static final ForgeConfigSpec.DoubleValue NETHERRACK_BLAST_DAMAGE;
    private static final ForgeConfigSpec.IntValue NETHERRACK_BURN_SECONDS;
    private static final ForgeConfigSpec.IntValue NETHERRACK_BLAST_COST;
    private static final ForgeConfigSpec.BooleanValue NETHERRACK_BLAST_HURTS_PLAYERS;
    private static final ForgeConfigSpec.DoubleValue NETHERRACK_DROP_CHANCE;

    static {
        BUILDER.push("general");
        PASSIVE_DECAY_TICKS = BUILDER.comment("Ticks per -1 passive integrity").defineInRange("passiveDecayTicks", 600, 20, 72000);
        HOME_RADIUS = BUILDER.comment("Radius (blocks) around home a golem is restricted to").defineInRange("homeRadius", 16, 1, 128);
        BUILDER.pop();

        BUILDER.push("dirt");
        DIRT_HARVEST_COST = BUILDER.comment("Integrity lost per harvest").defineInRange("harvestCost", 1, 0, 100);
        DIRT_HARVEST_SCAN_INTERVAL = BUILDER.comment("Ticks between crop scans").defineInRange("harvestScanInterval", 40, 1, 1200);
        DIRT_HARVEST_SCAN_RADIUS = BUILDER.comment("Crop scan radius from the golem").defineInRange("harvestScanRadius", 8, 1, 32);
        DIRT_RAIN_DECAY_PER_SECOND = BUILDER.comment("Integrity lost per second in rain (water = 2x)").defineInRange("rainDecayPerSecond", 2, 0, 100);
        DIRT_DROP_CHANCE = BUILDER.comment("Chance to drop dirt when crumbling").defineInRange("dropChance", 0.5, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("sand");
        SAND_TURRET_RANGE = BUILDER.comment("Turret range (blocks)").defineInRange("turretRange", 12, 1, 64);
        SAND_TURRET_COOLDOWN = BUILDER.comment("Ticks between shots").defineInRange("turretCooldown", 40, 1, 1200);
        SAND_SHOT_COST = BUILDER.comment("Integrity lost per shot").defineInRange("shotCost", 2, 0, 100);
        SAND_DAMAGE = BUILDER.comment("Sand blast damage").defineInRange("sandDamage", 1.0, 0.0, 100.0);
        SAND_SLOWNESS_TICKS = BUILDER.comment("Slowness II duration (ticks)").defineInRange("slownessTicks", 80, 0, 12000);
        SAND_IN_EYES_TICKS = BUILDER.comment("Sand in Eyes duration (ticks)").defineInRange("sandInEyesTicks", 60, 0, 12000);
        SAND_DROP_CHANCE = BUILDER.comment("Chance to drop sand when crumbling").defineInRange("dropChance", 0.5, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("netherrack");
        NETHERRACK_LIGHT_LEVEL = BUILDER.comment("Walking light level").defineInRange("lightLevel", 12, 0, 15);
        NETHERRACK_KAMIKAZE_RANGE = BUILDER.comment("Hostile detection range").defineInRange("kamikazeRange", 10, 1, 64);
        NETHERRACK_FUSE_TICKS = BUILDER.comment("Fuse duration (ticks)").defineInRange("fuseTicks", 30, 1, 1200);
        NETHERRACK_BLAST_RADIUS = BUILDER.comment("Thermal blast radius").defineInRange("blastRadius", 3.0, 0.5, 16.0);
        NETHERRACK_BLAST_DAMAGE = BUILDER.comment("Thermal blast damage").defineInRange("blastDamage", 4.0, 0.0, 100.0);
        NETHERRACK_BURN_SECONDS = BUILDER.comment("Seconds targets burn").defineInRange("burnSeconds", 5, 0, 60);
        NETHERRACK_BLAST_COST = BUILDER.comment("Integrity lost per blast").defineInRange("blastCost", 34, 0, 100);
        NETHERRACK_BLAST_HURTS_PLAYERS = BUILDER.comment("Whether the thermal blast hurts players").define("blastHurtsPlayers", false);
        NETHERRACK_DROP_CHANCE = BUILDER.comment("Chance to drop charcoal when crumbling").defineInRange("dropChance", 1.0, 0.0, 1.0);
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private GolemConfig() {}

    public static int passiveDecayTicks() { return PASSIVE_DECAY_TICKS.get(); }
    public static int homeRadius() { return HOME_RADIUS.get(); }

    public static int dirtHarvestCost() { return DIRT_HARVEST_COST.get(); }
    public static int dirtHarvestScanInterval() { return DIRT_HARVEST_SCAN_INTERVAL.get(); }
    public static int dirtHarvestScanRadius() { return DIRT_HARVEST_SCAN_RADIUS.get(); }
    public static int dirtRainDecayPerSecond() { return DIRT_RAIN_DECAY_PER_SECOND.get(); }
    public static double dirtDropChance() { return DIRT_DROP_CHANCE.get(); }

    public static int sandTurretRange() { return SAND_TURRET_RANGE.get(); }
    public static int sandTurretCooldown() { return SAND_TURRET_COOLDOWN.get(); }
    public static int sandShotCost() { return SAND_SHOT_COST.get(); }
    public static double sandDamage() { return SAND_DAMAGE.get(); }
    public static int sandSlownessTicks() { return SAND_SLOWNESS_TICKS.get(); }
    public static int sandInEyesTicks() { return SAND_IN_EYES_TICKS.get(); }
    public static double sandDropChance() { return SAND_DROP_CHANCE.get(); }

    public static int netherrackLightLevel() { return NETHERRACK_LIGHT_LEVEL.get(); }
    public static int netherrackKamikazeRange() { return NETHERRACK_KAMIKAZE_RANGE.get(); }
    public static int netherrackFuseTicks() { return NETHERRACK_FUSE_TICKS.get(); }
    public static double netherrackBlastRadius() { return NETHERRACK_BLAST_RADIUS.get(); }
    public static double netherrackBlastDamage() { return NETHERRACK_BLAST_DAMAGE.get(); }
    public static int netherrackBurnSeconds() { return NETHERRACK_BURN_SECONDS.get(); }
    public static int netherrackBlastCost() { return NETHERRACK_BLAST_COST.get(); }
    public static boolean netherrackBlastHurtsPlayers() { return NETHERRACK_BLAST_HURTS_PLAYERS.get(); }
    public static double netherrackDropChance() { return NETHERRACK_DROP_CHANCE.get(); }
}
