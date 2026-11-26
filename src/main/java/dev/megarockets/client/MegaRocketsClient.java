package dev.megarockets.client;

import dev.megarockets.MegaRockets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class MegaRocketsClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        for(var glass:dev.megarockets.BuildingBlocks.glass())net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(glass,net.minecraft.client.render.RenderLayer.getTranslucent());
        CockpitControls.init();
        PlanetLoading.init();
        EntityRendererRegistry.register(MegaRockets.TITAN, TitanRenderer::new);
        EntityRendererRegistry.register(MegaRockets.PHANTOM,TitanRenderer::new);
        EntityRendererRegistry.register(MegaRockets.HYPERION,TitanRenderer::new);
    }
}
