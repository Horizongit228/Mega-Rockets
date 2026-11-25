package dev.megarockets.client;

import dev.megarockets.TitanRocket;
import dev.megarockets.Destination;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class CockpitScreen extends Screen {
    private final TitanRocket rocket;
    private ButtonWidget moon,mars,launch;
    public CockpitScreen(TitanRocket rocket){super(Text.translatable("cockpit.megarockets.title"));this.rocket=rocket;}
    @Override protected void init(){
        int x=width/2-136,y=height/2-90;
        moon=addDrawableChild(ButtonWidget.builder(Text.translatable("destination.megarockets.moon"),b->command("rocket destination moon")).dimensions(x+12,y+108,118,20).build());
        mars=addDrawableChild(ButtonWidget.builder(Text.translatable("destination.megarockets.mars"),b->command("rocket destination mars")).dimensions(x+142,y+108,118,20).build());
        launch=addDrawableChild(ButtonWidget.builder(Text.translatable("cockpit.megarockets.launch"),b->{command("rocket launch");close();}).dimensions(x+12,y+135,118,20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"),b->close()).dimensions(x+142,y+135,118,20).build());
    }
    private void command(String cmd){
        if(client!=null&&client.player!=null&&client.player.getVehicle()==rocket)client.player.networkHandler.sendChatCommand(cmd);
    }
    @Override public void tick(){
        if(client==null||client.player==null||client.player.getVehicle()!=rocket){close();return;}
        boolean ready=rocket.cabinReady();moon.active=ready;mars.active=ready;launch.active=ready;
        moon.setMessage(Text.literal(rocket.cabinTarget()==Destination.MOON?"● ":"○ ").append(Text.translatable("destination.megarockets.moon")));
        mars.setMessage(Text.literal(rocket.cabinTarget()==Destination.MARS?"● ":"○ ").append(Text.translatable("destination.megarockets.mars")));
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void renderBackground(DrawContext context,int mouseX,int mouseY,float delta){context.fill(0,0,width,height,0x9007111a);}
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){
        renderBackground(context,mouseX,mouseY,delta);
        int x=width/2-136,y=height/2-90;
        context.fill(x,y,x+272,y+180,0xff152332);
        context.drawBorder(x,y,272,180,0xff73a8c0);
        context.fill(x+8,y+27,x+264,y+95,0xff08171e);
        context.drawCenteredTextWithShadow(textRenderer,title,width/2,y+10,0xffcaefff);
        context.drawTextWithShadow(textRenderer,rocket.getName(),x+15,y+34,0xffd1e5ef);
        context.drawTextWithShadow(textRenderer,Text.translatable("cockpit.megarockets.speed",rocket.cabinSpeed()),x+15,y+47,0xff72e6f2);
        context.drawTextWithShadow(textRenderer,Text.translatable("cockpit.megarockets.altitude",(int)rocket.getY()),x+145,y+47,0xff72e6f2);
        context.drawTextWithShadow(textRenderer,rocket.cabinPhase(),x+15,y+60,0xffdfc079);
        context.fill(x+15,y+77,x+257,y+86,0xff314653);
        context.fill(x+15,y+77,x+15+242*rocket.cabinProgress()/100,y+86,0xff47c2c8);
        context.drawCenteredTextWithShadow(textRenderer,Text.translatable("cockpit.megarockets.pressurized"),width/2,y+163,0xff87e5ac);
        super.render(context,mouseX,mouseY,delta);
    }
}
