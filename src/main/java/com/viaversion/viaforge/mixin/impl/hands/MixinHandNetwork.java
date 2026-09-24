package com.viaversion.viaforge.mixin.impl.hands;

import com.viaversion.viaforge.hands.*;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinHandNetwork {
    @Inject(method="addToSendQueue",at=@At("HEAD"),cancellable=true)
    private void handPacket(Packet packet,CallbackInfo ci) {
        if(!Offhand.active())return;
        if(packet instanceof C0APacketAnimation && Offhand.localUseSwing()){ci.cancel();return;}
        if(packet instanceof C15PacketClientSettings){((NetHandlerPlayClient)(Object)this).addToSendQueue(HandPackets.wrapped(packet,Offhand.mainLeft()?0:1));ci.cancel();return;}
        if(packet instanceof C0APacketAnimation)Offhand.swingHand=Math.max(0,Offhand.context);
        // Modern clicks already choose their original swing/attack order. Route
        // every swing through the hand path immediately: ViaRewind's legacy
        // animation queue otherwise waits for movement, after sneak/sprint input.
        boolean use=packet instanceof C0APacketAnimation||Offhand.context>=0&&(packet instanceof C08PacketPlayerBlockPlacement||packet instanceof C02PacketUseEntity);
        boolean inventory=packet instanceof C0EPacketClickWindow && ((C0EPacketClickWindow)packet).getWindowId()==0;
        boolean creative=packet instanceof C10PacketCreativeInventoryAction && ((C10PacketCreativeInventoryAction)packet).getSlotId()==45;
        if(use||inventory||creative){((NetHandlerPlayClient)(Object)this).addToSendQueue(HandPackets.wrapped(packet,Math.max(0,Offhand.context)));ci.cancel();}
    }
}
