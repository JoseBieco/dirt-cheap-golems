package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.entity.EndStoneGolem;
import com.josebieco.dirtcheapgolems.item.GolemCoreItem;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import com.josebieco.dirtcheapgolems.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.gametest.GameTestPrefix;

/** End Stone Golem: flight and container link (spec v2 section 6.1). */
@GameTestNamespace("dirtcheapgolems")
@GameTestPrefix("end_stone")
public final class EndStoneGolemTests {
    static final BlockPos CENTER = new BlockPos(4, 1, 4);

    static EndStoneGolem golemAt(GameTestHelper helper, BlockPos pos) {
        Arena.floor(helper);
        var golem = helper.spawnWithNoFreeWill(ModEntities.END_STONE_GOLEM.get(), pos);
        golem.setHomePos(helper.absolutePos(pos));
        return golem;
    }

    private static GlobalPos link(GameTestHelper helper, BlockPos relative) {
        return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(relative));
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void endStonePillarCreatesGolem(GameTestHelper helper) {
        ActivationTests.buildPillar(helper, Blocks.END_STONE, Blocks.END_STONE);
        ActivationTests.useCore(helper, GameType.SURVIVAL, ActivationTests.TOP);
        helper.assertEntitiesPresent(ModEntities.END_STONE_GOLEM.get(), 1);
        helper.succeed();
    }

