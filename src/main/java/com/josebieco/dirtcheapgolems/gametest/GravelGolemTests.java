package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.entity.GravelGolem;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.gametest.GameTestPrefix;

@GameTestNamespace("dirtcheapgolems")
@GameTestPrefix("gravel")
public final class GravelGolemTests {
    static GravelGolem gravelGolem(GameTestHelper helper, BlockPos pos) {
        var golem = helper.spawn(ModEntities.GRAVEL_GOLEM.get(), pos);
        golem.setHomePos(helper.absolutePos(pos));
        return golem;
    }

    /** V2-T9. Husks (not zombies) do not burn under the arena's sunlit barrier ceiling. */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 140)
    public static void firesAtVisibleZombie(GameTestHelper helper) {
        Arena.floor(helper);
        var husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(7, 1, 4));
        var golem = gravelGolem(helper, new BlockPos(1, 1, 4));
        helper.succeedWhen(() -> {
            helper.assertLivingEntityHasMobEffect(husk, MobEffects.SLOWNESS, 0);
            helper.assertTrue(golem.getIntegrity() <= 97, "a shot costs 3 integrity");
        });
    }

    @GameTest(structure = Arena.STRUCTURE, maxTicks = 140)
    public static void shardPassesThroughVillager(GameTestHelper helper) {
        Arena.floor(helper);
        var husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(8, 1, 4));
        var villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(4, 1, 4));
        gravelGolem(helper, new BlockPos(0, 1, 4));
        helper.succeedWhen(() -> {
            helper.assertLivingEntityHasMobEffect(husk, MobEffects.SLOWNESS, 0);
            helper.assertFalse(villager.hasEffect(MobEffects.SLOWNESS), "villager must not be slowed");
            helper.assertValueEqual(villager.getMaxHealth(), villager.getHealth(), "villager health", "villager must not be hurt");
        });
    }

    /** The lower candidate wins even though the higher one is closer. */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 80)
    public static void prefersLowerTarget(GameTestHelper helper) {
        Arena.floor(helper);
        helper.setBlock(new BlockPos(7, 1, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(7, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 6), Blocks.STONE);
        var lower = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(1, 1, 4));   // 6 blocks away, y=1
        var higher = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(3, 3, 6));  // ~4.5 blocks away, y=3
        var golem = gravelGolem(helper, new BlockPos(7, 3, 4));
        golem.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0); // stay on the column
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(golem.getTarget() == lower,
                () -> "golem should target the lower husk but targets " + golem.getTarget());
            helper.assertTrue(higher.isAlive(), "higher husk exists");
            helper.succeed();
        });
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void gravelPillarCreatesGolem(GameTestHelper helper) {
        ActivationTests.buildPillar(helper, Blocks.GRAVEL, Blocks.GRAVEL);
        ActivationTests.useCore(helper, GameType.SURVIVAL, ActivationTests.TOP);
        helper.assertEntitiesPresent(ModEntities.GRAVEL_GOLEM.get(), 1);
        helper.succeed();
    }

    /** Knockback-immune husk at (4,1,4): the golem's flint shards must not shove it out of the leap's reach. */
    static net.minecraft.world.entity.monster.zombie.Husk stationaryHusk(GameTestHelper helper) {
        var husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(4, 1, 4));
        husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        return husk;
    }

    /** V2-T10. Husk instead of zombie: no burning under the sunlit barrier ceiling. */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 200)
    public static void collapseBuriesZombie(GameTestHelper helper) {
        Arena.floor(helper);
        var husk = stationaryHusk(helper);
        var golem = gravelGolem(helper, new BlockPos(4, 1, 2));
        // The golem's own flint shards knock the (AI-less, frictionless) husk around, so bury-check where it stood
        // when the golem collapsed rather than at its spawn point.
        BlockPos[] buriedAt = {null};
        boolean[] wasInWall = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(golem.isRemoved(), "golem should have collapsed");
            if (buriedAt[0] == null) buriedAt[0] = husk.blockPosition();
            helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "husk should be hurt");
            boolean gravel = false;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int y = 0; y <= 2; y++) {
                gravel |= helper.getLevel().getBlockState(buriedAt[0].offset(dx, y, dz)).is(Blocks.GRAVEL);
            }
            helper.assertTrue(gravel, "target buried in gravel");
            // Impact and shards already hurt it: require that the gravel really entombs it (eye block filled).
            wasInWall[0] |= husk.isInWall();
            helper.assertTrue(wasInWall[0], "target should end up inside the gravel (suffocation), eye block "
                + helper.getLevel().getBlockState(BlockPos.containing(husk.getEyePosition())));
        });
    }

    /** V2-T11. Runs in a test environment with mobGriefing off; no global state is touched here. */
    @GameTest(structure = Arena.STRUCTURE, environment = "no_mob_griefing", maxTicks = 200)
    public static void noBlocksWithoutMobGriefing(GameTestHelper helper) {
        Arena.floor(helper);
        stationaryHusk(helper);
        var golem = gravelGolem(helper, new BlockPos(4, 1, 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(golem.isRemoved(), "golem should have collapsed");
            for (int x = 0; x < 9; x++) for (int y = 1; y < 5; y++) for (int z = 0; z < 9; z++) {
                helper.assertFalse(helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.GRAVEL), "no gravel block without mobGriefing");
            }
            int items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2.0),
                e -> e.getItem().is(Items.GRAVEL)).stream().mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertTrue(items == 4, () -> "expected 4 gravel items, found " + items);
        });
    }

    /** V2-T12. Glass over the head and on all four sides must survive the collapse. */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 80)
    public static void neverReplacesSolidBlocks(GameTestHelper helper) {
        Arena.floor(helper);
        var husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(4, 1, 4));
        var golem = gravelGolem(helper, new BlockPos(1, 1, 1));
        var head = GravelGolem.headPos(husk);
        var glass = new java.util.ArrayList<BlockPos>();
        glass.add(head);
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) glass.add(head.relative(d));
        for (var p : glass) helper.getLevel().setBlockAndUpdate(p, Blocks.GLASS.defaultBlockState());
        golem.collapseOnto(husk);
        helper.runAfterDelay(20, () -> {
            for (var p : glass) helper.assertTrue(helper.getLevel().getBlockState(p).is(Blocks.GLASS), "glass at " + p + " intact");
            helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "collapse ran (target hurt)");
            helper.succeed();
        });
    }

    @GameTest(structure = Arena.STRUCTURE, maxTicks = 100)
    public static void leapsDownFromWall(GameTestHelper helper) {
        Arena.floor(helper);
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.STONE);
        var husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(6, 1, 4)); // 2 blocks aside, 2 below: only the "below" rule applies
        husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0); // shards must not shove it out of reach
        var golem = gravelGolem(helper, new BlockPos(4, 3, 4));
        golem.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0); // only an impulse can move it
        var column = helper.absolutePos(new BlockPos(4, 3, 4));
        boolean[] left = {false};
        helper.succeedWhen(() -> {
            if (!golem.isRemoved() && (golem.blockPosition().getX() != column.getX() || golem.blockPosition().getZ() != column.getZ())) left[0] = true;
            helper.assertTrue(left[0], "golem should leave its column");
        });
    }
}
