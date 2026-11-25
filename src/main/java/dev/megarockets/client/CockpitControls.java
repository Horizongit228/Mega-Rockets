package dev.megarockets.client;

import dev.megarockets.TitanRocket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class CockpitControls {
    private CockpitControls(){}
    public static void init(){
        KeyBinding key=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.megarockets.cockpit",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_R,"key.categories.megarockets"));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            while(key.wasPressed()){
                if(client.player!=null&&client.currentScreen==null&&client.player.getVehicle() instanceof TitanRocket rocket)
                    client.setScreen(new CockpitScreen(rocket));
            }
        });
    }
}
