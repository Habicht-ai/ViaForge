package com.viaversion.viaforge.mobs;

import com.viaversion.viaforge.common.blocks.MobKind;
import com.viaversion.viaforge.items.ServerEntityViews;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.util.MathHelper;

/** Runs after Via has updated its trackers. Only the client representation is replaced. */
public final class ServerMobs {
    private static final Map<Integer, MobState> MOBS = new HashMap<>();
    public static void clear() { MOBS.clear(); }
    public static void remove(int id) { MOBS.remove(id); }
    public static MobState get(Entity entity) { return entity == null ? null : MOBS.get(entity.getEntityId()); }
    public static void accept(WorldClient world, int protocol, int operation, ByteBuf input) throws Exception {
        if (operation == 14 || operation == 15) { ServerMobSounds.packet(world,protocol,operation,input); return; }
        if (operation == 16) {
            int id = Types.VAR_INT.readPrimitive(input); input.skipBytes(16); int type = input.readUnsignedByte();
            double x = input.readDouble(), y = input.readDouble(), z = input.readDouble();
            float pitch = input.readByte()*360F/256, yaw = input.readByte()*360F/256; input.readInt();
            double vx = input.readShort()/8000D, vy = input.readShort()/8000D, vz = input.readShort()/8000D;
            ServerMobProjectile entity = new ServerMobProjectile(world,type);
            entity.setPositionAndRotation(x,y,z,yaw,pitch); entity.prevRotationYaw = yaw; entity.prevRotationPitch = pitch;
            entity.serverPosX = MathHelper.floor_double(x*32); entity.serverPosY = MathHelper.floor_double(y*32); entity.serverPosZ = MathHelper.floor_double(z*32);
            if (type == 93) { double length = Math.sqrt(vx*vx+vy*vy+vz*vz); if (length != 0) { entity.accelerationX = vx/length*.1; entity.accelerationY = vy/length*.1; entity.accelerationZ = vz/length*.1; } }
            else entity.setVelocity(vx,vy,vz);
            world.addEntityToWorld(id,entity);
            if (type == 68) for (int i=0;i<7;i++) MobParticles.spawn(world,48,x,y,z,vx*(.4+i*.1),vy,vz*(.4+i*.1));
            return;
        }
        if (operation == 11) {
            int id = Types.VAR_INT.readPrimitive(input);
            UUID uuid = new UUID(input.readLong(), input.readLong());
            int type = protocol >= 315 ? Types.VAR_INT.readPrimitive(input) : input.readUnsignedByte();
            double x = input.readDouble(), y = input.readDouble(), z = input.readDouble();
            float yaw = input.readByte() * 360F / 256, pitch = input.readByte() * 360F / 256, head = input.readByte() * 360F / 256;
            double vx = input.readShort() / 8000D, vy = input.readShort() / 8000D, vz = input.readShort() / 8000D;
            MobState state = new MobState(protocol, type);
            state.update((protocol >= 335 ? Types.ENTITY_DATA_LIST1_12 : Types.ENTITY_DATA_LIST1_9).read(input));
            if (state.kind == null) return;
            MOBS.put(id, state);
            Entity entity = world.getEntityByID(id);
            if (state.kind.custom()) {
                ServerMob mob = new ServerMob(world, state, uuid);
                mob.setPositionAndRotation(x, y, z, yaw, pitch);
                mob.prevRotationYaw = mob.renderYawOffset = mob.prevRenderYawOffset = yaw;
                mob.prevRotationPitch = pitch; mob.rotationYawHead = mob.prevRotationYawHead = head;
                mob.serverPosX = MathHelper.floor_double(x * 32); mob.serverPosY = MathHelper.floor_double(y * 32); mob.serverPosZ = MathHelper.floor_double(z * 32);
                mob.setVelocity(vx, vy, vz);
                world.addEntityToWorld(id, mob);
                mob.applyMetadata();
            } else if (entity instanceof EntityRabbit) {
                ((com.viaversion.viaforge.mixin.impl.mobs.MobSizeAccess)entity).viaForge$size(.4F, .5F);
                entity.setPosition(entity.posX, entity.posY, entity.posZ);
                // setGrowingAge preserves the native child's scale after changing the adult dimensions.
                ((EntityRabbit)entity).setGrowingAge(state.flag(state.first) ? -24000 : 0);
            }
        } else if (operation == 12) {
            Entity entity = world.getEntityByID(input.readInt()); int status = input.readUnsignedByte();
            // 1.8 processes the shared statuses itself; Via cancels the later additions.
            if (get(entity) != null && status > 23) entity.handleStatusUpdate((byte)status);
            if (entity instanceof ServerMobProjectile) entity.handleStatusUpdate((byte)status);
        } else if (operation == 13) {
            int vehicle = Types.VAR_INT.readPrimitive(input), count = Types.VAR_INT.readPrimitive(input);
            Entity mount = world.getEntityByID(vehicle);
            for (int i = 0; i < count; i++) {
                Entity passenger = world.getEntityByID(Types.VAR_INT.readPrimitive(input));
                if (i == 0 && mount instanceof ServerMob && passenger != null && passenger.ridingEntity != mount) passenger.mountEntity(mount);
            }
            if (count == 0 && mount instanceof ServerMob && mount.riddenByEntity != null) mount.riddenByEntity.mountEntity(null);
        }
    }
    public static void metadata(WorldClient world, int id, List<EntityData> entries) {
        Entity entity = world.getEntityByID(id);
        if (entity instanceof ServerMobProjectile) {
            ((ServerMobProjectile)entity).applyMetadata(entries);
            return;
        }
        MobState state = MOBS.get(id);
        if (state == null) return;
        state.update(entries);
        if (entity instanceof ServerMob) ((ServerMob)entity).applyMetadata();
    }
    public static void equipment(WorldClient world, int id, int slot, ByteBuf input) throws Exception {
        Entity entity = world.getEntityByID(id);
        if (!(entity instanceof ServerMob)) return;
        net.minecraft.item.ItemStack stack = ServerEntityViews.item(Types.ITEM1_8.read(input));
        if (slot == 1) ((ServerMob)entity).offhand = stack;
        else if (slot >= 0 && slot <= 5) ((ServerMob)entity).setCurrentItemOrArmor(slot == 0 ? 0 : slot - 1, stack);
    }
    private ServerMobs() { }
}
