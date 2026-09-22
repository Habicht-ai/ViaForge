package com.viaversion.viaforge.mixin.impl.compatibility;

import com.viaversion.viaforge.compatibility.ClientEntityMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinEntityMotionPackets {
    @Inject(method="handleEntityMovement",at=@At("HEAD"),cancellable=true)
    private void originalMovement(S14PacketEntity packet,CallbackInfo ci) {
        Minecraft mc=Minecraft.getMinecraft();PacketThreadUtil.checkThreadAndEnqueue(packet,(NetHandlerPlayClient)(Object)this,mc);
        if(mc.theWorld!=null&&ClientEntityMotion.tracks(packet.getEntity(mc.theWorld)))ci.cancel();
    }
    @Inject(method="handleEntityTeleport",at=@At("HEAD"),cancellable=true)
    private void originalTeleport(S18PacketEntityTeleport packet,CallbackInfo ci) {
        Minecraft mc=Minecraft.getMinecraft();PacketThreadUtil.checkThreadAndEnqueue(packet,(NetHandlerPlayClient)(Object)this,mc);
        if(mc.theWorld!=null&&ClientEntityMotion.tracks(mc.theWorld.getEntityByID(packet.getEntityId())))ci.cancel();
    }
}
