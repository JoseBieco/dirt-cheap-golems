package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.item.GolemCoreItem;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import com.josebieco.dirtcheapgolems.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.gametest.GameTestPrefix;

import static com.josebieco.dirtcheapgolems.gametest.ActivationTests.BOTTOM;
import static com.josebieco.dirtcheapgolems.gametest.ActivationTests.TOP;

/** Golem Core command mode (spec v2 section 3.1). */
@GameTestNamespace("dirtcheapgolems")
@GameTestPrefix("command")
public final class CommandTests {
    private static GlobalPos link(GameTestHelper helper, BlockPos relative) {
        return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(relative));
    }

    private static ItemStack linkedCore(GameTestHelper helper) {
        var stack = new ItemStack(ModItems.GOLEM_CORE.get(), 1);
        GolemCoreItem.setLink(stack, link(helper, new BlockPos(1, 1, 1)));
        return stack;
    }

    /** V2-T1 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void sneakingOnPillarDoesNothing(GameTestHelper helper) {
        ActivationTests.buildPillar(helper, Blocks.DIRT, Blocks.DIRT);
        var stack = ActivationTests.useCore(helper, GameType.SURVIVAL, TOP, true);
        ActivationTests.assertNoGolemAndIntactPillar(helper, stack, Blocks.DIRT, Blocks.DIRT);
        helper.succeed();
    }

    /** V2-T2 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void sneakingOnChestStoresLink(GameTestHelper helper) {
        Arena.floor(helper);
        helper.setBlock(BOTTOM, Blocks.CHEST);
        var stack = ActivationTests.useCore(helper, GameType.SURVIVAL, BOTTOM, true);
        helper.assertValueEqual(link(helper, BOTTOM), GolemCoreItem.getLink(stack), "link", "core stores the chest position");
        helper.assertValueEqual(1, stack.getCount(), "core count", "core must not be consumed");
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void sneakingOnStoneDoesNotLink(GameTestHelper helper) {
        Arena.floor(helper);
        var stack = ActivationTests.useCore(helper, GameType.SURVIVAL, new BlockPos(2, 0, 2), true);
        helper.assertTrue(GolemCoreItem.getLink(stack) == null, "stone must not be linkable");
        helper.succeed();
    }

    /** Sneaking player looking at the given pitch. Pitch 0 at y=30 is open sky; pitch 90 at y=1 looks at the arena floor. */
    private static net.minecraft.server.level.ServerPlayer sneakingPlayerLooking(GameTestHelper helper, float xRot, ItemStack stack) {
        var player = Arena.survivalPlayer(helper, new Vec3(4.5, xRot == 0 ? 30 : 1, 0.5));
        player.setShiftKeyDown(true);
        player.setYRot(0);
        player.setXRot(xRot);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void sneakingInAirClearsLink(GameTestHelper helper) {
        Arena.floor(helper);
        var stack = linkedCore(helper);
        var player = sneakingPlayerLooking(helper, 0, stack);
        helper.assertTrue(GolemCoreItem.getLink(stack) != null, "precondition: core is linked");
        stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        Arena.removePlayer(helper, player);
        helper.assertTrue(GolemCoreItem.getLink(stack) == null, "link should be cleared");
        helper.succeed();
    }

    /** Spec 3.1: sneak-click on a block without inventory does nothing; the follow-up use() must not clear the link. */
    @GameTest(structure = Arena.STRUCTURE)
    public static void sneakingOnStoneKeepsLink(GameTestHelper helper) {
        Arena.floor(helper);
        var stack = linkedCore(helper);
        var before = GolemCoreItem.getLink(stack);
        var player = sneakingPlayerLooking(helper, 90, stack);
        var abs = helper.absolutePos(new BlockPos(4, 0, 0));
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false);
        var result = stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertFalse(result.consumesAction(), "stone click should pass");
        // The game then falls through to use() because the block click was not consumed.
        stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        Arena.removePlayer(helper, player);
        helper.assertValueEqual(before, GolemCoreItem.getLink(stack), "link", "link kept after clicking stone");
        helper.succeed();
    }

    /** Review Focus 3: base golems ignore the command. */
    @GameTest(structure = Arena.STRUCTURE)
    public static void commandOnDirtGolemIsIgnored(GameTestHelper helper) {
        Arena.floor(helper);
        var golem = helper.spawn(ModEntities.DIRT_GOLEM.get(), new BlockPos(2, 1, 2));
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        var stack = linkedCore(helper);
        var before = GolemCoreItem.getLink(stack);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.interactOn(golem, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertValueEqual(before, GolemCoreItem.getLink(stack), "link", "link is unchanged");
        helper.assertValueEqual(1, stack.getCount(), "core count", "core must not be consumed");
        helper.assertTrue(golem.getIntegrity() >= 99, "golem integrity untouched");
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void normalClickStillActivates(GameTestHelper helper) {
        ActivationTests.buildPillar(helper, Blocks.DIRT, Blocks.DIRT);
        ActivationTests.useCore(helper, GameType.SURVIVAL, TOP, false);
        helper.assertEntitiesPresent(ModEntities.DIRT_GOLEM.get(), 1);
        helper.succeed();
    }
}
