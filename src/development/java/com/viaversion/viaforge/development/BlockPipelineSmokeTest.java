package com.viaversion.viaforge.development;

import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.common.blocks.BlockPreservingDecodeHandler;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.chunks.BaseChunk;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSectionImpl;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_1;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.platform.ViaDecodeHandler;
import com.viaversion.viaversion.platform.ViaEncodeHandler;
import com.viaversion.viaversion.platform.ViaChannelInitializer;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.NettyCompressionDecoder;
import net.minecraft.network.NettyCompressionEncoder;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;

/** Real Via/ViaBackwards/ViaRewind transformations followed by native Minecraft decoding. */
final class BlockPipelineSmokeTest {
    private static final int COMPRESSION_THRESHOLD = 256;

    static void verify(BlockVersionProfile profile, WorldClient world) throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        EmbeddedChannel serverCompression = new EmbeddedChannel(new NettyCompressionEncoder(COMPRESSION_THRESHOLD));
        UserConnection user = new UserConnectionImpl(channel, true);
        user.getProtocolInfo().setProtocolVersion(ProtocolVersion.v1_8);
        user.getProtocolInfo().setServerProtocolVersion(ProtocolVersion.getProtocol(profile.protocol()));
        user.getProtocolInfo().setUsername("BlockSmokeTest");
        user.getProtocolInfo().setUuid(new UUID(0, 1));
        ProtocolPipelineImpl pipeline = new ProtocolPipelineImpl(user);
        pipeline.add(com.viaversion.viaforge.platform.ViaForgeProtocol.INSTANCE);
        List<ProtocolPathEntry> path = Via.getManager().getProtocolManager().getProtocolPath(ProtocolVersion.v1_8,
                ProtocolVersion.getProtocol(profile.protocol()));
        if (path == null) throw new AssertionError("No Via path for " + profile);
        for (ProtocolPathEntry entry : path) {
            Via.getManager().getProtocolManager().completeMappingDataLoading(entry.protocol().getClass());
            pipeline.add(entry.protocol());
        }
        user.getProtocolInfo().setState(State.PLAY);
        // Both must precede EmbeddedChannel's inbound message collector.
        channel.pipeline().addFirst("encoder", new ChannelOutboundHandlerAdapter());
        channel.pipeline().addFirst("decoder", new ChannelInboundHandlerAdapter());
        com.viaversion.viaforge.common.compatibility.CompatibilityProfile target=com.viaversion.viaforge.common.compatibility.CompatibilityRegistry.DEFAULT.resolve(profile.protocol());
        user.put(target);
        channel.pipeline().addBefore("decoder", ViaDecodeHandler.NAME,
                new com.viaversion.viaforge.common.compatibility.CompatibilityDecodeHandler(user,target.adapter().create(target,ClientBlocks::localState), () -> {}, () -> {}));
        channel.pipeline().addBefore("encoder", ViaEncodeHandler.NAME, new ViaEncodeHandler(user));
        try {
            enableCompression(channel);
            ByteBuf join = packet(0x23);
            join.writeInt(1).writeByte(1);
            if (profile.hasIntJoinDimension()) join.writeInt(0); else join.writeByte(0);
            join.writeByte(0).writeByte(20);
            Types.STRING.write(join, "default");
            join.writeBoolean(false);
            receiveCompressed(channel, serverCompression, join);
            take(channel, 0x01).release();
            discard(channel);

            ChunkSection[] sections = new ChunkSection[16];
            sections[4] = new ChunkSectionImpl(true);
            sections[4].palette(PaletteType.BLOCKS).addId(0);
            sections[4].getLight().setBlockLight(new byte[2048]);
            sections[4].getLight().setSkyLight(new byte[2048]);
            List<Integer> originals = new ArrayList<>();
            for (int raw = 0; raw < 4096; raw++) {
                if (profile.supportsState(raw)) {
                    sections[4].palette(PaletteType.BLOCKS).setIdAt(originals.size(), raw);
                    originals.add(raw);
                }
            }
            Chunk chunk = new BaseChunk(0, 0, true, false, 16, sections, new int[256], new ArrayList<>());
            ByteBuf source = packet(0x20);
            if (profile.hasChunkBlockEntities()) new ChunkType1_9_3(true).write(source, chunk);
            else new ChunkType1_9_1(true).write(source, chunk);
            receiveCompressed(channel, serverCompression, source);
            ByteBuf translated = take(channel, 0x21);
            S21PacketChunkData nativeChunk = new S21PacketChunkData();
            try { nativeChunk.readPacketData(new PacketBuffer(translated)); }
            finally { translated.release(); }
            world.getChunkFromChunkCoords(0, 0).fillChunk(nativeChunk.getExtractedDataBytes(), nativeChunk.getExtractedSize(), true);
            for (int i = 0; i < originals.size(); i++) {
                BlockPos pos = new BlockPos(i & 15, 64 + (i >> 8), (i >> 4) & 15);
                if (Block.BLOCK_STATE_IDS.get(world.getBlockState(pos)) != ClientBlocks.localState(originals.get(i))) {
                    throw new AssertionError("Via -> Minecraft chunk state mismatch for " + profile + ": " + originals.get(i));
                }
            }

            if (profile.protocol() >= 335) {
                // Real servers send recipe/advancement traffic between chunks and
                // placement confirmations. Via intentionally cancels this packet.
                ByteBuf recipe = packet(profile.protocol() == 335
                        ? com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ClientboundPackets1_12.RECIPE.getId()
                        : com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1.RECIPE.getId());
                Types.VAR_INT.writePrimitive(recipe, 0);
                recipe.writeBoolean(false).writeBoolean(false);
                Types.VAR_INT_ARRAY_PRIMITIVE.write(recipe, new int[0]);
                Types.VAR_INT_ARRAY_PRIMITIVE.write(recipe, new int[0]);
                receiveCompressed(channel, serverCompression, recipe);
                if (channel.readInbound() != null) throw new AssertionError("Recipe packet should be cancelled by Via");
            }
            List<Integer> placementStates = new ArrayList<>();
            for (int raw : originals) {
                int block = raw >> 4;
                if (block >= 201 && block <= 205 || block >= 219 && block <= 234) placementStates.add(raw);
            }
            placementStates.add(0);
            for (int raw : placementStates) {
                // Reordering must retain the decoder's captured chunk states.
                enableCompression(channel);
                ByteBuf update = packet(0x0b);
                Types.BLOCK_POSITION1_8.write(update, new BlockPosition(0, 64, 0));
                Types.VAR_INT.writePrimitive(update, raw);
                receiveCompressed(channel, serverCompression, update);
                ByteBuf restored = take(channel, 0x23);
                S23PacketBlockChange nativeUpdate = new S23PacketBlockChange();
                try { nativeUpdate.readPacketData(new PacketBuffer(restored)); }
                finally { restored.release(); }
                int actual = Block.BLOCK_STATE_IDS.get(nativeUpdate.getBlockState());
                int expected = raw == 0 ? 0 : ClientBlocks.localState(raw);
                if (actual != expected) throw new AssertionError("Placement confirmation changed block after cancelled traffic for " + profile + ": server=" + raw + ", expected local=" + expected + ", actual=" + actual);
            }
            // Servers also confirm batches (including neighbor changes) using
            // multi-block updates. Exercise every supported color/orientation.
            com.viaversion.viaversion.api.minecraft.BlockChangeRecord[] records = new com.viaversion.viaversion.api.minecraft.BlockChangeRecord[placementStates.size()];
            for (int i=0;i<records.length;i++)records[i]=new com.viaversion.viaversion.api.minecraft.BlockChangeRecord1_8(i&15,160+(i>>8),(i>>4)&15,placementStates.get(i));
            ByteBuf multi=packet(0x10);multi.writeInt(0).writeInt(0);Types.BLOCK_CHANGE_ARRAY.write(multi,records);
            receiveCompressed(channel,serverCompression,multi);
            ByteBuf changes=take(channel,0x22);
            try {
                if(changes.readInt()!=0||changes.readInt()!=0)throw new AssertionError("Placement batch chunk coordinates");
                com.viaversion.viaversion.api.minecraft.BlockChangeRecord[] received=Types.BLOCK_CHANGE_ARRAY.read(changes);
                if(received.length!=records.length)throw new AssertionError("Placement batch length");
                for(int i=0;i<records.length;i++) {
                    int raw=placementStates.get(i),expected=raw==0?0:ClientBlocks.localState(raw);
                    if(received[i].getBlockId()!=expected)throw new AssertionError("Placement batch changed state "+raw+" for "+profile);
                }
            }finally{changes.release();}
            BlockItemPipelineSmokeTest.verify(profile, channel, serverCompression, world);
            ServerItemSmokeTest.pipeline(profile, channel, serverCompression);
            ServerEntitySmokeTest.pipeline(profile, channel, serverCompression, world);
            BlockEditorSmokeTest.verify(profile, channel, serverCompression, world);
            BedPipelineSmokeTest.verify(profile, channel, serverCompression, world);
            if (profile.protocol() >= 335) {
                ByteBuf falling = packet(0x00);
                Types.VAR_INT.writePrimitive(falling, 42); falling.writeLong(0).writeLong(42).writeByte(70);
                falling.writeDouble(8).writeDouble(80).writeDouble(8).writeByte(0).writeByte(0).writeInt(252 | 14 << 12);
                falling.writeShort(0).writeShort(0).writeShort(0);
                receiveCompressed(channel, serverCompression, falling);
                ByteBuf entity = take(channel, 0x0e);
                try {
                    Types.VAR_INT.readPrimitive(entity); entity.skipBytes(15);
                    int data = entity.readInt();
                    int expected = ClientBlocks.localState(252 << 4 | 14);
                    if ((data & 4095) != expected >> 4 || (data >> 12 & 15) != (expected & 15)) {
                        throw new AssertionError("Falling concrete powder block state");
                    }
                } finally { entity.release(); }
            }
            if (profile.protocol() >= 315) {
                ByteBuf event = packet(0x0a);
                Types.BLOCK_POSITION1_8.write(event, new BlockPosition(0, 64, 0));
                event.writeByte(1).writeByte(1); Types.VAR_INT.writePrimitive(event, 219);
                receiveCompressed(channel, serverCompression, event);
                ByteBuf restoredEvent = take(channel, 0x24);
                try {
                    Types.BLOCK_POSITION1_8.read(restoredEvent); restoredEvent.skipBytes(2);
                    int blockId = Types.VAR_INT.readPrimitive(restoredEvent);
                    if (Block.getBlockById(blockId) != Block.BLOCK_STATE_IDS.getByValue(ClientBlocks.localState(219 << 4)).getBlock()) {
                        throw new AssertionError("Shulker block event registry id");
                    }
                } finally { restoredEvent.release(); }
            }
        } finally {
            discard(channel);
            channel.close();
            channel.runPendingTasks();
            channel.checkException();
            discard(channel);
            serverCompression.close();
            serverCompression.runPendingTasks();
            discard(serverCompression);
        }
    }


    private static void enableCompression(EmbeddedChannel channel) {
        if (channel.pipeline().get("decompress") != null) channel.pipeline().remove("decompress");
        if (channel.pipeline().get("compress") != null) channel.pipeline().remove("compress");
        Object decoder = channel.pipeline().get(ViaDecodeHandler.NAME);
        // Match NetworkManager.setCompressionTreshold and its ViaForge mixin.
        channel.pipeline().addBefore("decoder", "decompress", new NettyCompressionDecoder(COMPRESSION_THRESHOLD));
        channel.pipeline().addBefore("encoder", "compress", new NettyCompressionEncoder(COMPRESSION_THRESHOLD));
        ViaChannelInitializer.reorderPipeline(channel.pipeline(), "compress", "decompress");
        channel.checkException();
        if (channel.pipeline().get(ViaDecodeHandler.NAME) != decoder
                || channel.pipeline().names().indexOf("decompress") >= channel.pipeline().names().indexOf(ViaDecodeHandler.NAME)) {
            throw new AssertionError("Block decoder must be reinserted after decompression");
        }
    }

    static void receiveCompressed(EmbeddedChannel channel, EmbeddedChannel serverCompression, ByteBuf source) {
        if (!serverCompression.writeOutbound(source)) throw new AssertionError("Missing compressed server packet");
        ByteBuf compressed = (ByteBuf) serverCompression.readOutbound();
        channel.writeInbound(compressed);
    }

    static ByteBuf packet(int id) {
        ByteBuf buffer = Unpooled.buffer();
        Types.VAR_INT.writePrimitive(buffer, id);
        return buffer;
    }

    static ByteBuf take(EmbeddedChannel channel, int wanted) {
        ByteBuf buffer;
        while ((buffer = channel.readInbound()) != null) {
            if (Types.VAR_INT.readPrimitive(buffer) == wanted) return buffer;
            buffer.release();
        }
        throw new AssertionError("Missing translated packet " + wanted);
    }

    private static void discard(EmbeddedChannel channel) {
        ByteBuf buffer;
        while ((buffer = channel.readInbound()) != null) buffer.release();
        while ((buffer = channel.readOutbound()) != null) buffer.release();
    }
}
