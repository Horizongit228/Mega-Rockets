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
    // Inverse of the hull's render transform; intersect the front instrument face.
    private static net.minecraft.util.math.Vec3d local(net.minecraft.util.math.Vec3d v,double yaw,double pitch){
        double a=Math.toRadians(yaw),b=Math.toRadians(-pitch);
        double x=v.x*Math.cos(a)+v.z*Math.sin(a),z=-v.x*Math.sin(a)+v.z*Math.cos(a);
        return new net.minecraft.util.math.Vec3d(x*Math.cos(b)-v.y*Math.sin(b),x*Math.sin(b)+v.y*Math.cos(b),z);
    }
    private static CockpitScreen.Tab instrumentTab(TitanRocket rocket,net.minecraft.util.math.Vec3d eye,net.minecraft.util.math.Vec3d ray){
        var origin=local(eye.subtract(rocket.getPos()).add(0,-32,0),rocket.getYaw(),rocket.getPitch()).add(0,32,0);
        var direction=local(ray,rocket.getYaw(),rocket.getPitch());
        if(Math.abs(direction.z)<1e-6)return CockpitScreen.Tab.NAV;
        double t=(-2.20-origin.z)/direction.z;
        double x=origin.x+direction.x*t,y=origin.y+direction.y*t;
        if(t<=0||t>8||Math.abs(x)>1.8||y<52.0||y>53.0)return CockpitScreen.Tab.NAV;
        return x<-.62?CockpitScreen.Tab.NAV:x>.62?CockpitScreen.Tab.LIFE:CockpitScreen.Tab.ENG;
    }
    private static KeyBinding key(String name,int code){return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.megarockets."+name,InputUtil.Type.KEYSYM,code,"key.categories.megarockets"));}
    public static void init(){
        KeyBinding map=key("solar_map",GLFW.GLFW_KEY_U),screen=key("cockpit",GLFW.GLFW_KEY_R),instrument=key("instrument",GLFW.GLFW_KEY_G),manual=key("manual",GLFW.GLFW_KEY_M),
            approach=key("approach",GLFW.GLFW_KEY_L),brake=key("brake",GLFW.GLFW_KEY_B);
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            while(map.wasPressed())if(client.player!=null&&client.currentScreen==null)client.setScreen(new SolarMapScreen(null));
            while(screen.wasPressed())if(client.player!=null&&client.currentScreen==null&&client.player.getVehicle() instanceof TitanRocket rocket)client.setScreen(new CockpitScreen(rocket));
            while(instrument.wasPressed())if(client.player!=null&&client.currentScreen==null&&client.player.getVehicle() instanceof TitanRocket rocket){
                var tab=instrumentTab(rocket,client.player.getEyePos(),client.player.getRotationVec(1));
                client.setScreen(new CockpitScreen(rocket,tab));
            }
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
