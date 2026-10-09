package com.josebieco.dirtcheapgolems.registry;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import com.josebieco.dirtcheapgolems.effect.SandInEyesEffect;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, DirtCheapGolems.MOD_ID);

    public static final RegistryObject<MobEffect> SAND_IN_EYES = EFFECTS.register("sand_in_eyes", SandInEyesEffect::new);

    private ModEffects() {}

    public static Holder<MobEffect> sandInEyes() {
        return SAND_IN_EYES.getHolder().orElseThrow();
    }

    public static void register(BusGroup modBusGroup) {
        EFFECTS.register(modBusGroup);
    }
}
