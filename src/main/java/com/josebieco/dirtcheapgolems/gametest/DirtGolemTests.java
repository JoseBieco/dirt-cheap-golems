package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.entity.DirtGolem;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.gametest.GameTestPrefix;

@GameTestNamespace("dirtcheapgolems")
@GameTestPrefix("dirt")
public final class DirtGolemTests {
    /** Places farmland at (x, 1, z) with the crop at the given age on top; returns the crop position. */
    static BlockPos plant(GameTestHelper helper, int x, int z, Block crop, int age) {
        var farmland = new BlockPos(x, 1, z);
        helper.setBlock(farmland, Blocks.FARMLAND);
        var cropPos = farmland.above();
        helper.setBlock(cropPos, ((CropBlock) crop).getStateForAge(age));
        return cropPos;
    }

    static DirtGolem golemAt(GameTestHelper helper, int x, int z) {
        var golem = helper.spawn(ModEntities.DIRT_GOLEM.get(), new BlockPos(x, 1, z));
        golem.setHomePos(helper.absolutePos(new BlockPos(x, 1, z)));
        return golem;
    }

    static int age(GameTestHelper helper, BlockPos cropPos) {
        var state = helper.getBlockState(cropPos);
        return state.getBlock() instanceof CropBlock crop ? crop.getAge(state) : -1;
    }

    /** T4 */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 200)
    public static void harvestsAndReplantsWheat(GameTestHelper helper) {
        Arena.floor(helper);
        var crop = plant(helper, 4, 4, Blocks.WHEAT, 7);
        var golem = golemAt(helper, 2, 4);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.WHEAT, crop);
            helper.assertValueEqual(0, age(helper, crop), "wheat age", "replanted");
            helper.assertItemEntityPresent(Items.WHEAT);
            helper.assertValueEqual(99, golem.getIntegrity(), "integrity", "after one harvest");
        });
    }

    @GameTest(structure = Arena.STRUCTURE, maxTicks = 220)
    public static void ignoresImmatureCrops(GameTestHelper helper) {
        Arena.floor(helper);
        var crop = plant(helper, 4, 4, Blocks.WHEAT, 3);
        var golem = golemAt(helper, 2, 4);
        helper.runAfterDelay(200, () -> {
            helper.assertValueEqual(3, age(helper, crop), "wheat age", "immature crop untouched");
            helper.assertTrue(golem.getIntegrity() >= 99, "no harvest cost");
            helper.succeed();
        });
    }

    @GameTest(structure = Arena.STRUCTURE, maxTicks = 200)
    public static void replantsPotato(GameTestHelper helper) {
        Arena.floor(helper);
        var crop = plant(helper, 4, 4, Blocks.POTATOES, 7);
        golemAt(helper, 2, 4);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.POTATOES, crop);
            helper.assertValueEqual(0, age(helper, crop), "potato age", "replanted");
        });
    }

    /** Review Focus 3: mature crop inside the scan radius but outside the home radius is ignored. */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 220)
    public static void respectsHomeRadius(GameTestHelper helper) {
        Arena.floor(helper);
        var crop = plant(helper, 6, 4, Blocks.WHEAT, 7);
        var golem = golemAt(helper, 1, 4);
        golem.setHomeRadiusOverride(3);
        helper.runAfterDelay(200, () -> {
            helper.assertValueEqual(7, age(helper, crop), "wheat age", "crop outside home radius untouched");
            helper.succeed();
        });
    }

    /** Review Focus 4: two golems, one crop → harvested once, one integrity point lost in total. */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 220)
    public static void twoGolemsOneCrop(GameTestHelper helper) {
        Arena.floor(helper);
        var crop = plant(helper, 4, 4, Blocks.WHEAT, 7);
        var a = golemAt(helper, 2, 4);
        var b = golemAt(helper, 6, 4);
        helper.runAfterDelay(200, () -> {
            helper.assertValueEqual(0, age(helper, crop), "wheat age", "harvested and replanted");
            // Passive decay (600 ticks) cannot happen within 200 ticks, so all loss comes from harvesting.
            int lost = (100 - a.getIntegrity()) + (100 - b.getIntegrity());
            helper.assertValueEqual(1, lost, "integrity lost", "exactly one harvest");
            helper.succeed();
        });
    }

    @GameTest(structure = Arena.STRUCTURE, maxTicks = 60)
    public static void doesNotTrampleFarmland(GameTestHelper helper) {
        Arena.floor(helper);
        var farmland = new BlockPos(4, 1, 4);
        helper.setBlock(farmland, Blocks.FARMLAND);
        var golem = helper.spawn(ModEntities.DIRT_GOLEM.get(), new BlockPos(4, 4, 4));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(golem.getY() < helper.absolutePos(new BlockPos(4, 3, 4)).getY(), "golem should have fallen");
            helper.assertBlockPresent(Blocks.FARMLAND, farmland);
            helper.succeed();
        });
    }
}
