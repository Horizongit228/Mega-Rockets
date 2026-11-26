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
        var planet=MegaRockets.planet(self.getWorld());
        if(planet!=null)cir.setReturnValue(cir.getReturnValue()*planet.gravity);
        else if(self.getWorld().getRegistryKey().equals(MegaRockets.ORBIT))
            cir.setReturnValue(dev.megarockets.StationLayout.gravity(self.getX(),self.getY(),self.getZ())?cir.getReturnValue():0.0);
    }
}
