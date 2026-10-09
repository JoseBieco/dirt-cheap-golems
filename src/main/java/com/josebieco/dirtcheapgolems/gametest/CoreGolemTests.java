package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.AbstractCheapGolem;
import com.josebieco.dirtcheapgolems.entity.DirtGolem;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.gametest.GameTestPrefix;

@GameTestNamespace("dirtcheapgolems")
@GameTestPrefix("core")
public final class CoreGolemTests {
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);

    static DirtGolem spawnDirtGolem(GameTestHelper helper, BlockPos relative) {
        Arena.floor(helper);
        var golem = helper.spawn(ModEntities.DIRT_GOLEM.get(), relative);
        golem.setHomePos(helper.absolutePos(relative));
        return golem;
    }

    /** T3: integrity 1 + one passive decay cycle → crumbles. */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 700)
    public static void integrityDepletionCrumbles(GameTestHelper helper) {
        var golem = spawnDirtGolem(helper, CENTER);
        golem.consumeIntegrity(99);
        helper.assertValueEqual(1, golem.getIntegrity(), "integrity", "after consuming 99");
        helper.assertFalse(golem.isRemoved(), "golem with integrity 1 must still exist");
        helper.runAfterDelay(GolemConfig.passiveDecayTicks() + 5, () -> {
            helper.assertTrue(golem.isRemoved(), "golem should crumble after one passive decay cycle");
            helper.succeed();
        });
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void consumeToZeroCrumblesImmediately(GameTestHelper helper) {
        var golem = spawnDirtGolem(helper, CENTER);
        golem.consumeIntegrity(100);
        helper.assertTrue(golem.isRemoved(), "golem should be removed in the same tick");
        helper.succeed();
    }

    /** T11: integrity and home survive an NBT round trip. */
    @GameTest(structure = Arena.STRUCTURE)
    public static void nbtRoundTrip(GameTestHelper helper) {
        var golem = spawnDirtGolem(helper, CENTER);
        var home = helper.absolutePos(new BlockPos(2, 1, 2));
        golem.consumeIntegrity(43);
        golem.setHomePos(home);

        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        golem.saveWithoutId(output);
        var tag = output.buildResult();

        var copy = ModEntities.DIRT_GOLEM.get().create(helper.getLevel(), EntitySpawnReason.LOAD);
        copy.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));

        helper.assertValueEqual(57, copy.getIntegrity(), "integrity", "after reload");
        helper.assertValueEqual(home, copy.getHomePos(), "home", "after reload");
        helper.succeed();
    }

    /** Review Focus 2: a golem spawned without a pillar (spawn egg) gets its spawn position as home. */
    @GameTest(structure = Arena.STRUCTURE)
    public static void spawnEggSetsHome(GameTestHelper helper) {
        Arena.floor(helper);
        var pos = helper.absolutePos(CENTER);
        AbstractCheapGolem golem = ModEntities.DIRT_GOLEM.get().spawn(helper.getLevel(), pos, EntitySpawnReason.SPAWN_ITEM_USE);
        helper.assertTrue(golem != null, "golem should spawn");
        helper.assertValueEqual(pos, golem.getHomePos(), "home", "spawn egg golem");
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void healingDoesNotRestoreIntegrity(GameTestHelper helper) {
        var golem = spawnDirtGolem(helper, CENTER);
        golem.consumeIntegrity(30);
        golem.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 4.0F);
        golem.heal(10.0F);
        helper.assertValueEqual(70, golem.getIntegrity(), "integrity", "after healing");
        helper.succeed();
    }
}
