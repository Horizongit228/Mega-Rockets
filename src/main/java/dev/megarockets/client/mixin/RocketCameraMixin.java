package dev.megarockets.client.mixin;

import dev.megarockets.TitanRocket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class RocketCameraMixin {
    // Only third person uses clipToSpace during Camera.update. First person is untouched.
    @ModifyArg(method="update", at=@At(value="INVOKE",target="Lnet/minecraft/client/render/Camera;clipToSpace(F)F"), index=0)
    private float megaRockets$wideRocketView(float vanillaDistance) {
        var client=MinecraftClient.getInstance();
        var focused=client.getCameraEntity();
        return focused!=null && focused.getVehicle() instanceof TitanRocket ? 96f : vanillaDistance;
    }
}
