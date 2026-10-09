package com.josebieco.dirtcheapgolems.client;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only registrations. Only loaded on {@link Dist#CLIENT}; common code never references this package. */
@Mod.EventBusSubscriber(modid = DirtCheapGolems.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CheapGolemModel.LAYER, CheapGolemModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DIRT_GOLEM.get(),
            context -> new CheapGolemRenderer<>(context, CheapGolemRenderer.texture("dirt_golem")));
    }
}
