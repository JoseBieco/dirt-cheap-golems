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
    /** System property set by every Gradle dev run; absent in a released game. */
    public static final String GAMETESTS_PROPERTY = "dirtcheapgolems.gametests";

    public DirtCheapGolems(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        ModEntities.register(modBusGroup);
        ModDataComponents.register(modBusGroup);
        ModItems.register(modBusGroup);
        ModEffects.register(modBusGroup);
        // GameTest functions exist only in dev runs (build.gradle sets this property);
        // players' games never see them in /test.
        if (Boolean.getBoolean(GAMETESTS_PROPERTY)) ModGameTests.register(modBusGroup);
        GatherDataEvent.getBus(modBusGroup).addListener(DataGenerators::gatherData);

        context.registerConfig(ModConfig.Type.COMMON, GolemConfig.SPEC);
    }
}
