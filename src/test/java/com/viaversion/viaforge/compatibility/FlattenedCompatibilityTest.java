package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocol.packet.PacketWrapperImpl;
import io.netty.buffer.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlattenedCompatibilityTest {
    @Test public void eightExactTargetsHaveTheirOwnCodecAndResources() {
        int[] protocols={393,401,404,477,480,485,490,498};String[] versions={"1.13","1.13.1","1.13.2","1.14","1.14.1","1.14.2","1.14.3","1.14.4"};
        for(int i=0;i<protocols.length;i++) {
            CompatibilityProfile profile=CompatibilityRegistry.DEFAULT.resolve(protocols[i]);
            assertEquals(protocols[i],profile.serverProtocol());assertEquals(versions[i],profile.resources().version());
            assertEquals("flattened-"+protocols[i],profile.adapter().id());
            assertTrue(profile.adapter().create(profile,x->x) instanceof FlattenedProtocolAdapter);
            for(ClientFeature feature:ClientFeature.values())assertTrue(profile.has(feature));
        }
    }
    @Test public void neighboringUnverifiedTargetsCannotUseTheFlattenedFactory() {
        for(int id:new int[]{392,394,400,402,403,405,476,478,479,481,484,486,489,491,497,499,776}) {
            assertFalse(CompatibilityRegistry.DEFAULT.resolve(id).extended());
            try{new FlattenedProtocolAdapter.Factory(id);fail("Registered "+id);}catch(IllegalArgumentException expected){}
        }
    }
    @Test public void observingMixedTypedFieldsAndUnreadBytesDoesNotConsumeOrRetypeThem()throws Exception {
        ByteBuf tail=Unpooled.buffer().writeShort(1234).writeByte(19);
        PacketWrapperImpl packet=new PacketWrapperImpl(0x17,tail,null);
        packet.write(Types.BYTE,(byte)-1);packet.write(Types.VAR_INT,300);packet.resetReader();
        try {
            for(int i=0;i<2;i++) {
                ByteBuf copy=FlattenedProtocolAdapter.snapshot(packet);
                try{assertEquals(0x17,Types.VAR_INT.readPrimitive(copy));assertEquals(-1,copy.readByte());assertEquals(300,Types.VAR_INT.readPrimitive(copy));assertEquals(1234,copy.readShort());assertEquals(19,copy.readByte());assertFalse(copy.isReadable());}
                finally{copy.release();}
                assertEquals(0,tail.readerIndex());
            }
            assertEquals(Byte.valueOf((byte)-1),packet.read(Types.BYTE));assertEquals(Integer.valueOf(300),packet.read(Types.VAR_INT));
            assertEquals(Short.valueOf((short)1234),packet.read(Types.SHORT));assertEquals(Byte.valueOf((byte)19),packet.read(Types.BYTE));
        }finally{tail.release();}
    }
    @Test public void internallyGeneratedPacketsWithoutAnInputBufferCanBeObserved()throws Exception {
        PacketWrapperImpl packet=new PacketWrapperImpl(3,null,null);packet.write(Types.INT,42);packet.resetReader();
        ByteBuf copy=FlattenedProtocolAdapter.snapshot(packet);
        try{assertEquals(3,Types.VAR_INT.readPrimitive(copy));assertEquals(42,copy.readInt());assertEquals(Integer.valueOf(42),packet.read(Types.INT));}finally{copy.release();}
    }
}
