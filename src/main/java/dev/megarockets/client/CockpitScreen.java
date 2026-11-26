package dev.megarockets.client;

import dev.megarockets.*;
import dev.megarockets.CockpitAction.Control;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.text.Text;
import java.util.EnumMap;
import java.util.Map;

public final class CockpitScreen extends Screen {
    public enum Tab {NAV,ENG,LIFE,SYS,VIEW}
    private final TitanRocket rocket;
    private Tab tab;
    private final Map<Destination,ButtonWidget> targets=new EnumMap<>(Destination.class);
    private final Map<Control,ButtonWidget> actions=new EnumMap<>(Control.class);
    private int x,y,w,h,accent;
    public CockpitScreen(TitanRocket rocket){this(rocket,Tab.NAV);}
    public CockpitScreen(TitanRocket rocket,Tab tab){super(Text.translatable("cockpit.megarockets.title"));this.rocket=rocket;this.tab=tab;}
    private Text tr(String key,Object... args){return Text.translatable("cockpit.megarockets."+key,args);}
    private void send(Control action){
        if(client!=null&&client.player!=null&&client.player.getVehicle()==rocket&&ClientPlayNetworking.canSend(CockpitAction.ID))
            ClientPlayNetworking.send(new CockpitAction(action.ordinal()));
    }
    private void command(String cmd){if(client!=null&&client.player!=null&&client.player.getVehicle()==rocket)client.player.networkHandler.sendChatCommand(cmd);}
    private void control(Control c,String label,int dx,int dy,int width){
        actions.put(c,addDrawableChild(ButtonWidget.builder(tr(label),b->send(c)).dimensions(x+dx,y+dy,width,20).build()));
    }
    private void selectTab(Tab t){tab=t;clearChildren();init();}
    @Override protected void init(){
        targets.clear();actions.clear();w=Math.min(width-12,324);h=224;
        x=(width-w)/2;y=Math.max(2,(height-h)/2);
        accent=rocket.getType()==MegaRockets.HYPERION?0xff65dcff:rocket.getType()==MegaRockets.PHANTOM?0xffff984f:0xfff17c6a;
        int tw=(w-12)/5;
        for(Tab t:Tab.values()){
            var b=addDrawableChild(ButtonWidget.builder(tr("tab_"+t.name().toLowerCase(java.util.Locale.ROOT)),v->selectTab(t)).dimensions(x+6+t.ordinal()*tw,y+25,tw-2,20).build());
            b.active=t!=tab;
        }
        int half=(w-18)/2;
        switch(tab){
            case NAV ->{
                int cw=(w-16)/4;
                for(Destination d:Destination.values()){
                    int i=d.ordinal();
                    targets.put(d,addDrawableChild(ButtonWidget.builder(Text.translatable("destination.megarockets."+d.key()),b->command("rocket destination "+d.key())).dimensions(x+8+(i%4)*cw,y+96+(i/4)*22,cw-2,20).build()));
                }
                control(Control.LAUNCH,"launch",8,164,half);
                control(Control.MANUAL,"manual",10+half,164,half);
                control(Control.AUTO_NAV,"auto_nav",8,186,half);
                control(Control.APPROACH,"approach",10+half,186,half);
            }
            case ENG ->{
                control(Control.ARM,"arm",8,119,half);control(Control.HOLD,"hold",10+half,119,half);
                control(Control.THROTTLE_DOWN,"throttle_down",8,143,half);control(Control.THROTTLE_UP,"throttle_up",10+half,143,half);
                control(Control.LAUNCH,"launch",8,167,w-16);
            }
            case LIFE -> control(Control.LIFE,"life",8,142,w-16);
            case SYS ->{
                control(Control.LIGHTS,"lights",8,105,half);control(Control.BEACON,"beacon",10+half,105,half);
                control(Control.HOLD,"hold",8,131,w-16);
            }
            case VIEW ->{
                camera("view_first",Perspective.FIRST_PERSON,96);
                camera("view_back",Perspective.THIRD_PERSON_BACK,120);
                camera("view_front",Perspective.THIRD_PERSON_FRONT,144);
            }
        }
        addDrawableChild(ButtonWidget.builder(tr("close"),b->close()).dimensions(x+w-59,y+3,52,18).build());
        tick();
    }
    private void camera(String key,Perspective view,int dy){
        addDrawableChild(ButtonWidget.builder(tr(key),b->{if(client!=null){client.options.setPerspective(view);close();}}).dimensions(x+8,y+dy,w-16,20).build());
    }
    private void state(Control c,boolean active,String key,Object... values){
        var b=actions.get(c);if(b!=null){b.active=active;b.setMessage(tr(key,values));}
    }
    private Text on(boolean b){return tr(b?"on":"off");}
    @Override public void tick(){
        if(client==null||client.player==null||client.player.getVehicle()!=rocket){close();return;}
        boolean ready=rocket.cabinReady(),orbit=rocket.getWorld().getRegistryKey().equals(MegaRockets.ORBIT),manual=rocket.manualMode(),armed=rocket.armed();
        for(var e:targets.entrySet()){
            e.getValue().active=ready||manual;
            e.getValue().setMessage(Text.literal(rocket.cabinTarget()==e.getKey()?"● ":"").append(Text.translatable("destination.megarockets."+e.getKey().key())));
        }
        state(Control.LAUNCH,ready&&armed,"launch");
        state(Control.MANUAL,true,"manual_state",on(manual&&!rocket.autoNavigation()));
        state(Control.AUTO_NAV,orbit&&armed,"auto_nav");
        state(Control.APPROACH,orbit&&manual&&armed,"approach");
        state(Control.HOLD,true,"hold");
        state(Control.ARM,true,"arm_state",on(armed));
        state(Control.THROTTLE_DOWN,rocket.throttle()>25,"throttle_down");
        state(Control.THROTTLE_UP,rocket.throttle()<100,"throttle_up");
        state(Control.LIFE,true,"life_state",on(rocket.lifeSupport()));
        state(Control.LIGHTS,true,"lights_state",on(rocket.cabinLights()));
        state(Control.BEACON,true,"beacon_state",on(rocket.beacon()));
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void renderBackground(DrawContext c,int mx,int my,float d){c.fill(0,0,width,height,0x9007111a);}
    private void line(DrawContext c,Text s,int dx,int dy,int color){c.drawTextWithShadow(textRenderer,s,x+dx,y+dy,color);}
    private void meter(DrawContext c,Text name,int value,int dy,int color){
        value=Math.max(0,Math.min(100,value));
        line(c,name,12,dy,0xffdce6ef);
        c.drawTextWithShadow(textRenderer,value+"%",x+w-12-textRenderer.getWidth(value+"%"),y+dy,color);
        c.fill(x+12,y+dy+11,x+w-12,y+dy+18,0xff263d4b);
        c.fill(x+12,y+dy+11,x+12+(w-24)*value/100,y+dy+18,color);
    }
    @Override public void render(DrawContext c,int mx,int my,float d){
        renderBackground(c,mx,my,d);c.fill(x,y,x+w,y+h,0xff111f2b);c.drawBorder(x,y,w,h,accent);
        line(c,rocket.getName(),10,8,accent);
        c.fill(x+6,y+49,x+w-6,y+(tab==Tab.NAV?93:tab==Tab.ENG?115:tab==Tab.LIFE?138:tab==Tab.SYS?101:92),0xff071721);
        int white=0xffdce6ef,green=0xff82e8ad,amber=0xffffc779,red=0xffff6858;
        switch(tab){
            case NAV ->{
                line(c,rocket.cabinPhase(),12,54,accent);
                line(c,tr("speed",rocket.cabinSpeed()),12,66,white);
                line(c,rocket.groundClearance()>=0?tr("agl",rocket.groundClearance()):tr("altitude",(int)rocket.getY()),w/2,66,white);
                line(c,rocket.entryProgress()>=0?tr("entry_loading",rocket.entryProgress()):rocket.groundClearance()>=0?tr("manual_landing_hint"):tr("manual_target",(int)rocket.cabinTarget().distance,(int)rocket.cabinTarget().lane),12,78,amber);
            }
            case ENG ->{
                meter(c,tr("velocity"),(int)(100*rocket.cabinSpeed()/(rocket.maxSpeed()*20)),53,accent);
                meter(c,tr("heat"),rocket.engineHeat(),74,rocket.engineHeat()>80?red:amber);
                meter(c,tr("manual_throttle"),rocket.throttle(),95,green);
                line(c,tr("engine_state",on(rocket.enginesRunning())),12,192,rocket.enginesRunning()?green:amber);
            }
            case LIFE ->{
                meter(c,tr("pressure"),rocket.cabinPressure(),54,rocket.cabinBreathable()?green:red);
                int oxygen=0;boolean tank=false;
                if(client!=null&&client.player!=null){var stack=client.player.getEquippedStack(EquipmentSlot.CHEST);tank=stack.isOf(SpaceSuitItems.CHEST);if(tank)oxygen=OxygenSystem.oxygen(stack)*100/OxygenRules.CAPACITY;}
                meter(c,tr(tank?"suit_oxygen":"no_tank"),oxygen,79,tank?accent:amber);
                line(c,tr(rocket.cabinBreathable()?"breathable":"vacuum_warning"),12,105,rocket.cabinBreathable()?green:red);
                line(c,tr("life_state",on(rocket.lifeSupport())),12,119,white);
                line(c,tr("life_hint"),12,170,amber);
            }
            case SYS ->{
                line(c,tr("lights_state",on(rocket.cabinLights())),12,55,white);
                line(c,tr("beacon_state",on(rocket.beacon())),12,69,white);
                line(c,tr("arm_state",on(rocket.armed())),12,83,white);
                line(c,tr("hold_hint"),12,160,amber);
            }
            case VIEW ->{
                line(c,tr("view_hint"),12,57,white);
                line(c,tr("view_hint2"),12,72,accent);
            }
        }
        line(c,tab==Tab.NAV&&rocket.getWorld().getRegistryKey().equals(MegaRockets.ORBIT)?tr("earth_gate",(int)ApproachRules.EARTH_X,(int)ApproachRules.EARTH_Z):tr(rocket.cabinBreathable()?"pressurized":"vacuum_warning"),10,211,rocket.cabinBreathable()?green:red);
        super.render(c,mx,my,d);
    }
}
