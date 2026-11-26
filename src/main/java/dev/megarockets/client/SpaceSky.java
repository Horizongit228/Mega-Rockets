package dev.megarockets.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.megarockets.MegaRockets;
import dev.megarockets.Destination;
import dev.megarockets.ApproachRules;
import dev.megarockets.SolarScale;
import dev.megarockets.SolarDynamics;
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
        if(world.getRegistryKey().equals(MegaRockets.ORBIT)||MegaRockets.planet(world)!=null)return 1;
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
        var body=MegaRockets.planet(world);
        boolean atmosphere=body==Destination.MARS||body==Destination.VENUS||body==Destination.TITAN;
        double ticks=world.getTime()+delta;
        double local=body==null?world.getTimeOfDay()%24000:SolarDynamics.localTime(body,world.getTime());
        double sunAngle=2*Math.PI*local/24000;
        Vec3d sunDir=orbit?new Vec3d(-pos.x,30-pos.y,-pos.z).normalize():new Vec3d(-Math.cos(sunAngle),Math.sin(sunAngle),0);
        double day=Math.max(0,Math.min(1,(sunDir.y+.16)/.4));
        double air=atmosphere?Math.max(0,Math.min(1,(world.getTopY()+160-pos.y)/180)):0;
        Vec3d sky=world.getSkyColor(pos,delta).multiply(1-dark);
        if(body==Destination.MARS)sky=new Vec3d(.42,.24,.14);
        if(body==Destination.VENUS)sky=new Vec3d(.60,.43,.23);
        if(body==Destination.TITAN)sky=new Vec3d(.45,.25,.10);
        if(atmosphere)sky=sky.multiply((.055+.945*day)*air);
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
                int c=((int)(dark*(atmosphere?240-195*day*air:240))<<24)|star.rgb;
                Vec3d q=center.subtract(a).subtract(b),w=center.add(a).subtract(b),e=center.add(a).add(b),r=center.subtract(a).add(b);
                v(stars,matrix,q,c);v(stars,matrix,w,c);v(stars,matrix,e,c);v(stars,matrix,q,c);v(stars,matrix,e,c);v(stars,matrix,r,c);
            }draw(stars);
            if(orbit||sunDir.y>-.09) sun(matrix,sunDir,atmosphere?.4+.6*(1-air):dark,ticks);
            if(orbit){
                java.util.List<Destination> targets=new java.util.ArrayList<>(java.util.Arrays.asList(Destination.values()));targets.add(null);
                targets.sort(java.util.Comparator.comparingDouble(d->-SolarDynamics.position(d,ticks).distance(pos.x,pos.z)));
                for(Destination target:targets){
                    var p=SolarDynamics.position(target,ticks);
                    double approach=p.distance(pos.x,pos.z);
                    Vec3d toward=new Vec3d(p.x()-pos.x,56-pos.y,p.z()-pos.z);
                    double radius=target==null?SolarScale.EARTH_RADIUS_KM:target.meanRadiusKm;
                    double angular=SolarScale.apparent(radius,approach);
                    double blend=Math.max(0,Math.min(1,(ApproachRules.LOAD_RADIUS-approach)/(ApproachRules.LOAD_RADIUS-ApproachRules.ENTRY_RADIUS)));
                    angular=angular*(1-blend)+((target!=null&&target.orbitOnly())?.67:1.05)*blend;
                    rotatingGlobe(matrix,toward.normalize(),angular,target,ticks,new Vec3d(-p.x(),0,-p.z()).normalize());
                }
            }else if(body!=null){
                if(body==Destination.EUROPA)rotatingGlobe(matrix,new Vec3d(.3,.65,-.7).normalize(),.32,Destination.JUPITER,ticks,sunDir);
                else if(body==Destination.TITAN)rotatingGlobe(matrix,new Vec3d(.3,.65,-.7).normalize(),.22,Destination.SATURN,ticks,sunDir);
                else if(body==Destination.MOON)rotatingGlobe(matrix,new Vec3d(-.8,.55,.4).normalize(),.18,null,ticks,sunDir);
                if(atmosphere&&air>.01)clouds(matrix,body,ticks,day,air);
            }else{
                rotatingGlobe(matrix,new Vec3d(0,-1,0),.95-.50*Math.max(0,Math.min(1,(pos.y-1200)/8800)),null,ticks,sunDir);
            }
        }finally{
            RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.setShaderColor(1,1,1,1);fog.run();
        }
    }
    private static void rotatingGlobe(Matrix4f matrix,Vec3d dir,double angular,Destination body,double ticks,Vec3d sun){
        if(dir.lengthSquared()<.001)return;
        Vec3d center=dir.multiply(45),u=axis(dir),w=dir.crossProduct(u).normalize();
        double radius=45*angular,spin=SolarDynamics.spin(body,ticks);
        String key=body==null?"earth":body.key();
        if(body==Destination.SATURN)rings(matrix,center,u,w,dir,radius,false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0,MegaRockets.id("textures/sky/"+key+"_map.png"));
        BufferBuilder disk=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_TEXTURE_COLOR);
        for(int j=0;j<12;j++)for(int i=0;i<48;i++){
            double r=j/12.,rr=(j+1)/12.,a=i*Math.PI/24,b=(i+1)*Math.PI/24;
            Vec3d[] normals={normal(dir,u,w,r,a),normal(dir,u,w,rr,a),normal(dir,u,w,rr,b),normal(dir,u,w,r,b)};
            Vec3d[] points={point(center,u,w,radius,r,a),point(center,u,w,radius,rr,a),point(center,u,w,radius,rr,b),point(center,u,w,radius,r,b)};
            double[] lon=new double[4];double min=10,max=-10;
            for(int k=0;k<4;k++){lon[k]=(Math.atan2(normals[k].z,normals[k].x)-spin)/(2*Math.PI)+.5;lon[k]-=Math.floor(lon[k]);min=Math.min(min,lon[k]);max=Math.max(max,lon[k]);}
            for(int k=0;k<4;k++){
                if(max-min>.5&&lon[k]<.5)lon[k]+=1;
                float lat=(float)(.5-Math.asin(Math.max(-1,Math.min(1,normals[k].y)))/Math.PI);
                int shade=(int)(255*(.13+.87*Math.max(0,normals[k].dotProduct(sun))));
                disk.vertex(matrix,(float)points[k].x,(float)points[k].y,(float)points[k].z).texture((float)lon[k],lat).color(shade,shade,shade,255);
            }
        }
        draw(disk);RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        double density=SolarDynamics.atmosphere(body);
        if(density>0){
            var b=begin();int rgb=body==null?0x66adff:body==Destination.MARS?0xca8056:0xe2b36a;
            for(int i=0;i<72;i++){double a=i*Math.PI/36,c=(i+1)*Math.PI/36;int alpha=((int)(70*Math.min(1,density))<<24)|rgb;
                Vec3d p=point(center,u,w,radius,1,a),q=point(center,u,w,radius,1,c),s=point(center,u,w,radius,1.045,c),t=point(center,u,w,radius,1.045,a);
                v(b,matrix,p,alpha);v(b,matrix,q,alpha);v(b,matrix,s,rgb);v(b,matrix,p,alpha);v(b,matrix,s,rgb);v(b,matrix,t,rgb);
            }draw(b);
        }
        if(body==Destination.SATURN)rings(matrix,center,u,w,dir,radius,true);
    }
    private static Vec3d normal(Vec3d dir,Vec3d u,Vec3d w,double r,double a){
        return u.multiply(r*Math.cos(a)).add(w.multiply(r*Math.sin(a))).subtract(dir.multiply(Math.sqrt(Math.max(0,1-r*r)))).normalize();
    }
    private static void rings(Matrix4f matrix,Vec3d center,Vec3d u,Vec3d w,Vec3d dir,double radius,boolean front){
        BufferBuilder b=begin();Vec3d tilt=w.multiply(.33).add(dir.multiply(.944));
        for(int i=0;i<72;i++){double a=i*Math.PI/36,c=(i+1)*Math.PI/36;if((Math.sin((a+c)/2)<0)!=front)continue;
            for(int j=0;j<9;j++){if(j==4)continue;double r=1.24+j*.10,s=r+.093;
                int shade=150+j*7,col=color(shade+34,shade+10,shade-19,205);
                Vec3d p=point(center,u,tilt,radius,r,a),q=point(center,u,tilt,radius,r,c),t=point(center,u,tilt,radius,s,c),vtx=point(center,u,tilt,radius,s,a);
                v(b,matrix,p,col);v(b,matrix,q,col);v(b,matrix,t,col);v(b,matrix,p,col);v(b,matrix,t,col);v(b,matrix,vtx,col);
            }
        }draw(b);
    }
    private static void clouds(Matrix4f matrix,Destination body,double ticks,double day,double air){
        for(int layer=0;layer<2;layer++){
            var b=begin();double drift=ticks*(layer==0?.00015:.00009);
            for(int i=0;i<48;i++)for(int j=0;j<12;j++){
                double a=i*Math.PI/24,c=(i+1)*Math.PI/24,p=j*Math.PI/24,q=(j+1)*Math.PI/24;
                Vec3d[] n={polar(a,p,1),polar(c,p,1),polar(c,q,1),polar(a,q,1)};
                int[] colors=new int[4];
                for(int k=0;k<4;k++){var v=n[k];double lon=Math.atan2(v.z,v.x)+drift,lat=Math.asin(v.y);
                    double noise=Math.sin(lon*7+Math.sin(lat*8+layer)*2)+.45*Math.sin(lon*15-lat*12);
                    double opacity=Math.max(0,Math.min(1,(noise+.4)*.65))*(body==Destination.MARS?.22:.64)*air;
                    double light=.15+.85*day;
                    colors[k]=color(220*light,body==Destination.MARS?135*light:181*light,98*light,(int)(255*opacity));
                }
                double r=layer==0?82:80;
                v(b,matrix,n[0].multiply(r),colors[0]);v(b,matrix,n[1].multiply(r),colors[1]);v(b,matrix,n[2].multiply(r),colors[2]);
                v(b,matrix,n[0].multiply(r),colors[0]);v(b,matrix,n[2].multiply(r),colors[2]);v(b,matrix,n[3].multiply(r),colors[3]);
            }draw(b);
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
    private static void globe(Matrix4f matrix,Vec3d dir,double angular,Identifier texture){
        Vec3d center=dir.multiply(45),u=axis(dir),w=dir.crossProduct(u).normalize();
        Vec3d right=u.multiply(45*angular),down=w.multiply(45*angular);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);RenderSystem.setShaderTexture(0,texture);
        BufferBuilder disk=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_TEXTURE_COLOR);
        textured(disk,matrix,center.subtract(right).subtract(down),0,0,255);
        textured(disk,matrix,center.add(right).subtract(down),1,0,255);
        textured(disk,matrix,center.add(right).add(down),1,1,255);
        textured(disk,matrix,center.subtract(right).add(down),0,1,255);
        draw(disk);RenderSystem.setShader(GameRenderer::getPositionColorProgram);
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
