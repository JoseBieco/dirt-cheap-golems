package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.entity.GravelGolem;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
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
}
