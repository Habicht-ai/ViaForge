package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;

/** Actual client inputs and tick boundaries replace ViaBackwards' proxy wall-clock approximation. */
public final class ClientTickPackets implements StorableObject {
    public static final String CHANNEL="VF|tick";
    private int previous=-1;
    public static void translate(PacketWrapper packet) {
        packet.cancel();
        if(!packet.user().getProtocolInfo().getPipeline().contains(Protocol1_21_2To1_21.class))return;
        int action=packet.read(Types.BYTE);
        if(action==0) {
            int flags=packet.read(Types.BYTE);
            ClientTickPackets state=packet.user().get(ClientTickPackets.class);
            if(state==null){state=new ClientTickPackets();packet.user().put(state);}
            if(state.previous==flags)return;
            state.previous=flags;
            PacketWrapper input=PacketWrapper.create(ServerboundPackets1_21_2.PLAYER_INPUT,packet.user());
            input.write(Types.BYTE,(byte)flags);input.sendToServer(Protocol1_21_2To1_21.class,true);
        }else if(action==1) {
            PacketWrapper end=PacketWrapper.create(ServerboundPackets1_21_2.CLIENT_TICK_END,packet.user());
            end.sendToServer(Protocol1_21_2To1_21.class,true);
        }
    }
}
