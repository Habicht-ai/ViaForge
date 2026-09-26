package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.compatibility.ServerBubbleVisuals;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldClient.class)
public abstract class MixinBubbleDisplay {
    @Inject(method="doVoidFogParticles",at=@At("TAIL"))
    private void columns(int x,int y,int z,CallbackInfo ci) {
        ServerBubbleVisuals.animate((WorldClient)(Object)this,x,y,z);
    }
}
