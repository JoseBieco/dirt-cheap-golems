package com.josebieco.dirtcheapgolems.entity;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.ai.HarvestAndReplantGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
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

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new HarvestAndReplantGoal(this, 1.0));
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
