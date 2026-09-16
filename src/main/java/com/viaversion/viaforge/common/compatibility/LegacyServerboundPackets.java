package com.viaversion.viaforge.common.compatibility;
import com.viaversion.viaforge.platform.ViaForgeProtocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.*;
import com.viaversion.viarewind.protocol.v1_9to1_8.Protocol1_9To1_8;

/** Outbound codec for the legacy family; native UI only emits semantic channel operations. */
final class LegacyServerboundPackets {
    static void translate(String channel,PacketWrapper wrapper) {
        wrapper.cancel();
        if(channel.equals("VF|hands"))hands(wrapper);
        else if(channel.equals("VF|boat"))boats(wrapper);
    }
    private static void hands(PacketWrapper wrapper) {
        wrapper.cancel();
        int op=wrapper.read(Types.UNSIGNED_BYTE);
        PacketWrapper modern;
        if(op==0){modern=PacketWrapper.create(ServerboundPackets1_9.PLAYER_ACTION,wrapper.user());modern.write(Types.VAR_INT,(int)wrapper.read(Types.UNSIGNED_BYTE));modern.write(Types.BLOCK_POSITION1_8,new com.viaversion.viaversion.api.minecraft.BlockPosition(0,0,0));modern.write(Types.UNSIGNED_BYTE,(short)0);}
        else {
            int hand=wrapper.read(Types.UNSIGNED_BYTE);if(hand>1)return;
            if(op==1){
                com.viaversion.viaversion.api.minecraft.BlockPosition pos=wrapper.read(Types.BLOCK_POSITION1_8);int face=wrapper.read(Types.UNSIGNED_BYTE);wrapper.read(Types.ITEM1_8);
                modern=PacketWrapper.create(face==255?ServerboundPackets1_9.USE_ITEM:ServerboundPackets1_9.USE_ITEM_ON,wrapper.user());
                if(face!=255){modern.write(Types.BLOCK_POSITION1_8,pos);modern.write(Types.VAR_INT,face);}
                modern.write(Types.VAR_INT,hand);
                if(face!=255)for(int i=0;i<3;i++)modern.write(Types.UNSIGNED_BYTE,wrapper.read(Types.UNSIGNED_BYTE));
            } else if(op==2){
                modern=PacketWrapper.create(ServerboundPackets1_9.INTERACT,wrapper.user());modern.write(Types.VAR_INT,wrapper.read(Types.VAR_INT));int action=wrapper.read(Types.VAR_INT);modern.write(Types.VAR_INT,action);
                if(action==2)for(int i=0;i<3;i++)modern.write(Types.FLOAT,wrapper.read(Types.FLOAT));if(action!=1)modern.write(Types.VAR_INT,hand);
            } else if(op==3){modern=PacketWrapper.create(ServerboundPackets1_9.SWING,wrapper.user());modern.write(Types.VAR_INT,hand);}
            else if(op==4){
                modern=PacketWrapper.create(ServerboundPackets1_9.CONTAINER_CLICK,wrapper.user());modern.write(Types.BYTE,wrapper.read(Types.BYTE));modern.write(Types.SHORT,wrapper.read(Types.SHORT));modern.write(Types.BYTE,wrapper.read(Types.BYTE));modern.write(Types.SHORT,wrapper.read(Types.SHORT));modern.write(Types.VAR_INT,(int)wrapper.read(Types.BYTE));modern.write(Types.ITEM1_8,item(wrapper));
            }else if(op==5){modern=PacketWrapper.create(ServerboundPackets1_9.SET_CREATIVE_MODE_SLOT,wrapper.user());modern.write(Types.SHORT,wrapper.read(Types.SHORT));modern.write(Types.ITEM1_8,item(wrapper));}
            else if(op==6){
                modern=PacketWrapper.create(ServerboundPackets1_9.CLIENT_INFORMATION,wrapper.user());modern.write(Types.STRING,wrapper.read(Types.STRING));modern.write(Types.BYTE,wrapper.read(Types.BYTE));modern.write(Types.VAR_INT,(int)wrapper.read(Types.BYTE));modern.write(Types.BOOLEAN,wrapper.read(Types.BOOLEAN));short flags=wrapper.read(Types.UNSIGNED_BYTE);modern.write(Types.UNSIGNED_BYTE,flags);modern.write(Types.VAR_INT,hand);
                // Preserve ViaRewind's local skin-layer echo when bypassing its settings rewrite.
                PacketWrapper skin=PacketWrapper.create(ClientboundPackets1_8.SET_ENTITY_DATA,wrapper.user());
                skin.write(Types.VAR_INT,wrapper.user().getEntityTracker(Protocol1_9To1_8.class).clientEntityId());
                skin.write(Types.ENTITY_DATA_LIST1_8,java.util.Collections.singletonList(new com.viaversion.viaversion.api.minecraft.entitydata.EntityData(10,com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_8.BYTE,(byte)flags)));
                skin.scheduleSend(Protocol1_9To1_8.class);
            }
            else return;
        }
        modern.sendToServer(Protocol1_9To1_8.class,true);
    }
    private static com.viaversion.viaversion.api.minecraft.item.Item item(PacketWrapper wrapper) {
        com.viaversion.viaversion.api.minecraft.item.Item fallback=ViaForgeProtocol.INSTANCE.getItemRewriter().handleItemToServer(wrapper.user(),wrapper.read(Types.ITEM1_8));
        return wrapper.user().getProtocolInfo().getPipeline().getProtocol(Protocol1_9To1_8.class).getItemRewriter().handleItemToServer(wrapper.user(),fallback);
    }
    private static void boats(PacketWrapper wrapper) {
            int operation=wrapper.read(Types.UNSIGNED_BYTE);
            ServerboundPackets1_9 type=operation==0 ? ServerboundPackets1_9.MOVE_VEHICLE:operation==1 ? ServerboundPackets1_9.PADDLE_BOAT:ServerboundPackets1_9.PLAYER_INPUT;
            PacketWrapper modern=PacketWrapper.create(type,wrapper.user());
            if(operation==0) { modern.write(Types.DOUBLE,wrapper.read(Types.DOUBLE)); modern.write(Types.DOUBLE,wrapper.read(Types.DOUBLE)); modern.write(Types.DOUBLE,wrapper.read(Types.DOUBLE)); modern.write(Types.FLOAT,wrapper.read(Types.FLOAT)); modern.write(Types.FLOAT,wrapper.read(Types.FLOAT)); }
            else if(operation==1) { modern.write(Types.BOOLEAN,wrapper.read(Types.BOOLEAN)); modern.write(Types.BOOLEAN,wrapper.read(Types.BOOLEAN)); }
            // PacketWrapper's in-memory fields are typed. Later Via layers read BYTE,
            // so UNSIGNED_BYTE fails despite having the same one-byte wire encoding.
            else if(operation==2) { modern.write(Types.FLOAT,wrapper.read(Types.FLOAT)); modern.write(Types.FLOAT,wrapper.read(Types.FLOAT)); modern.write(Types.BYTE,wrapper.read(Types.BYTE)); }
            else return;
            modern.sendToServer(Protocol1_9To1_8.class,true);
    }
    private LegacyServerboundPackets(){ }
}