    /** V2-T3 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void linksToChestNearHome(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        var chest = CENTER.west(3);
        helper.setBlock(chest, Blocks.CHEST);
        var stack = new ItemStack(ModItems.GOLEM_CORE.get(), 1);
        GolemCoreItem.setLink(stack, link(helper, chest));
        var player = helper.makeMockServerPlayerFull(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.interactOn(golem, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertValueEqual(link(helper, chest), golem.getLinkedContainer(), "link", "golem linked to the chest");
        helper.assertValueEqual(1, stack.getCount(), "core count", "core must not be consumed");
        helper.succeed();
    }

    /** Core without link: the command is ignored. */
    @GameTest(structure = Arena.STRUCTURE)
    public static void unlinkedCoreDoesNothing(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        var stack = new ItemStack(ModItems.GOLEM_CORE.get(), 1);
        var player = helper.makeMockServerPlayerFull(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.interactOn(golem, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(golem.getLinkedContainer() == null, "no link");
        helper.succeed();
    }

    /** V2-T4 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void refusesChestTooFar(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        var result = golem.tryLink(link(helper, CENTER.east(40)));
        helper.assertValueEqual(EndStoneGolem.LinkResult.TOO_FAR, result, "result", "40 blocks from home");
        helper.assertTrue(golem.getLinkedContainer() == null, "no link stored");
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void refusalKeepsPreviousLink(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        var chest = CENTER.west(3);
        helper.setBlock(chest, Blocks.CHEST);
        helper.assertValueEqual(EndStoneGolem.LinkResult.LINKED, golem.tryLink(link(helper, chest)), "result", "first link");
        helper.assertValueEqual(EndStoneGolem.LinkResult.TOO_FAR, golem.tryLink(link(helper, CENTER.east(40))), "result", "far");
        helper.assertValueEqual(link(helper, chest), golem.getLinkedContainer(), "link", "previous link kept");
        golem.clearLink();
        helper.assertTrue(golem.getLinkedContainer() == null, "cleared");
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void refusesOtherDimension(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        var elsewhere = GlobalPos.of(Level.NETHER, helper.absolutePos(CENTER));
        helper.assertValueEqual(EndStoneGolem.LinkResult.OTHER_DIMENSION, golem.tryLink(elsewhere), "result", "other dimension");
        helper.assertTrue(golem.getLinkedContainer() == null, "no link stored");
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void refusesBlockWithoutInventory(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        var result = golem.tryLink(link(helper, new BlockPos(4, 0, 4)));
        helper.assertValueEqual(EndStoneGolem.LinkResult.NO_INVENTORY, result, "result", "stone has no inventory");
        helper.assertTrue(golem.getLinkedContainer() == null, "no link stored");
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void doesNotFall(GameTestHelper helper) {
        Arena.floor(helper);
        var golem = helper.spawnWithNoFreeWill(ModEntities.END_STONE_GOLEM.get(), new BlockPos(4, 3, 4));
        helper.runAfterDelay(40, () -> {
            double y = helper.relativeVec(golem.position()).y;
            helper.assertTrue(y >= 1.4, "golem fell to y=" + y); // settles into the 0.5-1.5 hover band, never lands
            helper.assertTrue(golem.getHealth() == golem.getMaxHealth(), "no fall damage");
            helper.succeed();
        });
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void linkSurvivesReload(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        var chest = CENTER.west(3);
        helper.setBlock(chest, Blocks.CHEST);
        golem.tryLink(link(helper, chest));
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        golem.saveWithoutId(output);
        var tag = output.buildResult();
        var reloaded = ModEntities.END_STONE_GOLEM.get().create(helper.getLevel(), EntitySpawnReason.LOAD);
        reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        helper.assertValueEqual(link(helper, chest), reloaded.getLinkedContainer(), "link", "link reloaded");
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void crumbleDropsEndStone(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        golem.setCrumbleDropChanceOverride(1.0);
        golem.kill(helper.getLevel());
        helper.assertTrue(golem.isRemoved(), "golem crumbled");
        helper.succeedWhen(() -> helper.assertItemEntityPresent(Items.END_STONE, CENTER, 3.0));
    }

    // ---- item collection (V2-T5..T7, Task 4) ----

    /** A golem with free will (goals active), home at its position. */
    static EndStoneGolem activeGolemAt(GameTestHelper helper, BlockPos pos) {
        Arena.floor(helper);
        var golem = helper.spawn(ModEntities.END_STONE_GOLEM.get(), pos);
        golem.setHomePos(helper.absolutePos(pos));
        return golem;
    }

    private static int count(BaseContainerBlockEntity be, Item item) {
        int n = 0;
        for (int i = 0; i < be.getContainerSize(); i++)
            if (be.getItem(i).is(item)) n += be.getItem(i).getCount();
        return n;
    }

    private static void fillWithDirt(BaseContainerBlockEntity be) {
        for (int i = 0; i < be.getContainerSize(); i++) be.setItem(i, new ItemStack(Items.DIRT, 64));
    }

    private static int groundItems(GameTestHelper helper) {
        return helper.getEntities(EntityTypes.ITEM, CENTER, 8.0).size();
    }

    /** V2-T5 */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 450)
    public static void teleportsItemsIntoChest(GameTestHelper helper) {
        var golem = activeGolemAt(helper, CENTER);
        var chestPos = CENTER.west(3);
        helper.setBlock(chestPos, Blocks.CHEST);
        golem.tryLink(link(helper, chestPos));
        int[][] spots = {{2, 2}, {6, 2}, {2, 6}, {6, 6}, {4, 2}, {4, 6}, {3, 4}, {5, 5}};
        for (int[] s : spots) helper.spawnItem(Items.STONE, new Vec3(s[0] + 0.5, 1.0, s[1] + 0.5)).setNoPickUpDelay();
        var chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(count(chest, Items.STONE) == 8, () -> "chest has " + count(chest, Items.STONE) + " stone");
            helper.assertTrue(groundItems(helper) == 0, "no item entities left");
            int integrity = golem.getIntegrity();
            helper.assertTrue(integrity >= 97 && integrity <= 98, () -> "integrity " + integrity);
        });
    }

    /** V2-T6 */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 250)
    public static void fullChestLeavesItems(GameTestHelper helper) {
        var golem = activeGolemAt(helper, CENTER);
        var chestPos = CENTER.west(3);
        helper.setBlock(chestPos, Blocks.CHEST);
        var chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        fillWithDirt(chest);
        golem.tryLink(link(helper, chestPos));
        for (int i = 0; i < 4; i++) helper.spawnItem(Items.STONE, new Vec3(3.5 + i, 1.0, 6.5)).setNoPickUpDelay();
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(groundItems(helper) == 4, () -> "ground items " + groundItems(helper));
            helper.assertTrue(golem.getIntegrity() >= 99, "integrity untouched");
            helper.succeed();
        });
    }

    /** V2-T7 */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 100)
    public static void brokenChestClearsLink(GameTestHelper helper) {
        var golem = activeGolemAt(helper, CENTER);
        var chestPos = CENTER.west(3);
        helper.setBlock(chestPos, Blocks.CHEST);
        golem.tryLink(link(helper, chestPos));
        helper.assertTrue(golem.getLinkedContainer() != null, "linked");
        helper.destroyBlock(chestPos);
        helper.succeedWhen(() -> {
            helper.assertTrue(golem.getLinkedContainer() == null, "link cleared");
            helper.assertTrue(golem.isAlive(), "golem alive");
        });
    }

    /** Review focus 1: the capability of a double chest half is the combined inventory. */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 450)
    public static void doubleChestUsesBothHalves(GameTestHelper helper) {
        var golem = activeGolemAt(helper, CENTER);
        var a = CENTER.west(3);
        var b = CENTER.west(2);
        helper.setBlock(a, Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
        helper.setBlock(b, Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
        var halfA = helper.getBlockEntity(a, ChestBlockEntity.class);
        var halfB = helper.getBlockEntity(b, ChestBlockEntity.class);
        fillWithDirt(halfA);
        helper.assertValueEqual(EndStoneGolem.LinkResult.LINKED, golem.tryLink(link(helper, a)), "result", "link half A");
        for (int i = 0; i < 4; i++) helper.spawnItem(Items.STONE, new Vec3(3.5 + i, 1.0, 6.5)).setNoPickUpDelay();
        helper.succeedWhen(() -> {
            helper.assertTrue(count(halfB, Items.STONE) == 4, () -> "half B has " + count(halfB, Items.STONE) + " stone");
            helper.assertTrue(count(halfA, Items.STONE) == 0, "half A untouched");
        });
    }

    /** Review focus 2 */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 450)
    public static void ignoresItemsWithPickupDelay(GameTestHelper helper) {
        var golem = activeGolemAt(helper, CENTER);
        var chestPos = CENTER.west(3);
        helper.setBlock(chestPos, Blocks.CHEST);
        golem.tryLink(link(helper, chestPos));
        var item = helper.spawnItem(Items.STONE, new Vec3(6.5, 1.0, 6.5));
        item.setPickUpDelay(200);
        var chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(!item.isRemoved() && count(chest, Items.STONE) == 0, "item still on the ground at tick 100");
            helper.succeedWhen(() -> helper.assertTrue(count(chest, Items.STONE) == 1, "item eventually in the chest"));
        });
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void itemsCarriedSurvivesReload(GameTestHelper helper) {
        var golem = golemAt(helper, CENTER);
        golem.setItemsCarried(3);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        golem.saveWithoutId(output);
        var tag = output.buildResult();
        var reloaded = ModEntities.END_STONE_GOLEM.get().create(helper.getLevel(), EntitySpawnReason.LOAD);
        reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        helper.assertValueEqual(3, reloaded.getItemsCarried(), "itemsCarried", "reloaded");
        helper.succeed();
    }

    /** Spec 6.1: hovers 0.5 to 1.5 blocks above the ground when idle. */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 150)
    public static void hoversAboveGroundWhenIdle(GameTestHelper helper) {
        var golem = activeGolemAt(helper, CENTER);
        helper.onEachTick(() -> {
            long t = helper.getTick();
            if (t < 40) return;
            double h = helper.relativeVec(golem.position()).y - 1.0;
            helper.assertTrue(h >= 0.4 && h <= 1.6, () -> "height above floor " + h + " at tick " + t);
            if (t >= 100) helper.succeed();
        });
    }
}
