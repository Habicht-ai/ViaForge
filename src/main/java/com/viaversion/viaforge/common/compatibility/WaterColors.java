package com.viaversion.viaforge.common.compatibility;

import com.viaversion.nbt.tag.*;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.*;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypeMap;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Retains original biome colors before Via collapses biomes into native IDs.
 * Immutable column snapshots can be read by Minecraft's chunk render workers. */
public final class WaterColors {
    public static final int DEFAULT = 0x3F76E4;
    private final Map<Long,int[]> columns = new ConcurrentHashMap<>();
    private final Map<Integer,Integer> registry = new HashMap<>();
    private int minY, sectionCount = 16, biomeCount = 1;
    private final java.util.Queue<Long> updates = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public void observe(ByteBuf source, UserConnection user) throws Exception {
        State state = user.getProtocolInfo().getServerState();
        if (state != State.PLAY && state != State.CONFIGURATION) return;
        int version = user.getProtocolInfo().serverProtocolVersion().getVersion();
        PacketTypeMap<?> packetTypes = null;
        for (Protocol protocol : user.getProtocolInfo().getPipeline().reversedPipes()) {
            if (!(protocol instanceof com.viaversion.viabackwards.api.BackwardsProtocol)) continue;
            packetTypes = (PacketTypeMap<?>) protocol.getPacketTypesProvider().unmappedClientboundPacketTypes().get(state);
            break;
        }
        // The first configuration bridge (1.20.2 -> 1.20) registers numeric
        // packet IDs and does not expose CONFIGURATION in its type provider.
        if(state==State.CONFIGURATION)packetTypes=configurationTypes(version);
        if (packetTypes == null) return;
        ByteBuf input = source.duplicate();
        PacketType packet = packetTypes.typeById(Types.VAR_INT.readPrimitive(input));
        if (packet == null) return;
        switch (packet.getName()) {
            case "REGISTRY_DATA":
                if (version < 766) registry(Types.COMPOUND_TAG.read(input));
                else if ("minecraft:worldgen/biome".equals(Types.STRING.read(input))) {
                    RegistryEntry[] entries = Types.REGISTRY_ENTRY_ARRAY.read(input);
                    registry.clear(); biomeCount = entries.length;
                    for (int i=0;i<entries.length;i++) registry.put(i, color(entries[i].key(), entries[i].tag()));
                }
                break;
            case "LOGIN":
                clear();
                if (version >= 735 && version < 764) {
                    input.skipBytes(version >= 751 ? 7 : 6);
                    Types.STRING_ARRAY.read(input); registry(Types.NAMED_COMPOUND_TAG.read(input));
                }
                break;
            case "RESPAWN": clear(); break;
            case "FORGET_LEVEL_CHUNK":
                if (version >= 764) { com.viaversion.viaversion.api.minecraft.ChunkPosition pos = Types.CHUNK_POSITION.read(input); unload(pos.chunkX(),pos.chunkZ()); }
                else unload(input.readInt(),input.readInt());
                break;
            case "CHUNKS_BIOMES": {
                int count=Types.VAR_INT.readPrimitive(input);
                int bits=com.viaversion.viaversion.util.MathUtil.ceilLog2(biomeCount);
                com.viaversion.viaversion.api.type.Type<DataPalette[]> type=version>=770
                        ?new com.viaversion.viaversion.api.type.types.chunk.ChunkBiomesType1_21_5(sectionCount,bits)
                        :new com.viaversion.viaversion.api.type.types.chunk.ChunkBiomesType1_19_4(sectionCount,bits);
                for(int i=0;i<count;i++) {
                    com.viaversion.viaversion.api.minecraft.ChunkPosition pos=Types.CHUNK_POSITION.read(input);
                    DataPalette[] palettes=type.read(input);
                    if(columns.containsKey(key(pos.chunkX(),pos.chunkZ()))) {
                        captureSections(pos.chunkX(),pos.chunkZ(),palettes,minY);
                        updates.add(key(pos.chunkX(),pos.chunkZ()));
                    }
                }
                break;
            }
            default: break;
        }
    }

    private static PacketTypeMap<?> configurationTypes(int version) {
        if(version>=773)return PacketTypeMap.of(com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundConfigurationPackets1_21_9.class);
        if(version>=771)return PacketTypeMap.of(com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundConfigurationPackets1_21_6.class);
        if(version>=767)return PacketTypeMap.of(com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21.class);
        if(version>=766)return PacketTypeMap.of(com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundConfigurationPackets1_20_5.class);
        if(version>=765)return PacketTypeMap.of(com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundConfigurationPackets1_20_3.class);
        return PacketTypeMap.of(com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundConfigurationPackets1_20_2.class);
    }

