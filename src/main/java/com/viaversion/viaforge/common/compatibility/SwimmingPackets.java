package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_13;
import com.viaversion.viaversion.api.type.types.version.Types1_14;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;
import io.netty.buffer.ByteBuf;
import java.util.List;

/** Retains swim flags, poses and aquatic effects before Via's lossy boundaries. */
public final class SwimmingPackets {
    public static ByteBuf attributes(ByteBuf source) throws Exception {
        return attributes(source,id->com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21.MAPPINGS.getAttributeMappings().mappedIdentifier(id));
    }
    public static ByteBuf attributes(ByteBuf source,java.util.function.IntFunction<String> identifiers) throws Exception {
        ByteBuf input=source.duplicate();
        if(Types.VAR_INT.readPrimitive(input)!=com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21.UPDATE_ATTRIBUTES.getId())return null;
        int entity=Types.VAR_INT.readPrimitive(input),count=Types.VAR_INT.readPrimitive(input);
        ByteBuf values=source.alloc().buffer();int kept=0;
        try {
            for(int i=0;i<count;i++) {
                int id=Types.VAR_INT.readPrimitive(input);
                String key=identifiers.apply(id);
                double base=input.readDouble();int modifiers=Types.VAR_INT.readPrimitive(input);
                double[] amounts=new double[modifiers];int[] operations=new int[modifiers];
                for(int j=0;j<modifiers;j++){Types.STRING.read(input);amounts[j]=input.readDouble();operations[j]=input.readUnsignedByte();}
                int kind=key!=null&&key.endsWith("water_movement_efficiency")?0:key!=null&&key.endsWith("gravity")?1:
                        key!=null&&key.endsWith("sneaking_speed")?2:-1;
                if(kind<0)continue;
                for(int j=0;j<modifiers;j++)if(operations[j]==0)base+=amounts[j];
                double value=base;
                for(int j=0;j<modifiers;j++)if(operations[j]==1)value+=base*amounts[j];
                for(int j=0;j<modifiers;j++)if(operations[j]==2)value*=1+amounts[j];
                values.writeByte(kind).writeDouble(Math.max(0,Math.min(1,value)));kept++;
            }
            if(kept==0)return null;
            ByteBuf result=event(source,36);Types.VAR_INT.writePrimitive(result,entity);result.writeByte(kept).writeBytes(values);return result;
        }finally{values.release();}
    }
    public static ByteBuf capture(ByteBuf source, boolean poses) throws Exception {
        ByteBuf input=source.duplicate(); int packet=Types.VAR_INT.readPrimitive(input);
        String name=poses ? (packet>=0&&packet<ClientboundPackets1_14.values().length?ClientboundPackets1_14.values()[packet].name():"")
                : (packet>=0&&packet<ClientboundPackets1_13.values().length?ClientboundPackets1_13.values()[packet].name():"");
        if(!poses&&(name.equals("UPDATE_MOB_EFFECT")||name.equals("REMOVE_MOB_EFFECT"))) {
            int entity=Types.VAR_INT.readPrimitive(input),effect=input.readUnsignedByte();
            if(effect<28||effect>30)return null;
            ByteBuf result=event(source,name.equals("UPDATE_MOB_EFFECT")?17:18);
            Types.VAR_INT.writePrimitive(result,entity);result.writeByte(effect).writeBytes(input);return result;
        }
        if(!name.equals("SET_ENTITY_DATA")&&!name.equals("ADD_PLAYER"))return null;
        int entity=Types.VAR_INT.readPrimitive(input);
        if(name.equals("ADD_PLAYER"))input.skipBytes(16+24+2);
        List<EntityData> entries=(poses?Types1_14.ENTITY_DATA_LIST:Types1_13.ENTITY_DATA_LIST).read(input);
        int swim=-1,pose=-1;
        for(EntityData data:entries) {
            if(!poses&&data.id()==0&&data.getValue() instanceof Byte)swim=(((Byte)data.getValue())&16)!=0?1:0;
            if(poses&&data.id()==6&&data.getValue() instanceof Integer)pose=(Integer)data.getValue();
        }
        if(swim<0&&pose<0)return null;
        ByteBuf result=event(source,34);Types.VAR_INT.writePrimitive(result,entity);result.writeByte(swim).writeByte(pose);return result;
    }
    private static ByteBuf event(ByteBuf source,int operation) throws Exception {
        ByteBuf result=source.alloc().buffer();Types.VAR_INT.writePrimitive(result,0x3f);Types.STRING.write(result,"VF|entity");
        result.writeShort(340).writeByte(operation);return result;
    }
    private SwimmingPackets(){}
}
