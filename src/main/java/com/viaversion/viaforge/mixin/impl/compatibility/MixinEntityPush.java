package com.viaversion.viaforge.mixin.impl.compatibility;

import com.viaversion.viaforge.compatibility.ClientEntityPush;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityPush {
    @Redirect(method="onLivingUpdate",slice=@Slice(from=@At(value="CONSTANT",args="stringValue=push")),
        at=@At(value="FIELD",target="Lnet/minecraft/world/World;isRemote:Z",opcode=180))
    private boolean clientPushStep(World world) {
        return world.isRemote&&!ServerSession.rule(ClientRule.CLIENT_ENTITY_PUSH);
    }
    @Inject(method="collideWithNearbyEntities",at=@At("HEAD"),cancellable=true)
    private void originalClientSelection(CallbackInfo ci) {
        EntityLivingBase entity=(EntityLivingBase)(Object)this;
        if(entity.worldObj.isRemote&&ServerSession.rule(ClientRule.CLIENT_ENTITY_PUSH)) {
            ClientEntityPush.tick(entity);ci.cancel();
        }
    }
}
