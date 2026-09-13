package com.viaversion.viaforge.mixin.impl.boats;

import com.viaversion.viaforge.boats.ServerBoat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinBoatPackets {
    @Inject(method="handleEntityMovement",at=@At("HEAD"),cancellable=true)
    private void originalMovement(S14PacketEntity packet,CallbackInfo ci) {
        Minecraft mc=Minecraft.getMinecraft(); PacketThreadUtil.checkThreadAndEnqueue(packet,(NetHandlerPlayClient)(Object)this,mc);
        if(mc.theWorld!=null && packet.getEntity(mc.theWorld) instanceof ServerBoat) ci.cancel();
    }
    @Inject(method="handleEntityTeleport",at=@At("HEAD"),cancellable=true)
    private void originalTeleport(S18PacketEntityTeleport packet,CallbackInfo ci) {
        Minecraft mc=Minecraft.getMinecraft(); PacketThreadUtil.checkThreadAndEnqueue(packet,(NetHandlerPlayClient)(Object)this,mc);
        if(mc.theWorld!=null && mc.theWorld.getEntityByID(packet.getEntityId()) instanceof ServerBoat) ci.cancel();
    }
    @Inject(method="handleEntityAttach",at=@At("HEAD"),cancellable=true)
    private void originalSeats(S1BPacketEntityAttach packet,CallbackInfo ci) {
        Minecraft mc=Minecraft.getMinecraft(); PacketThreadUtil.checkThreadAndEnqueue(packet,(NetHandlerPlayClient)(Object)this,mc);
        if(mc.theWorld==null || packet.getLeash()!=0) return;
        Entity mount=mc.theWorld.getEntityByID(packet.getVehicleEntityId()),rider=mc.theWorld.getEntityByID(packet.getEntityId());
        if(mount instanceof ServerBoat || rider!=null && rider.ridingEntity instanceof ServerBoat || mount!=null && mount.ridingEntity instanceof ServerBoat) ci.cancel();
    }
}
