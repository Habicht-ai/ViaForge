package com.viaversion.viaforge.mixin.impl.hands;
import com.viaversion.viaforge.hands.HandRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemRenderer.class)
public abstract class MixinBothHands {
    @Inject(method="renderItemInFirstPerson",at=@At("HEAD"),cancellable=true)
    private void hands(float partial,CallbackInfo ci){if(HandRenderer.needed()){HandRenderer.render((ItemRenderer)(Object)this,partial);ci.cancel();}}
}
