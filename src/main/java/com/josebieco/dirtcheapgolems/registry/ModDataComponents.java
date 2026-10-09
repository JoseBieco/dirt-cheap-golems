package com.josebieco.dirtcheapgolems.registry;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DirtCheapGolems.MOD_ID);

    /** Container linked to a Golem Core (spec v2 §3.1). */
    public static final RegistryObject<DataComponentType<GlobalPos>> LINKED_CONTAINER = COMPONENTS.register("linked_container",
        () -> DataComponentType.<GlobalPos>builder()
            .persistent(GlobalPos.CODEC)
            .networkSynchronized(GlobalPos.STREAM_CODEC)
            .build());

    private ModDataComponents() {}

    public static void register(BusGroup modBusGroup) {
        COMPONENTS.register(modBusGroup);
    }
}