    public void registry(CompoundTag root) {
        CompoundTag biomes = root == null ? null : root.getCompoundTag("minecraft:worldgen/biome");
        if (biomes == null) return;
        registry.clear();
        ListTag<CompoundTag> entries = biomes.getListTag("value",CompoundTag.class);
        biomeCount = entries.size();
        for (CompoundTag entry : entries) registry.put(entry.getInt("id"),color(entry.getString("name"),entry.getCompoundTag("element")));
    }
    public static int color(String name, Tag data) {
        if (data instanceof CompoundTag) {
            CompoundTag effects=((CompoundTag)data).getCompoundTag("effects");
            Tag value=effects==null?null:effects.get("water_color");
            if (value instanceof NumberTag) return ((NumberTag)value).asInt() & 0xFFFFFF;
            if (value instanceof StringTag) {
                String hex=((StringTag)value).getValue();
                if(hex.matches("#[0-9a-fA-F]{6}"))return Integer.parseInt(hex.substring(1),16);
            }
        }
        switch(name.replace("minecraft:","")) {
            case "swamp": case "swamp_hills": return 0x617B64;
            case "mangrove_swamp": return 0x3A7A6A;
            case "warm_ocean": case "deep_warm_ocean": return 0x43D5EE;
            case "lukewarm_ocean": case "deep_lukewarm_ocean": return 0x45ADF2;
            case "cold_ocean": case "deep_cold_ocean": case "snowy_beach": case "snowy_taiga": return 0x3D57D6;
            case "frozen_ocean": case "deep_frozen_ocean": case "frozen_river": return 0x3938C9;
            case "meadow": return 0x0E4ECF;
            case "cherry_grove": return 0x5DB7EF;
            case "pale_garden": return 0x76889D;
            case "sulfur_caves": return 0x34BF89;
            default: return DEFAULT;
        }
    }
    private int color(int biome) {
        if (!registry.isEmpty()) return registry.getOrDefault(biome,DEFAULT);
        return legacyBiomeColor(biome);
    }
    public static int legacyBiomeColor(int biome) {
        switch(biome) {
            case 6: case 134: return 0x617B64;
            case 44: case 47: return 0x43D5EE;
            case 45: case 48: return 0x45ADF2;
            case 26: case 30: case 31: case 158: case 46: case 49: return 0x3D57D6;
            case 10: case 11: case 50: return 0x3938C9;
            default: return DEFAULT;
        }
    }
    public void capture(Chunk chunk, int lowestY) {
        int[] biomes=chunk.getBiomeData();
        if (biomes!=null) {
            int[] colors=new int[biomes.length==256?256:1024];
            for(int i=0;i<colors.length;i++) {
                int source=i-(biomes.length==256?0:(lowestY>>2)*16);
                colors[i]=source>=0&&source<biomes.length?color(biomes[source]):DEFAULT;
            }
            columns.put(key(chunk.getX(),chunk.getZ()),colors);
        } else {
            DataPalette[] palettes=new DataPalette[chunk.getSections().length];
            for(int i=0;i<palettes.length;i++)if(chunk.getSections()[i]!=null)palettes[i]=chunk.getSections()[i].palette(PaletteType.BIOMES);
            captureSections(chunk.getX(),chunk.getZ(),palettes,lowestY);
        }
    }
    private void captureSections(int x,int z,DataPalette[] palettes,int lowestY) {
        minY=lowestY;sectionCount=palettes.length;
        int[] colors=new int[1024];Arrays.fill(colors,DEFAULT);
        boolean present=false;
        for(int y=0;y<64;y++) {
            int section=(y*4-lowestY)>>4;
            if(section<0||section>=palettes.length||palettes[section]==null)continue;
            present=true;
            for(int horizontal=0;horizontal<16;horizontal++)colors[y*16+horizontal]=color(palettes[section].idAt((y&3)*16+horizontal));
        }
        if(present)columns.put(key(x,z),colors);
    }
    public int at(int x,int y,int z,int fallback) {
        int[] colors=columns.get(key(x>>4,z>>4));if(colors==null)return fallback;
        int index=colors.length==256?(z&15)*16+(x&15):Math.max(0,Math.min(63,y>>2))*16+((z&15)>>2)*4+((x&15)>>2);
        return index<colors.length?colors[index]:fallback;
    }
    public void unload(int x,int z){columns.remove(key(x,z));}
    public Long pollUpdate(){return updates.poll();}
    public void clear(){columns.clear();updates.clear();}
    private static long key(int x,int z){return (long)x<<32|z&0xFFFFFFFFL;}
}
