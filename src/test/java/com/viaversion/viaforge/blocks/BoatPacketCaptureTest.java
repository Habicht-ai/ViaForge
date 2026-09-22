package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import io.netty.buffer.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BoatPacketCaptureTest {
    @Test public void trackedMotionAndRemovalClearReusedBoatIds() {
        for (BlockVersionProfile profile : BlockVersionProfile.values()) {
            LegacyEntityPackets capture = new LegacyEntityPackets(profile);
            spawn(profile,capture,1);
            for (String type : new String[]{"MOVE_ENTITY_POS","MOVE_ENTITY_POS_ROT","MOVE_ENTITY_ROT","TELEPORT_ENTITY"}) {
                ByteBuf boat=packet(profile,type);Types.VAR_INT.writePrimitive(boat,88); assertCaptured(capture,boat,true);
                ByteBuf mob=packet(profile,type);Types.VAR_INT.writePrimitive(mob,89); assertCaptured(capture,mob,false);
            }
            ByteBuf remove=packet(profile,"REMOVE_ENTITIES");Types.VAR_INT.writePrimitive(remove,1);Types.VAR_INT.writePrimitive(remove,88);assertCaptured(capture,remove,true);
            motion(profile,capture,false);
            spawn(profile,capture,1); spawn(profile,capture,2); motion(profile,capture,false);
            spawn(profile,capture,1);
            ByteBuf mob=packet(profile,"ADD_MOB");Types.VAR_INT.writePrimitive(mob,88);assertCaptured(capture,mob,true);
            ByteBuf move=packet(profile,"MOVE_ENTITY_POS");Types.VAR_INT.writePrimitive(move,88);
            ByteBuf result=capture.capture(move);
            try {
                assertNotNull(result);Types.VAR_INT.readPrimitive(result);Types.STRING.read(result);
                assertEquals(30,com.viaversion.viaforge.common.compatibility.ClientEventEnvelope.read(result).operation);
                assertEquals(0,move.readerIndex());
            } finally {move.release();if(result!=null)result.release();}
            // A reused living ID must never be routed to the old boat event.
            spawn(profile,capture,2);motion(profile,capture,false);
        }
    }
    @Test public void sameDimensionRespawnKeepsBoatsButWorldChangeAndLoginClearThem() {
        for (BlockVersionProfile profile : BlockVersionProfile.values()) {
            LegacyEntityPackets capture=new LegacyEntityPackets(profile);
            login(profile,capture);spawn(profile,capture,1);
            ByteBuf same=packet(profile,"RESPAWN");same.writeInt(0);assertCaptured(capture,same,true);motion(profile,capture,true);
            ByteBuf changed=packet(profile,"RESPAWN");changed.writeInt(1);assertCaptured(capture,changed,true);motion(profile,capture,false);
            spawn(profile,capture,1);login(profile,capture);motion(profile,capture,false);
        }
    }
    private static void login(BlockVersionProfile p,LegacyEntityPackets c) {
        ByteBuf data=packet(p,"LOGIN");data.writeInt(1).writeByte(0);if(p.hasIntJoinDimension())data.writeInt(0);else data.writeByte(0);assertCaptured(c,data,true);
    }
    private static void spawn(BlockVersionProfile p,LegacyEntityPackets c,int type) {
        ByteBuf data=packet(p,"ADD_ENTITY");Types.VAR_INT.writePrimitive(data,88);data.writeLong(0).writeLong(88).writeByte(type);assertCaptured(c,data,type==1);
    }
    private static void motion(BlockVersionProfile p,LegacyEntityPackets c,boolean expected) {
        ByteBuf data=packet(p,"MOVE_ENTITY_POS");Types.VAR_INT.writePrimitive(data,88);assertCaptured(c,data,expected);
    }
    private static void assertCaptured(LegacyEntityPackets c,ByteBuf data,boolean expected) {
        try { ByteBuf result=c.capture(data);try {assertEquals(expected,result!=null);assertEquals(0,data.readerIndex());}finally{if(result!=null)result.release();} }
        finally {data.release();}
    }
    private static ByteBuf packet(BlockVersionProfile profile,String name) {
        ClientboundPacketType[] values=profile.protocol()>=338?ClientboundPackets1_12_1.values():profile.protocol()>=335?ClientboundPackets1_12.values():profile.protocol()>=110?ClientboundPackets1_9_3.values():ClientboundPackets1_9.values();
        for(ClientboundPacketType type:values)if(type.getName().equals(name)){ByteBuf data=Unpooled.buffer();Types.VAR_INT.writePrimitive(data,type.getId());return data;}
        throw new AssertionError(name);
    }
}
