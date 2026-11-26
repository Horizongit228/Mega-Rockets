package dev.megarockets.client;

import dev.megarockets.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class CockpitControls {
    private CockpitControls(){}
    private static KeyBinding key(String name,int code){return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.megarockets."+name,InputUtil.Type.KEYSYM,code,"key.categories.megarockets"));}
    public static void init(){
        KeyBinding screen=key("cockpit",GLFW.GLFW_KEY_R),manual=key("manual",GLFW.GLFW_KEY_M),
            approach=key("approach",GLFW.GLFW_KEY_L),brake=key("brake",GLFW.GLFW_KEY_B);
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            while(screen.wasPressed())if(client.player!=null&&client.currentScreen==null&&client.player.getVehicle() instanceof TitanRocket rocket)client.setScreen(new CockpitScreen(rocket));
            while(manual.wasPressed())if(client.player!=null&&client.currentScreen==null&&client.player.getVehicle() instanceof TitanRocket)client.player.networkHandler.sendChatCommand("rocket manual");
            while(approach.wasPressed())if(client.player!=null&&client.currentScreen==null&&client.player.getVehicle() instanceof TitanRocket)client.player.networkHandler.sendChatCommand("rocket approach");
            if(client.player==null||!(client.player.getVehicle() instanceof TitanRocket rocket)||!rocket.manualMode()||!ClientPlayNetworking.canSend(ManualInput.ID))return;
            int mask=0;
            if(client.currentScreen==null){
                if(client.options.forwardKey.isPressed())mask|=ManualFlight.FORWARD;
                if(client.options.backKey.isPressed())mask|=ManualFlight.BACK;
                if(client.options.leftKey.isPressed())mask|=ManualFlight.LEFT;
                if(client.options.rightKey.isPressed())mask|=ManualFlight.RIGHT;
                if(client.options.jumpKey.isPressed())mask|=ManualFlight.UP;
                if(client.options.sprintKey.isPressed())mask|=ManualFlight.DOWN;
                if(brake.isPressed())mask|=ManualFlight.BRAKE;
            }
            ClientPlayNetworking.send(new ManualInput(mask));
        });
    }
}
