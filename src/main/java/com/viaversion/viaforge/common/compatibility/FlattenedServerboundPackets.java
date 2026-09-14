package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viabackwards.protocol.v1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ServerboundPackets1_12_1;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ServerboundPackets1_13;
import io.netty.buffer.ByteBuf;

/** Targeted correction for ViaBackwards 5.11.0's structure conversion: it reads
 * mirror/rotation but omits both fields from the new dedicated packet. */
public final class FlattenedServerboundPackets {
    public static boolean translate(Object protocol, PacketWrapper packet) throws Exception {
        if (!(protocol instanceof Protocol1_13To1_12_2)
                || packet.getId()!=ServerboundPackets1_12_1.CUSTOM_PAYLOAD.getId()
                || !(CompatibilityRegistry.forUser(packet.user()).adapter() instanceof FlattenedProtocolAdapter.Factory)) return false;
        ByteBuf input=FlattenedProtocolAdapter.snapshot(packet);
        try {
            Types.VAR_INT.readPrimitive(input);
            if (!"MC|Struct".equals(Types.STRING.read(input))) return false;
            BlockPosition pos=new BlockPosition(input.readInt(),input.readInt(),input.readInt());
            int action=input.readUnsignedByte()-1;
            int mode=index(Types.STRING.read(input),"SAVE","LOAD","CORNER","DATA");
            String name=Types.STRING.read(input);byte[] bounds=new byte[6];
            for(int i=0;i<6;i++)bounds[i]=(byte)input.readInt();
            int mirror=index(Types.STRING.read(input),"NONE","LEFT_RIGHT","FRONT_BACK");
            int rotation=index(Types.STRING.read(input),"NONE","CLOCKWISE_90","CLOCKWISE_180","COUNTERCLOCKWISE_90");
            String metadata=Types.STRING.read(input);byte flags=0;
            if(input.readBoolean())flags|=1;if(input.readBoolean())flags|=2;if(input.readBoolean())flags|=4;
            float integrity=input.readFloat();long seed=Types.VAR_LONG.readPrimitive(input);
            if(input.isReadable()||action<0||action>3)throw new IllegalArgumentException("Invalid structure operation");
            packet.clearPacket();packet.setPacketType(ServerboundPackets1_13.SET_STRUCTURE_BLOCK);
            packet.write(Types.BLOCK_POSITION1_8,pos);packet.write(Types.VAR_INT,action);packet.write(Types.VAR_INT,mode);packet.write(Types.STRING,name);
            for(byte value:bounds)packet.write(Types.BYTE,value);
            packet.write(Types.VAR_INT,mirror);packet.write(Types.VAR_INT,rotation);packet.write(Types.STRING,metadata);
            packet.write(Types.FLOAT,integrity);packet.write(Types.VAR_LONG,seed);packet.write(Types.BYTE,flags);
            return true;
        }finally{input.release();}
    }
    private static int index(String value,String... choices) {
        for(int i=0;i<choices.length;i++)if(value.equals(choices[i]))return i;
        throw new IllegalArgumentException("Unknown structure option: "+value);
    }
    private FlattenedServerboundPackets(){}
}
