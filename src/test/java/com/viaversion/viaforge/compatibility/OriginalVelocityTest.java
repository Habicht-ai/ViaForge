package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaversion.api.minecraft.Vector3d;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPackets1_21_9;
import io.netty.buffer.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class OriginalVelocityTest {
    // Tests run against the Java8-downgraded Via classes. Reflection avoids
    // requiring java.lang.Record from the compile-time upstream dependency.
    private static double component(Object vector,String axis)throws Exception {
        return (Double)vector.getClass().getMethod(axis).invoke(vector);
    }
    @Test public void preservesDecodedWireValuesWithoutShortQuantizationOrClipping() throws Exception {
        for (Vector3d value : new Vector3d[]{new Vector3d(0,0,.3478),new Vector3d(4.25,-8.125,12.75),Vector3d.ZERO}) {
            ByteBuf source=Unpooled.buffer(),legacy=Unpooled.buffer();ByteBuf event=null;
            try {
                Types.VAR_INT.writePrimitive(source,ClientboundPackets1_21_9.SET_ENTITY_MOTION.getId());
                Types.VAR_INT.writePrimitive(source,1234);Types.LOW_PRECISION_VECTOR.write(source,value);
                int index=source.readerIndex();OriginalVelocity retained=OriginalVelocity.capture(source);
                assertEquals(index,source.readerIndex());assertEquals(1234,retained.entityId);
                ByteBuf wire=source.duplicate();Types.VAR_INT.readPrimitive(wire);Types.VAR_INT.readPrimitive(wire);
                Vector3d expected=Types.LOW_PRECISION_VECTOR.read(wire);
                Types.VAR_INT.writePrimitive(legacy,0x12);Types.VAR_INT.writePrimitive(legacy,1234);
                assertTrue(retained.replaces(legacy));assertEquals(0,legacy.readerIndex());
                event=retained.event();assertEquals(0x3f,Types.VAR_INT.readPrimitive(event));
                assertEquals("VF|entity",Types.STRING.read(event));assertEquals(37,ClientEventEnvelope.read(event).operation);
                assertEquals(1234,Types.VAR_INT.readPrimitive(event));
                assertEquals(component(expected,"x"),event.readDouble(),0);assertEquals(component(expected,"y"),event.readDouble(),0);assertEquals(component(expected,"z"),event.readDouble(),0);
                assertFalse(event.isReadable());
                if(component(value,"z")==.3478)assertTrue(Math.abs(component(expected,"z")-.34775)>1e-5);
            } finally {source.release();legacy.release();if(event!=null)event.release();}
        }
    }

    @Test public void doesNotReplaceUnrelatedPacketsOrOtherEntities() {
        ByteBuf source=Unpooled.buffer(),other=Unpooled.buffer();
        try {
            Types.VAR_INT.writePrimitive(source,ClientboundPackets1_21_9.SET_ENTITY_MOTION.getId());
            Types.VAR_INT.writePrimitive(source,1);source.writeByte(0);
            OriginalVelocity retained=OriginalVelocity.capture(source);
            other.writeByte(0x12).writeByte(2);assertFalse(retained.replaces(other));
            other.clear().writeByte(0x1c);assertFalse(retained.replaces(other));
            other.clear().writeByte(ClientboundPackets1_21_9.SET_ENTITY_DATA.getId());assertNull(OriginalVelocity.capture(other));
        } finally {source.release();other.release();}
    }
}
