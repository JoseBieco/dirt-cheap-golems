package com.josebieco.dirtcheapgolems.registry;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import com.josebieco.dirtcheapgolems.entity.AbstractCheapGolem;
import com.josebieco.dirtcheapgolems.entity.DirtGolem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DirtCheapGolems.MOD_ID);

    public static final RegistryObject<EntityType<DirtGolem>> DIRT_GOLEM = ENTITIES.register("dirt_golem",
        () -> EntityType.Builder.of(DirtGolem::new, MobCategory.MISC)
            .sized(0.6F, 0.9F)
            .clientTrackingRange(8)
            .build(ENTITIES.key("dirt_golem")));

    private ModEntities() {}

    public static void register(BusGroup modBusGroup) {
        ENTITIES.register(modBusGroup);
        EntityAttributeCreationEvent.BUS.addListener(ModEntities::createAttributes);
    }

    private static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(DIRT_GOLEM.get(), AbstractCheapGolem.createBaseAttributes().build());
    }
}
