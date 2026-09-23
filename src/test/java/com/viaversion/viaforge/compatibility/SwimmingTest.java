package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SwimmingTest {
    @Test public void zeroSneakingSpeedProducesFiniteStationaryInput() {
        assertArrayEquals(new float[]{0,0},SwimmingPhysics.squareInput(0,1,1,0),0);
        assertArrayEquals(new float[]{0,0},SwimmingPhysics.squareInput(1,1,1,0),0);
    }
    @Test public void targetBoundariesAreIndependentOfInternal340Format() {
        for(int p:new int[]{47,340,393,401,404,477,578,735,754,755,757,758,761,762,767,770,774,775,776,777,778}) {
            VersionRules r=CompatibilityRegistry.DEFAULT.resolve(p).rules();
            assertEquals(p>=393&&p<=777,r.enabled(ClientRule.SWIMMING));
            assertEquals(p>=477&&p<=777,r.enabled(ClientRule.DOUBLE_SWIM_INPUT));
            assertEquals(p>=477&&p<=498,r.enabled(ClientRule.SHIFT_SWIM_INPUT));
            assertEquals(p>=735&&p<=777,r.enabled(ClientRule.MINIMUM_FLUID_CURRENT));
            assertEquals(p>=755&&p<=777,r.enabled(ClientRule.SWIM_FEET_IN_WATER));
            assertEquals(p>=758&&p<=777,r.enabled(ClientRule.PRECISE_MOVEMENT_PACKETS));
            assertEquals(p>=762&&p<=777,r.enabled(ClientRule.CURRENT_AIR_SPRINT));
            assertEquals(p>=767&&p<=777,r.enabled(ClientRule.WATER_EFFICIENCY_ATTRIBUTE));
            assertEquals(p>=759&&p<=777,r.enabled(ClientRule.SWIFT_SNEAK));
            assertEquals(p>=767&&p<=777,r.enabled(ClientRule.SNEAK_SPEED_ATTRIBUTE));
            assertEquals(p>=775&&p<=777,r.enabled(ClientRule.ENTITY_FLUID_TRACKER));
        }
    }
    @Test public void lookBuoyancyAndSurfaceExitFollowVanilla() {
        assertEquals(.03,SwimmingPhysics.lookY(0,.5,false,true),0);
        assertEquals(0,SwimmingPhysics.lookY(0,.5,false,false),0);
        assertEquals(.03,SwimmingPhysics.lookY(0,.5,true,false),0);
        assertEquals(-.0425,SwimmingPhysics.lookY(0,-.5,false,false),0);
        assertEquals(-.003,SwimmingPhysics.falling(.002,.08,true,false),0);
        assertEquals(.002,SwimmingPhysics.falling(.002,.08,true,true),0);
        float[] plain=SwimmingPhysics.waterFactors(true,0,false,.13F,false);
        assertArrayEquals(new float[]{.9F,.02F},plain,0);
        assertArrayEquals(new float[]{.54600006F,.13F},SwimmingPhysics.waterFactors(false,3,true,.13F,false),0);
        assertEquals(.96F,SwimmingPhysics.waterFactors(false,3,true,.13F,true)[0],0);
    }
    @Test public void metadataSurvivesTheLossyBoundaryWithoutConsumingOriginal() throws Exception {
        ByteBuf wire=Unpooled.buffer(),event=null;
        try {
            Types.VAR_INT.writePrimitive(wire,com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13.SET_ENTITY_DATA.getId());
            Types.VAR_INT.writePrimitive(wire,91);wire.writeByte(0).writeByte(0).writeByte(0x10).writeByte(255);
            event=SwimmingPackets.capture(wire,false);assertEquals(0,wire.readerIndex());
            assertEquals(0x3f,Types.VAR_INT.readPrimitive(event));assertEquals("VF|entity",Types.STRING.read(event));
            assertEquals(34,ClientEventEnvelope.read(event).operation);assertEquals(91,Types.VAR_INT.readPrimitive(event));
            assertEquals(1,event.readByte());assertEquals(-1,event.readByte());assertFalse(event.isReadable());
        }finally{wire.release();if(event!=null)event.release();}
    }
    @Test public void poseAndAquaticEffectPreservation() throws Exception {
        ByteBuf wire=Unpooled.buffer(),event=null;
        try {
            Types.VAR_INT.writePrimitive(wire,com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14.SET_ENTITY_DATA.getId());
            Types.VAR_INT.writePrimitive(wire,4);wire.writeByte(6).writeByte(18).writeByte(3).writeByte(255);
            event=SwimmingPackets.capture(wire,true);Types.VAR_INT.readPrimitive(event);Types.STRING.read(event);
            assertEquals(34,ClientEventEnvelope.read(event).operation);assertEquals(4,Types.VAR_INT.readPrimitive(event));
            assertEquals(-1,event.readByte());assertEquals(3,event.readByte());event.release();event=null;wire.clear();
            Types.VAR_INT.writePrimitive(wire,com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13.UPDATE_MOB_EFFECT.getId());
            Types.VAR_INT.writePrimitive(wire,4);wire.writeByte(30).writeByte(2);Types.VAR_INT.writePrimitive(wire,600);wire.writeByte(3);
            event=SwimmingPackets.capture(wire,false);Types.VAR_INT.readPrimitive(event);Types.STRING.read(event);
            assertEquals(17,ClientEventEnvelope.read(event).operation);assertEquals(4,Types.VAR_INT.readPrimitive(event));
            assertEquals(30,event.readByte());assertEquals(2,event.readByte());assertEquals(600,Types.VAR_INT.readPrimitive(event));
        }finally{wire.release();if(event!=null)event.release();}
    }
    @Test public void originalFluidsDistinguishDryWaterloggedAndBubbleStates() throws Exception {
        assertEquals(0,SwimmingFluids.descriptor("minecraft:oak_slab[type=bottom,waterlogged=false]"));
        assertEquals(1,SwimmingFluids.descriptor("minecraft:oak_slab[type=bottom,waterlogged=true]"));
        assertEquals(8,SwimmingFluids.descriptor("minecraft:water[level=7]"));
        assertEquals(18,SwimmingFluids.descriptor("minecraft:bubble_column[drag=true]"));
        SwimmingFluids store=new SwimmingFluids();
        ByteBuf packet=SwimmingFluids.block(new com.viaversion.viaversion.api.minecraft.BlockPosition(-1,63,17),17);
        try {
            Types.VAR_INT.readPrimitive(packet);Types.STRING.read(packet);assertEquals(35,ClientEventEnvelope.read(packet).operation);
            store.accept(packet);assertEquals(17,store.get(-1,63,17));assertEquals(-1,store.get(32,63,17));
            store.clear();assertEquals(-1,store.get(-1,63,17));
        }finally{packet.release();}
    }
    @Test public void modernFluidFactsAreRetainedBeforeBlockFallbacks() throws Exception {
        // Independent golden states from the documented Mojang block reports.
        // Each pair differs only in waterlogged; all were introduced after 1.13.
        String[] versions={"1.14","1.17","1.21","26.2"};
        int[] wet={11216,17358,24676,29520}; // campfire, candle, copper grate, copper grate
        for(int i=0;i<versions.length;i++) {
            java.util.function.IntUnaryOperator table=SwimmingFluidRegistry.forVersion(versions[i]);
            assertEquals(versions[i],1,table.applyAsInt(wet[i]));
            assertEquals(versions[i],0,table.applyAsInt(wet[i]+1));
            assertEquals(0,table.applyAsInt(0));assertEquals(0,table.applyAsInt(-1));
        }
        assertEquals(1,SwimmingFluidRegistry.forBoundary(com.viaversion.viabackwards.protocol.v26_2to26_1.Protocol26_2To26_1.class).applyAsInt(29520));
    }

    @Test public void fullDryChunkAndUnloadDiscardPreviousWater() throws Exception {
        SwimmingFluids store=new SwimmingFluids();
        ByteBuf block=SwimmingFluids.block(new com.viaversion.viaversion.api.minecraft.BlockPosition(-1,64,0),18);
        accept(store,block);assertEquals(18,store.get(-1,64,0));
        com.viaversion.viaversion.api.minecraft.chunks.Chunk chunk=new com.viaversion.viaversion.api.minecraft.chunks.BaseChunk(-1,0,true,false,0,
                new com.viaversion.viaversion.api.minecraft.chunks.ChunkSection[16],new int[256],null,new java.util.ArrayList<com.viaversion.nbt.tag.CompoundTag>());
        accept(store,SwimmingFluids.chunk(chunk,id->0));assertEquals(0,store.get(-1,64,0));
        accept(store,SwimmingFluids.unload(-1,0));assertEquals(-1,store.get(-1,64,0));
    }
    private static void accept(SwimmingFluids store,ByteBuf packet) throws Exception {
        try{Types.VAR_INT.readPrimitive(packet);Types.STRING.read(packet);ClientEventEnvelope.read(packet);store.accept(packet);}
        finally{packet.release();}
    }

    @Test public void sneakAttributeSurvivesTranslationWithModifiersAndClamping() throws Exception {
        // Packet arithmetic is independent of a running Via platform. Actual
        // mapping lookup is exercised by the live attribute/equipment cases.
        int id=17;
        for(double additive:new double[]{.45,2}) {
            ByteBuf wire=Unpooled.buffer(),event=null;
            try {
                Types.VAR_INT.writePrimitive(wire,com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21.UPDATE_ATTRIBUTES.getId());
                Types.VAR_INT.writePrimitive(wire,17);Types.VAR_INT.writePrimitive(wire,1);Types.VAR_INT.writePrimitive(wire,id);
                wire.writeDouble(.3);Types.VAR_INT.writePrimitive(wire,1);Types.STRING.write(wire,"minecraft:enchantment.swift_sneak");wire.writeDouble(additive);wire.writeByte(0);
                event=SwimmingPackets.attributes(wire,key->key==17?"minecraft:player.sneaking_speed":null);assertNotNull(event);assertEquals(0,wire.readerIndex());
                Types.VAR_INT.readPrimitive(event);Types.STRING.read(event);assertEquals(36,ClientEventEnvelope.read(event).operation);
                assertEquals(17,Types.VAR_INT.readPrimitive(event));assertEquals(1,event.readUnsignedByte());assertEquals(2,event.readUnsignedByte());
                assertEquals(additive==.45?.75:1,event.readDouble(),0);assertFalse(event.isReadable());
            }finally {wire.release();if(event!=null)event.release();}
        }
    }
}
