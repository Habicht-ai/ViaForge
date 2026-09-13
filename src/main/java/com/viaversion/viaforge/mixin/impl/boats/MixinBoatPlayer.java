package com.viaversion.viaforge.mixin.impl.boats;

import com.viaversion.viaforge.boats.*;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0CPacketInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class MixinBoatPlayer {
    @Inject(method="onUpdate",at=@At("TAIL"))
    private void vehiclePosition(CallbackInfo ci) {
        EntityPlayerSP player=(EntityPlayerSP)(Object)this;
        if(player.ridingEntity instanceof ServerBoat && player.worldObj.isBlockLoaded(new net.minecraft.util.BlockPos(player.posX,0,player.posZ))) BoatPackets.move((ServerBoat)player.ridingEntity);
    }
    @Redirect(method="onUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/client/network/NetHandlerPlayClient;addToSendQueue(Lnet/minecraft/network/Packet;)V"))
    private void modernVehicleInput(NetHandlerPlayClient handler,Packet packet) {
        EntityPlayerSP player=(EntityPlayerSP)(Object)this;
        if(player.ridingEntity instanceof ServerBoat && packet instanceof C0CPacketInput) {
            handler.addToSendQueue(BoatPackets.input(player.moveStrafing,player.moveForward,player.movementInput.jump,player.movementInput.sneak));
        } else handler.addToSendQueue(packet);
    }
}
