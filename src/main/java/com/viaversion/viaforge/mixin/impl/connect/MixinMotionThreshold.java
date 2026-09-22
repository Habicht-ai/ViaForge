package com.viaversion.viaforge.mixin.impl.connect;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(EntityLivingBase.class)
public abstract class MixinMotionThreshold {
    @ModifyConstant(method="onLivingUpdate",constant=@Constant(doubleValue=.005D,ordinal=1),require=1)
    private double verticalMotionThreshold(double nativeValue) {
        return (Object)this==Minecraft.getMinecraft().thePlayer && ServerSession.rule(ClientRule.SMALL_MOTION_THRESHOLD)?.003D:nativeValue;
    }
    @ModifyConstant(method="onLivingUpdate",constant={@Constant(doubleValue=.005D,ordinal=0),@Constant(doubleValue=.005D,ordinal=2)},require=2)
    private double horizontalMotionThreshold(double nativeValue) {
        if((Object)this!=Minecraft.getMinecraft().thePlayer)return nativeValue;
        if(ServerSession.rule(ClientRule.HORIZONTAL_MOTION_THRESHOLD)) {
            EntityLivingBase entity=(EntityLivingBase)(Object)this;
            // Original 1.21.5 player rule: zero both axes only below 0.003^2.
            // When the vector survives, neither old per-axis comparison may zero it.
            if(entity.motionX*entity.motionX+entity.motionZ*entity.motionZ>=9.0E-6)return 0;
        }
        return ServerSession.rule(ClientRule.SMALL_MOTION_THRESHOLD)?.003D:nativeValue;
    }
}
