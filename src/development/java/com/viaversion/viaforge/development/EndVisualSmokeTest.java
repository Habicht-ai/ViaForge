package com.viaversion.viaforge.development;

import com.viaversion.viaforge.blocks.*;
import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Proxy;
import java.util.*;
import net.minecraft.client.model.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraft.world.IWorldAccess;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.*;

final class EndVisualSmokeTest {
    static void pipeline(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, NetHandlerPlayClient handler, WorldClient world) throws Exception {
        int entityId = 930, first = profile.protocol() >= 210 ? 6 : 5;
        BlockPos pos = new BlockPos(4, 78, 4);
        try {
            ByteBuf spawn = packet(profile, "ADD_ENTITY"); Types.VAR_INT.writePrimitive(spawn, entityId);
            spawn.writeLong(0).writeLong(entityId).writeByte(51).writeDouble(4.5).writeDouble(79).writeDouble(4.5).writeByte(0).writeByte(0).writeInt(0).writeShort(0).writeShort(0).writeShort(0);
            send(client, server, handler, spawn);
            require(world.getEntityByID(entityId) instanceof EntityEnderCrystal, "Native crystal spawn");
            EntityEnderCrystal crystal = (EntityEnderCrystal)world.getEntityByID(entityId);
            ModelBase ordinary = new ModelEnderCrystal(0, true);
            require(ServerCrystalVisuals.model(ordinary, crystal) == ordinary, "Crystal defaults to server's visible base");
            for (boolean base : new boolean[]{false, true, false}) {
                ByteBuf metadata = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(metadata, entityId);
                metadata.writeByte(first + 1).writeByte(6).writeBoolean(base).writeByte(255); send(client, server, handler, metadata);
                require((ServerCrystalVisuals.model(ordinary, crystal) == ordinary) == base, "Crystal base follows original boolean metadata: " + profile);
            }
            for (boolean present : new boolean[]{true, false}) {
                ByteBuf metadata = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(metadata, entityId);
                metadata.writeByte(first).writeByte(9).writeBoolean(present);
                if (present) Types.BLOCK_POSITION1_8.write(metadata, new com.viaversion.viaversion.api.minecraft.BlockPosition(8, 80, 8));
                metadata.writeByte(255); send(client, server, handler, metadata);
                require(ServerCrystalVisuals.hasBeam(crystal) == present && !ServerEntityViews.get(entityId).crystalBase, "Crystal beam set/clear retains separate base flag");
            }
            block(profile, client, server, handler, pos, 209 << 4);
            require(world.getTileEntity(pos) instanceof GatewayBlockEntity, "Gateway creates native tile renderer");
            GatewayBlockEntity tile = (GatewayBlockEntity)world.getTileEntity(pos);
            com.viaversion.nbt.tag.CompoundTag tag = new com.viaversion.nbt.tag.CompoundTag(); tag.putLong("Age", 199);
            ByteBuf update = packet(profile, "BLOCK_ENTITY_DATA"); Types.BLOCK_POSITION1_8.write(update, wire(pos)); update.writeByte(8); Types.NAMED_COMPOUND_TAG.write(update, tag);
            send(client, server, handler, update); require(tile.age() == 199 && tile.spawning(), "Original gateway NBT survives Via");
            tile.update(); require(!tile.spawning(), "Gateway spawn effect ends after 200 ticks");
            ByteBuf event = packet(profile, "BLOCK_EVENT"); Types.BLOCK_POSITION1_8.write(event, wire(pos)); event.writeByte(1).writeByte(0); Types.VAR_INT.writePrimitive(event, 209);
            send(client, server, handler, event);
            int duration = profile.protocol() >= 315 ? 40 : 20;
            require(tile.cooldown() == duration, "Gateway cooldown release boundary");
            for (int tick = 0; tick < duration / 2; tick++) tile.update();
            require(Math.abs(tile.progress(0) - .5F) < .00001, "Gateway beam reaches native midpoint");
            for (int tick = 0; tick < duration / 2; tick++) tile.update(); require(!tile.cooling(), "Gateway cooldown expires without client teleportation");
            world.setBlockState(pos.up(), Blocks.bedrock.getDefaultState(), 0); world.setBlockState(pos.down(), Blocks.bedrock.getDefaultState(), 0);
            require(!tile.visible(EnumFacing.UP) && !tile.visible(EnumFacing.DOWN) && tile.visible(EnumFacing.EAST), "Gateway only renders exposed faces");
            List<Object[]> particles = new ArrayList<>();
            IWorldAccess access = (IWorldAccess)Proxy.newProxyInstance(IWorldAccess.class.getClassLoader(), new Class<?>[]{IWorldAccess.class}, (proxy, method, args) -> {
                if (method.getName().equals("equals")) return proxy == args[0];
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("spawnParticle")) particles.add(args); return null;
            });
            world.addWorldAccess(access);
            try { tile.getBlockType().randomDisplayTick(world, pos, world.getBlockState(pos), new Random(42)); }
            finally { world.removeWorldAccess(access); }
            require(particles.size() == 4, "Gateway emits one portal particle per exposed face");
            for (Object[] p : particles) require((Integer)p[0] == EnumParticleTypes.PORTAL.getParticleID(), "Gateway uses native violet portal particles");
            if (profile.hasChunkBlockEntities()) {
                ChunkSection[] sections = new ChunkSection[16]; sections[4] = new ChunkSectionImpl(true);
                sections[4].palette(PaletteType.BLOCKS).addId(0); sections[4].palette(PaletteType.BLOCKS).setIdAt(0, 209 << 4);
                sections[4].getLight().setBlockLight(new byte[2048]); sections[4].getLight().setSkyLight(new byte[2048]);
                com.viaversion.nbt.tag.CompoundTag chunkTag = new com.viaversion.nbt.tag.CompoundTag(); chunkTag.putString("id", "minecraft:end_gateway");
                chunkTag.putInt("x", 32); chunkTag.putInt("y", 64); chunkTag.putInt("z", 0); chunkTag.putLong("Age", 6000);
                Chunk chunk = new BaseChunk(2, 0, true, false, 16, sections, new int[256], Collections.singletonList(chunkTag));
                ByteBuf data = packet(profile, "LEVEL_CHUNK"); new ChunkType1_9_3(true).write(data, chunk);
                send(client, server, handler, data);
                GatewayBlockEntity loaded = (GatewayBlockEntity)world.getTileEntity(new BlockPos(32, 64, 0));
                require(loaded != null && loaded.age() == 6000 && !loaded.spawning(), "Chunk gateway age prevents false spawn beams");
            }
            ByteBuf remove = packet(profile, "REMOVE_ENTITIES"); Types.VAR_INT.writePrimitive(remove, 1); Types.VAR_INT.writePrimitive(remove, entityId); send(client, server, handler, remove);
            require(ServerEntityViews.get(entityId) == null, "Crystal metadata discarded on destroy");
        } finally { world.removeEntityFromWorld(entityId); world.setBlockToAir(pos); world.setBlockToAir(pos.up()); world.setBlockToAir(pos.down()); world.doPreChunk(2, 0, false); }
    }
    private static com.viaversion.viaversion.api.minecraft.BlockPosition wire(BlockPos pos) { return new com.viaversion.viaversion.api.minecraft.BlockPosition(pos.getX(),pos.getY(),pos.getZ()); }
    private static void block(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, NetHandlerPlayClient handler, BlockPos pos, int state) throws Exception {
        ByteBuf update = packet(profile, "BLOCK_UPDATE"); Types.BLOCK_POSITION1_8.write(update, wire(pos)); Types.VAR_INT.writePrimitive(update, state); send(client, server, handler, update);
    }
}
