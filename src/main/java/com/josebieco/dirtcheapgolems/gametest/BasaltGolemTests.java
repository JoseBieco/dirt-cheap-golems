package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.entity.BasaltGolem;
import com.josebieco.dirtcheapgolems.entity.ShieldMode;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import com.josebieco.dirtcheapgolems.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.gametest.GameTestPrefix;

/** Basalt Golem: owner, shield modes and source block (spec v2 section 6.3). */
@GameTestNamespace("dirtcheapgolems")
@GameTestPrefix("basalt")
public final class BasaltGolemTests {
    static final BlockPos CENTER = new BlockPos(4, 1, 4);
    static final Vec3 OWNER_SPOT = new Vec3(1.5, 1, 1.5);

    /** Builds a pillar and activates it with a real player in the level; returns that player. */
    private static ServerPlayer activate(GameTestHelper helper, Block top, Block bottom) {
        ActivationTests.buildPillar(helper, top, bottom);
        var player = Arena.survivalPlayer(helper, OWNER_SPOT);
        var stack = new ItemStack(ModItems.GOLEM_CORE.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var abs = helper.absolutePos(ActivationTests.TOP);
        var hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        return player;
    }

    private static BasaltGolem ownedGolem(GameTestHelper helper, ServerPlayer owner) {
        Arena.floor(helper);
        var golem = helper.spawnWithNoFreeWill(ModEntities.BASALT_GOLEM.get(), CENTER);
        golem.notifyCreated(Blocks.BASALT.defaultBlockState(), owner);
        golem.setHomePos(helper.absolutePos(CENTER));
        return golem;
    }

    private static void command(ServerPlayer player, BasaltGolem golem) {
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GOLEM_CORE.get(), 1));
        player.interactOn(golem, InteractionHand.MAIN_HAND, Vec3.ZERO);
    }

    /** V2-T8 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void mixedBasaltDeepslatePillar(GameTestHelper helper) {
        var player = activate(helper, Blocks.BASALT, Blocks.DEEPSLATE);
        var golem = helper.findOneEntity(ModEntities.BASALT_GOLEM.get());
        helper.assertValueEqual(player.getUUID(), golem.getOwnerId(), "owner", "owner is the creator");
        helper.assertValueEqual(ShieldMode.FOLLOW, golem.getMode(), "mode", "owned golem starts following");
        helper.assertValueEqual(Blocks.DEEPSLATE, golem.getSourceBlock().getBlock(), "source", "source is the bottom block");
        Arena.removePlayer(helper, player);
        helper.succeed();
    }

    /** V2-T16 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void strangerCannotChangeMode(GameTestHelper helper) {
        var owner = Arena.survivalPlayer(helper, OWNER_SPOT);
        var golem = ownedGolem(helper, owner);
        var stranger = Arena.survivalPlayer(helper, new Vec3(7.5, 1, 7.5));
        helper.assertTrue(!stranger.getUUID().equals(owner.getUUID()), "distinct players");
        command(stranger, golem);
        helper.assertValueEqual(ShieldMode.FOLLOW, golem.getMode(), "mode", "stranger must not change the mode");
        Arena.removePlayer(helper, stranger);
        Arena.removePlayer(helper, owner);
        helper.succeed();
    }

    /** V2-T17 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void ownerSwitchesToGuard(GameTestHelper helper) {
        var owner = Arena.survivalPlayer(helper, OWNER_SPOT);
        var golem = ownedGolem(helper, owner);
        var moved = helper.absoluteVec(new Vec3(6.5, 1, 6.5));
        golem.teleportTo(moved.x, moved.y, moved.z);
        command(owner, golem);
        helper.assertValueEqual(ShieldMode.GUARD, golem.getMode(), "mode", "owner switches to guard");
        helper.assertValueEqual(golem.blockPosition(), golem.getHomePos(), "home", "home is the current position");
        helper.assertTrue(golem.hasHome(), "guard restricts the golem to its home");
        Arena.removePlayer(helper, owner);
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void followHasNoHomeRestrictionAndGuardRestoresIt(GameTestHelper helper) {
        var owner = Arena.survivalPlayer(helper, OWNER_SPOT);
        var golem = ownedGolem(helper, owner);
        helper.assertTrue(!golem.hasHome(), "following golems are not restricted to a home");
        command(owner, golem);
        helper.assertTrue(golem.hasHome(), "guarding golems are restricted");
        command(owner, golem);
        helper.assertValueEqual(ShieldMode.FOLLOW, golem.getMode(), "mode", "back to follow with the owner online");
        helper.assertTrue(!golem.hasHome(), "restriction off again");
        Arena.removePlayer(helper, owner);
        helper.succeed();
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void cannotFollowWithOwnerOffline(GameTestHelper helper) {
        var owner = Arena.survivalPlayer(helper, OWNER_SPOT);
        var golem = ownedGolem(helper, owner);
        command(owner, golem);
        helper.assertValueEqual(ShieldMode.GUARD, golem.getMode(), "mode", "now guarding");
        Arena.removePlayer(helper, owner);
        helper.assertTrue(golem.getOwner() == null, "owner resolves to null when offline");
        helper.assertTrue(golem.getOwnerId() != null, "owner id is kept");
        command(owner, golem);
        helper.assertValueEqual(ShieldMode.GUARD, golem.getMode(), "mode", "cannot follow an offline owner");
        helper.succeed();
    }

    /** V2-T18 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void dropsSourceBlock(GameTestHelper helper) {
        var player = activate(helper, Blocks.POLISHED_BASALT, Blocks.POLISHED_BASALT);
        var golem = helper.findOneEntity(ModEntities.BASALT_GOLEM.get());
        golem.setCrumbleDropChanceOverride(1.0);
        golem.consumeIntegrity(100);
        helper.assertTrue(golem.isRemoved(), "golem crumbled");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, golem.getBoundingBox().inflate(3.0),
            item -> item.getItem().is(Items.POLISHED_BASALT)).isEmpty(), "polished basalt dropped");
        Arena.removePlayer(helper, player);
        helper.succeed();
    }

    /** V2-T20 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void eggGolemIsGuardAndIgnoresCommands(GameTestHelper helper) {
        Arena.floor(helper);
        var golem = helper.spawnWithNoFreeWill(ModEntities.BASALT_GOLEM.get(), CENTER);
        helper.assertValueEqual(ShieldMode.GUARD, golem.getMode(), "mode", "no owner means guard");
        helper.assertTrue(golem.getOwnerId() == null, "no owner");
        helper.assertValueEqual(Blocks.BASALT, golem.getSourceBlock().getBlock(), "source", "default source");
        var player = Arena.survivalPlayer(helper, OWNER_SPOT);
        command(player, golem);
        helper.assertValueEqual(ShieldMode.GUARD, golem.getMode(), "mode", "commands are ignored");
        Arena.removePlayer(helper, player);
        helper.succeed();
    }

    /** Part of V2-T19 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void ownerModeSourceSurviveReload(GameTestHelper helper) {
        var owner = Arena.survivalPlayer(helper, OWNER_SPOT);
        Arena.floor(helper);
        var golem = helper.spawnWithNoFreeWill(ModEntities.BASALT_GOLEM.get(), CENTER);
        golem.notifyCreated(Blocks.DEEPSLATE.defaultBlockState(), owner);
        helper.assertValueEqual(ShieldMode.FOLLOW, golem.getMode(), "mode", "following before save");
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        golem.saveWithoutId(output);
        var tag = output.buildResult();
        var reloaded = ModEntities.BASALT_GOLEM.get().create(helper.getLevel(), EntitySpawnReason.LOAD);
        reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        helper.assertValueEqual(owner.getUUID(), reloaded.getOwnerId(), "owner", "owner reloaded");
        helper.assertValueEqual(ShieldMode.FOLLOW, reloaded.getMode(), "mode", "mode reloaded");
        helper.assertValueEqual(Blocks.DEEPSLATE, reloaded.getSourceBlock().getBlock(), "source", "source reloaded");
        helper.assertTrue(!reloaded.hasHome(), "following restriction stays off after reload");
        Arena.removePlayer(helper, owner);
        helper.succeed();
    }
}
