package dev.megarockets.mixin;

import dev.megarockets.MegaRockets;
import dev.megarockets.MoonWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class SpaceGravityMixin {
    @Inject(method="getFinalGravity",at=@At("RETURN"),cancellable=true)
    private void megaRockets$gravity(CallbackInfoReturnable<Double> cir) {
        Entity self=(Entity)(Object)this;
        if(MegaRockets.isMoon(self.getWorld()))
            cir.setReturnValue(cir.getReturnValue()*.165);
        else if(self.getWorld().getRegistryKey().equals(MegaRockets.ORBIT))
            cir.setReturnValue(0.0);
    }
}
