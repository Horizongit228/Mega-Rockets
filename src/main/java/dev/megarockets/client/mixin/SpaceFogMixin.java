package dev.megarockets.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.megarockets.client.SpaceSky;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public abstract class SpaceFogMixin {
    @Shadow private static float red;
    @Shadow private static float green;
    @Shadow private static float blue;
    @Inject(method="render",at=@At("RETURN"))
    private static void megaRockets$darkFog(Camera camera,float delta,ClientWorld world,int distance,float skyDarkness,CallbackInfo ci){
        var body=dev.megarockets.MegaRockets.planet(world);
        if(body==dev.megarockets.Destination.MARS||body==dev.megarockets.Destination.VENUS||body==dev.megarockets.Destination.TITAN){
            float day=(float)Math.max(0,Math.min(1,(Math.sin(world.getTimeOfDay()*Math.PI/12000)+.16)/.4));
            float air=(float)Math.max(0,Math.min(1,(world.getTopY()+160-camera.getPos().y)/180));
            float light=(.055f+.945f*day)*air;
            red=(body==dev.megarockets.Destination.VENUS?.60f:body==dev.megarockets.Destination.TITAN?.45f:.42f)*light;
            green=(body==dev.megarockets.Destination.VENUS?.43f:body==dev.megarockets.Destination.TITAN?.25f:.24f)*light;
            blue=(body==dev.megarockets.Destination.VENUS?.23f:body==dev.megarockets.Destination.TITAN?.10f:.14f)*light;
            RenderSystem.clearColor(red,green,blue,0);return;
        }
        float f=(float)SpaceSky.darkness(world,camera.getPos().y);
        if(f>0){red=red*(1-f)+.008f*f;green=green*(1-f)+.012f*f;blue=blue*(1-f)+.03f*f;RenderSystem.clearColor(red,green,blue,0);}
    }
}
