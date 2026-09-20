package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocol.packet.PacketWrapperImpl;
import io.netty.buffer.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlattenedCompatibilityTest {
    @Test public void exactTargetsHaveTheirOwnCodecAndResources() {
        int[] protocols={393,401,404,477,480,485,490,498,573,575,578,735,736,751,753,754,755,756,757,758,759,760,761,762,763,764,765,766,767,768,769,770,771,772,773,774,775,776,777};String[] versions={"1.13","1.13.1","1.13.2","1.14","1.14.1","1.14.2","1.14.3","1.14.4","1.15","1.15.1","1.15.2","1.16","1.16.1","1.16.2","1.16.3","1.16.5","1.17","1.17.1","1.18.1","1.18.2","1.19","1.19.2","1.19.3","1.19.4","1.20.1","1.20.2","1.20.4","1.20.6","1.21.1","1.21.3","1.21.4","1.21.5","1.21.6","1.21.8","1.21.10","1.21.11","26.1.2","26.2","26.3"};
        for(int i=0;i<protocols.length;i++) {
            CompatibilityProfile profile=CompatibilityRegistry.DEFAULT.resolve(protocols[i]);
            assertEquals(protocols[i],profile.serverProtocol());assertEquals(versions[i],profile.resources().version());
            assertEquals("flattened-"+protocols[i],profile.adapter().id());
            assertEquals(protocols[i]>=401,profile.rules().enabled(ClientRule.PREDICT_HOTBAR_DROPS));
            assertTrue(profile.adapter().create(profile,x->x) instanceof FlattenedProtocolAdapter);
            for(ClientFeature feature:ClientFeature.values())assertTrue(profile.has(feature));
        }
    }
    @Test public void neighboringUnverifiedTargetsCannotUseTheFlattenedFactory() {
        for(int id:new int[]{392,394,400,402,403,405,476,478,479,481,484,486,489,491,497,499,778}) {
            assertFalse(CompatibilityRegistry.DEFAULT.resolve(id).extended());
            assertFalse(CompatibilityRegistry.DEFAULT.resolve(id).rules().enabled(ClientRule.PREDICT_HOTBAR_DROPS));
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
