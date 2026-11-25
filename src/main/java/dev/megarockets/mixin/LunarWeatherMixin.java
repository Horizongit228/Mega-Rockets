package dev.megarockets.mixin;
import dev.megarockets.MegaRockets;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class LunarWeatherMixin {
    @Inject(method={"getRainGradient","getThunderGradient"},at=@At("HEAD"),cancellable=true)
    private void megaRockets$dryMoon(float delta,CallbackInfoReturnable<Float> cir){
        if(MegaRockets.isMoon((World)(Object)this))cir.setReturnValue(0f);
    }
    @Inject(method={"isRaining","isThundering"},at=@At("HEAD"),cancellable=true)
    private void megaRockets$noMoonWeather(CallbackInfoReturnable<Boolean> cir){
        if(MegaRockets.isMoon((World)(Object)this))cir.setReturnValue(false);
    }
}
