package com.josebieco.dirtcheapgolems.registry;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import com.josebieco.dirtcheapgolems.entity.AbstractCheapGolem;
import com.josebieco.dirtcheapgolems.entity.DirtGolem;
import com.josebieco.dirtcheapgolems.entity.SandGolem;
import com.josebieco.dirtcheapgolems.entity.projectile.SandBlast;
import net.minecraft.world.entity.Entity;
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

    public static final RegistryObject<EntityType<DirtGolem>> DIRT_GOLEM = golem("dirt_golem", DirtGolem::new);
    public static final RegistryObject<EntityType<SandGolem>> SAND_GOLEM = golem("sand_golem", SandGolem::new);

    public static final RegistryObject<EntityType<SandBlast>> SAND_BLAST = ENTITIES.register("sand_blast",
        () -> EntityType.Builder.<SandBlast>of(SandBlast::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build(ENTITIES.key("sand_blast")));

    private ModEntities() {}

    private static <T extends Entity> RegistryObject<EntityType<T>> golem(String name, EntityType.EntityFactory<T> factory) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC)
            .sized(0.6F, 0.9F)
            .clientTrackingRange(8)
            .build(ENTITIES.key(name)));
    }

    public static void register(BusGroup modBusGroup) {
        ENTITIES.register(modBusGroup);
        EntityAttributeCreationEvent.BUS.addListener(ModEntities::createAttributes);
    }

    private static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(DIRT_GOLEM.get(), AbstractCheapGolem.createBaseAttributes().build());
        event.put(SAND_GOLEM.get(), SandGolem.createAttributes().build());
    }
}
