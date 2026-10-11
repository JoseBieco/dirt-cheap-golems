package com.josebieco.dirtcheapgolems.entity.ai;

import com.josebieco.dirtcheapgolems.entity.BasaltGolem;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/** Stands between a protected player and the nearest ranged attacker aiming at them. */
public class InterceptThreatGoal extends Goal {
    private final BasaltGolem golem;
    private @Nullable Vec3 point;
    private int recalc;

    public InterceptThreatGoal(BasaltGolem golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        this.point = golem.interceptPoint();
        return point != null;
    }

    @Override
    public boolean canContinueToUse() {
        return point != null && !golem.isRemoved();
    }

    @Override
    public void start() {
        this.recalc = 0;
    }

    @Override
    public void stop() {
        this.point = null;
        golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (point != null) {
            double dist = golem.position().distanceTo(new Vec3(point.x, golem.getY(), point.z));
            // Last stretch: the path ends at a block centre, so steer straight onto the exact point.
            if (dist < 1.0 && dist > 0.03) golem.getMoveControl().setWantedPosition(point.x, point.y, point.z, 0.4);
        }
        if (--recalc > 0) return;
        recalc = this.adjustedTickDelay(10);
        this.point = golem.interceptPoint();
        if (point == null) return; // canContinueToUse ends the goal
        if (golem.position().distanceToSqr(point.x, golem.getY(), point.z) > 1.0)
            golem.getNavigation().moveTo(point.x, point.y, point.z, 0, 1.0);
    }
}
