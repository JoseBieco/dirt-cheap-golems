package com.josebieco.dirtcheapgolems.entity;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.ai.HarvestAndReplantGoal;
import com.josebieco.dirtcheapgolems.entity.ai.SeekShelterFromRainGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** "Lavrador" (spec §6.1). */
public class DirtGolem extends AbstractCheapGolem {
    public DirtGolem(EntityType<? extends DirtGolem> type, Level level) {
        super(type, level);
    }

    private int environmentTicks;

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new SeekShelterFromRainGoal(this, 1.2));
        this.goalSelector.addGoal(3, new HarvestAndReplantGoal(this, 1.0));
        // Shelter beats strolling (spec 6.1): never wander off while it rains, or it walks out of cover.
        this.goalSelector.removeAllGoals(goal -> goal instanceof WaterAvoidingRandomStrollGoal);
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !DirtGolem.this.level().isRaining() && super.canUse();
            }
        });
    }

    /** Melts in rain (rainDecayPerSecond) and twice as fast in water, checked once per second. */
    @Override
    protected void environmentTick() {
        if (++environmentTicks < 20) return;
        environmentTicks = 0;
        int rate = GolemConfig.dirtRainDecayPerSecond();
        if (this.isInWater())
            consumeIntegrity(2 * rate);
        else if (this.level().isRainingAt(this.blockPosition().above()))
            consumeIntegrity(rate);
    }

    /** Never turns farmland back into dirt, whether walking or falling on it. */
    @Override
    public boolean canTrample(ServerLevel level, BlockState state, BlockPos pos, double fallDistance) {
        return false;
    }

    @Override
    protected BlockState getMaterialState() {
        return Blocks.DIRT.defaultBlockState();
    }

    @Override
    protected ItemStack getCrumbleDrop() {
        return new ItemStack(Items.DIRT);
    }

    @Override
    protected double getCrumbleDropChance() {
        return GolemConfig.dirtDropChance();
    }
}
