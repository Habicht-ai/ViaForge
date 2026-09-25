package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_14;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.*;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.Protocol1_13_2To1_14;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_13to1_13_1.Protocol1_13To1_13_1;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import io.netty.buffer.ByteBuf;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.type.Type;
import java.util.function.IntUnaryOperator;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.IntFunction;

/** Retains 1.14 identities before Via approximates newly introduced blocks.
 * Light, heightmaps, entity tracking and the live chunk are translated by Via once. */
final class VillageBlockData {
    private final FlattenedBlockData legacy;
    private final Mappings states = Protocol1_13_2To1_14.MAPPINGS.getBlockStateMappings().inverse();
    private final Mappings patchStates = Protocol1_13To1_13_1.MAPPINGS.getBlockStateMappings().inverse();
    private final Mappings blocks = Protocol1_13_2To1_14.MAPPINGS.getBlockMappings().inverse();
    private final ClientboundPacketType[] packets;
    private final Supplier<Type<Chunk>> chunkType;
    private final IntSupplier minY;
    private final IntUnaryOperator sourceStates, sourceBlocks;
    private final int fallingBlock;
    private IntFunction<String> blockEntityNames;
    private boolean sectionLightFlag=true,lowPrecisionMovement;
    private IntUnaryOperator originalFluids=SwimmingFluidRegistry.forVersion("1.14");
    VillageBlockData(FlattenedBlockData legacy) {
        this(legacy, ClientboundPackets1_14.values(), ChunkType1_14.TYPE,
                id -> id, id -> id, EntityTypes1_14.FALLING_BLOCK.getId());
    }
    VillageBlockData(FlattenedBlockData legacy, ClientboundPacketType[] packets, Type<Chunk> chunkType,
            IntUnaryOperator sourceStates, IntUnaryOperator sourceBlocks, int fallingBlock) {
        this(legacy, packets, () -> chunkType, () -> 0, sourceStates, sourceBlocks, fallingBlock);
    }
    VillageBlockData(FlattenedBlockData legacy, ClientboundPacketType[] packets, Supplier<Type<Chunk>> chunkType,
            IntSupplier minY, IntUnaryOperator sourceStates, IntUnaryOperator sourceBlocks, int fallingBlock) {
        this.legacy = legacy; this.packets = packets; this.chunkType = chunkType; this.minY = minY;
        this.sourceStates = sourceStates; this.sourceBlocks = sourceBlocks; this.fallingBlock = fallingBlock;
    }
    VillageBlockData sectionedEntities(IntFunction<String> names) { this.blockEntityNames = names; return this; }
    VillageBlockData fluidRegistry(IntUnaryOperator fluids) { this.originalFluids=fluids;return this; }

    VillageBlockData lowPrecisionMovement(){lowPrecisionMovement=true;return this;}
    VillageBlockData withoutSectionLightFlag(){sectionLightFlag=false;return this;}

    private int state(int modern) {
        int village = sourceStates.applyAsInt(modern);
        int patch = village < 0 ? -1 : states.getNewId(village);
        return patch < 0 ? 65535 : legacy.state(patchStates.getNewId(patch));
    }
    private int fluid(int modern) {
        return originalFluids.applyAsInt(modern);
    }

