package dev.megarockets.client;

import dev.megarockets.TitanRocket;
import dev.megarockets.Destination;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.EnumMap;
import java.util.Map;

public final class CockpitScreen extends Screen {
    private final TitanRocket rocket;
    private final Map<Destination,ButtonWidget> targets=new EnumMap<>(Destination.class);
    private ButtonWidget launch;
    public CockpitScreen(TitanRocket rocket){super(Text.translatable("cockpit.megarockets.title"));this.rocket=rocket;}
    @Override protected void init(){
        targets.clear();int x=width/2-148,y=height/2-104;
        for(Destination d:Destination.values()){
            int i=d.ordinal();
            var button=addDrawableChild(ButtonWidget.builder(Text.translatable("destination.megarockets."+d.key()),b->command("rocket destination "+d.key())).dimensions(x+10+(i%3)*92,y+112+(i/3)*24,88,20).build());
            targets.put(d,button);
        }
        launch=addDrawableChild(ButtonWidget.builder(Text.translatable("cockpit.megarockets.launch"),b->{command("rocket launch");close();}).dimensions(x+10,y+165,135,20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"),b->close()).dimensions(x+151,y+165,135,20).build());
        tick();
    }
    private void command(String cmd){if(client!=null&&client.player!=null&&client.player.getVehicle()==rocket)client.player.networkHandler.sendChatCommand(cmd);}
    @Override public void tick(){
        if(client==null||client.player==null||client.player.getVehicle()!=rocket){close();return;}
        boolean ready=rocket.cabinReady();launch.active=ready;
        for(var e:targets.entrySet()){
            e.getValue().active=ready;
            e.getValue().setMessage(Text.literal(rocket.cabinTarget()==e.getKey()?"● ":"○ ").append(Text.translatable("destination.megarockets."+e.getKey().key())));
        }
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void renderBackground(DrawContext context,int mouseX,int mouseY,float delta){context.fill(0,0,width,height,0x9007111a);}
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){
        renderBackground(context,mouseX,mouseY,delta);
        int x=width/2-148,y=height/2-104;
        context.fill(x,y,x+296,y+208,0xff152332);
        context.drawBorder(x,y,296,208,0xff73a8c0);
        context.fill(x+8,y+26,x+288,y+103,0xff08171e);
        context.drawCenteredTextWithShadow(textRenderer,title,width/2,y+10,0xffcaefff);
        context.drawTextWithShadow(textRenderer,rocket.getName(),x+15,y+32,0xffd1e5ef);
        context.drawTextWithShadow(textRenderer,Text.translatable("cockpit.megarockets.speed",rocket.cabinSpeed()),x+15,y+44,0xff72e6f2);
        context.drawTextWithShadow(textRenderer,Text.translatable("cockpit.megarockets.altitude",(int)rocket.getY()),x+166,y+44,0xff72e6f2);
        context.drawTextWithShadow(textRenderer,rocket.cabinPhase(),x+15,y+56,0xffdfc079);
        context.drawTextWithShadow(textRenderer,Text.translatable("cockpit.megarockets.route", (int)rocket.cabinTarget().distance,rocket.cabinTarget().referenceKm),x+15,y+68,0xffd1e5ef);
        context.fill(x+15,y+86,x+281,y+96,0xff314653);
        context.fill(x+15,y+86,x+15+266*rocket.cabinProgress()/100,y+96,0xff47c2c8);
        context.drawCenteredTextWithShadow(textRenderer,Text.translatable("cockpit.megarockets.pressurized"),width/2,y+193,0xff87e5ac);
        super.render(context,mouseX,mouseY,delta);
    }
}
