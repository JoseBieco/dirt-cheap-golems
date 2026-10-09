package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.entity.NetherrackGolem;
import com.josebieco.dirtcheapgolems.entity.ThermalBlast;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.gametest.GameTestPrefix;

@GameTestNamespace("dirtcheapgolems")
@GameTestPrefix("netherrack")
public final class NetherrackGolemTests {
    static final BlockPos CENTER = new BlockPos(4, 1, 4);

    static NetherrackGolem golemAt(GameTestHelper helper, BlockPos pos) {
        Arena.floor(helper);
        var golem = helper.spawnWithNoFreeWill(ModEntities.NETHERRACK_GOLEM.get(), pos);
        golem.setHomePos(helper.absolutePos(pos));
        return golem;
    }

    static int countLights(GameTestHelper helper) {
        int count = 0;
        for (var pos : BlockPos.betweenClosed(0, 0, 0, 8, 4, 8))
            if (helper.getBlockState(pos).is(Blocks.LIGHT)) count++;
        return count;
    }

    // ---- walking light (spec §6.3) ----

    @GameTest(structure = Arena.STRUCTURE)
    public static void placesLightAtHead(GameTestHelper helper) {
        golemAt(helper, CENTER);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.LIGHT, CENTER.above());
            helper.assertBlockProperty(CENTER.above(), LightBlock.LEVEL, 12);
        });
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void lightFollowsGolem(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        var moved = CENTER.east(3);
        helper.runAfterDelay(3, () -> {
            helper.assertBlockPresent(Blocks.LIGHT, CENTER.above());
            var target = helper.absoluteVec(net.minecraft.world.phys.Vec3.atBottomCenterOf(moved));
            golem.teleportTo(target.x, target.y, target.z);
            helper.runAfterDelay(3, () -> {
                helper.assertBlockPresent(Blocks.AIR, CENTER.above());
                helper.assertBlockPresent(Blocks.LIGHT, moved.above());
                helper.assertValueEqual(1, countLights(helper), "light count", "only one light");
                helper.succeed();
            });
        });
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void doesNotReplaceSolidBlocks(GameTestHelper helper) {
        Arena.floor(helper);
        helper.setBlock(CENTER.above(), Blocks.GLASS);
        golemAt(helper, CENTER);
        helper.runAfterDelay(5, () -> {
            helper.assertBlockPresent(Blocks.GLASS, CENTER.above());
            helper.assertValueEqual(0, countLights(helper), "light count", "no light placed");
            helper.succeed();
        });
    }

    /** T10 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void noLightLeftAfterDeath(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        helper.runAfterDelay(3, () -> {
            helper.assertValueEqual(1, countLights(helper), "light count", "light while alive");
            golem.kill(helper.getLevel());
            helper.assertTrue(golem.isRemoved(), "golem should crumble");
            helper.assertValueEqual(0, countLights(helper), "light count", "after death");
            helper.succeed();
        });
    }

    // ---- kamikaze (spec §6.3) ----

    static NetherrackGolem activeGolemAt(GameTestHelper helper, BlockPos pos) {
        Arena.floor(helper);
        var golem = helper.spawn(ModEntities.NETHERRACK_GOLEM.get(), pos);
        golem.setHomePos(helper.absolutePos(pos));
        return golem;
    }

    static int countNonAirBlocksExceptLight(GameTestHelper helper) {
        int count = 0;
        for (var pos : BlockPos.betweenClosed(0, 0, 0, 8, 4, 8)) {
            var state = helper.getBlockState(pos);
            if (!state.isAir() && !state.is(Blocks.LIGHT)) count++;
        }
        return count;
    }

    /** Calls {@code action} on the first tick {@code condition} holds. */
    static void whenFirst(GameTestHelper helper, java.util.function.BooleanSupplier condition, Runnable action) {
        if (condition.getAsBoolean()) action.run();
        else helper.runAfterDelay(1, () -> whenFirst(helper, condition, action));
    }

    /** T8 */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 200)
    public static void blastBurnsAndDamagesZombie(GameTestHelper helper) {
        var golem = activeGolemAt(helper, new BlockPos(1, 1, 4));
        var zombie = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(5, 1, 4));
        int blocksBefore = countNonAirBlocksExceptLight(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.isOnFire(), "zombie should burn");
            helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "zombie should be hurt");
            helper.assertTrue(Math.abs(golem.getIntegrity() - 66) <= 1, () -> "integrity should be 66 but was " + golem.getIntegrity());
            helper.assertValueEqual(blocksBefore, countNonAirBlocksExceptLight(helper), "blocks", "the blast must not change blocks");
        });
    }

    @GameTest(structure = Arena.STRUCTURE, maxTicks = 200)
    public static void fuseCancelsWhenTargetLeaves(GameTestHelper helper) {
        var golem = activeGolemAt(helper, new BlockPos(1, 1, 4));
        var zombie = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(3, 1, 4));
        whenFirst(helper, () -> golem.getFuseTicks() > 0, () -> {
            var away = helper.absoluteVec(net.minecraft.world.phys.Vec3.atBottomCenterOf(new BlockPos(7, 1, 8)));
            zombie.teleportTo(away.x, away.y, away.z);
            helper.runAfterDelay(com.josebieco.dirtcheapgolems.config.GolemConfig.netherrackFuseTicks() + 2, () -> {
                helper.assertFalse(zombie.isOnFire(), "fuse should have been cancelled");
                helper.assertValueEqual(100, golem.getIntegrity(), "integrity", "no blast happened");
                helper.succeed();
            });
        });
    }

    @GameTest(structure = Arena.STRUCTURE, maxTicks = 200)
    public static void blastSparesPlayersByDefault(GameTestHelper helper) {
        activeGolemAt(helper, new BlockPos(1, 1, 4));
        var zombie = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(5, 1, 4));
        var player = Arena.survivalPlayer(helper, new net.minecraft.world.phys.Vec3(5.5, 1, 5.5));
        whenFirst(helper, zombie::isOnFire, () -> {
            Arena.removePlayer(helper, player);
            helper.assertFalse(player.isOnFire(), "player must not burn");
            helper.assertValueEqual(player.getMaxHealth(), player.getHealth(), "player health", "player must not be hurt");
            helper.succeed();
        });
    }

    /** Spec §5.8: the thermal blast never harms golems or passive mobs. */
    @GameTest(structure = Arena.STRUCTURE)
    public static void blastSparesGolemsAndPassiveMobs(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        var zombie = helper.spawnWithNoFreeWill(EntityTypes.HUSK, CENTER.east());
        var villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, CENTER.west());
        var dirtGolem = helper.spawnWithNoFreeWill(ModEntities.DIRT_GOLEM.get(), CENTER.north());
        ThermalBlast.detonate(helper.getLevel(), golem, 3.0, 4.0F, 5, false);
        helper.assertTrue(zombie.isOnFire(), "zombie should burn");
        helper.assertFalse(villager.isOnFire(), "villager must not burn");
        helper.assertFalse(dirtGolem.isOnFire(), "golem must not burn");
        helper.assertValueEqual(villager.getMaxHealth(), villager.getHealth(), "villager health", "villager must not be hurt");
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void threeBlastsCrumbleToCharcoal(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        for (int i = 0; i < 3; i++) golem.consumeIntegrity(com.josebieco.dirtcheapgolems.config.GolemConfig.netherrackBlastCost());
        helper.assertTrue(golem.isRemoved(), "golem should crumble after 3 blasts");
        helper.assertItemEntityPresent(net.minecraft.world.item.Items.CHARCOAL);
        helper.assertValueEqual(0, countLights(helper), "light count", "after crumbling");
        helper.succeed();
    }

    /** Review Focus 5: unload/reload never leaves an orphan light, even if one survived a crash. */
    @GameTest(structure = Arena.STRUCTURE)
    public static void noLightLeftAfterReload(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        helper.runAfterDelay(3, () -> {
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
            golem.saveWithoutId(output);
            var tag = output.buildResult();

            golem.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            helper.assertValueEqual(0, countLights(helper), "light count", "after unload");

            // Simulate a crash that left the saved light in the world.
            helper.setBlock(CENTER.above(), Blocks.LIGHT);

            var reloaded = ModEntities.NETHERRACK_GOLEM.get().create(helper.getLevel(), EntitySpawnReason.LOAD);
            reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
            var elsewhere = helper.absoluteVec(net.minecraft.world.phys.Vec3.atBottomCenterOf(CENTER.west(3)));
            reloaded.setPos(elsewhere.x, elsewhere.y, elsewhere.z);
            reloaded.setNoAi(true);
            helper.getLevel().addFreshEntity(reloaded);

            helper.runAfterDelay(3, () -> {
                helper.assertBlockPresent(Blocks.AIR, CENTER.above());
                helper.assertBlockPresent(Blocks.LIGHT, CENTER.west(3).above());
                helper.assertValueEqual(1, countLights(helper), "light count", "only the new light");
                helper.succeed();
            });
        });
    }
}
