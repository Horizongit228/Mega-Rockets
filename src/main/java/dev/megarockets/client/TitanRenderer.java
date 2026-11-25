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
        if (rocket.getY() >= 320) light = 0xF000F0; // Prevent a dark hull beyond the world lighting sections.
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        MatrixStack.Entry matrix = matrices.peek();
        for (TitanMesh.Quad q : TitanMesh.QUADS) {
            for (TitanMesh.Point p : q.points()) {
                buffer.vertex(matrix,p.x(),p.y(),p.z()).color(q.argb()).texture(.5f,.5f)
                    .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix,q.nx(),q.ny(),q.nz());
            }
        }
        if (rocket.enginesRunning()) {
            // Full-bright geometry, independent of particle distance and settings.
            VertexConsumer exhaust=consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
            float t=rocket.age+tickDelta;
            for(int i=0;i<7;i++) {
                double a=i*Math.PI/3;
                float x=i==6?0:2.8f*(float)Math.cos(a), z=i==6?0:2.8f*(float)Math.sin(a);
                float length=12f+.7f*(float)Math.sin(t*.9+i*1.3)+.3f*(float)Math.sin(t*2.1+i);
                plume(exhaust,matrix,x,z,-.02f,.70f,-2.1f,.88f,0xfffff0bd);
                plume(exhaust,matrix,x,z,-2.1f,.88f,-5.2f,.60f,0xffffa429);
                plume(exhaust,matrix,x,z,-5.2f,.60f,-8.2f,.34f,0xffff6518);
                plume(exhaust,matrix,x,z,-8.2f,.34f,-length,.015f,0xffdc3812);
            }
        }
        super.render(rocket,yaw,tickDelta,matrices,consumers,light);
    }
    private static void plume(VertexConsumer out, MatrixStack.Entry matrix,float x,float z,float y0,float r0,float y1,float r1,int color) {
        for(int i=0;i<16;i++) {
            double a=i*Math.PI/8,b=(i+1)*Math.PI/8;
            float nx=(float)Math.cos((a+b)/2),nz=(float)Math.sin((a+b)/2);
            flameVertex(out,matrix,x,z,y0,r0,a,nx,nz,color);
            flameVertex(out,matrix,x,z,y1,r1,a,nx,nz,color);
            flameVertex(out,matrix,x,z,y1,r1,b,nx,nz,color);
            flameVertex(out,matrix,x,z,y0,r0,b,nx,nz,color);
        }
    }
    private static void flameVertex(VertexConsumer out,MatrixStack.Entry m,float x,float z,float y,float r,double a,float nx,float nz,int color) {
        out.vertex(m,x+r*(float)Math.cos(a),y,z+r*(float)Math.sin(a)).color(color).texture(.5f,.5f)
            .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(m,nx,0,nz);
    }
}
