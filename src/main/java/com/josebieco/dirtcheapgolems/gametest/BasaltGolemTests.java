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

    // ---- projectile magnet and melee hits (spec v2 6.3) ----

    private static net.minecraft.world.entity.projectile.arrow.Arrow arrowFrom(GameTestHelper helper,
            net.minecraft.world.entity.Entity owner, Vec3 relative) {
        var arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(helper.getLevel(),
            (net.minecraft.world.entity.LivingEntity) owner, new ItemStack(Items.ARROW), null);
        var pos = helper.absoluteVec(relative);
        arrow.setPos(pos.x, pos.y, pos.z);
        arrow.setDeltaMovement(Vec3.ZERO);
        arrow.setNoGravity(true);
        helper.getLevel().addFreshEntity(arrow);
        return arrow;
    }

    /** V2-T13 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void absorbsSkeletonArrow(GameTestHelper helper) {
        Arena.floor(helper);
        var golem = helper.spawnWithNoFreeWill(ModEntities.BASALT_GOLEM.get(), CENTER);
        var skeleton = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.SKELETON, new BlockPos(7, 1, 7));
        skeleton.setNoAi(true);
        var arrow = arrowFrom(helper, skeleton, new Vec3(6.0, 2.0, 4.5));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(arrow.isRemoved(), "arrow absorbed");
            helper.assertTrue(golem.getIntegrity() >= 95 && golem.getIntegrity() <= 96,
                "arrow costs 4 integrity, got " + golem.getIntegrity());
            helper.succeed();
        });
    }

    /** An arrow already stuck in a block is inert: the magnet must not charge integrity for it. */
    @GameTest(structure = Arena.STRUCTURE)
    public static void ignoresArrowStuckInGround(GameTestHelper helper) throws Exception {
        Arena.floor(helper);
        var golem = helper.spawnWithNoFreeWill(ModEntities.BASALT_GOLEM.get(), CENTER);
        var skeleton = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.SKELETON, new BlockPos(7, 1, 7));
        skeleton.setNoAi(true);
        helper.setBlock(new BlockPos(6, 2, 4), Blocks.STONE);   // keeps the arrow stuck (it falls off if nothing is there)
        var arrow = arrowFrom(helper, skeleton, new Vec3(6.0, 2.0, 4.5));
        var setInGround = net.minecraft.world.entity.projectile.arrow.AbstractArrow.class
            .getDeclaredMethod("setInGround", boolean.class);
        setInGround.setAccessible(true);
        setInGround.invoke(arrow, true);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!arrow.isRemoved(), "stuck arrow untouched");
            helper.assertTrue(golem.getIntegrity() == 100, "no integrity lost, got " + golem.getIntegrity());
            helper.succeed();
        });
    }

    /** Two hits in the same tick: the second falls inside invulnerability frames and must not charge. */
    @GameTest(structure = Arena.STRUCTURE)
    public static void meleeHitCostsOnlyWhenItLands(GameTestHelper helper) {
        Arena.floor(helper);
        var golem = helper.spawnWithNoFreeWill(ModEntities.BASALT_GOLEM.get(), CENTER);
        var zombie = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.HUSK, new BlockPos(7, 1, 7));
        zombie.setNoAi(true);
        var source = helper.getLevel().damageSources().mobAttack(zombie);
        golem.hurtServer(helper.getLevel(), source, 3.0F);
        golem.hurtServer(helper.getLevel(), source, 3.0F);
        helper.assertTrue(golem.getIntegrity() >= 97 && golem.getIntegrity() <= 98,
            "one landed hit costs 2, got " + golem.getIntegrity());
        helper.succeed();
    }

    /** V2-T14 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void ignoresPlayerArrow(GameTestHelper helper) {
        Arena.floor(helper);
        var golem = helper.spawnWithNoFreeWill(ModEntities.BASALT_GOLEM.get(), CENTER);
        var player = Arena.survivalPlayer(helper, new Vec3(7.5, 1, 7.5));
        var arrow = arrowFrom(helper, player, new Vec3(6.0, 2.0, 4.5));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!arrow.isRemoved(), "player arrow untouched");
            helper.assertTrue(golem.getIntegrity() >= 99, "no integrity lost, got " + golem.getIntegrity());
            Arena.removePlayer(helper, player);
            helper.succeed();
        });
    }

    /** V2-T15 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void absorbsGhastFireballWithoutExplosion(GameTestHelper helper) {
        Arena.floor(helper);
        var golem = helper.spawnWithNoFreeWill(ModEntities.BASALT_GOLEM.get(), CENTER);
        var ghast = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.GHAST, new BlockPos(1, 2, 1));
        ghast.setNoAi(true);
        var toGolem = new Vec3(-0.3, 0.0, 0.0);
        var fireball = new net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball(
            helper.getLevel(), ghast, toGolem, 3);
        var pos = helper.absoluteVec(new Vec3(6.5, 2.0, 4.5));
        fireball.setPos(pos.x, pos.y, pos.z);
        fireball.setDeltaMovement(toGolem);
        helper.getLevel().addFreshEntity(fireball);
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(fireball.isRemoved(), "fireball absorbed");
            helper.assertTrue(golem.getIntegrity() >= 84 && golem.getIntegrity() <= 85,
                "large fireball costs 15, got " + golem.getIntegrity());
            for (int x = 2; x <= 6; x++)
                for (int z = 2; z <= 6; z++)
                    helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 0, z));
            helper.succeed();
        });
    }

    /** Review Focus 5 */
    @GameTest(structure = Arena.STRUCTURE)
    public static void ignoresSnowGolemSnowball(GameTestHelper helper) {
        Arena.floor(helper);
        helper.spawnWithNoFreeWill(ModEntities.BASALT_GOLEM.get(), CENTER);
        var snowGolem = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.SNOW_GOLEM, new BlockPos(7, 1, 7));
        snowGolem.setNoAi(true);
        var snowball = new net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball(
            helper.getLevel(), snowGolem, new ItemStack(Items.SNOWBALL));
        var pos = helper.absoluteVec(new Vec3(6.0, 2.0, 4.5));
        snowball.setPos(pos.x, pos.y, pos.z);
        snowball.setDeltaMovement(Vec3.ZERO);
        snowball.setNoGravity(true);
        helper.getLevel().addFreshEntity(snowball);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!snowball.isRemoved(), "snowball from a snow golem untouched");
            helper.succeed();
        });
    }

    @GameTest(structure = Arena.STRUCTURE)
    public static void meleeHitCostsIntegrity(GameTestHelper helper) {
        Arena.floor(helper);
        var golem = helper.spawnWithNoFreeWill(ModEntities.BASALT_GOLEM.get(), CENTER);
        var zombie = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.HUSK, new BlockPos(7, 1, 7));
        zombie.setNoAi(true);
        golem.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(zombie), 1.0F);
        helper.assertTrue(golem.getIntegrity() >= 97 && golem.getIntegrity() <= 98,
            "melee hit costs 2, got " + golem.getIntegrity());
        helper.succeed();
    }

    // ---- follow and intercept (spec v2 6.3) ----

    /** Owned FOLLOW golem with its goals active (helper.spawn, not spawnWithNoFreeWill). */
    private static BasaltGolem followingGolem(GameTestHelper helper, ServerPlayer owner, BlockPos at) {
        Arena.floor(helper);
        var golem = helper.spawn(ModEntities.BASALT_GOLEM.get(), at);
        golem.notifyCreated(Blocks.BASALT.defaultBlockState(), owner);
        return golem;
    }

    @GameTest(structure = Arena.STRUCTURE, maxTicks = 200)
    public static void followsOwner(GameTestHelper helper) {
        var owner = Arena.survivalPlayer(helper, new Vec3(1.5, 1, 4.5));
        var golem = followingGolem(helper, owner, new BlockPos(7, 1, 4));
        helper.assertTrue(golem.distanceTo(owner) > 5.0, "starts about 6 blocks away");
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(golem.distanceTo(owner) <= 3.5, "golem reached its owner, distance " + golem.distanceTo(owner));
            Arena.removePlayer(helper, owner);
            helper.succeed();
        });
    }

    @GameTest(structure = Arena.STRUCTURE, maxTicks = 200)
    public static void stopsWhenOwnerTooFar(GameTestHelper helper) {
        var owner = Arena.survivalPlayer(helper, new Vec3(34.5, 1, 4.5));
        var golem = followingGolem(helper, owner, CENTER);
        var start = golem.position();
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(golem.position().distanceTo(start) < 1.0,
                "golem must wait, moved " + golem.position().distanceTo(start));
            helper.assertValueEqual(ShieldMode.FOLLOW, golem.getMode(), "mode", "still following");
            Arena.removePlayer(helper, owner);
            helper.succeed();
        });
    }

    /** Review Focus 4 */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 200)
    public static void ownerLeavesWhileFollowing(GameTestHelper helper) {
        var owner = Arena.survivalPlayer(helper, new Vec3(1.5, 1, 4.5));
        var golem = followingGolem(helper, owner, new BlockPos(7, 1, 4));
        helper.runAfterDelay(10, () -> {
            Arena.removePlayer(helper, owner);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(!golem.isRemoved() && golem.isAlive(), "golem survives the owner leaving");
                helper.assertValueEqual(ShieldMode.FOLLOW, golem.getMode(), "mode", "still following");
                helper.assertTrue(golem.getOwner() == null, "owner offline");
                helper.succeed();
            });
        });
    }

    /** The skeleton wears a helmet so it does not burn in the sunlit arena. */
    @GameTest(structure = Arena.STRUCTURE, maxTicks = 300)
    public static void positionsBetweenOwnerAndSkeleton(GameTestHelper helper) {
        var owner = Arena.survivalPlayer(helper, new Vec3(1.5, 1, 4.5));
        var golem = followingGolem(helper, owner, new BlockPos(5, 1, 7));
        var skeleton = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.SKELETON, new BlockPos(7, 1, 4));
        skeleton.setNoAi(true);
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        skeleton.setTarget(owner);
        helper.runAfterDelay(120, () -> {
            var rel = helper.relativeVec(golem.position());
            helper.assertTrue(rel.x >= 2.0 && rel.x <= 4.0 && rel.z >= 3.3 && rel.z <= 4.7,
                "golem between owner and skeleton, at " + rel);
            Arena.removePlayer(helper, owner);
            helper.succeed();
        });
    }
}
