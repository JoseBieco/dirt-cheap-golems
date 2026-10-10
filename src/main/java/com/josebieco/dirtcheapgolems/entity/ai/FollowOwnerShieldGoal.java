package com.josebieco.dirtcheapgolems.entity.ai;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.BasaltGolem;
import com.josebieco.dirtcheapgolems.entity.ShieldMode;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * FOLLOW mode: walks to within followDistance of the owner. Waits (navigation stopped) when the owner is
 * absent or farther than maxFollowDistance. Never teleports.
 */
public class FollowOwnerShieldGoal extends Goal {
    private final BasaltGolem golem;
    private @Nullable Player owner;
    private int recalc;

    public FollowOwnerShieldGoal(BasaltGolem golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean ownerInRange(Player owner) {
        double max = GolemConfig.basaltMaxFollowDistance();
        return this.golem.distanceToSqr(owner) <= max * max;
    }

    @Override
    public boolean canUse() {
        if (golem.getMode() != ShieldMode.FOLLOW) return false;
        Player candidate = golem.getOwner();
        if (candidate == null || !ownerInRange(candidate)) return false;
        double start = GolemConfig.basaltFollowDistance() + 0.5;
        if (golem.distanceToSqr(candidate) <= start * start) return false;
        this.owner = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (golem.getMode() != ShieldMode.FOLLOW || owner == null || owner.isRemoved()) return false;
        if (golem.getOwner() != owner || !ownerInRange(owner)) return false;
        double stop = GolemConfig.basaltFollowDistance();
        return golem.distanceToSqr(owner) > stop * stop;
    }

    @Override
    public void start() {
        this.recalc = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;
        golem.getLookControl().setLookAt(owner, 10.0F, golem.getMaxHeadXRot());
        if (--recalc <= 0) {
            recalc = this.adjustedTickDelay(10);
            golem.getNavigation().moveTo(owner, 1.0);
        }
    }
}
