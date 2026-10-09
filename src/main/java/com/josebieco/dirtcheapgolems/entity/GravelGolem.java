package com.josebieco.dirtcheapgolems.entity;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.ai.GravelArtilleryGoal;
import com.josebieco.dirtcheapgolems.entity.ai.LowestFirstTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "Artilheiro de Cerco" (spec v2 6.2). Artillery lives in {@link GravelArtilleryGoal}; the leap/collapse goal
 * is added on top in {@link #registerGoals()} (priority 1 is left free for it).
 */
public class GravelGolem extends AbstractCheapGolem {
    public GravelGolem(EntityType<? extends GravelGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Follow range is the upper bound for targeting; the configured range is enforced by the target selector.
        return createBaseAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.MOVEMENT_SPEED, 0.15)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new GravelArtilleryGoal(this));
        this.targetSelector.addGoal(1, new LowestFirstTargetGoal(this, 10, true,
            (target, level) -> target instanceof Enemy && this.distanceTo(target) <= GolemConfig.gravelArtilleryRange()));
    }

    @Override
    protected BlockState getMaterialState() {
        return Blocks.GRAVEL.defaultBlockState();
    }

    @Override
    protected ItemStack getCrumbleDrop() {
        return new ItemStack(Items.GRAVEL);
    }

    @Override
    protected double getCrumbleDropChance() {
        return GolemConfig.gravelDropChance();
    }
}
