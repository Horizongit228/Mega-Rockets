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
        if(world.getRegistryKey().equals(dev.megarockets.MegaRockets.MARS)){red=.42f;green=.24f;blue=.14f;RenderSystem.clearColor(red,green,blue,0);return;}
        var body=dev.megarockets.MegaRockets.planet(world);
        if(body==dev.megarockets.Destination.VENUS){red=.60f;green=.43f;blue=.23f;RenderSystem.clearColor(red,green,blue,0);return;}
        if(body==dev.megarockets.Destination.TITAN){red=.45f;green=.25f;blue=.10f;RenderSystem.clearColor(red,green,blue,0);return;}
        float f=(float)SpaceSky.darkness(world,camera.getPos().y);
        if(f>0){red=red*(1-f)+.008f*f;green=green*(1-f)+.012f*f;blue=blue*(1-f)+.03f*f;RenderSystem.clearColor(red,green,blue,0);}
    }
}
