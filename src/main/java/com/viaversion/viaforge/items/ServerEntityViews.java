package com.viaversion.viaforge.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.blocks.LegacyItemDefinition;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

/** Main-thread, world-scoped visuals. Effects and inventory authority stay on the server. */
public final class ServerEntityViews {
    public static final class View {
        public final int type;
        public ItemStack potion, offhand;
        public int handState;
        public boolean leftHanded, fallFlying;
        public boolean crystalBase = true;
        public net.minecraft.util.BlockPos crystalBeam;
        public com.viaversion.viaforge.mobs.ServerMob leftShoulder, rightShoulder;
        View(int type) { this.type = type; }
    }
    private static WorldClient world;
    private static final Map<Integer, View> VIEWS = new HashMap<>();
    public static void clear() { VIEWS.clear(); world = null; ServerTotemAnimation.clear(); ServerCombatState.clear(); ServerItemCooldowns.clear(); com.viaversion.viaforge.hands.Offhand.clear(); com.viaversion.viaforge.mobs.ServerMobs.clear(); com.viaversion.viaforge.boats.ServerBoats.clear(); }
    public static View get(int id) { return world == Minecraft.getMinecraft().theWorld ? VIEWS.get(id) : null; }
    public static boolean blocking(EntityLivingBase entity, boolean offhand, ItemStack stack) {
        if (!ClientItems.is(stack, com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind.SHIELD)) return false;
        if (entity == Minecraft.getMinecraft().thePlayer) return (offhand==(com.viaversion.viaforge.hands.Offhand.useHand==1)) && ((EntityPlayer)entity).getItemInUse() == stack;
        View view = get(entity.getEntityId());
        return view != null && (view.handState & 1) != 0 && ((view.handState & 2) != 0) == offhand;
    }
    public static void accept(ByteBuf input) throws Exception {
        com.viaversion.viaforge.common.compatibility.ClientEventEnvelope envelope=com.viaversion.viaforge.common.compatibility.ClientEventEnvelope.read(input);
        int protocol=envelope.format.revision(),operation=envelope.operation; // Internal layout only; behavior comes from ServerSession.
        if(!ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientEventEnvelope.feature(operation))
                && !(operation==13&&ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.BOATS)))return;
        if (!ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.ENTITY_VISUALS)) return;
        WorldClient current = Minecraft.getMinecraft().theWorld;
        if (world != current || operation == 0) { clear(); world = current; }
        if (operation == 6) ServerItemCooldowns.clear();
        // A same-dimension respawn retains the native world and its tracked entities.
        if (world == null || operation == 0 || operation == 6) return;
        if (operation == 7) {
            int event = input.readInt();
            net.minecraft.util.BlockPos pos = net.minecraft.util.BlockPos.fromLong(input.readLong());
            ServerPotionImpact.play(world, protocol, event, pos, input.readInt());
            return;
        }
        if (operation == 8) {
            net.minecraft.entity.Entity entity = world.getEntityByID(input.readInt());
            if (input.readByte() == 35 && protocol >= 315 && entity != null) ServerTotemAnimation.activate(entity);
            return;
        }
        if (operation == 9) { ServerParticlePackets.accept(world, input); return; }
        if (operation == 10) { ServerCombatState.attributes(input); return; }
        if (operation == 17 || operation == 18) { ServerPotions.accept(world, operation, input); return; }
        if (operation == 19) { ServerItemCooldowns.accept(input); return; }
        if (operation == 26) { com.viaversion.viaforge.hands.Offhand.accept(input); return; }
        if (operation == 27) { com.viaversion.viaforge.hands.Offhand.animation(input); return; }
        if (operation == 20) { com.viaversion.viaforge.boats.ServerBoats.spawn(world, protocol, input); return; }
        if (operation >= 21 && operation <= 25) { com.viaversion.viaforge.boats.ServerBoats.motion(world, operation, input); return; }
        if (operation == 13 && com.viaversion.viaforge.boats.ServerBoats.seats(world, input.duplicate())) return;
        if (operation >= 11) { com.viaversion.viaforge.mobs.ServerMobs.accept(world, protocol, operation, input); return; }
        int entityId = operation == 4 ? -1 : Types.VAR_INT.readPrimitive(input);
        int first = protocol >= 210 ? 6 : 5;
        switch (operation) {
            case 1: {
                input.skipBytes(16); int type = input.readUnsignedByte();
                double x = input.readDouble(), y = input.readDouble(), z = input.readDouble();
                if (type != 3 && type != 73 && type != 60 && type != 91 && type != 51) return;
                View view = new View(type); VIEWS.put(entityId, view);
                if (type == 3) {
                    ServerAreaEffectCloud cloud = new ServerAreaEffectCloud(world);
                    cloud.setPosition(x, y, z);
                    cloud.serverPosX = (int)Math.floor(x * 32); cloud.serverPosY = (int)Math.floor(y * 32); cloud.serverPosZ = (int)Math.floor(z * 32);
                    world.addEntityToWorld(entityId, cloud);
                } else if (type == 60 || type == 91) {
                    float pitch = input.readByte() * 360F / 256F, yaw = input.readByte() * 360F / 256F;
                    int owner = input.readInt() - 1;
                    ServerArrow arrow = new ServerArrow(world, type == 91);
                    arrow.setPositionAndRotation(x, y, z, yaw, pitch);
                    arrow.prevRotationYaw = yaw; arrow.prevRotationPitch = pitch;
                    arrow.serverPosX = (int)Math.floor(x * 32); arrow.serverPosY = (int)Math.floor(y * 32); arrow.serverPosZ = (int)Math.floor(z * 32);
                    arrow.setVelocity(input.readShort() / 8000D, input.readShort() / 8000D, input.readShort() / 8000D);
                    if (world.getEntityByID(owner) instanceof EntityLivingBase) arrow.shootingEntity = (EntityLivingBase)world.getEntityByID(owner);
                    world.addEntityToWorld(entityId, arrow);
                } else if (type == 73) view.potion = item(new com.viaversion.viaversion.api.minecraft.item.DataItem(438, (byte)1, (short)0, null));
                break;
            }
            case 5:
                input.skipBytes(42); // UUID, position and rotation
                VIEWS.put(entityId, new View(-1));
                metadata(entityId, first, (protocol >= 335 ? Types.ENTITY_DATA_LIST1_12 : Types.ENTITY_DATA_LIST1_9).read(input));
                break;
            case 2: {
                List<EntityData> data = (protocol >= 335 ? Types.ENTITY_DATA_LIST1_12 : Types.ENTITY_DATA_LIST1_9).read(input);
                if (world.getEntityByID(entityId) instanceof com.viaversion.viaforge.boats.ServerBoat) ((com.viaversion.viaforge.boats.ServerBoat)world.getEntityByID(entityId)).metadata(data);
                com.viaversion.viaforge.mobs.ServerMobs.metadata(world, entityId, data);
                metadata(entityId, first, data); break;
            }
            case 3: {
                int slot = Types.VAR_INT.readPrimitive(input);
                com.viaversion.viaforge.mobs.ServerMobs.equipment(world, entityId, slot, input.duplicate());
                if (slot != 1) break;
                View view = VIEWS.get(entityId);
                if (view == null && world.getEntityByID(entityId) instanceof EntityPlayer) { view = new View(-1); VIEWS.put(entityId, view); }
                if (view != null && view.type == -1) view.offhand = item(Types.ITEM1_8.read(input));
                break;
            }
            case 4:
                int count = Types.VAR_INT.readPrimitive(input);
                for (int i = 0; i < count; i++) {
                    int id = Types.VAR_INT.readPrimitive(input); VIEWS.remove(id); com.viaversion.viaforge.hands.Offhand.remove(id); com.viaversion.viaforge.mobs.ServerMobs.remove(id); com.viaversion.viaforge.boats.ServerBoats.remove(world, id);
                }
                break;
            default: break;
        }
    }
    private static void metadata(int id, int first, List<EntityData> entries) throws Exception {
        View view = VIEWS.get(id);
        if (view == null && world.getEntityByID(id) instanceof EntityPlayer) { view = new View(-1); VIEWS.put(id, view); }
        if (view == null) return;
        for (EntityData data : entries) {
            Object value = data.getValue();
            if (view.type == 73 && data.id() == first && data.dataType().type() == Types.ITEM1_8) {
                ItemStack stack = item((com.viaversion.viaversion.api.minecraft.item.Item)value);
                if (ClientItems.is(stack, com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind.LINGERING)
                        || ClientItems.is(stack, com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind.SPLASH)) view.potion = stack;
            } else if (view.type == 3 && world.getEntityByID(id) instanceof ServerAreaEffectCloud) {
                ((ServerAreaEffectCloud)world.getEntityByID(id)).metadata(data.id() - first, value);
            } else if ((view.type == 60 || view.type == 91) && world.getEntityByID(id) instanceof ServerArrow) {
                ServerArrow arrow = (ServerArrow)world.getEntityByID(id);
                if (data.id() == first && value instanceof Byte) arrow.setIsCritical(((Byte)value & 1) != 0);
                if (view.type == 60 && data.id() == first + 1 && value instanceof Integer) arrow.color = (Integer)value;
            } else if (view.type == 51) {
                if (data.id() == first && (value == null || value instanceof com.viaversion.viaversion.api.minecraft.BlockPosition)) {
                    com.viaversion.viaversion.api.minecraft.BlockPosition pos = (com.viaversion.viaversion.api.minecraft.BlockPosition)value;
                    view.crystalBeam = pos == null ? null : new net.minecraft.util.BlockPos(pos.x(), pos.y(), pos.z());
                }
                if (data.id() == first + 1 && value instanceof Boolean) view.crystalBase = (Boolean)value;
            } else if (view.type == -1 && value instanceof Byte) {
                if (data.id() == 0) view.fallFlying = ((Byte)value & 128) != 0;
                if (data.id() == first) view.handState = (Byte)value;
                if (data.id() == first + 8) view.leftHanded = (Byte)value == 0;
            } else if (view.type == -1 && (data.id() == first + 9 || data.id() == first + 10)) {
                com.viaversion.viaforge.mobs.ServerMob parrot = com.viaversion.viaforge.mobs.ShoulderParrots.create(world,value);
                if (data.id() == first + 9) view.leftShoulder = parrot; else view.rightShoulder = parrot;
            }
        }
    }
    public static ItemStack item(com.viaversion.viaversion.api.minecraft.item.Item source) throws Exception {
        if (source == null) return null;
        int local = ClientItems.localItem(source.identifier(), source.data());
        com.viaversion.viaversion.api.minecraft.item.Item copy = source.copy();
        if (local >= 0) {
            LegacyItemDefinition definition = ClientItems.serverItem(local);
            if (!ServerSession.supportsItem(definition)) return null;
            copy.setIdentifier(local); if (!definition.preservesDamage()) copy.setData((short)0);
        } else if (source.identifier() >= 198 && source.identifier() < 256 || source.identifier() > 425) return null;
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try { Types.ITEM1_8.write(buffer, copy); return buffer.readItemStackFromBuffer(); }
        finally { buffer.release(); }
    }
    private ServerEntityViews() { }
}
