package com.josebieco.dirtcheapgolems.entity;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
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
