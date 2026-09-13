package com.viaversion.viaforge.boats;

import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;

/** Receives original coordinates and seat lists after Via has maintained its trackers. */
public final class ServerBoats {
    private static final Map<Integer,int[]> SEATS=new HashMap<>();
    public static void clear() { SEATS.clear(); }
    public static void remove(WorldClient world,int id) {
        SEATS.remove(id);
        for(Map.Entry<Integer,int[]> entry:SEATS.entrySet()) entry.setValue(Arrays.stream(entry.getValue()).filter(rider -> rider!=id).toArray());
        Entity entity=world.getEntityByID(id);
        if(entity instanceof ServerBoat) ((ServerBoat)entity).seats(Collections.emptyList());
        if(entity!=null && entity.ridingEntity instanceof ServerBoat) ((ServerBoat)entity.ridingEntity).detach(entity);
        resolve(world);
    }
    public static void spawn(WorldClient world,int protocol,ByteBuf input) {
        int id=Types.VAR_INT.readPrimitive(input); input.skipBytes(16); input.readByte();
        ServerBoat boat=new ServerBoat(world,protocol);
        double x=input.readDouble(),y=input.readDouble(),z=input.readDouble();
        float pitch=input.readByte()*360F/256,yaw=input.readByte()*360F/256; input.readInt();
        boat.correction(x,y,z,yaw,pitch); boat.prevRotationYaw=yaw; boat.prevRotationPitch=pitch;
        boat.setVelocity(input.readShort()/8000D,input.readShort()/8000D,input.readShort()/8000D);
        world.addEntityToWorld(id,boat); resolve(world);
    }
    public static boolean seats(WorldClient world,ByteBuf input) {
        int vehicle=Types.VAR_INT.readPrimitive(input),count=Types.VAR_INT.readPrimitive(input); int[] riders=new int[count];
        for(int i=0;i<count;i++) riders[i]=Types.VAR_INT.readPrimitive(input);
        for (Map.Entry<Integer,int[]> entry:SEATS.entrySet()) if(entry.getKey()!=vehicle)
            entry.setValue(Arrays.stream(entry.getValue()).filter(id -> Arrays.stream(riders).noneMatch(next -> next==id)).toArray());
        if(!(world.getEntityByID(vehicle) instanceof ServerBoat)) {
            SEATS.remove(vehicle);
            for(int id:riders) { Entity rider=entity(world,id); if(rider!=null && rider.ridingEntity instanceof ServerBoat) ((ServerBoat)rider.ridingEntity).detach(rider); }
            resolve(world);
            return false;
        }
        SEATS.put(vehicle,riders); resolve(world); return true;
    }
    public static void resolve(WorldClient world) {
        for(Map.Entry<Integer,int[]> entry:SEATS.entrySet()) {
            Entity entity=world.getEntityByID(entry.getKey()); if(!(entity instanceof ServerBoat)) continue;
            List<Entity> riders=new ArrayList<>();
            for(int id:entry.getValue()) { Entity rider=entity(world,id); if(rider!=null) riders.add(rider); }
            // A delayed driver spawn must never promote the second seat to driver.
            if(riders.size()!=entry.getValue().length) continue;
            ServerBoat boat=(ServerBoat)entity;
            if(!boat.passengers().equals(riders)) boat.seats(riders);
        }
    }
    private static Entity entity(WorldClient world,int id) {
        Entity entity=world.getEntityByID(id);
        return entity==null && Minecraft.getMinecraft().thePlayer!=null && Minecraft.getMinecraft().thePlayer.getEntityId()==id ? Minecraft.getMinecraft().thePlayer:entity;
    }
    public static void motion(WorldClient world,int operation,ByteBuf input) {
        if(operation==25) {
            Entity player=Minecraft.getMinecraft().thePlayer;
            if(player!=null && player.ridingEntity instanceof ServerBoat) ((ServerBoat)player.ridingEntity).correction(input.readDouble(),input.readDouble(),input.readDouble(),input.readFloat(),input.readFloat());
            return;
        }
        Entity entity=world.getEntityByID(Types.VAR_INT.readPrimitive(input)); if(!(entity instanceof ServerBoat)) return;
        ServerBoat boat=(ServerBoat)entity;
        if(operation==24) { boat.wireX=input.readDouble(); boat.wireY=input.readDouble(); boat.wireZ=input.readDouble(); }
        else if(operation==21 || operation==22) { boat.wireX+=input.readShort()/4096D; boat.wireY+=input.readShort()/4096D; boat.wireZ+=input.readShort()/4096D; }
        if(operation==22 || operation==23 || operation==24) { boat.wireYaw=input.readByte()*360F/256; boat.wirePitch=input.readByte()*360F/256; }
        boat.onGround=input.readBoolean(); boat.setPositionAndRotation2(boat.wireX,boat.wireY,boat.wireZ,boat.wireYaw,boat.wirePitch,10,false);
    }
    private ServerBoats() { }
}
