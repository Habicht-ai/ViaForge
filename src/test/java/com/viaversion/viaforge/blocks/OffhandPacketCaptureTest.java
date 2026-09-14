package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.minecraft.item.*;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.*;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import io.netty.buffer.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class OffhandPacketCaptureTest {
    @Test public void onlyPlayerOffhandSlotsAreCaptured(){
        for(BlockVersionProfile profile:BlockVersionProfile.values()){
            LegacyEntityPackets capture=new LegacyEntityPackets(profile);
            for(int[] slot:new int[][]{{0,45,1},{-2,40,1},{0,40,0},{2,45,0},{-1,-1,0}}){
                ByteBuf packet=packet(profile,"CONTAINER_SET_SLOT");packet.writeByte(slot[0]).writeShort(slot[1]);Types.ITEM1_8.write(packet,new DataItem(442,(byte)1,(short)27,null));
                try{ByteBuf result=capture.capture(packet);try{
                    assertEquals(slot[2]==1,result!=null);assertEquals(0,packet.readerIndex());
                    if(result!=null){header(result,profile,26);Item shield=Types.ITEM1_8.read(result);assertEquals(442,shield.identifier());assertEquals(27,shield.data());assertFalse(result.isReadable());}
                }finally{if(result!=null)result.release();}}finally{packet.release();}
            }
        }
    }
    @Test public void bulkInventoryKeepsLastSlotIncludingEmpty(){
        for(BlockVersionProfile profile:BlockVersionProfile.values())for(int window:new int[]{0,3})for(int length:new int[]{45,46,54})for(boolean filled:new boolean[]{false,true}){
            LegacyEntityPackets capture=new LegacyEntityPackets(profile);Item[] items=new Item[length];if(filled&&length>=46)items[45]=new DataItem(432,(byte)12,(short)0,null);
            ByteBuf packet=packet(profile,"CONTAINER_SET_CONTENT");packet.writeByte(window);Types.ITEM1_8_SHORT_ARRAY.write(packet,items);
            try{ByteBuf result=capture.capture(packet);try{
                assertEquals(window==0&&length==46,result!=null);assertEquals(0,packet.readerIndex());
                if(result!=null){header(result,profile,26);Item item=Types.ITEM1_8.read(result);if(filled){assertEquals(432,item.identifier());assertEquals(12,item.amount());}else assertNull(item);assertFalse(result.isReadable());}
            }finally{if(result!=null)result.release();}}finally{packet.release();}
        }
    }
    @Test public void bothSwingHandsArePreservedWithoutReplacingOtherAnimations(){
        for(BlockVersionProfile profile:BlockVersionProfile.values())for(int animation:new int[]{0,1,2,3,4,5}){
            ByteBuf packet=packet(profile,"ANIMATE");Types.VAR_INT.writePrimitive(packet,981);packet.writeByte(animation);
            try{ByteBuf result=new LegacyEntityPackets(profile).replacement(packet);try{
                assertEquals(animation==0||animation==3,result!=null);assertEquals(0,packet.readerIndex());
                if(result!=null){header(result,profile,27);assertEquals(981,Types.VAR_INT.readPrimitive(result));assertEquals(animation,result.readUnsignedByte());assertFalse(result.isReadable());}
            }finally{if(result!=null)result.release();}}finally{packet.release();}
        }
    }
    private static void header(ByteBuf data,BlockVersionProfile profile,int operation){assertEquals(ClientboundPackets1_8.CUSTOM_PAYLOAD.getId(),Types.VAR_INT.readPrimitive(data));assertEquals(LegacyEntityPackets.CHANNEL,Types.STRING.read(data));assertEquals(profile.protocol(),data.readUnsignedShort());assertEquals(operation,data.readUnsignedByte());}
    private static ByteBuf packet(BlockVersionProfile profile,String name){
        ClientboundPacketType[] values=profile.protocol()>=338?ClientboundPackets1_12_1.values():profile.protocol()>=335?ClientboundPackets1_12.values():profile.protocol()>=110?ClientboundPackets1_9_3.values():ClientboundPackets1_9.values();
        for(ClientboundPacketType type:values)if(type.getName().equals(name)){ByteBuf data=Unpooled.buffer();Types.VAR_INT.writePrimitive(data,type.getId());return data;}throw new AssertionError(name);
    }
}
