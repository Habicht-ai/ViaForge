package com.viaversion.viaforge.common.compatibility;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
/** Shared serverbound route, paired with the selected inbound adapter. */
public final class ExtensionPackets {
    public static void translate(String channel,PacketWrapper packet) {
        packet.cancel();CompatibilityProfile target=CompatibilityRegistry.forUser(packet.user());
        ClientFeature feature=channel.equals("VF|hands")?ClientFeature.TWO_HANDS:channel.equals("VF|boat")?ClientFeature.BOATS:channel.equals("VF|elytra")?ClientFeature.ELYTRA:null;
        if(feature!=null&&target.has(feature))target.adapter().serverbound(channel,packet);
    }
    private ExtensionPackets(){ }
}
