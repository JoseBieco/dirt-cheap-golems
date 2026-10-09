package com.josebieco.dirtcheapgolems.entity.ai;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.AbstractCheapGolem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;

/** Dirt golem job (spec §6.1): walk to the nearest mature crop around home, harvest it and replant. */
public class HarvestAndReplantGoal extends Goal {
    private static final double REACH = 1.5;
    private static final int GIVE_UP_TICKS = 200;

    private final AbstractCheapGolem golem;
    private final double speed;
    private int scanCooldown;
    private @Nullable BlockPos target;
    private int ticksRunning;

    public HarvestAndReplantGoal(AbstractCheapGolem golem, double speed) {
        this.golem = golem;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--scanCooldown > 0) return false;
        scanCooldown = GolemConfig.dirtHarvestScanInterval();
        target = findNearestMatureCrop();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && ticksRunning < GIVE_UP_TICKS && isMatureCrop(golem.level().getBlockState(target))
            && !isRainedOn(target);
    }

    @Override
    public void start() {
        ticksRunning = 0;
        moveToTarget();
    }

    @Override
    public void stop() {
        target = null;
        golem.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) return;
        ticksRunning++;
        golem.getLookControl().setLookAt(Vec3.atCenterOf(target));
        if (golem.position().closerThan(Vec3.atBottomCenterOf(target), REACH)) {
            harvest(target);
            target = null;
        } else if (ticksRunning % 20 == 0 || golem.getNavigation().isDone()) {
            moveToTarget();
        }
    }

    private void moveToTarget() {
        if (target != null)
            golem.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
    }

    private @Nullable BlockPos findNearestMatureCrop() {
        var level = golem.level();
        var origin = golem.blockPosition();
        var home = golem.getHomePos();
        int radius = GolemConfig.dirtHarvestScanRadius();
        long homeRadiusSqr = (long) golem.effectiveHomeRadius() * golem.effectiveHomeRadius();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (var pos : BlockPos.betweenClosed(origin.offset(-radius, -2, -radius), origin.offset(radius, 2, radius))) {
            if (home != null && home.distSqr(pos) > homeRadiusSqr) continue;
            double dist = origin.distSqr(pos);
            if (dist > (double) radius * radius || dist >= bestDist) continue;
            if (!isMatureCrop(level.getBlockState(pos)) || isRainedOn(pos)) continue;
            best = pos.immutable();
            bestDist = dist;
        }
        return best;
    }

    /** Spec §6.1: sheltering from rain beats harvesting, so crops out in the rain wait. */
    private boolean isRainedOn(BlockPos crop) {
        return golem.level().isRainingAt(crop.above());
    }

    private static boolean isMatureCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    private void harvest(BlockPos pos) {
        if (!(golem.level() instanceof ServerLevel level)) return;
        var state = level.getBlockState(pos);
        // Revalidate: another golem (or a player) may have harvested it first. No cost then.
        if (!isMatureCrop(state)) return;

        var drops = new ArrayList<>(Block.getDrops(state, level, pos, null, golem, ItemStack.EMPTY));
        var seed = state.getCloneItemStack(level, pos, false);
        level.destroyBlock(pos, false, golem, 512);

        boolean replanted = false;
        for (var drop : drops) {
            if (!replanted && !seed.isEmpty() && ItemStack.isSameItem(drop, seed)) {
                drop.shrink(1);
                replanted = true;
            }
        }
        if (replanted) level.setBlockAndUpdate(pos, state.getBlock().defaultBlockState());

        for (var drop : drops)
            if (!drop.isEmpty()) Block.popResource(level, pos, drop);

        golem.consumeIntegrity(GolemConfig.dirtHarvestCost());
    }
}
