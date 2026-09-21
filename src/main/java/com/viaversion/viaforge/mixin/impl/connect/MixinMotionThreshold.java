package com.viaversion.viaforge.mixin.impl.connect;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(EntityLivingBase.class)
public abstract class MixinMotionThreshold {
    @ModifyConstant(method="onLivingUpdate",constant=@Constant(doubleValue=.005D),require=3)
    private double originalMotionThreshold(double nativeValue) {
        return (Object)this==Minecraft.getMinecraft().thePlayer && ServerSession.rule(ClientRule.SMALL_MOTION_THRESHOLD)?.003D:nativeValue;
    }
}
