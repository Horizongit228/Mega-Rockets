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
    private static final Identifier TITAN= MegaRockets.id("textures/entity/titan_reborn.png");
    private static final Identifier PHANTOM= MegaRockets.id("textures/entity/phantom_reborn.png");
    private static final Identifier HYPERION= MegaRockets.id("textures/entity/hyperion_reborn.png");
    private static final Identifier WHITE = MegaRockets.id("textures/entity/titan_white.png");
    public TitanRenderer(EntityRendererFactory.Context context) { super(context); shadowRadius=0; }
    @Override public boolean shouldRender(TitanRocket rocket,Frustum frustum,double x,double y,double z) {
        // Tilted hull and exhaust can extend outside the original vertical collision box.
        if(rocket.hasPassengers() && rocket.squaredDistanceTo(x,y,z)<256*256) return true;
        return super.shouldRender(rocket,frustum,x,y,z);
    }
    @Override public Identifier getTexture(TitanRocket rocket) { return rocket.getType()==MegaRockets.HYPERION?HYPERION:rocket.getType()==MegaRockets.PHANTOM?PHANTOM:TITAN; }
    @Override public void render(TitanRocket rocket, float yaw, float tickDelta, MatrixStack matrices,
                                  VertexConsumerProvider consumers, int light) {
        if (rocket.getY() >= 320) light = 0xF000F0; // Prevent a dark hull beyond the world lighting sections.
        matrices.push();
        matrices.translate(0,32,0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rocket.getYaw(tickDelta)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rocket.getPitch(tickDelta)));
        matrices.translate(0,-32,0);
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(getTexture(rocket)));
        MatrixStack.Entry matrix = matrices.peek();
        boolean phantom=rocket.getType()==MegaRockets.PHANTOM,hyperion=rocket.getType()==MegaRockets.HYPERION;
        for (TitanMesh.Quad q : TitanMesh.QUADS) {
            for (int i=0;i<4;i++) {
                TitanMesh.Point p=q.points()[i];
                int c=q.argb();
                if(c==0xffe4e8ee)c=0xffffffff;
                if(c==0xff286583||c==0xff286083)c=hyperion?0xffd5ae64:phantom?0xffec7252:0xffc5d6e7;
                float taper=(float)Math.max(0,Math.min(1,Math.min((p.y()-5)/8,(48-p.y())/8)));
                float profile=1-taper*(phantom?.24f:hyperion?-.10f:0f);
                int faceLight=c==0xff59e4ff||c==0xffff7962?0xF000F0:light;
                // Map the shell as a cylindrical texture, not a full repeated bitmap per quad.
                float u=(float)(Math.atan2(p.z(),p.x())/(Math.PI*2)+.5);
                float anchor=(float)(Math.atan2(q.points()[0].z(),q.points()[0].x())/(Math.PI*2)+.5);
                if(u-anchor>.5f)u-=1;else if(anchor-u>.5f)u+=1;
                float vv=1-p.y()/64f;
                buffer.vertex(matrix,p.x()*profile,p.y(),p.z()*profile).color(c).texture(u,vv)
                    .overlay(OverlayTexture.DEFAULT_UV).light(faceLight).normal(matrix,q.nx(),q.ny(),q.nz());
            }
        }
        // Interior has independent emergency lighting; glass is genuinely translucent.
        VertexConsumer interior=consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE));
        for(TitanMesh.Quad q:TitanMesh.CABIN)emit(interior,matrix,q,0xF000F0);
        VertexConsumer glass=consumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE));
        for(TitanMesh.Quad q:TitanMesh.GLASS)emit(glass,matrix,q,0xF000F0);
        if (rocket.enginesRunning()) {
            // Full-bright geometry, independent of particle distance and settings.
            VertexConsumer exhaust=consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE));
            float t=rocket.age+tickDelta;
            for(int i=0;i<7;i++) {
                double a=i*Math.PI/3;
                float x=i==6?0:2.8f*(float)Math.cos(a), z=i==6?0:2.8f*(float)Math.sin(a);
                boolean vacuum=!rocket.getWorld().getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)||rocket.getY()>1000;
                float length=(hyperion?48f:phantom?38f:28f)+1.1f*(float)Math.sin(t*.7+i*1.3)+.5f*(float)Math.sin(t*1.6+i);
                float spread=vacuum?1.25f:1f;
                plume(exhaust,matrix,x,z,-.02f,.70f,-2.8f,.86f,hyperion?0xffe9fbff:0xfffff9e9);
                plume(exhaust,matrix,x,z,-2.8f,.86f,-7.2f,1.02f*spread,hyperion?0xffa9eaff:0xffffe5a4);
                plume(exhaust,matrix,x,z,-7.2f,1.02f*spread,-12f,.65f*spread,hyperion?0xff54b8ed:phantom?0xffff7544:0xffffa133);
                plume(exhaust,matrix,x,z,-12f,.65f*spread,-length,.015f,hyperion?0xff277bd0:phantom?0xffbd332a:0xffe64b16);
            }
        }
        matrices.pop();
        super.render(rocket,yaw,tickDelta,matrices,consumers,light);
    }
    private static void emit(VertexConsumer out,MatrixStack.Entry matrix,TitanMesh.Quad q,int light){
        for(int i=0;i<4;i++){
            TitanMesh.Point p=q.points()[i];
            out.vertex(matrix,p.x(),p.y(),p.z()).color(q.argb()).texture(.5f,.5f)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix,q.nx(),q.ny(),q.nz());
        }
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
