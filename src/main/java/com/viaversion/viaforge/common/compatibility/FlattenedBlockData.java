package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.*;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.data.BlockStates1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import io.netty.buffer.ByteBuf;
import java.util.*;

/** Stateless conversion of the 1.13 boundary's original block identities. Via still
 * processes the real packet once for its connection/entity/block-entity trackers. */
public final class FlattenedBlockData {
    private final Map<Integer,String> names = new HashMap<>();
    private final Map<Integer,Integer> eventBlocks = new HashMap<>();
    private int dimension;
    final WaterColors waterColors;
    final List<ByteBuf> fluidUpdates=new ArrayList<>();
    private final Map<Integer,Integer> fluids=new HashMap<>();
    int fluid(int state){return fluids.getOrDefault(state,0);}
    boolean overworld() { return dimension == 0; }
    int eventBlock(int id) { return eventBlocks.getOrDefault(id,4095); }
    public FlattenedBlockData() {
        this(new WaterColors());
    }
    public FlattenedBlockData(WaterColors waterColors) {
        this.waterColors = waterColors;
        final String[] last = {""}; final int[] block = {-1};
        BlockStates1_13.forEach(MappingDataLoader.INSTANCE.loadNBT("blockstates-1.13.nbt"), (key,id) -> {
            names.put(id,key);fluids.put(id,SwimmingFluids.descriptor(key));
            String name = key.split("\\[",2)[0];
            if (!name.equals(last[0])) {
                last[0]=name; block[0]++;
                int legacy=state(id);
                eventBlocks.put(block[0],LegacyBlockCatalog.isBedState(legacy)?26:legacy >> 4);
            }
        });
    }
    public int state(int id) {
        String key=names.get(id);
        if (key==null) return 65535; // unknown internal state: leave Via's fallback untouched
        String name=key.split("\\[",2)[0].replace("minecraft:","");
        if (name.endsWith("_bed")) {
            for(int color=0;color<16;color++) if(name.equals(modernName(LegacyBlockCatalog.COLORS[color])+"_bed")) {
                int facing=key.contains("facing=west")?1:key.contains("facing=north")?2:key.contains("facing=east")?3:0;
                return LegacyBlockCatalog.bedState(facing | (key.contains("part=head")?8:0) | (key.contains("occupied=true")&&key.contains("part=head")?4:0),color);
            }
        }
        int legacy=Protocol1_13To1_12_2.MAPPINGS.getNewBlockStateId(id);
        LegacyBlockCatalog.Definition definition=LegacyBlockCatalog.state(legacy);
        // A newly introduced aquatic block must not masquerade as a catalog entry
        // merely because Via chose that entry as its approximation.
        if(definition!=null && !name.equals(modernName(definition.name))) return 65535;
        return legacy<0?65535:legacy;
    }
    public static String modernName(String name) {
        if(name.equals("end_bricks"))return "end_stone_bricks";
        if(name.equals("magma"))return "magma_block";
        if(name.equals("red_nether_brick"))return "red_nether_bricks";
        if(name.equals("purpur_double_slab"))return "purpur_slab";
        return name.replace("silver","light_gray");
    }
    public ByteBuf normalize(ByteBuf source) throws Exception {
        ByteBuf input=source.duplicate(); int id=Types.VAR_INT.readPrimitive(input);
        if(id<0||id>=ClientboundPackets1_13.values().length)return null;
        String name=ClientboundPackets1_13.values()[id].name();
        switch(name) {
            case "LOGIN": case "RESPAWN": case "LEVEL_CHUNK": case "BLOCK_UPDATE":
            case "CHUNK_BLOCKS_UPDATE": case "BLOCK_EVENT": case "BLOCK_ENTITY_DATA":
            case "FORGET_LEVEL_CHUNK": case "EXPLODE": case "ADD_ENTITY": break;
            default:return null;
        }
        ByteBuf output=source.alloc().buffer();
        try {
            Types.VAR_INT.writePrimitive(output,ClientboundPackets1_12_1.valueOf(name).getId());
            switch(name) {
                case "LOGIN":dimension=input.getInt(input.readerIndex()+5);break;
                case "RESPAWN":dimension=input.getInt(input.readerIndex());break;
                case "LEVEL_CHUNK": {
                    Chunk chunk=new ChunkType1_13(dimension==0).read(input);
                    waterColors.capture(chunk,0);
                    fluidUpdates.add(SwimmingFluids.chunk(chunk,this::fluid));
                    for(ChunkSection section:chunk.getSections()) if(section!=null) {
                        DataPalette palette=section.palette(PaletteType.BLOCKS);
                        for(int i=0;i<palette.size();i++)palette.setIdByIndex(i,state(palette.idByIndex(i)));
                    }
                    // Color belongs to the flattened state. Stale/absent tile color
                    // must not overwrite it in the shared legacy store.
                    for(com.viaversion.nbt.tag.CompoundTag tag:chunk.getBlockEntities())
                        if("minecraft:bed".equals(tag.getString("id")))tag.remove("color");
                    new ChunkType1_9_3(dimension==0).write(output,chunk);break;
                }
                case "BLOCK_UPDATE": {
                    com.viaversion.viaversion.api.minecraft.BlockPosition pos=Types.BLOCK_POSITION1_8.read(input);int block=Types.VAR_INT.readPrimitive(input);
                    Types.BLOCK_POSITION1_8.write(output,pos);Types.VAR_INT.writePrimitive(output,state(block));
                    fluidUpdates.add(SwimmingFluids.block(pos,fluid(block)));break;
                }
                case "CHUNK_BLOCKS_UPDATE": {
                    int x=input.readInt(),z=input.readInt();output.writeInt(x).writeInt(z);
                    BlockChangeRecord[] records=Types.BLOCK_CHANGE_ARRAY.read(input);
                    for(BlockChangeRecord record:records){fluidUpdates.add(SwimmingFluids.block(new com.viaversion.viaversion.api.minecraft.BlockPosition((x<<4)+record.getSectionX(),record.getY(),(z<<4)+record.getSectionZ()),fluid(record.getBlockId())));record.setBlockId(state(record.getBlockId()));}
                    Types.BLOCK_CHANGE_ARRAY.write(output,records);break;
                }
                case "BLOCK_EVENT":
                    Types.BLOCK_POSITION1_8.write(output,Types.BLOCK_POSITION1_8.read(input));
                    output.writeByte(input.readByte()).writeByte(input.readByte());
                    Types.VAR_INT.writePrimitive(output,eventBlocks.getOrDefault(Types.VAR_INT.readPrimitive(input),4095));break;
                case "ADD_ENTITY":
                    Types.VAR_INT.writePrimitive(output,Types.VAR_INT.readPrimitive(input));
                    output.writeBytes(input,16); int type=input.readUnsignedByte();output.writeByte(type);
                    output.writeBytes(input,26);int data=input.readInt();
                    if(type==70){int legacy=state(data);data=(legacy>>4)|((legacy&15)<<12);}
                    output.writeInt(data);break;
                case "FORGET_LEVEL_CHUNK":fluidUpdates.add(SwimmingFluids.unload(input.getInt(input.readerIndex()),input.getInt(input.readerIndex()+4)));break;
                default:break;
            }
            output.writeBytes(input);return output;
        } catch(Throwable error){output.release();throw error;}
    }
}
