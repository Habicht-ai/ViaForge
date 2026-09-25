package com.viaversion.viaforge.development;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.SharedMonsterAttributes;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;

/** Development-only observation. Never writes player state or sends packets. */
public final class MovementTrace {
    // Capture timestamps/bytes on the producer, format them on the observing
    // client thread. JSON/hex formatting on Netty perturbs the race we measure.
    private static final java.util.Queue<PacketObservation> packets=new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final class PacketObservation {
        final long time=System.currentTimeMillis();
        final String thread=Thread.currentThread().getName(),phase,channel;
        final byte[] bytes;
        final net.minecraft.network.play.server.S1CPacketEntityMetadata metadata;
        PacketObservation(String phase,String channel,io.netty.buffer.ByteBuf data,int length) {
            this.phase=phase;this.channel=channel;metadata=null;bytes=new byte[length];
            data.getBytes(data.readerIndex(),bytes);
        }
        PacketObservation(String phase,net.minecraft.network.play.server.S1CPacketEntityMetadata metadata) {
            this.phase=phase;this.channel="legacy-metadata";this.metadata=metadata;bytes=null;
        }
        String json() {
            JsonObject j=new JsonObject();j.addProperty("phase",phase);j.addProperty("time_ms",time);
            j.addProperty("thread",thread);if(channel!=null)j.addProperty("channel",channel);
            if(metadata==null) {
                // 1.8 ships Netty 4.0, without hexDump(byte[]).
                char[] hex=new char[bytes.length*2];String digits="0123456789abcdef";
                for(int i=0;i<bytes.length;i++){hex[i*2]=digits.charAt((bytes[i]&255)>>>4);hex[i*2+1]=digits.charAt(bytes[i]&15);}
                j.addProperty("bytes",new String(hex));
            }
            else {
                net.minecraft.network.PacketBuffer buffer=new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());
                try {metadata.writePacketData(buffer);j.addProperty("bytes",io.netty.buffer.ByteBufUtil.hexDump(buffer));}
                catch(java.io.IOException error){throw new IllegalStateException(error);}
                finally {buffer.release();}
            }
            return j.toString();
        }
    }
    public static void metadata(net.minecraft.network.play.server.S1CPacketEntityMetadata packet,String phase) {
        // The decoded packet's entries are immutable during native handling;
        // DataWatcher copies their values into its separate existing entries.
        packets.add(new PacketObservation(phase,packet));
    }
    public static void transport(io.netty.buffer.ByteBuf data) {
        packets.add(new PacketObservation("RAW_RECEIVE",null,data,Math.min(24,data.readableBytes())));
    }
    public static void packet(String phase,String channel,io.netty.buffer.ByteBuf data) {
        if(!channel.equals("VF|entity")&&!channel.equals("VF|pong")&&!channel.equals("legacy-metadata"))return;
        if(channel.equals("VF|entity")&&(data.readableBytes()<3||data.getUnsignedByte(data.readerIndex()+2)!=2))return;
        packets.add(new PacketObservation(phase,channel,data,data.readableBytes()));
    }
    public static void sample(Object entity,String phase) {
        String folder=System.getenv("VIAFORGE_SNEAK_PROBE");
        if(folder==null||entity!=Minecraft.getMinecraft().thePlayer)return;
        EntityPlayerSP p=(EntityPlayerSP)entity;
        if(p.ticksExisted>12000)return;
        try {
            JsonObject j=new JsonObject();j.addProperty("phase",phase);j.addProperty("time_ms",System.currentTimeMillis());j.addProperty("player_tick",p.ticksExisted);
            j.addProperty("key_forward",Minecraft.getMinecraft().gameSettings.keyBindForward.isKeyDown());j.addProperty("key_back",Minecraft.getMinecraft().gameSettings.keyBindBack.isKeyDown());
            j.addProperty("key_left",Minecraft.getMinecraft().gameSettings.keyBindLeft.isKeyDown());j.addProperty("key_right",Minecraft.getMinecraft().gameSettings.keyBindRight.isKeyDown());
            j.addProperty("key_sneak",Minecraft.getMinecraft().gameSettings.keyBindSneak.isKeyDown());j.addProperty("key_sprint",Minecraft.getMinecraft().gameSettings.keyBindSprint.isKeyDown());j.addProperty("key_jump",Minecraft.getMinecraft().gameSettings.keyBindJump.isKeyDown());
            j.addProperty("input_forward",p.movementInput.moveForward);j.addProperty("input_strafe",p.movementInput.moveStrafe);j.addProperty("input_sneak",p.movementInput.sneak);
            j.addProperty("travel_forward",p.moveForward);j.addProperty("travel_strafe",p.moveStrafing);
            j.addProperty("sprinting",p.isSprinting());j.addProperty("sneaking",p.isSneaking());j.addProperty("native_sprint_ticks",p.sprintingTicksLeft);
            j.addProperty("speed_attribute",p.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue());
            j.addProperty("height",p.height);j.addProperty("eye_height",p.getEyeHeight());j.addProperty("water",p.isInWater());j.addProperty("eye_water",com.viaversion.viaforge.compatibility.ServerSwimming.eye(p));
            j.addProperty("swimming",com.viaversion.viaforge.compatibility.ServerSwimming.swimming(p));j.addProperty("crawling",com.viaversion.viaforge.items.ServerElytraFlight.crawling(p));
            j.addProperty("elytra",com.viaversion.viaforge.items.ServerElytraFlight.flying(p));
            j.addProperty("x",p.posX);j.addProperty("y",p.posY);j.addProperty("z",p.posZ);j.addProperty("vx",p.motionX);j.addProperty("vy",p.motionY);j.addProperty("vz",p.motionZ);
            j.addProperty("box",p.getEntityBoundingBox().toString());j.addProperty("ground",p.onGround);j.addProperty("collision_h",p.isCollidedHorizontally);j.addProperty("collision_v",p.isCollidedVertically);
            java.lang.reflect.Field states=com.viaversion.viaforge.compatibility.ServerSwimming.class.getDeclaredField("STATES");states.setAccessible(true);
            Object state=((java.util.Map<?,?>)states.get(null)).get(p);
            if(state!=null)for(String name:new String[]{"previousSneak","previousForward","slowMovement","sprintWindow"}) {
                java.lang.reflect.Field f=state.getClass().getDeclaredField(name);f.setAccessible(true);Object value=f.get(state);
                if(value instanceof Boolean)j.addProperty(name,(Boolean)value);else if(value instanceof Number)j.addProperty(name,(Number)value);
            }
            Files.write(Paths.get(folder,"movement.jsonl"),Collections.singleton(j.toString()),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            if(!packets.isEmpty()) {
                java.util.List<String> batch=new java.util.ArrayList<>();PacketObservation entry;
                while((entry=packets.poll())!=null)batch.add(entry.json());
                Files.write(Paths.get(folder,"packet-order.jsonl"),batch,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }
        }catch(Exception failure){throw new IllegalStateException("Movement observation failed",failure);}
    }
    private MovementTrace(){}
}
