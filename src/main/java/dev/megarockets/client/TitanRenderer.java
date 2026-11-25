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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.Frustum;

public final class TitanRenderer extends EntityRenderer<TitanRocket> {
    private static final Identifier TEXTURE = MegaRockets.id("textures/entity/titan_hull.png");
    private static final Identifier WHITE = MegaRockets.id("textures/entity/titan_white.png");
    public TitanRenderer(EntityRendererFactory.Context context) { super(context); shadowRadius=0; }
    @Override public boolean shouldRender(TitanRocket rocket,Frustum frustum,double x,double y,double z) {
        // Tilted hull and exhaust can extend outside the original vertical collision box.
        if(rocket.hasPassengers() && rocket.squaredDistanceTo(x,y,z)<256*256) return true;
        return super.shouldRender(rocket,frustum,x,y,z);
    }
    @Override public Identifier getTexture(TitanRocket rocket) { return TEXTURE; }
    @Override public void render(TitanRocket rocket, float yaw, float tickDelta, MatrixStack matrices,
                                  VertexConsumerProvider consumers, int light) {
        if (rocket.getY() >= 320) light = 0xF000F0; // Prevent a dark hull beyond the world lighting sections.
        matrices.push();
        matrices.translate(0,32,0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rocket.getPitch(tickDelta)));
        matrices.translate(0,-32,0);
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        MatrixStack.Entry matrix = matrices.peek();
        for (TitanMesh.Quad q : TitanMesh.QUADS) {
            for (int i=0;i<4;i++) {
                TitanMesh.Point p=q.points()[i];
                int c=q.argb();
                int faceLight=c==0xff59e4ff||c==0xffff7962?0xF000F0:light;
                buffer.vertex(matrix,p.x(),p.y(),p.z()).color(c).texture(i==0||i==3?.02f:.98f,i<2?.02f:.98f)
                    .overlay(OverlayTexture.DEFAULT_UV).light(faceLight).normal(matrix,q.nx(),q.ny(),q.nz());
            }
        }
        if (rocket.enginesRunning()) {
            // Full-bright geometry, independent of particle distance and settings.
            VertexConsumer exhaust=consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE));
            float t=rocket.age+tickDelta;
            for(int i=0;i<7;i++) {
                double a=i*Math.PI/3;
                float x=i==6?0:2.8f*(float)Math.cos(a), z=i==6?0:2.8f*(float)Math.sin(a);
                boolean vacuum=!rocket.getWorld().getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)||rocket.getY()>1000;
                float length=28f+1.1f*(float)Math.sin(t*.7+i*1.3)+.5f*(float)Math.sin(t*1.6+i);
                float spread=vacuum?1.25f:1f;
                plume(exhaust,matrix,x,z,-.02f,.70f,-2.8f,.86f,0xfffff9e9);
                plume(exhaust,matrix,x,z,-2.8f,.86f,-7.2f,1.02f*spread,0xffffe5a4);
                plume(exhaust,matrix,x,z,-7.2f,1.02f*spread,-12f,.65f*spread,0xffffa133);
                plume(exhaust,matrix,x,z,-12f,.65f*spread,-length,.015f,0xffe64b16);
            }
        }
        matrices.pop();
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
