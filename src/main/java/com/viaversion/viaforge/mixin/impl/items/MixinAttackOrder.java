package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinAttackOrder {
    @Unique private boolean viaForge$pendingSwing;
    @Inject(method="clickMouse",at=@At("HEAD"))
    private void beginClick(CallbackInfo ci){viaForge$pendingSwing=false;}
    @Redirect(method="clickMouse",at=@At(value="INVOKE",target="Lnet/minecraft/client/entity/EntityPlayerSP;swingItem()V"))
    private void beforeHit(EntityPlayerSP player) {
        Minecraft mc=(Minecraft)(Object)this;
        if(!ServerSession.has(ClientFeature.COMBAT))player.swingItem();
        else viaForge$pendingSwing=mc.objectMouseOver!=null;
    }
    @Inject(method="clickMouse",at=@At("RETURN"))
    private void afterHit(CallbackInfo ci) {
        // Original 1.9+ swings after entity attack OR initial block damage.
        // A click rejected by the native cooldown never reaches the redirect.
        if(viaForge$pendingSwing){viaForge$pendingSwing=false;((Minecraft)(Object)this).thePlayer.swingItem();}
    }
}
