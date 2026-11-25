package dev.megarockets.client;

import dev.megarockets.MegaRockets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class MegaRocketsClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(MegaRockets.TITAN, TitanRenderer::new);
        EntityRendererRegistry.register(MegaRockets.PHANTOM,TitanRenderer::new);
        EntityRendererRegistry.register(MegaRockets.HYPERION,TitanRenderer::new);
    }
}
