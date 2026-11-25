package dev.megarockets.client;

import dev.megarockets.MegaRockets;
import dev.megarockets.TitanRocket;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public final class TitanRenderer extends EntityRenderer<TitanRocket> {
    private static final Identifier TEXTURE = MegaRockets.id("textures/entity/titan_white.png");
    public TitanRenderer(EntityRendererFactory.Context context) { super(context); shadowRadius=0; }
    @Override public Identifier getTexture(TitanRocket rocket) { return TEXTURE; }
    @Override public void render(TitanRocket rocket, float yaw, float tickDelta, MatrixStack matrices,
                                  VertexConsumerProvider consumers, int light) {
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        MatrixStack.Entry matrix = matrices.peek();
        for (TitanMesh.Quad q : TitanMesh.QUADS) {
            for (TitanMesh.Point p : q.points()) {
                buffer.vertex(matrix,p.x(),p.y(),p.z()).color(q.argb()).texture(.5f,.5f)
                    .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix,q.nx(),q.ny(),q.nz());
            }
        }
        super.render(rocket,yaw,tickDelta,matrices,consumers,light);
    }
}
