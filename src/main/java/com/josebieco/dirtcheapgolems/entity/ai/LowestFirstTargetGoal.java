package com.josebieco.dirtcheapgolems.entity.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.Comparator;

/**
 * Like {@link NearestAttackableTargetGoal} but ranks candidates by lowest block Y first, then nearest
 * (a golem on a wall prefers whoever is at the foot of it).
 */
public class LowestFirstTargetGoal extends NearestAttackableTargetGoal<Mob> {
    public LowestFirstTargetGoal(Mob mob, int randomInterval, boolean mustSee, TargetingConditions.Selector selector) {
        super(mob, Mob.class, randomInterval, mustSee, false, selector);
    }

    @Override
    protected void findTarget() {
        var level = (ServerLevel) this.mob.level();
        var conditions = this.targetConditions.range(this.getFollowDistance());
        Comparator<LivingEntity> order = Comparator.<LivingEntity>comparingInt(LivingEntity::getBlockY)
            .thenComparingDouble(e -> e.distanceToSqr(this.mob));
        this.target = this.mob.level()
            .getEntitiesOfClass(this.targetType, this.getTargetSearchArea(this.getFollowDistance()), e -> true)
            .stream()
            .filter(e -> conditions.test(level, this.mob, e))
            .min(order)
            .orElse(null);
    }
}
