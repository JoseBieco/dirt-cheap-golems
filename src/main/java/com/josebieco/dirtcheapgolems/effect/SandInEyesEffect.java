package com.josebieco.dirtcheapgolems.effect;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * "Sand in Eyes" (spec §6.2, D8): vanilla Blindness barely affects mobs, so this effect cuts the
 * mob's follow range by 75% and makes it drop its current target when applied.
 */
public class SandInEyesEffect extends MobEffect {
    public static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(DirtCheapGolems.MOD_ID, "sand_in_eyes");

    public SandInEyesEffect() {
        super(MobEffectCategory.HARMFUL, 0xDBCFA3);
        this.addAttributeModifier(Attributes.FOLLOW_RANGE, MODIFIER_ID, -0.75, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public void onEffectAdded(LivingEntity entity, int amplifier) {
        super.onEffectAdded(entity, amplifier);
        if (entity instanceof Mob mob) mob.setTarget(null);
    }
}
