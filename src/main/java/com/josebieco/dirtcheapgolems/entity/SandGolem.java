package com.josebieco.dirtcheapgolems.entity;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.ai.StationaryTurretGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** "Torre" (spec §6.2): a stationary turret that never moves. */
public class SandGolem extends AbstractCheapGolem {
    public SandGolem(EntityType<? extends SandGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Follow range is the upper bound for targeting; the configured turret range is enforced by the target selector.
        return createBaseAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        // No movement goals at all: only aim, shoot and look around.
        this.goalSelector.addGoal(1, new StationaryTurretGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
            (target, level) -> target instanceof Enemy && this.distanceTo(target) <= GolemConfig.sandTurretRange()));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected BlockState getMaterialState() {
        return Blocks.SAND.defaultBlockState();
    }

    @Override
    protected ItemStack getCrumbleDrop() {
        return new ItemStack(Items.SAND);
    }

    @Override
    protected double getCrumbleDropChance() {
        return GolemConfig.sandDropChance();
    }
}
