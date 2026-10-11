package com.josebieco.dirtcheapgolems.entity.ai;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.GravelGolem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Gravel Golem leap (spec v2 6.2): jumps on a hostile that is close, or that is below and beside the golem
 * (it comes down off a wall), then collapses onto it on contact or on landing within 1.5 blocks.
 */
public class GravelLeapGoal extends Goal {
    private static final int MAX_FLIGHT_TICKS = 60;
    private static final int COOLDOWN = 20;
    private static final double COLLAPSE_RANGE = 1.5;

    private final GravelGolem golem;
    private int flightTicks;
    private int cooldown;

    public GravelLeapGoal(GravelGolem golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        var target = golem.getTarget();
        return target != null && target.isAlive() && golem.onGround() && inTriggerArea(target);
    }

    private boolean inTriggerArea(LivingEntity target) {
        if (golem.distanceTo(target) <= GolemConfig.gravelLeapTriggerRange()) return true;
        double dx = target.getX() - golem.getX();
        double dz = target.getZ() - golem.getZ();
        double depth = golem.getY() - target.getY();
        return depth > 0 && depth <= GolemConfig.gravelLeapBelowDepth()
            && Math.sqrt(dx * dx + dz * dz) <= GolemConfig.gravelLeapBelowHorizontal();
    }

    @Override
    public boolean canContinueToUse() {
        var target = golem.getTarget();
        return !golem.isRemoved() && target != null && target.isAlive() && flightTicks < MAX_FLIGHT_TICKS;
    }

    @Override
    public void start() {
        flightTicks = 0;
        var target = golem.getTarget();
        if (target == null) return;
        Vec3 toTarget = new Vec3(target.getX() - golem.getX(), 0.0, target.getZ() - golem.getZ());
        double horizontal = toTarget.length();
        // Air drag (0.91/tick) over a ~12 tick hop covers about 7.5x the launch speed; aim slightly past the target.
        double speed = Math.min(0.7, Math.max(0.25, (horizontal + 0.5) / 7.0));
        Vec3 dir = horizontal < 1.0E-4 ? Vec3.ZERO : toTarget.scale(speed / horizontal);
        golem.setDeltaMovement(dir.x, 0.45, dir.z);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        flightTicks++;
        var target = golem.getTarget();
        if (target == null) return;
        golem.getLookControl().setLookAt(target, 60.0F, 60.0F);
        // Touching (or close enough: entity pushing shoves a target away on contact) triggers the collapse.
        if (golem.distanceTo(target) <= COLLAPSE_RANGE) {
            golem.collapseOnto(target);
        } else if (flightTicks > 3 && golem.onGround()) {
            flightTicks = MAX_FLIGHT_TICKS; // landed short: give up this leap, retry after the cooldown
        }
    }

    @Override
    public void stop() {
        cooldown = COOLDOWN;
    }
}
