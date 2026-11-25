package dev.megarockets.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.megarockets.MegaRockets;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import java.util.Random;

/** Camera-centered sky dome with procedural planets; no texture or shader-pack dependency. */
public final class SpaceSky {
    private record Star(Vec3d dir,double size,int rgb){}
    private static final Star[] STARS=stars();
    private static final Identifier EARTH= MegaRockets.id("textures/sky/earth.png");
    private static final Identifier MOON= MegaRockets.id("textures/sky/moon.png");
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
        if(world.getRegistryKey().equals(MegaRockets.ORBIT)||MegaRockets.isMoon(world)||world.getRegistryKey().equals(MegaRockets.MARS))return 1;
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
        boolean mars=world.getRegistryKey().equals(MegaRockets.MARS);
        Vec3d sky=world.getSkyColor(pos,delta).multiply(1-dark);
        if(mars)sky=new Vec3d(.42,.24,.14);
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
                int c=((int)(dark*(mars?45:240))<<24)|star.rgb;
                Vec3d q=center.subtract(a).subtract(b),w=center.add(a).subtract(b),e=center.add(a).add(b),r=center.subtract(a).add(b);
                v(stars,matrix,q,c);v(stars,matrix,w,c);v(stars,matrix,e,c);v(stars,matrix,q,c);v(stars,matrix,e,c);v(stars,matrix,r,c);
            }draw(stars);
            sun(matrix,new Vec3d(.24,.62,-.75).normalize(),dark,world.getTime()+delta);
            if(orbit){
                planet(matrix,new Vec3d(-1,-.24,0).normalize(),Math.max(.12,.38-pos.x/30000),true,1);
                Vec3d toward=new Vec3d((6000.5+4600)-pos.x,56-pos.y,.5-pos.z);
                double radius=Math.min(.78,4096/Math.max(4600,toward.length()));
                planet(matrix,toward.normalize(),radius,false,1);
                Vec3d redPlanet=new Vec3d(36050-pos.x,65-pos.y,4500-pos.z);
                marsDisk(matrix,redPlanet.normalize(),Math.min(.68,8192/Math.max(12050,redPlanet.length())));
            }else if(mars){
                planet(matrix,new Vec3d(-.8,.5,.3).normalize(),.035,true,1);
            }else if(moon){
                planet(matrix,new Vec3d(-.8,.55,.4).normalize(),.18,true,1);
            }else{
                planet(matrix,new Vec3d(0,-1,0),.95-.50*Math.max(0,Math.min(1,(pos.y-1200)/8800)),true,dark);
            }
        }finally{
            RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.setShaderColor(1,1,1,1);fog.run();
        }
    }
    private static Vec3d point(Vec3d center,Vec3d u,Vec3d w,double radius,double r,double a){
        return center.add(u.multiply(radius*r*Math.cos(a))).add(w.multiply(radius*r*Math.sin(a)));
    }
    // Render behind the planets so their disks can occult the Sun. All vertices
    // are camera-centered, so it remains visible independent of render distance.
    private static void sun(Matrix4f matrix,Vec3d dir,double alpha,double time){
        Vec3d center=dir.multiply(64),u=axis(dir),w=dir.crossProduct(u).normalize();
        double radius=2.8; int segments=96;
        BufferBuilder b=begin();
        // Soft extended corona, with subtly animated irregular rays.
        for(int ring=0;ring<7;ring++){
            double r0=1+ring*.28,r1=r0+.28;
            for(int i=0;i<segments;i++){
                double a=i*Math.PI*2/segments,c=(i+1)*Math.PI*2/segments;
                Vec3d p=point(center,u,w,radius,r0,a),q=point(center,u,w,radius,r0,c),
                    s=point(center,u,w,radius,r1,c),t=point(center,u,w,radius,r1,a);
                int p0=corona(r0,a,alpha,time),q0=corona(r0,c,alpha,time),
                    s0=corona(r1,c,alpha,time),t0=corona(r1,a,alpha,time);
                v(b,matrix,p,p0);v(b,matrix,q,q0);v(b,matrix,s,s0);
                v(b,matrix,p,p0);v(b,matrix,s,s0);v(b,matrix,t,t0);
            }
        }
        // Photosphere, with bright core and warmer limb; not a flat square sprite.
        for(int ring=0;ring<24;ring++){
            double r0=ring/24.0,r1=(ring+1)/24.0;
            for(int i=0;i<segments;i++){
                double a=i*Math.PI*2/segments,c=(i+1)*Math.PI*2/segments;
                Vec3d p=point(center,u,w,radius,r0,a),q=point(center,u,w,radius,r0,c),
                    s=point(center,u,w,radius,r1,c),t=point(center,u,w,radius,r1,a);
                int p0=photosphere(r0,a,alpha),q0=photosphere(r0,c,alpha),
                    s0=photosphere(r1,c,alpha),t0=photosphere(r1,a,alpha);
                v(b,matrix,p,p0);v(b,matrix,q,q0);v(b,matrix,s,s0);
                v(b,matrix,p,p0);v(b,matrix,s,s0);v(b,matrix,t,t0);
            }
        }
        draw(b);
    }
    private static int corona(double r,double a,double alpha,double time){
        double edge=Math.max(0,1-(r-1)/1.96);
        double rays=.72+.16*Math.sin(a*11+time*.004)+.12*Math.cos(a*23-time*.003);
        return color(255,155+70*edge,55+125*edge,(int)(alpha*150*edge*edge*rays));
    }
    private static int photosphere(double r,double a,double alpha){
        double x=r*Math.cos(a),y=r*Math.sin(a);
        double grain=Math.sin(x*43+Math.cos(y*31))*Math.cos(y*37-x*13);
        double limb=Math.pow(r,5);
        return color(255,247-48*limb+4*grain,218-100*limb+8*grain,(int)(255*alpha));
    }
    private static void marsDisk(Matrix4f matrix,Vec3d dir,double angular){
        Vec3d center=dir.multiply(45),u=axis(dir),w=dir.crossProduct(u).normalize();double radius=45*angular;
        BufferBuilder b=begin();
        for(int ring=0;ring<32;ring++)for(int i=0;i<96;i++){
            double r=ring/32.,s=(ring+1)/32.,a=i*Math.PI/48,c=(i+1)*Math.PI/48;
            Vec3d p=point(center,u,w,radius,r,a),q=point(center,u,w,radius,r,c),t=point(center,u,w,radius,s,c),vtx=point(center,u,w,radius,s,a);
            int p1=marsColor(r,a),p2=marsColor(r,c),p3=marsColor(s,c),p4=marsColor(s,a);
            v(b,matrix,p,p1);v(b,matrix,q,p2);v(b,matrix,t,p3);v(b,matrix,p,p1);v(b,matrix,t,p3);v(b,matrix,vtx,p4);
        }draw(b);
    }
    private static int marsColor(double r,double a){
        double x=r*Math.cos(a),y=r*Math.sin(a),z=Math.sqrt(Math.max(0,1-r*r));
        double light=.26+.74*Math.max(0,.45*x+.4*y+.79*z);
        double n=Math.sin(x*9+Math.cos(y*7))*Math.cos(y*11-x*4)+.23*Math.sin(x*29+y*17);
        if(Math.abs(y)>.88)return color(227*light,214*light,194*light,255);
        return color((175+34*n)*light,(91+22*n)*light,(57+15*n)*light,255);
    }
    private static void planet(Matrix4f matrix,Vec3d dir,double angular,boolean earth,double alpha){
        Vec3d center=dir.multiply(45),u=axis(dir),w=dir.crossProduct(u).normalize();double radius=45*angular;
        int opacity=(int)(alpha*255);
        // Blue atmospheric limb on Earth only; the Moon is airless.
        if(earth){
        BufferBuilder b=begin();
        for(int i=0;i<96;i++){
            double a=i*Math.PI/48,c=(i+1)*Math.PI/48;
            Vec3d p=point(center,u,w,radius,1,a),q=point(center,u,w,radius,1,c),s=point(center,u,w,radius,1.035,c),t=point(center,u,w,radius,1.035,a);
            int inner=((int)(opacity*.65)<<24)|0x5eaaff,outer=0x005eaaff;
            v(b,matrix,p,inner);v(b,matrix,q,inner);v(b,matrix,s,outer);v(b,matrix,p,inner);v(b,matrix,s,outer);v(b,matrix,t,outer);
        }
        draw(b);}
        // A real satellite mosaic projected onto an illuminated globe during the build.
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0,earth?EARTH:MOON);
        BufferBuilder disk=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_TEXTURE_COLOR);
        Vec3d right=u.multiply(radius),down=w.multiply(radius);
        textured(disk,matrix,center.subtract(right).subtract(down),0,0,opacity);
        textured(disk,matrix,center.add(right).subtract(down),1,0,opacity);
        textured(disk,matrix,center.add(right).add(down),1,1,opacity);
        textured(disk,matrix,center.subtract(right).add(down),0,1,opacity);
        draw(disk);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    }
    private static void textured(BufferBuilder b,Matrix4f matrix,Vec3d p,float u,float v,int a){
        b.vertex(matrix,(float)p.x,(float)p.y,(float)p.z).texture(u,v).color(255,255,255,a);
    }
}
