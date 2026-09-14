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

/** Retains 1.14 identities before Via approximates newly introduced blocks.
 * Light, heightmaps, entity tracking and the live chunk are translated by Via once. */
final class VillageBlockData {
    private final FlattenedBlockData legacy;
    private final Mappings states = Protocol1_13_2To1_14.MAPPINGS.getBlockStateMappings().inverse();
    private final Mappings patchStates = Protocol1_13To1_13_1.MAPPINGS.getBlockStateMappings().inverse();
    private final Mappings blocks = Protocol1_13_2To1_14.MAPPINGS.getBlockMappings().inverse();
    VillageBlockData(FlattenedBlockData legacy) { this.legacy = legacy; }

    private int state(int modern) {
        int patch = states.getNewId(modern);
        return patch < 0 ? 65535 : legacy.state(patchStates.getNewId(patch));
    }

    ByteBuf normalize(ByteBuf source) throws Exception {
        ByteBuf input = source.duplicate();
        int id = Types.VAR_INT.readPrimitive(input);
        if (id < 0 || id >= ClientboundPackets1_14.values().length) return null;
        String name = ClientboundPackets1_14.values()[id].name();
        switch (name) {
            case "LEVEL_CHUNK": case "BLOCK_UPDATE": case "CHUNK_BLOCKS_UPDATE":
            case "BLOCK_EVENT": case "ADD_ENTITY": break;
            default: return null;
        }
        ByteBuf output = source.alloc().buffer();
        try {
            Types.VAR_INT.writePrimitive(output, ClientboundPackets1_12_1.valueOf(name).getId());
            switch (name) {
                case "LEVEL_CHUNK": {
                    Chunk chunk = ChunkType1_14.TYPE.read(input);
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
                    new ChunkType1_9_3(legacy.overworld()).write(output, chunk);
                    break;
                }
                case "BLOCK_UPDATE":
                    Types.BLOCK_POSITION1_8.write(output, Types.BLOCK_POSITION1_14.read(input));
                    Types.VAR_INT.writePrimitive(output, state(Types.VAR_INT.readPrimitive(input)));
                    break;
                case "CHUNK_BLOCKS_UPDATE": {
                    output.writeInt(input.readInt()).writeInt(input.readInt());
                    BlockChangeRecord[] records = Types.BLOCK_CHANGE_ARRAY.read(input);
                    for (BlockChangeRecord record : records) record.setBlockId(state(record.getBlockId()));
                    Types.BLOCK_CHANGE_ARRAY.write(output, records);
                    break;
                }
                case "BLOCK_EVENT":
                    Types.BLOCK_POSITION1_8.write(output, Types.BLOCK_POSITION1_14.read(input));
                    output.writeByte(input.readByte()).writeByte(input.readByte());
                    Types.VAR_INT.writePrimitive(output, legacy.eventBlock(blocks.getNewId(Types.VAR_INT.readPrimitive(input))));
                    break;
                case "ADD_ENTITY": {
                    Types.VAR_INT.writePrimitive(output, Types.VAR_INT.readPrimitive(input));
                    output.writeBytes(input, 16);
                    if (Types.VAR_INT.readPrimitive(input) != EntityTypes1_14.FALLING_BLOCK.getId()) {
                        output.release(); return null;
                    }
                    output.writeByte(70).writeBytes(input, 26);
                    int state = state(input.readInt());
                    output.writeInt((state >> 4) | ((state & 15) << 12));
                    break;
                }
            }
            output.writeBytes(input); return output;
        } catch (Throwable error) { output.release(); throw error; }
    }
}
