package dev.megarockets.mixin;
import dev.megarockets.MegaRockets;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class PlanetFallMixin {
 @Inject(method="damage",at=@At("HEAD"),cancellable=true)
 private void megarockets$noPlanetFall(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
  LivingEntity self=(LivingEntity)(Object)this;
  if(source.isOf(DamageTypes.FALL)&&MegaRockets.planet(self.getWorld())!=null)ci.setReturnValue(false);
 }
}
