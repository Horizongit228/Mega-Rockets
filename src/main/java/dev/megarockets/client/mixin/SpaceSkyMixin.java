package dev.megarockets.client.mixin;

import dev.megarockets.client.SpaceSky;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class SpaceSkyMixin {
    @Inject(method="renderSky",at=@At("HEAD"),cancellable=true)
    private void megaRockets$sky(Matrix4f matrix,Matrix4f projection,float delta,Camera camera,boolean thickFog,Runnable fog,CallbackInfo ci){
        var world=MinecraftClient.getInstance().world;
        if(world!=null&&SpaceSky.darkness(world,camera.getPos().y)>.00001){
            SpaceSky.render(matrix,camera,world,delta,fog);ci.cancel();
        }
    }
}
