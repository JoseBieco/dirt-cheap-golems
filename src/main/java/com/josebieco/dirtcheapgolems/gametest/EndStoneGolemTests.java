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
import net.minecraft.world.entity.EntitySpawnReason;
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
            helper.assertTrue(y >= 2.5, "golem fell to y=" + y);
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
}
