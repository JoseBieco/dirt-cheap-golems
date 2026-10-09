package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.entity.SandGolem;
import com.josebieco.dirtcheapgolems.entity.projectile.SandBlast;
import com.josebieco.dirtcheapgolems.registry.ModEffects;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
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

    static SandGolem sandGolem(GameTestHelper helper, BlockPos pos) {
        var golem = helper.spawn(ModEntities.SAND_GOLEM.get(), pos);
        golem.setHomePos(helper.absolutePos(pos));
        return golem;
    }

    // ---- turret (spec §6.2) ----

    /** T6 */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 100)
    public static void firesAtZombieInSight(GameTestHelper helper) {
        var zombie = zombie(helper, new BlockPos(8, 1, 4));
        var golem = sandGolem(helper, new BlockPos(0, 1, 4));
        helper.succeedWhen(() -> {
            helper.assertLivingEntityHasMobEffect(zombie, MobEffects.SLOWNESS, 1);
            helper.assertTrue(zombie.hasEffect(ModEffects.sandInEyes()), "zombie should have Sand in Eyes");
            helper.assertTrue(golem.getIntegrity() <= 98, "a shot costs 2 integrity");
        });
    }

    /** T7 */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 120)
    public static void noFireThroughWall(GameTestHelper helper) {
        var zombie = zombie(helper, new BlockPos(8, 1, 4));
        for (int y = 1; y <= 4; y++)
            for (int z = 0; z < 9; z++)
                helper.setBlock(new BlockPos(4, y, z), Blocks.STONE);
        var golem = sandGolem(helper, new BlockPos(0, 1, 4));
        helper.runAfterDelay(100, () -> {
            helper.assertFalse(zombie.hasEffect(MobEffects.SLOWNESS), "zombie must not be hit");
            helper.assertFalse(zombie.hasEffect(ModEffects.sandInEyes()), "zombie must not be hit");
            helper.assertTrue(golem.getIntegrity() >= 99, "no shot fired");
            helper.succeed();
        });
    }

    @GameTest(structure = Arena.STRUCTURE, maxTicks = 60)
    public static void doesNotMove(GameTestHelper helper) {
        Arena.floor(helper);
        var golem = sandGolem(helper, new BlockPos(4, 1, 4));
        helper.runAfterDelay(5, () -> {
            double x = golem.getX(), z = golem.getZ();
            golem.knockback(2.0, 1, 0, helper.getLevel().damageSources().generic(), 0.0F);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(Math.abs(golem.getX() - x) < 0.01 && Math.abs(golem.getZ() - z) < 0.01,
                    () -> "sand golem moved from " + x + "," + z + " to " + golem.getX() + "," + golem.getZ());
                helper.succeed();
            });
        });
    }

    @GameTest(structure = Arena.STRUCTURE, maxTicks = 120)
    public static void ignoresPlayers(GameTestHelper helper) {
        Arena.floor(helper);
        sandGolem(helper, new BlockPos(0, 1, 4));
        var player = Arena.survivalPlayer(helper, new Vec3(6.5, 1, 4.5));
        helper.runAfterDelay(100, () -> {
            Arena.removePlayer(helper, player);
            helper.assertEntityNotPresent(ModEntities.SAND_BLAST.get());
            helper.succeed();
        });
    }

    /** Sand blasts fly through golems instead of hurting them. */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 60)
    public static void blastPassesThroughGolems(GameTestHelper helper) {
        Arena.floor(helper);
        var dirtGolem = helper.spawnWithNoFreeWill(ModEntities.DIRT_GOLEM.get(), new BlockPos(4, 1, 4));
        var zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(7, 1, 4));
        var start = helper.absoluteVec(new Vec3(1.5, 1.4, 4.5));
        var blast = new SandBlast(ModEntities.SAND_BLAST.get(), helper.getLevel());
        blast.setPos(start.x, start.y, start.z);
        blast.shoot(1, 0, 0, 1.5F, 0);
        helper.getLevel().addFreshEntity(blast);
        helper.runAfterDelay(40, () -> {
            helper.assertFalse(dirtGolem.hasEffect(MobEffects.SLOWNESS), "golem must not be slowed");
            helper.assertValueEqual(dirtGolem.getMaxHealth(), dirtGolem.getHealth(), "health", "golem must not be hurt");
            helper.assertTrue(zombie.hasEffect(ModEffects.sandInEyes()), "blast should fly through the golem and hit the zombie");
            helper.succeed();
        });
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void golemFromSandPillar(GameTestHelper helper) {
        ActivationTests.buildPillar(helper, Blocks.RED_SAND, Blocks.SAND);
        ActivationTests.useCore(helper, GameType.SURVIVAL, ActivationTests.TOP);
        helper.assertEntitiesPresent(ModEntities.SAND_GOLEM.get(), 1);
        helper.succeed();
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
