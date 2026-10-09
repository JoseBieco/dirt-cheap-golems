package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.registry.ModEntities;
import com.josebieco.dirtcheapgolems.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.gametest.GameTestPrefix;

@GameTestNamespace("dirtcheapgolems")
@GameTestPrefix("activation")
public final class ActivationTests {
    static final BlockPos BOTTOM = new BlockPos(4, 1, 4);
    static final BlockPos TOP = BOTTOM.above();

    static void buildPillar(GameTestHelper helper, Block top, Block bottom) {
        Arena.floor(helper);
        helper.setBlock(BOTTOM, bottom);
        helper.setBlock(TOP, top);
    }

    /** Makes a mock player holding one golem core and uses it on the given relative block. */
    static ItemStack useCore(GameTestHelper helper, GameType gameType, BlockPos relativeClicked) {
        Player player = helper.makeMockServerPlayer(gameType);
        var stack = new ItemStack(ModItems.GOLEM_CORE.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var abs = helper.absolutePos(relativeClicked);
        var hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        return stack;
    }

    static void assertNoGolemAndIntactPillar(GameTestHelper helper, ItemStack stack, Block top, Block bottom) {
        helper.assertEntityNotPresent(ModEntities.DIRT_GOLEM.get());
        helper.assertBlockPresent(bottom, BOTTOM);
        helper.assertBlockPresent(top, TOP);
        helper.assertValueEqual(1, stack.getCount(), "core count", "core must not be consumed");
    }

    /** T1 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void dirtPillarCreatesGolem(GameTestHelper helper) {
        buildPillar(helper, Blocks.DIRT, Blocks.DIRT);
        var stack = useCore(helper, GameType.SURVIVAL, TOP);
        helper.assertEntitiesPresent(ModEntities.DIRT_GOLEM.get(), 1);
        var golem = helper.findOneEntity(ModEntities.DIRT_GOLEM.get());
        helper.assertValueEqual(helper.absolutePos(BOTTOM), golem.blockPosition(), "golem position", "golem spawns on the bottom block");
        helper.assertValueEqual(helper.absolutePos(BOTTOM), golem.getHomePos(), "home", "home is the bottom block");
        helper.assertBlockPresent(Blocks.AIR, BOTTOM);
        helper.assertBlockPresent(Blocks.AIR, TOP);
        helper.assertValueEqual(0, stack.getCount(), "core count", "one core consumed");
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void clickingBottomBlockAlsoWorks(GameTestHelper helper) {
        buildPillar(helper, Blocks.DIRT, Blocks.DIRT);
        useCore(helper, GameType.SURVIVAL, BOTTOM);
        helper.assertEntitiesPresent(ModEntities.DIRT_GOLEM.get(), 1);
        helper.assertBlockPresent(Blocks.AIR, BOTTOM);
        helper.assertBlockPresent(Blocks.AIR, TOP);
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void coarseDirtMixedWithDirtWorks(GameTestHelper helper) {
        buildPillar(helper, Blocks.COARSE_DIRT, Blocks.DIRT);
        useCore(helper, GameType.SURVIVAL, TOP);
        helper.assertEntitiesPresent(ModEntities.DIRT_GOLEM.get(), 1);
        helper.succeed();
    }

    /** T2 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void mixedPillarDoesNothing(GameTestHelper helper) {
        buildPillar(helper, Blocks.DIRT, Blocks.SAND);
        var stack = useCore(helper, GameType.SURVIVAL, TOP);
        assertNoGolemAndIntactPillar(helper, stack, Blocks.DIRT, Blocks.SAND);
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void creativeDoesNotConsumeCore(GameTestHelper helper) {
        buildPillar(helper, Blocks.DIRT, Blocks.DIRT);
        var stack = useCore(helper, GameType.CREATIVE, TOP);
        helper.assertEntitiesPresent(ModEntities.DIRT_GOLEM.get(), 1);
        helper.assertValueEqual(1, stack.getCount(), "core count", "creative keeps the core");
        helper.succeed();
    }

    /** Spec §5.1: clay cross + redstone center crafts 2 cores. */
    @GameTest(structure = Arena.STRUCTURE)
    public static void coreRecipeYieldsTwo(GameTestHelper helper) {
        var e = ItemStack.EMPTY;
        var c = new ItemStack(Items.CLAY_BALL);
        var r = new ItemStack(Items.REDSTONE);
        var input = CraftingInput.of(3, 3, List.of(e, c, e, c, r, c, e, c, e));
        var recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "golem core recipe should match");
        var result = recipe.get().value().assemble(input);
        helper.assertTrue(result.is(ModItems.GOLEM_CORE.get()), "result should be a golem core");
        helper.assertValueEqual(2, result.getCount(), "result count", "recipe yields 2");
        helper.succeed();
    }

    /** Review Focus 1 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void adventureModeCannotActivate(GameTestHelper helper) {
        buildPillar(helper, Blocks.DIRT, Blocks.DIRT);
        var stack = useCore(helper, GameType.ADVENTURE, TOP);
        assertNoGolemAndIntactPillar(helper, stack, Blocks.DIRT, Blocks.DIRT);
        helper.succeed();
    }
}
