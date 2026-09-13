package com.viaversion.viaforge.development;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

final class BedPipelineSmokeTest {
    static void verify(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, WorldClient world) throws Exception {
        List<ItemStack> vanillaEntries = new ArrayList<>();
        Items.bed.getSubItems(Items.bed, Items.bed.getCreativeTab(), vanillaEntries);
        require(vanillaEntries.size() == (profile.protocol() >= 335 ? 0 : 1), "Versioned creative bed replacement");
        if (profile.protocol() < 335) return;
        ChunkSection[] sections = new ChunkSection[16]; sections[6] = new ChunkSectionImpl(true);
        sections[6].palette(PaletteType.BLOCKS).addId(0);
        sections[6].getLight().setBlockLight(new byte[2048]); sections[6].getLight().setSkyLight(new byte[2048]);
        List<CompoundTag> tags = new ArrayList<>();
        for (int color = 0; color < 16; color++) {
            BlockPos foot = foot(color), head = foot.offset(EnumFacing.getHorizontal(color & 3));
            sections[6].palette(PaletteType.BLOCKS).setIdAt(index(foot), 26 << 4 | color & 3);
            sections[6].palette(PaletteType.BLOCKS).setIdAt(index(head), 26 << 4 | 8 | color & 3);
            tags.add(tag(head, color));
        }
        ByteBuf source = BlockPipelineSmokeTest.packet(0x20);
        new ChunkType1_9_3(true).write(source, new BaseChunk(0, 0, true, false, 64, sections, new int[256], tags));
        BlockPipelineSmokeTest.receiveCompressed(client, server, source);
        ByteBuf nativeChunk = BlockPipelineSmokeTest.take(client, 0x21);
        try {
            S21PacketChunkData packet = new S21PacketChunkData(); packet.readPacketData(new PacketBuffer(nativeChunk));
            world.getChunkFromChunkCoords(0, 0).fillChunk(packet.getExtractedDataBytes(), packet.getExtractedSize(), true);
        } finally { nativeChunk.release(); }
        for (int color = 0; color < 16; color++) {
            EnumFacing direction = EnumFacing.getHorizontal(color & 3);
            for (boolean head : new boolean[]{false, true}) {
                BlockPos pos = head ? foot(color).offset(direction) : foot(color);
                IBlockState state = world.getBlockState(pos);
                require(ClientBlocks.definition(state.getBlock()).color == color, "Native colored bed half " + color);
                require(state.getValue(BlockBed.FACING) == direction && (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) == head, "Bed orientation/part");
                require(state.getBlock().isBed(world, pos, null) && state.getBlock().getBedDirection(world, pos) == direction, "Forge bed recognition");
                require(Math.abs(state.getBlock().getCollisionBoundingBox(world, pos, state).maxY - pos.getY() - .5625) < .00001, "Bed collision height");
                ItemStack picked = state.getBlock().getPickBlock(null, world, pos, null);
                require(picked.getItem() == Item.getItemById(ClientBlocks.localItem(355, color)) && picked.getMaxStackSize() == 1, "Colored bed pick and stack limit");
            }
        }
        BlockPos head = foot(0).south();
        ByteBuf occupied = BlockPipelineSmokeTest.packet(0x0b);
        Types.BLOCK_POSITION1_8.write(occupied, new BlockPosition(head.getX(), head.getY(), head.getZ()));
        Types.VAR_INT.writePrimitive(occupied, 26 << 4 | 12);
        BlockPipelineSmokeTest.receiveCompressed(client, server, occupied); applyUpdate(client, world);
        require(world.getBlockState(head).getValue(BlockBed.OCCUPIED), "Occupied colored bed");
        ByteBuf changed = BlockPipelineSmokeTest.packet(0x09);
        Types.BLOCK_POSITION1_8.write(changed, new BlockPosition(head.getX(), head.getY(), head.getZ())); changed.writeByte(11);
        Types.NAMED_COMPOUND_TAG.write(changed, tag(head, 11));
        BlockPipelineSmokeTest.receiveCompressed(client, server, changed);
        applyUpdate(client, world); applyUpdate(client, world);
        require(ClientBlocks.definition(world.getBlockState(head).getBlock()).color == 11
                && ClientBlocks.definition(world.getBlockState(foot(0)).getBlock()).color == 11, "Bed tile update reaches both native halves");
        require(world.getBlockState(head).getValue(BlockBed.OCCUPIED), "Color update retains occupied state");
    }
    private static void applyUpdate(EmbeddedChannel client, WorldClient world) throws Exception {
        ByteBuf data = BlockPipelineSmokeTest.take(client, 0x23);
        try {
            S23PacketBlockChange packet = new S23PacketBlockChange(); packet.readPacketData(new PacketBuffer(data));
            world.setBlockState(packet.getBlockPosition(), packet.getBlockState(), 3);
        } finally { data.release(); }
    }
    private static int index(BlockPos pos) { return (pos.getZ() & 15) * 16 + (pos.getX() & 15); }
    private static BlockPos foot(int color) { return new BlockPos(color % 4 * 4 + 1, 96, color / 4 * 4 + 1); }
    private static CompoundTag tag(BlockPos pos, int color) {
        CompoundTag tag = new CompoundTag(); tag.putString("id", "minecraft:bed"); tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY()); tag.putInt("z", pos.getZ()); tag.putInt("color", color); return tag;
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
