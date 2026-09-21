package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viabackwards.protocol.v1_17to1_16_4.Protocol1_17To1_16_4;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;

/** Transport acknowledgement, deliberately independent of gameplay features and resource revisions. */
public final class OrderedPing {
    public static final String CHANNEL="VF|pong";
    public static void reply(PacketWrapper packet) {
        packet.cancel();
        if(!packet.user().getProtocolInfo().getPipeline().contains(Protocol1_17To1_16_4.class))return;
        PacketWrapper pong=PacketWrapper.create(ServerboundPackets1_17.PONG,packet.user());
        pong.write(Types.INT,packet.read(Types.INT));
        pong.sendToServer(Protocol1_17To1_16_4.class,true);
    }
    private OrderedPing(){ }
}
