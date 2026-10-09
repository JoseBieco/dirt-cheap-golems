package com.josebieco.dirtcheapgolems.entity.ai;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.NetherrackGolem;
import com.josebieco.dirtcheapgolems.entity.ThermalBlast;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Netherrack Golem combat (spec §6.3): chase the target, light the fuse on contact, blow up when the fuse
 * burns out. The fuse is cancelled if the target gets more than {@link #CANCEL_DISTANCE} blocks away.
 */
public class KamikazeChargeGoal extends Goal {
    private static final double CONTACT_DISTANCE = 1.5;
    private static final double CANCEL_DISTANCE = 3.0;

    private final NetherrackGolem golem;
    private final double speedModifier;
    private int repathCooldown;

    public KamikazeChargeGoal(NetherrackGolem golem, double speedModifier) {
        this.golem = golem;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public KamikazeChargeGoal(NetherrackGolem golem) {
        this(golem, 1.3);
    }

    @Override
    public boolean canUse() {
        var target = golem.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        repathCooldown = 0;
    }

    @Override
    public void stop() {
        golem.cancelFuse();
        golem.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        var target = golem.getTarget();
        if (target == null) return;
        golem.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = golem.distanceTo(target);

        if (golem.getFuseTicks() > 0) {
            if (distance > CANCEL_DISTANCE) {
                golem.cancelFuse();
                return;
            }
            golem.getNavigation().stop();
            golem.advanceFuse();
            if (golem.getFuseTicks() >= GolemConfig.netherrackFuseTicks()) explode();
            return;
        }

        if (distance <= CONTACT_DISTANCE) {
            golem.getNavigation().stop();
            golem.startFuse();
        } else if (--repathCooldown <= 0 || golem.getNavigation().isDone()) {
            repathCooldown = 10;
            golem.getNavigation().moveTo(target, speedModifier);
        }
    }

    private void explode() {
        golem.cancelFuse();
        if (!(golem.level() instanceof ServerLevel level)) return;
        ThermalBlast.detonate(level, golem, GolemConfig.netherrackBlastRadius(), (float) GolemConfig.netherrackBlastDamage(),
            GolemConfig.netherrackBurnSeconds(), GolemConfig.netherrackBlastHurtsPlayers());
        golem.consumeIntegrity(GolemConfig.netherrackBlastCost());
    }
}
