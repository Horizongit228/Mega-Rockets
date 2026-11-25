package dev.megarockets.mixin;
import dev.megarockets.MegaRockets;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class LunarSnowMixin {
    @Inject(method="tickIceAndSnow",at=@At("HEAD"),cancellable=true)
    private void megaRockets$noNaturalSnow(BlockPos pos,CallbackInfo ci){
        if(MegaRockets.isMoon((ServerWorld)(Object)this))ci.cancel();
    }
}
