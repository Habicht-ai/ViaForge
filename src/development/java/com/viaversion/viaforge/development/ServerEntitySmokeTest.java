package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.item.*;
import net.minecraft.network.*;

/** Original packets pass through the complete compressed Via pipeline and native handlers. */
final class ServerEntitySmokeTest {
    static void pipeline(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, WorldClient world) throws Exception {
        Minecraft mc = Minecraft.getMinecraft(); WorldClient previous = mc.theWorld;
        net.minecraft.client.entity.EntityPlayerSP previousPlayer = mc.thePlayer;
        net.minecraft.entity.Entity previousCamera = mc.getRenderViewEntity();
        NetHandlerPlayClient handler = new NetHandlerPlayClient(mc, null, new NetworkManager(EnumPacketDirection.CLIENTBOUND), new GameProfile(new UUID(0, 17), "VisualTest"));
        Field field = NetHandlerPlayClient.class.getDeclaredField("clientWorldController"); field.setAccessible(true); field.set(handler, world);
        mc.theWorld = world; ServerEntityViews.clear();
        mc.thePlayer = new net.minecraft.client.entity.EntityPlayerSP(mc, world, handler, new net.minecraft.stats.StatFileWriter());
        mc.setRenderViewEntity(mc.thePlayer);
        int first = profile.protocol() >= 210 ? 6 : 5;
        try {
            spawn(profile, client, server, handler, 610, 3);
            require(world.getEntityByID(610) instanceof ServerAreaEffectCloud, "Cloud survives Via cancellation " + profile);
            ServerAreaEffectCloud cloud = (ServerAreaEffectCloud)world.getEntityByID(610);
            ByteBuf metadata = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(metadata, 610);
            metadata.writeByte(first).writeByte(2).writeFloat(2.5F);
            metadata.writeByte(first + 1).writeByte(1); Types.VAR_INT.writePrimitive(metadata, 0x9643bb);
            metadata.writeByte(first + 2).writeByte(6).writeBoolean(true).writeByte(255);
            send(client, server, handler, metadata);
            require(cloud.radius == 2.5F && cloud.width == 5 && cloud.color == 0x9643bb && cloud.waiting, "Cloud metadata layout " + profile);
            metadata = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(metadata, 610);
            metadata.writeByte(first).writeByte(2).writeFloat(1.25F).writeByte(first + 2).writeByte(6).writeBoolean(false).writeByte(255);
            send(client, server, handler, metadata);
            require(cloud.radius == 1.25F && cloud.color == 0x9643bb && !cloud.waiting, "Partial cloud updates preserve color " + profile);
            require((cloud.getEntityBoundingBox().minX + cloud.getEntityBoundingBox().maxX) / 2 == cloud.posX, "Shrinking cloud bounds stay centered");
            cloud.onUpdate(); require(!cloud.isDead && cloud.radius == 1.25F, "Cloud does not invent server lifetime/radius");
            require(mc.getRenderManager().<ServerAreaEffectCloud>getEntityRenderObject(cloud) instanceof ServerCloudRenderer, "Cloud has no fallback bounding-box mesh");
            ByteBuf respawn = packet(profile, "RESPAWN"); respawn.writeInt(0).writeByte(0).writeByte(1); Types.STRING.write(respawn, "default");
            send(client, server, handler, respawn);
            require(ServerEntityViews.get(610) != null && world.getEntityByID(610) == cloud, "Same-dimension respawn retains tracked cloud");

            spawn(profile, client, server, handler, 611, 73);
            require(world.getEntityByID(611) instanceof EntityPotion, "Native thrown potion spawn");
            for (int itemId : new int[]{438, 441}) {
                ItemStack potion = ItemVariants.potion(stack(itemId, 0), "strong_healing");
                com.viaversion.viaversion.api.minecraft.item.Item wire = ServerItemSmokeTest.wire(potion); wire.setIdentifier(itemId);
                metadata = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(metadata, 611);
                metadata.writeByte(first).writeByte(5); Types.ITEM1_8.write(metadata, wire); metadata.writeByte(255);
                send(client, server, handler, metadata);
                ItemStack actual = ServerEntityViews.get(611).potion;
                require(actual.getItem() == potion.getItem() && actual.getTagCompound().equals(potion.getTagCompound()), "Thrown potion type and NBT " + profile);
                metadata = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(metadata, 611); metadata.writeByte(0).writeByte(0).writeByte(0).writeByte(255);
                send(client, server, handler, metadata);
                require(ServerEntityViews.get(611).potion == actual, "Unrelated metadata retains potion model");
            }

            EntityOtherPlayerMP player = new EntityOtherPlayerMP(world, new GameProfile(new UUID(0, 612), "ShieldTest")); world.addEntityToWorld(612, player);
            ByteBuf add = packet(profile, "ADD_PLAYER"); Types.VAR_INT.writePrimitive(add, 612); add.writeLong(0).writeLong(612);
            add.writeDouble(4).writeDouble(65).writeDouble(4).writeByte(0).writeByte(0);
            add.writeByte(first).writeByte(0).writeByte(3).writeByte(255);
            send(client, server, handler, add);
            ByteBuf equipment = packet(profile, "SET_EQUIPPED_ITEM"); Types.VAR_INT.writePrimitive(equipment, 612); Types.VAR_INT.writePrimitive(equipment, 1);
            Types.ITEM1_8.write(equipment, new com.viaversion.viaversion.api.minecraft.item.DataItem(442, (byte)1, (short)117, null));
            send(client, server, handler, equipment);
            ServerEntityViews.View view = ServerEntityViews.get(612);
            require(ClientItems.is(view.offhand, LegacyItemCatalog.Kind.SHIELD) && view.offhand.getItemDamage() == 117, "Offhand shield survives Via equipment cancellation");
            require(ServerEntityViews.blocking(player, true, view.offhand) && !ServerEntityViews.blocking(player, false, view.offhand), "Remote shield active hand");
            metadata = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(metadata, 612); metadata.writeByte(first).writeByte(0).writeByte(0).writeByte(255);
            send(client, server, handler, metadata);
            require(!ServerEntityViews.blocking(player, true, view.offhand), "Remote shield released");
            equipment = packet(profile, "SET_EQUIPPED_ITEM"); Types.VAR_INT.writePrimitive(equipment, 612); Types.VAR_INT.writePrimitive(equipment, 1); Types.ITEM1_8.write(equipment, null);
            send(client, server, handler, equipment); require(view.offhand == null, "Empty equipment clears remote shield");
            ServerEffectsSmokeTest.pipeline(profile, client, server, handler, world);
            ServerCombatSmokeTest.pipeline(profile, client, server, handler, world);
            EndVisualSmokeTest.pipeline(profile, client, server, handler, world);
            ServerMobSmokeTest.pipeline(profile, client, server, handler, world);
            ServerPotionStatusSmokeTest.verify(profile, client, server, handler);
            ItemCooldownSmokeTest.verify(profile, client, server, handler, world);
            DragonHeadSmokeTest.verify(profile, world);
            BoatSmokeTest.verify(profile, client, server, handler, world);
            ByteBuf remove = packet(profile, "REMOVE_ENTITIES"); Types.VAR_INT.writePrimitive(remove, 3);
            for (int id : new int[]{610, 611, 612}) Types.VAR_INT.writePrimitive(remove, id);
            send(client, server, handler, remove);
            require(world.getEntityByID(610) == null && world.getEntityByID(611) == null && ServerEntityViews.get(612) == null, "Entity destroy clears clouds/projectiles/hand state");
        } finally { for (int id : new int[]{610, 611, 612}) world.removeEntityFromWorld(id); ServerEntityViews.clear(); mc.theWorld = previous; mc.thePlayer = previousPlayer; mc.setRenderViewEntity(previousCamera); }
    }
    private static void spawn(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, NetHandlerPlayClient handler, int id, int type) throws Exception {
        ByteBuf add = packet(profile, "ADD_ENTITY"); Types.VAR_INT.writePrimitive(add, id); add.writeLong(0).writeLong(id).writeByte(type);
        add.writeDouble(4).writeDouble(65).writeDouble(4).writeByte(0).writeByte(0).writeInt(1).writeShort(0).writeShort(0).writeShort(0);
        send(client, server, handler, add);
    }
    static ByteBuf packet(BlockVersionProfile profile, String name) { return BlockPipelineSmokeTest.packet(BlockItemPipelineSmokeTest.clientbound(profile, name)); }
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void send(EmbeddedChannel client, EmbeddedChannel server, NetHandlerPlayClient handler, ByteBuf source) throws Exception {
        BlockPipelineSmokeTest.receiveCompressed(client, server, source);
        ByteBuf received;
        while ((received = client.readInbound()) != null) {
            try {
                int id = Types.VAR_INT.readPrimitive(received);
                // Player construction uses a local profile fixture; the visual data
                // still comes from the actual ADD_PLAYER packet through the pipeline.
                if (id != 0x0e && id != 0x0f && id != 0x15 && id != 0x17 && id != 0x18 && id != 0x19 && id != 0x1b && id != 0x1c && id != 0x13 && id != 0x3f && id != 0x04 && id != 0x12 && id != 0x1a && id != 0x23 && id != 0x35 && id != 0x24 && id != 0x21 && id != 0x2d && id != 0x2f && id != 0x30) continue;
                Packet nativePacket = EnumConnectionState.PLAY.getPacket(EnumPacketDirection.CLIENTBOUND, id);
                nativePacket.readPacketData(new PacketBuffer(received)); nativePacket.processPacket(handler);
            } finally { received.release(); }
        }
    }
    static ItemStack stack(int id, int data) { return new ItemStack(Item.getItemById(ClientItems.localItem(id, data))); }
    static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
