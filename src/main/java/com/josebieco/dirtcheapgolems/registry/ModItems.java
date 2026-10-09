package com.josebieco.dirtcheapgolems.registry;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import com.josebieco.dirtcheapgolems.item.GolemCoreItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DirtCheapGolems.MOD_ID);

    public static final RegistryObject<Item> GOLEM_CORE = ITEMS.register("golem_core",
        () -> new GolemCoreItem(new Item.Properties().setId(ITEMS.key("golem_core"))));

    private ModItems() {}

    public static void register(BusGroup modBusGroup) {
        ITEMS.register(modBusGroup);
    }
}
