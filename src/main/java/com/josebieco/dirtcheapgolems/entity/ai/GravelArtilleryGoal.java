package com.josebieco.dirtcheapgolems.entity.ai;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.GravelGolem;
import com.josebieco.dirtcheapgolems.entity.projectile.FlintShard;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Gravel Golem artillery (spec v2 6.2): lobs a {@link FlintShard} at a visible target every artilleryCooldown ticks. */
public class GravelArtilleryGoal extends Goal {
    private final GravelGolem golem;
    private int cooldown;

    public GravelArtilleryGoal(GravelGolem golem) {
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
        cooldown = GolemConfig.gravelArtilleryCooldown();
        fire(target);
    }

    private void fire(LivingEntity target) {
        var shard = new FlintShard(golem.level(), golem);
        double dx = target.getX() - golem.getX();
        double dz = target.getZ() - golem.getZ();
        double dy = target.getY(0.3333333333333333) - shard.getY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        // Height compensation like the skeleton, but derived from the shard's gravity: the shard falls about
        // g * t * (t + 1) / 2 over t = horizontal / SPEED ticks, so aim that much higher.
        double flightTicks = horizontal / FlintShard.SPEED;
        double drop = FlintShard.GRAVITY * flightTicks * (flightTicks + 1.0) / 2.0;
        shard.shoot(dx, dy + drop, dz, FlintShard.SPEED, 1.0F);
        golem.level().addFreshEntity(shard);
        golem.playSound(SoundEvents.GRAVEL_HIT, 1.0F, 0.8F);
        golem.consumeIntegrity(GolemConfig.gravelShotCost());
    }
}
