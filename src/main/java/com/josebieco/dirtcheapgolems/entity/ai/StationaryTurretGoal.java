package com.josebieco.dirtcheapgolems.entity.ai;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.SandGolem;
import com.josebieco.dirtcheapgolems.entity.projectile.SandBlast;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Sand Golem turret (spec §6.2): fires a {@link SandBlast} at a visible target every turretCooldown ticks. */
public class StationaryTurretGoal extends Goal {
    private final SandGolem golem;
    private int cooldown;

    public StationaryTurretGoal(SandGolem golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        var target = golem.getTarget();
        return target != null && target.isAlive() && golem.hasLineOfSight(target);
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
        if (--cooldown > 0) return;
        cooldown = GolemConfig.sandTurretCooldown();
        fire(target);
    }

    private void fire(LivingEntity target) {
        var blast = new SandBlast(golem.level(), golem);
        double dx = target.getX() - golem.getX();
        double dz = target.getZ() - golem.getZ();
        double dy = target.getY(0.5) - blast.getY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        blast.shoot(dx, dy + horizontal * 0.05, dz, SandBlast.SPEED, 1.0F);
        golem.level().addFreshEntity(blast);
        golem.playSound(SoundEvents.SAND_BREAK, 1.0F, 0.6F);
        golem.consumeIntegrity(GolemConfig.sandShotCost());
    }
}