    boolean captures(int id) {
        if (id < 0 || id >= packets.length) return false;
        switch (packets[id].getName()) {
            case "LEVEL_CHUNK": case "BLOCK_UPDATE": case "CHUNK_BLOCKS_UPDATE":
            case "BLOCK_EVENT": case "ADD_ENTITY": case "SECTION_BLOCKS_UPDATE": case "LEVEL_CHUNK_WITH_LIGHT": return true;
            default: return false;
        }
    }
    ByteBuf normalize(ByteBuf source) throws Exception {
        ByteBuf input = source.duplicate();
        int id = Types.VAR_INT.readPrimitive(input);
        if (!captures(id)) return null;
        String name = packets[id].getName();
        ByteBuf output = source.alloc().buffer();
        try {
            String oldName=name.equals("SECTION_BLOCKS_UPDATE")?"CHUNK_BLOCKS_UPDATE":name.equals("LEVEL_CHUNK_WITH_LIGHT")?"LEVEL_CHUNK":name;
            Types.VAR_INT.writePrimitive(output, ClientboundPackets1_12_1.valueOf(oldName).getId());
            switch (name) {
                case "LEVEL_CHUNK": case "LEVEL_CHUNK_WITH_LIGHT": {
                    Chunk chunk = chunkType.get().read(input);
                    legacy.waterColors.capture(chunk,minY.getAsInt());
                    if (chunk instanceof Chunk1_18 || chunk instanceof Chunk1_21_5) {
                        java.util.BitSet mask=new java.util.BitSet();
                        for(int i=0;i<chunk.getSections().length;i++)if(chunk.getSections()[i].getNonAirBlocksCount()!=0)mask.set(i);
                        java.util.List<com.viaversion.nbt.tag.CompoundTag> tags=new java.util.ArrayList<>();
                        for(com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity entity:chunk.blockEntities()) {
                            String identifier=blockEntityNames.apply(entity.typeId());if(identifier==null)continue;
                            com.viaversion.nbt.tag.CompoundTag tag=entity.tag()==null?new com.viaversion.nbt.tag.CompoundTag():entity.tag().copy();
                            tag.putString("id",identifier.indexOf(':')<0?"minecraft:"+identifier:identifier);
                            tag.putInt("x",(chunk.getX()<<4)+entity.sectionX());tag.putInt("y",entity.y());tag.putInt("z",(chunk.getZ()<<4)+entity.sectionZ());tags.add(tag);
                        }
                        chunk=new BaseChunk(chunk.getX(),chunk.getZ(),true,false,mask,chunk.getSections(),new int[256],new com.viaversion.nbt.tag.CompoundTag(),tags);
                    }
                    if (chunk.getChunkMask() != null) {
                        // Match Via's native-height window using the original dimension.
                        // This does not extend the 1.8 renderer beyond Y=0..255.
                        int first = -(minY.getAsInt() >> 4), mask = 0;
                        ChunkSection[] visible = new ChunkSection[16];
                        for (int y = 0; y < 16; y++) {
                            int index = first + y;
                            if (index >= 0 && index < chunk.getSections().length && chunk.getChunkMask().get(index)) {
                                visible[y] = chunk.getSections()[index]; mask |= 1 << y;
                            }
                        }
                        chunk.setSections(visible); chunk.setBitmask(mask); chunk.setChunkMask(null);
                        chunk.getBlockEntities().removeIf(tag -> tag.getInt("y") < 0 || tag.getInt("y") > 255);
                    }
                    legacy.fluidUpdates.add(SwimmingFluids.chunk(chunk,this::fluid));
                    for (ChunkSection section : chunk.getSections()) if (section != null) {
                        DataPalette palette = section.palette(PaletteType.BLOCKS);
                        for (int i = 0; i < palette.size(); i++) palette.setIdByIndex(i, state(palette.idByIndex(i)));
                        // This detached copy feeds only the legacy state store. The
                        // actual light arrays remain owned by Via's ChunkLightStorage.
                        section.setLight(ChunkSectionLightImpl.createWithBlockLight());
                        if (legacy.overworld()) section.getLight().setSkyLight(new byte[2048]);
                    }
                    for (com.viaversion.nbt.tag.CompoundTag tag : chunk.getBlockEntities())
                        if ("minecraft:bed".equals(tag.getString("id"))) tag.remove("color");
                    // Only states enter this detached legacy packet. Via owns the real
                    // biome conversion, including 1.15's 1024 three-dimensional entries.
                    if (chunk.getBiomeData() != null && chunk.getBiomeData().length != 256) chunk.setBiomeData(new int[256]);
                    new ChunkType1_9_3(legacy.overworld()).write(output, chunk);
                    if(name.equals("LEVEL_CHUNK_WITH_LIGHT"))input.skipBytes(input.readableBytes()); // Via alone owns the light tail.
                    break;
                }
                case "BLOCK_UPDATE": {
                    com.viaversion.viaversion.api.minecraft.BlockPosition pos=Types.BLOCK_POSITION1_14.read(input);int block=Types.VAR_INT.readPrimitive(input);
                    Types.BLOCK_POSITION1_8.write(output,pos);Types.VAR_INT.writePrimitive(output,state(block));
                    if(pos.y()>=0&&pos.y()<256)legacy.fluidUpdates.add(SwimmingFluids.block(pos,fluid(block)));break;
                }
                case "CHUNK_BLOCKS_UPDATE": {
                    int x=input.readInt(),z=input.readInt();output.writeInt(x).writeInt(z);
                    BlockChangeRecord[] records = Types.BLOCK_CHANGE_ARRAY.read(input);
                    for (BlockChangeRecord record : records) {legacy.fluidUpdates.add(SwimmingFluids.block(new com.viaversion.viaversion.api.minecraft.BlockPosition((x<<4)+record.getSectionX(),record.getY(),(z<<4)+record.getSectionZ()),fluid(record.getBlockId())));record.setBlockId(state(record.getBlockId()));}
                    Types.BLOCK_CHANGE_ARRAY.write(output, records);
                    break;
                }
                case "SECTION_BLOCKS_UPDATE": {
                    long position=input.readLong(); if(sectionLightFlag)input.readBoolean();
                    int x=(int)(position>>42),y=(int)(position<<44>>44),z=(int)(position<<22>>42);
                    BlockChangeRecord[] records=Types.VAR_LONG_BLOCK_CHANGE_ARRAY.read(input);
                    if(y<0||y>=16){output.release();return null;}
                    output.writeInt(x).writeInt(z);
                    BlockChangeRecord[] legacyRecords=new BlockChangeRecord[records.length];
                    for(int i=0;i<records.length;i++) {
                        BlockChangeRecord record=records[i];
                        legacy.fluidUpdates.add(SwimmingFluids.block(new com.viaversion.viaversion.api.minecraft.BlockPosition((x<<4)+record.getSectionX(),record.getY(y),(z<<4)+record.getSectionZ()),fluid(record.getBlockId())));
                        legacyRecords[i]=new com.viaversion.viaversion.api.minecraft.BlockChangeRecord1_8(record.getSectionX(),record.getY(y),record.getSectionZ(),state(record.getBlockId()));
                    }
                    Types.BLOCK_CHANGE_ARRAY.write(output,legacyRecords);break;
                }
                case "BLOCK_EVENT":
                    Types.BLOCK_POSITION1_8.write(output, Types.BLOCK_POSITION1_14.read(input));
                    output.writeByte(input.readByte()).writeByte(input.readByte());
                    int block = sourceBlocks.applyAsInt(Types.VAR_INT.readPrimitive(input));
                    Types.VAR_INT.writePrimitive(output, legacy.eventBlock(block < 0 ? -1 : blocks.getNewId(block)));
                    break;
                case "ADD_ENTITY": {
                    Types.VAR_INT.writePrimitive(output, Types.VAR_INT.readPrimitive(input));
                    output.writeBytes(input, 16);
                    if (Types.VAR_INT.readPrimitive(input) != fallingBlock) {
                        output.release(); return null;
                    }
                    output.writeByte(70).writeBytes(input, 24);
                    com.viaversion.viaversion.api.minecraft.Vector3d movement=lowPrecisionMovement?Types.LOW_PRECISION_VECTOR.read(input):null;
                    output.writeBytes(input,2);
                    if(blockEntityNames!=null)input.readByte(); // Unified spawn added head yaw and VarInt data in 1.19.
                    int state = state(blockEntityNames!=null?Types.VAR_INT.readPrimitive(input):input.readInt());
                    output.writeInt((state >> 4) | ((state & 15) << 12));
                    if(movement!=null){output.writeShort(com.viaversion.viabackwards.utils.VelocityUtil.toLegacyVelocity(movement.x()));output.writeShort(com.viaversion.viabackwards.utils.VelocityUtil.toLegacyVelocity(movement.y()));output.writeShort(com.viaversion.viabackwards.utils.VelocityUtil.toLegacyVelocity(movement.z()));}
                    break;
                }
            }
            output.writeBytes(input); return output;
        } catch (Throwable error) { output.release(); throw error; }
    }
}
