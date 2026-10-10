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

    // end_stone
    private static final ForgeConfigSpec.IntValue END_STONE_LINK_RANGE;
    private static final ForgeConfigSpec.IntValue END_STONE_ITEMS_PER_INTEGRITY;
    private static final ForgeConfigSpec.DoubleValue END_STONE_PICKUP_RANGE;
    private static final ForgeConfigSpec.DoubleValue END_STONE_FLY_SPEED;
    private static final ForgeConfigSpec.DoubleValue END_STONE_DROP_CHANCE;

    // gravel
    private static final ForgeConfigSpec.IntValue GRAVEL_ARTILLERY_RANGE;
    private static final ForgeConfigSpec.IntValue GRAVEL_ARTILLERY_COOLDOWN;
    private static final ForgeConfigSpec.IntValue GRAVEL_SHOT_COST;
    private static final ForgeConfigSpec.DoubleValue GRAVEL_SHARD_DAMAGE;
    private static final ForgeConfigSpec.IntValue GRAVEL_SLOWNESS_TICKS;
    private static final ForgeConfigSpec.DoubleValue GRAVEL_LEAP_TRIGGER_RANGE;
    private static final ForgeConfigSpec.IntValue GRAVEL_LEAP_BELOW_HORIZONTAL;
    private static final ForgeConfigSpec.IntValue GRAVEL_LEAP_BELOW_DEPTH;
    private static final ForgeConfigSpec.DoubleValue GRAVEL_LEAP_IMPACT_DAMAGE;
    private static final ForgeConfigSpec.IntValue GRAVEL_LEAP_BLOCKS;
    private static final ForgeConfigSpec.BooleanValue GRAVEL_LEAP_PLACES_BLOCKS;
    private static final ForgeConfigSpec.DoubleValue GRAVEL_DROP_CHANCE;
    // basalt
    private static final ForgeConfigSpec.DoubleValue BASALT_FOLLOW_DISTANCE;
    private static final ForgeConfigSpec.IntValue BASALT_MAX_FOLLOW_DISTANCE;
    private static final ForgeConfigSpec.IntValue BASALT_THREAT_RANGE;
    private static final ForgeConfigSpec.DoubleValue BASALT_MAGNET_RADIUS;
    private static final ForgeConfigSpec.IntValue BASALT_ARROW_COST;
    private static final ForgeConfigSpec.IntValue BASALT_SMALL_FIREBALL_COST;
    private static final ForgeConfigSpec.IntValue BASALT_LARGE_FIREBALL_COST;
    private static final ForgeConfigSpec.IntValue BASALT_OTHER_PROJECTILE_COST;
    private static final ForgeConfigSpec.IntValue BASALT_MELEE_HIT_COST;
    private static final ForgeConfigSpec.DoubleValue BASALT_DROP_CHANCE;

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

        BUILDER.push("end_stone");
        END_STONE_LINK_RANGE = BUILDER.comment("Max distance (blocks) between the linked container and home").defineInRange("linkRange", 32, 1, 128);
        END_STONE_ITEMS_PER_INTEGRITY = BUILDER.comment("Items teleported per -1 integrity").defineInRange("itemsPerIntegrity", 4, 1, 64);
        END_STONE_PICKUP_RANGE = BUILDER.comment("Distance (blocks) at which an item is teleported").defineInRange("pickupRange", 1.5, 0.5, 8.0);
        END_STONE_FLY_SPEED = BUILDER.comment("Flight speed").defineInRange("flySpeed", 0.4, 0.05, 2.0);
        END_STONE_DROP_CHANCE = BUILDER.comment("Chance to drop end stone when crumbling").defineInRange("dropChance", 0.5, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("gravel");
        GRAVEL_ARTILLERY_RANGE = BUILDER.comment("Firing range (blocks)").defineInRange("artilleryRange", 16, 1, 64);
        GRAVEL_ARTILLERY_COOLDOWN = BUILDER.comment("Ticks between shots").defineInRange("artilleryCooldown", 60, 1, 1200);
        GRAVEL_SHOT_COST = BUILDER.comment("Integrity lost per shot").defineInRange("shotCost", 3, 0, 100);
        GRAVEL_SHARD_DAMAGE = BUILDER.comment("Flint shard damage").defineInRange("shardDamage", 3.0, 0.0, 100.0);
        GRAVEL_SLOWNESS_TICKS = BUILDER.comment("Slowness I duration (ticks)").defineInRange("slownessTicks", 60, 0, 12000);
        GRAVEL_LEAP_TRIGGER_RANGE = BUILDER.comment("Distance that triggers the leap").defineInRange("leapTriggerRange", 2.5, 0.5, 8.0);
        GRAVEL_LEAP_BELOW_HORIZONTAL = BUILDER.comment("Horizontal reach of the downward leap").defineInRange("leapBelowHorizontal", 2, 0, 8);
        GRAVEL_LEAP_BELOW_DEPTH = BUILDER.comment("Max depth of the downward leap").defineInRange("leapBelowDepth", 6, 1, 32);
        GRAVEL_LEAP_IMPACT_DAMAGE = BUILDER.comment("Leap impact damage").defineInRange("leapImpactDamage", 4.0, 0.0, 100.0);
        GRAVEL_LEAP_BLOCKS = BUILDER.comment("Gravel blocks spawned by the collapse").defineInRange("leapBlocks", 4, 1, 9);
        GRAVEL_LEAP_PLACES_BLOCKS = BUILDER.comment("If false, the collapse drops gravel items instead of blocks").define("leapPlacesBlocks", true);
        GRAVEL_DROP_CHANCE = BUILDER.comment("Chance to drop gravel when crumbling normally").defineInRange("dropChance", 0.5, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("basalt");
        BASALT_FOLLOW_DISTANCE = BUILDER.comment("Distance kept from the owner while following").defineInRange("followDistance", 2.5, 1.0, 8.0);
        BASALT_MAX_FOLLOW_DISTANCE = BUILDER.comment("Beyond this distance from the owner the golem stops and waits").defineInRange("maxFollowDistance", 24, 4, 128);
        BASALT_THREAT_RANGE = BUILDER.comment("Range to detect ranged attackers").defineInRange("threatRange", 16, 1, 64);
        BASALT_MAGNET_RADIUS = BUILDER.comment("Radius of the projectile magnet").defineInRange("magnetRadius", 2.5, 0.5, 8.0);
        BASALT_ARROW_COST = BUILDER.comment("Integrity lost per absorbed arrow or trident").defineInRange("arrowCost", 4, 0, 100);
        BASALT_SMALL_FIREBALL_COST = BUILDER.comment("Integrity lost per absorbed small fireball").defineInRange("smallFireballCost", 6, 0, 100);
        BASALT_LARGE_FIREBALL_COST = BUILDER.comment("Integrity lost per absorbed ghast fireball").defineInRange("largeFireballCost", 15, 0, 100);
        BASALT_OTHER_PROJECTILE_COST = BUILDER.comment("Integrity lost per other absorbed hostile projectile").defineInRange("otherProjectileCost", 4, 0, 100);
        BASALT_MELEE_HIT_COST = BUILDER.comment("Integrity lost per melee hit received").defineInRange("meleeHitCost", 2, 0, 100);
        BASALT_DROP_CHANCE = BUILDER.comment("Chance to drop the source block when crumbling").defineInRange("dropChance", 0.5, 0.0, 1.0);
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

    public static int endStoneLinkRange() { return END_STONE_LINK_RANGE.get(); }
    public static int endStoneItemsPerIntegrity() { return END_STONE_ITEMS_PER_INTEGRITY.get(); }
    public static double endStonePickupRange() { return END_STONE_PICKUP_RANGE.get(); }
    public static double endStoneFlySpeed() { return END_STONE_FLY_SPEED.get(); }
    public static double endStoneDropChance() { return END_STONE_DROP_CHANCE.get(); }

    public static int gravelArtilleryRange() { return GRAVEL_ARTILLERY_RANGE.get(); }
    public static int gravelArtilleryCooldown() { return GRAVEL_ARTILLERY_COOLDOWN.get(); }
    public static int gravelShotCost() { return GRAVEL_SHOT_COST.get(); }
    public static double gravelShardDamage() { return GRAVEL_SHARD_DAMAGE.get(); }
    public static int gravelSlownessTicks() { return GRAVEL_SLOWNESS_TICKS.get(); }
    public static double gravelLeapTriggerRange() { return GRAVEL_LEAP_TRIGGER_RANGE.get(); }
    public static int gravelLeapBelowHorizontal() { return GRAVEL_LEAP_BELOW_HORIZONTAL.get(); }
    public static int gravelLeapBelowDepth() { return GRAVEL_LEAP_BELOW_DEPTH.get(); }
    public static double gravelLeapImpactDamage() { return GRAVEL_LEAP_IMPACT_DAMAGE.get(); }
    public static int gravelLeapBlocks() { return GRAVEL_LEAP_BLOCKS.get(); }
    public static boolean gravelLeapPlacesBlocks() { return GRAVEL_LEAP_PLACES_BLOCKS.get(); }
    public static double gravelDropChance() { return GRAVEL_DROP_CHANCE.get(); }
    public static double basaltFollowDistance() { return BASALT_FOLLOW_DISTANCE.get(); }
    public static int basaltMaxFollowDistance() { return BASALT_MAX_FOLLOW_DISTANCE.get(); }
    public static int basaltThreatRange() { return BASALT_THREAT_RANGE.get(); }
    public static double basaltMagnetRadius() { return BASALT_MAGNET_RADIUS.get(); }
    public static int basaltArrowCost() { return BASALT_ARROW_COST.get(); }
    public static int basaltSmallFireballCost() { return BASALT_SMALL_FIREBALL_COST.get(); }
    public static int basaltLargeFireballCost() { return BASALT_LARGE_FIREBALL_COST.get(); }
    public static int basaltOtherProjectileCost() { return BASALT_OTHER_PROJECTILE_COST.get(); }
    public static int basaltMeleeHitCost() { return BASALT_MELEE_HIT_COST.get(); }
    public static double basaltDropChance() { return BASALT_DROP_CHANCE.get(); }
}
