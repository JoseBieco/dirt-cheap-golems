package com.josebieco.dirtcheapgolems.item;

import com.josebieco.dirtcheapgolems.golem.GolemActivation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Golem Core (spec §5.1): used on a material pillar to create a golem. */
public class GolemCoreItem extends Item {
    public GolemCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        return GolemActivation.tryActivate(context.getLevel(), context.getClickedPos(), player, context.getItemInHand());
    }
}
