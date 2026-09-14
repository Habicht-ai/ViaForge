package com.viaversion.viaforge.common.compatibility;

import io.netty.buffer.ByteBuf;

/** Header shared by original-data adapters and the main-thread consumer. */
public final class ClientEventEnvelope {
    public final ClientEventFormat format;
    public final int operation;
    private ClientEventEnvelope(ClientEventFormat format,int operation){this.format=format;this.operation=operation;}
    public static ClientEventEnvelope read(ByteBuf input){
        ClientEventFormat format=ClientEventFormat.legacy(input.readUnsignedShort());int operation=input.readUnsignedByte();
        if(operation>27)throw new IllegalArgumentException("Unknown client event "+operation);
        return new ClientEventEnvelope(format,operation);
    }
    public static void write(ByteBuf output,ClientEventFormat format,int operation){
        if(operation<0||operation>27)throw new IllegalArgumentException("Unknown client event "+operation);
        output.writeShort(format.revision()).writeByte(operation);
    }
    public static ClientFeature feature(int operation){
        if(operation==26||operation==27)return ClientFeature.TWO_HANDS;
        if(operation==19)return ClientFeature.COOLDOWNS;
        if(operation==8)return ClientFeature.TOTEM;
        if(operation==10)return ClientFeature.COMBAT;
        if(operation==20||operation>=21&&operation<=25)return ClientFeature.BOATS;
        if(operation>=11&&operation<=16)return ClientFeature.MOBS;
        return ClientFeature.ENTITY_VISUALS;
    }
}
