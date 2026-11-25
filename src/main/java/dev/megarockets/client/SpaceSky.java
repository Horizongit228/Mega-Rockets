package dev.megarockets.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.megarockets.MegaRockets;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import java.util.Random;

/** Camera-centered sky dome with procedural planets; no texture or shader-pack dependency. */
public final class SpaceSky {
    private record Star(Vec3d dir,double size,int rgb){}
    private static final Star[] STARS=stars();
    private SpaceSky(){}
    private static Star[] stars(){
        Random r=new Random(512041);Star[] out=new Star[2000];
        for(int i=0;i<out.length;i++){
            double y=r.nextDouble()*2-1,a=r.nextDouble()*Math.PI*2,s=Math.sqrt(1-y*y);
            int c=150+r.nextInt(106);
            out[i]=new Star(new Vec3d(s*Math.cos(a),y,s*Math.sin(a)),.024+Math.pow(r.nextDouble(),4)*.11,
                i%6==0?(c<<16)|((int)(c*.82)<<8)|(int)(c*.65):i%5==0?((int)(c*.72)<<16)|((int)(c*.85)<<8)|c:(c<<16)|(c<<8)|c);
        }return out;
    }
    public static double darkness(ClientWorld world,double y){
        if(world.getRegistryKey().equals(MegaRockets.ORBIT)||MegaRockets.isMoon(world))return 1;
        if(!world.getRegistryKey().equals(World.OVERWORLD))return 0;
        double t=Math.max(0,Math.min(1,(y-180)/1100));return t*t*(3-2*t);
    }
    private static int color(double r,double g,double b,int alpha){
        return (alpha<<24)|((int)Math.max(0,Math.min(255,r))<<16)|((int)Math.max(0,Math.min(255,g))<<8)|(int)Math.max(0,Math.min(255,b));
    }
    private static void v(BufferBuilder b,Matrix4f matrix,Vec3d pos,int color){b.vertex(matrix,(float)pos.x,(float)pos.y,(float)pos.z).color(color);}
    private static BufferBuilder begin(){return Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES,VertexFormats.POSITION_COLOR);}
    private static void draw(BufferBuilder b){BufferRenderer.drawWithGlobalProgram(b.end());}
    private static Vec3d axis(Vec3d dir){return dir.crossProduct(Math.abs(dir.y)>.95?new Vec3d(1,0,0):new Vec3d(0,1,0)).normalize();}
    private static Vec3d polar(double lon,double lat,double radius){double c=Math.cos(lat);return new Vec3d(radius*c*Math.cos(lon),radius*Math.sin(lat),radius*c*Math.sin(lon));}
    public static void render(Matrix4f matrix,Camera camera,ClientWorld world,float delta,Runnable fog){
        Vec3d pos=camera.getPos();double dark=darkness(world,pos.y);
        boolean orbit=world.getRegistryKey().equals(MegaRockets.ORBIT),moon=MegaRockets.isMoon(world);
        Vec3d sky=world.getSkyColor(pos,delta).multiply(1-dark);
        RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);RenderSystem.setShaderColor(1,1,1,1);
        BackgroundRenderer.clearFog();
        try {
            BufferBuilder dome=begin();
            int top=color(sky.x*255+2*dark,sky.y*255+3*dark,sky.z*255+8*dark,255);
            for(int i=0;i<24;i++)for(int j=0;j<12;j++){
                double a=i*Math.PI/12,b=(i+1)*Math.PI/12,c=-Math.PI/2+j*Math.PI/12,d=c+Math.PI/12;
                Vec3d q=polar(a,c,94),w=polar(b,c,94),e=polar(b,d,94),r=polar(a,d,94);
                v(dome,matrix,q,top);v(dome,matrix,w,top);v(dome,matrix,e,top);
                v(dome,matrix,q,top);v(dome,matrix,e,top);v(dome,matrix,r,top);
            }draw(dome);
            BufferBuilder stars=begin();
            for(Star star:STARS){
                Vec3d center=star.dir.multiply(85),a=axis(star.dir).multiply(star.size),b=star.dir.crossProduct(a).normalize().multiply(star.size);
                int c=((int)(dark*240)<<24)|star.rgb;
                Vec3d q=center.subtract(a).subtract(b),w=center.add(a).subtract(b),e=center.add(a).add(b),r=center.subtract(a).add(b);
                v(stars,matrix,q,c);v(stars,matrix,w,c);v(stars,matrix,e,c);v(stars,matrix,q,c);v(stars,matrix,e,c);v(stars,matrix,r,c);
            }draw(stars);
            if(orbit){
                planet(matrix,new Vec3d(-1,-.24,0).normalize(),Math.max(.12,.38-pos.x/30000),true,1);
                Vec3d toward=new Vec3d((6000.5+4600)-pos.x,56-pos.y,.5-pos.z);
                double radius=Math.min(.78,4096/Math.max(4600,toward.length()));
                planet(matrix,toward.normalize(),radius,false,1);
            }else if(moon){
                planet(matrix,new Vec3d(-.8,.55,.4).normalize(),.18,true,1);
            }else{
                planet(matrix,new Vec3d(0,-1,0),.80,true,dark);
            }
        }finally{
            RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.setShaderColor(1,1,1,1);fog.run();
        }
    }
    private static Vec3d point(Vec3d center,Vec3d u,Vec3d w,double radius,double r,double a){
        return center.add(u.multiply(radius*r*Math.cos(a))).add(w.multiply(radius*r*Math.sin(a)));
    }
    private static int planetColor(double r,double a,boolean earth,int alpha){
        double x=r*Math.cos(a),y=r*Math.sin(a),z=Math.sqrt(Math.max(0,1-r*r));
        double light=.22+.78*Math.max(0,.48*x+.45*y+.75*z);
        double noise=Math.sin(x*9+Math.cos(y*12))*Math.cos(y*11-x*4)+.45*Math.sin(x*24+y*16);
        if(earth){
            boolean land=noise>.28;double clouds=Math.sin(x*27+y*14+Math.sin(y*19))*Math.cos(y*26-x*7);
            double rr=land?73:28,gg=land?133:91,bb=land?65:195;
            if(clouds>.69||Math.abs(y)>.88){rr=214;gg=224;bb=239;}
            double rim=Math.pow(r,7)*.65;
            return color(rr*light+35*rim,gg*light+80*rim,bb*light+115*rim,alpha);
        }
        double gray=(145+25*noise)*light;
        return color(gray,gray*.98,gray*.95,alpha);
    }
    private static void planet(Matrix4f matrix,Vec3d dir,double angular,boolean earth,double alpha){
        Vec3d center=dir.multiply(45),u=axis(dir),w=dir.crossProduct(u).normalize();double radius=45*angular;
        BufferBuilder b=begin();int opacity=(int)(alpha*255);
        // Blue atmospheric limb on Earth only; the Moon is airless.
        if(earth)for(int i=0;i<96;i++){
            double a=i*Math.PI/48,c=(i+1)*Math.PI/48;
            Vec3d p=point(center,u,w,radius,1,a),q=point(center,u,w,radius,1,c),s=point(center,u,w,radius,1.12,c),t=point(center,u,w,radius,1.12,a);
            int inner=((int)(opacity*.72)<<24)|0x5eaaff,outer=0x005eaaff;
            v(b,matrix,p,inner);v(b,matrix,q,inner);v(b,matrix,s,outer);v(b,matrix,p,inner);v(b,matrix,s,outer);v(b,matrix,t,outer);
        }
        for(int ring=0;ring<36;ring++)for(int i=0;i<96;i++){
            double r=ring/36.0,s=(ring+1)/36.0,a=i*Math.PI/48,c=(i+1)*Math.PI/48;
            Vec3d p=point(center,u,w,radius,r,a),q=point(center,u,w,radius,r,c),t=point(center,u,w,radius,s,c),vtx=point(center,u,w,radius,s,a);
            int p1=planetColor(r,a,earth,opacity),p2=planetColor(r,c,earth,opacity),p3=planetColor(s,c,earth,opacity),p4=planetColor(s,a,earth,opacity);
            v(b,matrix,p,p1);v(b,matrix,q,p2);v(b,matrix,t,p3);v(b,matrix,p,p1);v(b,matrix,t,p3);v(b,matrix,vtx,p4);
        }draw(b);
    }
}
