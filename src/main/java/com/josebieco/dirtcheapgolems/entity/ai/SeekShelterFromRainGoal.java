package com.josebieco.dirtcheapgolems.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/** While it rains on the mob, walk to a covered spot inside its home area (like {@code FleeSunGoal}). */
public class SeekShelterFromRainGoal extends Goal {
    private static final int ATTEMPTS = 10;
    private static final int RADIUS = 10;

    private final PathfinderMob mob;
    private final double speed;
    private @Nullable Vec3 shelter;

    public SeekShelterFromRainGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!isExposed()) return false;
        shelter = findShelter();
        return shelter != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (shelter != null) mob.getNavigation().moveTo(shelter.x, shelter.y, shelter.z, speed);
    }

    @Override
    public void stop() {
        shelter = null;
    }

    private boolean isExposed() {
        return mob.level().isRainingAt(mob.blockPosition().above());
    }

    private @Nullable Vec3 findShelter() {
        var level = mob.level();
        var random = mob.getRandom();
        var origin = mob.blockPosition();
        for (int i = 0; i < ATTEMPTS; i++) {
            BlockPos pos = origin.offset(random.nextInt(2 * RADIUS) - RADIUS, random.nextInt(6) - 3, random.nextInt(2 * RADIUS) - RADIUS);
            if (!mob.isWithinHome(pos)) continue;
            if (level.canSeeSky(pos.above())) continue;
            if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) continue;
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
            return Vec3.atBottomCenterOf(pos);
        }
        return null;
    }
}
