package com.josebieco.dirtcheapgolems;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.datagen.DataGenerators;
import com.josebieco.dirtcheapgolems.gametest.ModGameTests;
import com.josebieco.dirtcheapgolems.registry.ModDataComponents;
import com.josebieco.dirtcheapgolems.registry.ModEffects;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import com.josebieco.dirtcheapgolems.registry.ModItems;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DirtCheapGolems.MOD_ID)
public final class DirtCheapGolems {
    public static final String MOD_ID = "dirtcheapgolems";

    public DirtCheapGolems(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        ModEntities.register(modBusGroup);
        ModDataComponents.register(modBusGroup);
        ModItems.register(modBusGroup);
        ModEffects.register(modBusGroup);
        ModGameTests.register(modBusGroup);
        GatherDataEvent.getBus(modBusGroup).addListener(DataGenerators::gatherData);

        context.registerConfig(ModConfig.Type.COMMON, GolemConfig.SPEC);
    }
}
