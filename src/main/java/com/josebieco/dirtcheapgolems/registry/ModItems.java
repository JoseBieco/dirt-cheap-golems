package com.josebieco.dirtcheapgolems.registry;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import com.josebieco.dirtcheapgolems.item.GolemCoreItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DirtCheapGolems.MOD_ID);

    public static final RegistryObject<Item> GOLEM_CORE = ITEMS.register("golem_core",
        () -> new GolemCoreItem(new Item.Properties().setId(ITEMS.key("golem_core"))));

    public static final RegistryObject<Item> DIRT_GOLEM_SPAWN_EGG = spawnEgg("dirt_golem_spawn_egg", ModEntities.DIRT_GOLEM::get);
    public static final RegistryObject<Item> SAND_GOLEM_SPAWN_EGG = spawnEgg("sand_golem_spawn_egg", ModEntities.SAND_GOLEM::get);
    public static final RegistryObject<Item> NETHERRACK_GOLEM_SPAWN_EGG = spawnEgg("netherrack_golem_spawn_egg", ModEntities.NETHERRACK_GOLEM::get);
    public static final RegistryObject<Item> END_STONE_GOLEM_SPAWN_EGG = spawnEgg("end_stone_golem_spawn_egg", ModEntities.END_STONE_GOLEM::get);

    private ModItems() {}

    private static RegistryObject<Item> spawnEgg(String name, Supplier<? extends EntityType<?>> type) {
        return ITEMS.register(name, () -> new SpawnEggItem(new Item.Properties().spawnEgg(type.get()).setId(ITEMS.key(name))));
    }

    public static void register(BusGroup modBusGroup) {
        ITEMS.register(modBusGroup);
        BuildCreativeModeTabContentsEvent.BUS.addListener(ModItems::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS)
            event.accept(GOLEM_CORE);
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(DIRT_GOLEM_SPAWN_EGG);
            event.accept(SAND_GOLEM_SPAWN_EGG);
            event.accept(NETHERRACK_GOLEM_SPAWN_EGG);
            event.accept(END_STONE_GOLEM_SPAWN_EGG);
        }
    }
}
