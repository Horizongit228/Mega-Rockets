package dev.megarockets.client.mixin;

import dev.megarockets.client.PlanetLoading;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DownloadingTerrainScreen.class)
public abstract class PlanetLoadingMixin extends Screen{
    protected PlanetLoadingMixin(Text title){super(title);}
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void megarockets$loading(DrawContext context,int mouseX,int mouseY,float delta,CallbackInfo ci){
        if(PlanetLoading.renderScreen(context,width,height))ci.cancel();
    }
}
