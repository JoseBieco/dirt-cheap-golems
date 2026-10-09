package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.entity.NetherrackGolem;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
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
