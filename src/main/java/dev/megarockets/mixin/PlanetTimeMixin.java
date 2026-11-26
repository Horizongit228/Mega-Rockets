package dev.megarockets.mixin;
import dev.megarockets.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(World.class)
public abstract class PlanetTimeMixin {
 @Inject(method="getTimeOfDay",at=@At("HEAD"),cancellable=true)
 private void megaRockets$solarDay(CallbackInfoReturnable<Long> cir){
  World w=(World)(Object)this;
  var d=MegaRockets.planet(w);
  if(d!=null)cir.setReturnValue(SolarDynamics.localTime(d,w.getTime()));
 }
}
