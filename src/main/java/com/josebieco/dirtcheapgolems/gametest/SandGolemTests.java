package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.gametest.GameTestPrefix;

@GameTestNamespace("dirtcheapgolems")
@GameTestPrefix("sand")
public final class SandGolemTests {
    static Zombie zombie(GameTestHelper helper, BlockPos pos) {
        Arena.floor(helper);
        return helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, pos);
    }

    // ---- Sand in Eyes effect (spec §6.2, D8) ----

    @GameTest(structure = Arena.STRUCTURE)
    public static void clearsTargetOnApply(GameTestHelper helper) {
        var zombie = zombie(helper, new BlockPos(2, 1, 4));
        var villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(6, 1, 4));
        zombie.setTarget(villager);
        helper.assertTrue(zombie.getTarget() == villager, "zombie should target the villager");
        zombie.addEffect(new MobEffectInstance(ModEffects.sandInEyes(), 60));
        helper.assertTrue(zombie.getTarget() == null, "Sand in Eyes should clear the target");
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void reducesFollowRange(GameTestHelper helper) {
        var zombie = zombie(helper, new BlockPos(4, 1, 4));
        double before = zombie.getAttributeValue(Attributes.FOLLOW_RANGE);
        zombie.addEffect(new MobEffectInstance(ModEffects.sandInEyes(), 60));
        double after = zombie.getAttributeValue(Attributes.FOLLOW_RANGE);
        helper.assertTrue(Math.abs(after - before * 0.25) < 0.01,
            () -> "follow range should drop to 25% (" + before * 0.25 + ") but was " + after);
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE, maxTicks = 100)
    public static void followRangeRestoredAfterExpiry(GameTestHelper helper) {
        var zombie = zombie(helper, new BlockPos(4, 1, 4));
        double before = zombie.getAttributeValue(Attributes.FOLLOW_RANGE);
        zombie.addEffect(new MobEffectInstance(ModEffects.sandInEyes(), 60));
        helper.runAfterDelay(65, () -> {
            helper.assertFalse(zombie.hasEffect(ModEffects.sandInEyes()), "effect should have expired");
            helper.assertTrue(Math.abs(zombie.getAttributeValue(Attributes.FOLLOW_RANGE) - before) < 0.01, "follow range restored");
            helper.succeed();
        });
    }
}
