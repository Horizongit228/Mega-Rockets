package dev.megarockets.client;

import dev.megarockets.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.Locale;
import java.util.Random;

/** Per-destination art direction, with honest server preload progress and indeterminate client sync. */
public final class PlanetLoading {
    private record Theme(String key,int background,int accent,double radiusKm,double gravity,boolean giant){}
    private static Destination lastTarget;
    private static final int[][] STARS=new int[170][3];
    static{
        Random r=new Random(97311);
        for(int[] s:STARS){s[0]=r.nextInt(4096);s[1]=r.nextInt(4096);s[2]=r.nextInt(3);}
    }
    private PlanetLoading(){}
    private static Theme theme(Destination d){
        if(d==null)return new Theme("earth",0xff071a2d,0xff72c5ef,SolarScale.EARTH_RADIUS_KM,1,false);
        int[] colors=switch(d){
            case MOON -> new int[]{0xff151a24,0xffc3d4e7};
            case MERCURY -> new int[]{0xff242125,0xffc9c0b7};
            case VENUS -> new int[]{0xff372b18,0xffffcc77};
            case MARS -> new int[]{0xff311b19,0xffff9974};
            case EUROPA -> new int[]{0xff0e273b,0xffb7e8f7};
            case TITAN -> new int[]{0xff352616,0xffeaba6b};
            case JUPITER -> new int[]{0xff32231e,0xffefbb9e};
            case SATURN -> new int[]{0xff2c2519,0xffe9d298};
            case URANUS -> new int[]{0xff082d35,0xff94e7e9};
            case NEPTUNE -> new int[]{0xff101c3a,0xff8aafff};
        };
        return new Theme(d.key(),colors[0],colors[1],d.meanRadiusKm,d.gravity,d.orbitOnly());
    }
    private static Text tr(String key,Object... args){return Text.translatable("loading.megarockets."+key,args);}
    private static Text name(Theme t){return Text.translatable("destination.megarockets."+t.key());}
    public static void init(){
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.player==null){lastTarget=null;return;}
            if(client.player.getVehicle() instanceof TitanRocket rocket)lastTarget=rocket.cabinTarget();
        });
        HudRenderCallback.EVENT.register((context,tickCounter)->hud(context));
    }
    /** Called only for the vanilla terrain downloading screen; its normal tick/close behavior remains. */
    public static boolean renderScreen(DrawContext c,int width,int height){
        var client=MinecraftClient.getInstance();if(client.world==null)return false;
        var key=client.world.getRegistryKey();
        boolean orbit=key.equals(MegaRockets.ORBIT);
        Destination d=orbit?lastTarget:MegaRockets.planet(client.world);
        if(!orbit&&d==null&&!key.equals(World.OVERWORLD))return false;
        Theme t=orbit&&d==null?new Theme("orbit",0xff08121f,0xff80c8f7,0,0,false):theme(d);
        c.fillGradient(0,0,width,height,0xff050912,t.background());
        for(int[] s:STARS){int x=s[0]*width/4096,y=s[1]*height/4096;int shade=s[2]==0?0xff3a4c60:s[2]==1?0xff73869a:0xffb5c3d3;c.fill(x,y,x+1,y+1,shade);}
        int size=Math.max(32,Math.min(130,Math.min(height-86,width/3)));
        int gx=width/2-size/2,gy=Math.max(22,(height-size-63)/2);
        // Distinct planet globe assets from the sky renderer; no duplicated texture files.
        if(!t.key().equals("orbit"))c.drawTexture(MegaRockets.id("textures/sky/"+t.key()+".png"),gx,gy,0f,0f,size,size,size,size);
        else{
            int mid=width/2,cy=gy+size/2;
            c.fill(mid-size/2,cy-1,mid+size/2,cy+1,t.accent());
            c.fill(mid-1,cy-size/2,mid+1,cy+size/2,t.accent());
        }
        c.drawCenteredTextWithShadow(client.textRenderer,Text.literal("MEGA ROCKETS  /  FLIGHT SYSTEM"),width/2,9,t.accent());
        c.drawCenteredTextWithShadow(client.textRenderer,t.key().equals("orbit")?tr("orbit"):name(t),width/2,Math.max(22,gy-13),0xfff1f5fa);
        int base=Math.min(height-60,gy+size+5);
        if(t.radiusKm()>0){
            String km=String.format(Locale.ROOT,"%.1f",t.radiusKm()*2);
            c.drawCenteredTextWithShadow(client.textRenderer,tr("facts",km,String.format(Locale.ROOT,"%.3f",t.gravity())),width/2,base,0xffd7e5f1);
            c.drawCenteredTextWithShadow(client.textRenderer,tr("tip."+t.key()),width/2,base+13,t.accent());
        }
        int bw=Math.max(48,Math.min(240,width-32)),bx=(width-bw)/2,by=height-25;
        c.fill(bx,by,bx+bw,by+4,0xff283b4b);
        int segment=Math.max(10,bw/4),offset=(int)((System.currentTimeMillis()/15)%(bw+segment))-segment;
        c.fill(bx+Math.max(0,offset),by,bx+Math.min(bw,offset+segment),by+4,t.accent());
        c.drawCenteredTextWithShadow(client.textRenderer,tr(orbit?"sync_orbit":"sync_surface"),width/2,height-16,0xffd7e5f1);
        return true;
    }
    private static void hud(DrawContext c){
        var client=MinecraftClient.getInstance();
        if(client.player==null||client.world==null||client.currentScreen!=null||client.options.hudHidden||
            !(client.player.getVehicle() instanceof TitanRocket rocket)||!client.world.getRegistryKey().equals(MegaRockets.ORBIT))return;
        double x=rocket.getX(),z=rocket.getZ();
        Destination nearest=null;boolean earth=false;double best=ApproachRules.LOAD_RADIUS;
        double e=SolarDynamics.position(null,client.world.getTime()).distance(x,z);
        if(e<=best){earth=true;best=e;}
        for(Destination d:Destination.values()){
            double dist=SolarDynamics.position(d,client.world.getTime()).distance(x,z);
            if(dist<best){nearest=d;earth=false;best=dist;}
        }
        if(nearest==null&&!earth)return;
        Theme t=theme(nearest);
        int progress=rocket.entryProgress();
        // Giant planets have only an orbital arrival; never pretend a terrain load is happening.
        if(progress<0&&!t.giant())return;
        int w=Math.min(230,client.getWindow().getScaledWidth()-12),left=(client.getWindow().getScaledWidth()-w)/2,y=8;
        c.fill(left,y,left+w,y+48,t.background());c.drawBorder(left,y,w,48,t.accent());
        c.drawTexture(MegaRockets.id("textures/sky/"+t.key()+".png"),left+5,y+5,0f,0f,36,36,36,36);
        c.drawTextWithShadow(client.textRenderer,name(t),left+47,y+5,t.accent());
        c.drawTextWithShadow(client.textRenderer,t.giant()?tr("orbit_only"):tr("preload",progress),left+47,y+18,0xffe2eaf2);
        if(!t.giant()){
            c.fill(left+47,y+33,left+w-7,y+38,0xff263b48);
            c.fill(left+47,y+33,left+47+(w-54)*Math.max(0,Math.min(100,progress))/100,y+38,t.accent());
        }else c.drawTextWithShadow(client.textRenderer,tr("range",(int)best),left+47,y+33,0xffd7e5f1);
    }
}
