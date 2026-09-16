package com.viaversion.viaforge.development;

import com.viaversion.nbt.tag.*;
import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.blocks.resources.*;
import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import com.viaversion.viaversion.api.minecraft.item.*;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.*;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_13;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.platform.*;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.data.BlockStates1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.*;
import com.viaversion.viaversion.protocols.v1_13to1_13_1.Protocol1_13To1_13_1;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.Protocol1_13_2To1_14;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.*;
import com.viaversion.viaversion.protocols.v1_14_3to1_14_4.packet.ClientboundPackets1_14_4;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_14;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_15;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.Protocol1_14_4To1_15;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.packet.ClientboundPackets1_15;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.Protocol1_15_2To1_16;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ClientboundPackets1_16;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ServerboundPackets1_16;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.Protocol1_16_1To1_16_2;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ClientboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_17to1_17_1.packet.ClientboundPackets1_17_1;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_17;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.Protocol1_17_1To1_18;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.packet.ClientboundPackets1_18;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.Protocol1_18_2To1_19;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.packet.ClientboundPackets1_19;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.packet.ServerboundPackets1_19;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.packet.ClientboundPackets1_19_1;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.packet.ServerboundPackets1_19_1;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.Protocol1_19_1To1_19_3;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ServerboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.Protocol1_19_3To1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ServerboundPackets1_19_4;
import com.viaversion.viaversion.protocols.v1_19_4to1_20.Protocol1_19_4To1_20;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.Protocol1_20To1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.Protocol1_20_2To1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.*;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.minecraft.data.*;
import com.viaversion.viaversion.api.minecraft.item.data.Enchantments;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.*;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.Protocol1_21To1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.*;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPackets1_21_4;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.*;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.Protocol1_21_5To1_21_4;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.Protocol1_21_5To1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.*;
import com.viaversion.viaversion.protocols.v1_21_6to1_21_7.Protocol1_21_6To1_21_7;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.Protocol1_21_7To1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.*;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.Protocol1_21_9To1_21_11;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPackets1_21_11;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.Protocol1_21_11To26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.*;
import com.viaversion.viaversion.protocols.v26_1to26_2.Protocol26_1To26_2;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType26_1;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.network.*;
import net.minecraft.network.play.server.*;
import net.minecraft.util.BlockPos;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Actual flattened wire formats through compression, all Via layers, mixins and native decoding. */
final class FlattenedPipelineSmokeTest {
    private final CompatibilityProfile target;
    private final EmbeddedChannel client=new EmbeddedChannel(new ChannelInboundHandlerAdapter());
    private final EmbeddedChannel server=new EmbeddedChannel(new NettyCompressionEncoder(256),new NettyCompressionDecoder(256));
    private final UserConnection user=new UserConnectionImpl(client,true);
    private final Type<Item> itemType;
    private final boolean village;
    private final boolean bee, nether, netherPatch, caves, cavesPatch, cliffs, wild, wildPatch, wild3, wild4, trails, config, trials, components, tricky, bundles, winter, spring, summer, summerPatch, copper, mounts, year26, sulfur;
    private final Map<String,Integer> enchantmentIds=new HashMap<>();
    private final Map<String,Integer> states=new HashMap<>();
    private final List<String> stateNames=new ArrayList<>();
    private int joins;
    private FlattenedPipelineSmokeTest(CompatibilityProfile target)throws Exception {
        this.target=target;village=target.serverProtocol()>=477;bee=target.serverProtocol()>=573;nether=target.serverProtocol()>=735;netherPatch=target.serverProtocol()>=751;caves=target.serverProtocol()>=755;cavesPatch=target.serverProtocol()>=756;cliffs=target.serverProtocol()>=757;wild=target.serverProtocol()>=759;wildPatch=target.serverProtocol()>=760;wild3=target.serverProtocol()>=761;wild4=target.serverProtocol()>=762;trails=target.serverProtocol()>=763;config=target.serverProtocol()>=764;trials=target.serverProtocol()>=765;components=target.serverProtocol()>=766;tricky=target.serverProtocol()>=767;bundles=target.serverProtocol()>=768;winter=target.serverProtocol()>=769;spring=target.serverProtocol()>=770;summer=target.serverProtocol()>=771;summerPatch=target.serverProtocol()>=772;copper=target.serverProtocol()>=773;mounts=target.serverProtocol()>=774;year26=target.serverProtocol()>=775;sulfur=target.serverProtocol()>=776;itemType=sulfur?VersionedTypes.V26_2.item():year26?VersionedTypes.V26_1.item():mounts?VersionedTypes.V1_21_11.item():copper?VersionedTypes.V1_21_9.item():summer?VersionedTypes.V1_21_6.item():spring?VersionedTypes.V1_21_5.item():winter?VersionedTypes.V1_21_4.item():bundles?VersionedTypes.V1_21_2.item():tricky?VersionedTypes.V1_21.item():components?VersionedTypes.V1_20_5.item():config?Types.ITEM1_20_2:target.serverProtocol()>=404?Types.ITEM1_13_2:Types.ITEM1_13;
        user.getProtocolInfo().setProtocolVersion(ProtocolVersion.v1_8);
        user.getProtocolInfo().setServerProtocolVersion(ProtocolVersion.getProtocol(target.serverProtocol()));
        user.getProtocolInfo().setUsername("FlattenedSmoke");user.getProtocolInfo().setUuid(new UUID(0,33));
        ProtocolPipelineImpl pipeline=new ProtocolPipelineImpl(user);pipeline.add(com.viaversion.viaforge.platform.ViaForgeProtocol.INSTANCE);
        List<ProtocolPathEntry> path=Via.getManager().getProtocolManager().getProtocolPath(ProtocolVersion.v1_8,ProtocolVersion.getProtocol(target.serverProtocol()));
        require(path!=null,"Actual Via path "+target.serverProtocol());
        for(ProtocolPathEntry entry:path){Via.getManager().getProtocolManager().completeMappingDataLoading(entry.protocol().getClass());pipeline.add(entry.protocol());}
        user.put(target);user.getProtocolInfo().setState(State.PLAY);
        client.pipeline().addFirst("decoder",new ChannelInboundHandlerAdapter());client.pipeline().addFirst("encoder",new ChannelOutboundHandlerAdapter());
        client.pipeline().addBefore("decoder",ViaDecodeHandler.NAME,new CompatibilityDecodeHandler(user,target.adapter().create(target,ClientBlocks::localState),()->joins++,()->{}));
        client.pipeline().addBefore("encoder",ViaEncodeHandler.NAME,new ViaEncodeHandler(user));
        compression();
        BlockStates1_13.forEach(MappingDataLoader.INSTANCE.loadNBT("blockstates-1.13.nbt"),(key,id)->{states.put(key.replace("minecraft:",""),id);stateNames.add(key);});
    }
    static String verify(CompatibilityProfile target,WorldClient world,Path report)throws Exception {
        FlattenedPipelineSmokeTest test=new FlattenedPipelineSmokeTest(target);
        try {
            test.join();int count=test.blocks(world);test.heightWindow(world);test.items();test.eggs();test.events();test.boatInput(world);test.hands();test.editors();test.resources(report);MobRenderSmokeTest.modern(target,world,report);test.lifecycle();
            return target.resources().version()+": real flattened chunks ("+count+" inherited states), single/multi updates, bed colors without tile NBT, shulker events, falling blocks; all inherited block/item inventory round trips, Damage/enchantments/banner NBT, split counts, fresh Creative picks; 43 egg species: ID-only server items, fresh Creative picks, server echoes/models and click round trips"+(test.sulfur?", typed entity components/hashes and overridden spawn species":"")+"; offhand/full/direct/equipment, boat/player/mob metadata, passengers, boat movement/paddles and all 36 directional/jump/dismount input combinations, cooldowns/particles/Totem, both hand directions; original target resource aliases, particle atlas texels and all inherited block/item models; structure mirror/rotation/flags/seed, command/gateway/structure NBT, chunk unload and dimension cleanup"+(test.village?", separate sky/block light and JSON Lore":"")+(test.bee?", original chest face/UV checks":"")+(test.caves?", 384-height source and 1.17 inventory codec; native Y=0..255 window only":"")+" PASS";
        }finally{test.close();}
    }
    private int netherBlock(int id){if(nether)id=Protocol1_15_2To1_16.MAPPINGS.getNewBlockId(id);id=netherPatch?Protocol1_16_1To1_16_2.MAPPINGS.getNewBlockId(id):id;id=caves?Protocol1_16_4To1_17.MAPPINGS.getNewBlockId(id):id;id=wild?Protocol1_18_2To1_19.MAPPINGS.getNewBlockId(id):id;return later(id,MappingData::getBlockMappings);}
    private int modernBlockBits() {
        MappingData mapping=sulfur?Protocol26_1To26_2.MAPPINGS:year26?Protocol1_21_11To26_1.MAPPINGS:mounts?Protocol1_21_9To1_21_11.MAPPINGS:copper?Protocol1_21_7To1_21_9.MAPPINGS:summerPatch?Protocol1_21_6To1_21_7.MAPPINGS:summer?Protocol1_21_5To1_21_6.MAPPINGS:Protocol1_21_4To1_21_5.MAPPINGS;
        return com.viaversion.viaversion.util.MathUtil.ceilLog2(mapping.getBlockStateMappings().mappedSize());
    }
    private void writeModernChunk(ByteBuf packet,Chunk chunk)throws Exception {
        if(caves) {
            ChunkSection[] tall=new ChunkSection[24];System.arraycopy(chunk.getSections(),0,tall,4,16);
            tall[0]=new ChunkSectionImpl(false);tall[0].palette(PaletteType.BLOCKS).addId(wireState(states.get("purpur_block")));
            tall[23]=new ChunkSectionImpl(false);tall[23].palette(PaletteType.BLOCKS).addId(wireState(states.get("red_concrete_powder")));
            BitSet mask=new BitSet();for(int i=0;i<tall.length;i++)if(tall[i]!=null)mask.set(i);
            int[] biomes=new int[24*64];System.arraycopy(chunk.getBiomeData(),0,biomes,4*64,1024);
            chunk.setSections(tall);chunk.setChunkMask(mask);chunk.setBiomeData(biomes);
            if(cliffs) {
                for(int y=0;y<24;y++) {
                    if(tall[y]==null){tall[y]=new ChunkSectionImpl(false);tall[y].palette(PaletteType.BLOCKS).addId(0);}
                    DataPalette palette=new DataPaletteImpl(64);palette.addId(0);for(int i=0;i<64;i++)palette.setIdAt(i,biomes[y*64+i]);tall[y].addPalette(PaletteType.BIOMES,palette);
                    int nonAir=0;for(int i=0;i<4096;i++)if(tall[y].palette(PaletteType.BLOCKS).idAt(i)!=0)nonAir++;tall[y].setNonAirBlocksCount(nonAir);
                }
                // All sections, local biome palettes, typed block entities and light share one packet.
                Chunk1_18 modern=new Chunk1_18(chunk.getX(),chunk.getZ(),tall,chunk.getHeightMap(),new ArrayList<>());
                if(spring)(year26?new ChunkType26_1(24,modernBlockBits(),2):new ChunkType1_21_5(24,modernBlockBits(),2)).write(packet,new Chunk1_21_5(modern.getX(),modern.getZ(),tall,new Heightmap[]{new Heightmap(1,new long[37])},modern.blockEntities()));else if(config)new ChunkType1_20_2(24,15,2).write(packet,modern);else new ChunkType1_18(24,15,2).write(packet,modern);
                ByteBuf light=lightHeader();try{Types.VAR_INT.readPrimitive(light);Types.VAR_INT.readPrimitive(light);Types.VAR_INT.readPrimitive(light);packet.writeBytes(light);}finally{light.release();}
                byte[] sky=new byte[2048],block=new byte[2048];Arrays.fill(sky,(byte)0xaa);Arrays.fill(block,(byte)0x55);
                Types.VAR_INT.writePrimitive(packet,1);Types.BYTE_ARRAY_PRIMITIVE.write(packet,sky);Types.VAR_INT.writePrimitive(packet,1);Types.BYTE_ARRAY_PRIMITIVE.write(packet,block);
            }else new ChunkType1_17(24).write(packet,chunk);
        }else if(netherPatch)com.viaversion.viaversion.api.type.types.chunk.ChunkType1_16_2.TYPE.write(packet,chunk);
        else if(nether)com.viaversion.viaversion.api.type.types.chunk.ChunkType1_16.TYPE.write(packet,chunk);
        else ChunkType1_15.TYPE.write(packet,chunk);
    }
    private CompoundTag dimension(String name){CompoundTag tag=new CompoundTag();tag.putString("effects",name);tag.putInt("logical_height",256);tag.putInt("height",caves&&name.equals("minecraft:overworld")?384:256);tag.putInt("min_y",caves&&name.equals("minecraft:overworld")?-64:0);return tag;}
    private CompoundTag registry() {
        CompoundTag registry=new CompoundTag(),biomeRegistry=new CompoundTag();ListTag<CompoundTag> entries=new ListTag<>(CompoundTag.class);
        String[] names={"ocean","plains","desert"};for(int i=0;i<names.length;i++){CompoundTag entry=new CompoundTag();entry.putString("name","minecraft:"+names[i]);entry.putInt("id",i);CompoundTag element=new CompoundTag();element.putString("category","plains");element.putBoolean("has_precipitation",true);CompoundTag effects=new CompoundTag();effects.putInt("water_color",i==1?0x234567:0x617B64);element.put("effects",effects);entry.put("element",element);entries.add(entry);}
        biomeRegistry.put("value",entries);registry.put("minecraft:worldgen/biome",biomeRegistry);
        if(caves){CompoundTag dimensions=new CompoundTag();ListTag<CompoundTag> worlds=new ListTag<>(CompoundTag.class);int id=0;for(String name:new String[]{"minecraft:overworld","minecraft:the_nether"}){CompoundTag entry=new CompoundTag();entry.putString("name",name);entry.putInt("id",id++);entry.put("element",dimension(name));worlds.add(entry);}dimensions.put("value",worlds);registry.put("minecraft:dimension_type",dimensions);}
                if(wild)registry.put("minecraft:chat_type",Protocol1_18_2To1_19.MAPPINGS.chatRegistry().copy());
        return registry;
    }
    private int configPacket(String name){return copper?ClientboundConfigurationPackets1_21_9.valueOf(name).getId():summer?ClientboundConfigurationPackets1_21_6.valueOf(name).getId():tricky?ClientboundConfigurationPackets1_21.valueOf(name).getId():components?ClientboundConfigurationPackets1_20_5.valueOf(name).getId():trials?com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundConfigurationPackets1_20_3.valueOf(name).getId():ClientboundConfigurationPackets1_20_2.valueOf(name).getId();}
    private void configure()throws Exception {
        ByteBuf start=BlockPipelineSmokeTest.packet((year26?ClientboundPackets26_1.START_CONFIGURATION.getId():mounts?ClientboundPackets1_21_11.START_CONFIGURATION.getId():copper?ClientboundPackets1_21_9.START_CONFIGURATION.getId():summer?ClientboundPackets1_21_6.START_CONFIGURATION.getId():spring?ClientboundPackets1_21_5.START_CONFIGURATION.getId():bundles?ClientboundPackets1_21_2.START_CONFIGURATION.getId():tricky?ClientboundPackets1_21.START_CONFIGURATION.getId():components?ClientboundPackets1_20_5.START_CONFIGURATION.getId():trials?ClientboundPackets1_20_3.START_CONFIGURATION.getId():ClientboundPackets1_20_2.START_CONFIGURATION.getId()));receive(start);client.runPendingTasks();
        require(user.getProtocolInfo().getServerState()==State.CONFIGURATION,"Server entered real CONFIGURATION state");
        if(components) {
            ByteBuf packs=BlockPipelineSmokeTest.packet(configPacket("SELECT_KNOWN_PACKS"));Types.VAR_INT.writePrimitive(packs,1);Types.STRING.write(packs,"minecraft");Types.STRING.write(packs,"core");Types.STRING.write(packs,target.resources().version());receive(packs);client.runPendingTasks();
            if(tricky) {
                RegistryEntry[] enchantments=OriginalRegistryFixtures.load(target.resources().version(),"enchantment");
                for(int i=0;i<enchantments.length;i++)enchantmentIds.put(enchantments[i].key(),i);
                ByteBuf enchant=BlockPipelineSmokeTest.packet(configPacket("REGISTRY_DATA"));Types.STRING.write(enchant,"minecraft:enchantment");Types.REGISTRY_ENTRY_ARRAY.write(enchant,enchantments);receive(enchant);
            }
            for(Map.Entry<String,Tag> entry:registry().entrySet()) {
                ListTag<CompoundTag> values=((CompoundTag)entry.getValue()).getListTag("value",CompoundTag.class);RegistryEntry[] entries=new RegistryEntry[values.size()];
                for(int i=0;i<entries.length;i++)entries[i]=new RegistryEntry(values.get(i).getString("name"),values.get(i).getCompoundTag("element"));
                if(mounts&&(entry.getKey().equals("minecraft:dimension_type")||entry.getKey().equals("minecraft:worldgen/biome"))) {
                    Map<String,RegistryEntry> originals=new HashMap<>();for(RegistryEntry original:OriginalRegistryFixtures.load(target.resources().version(),entry.getKey().substring(10)))originals.put(original.key(),original);
                    for(int i=0;i<entries.length;i++){RegistryEntry original=originals.get(entries[i].key());require(original!=null,"Original dimension/biome registry entry "+entries[i].key());entries[i]=original;}
                }
                ByteBuf packet=BlockPipelineSmokeTest.packet(configPacket("REGISTRY_DATA"));Types.STRING.write(packet,entry.getKey());Types.REGISTRY_ENTRY_ARRAY.write(packet,entries);receive(packet);
            }
        } else {ByteBuf registry=BlockPipelineSmokeTest.packet(configPacket("REGISTRY_DATA"));Types.COMPOUND_TAG.write(registry,registry());receive(registry);}
        ByteBuf features=BlockPipelineSmokeTest.packet(configPacket("UPDATE_ENABLED_FEATURES"));Types.STRING_ARRAY.write(features,new String[]{"minecraft:vanilla"});receive(features);
        receive(BlockPipelineSmokeTest.packet(configPacket("FINISH_CONFIGURATION")));client.runPendingTasks();
        require(user.getProtocolInfo().getServerState()==State.PLAY,"Configuration completion restored PLAY");drain(client);drain(server);
    }
    private void joinConfigured()throws Exception {
        configure();ByteBuf packet=packet(ClientboundPackets1_13.LOGIN);packet.writeInt(1).writeBoolean(false);
        Types.STRING_ARRAY.write(packet,new String[]{"minecraft:overworld","minecraft:the_nether"});
        Types.VAR_INT.writePrimitive(packet,20);Types.VAR_INT.writePrimitive(packet,8);Types.VAR_INT.writePrimitive(packet,6);
        packet.writeBoolean(false).writeBoolean(true).writeBoolean(false);
        if(components)Types.VAR_INT.writePrimitive(packet,0);else Types.STRING.write(packet,"minecraft:overworld");Types.STRING.write(packet,"minecraft:overworld");packet.writeLong(12345L).writeByte(1).writeByte(-1).writeBoolean(false).writeBoolean(false);
        Types.OPTIONAL_GLOBAL_POSITION.write(packet,null);Types.VAR_INT.writePrimitive(packet,0);if(bundles)Types.VAR_INT.writePrimitive(packet,63);if(sulfur)packet.writeBoolean(true);if(components)packet.writeBoolean(false);
        int previous=joins;receive(packet);take(1).release();drain(client);require(joins==previous+1,"Cancelled/sent Via join still activates target session exactly once");
        if(bundles){ByteBuf abilities=BlockPipelineSmokeTest.packet(year26?ClientboundPackets26_1.PLAYER_ABILITIES.getId():mounts?ClientboundPackets1_21_11.PLAYER_ABILITIES.getId():copper?ClientboundPackets1_21_9.PLAYER_ABILITIES.getId():summer?ClientboundPackets1_21_6.PLAYER_ABILITIES.getId():spring?ClientboundPackets1_21_5.PLAYER_ABILITIES.getId():ClientboundPackets1_21_2.PLAYER_ABILITIES.getId());abilities.writeByte(0x0f).writeFloat(.05F).writeFloat(.1F);receive(abilities);drain(client);}
    }
    private void joinNether()throws Exception {
        ByteBuf packet=packet(ClientboundPackets1_13.LOGIN);packet.writeInt(1);if(netherPatch)packet.writeBoolean(false);packet.writeByte(1).writeByte(-1);
        Types.STRING_ARRAY.write(packet,new String[]{"minecraft:overworld","minecraft:the_nether"});
        CompoundTag registry=registry();Types.NAMED_COMPOUND_TAG.write(packet,registry);
        if(wild)Types.STRING.write(packet,"minecraft:overworld");else if(netherPatch)Types.NAMED_COMPOUND_TAG.write(packet,dimension("minecraft:overworld"));else Types.STRING.write(packet,"minecraft:overworld");
        Types.STRING.write(packet,"minecraft:overworld");packet.writeLong(12345L);if(netherPatch)Types.VAR_INT.writePrimitive(packet,20);else packet.writeByte(20);
        Types.VAR_INT.writePrimitive(packet,8);if(cliffs)Types.VAR_INT.writePrimitive(packet,6);packet.writeBoolean(false).writeBoolean(true).writeBoolean(false).writeBoolean(false);if(wild)Types.OPTIONAL_GLOBAL_POSITION.write(packet,null);if(trails)Types.VAR_INT.writePrimitive(packet,0);
        receive(packet);take(1).release();drain(client);require(joins==1,"Actual Nether join including registry/dimension/seed");
    }
    private void compression(){
        if(client.pipeline().get("decompress")!=null)client.pipeline().remove("decompress");
        if(client.pipeline().get("compress")!=null)client.pipeline().remove("compress");
        client.pipeline().addBefore("decoder","decompress",new NettyCompressionDecoder(256));client.pipeline().addBefore("encoder","compress",new NettyCompressionEncoder(256));
        ViaChannelInitializer.reorderPipeline(client.pipeline(),"compress","decompress");client.checkException();
    }
    private void join()throws Exception {
        if(config){joinConfigured();return;}if(nether){joinNether();return;}
        ByteBuf join=packet(ClientboundPackets1_13.LOGIN);join.writeInt(1).writeByte(1).writeInt(0);if(bee)join.writeLong(12345L);if(!village)join.writeByte(0);join.writeByte(20);Types.STRING.write(join,"default");if(village)Types.VAR_INT.writePrimitive(join,8);join.writeBoolean(false);if(bee)join.writeBoolean(true);
        receive(join);take(1).release();drain(client);require(joins==1,"One join through the actual translation pass");
    }
    private int blocks(WorldClient world)throws Exception {
        FlattenedBlockData mapping=new FlattenedBlockData();
        ChunkSection[] sections=new ChunkSection[16];sections[4]=new ChunkSectionImpl(true);sections[4].palette(PaletteType.BLOCKS).addId(0);
        sections[4].getLight().setSkyLight(new byte[2048]);sections[4].getLight().setBlockLight(new byte[2048]);
        List<Integer> expected=new ArrayList<>();Set<Integer> unique=new HashSet<>();
        for(int id=0;id<stateNames.size();id++) {
            int legacy=mapping.state(id);
            if(ClientBlocks.localState(legacy)<0||!unique.add(legacy))continue;
            sections[4].palette(PaletteType.BLOCKS).setIdAt(expected.size(),wireState(id));expected.add(legacy);
        }
        require(expected.size()>350,"Inherited flattened state coverage: "+expected.size());
        Chunk chunk=new BaseChunk(0,0,true,false,16,sections,new int[256],new ArrayList<>());
        if(!bee)chunk.getBiomeData()[0]=44;
        ByteBuf source=packet(ClientboundPackets1_13.LEVEL_CHUNK);if(village) {
            ByteBuf light=lightHeader();
            byte[] sky=new byte[2048],block=new byte[2048];Arrays.fill(sky,(byte)0xaa);Arrays.fill(block,(byte)0x55);
            if(caves)Types.VAR_INT.writePrimitive(light,1);Types.BYTE_ARRAY_PRIMITIVE.write(light,sky);if(caves)Types.VAR_INT.writePrimitive(light,1);Types.BYTE_ARRAY_PRIMITIVE.write(light,block);if(cliffs)light.release();else receive(light);
            require(client.readInbound()==null,"Separate light update is owned by Via");
            chunk.setHeightMap(new CompoundTag());
            if(bee){int[] biomes=new int[1024];Arrays.fill(biomes,0,16,1);Arrays.fill(biomes,16,1024,2);chunk.setBiomeData(biomes);writeModernChunk(source,chunk);}
            else ChunkType1_14.TYPE.write(source,chunk);
        }else new ChunkType1_13(true).write(source,chunk);receive(source);
        com.viaversion.viaforge.common.compatibility.WaterColors water=user.get(FlattenedProtocolAdapter.class).waterColors;
        require(water.at(0,0,0,-1)==(!bee?0x43D5EE:nether&&!mounts?0x234567:0x3F76E4),"Original water biome before Via remapping at Y=0");
        if(nether)require(water.at(0,64,0,-1)==(mounts?0x3F76E4:0x617B64),"Original three-dimensional water biome at Y=64");
        S21PacketChunkData nativeChunk=new S21PacketChunkData();ByteBuf data=take(0x21);
        try{nativeChunk.readPacketData(new PacketBuffer(data));}finally{data.release();}
        world.getChunkFromChunkCoords(0,0).fillChunk(nativeChunk.getExtractedDataBytes(),nativeChunk.getExtractedSize(),true);
        if(bee)for(byte biome:world.getChunkFromChunkCoords(0,0).getBiomeArray())require(biome==1,"1.15 biome cells translated once by Via");
        for(int i=0;i<expected.size();i++)require(Block.BLOCK_STATE_IDS.get(world.getBlockState(new BlockPos(i&15,64+(i>>8),(i>>4)&15)))==ClientBlocks.localState(expected.get(i)),"Flattened chunk state "+expected.get(i));
        if(village) {
            net.minecraft.world.chunk.storage.ExtendedBlockStorage section=world.getChunkFromChunkCoords(0,0).getBlockStorageArray()[4];
            require(section.getExtSkylightValue(15,15,15)==10&&section.getExtBlocklightValue(15,15,15)==5,"Original separate sky/block light survived restoration");
        }
        for(int color=0;color<16;color++)for(String part:new String[]{"head","foot"}) {
            compression();String name=FlattenedBlockData.modernName(LegacyBlockCatalog.COLORS[color])+"_bed[facing=east,occupied=false,part="+part+"]";
            ByteBuf update=packet(ClientboundPackets1_13.BLOCK_UPDATE);positionType().write(update,new BlockPosition(0,64,0));Types.VAR_INT.writePrimitive(update,wireState(states.get(name)));receive(update);
            S23PacketBlockChange nativeUpdate=new S23PacketBlockChange();data=take(0x23);
            try{nativeUpdate.readPacketData(new PacketBuffer(data));}finally{data.release();}
            require(Block.BLOCK_STATE_IDS.get(nativeUpdate.getBlockState())==ClientBlocks.localState(LegacyBlockCatalog.bedState(3|(part.equals("head")?8:0),color)),"Direct bed state color "+color+" "+part);
        }
        ByteBuf multi=packet(ClientboundPackets1_13.CHUNK_BLOCKS_UPDATE);
        if(netherPatch){multi.writeLong(4);if(!trails)multi.writeBoolean(false);Types.VAR_LONG_BLOCK_CHANGE_ARRAY.write(multi,new com.viaversion.viaversion.api.minecraft.BlockChangeRecord[]{new com.viaversion.viaversion.api.minecraft.BlockChangeRecord1_16_2(1,0,1,wireState(states.get("purple_concrete")))});}
        else {multi.writeInt(0).writeInt(0);Types.BLOCK_CHANGE_ARRAY.write(multi,new com.viaversion.viaversion.api.minecraft.BlockChangeRecord[]{new com.viaversion.viaversion.api.minecraft.BlockChangeRecord1_8(1,64,1,wireState(states.get("purple_concrete")))});}receive(multi);
        data=take(0x22);try{require(data.readInt()==0&&data.readInt()==0,"Multi-block coordinates");require(Types.BLOCK_CHANGE_ARRAY.read(data)[0].getBlockId()==ClientBlocks.localState(251<<4|10),"Flattened multi-block state");}finally{data.release();}
        int powder=states.get("red_concrete_powder");
        ByteBuf falling=packet(ClientboundPackets1_13.ADD_ENTITY);Types.VAR_INT.writePrimitive(falling,77);Types.UUID.write(falling,new UUID(0,77));objectType(falling,70);spawnCoordinates(falling,8,80,8);falling.writeByte(0).writeByte(0);objectData(falling,wireState(powder));spawnVelocity(falling);receive(falling);
        data=take(0x0e);try{Types.VAR_INT.readPrimitive(data);data.skipBytes(15);int raw=data.readInt();require(((raw&4095)<<4|(raw>>12&15))==ClientBlocks.localState(252<<4|14),"Flattened falling powder");}finally{data.release();}
        ByteBuf event=packet(ClientboundPackets1_13.BLOCK_EVENT);positionType().write(event,new BlockPosition(0,64,0));event.writeByte(1).writeByte(1);int shulker=village?Protocol1_13_2To1_14.MAPPINGS.getNewBlockId(483):483;Types.VAR_INT.writePrimitive(event,netherBlock(bee?Protocol1_14_4To1_15.MAPPINGS.getNewBlockId(shulker):shulker));receive(event);
        data=take(0x24);try{Types.BLOCK_POSITION1_8.read(data);data.skipBytes(2);require(Types.VAR_INT.readPrimitive(data)==ClientBlocks.localState(219<<4)>>4,"Flattened shulker block event");}finally{data.release();}
        int unsupported=-1;
        if(village) {
            com.viaversion.viaversion.api.data.Mappings inverse=(trials?Protocol1_20_2To1_20_3.MAPPINGS:trails?Protocol1_19_4To1_20.MAPPINGS:wild4?Protocol1_19_3To1_19_4.MAPPINGS:wild3?Protocol1_19_1To1_19_3.MAPPINGS:wild?Protocol1_18_2To1_19.MAPPINGS:caves?Protocol1_16_4To1_17.MAPPINGS:nether?Protocol1_15_2To1_16.MAPPINGS:bee?Protocol1_14_4To1_15.MAPPINGS:Protocol1_13_2To1_14.MAPPINGS).getBlockStateMappings().inverse();
            for(int id=0;id<inverse.size();id++)if(inverse.getNewId(id)<0){unsupported=netherPatch&&!caves?Protocol1_16_1To1_16_2.MAPPINGS.getNewBlockStateId(id):id;break;}
        }else for(int id=0;id<stateNames.size();id++)if(mapping.state(id)==65535){unsupported=wireState(id);break;}
        require(unsupported>=0,"Actual target contains a state outside the inherited catalog");
        for(int next:new int[]{wireState(states.get("purpur_block")),unsupported}) {
            ByteBuf update=packet(ClientboundPackets1_13.BLOCK_UPDATE);positionType().write(update,new BlockPosition(0,64,0));Types.VAR_INT.writePrimitive(update,next);receive(update);
            data=take(0x23);data.release();
            require(cached(0,64,0)==(next!=unsupported),"Unsupported target update invalidates previous Purpur state");
        }
        drain(client);return expected.size();
    }
    private void heightWindow(WorldClient world)throws Exception {
        if(!caves)return;
        require(world.isAirBlock(new BlockPos(0,0,0))&&world.isAirBlock(new BlockPos(0,255,0)),"Tall source sections never wrap into native Y=0/255");
        for(int y:new int[]{-64,-1,256,319}) {
            ByteBuf update=packet(ClientboundPackets1_13.BLOCK_UPDATE);positionType().write(update,new BlockPosition(0,y,0));Types.VAR_INT.writePrimitive(update,wireState(states.get("purpur_block")));receive(update);
            require(client.readInbound()==null,"Via clips out-of-window single update "+y);
            ByteBuf multi=packet(ClientboundPackets1_13.CHUNK_BLOCKS_UPDATE);multi.writeLong((y>>4)&0xfffffL);if(!trails)multi.writeBoolean(false);
            Types.VAR_LONG_BLOCK_CHANGE_ARRAY.write(multi,new com.viaversion.viaversion.api.minecraft.BlockChangeRecord[]{new com.viaversion.viaversion.api.minecraft.BlockChangeRecord1_16_2(0,y&15,0,wireState(states.get("purpur_block")))});receive(multi);
            require(client.readInbound()==null,"Via clips out-of-window section update "+y);
            require(!cached(0,y,0),"Out-of-window states are not falsely restored "+y);
        }
    }
    private void items()throws Exception {
        List<LegacyItemDefinition> definitions=new ArrayList<>(LegacyBlockCatalog.BLOCKS);definitions.addAll(LegacyItemCatalog.ITEMS);
        for(LegacyItemDefinition definition:definitions) {
            if(definition.itemId()<0)continue;
            CompoundTag tag=new CompoundTag();tag.putString("Custom","retained");
            if(definition.itemId()==383){CompoundTag entity=new CompoundTag();entity.putString("id","minecraft:bat");tag.put("EntityTag",entity);}
            Item legacy=new DataItem(definition.itemId(),(byte)1,(short)(definition.preservesDamage()?31:definition.itemData()),tag);
            Item original=target.adapter().items().toServerData(user,legacy);
            Item local=slot(original,36);
            require(local.identifier()==ClientItems.localItem(legacy.identifier(),legacy.data()),"Flattened inventory "+definition.itemId()+":"+definition.itemData()+" -> "+local);
            require(local.data()==(definition.preservesDamage()?31:0),"Flattened durability "+definition.itemId());
            for(boolean creative:new boolean[]{false,true}) {
                if(spring&&!creative){sendHashedItem(local,original);continue;}
                Item returned=sendItem(local,creative);require(returned.identifier()==original.identifier(),"Target item ID "+definition.itemId());
                require(returned.amount()==original.amount(),"Target amount");
                sameItemData(original,returned,"Target data "+definition.itemId());
            }
            Item fresh=local.copy();fresh.setTag(null);
            // A fresh egg's species is semantic NBT, supplied by Creative. Removing
            // it tests an invalid empty egg, not the same item without Via tags.
            if(definition.itemId()==383){fresh.setTag(new CompoundTag());fresh.tag().put("EntityTag",tag.getCompoundTag("EntityTag").copy());}
            Item picked=sendItem(fresh,true);require(picked.identifier()==original.identifier(),"Fresh flattened Creative item "+definition.itemId());
        }
        // Direct target NBT, independent of a reverse-generated fixture.
        CompoundTag tag=new CompoundTag();tag.putInt("Damage",57);tag.putString("Custom","shield");
        CompoundTag display=new CompoundTag();display.putString("Name","{\"text\":\"Original shield\",\"color\":\"gold\"}");tag.put("display",display);
        if(village){ListTag<StringTag> lore=new ListTag<>(StringTag.class);lore.add(new StringTag("{\"text\":\"Original 1.14 lore\",\"italic\":false}"));display.put("Lore",lore);}
        ListTag<CompoundTag> enchantments=new ListTag<>(CompoundTag.class);CompoundTag enchant=new CompoundTag();enchant.putString("id","minecraft:mending");enchant.putShort("lvl",(short)1);enchantments.add(enchant);tag.put("Enchantments",enchantments);
        CompoundTag banner=new CompoundTag();banner.putInt("Base",3);tag.put("BlockEntityTag",banner);
        int shield=wireItem(Protocol1_12_2To1_13.MAPPINGS.getNewItemId(442<<4));
        Item original=components?componentShield(shield):new DataItem(shield,(byte)1,(short)0,tag);Item local=slot(original,36);
        require(local.data()==57&&local.tag().getListTag("ench",CompoundTag.class).get(0).getShort("id")==70,"Direct modern Damage and namespaced enchantment");
        Item result=sendItem(local,spring);sameItemData(original,result,"Exact original shield data");
        if(spring) {
            for(boolean creativeMode:new boolean[]{true,false}) {
                ByteBuf abilities=BlockPipelineSmokeTest.packet(year26?ClientboundPackets26_1.PLAYER_ABILITIES.getId():mounts?ClientboundPackets1_21_11.PLAYER_ABILITIES.getId():copper?ClientboundPackets1_21_9.PLAYER_ABILITIES.getId():summer?ClientboundPackets1_21_6.PLAYER_ABILITIES.getId():ClientboundPackets1_21_5.PLAYER_ABILITIES.getId());abilities.writeByte(creativeMode?0x0f:0).writeFloat(.05F).writeFloat(.1F);receive(abilities);drain(client);
                sendHashedItem(slot(original,36),original);
            }
            ByteBuf abilities=BlockPipelineSmokeTest.packet(year26?ClientboundPackets26_1.PLAYER_ABILITIES.getId():mounts?ClientboundPackets1_21_11.PLAYER_ABILITIES.getId():copper?ClientboundPackets1_21_9.PLAYER_ABILITIES.getId():summer?ClientboundPackets1_21_6.PLAYER_ABILITIES.getId():ClientboundPackets1_21_5.PLAYER_ABILITIES.getId());abilities.writeByte(0x0f).writeFloat(.05F).writeFloat(.1F);receive(abilities);drain(client);
        }
        if(components) {
            net.minecraft.item.ItemStack nativeShield;PacketBuffer nativeBuffer=new PacketBuffer(io.netty.buffer.Unpooled.buffer());try{Types.ITEM1_8.write(nativeBuffer,local);nativeShield=nativeBuffer.readItemStackFromBuffer();}finally{nativeBuffer.release();}require(nativeShield.getMaxStackSize()==1&&nativeShield.getMaxDamage()==500,"Original component stack and damage limits in native ItemStack");
            Item damaged=slot(original,36);damaged.setData((short)61);require(sendItem(damaged,spring).dataContainer().get(StructuredDataKey.DAMAGE)==61,"Component durability remains live");
        }
        ByteBuf off=packet(ClientboundPackets1_13.CONTAINER_SET_SLOT);slotHeader(off,0,45);writeItem(off,original);receive(off);ByteBuf event=event(26);
        try{Item held=Types.ITEM1_8.read(event);require(held.identifier()==442&&held.data()==57,"Original offhand shield after flattened codec");require(ClientItems.is(ServerEntityViews.item(held),LegacyItemCatalog.Kind.SHIELD),"Native shield implementation selected");}finally{event.release();}
        ByteBuf full=packet(ClientboundPackets1_13.CONTAINER_SET_CONTENT);if(bundles)Types.VAR_INT.writePrimitive(full,0);else full.writeByte(0);if(cavesPatch){Types.VAR_INT.writePrimitive(full,103);Types.VAR_INT.writePrimitive(full,46);}else full.writeShort(46);
        for(int i=0;i<46;i++)writeItem(full,i==45?original.copy():null);if(cavesPatch)writeItem(full,null);receive(full);event=event(26);
        try{require(Types.ITEM1_8.read(event).identifier()==442,"Full flattened inventory includes shield in offhand");}finally{event.release();}
        ByteBuf direct=bundles?BlockPipelineSmokeTest.packet(year26?ClientboundPackets26_1.SET_PLAYER_INVENTORY.getId():mounts?ClientboundPackets1_21_11.SET_PLAYER_INVENTORY.getId():copper?ClientboundPackets1_21_9.SET_PLAYER_INVENTORY.getId():summer?ClientboundPackets1_21_6.SET_PLAYER_INVENTORY.getId():spring?ClientboundPackets1_21_5.SET_PLAYER_INVENTORY.getId():ClientboundPackets1_21_2.SET_PLAYER_INVENTORY.getId()):packet(ClientboundPackets1_13.CONTAINER_SET_SLOT);if(bundles)Types.VAR_INT.writePrimitive(direct,40);else slotHeader(direct,-2,40);writeItem(direct,original.copy());receive(direct);event=event(26);
        try{require(Types.ITEM1_8.read(event).data()==57,"Direct offhand index retains Damage");}finally{event.release();}
        ByteBuf equipment=packet(ClientboundPackets1_13.SET_EQUIPPED_ITEM);Types.VAR_INT.writePrimitive(equipment,1);if(nether)equipment.writeByte(1);else Types.VAR_INT.writePrimitive(equipment,1);writeItem(equipment,original.copy());receive(equipment);event=event(3);
        try{require(Types.VAR_INT.readPrimitive(event)==1&&Types.VAR_INT.readPrimitive(event)==1&&Types.ITEM1_8.read(event).identifier()==442,"Offhand equipment identity");}finally{event.release();}
        // Count changes must not be undone by snapshots.
        Item originalFruit=components?new StructuredItem(wireItem(Protocol1_12_2To1_13.MAPPINGS.getNewItemId(432<<4)),5):new DataItem(wireItem(Protocol1_12_2To1_13.MAPPINGS.getNewItemId(432<<4)),(byte)5,(short)0,null);
        Item fruit=slot(originalFruit,36);fruit.setAmount(2);if(spring){originalFruit.setAmount(2);sendHashedItem(fruit,originalFruit);}else require(sendItem(fruit,false).amount()==2,"Split inventory count");
        drain(client);
    }
    private void writeItem(ByteBuf buffer,Item item)throws Exception {itemType.write(buffer,components&&item==null?StructuredItem.empty():item);}
    private Item readItem(ByteBuf buffer)throws Exception {Item item=itemType.read(buffer);return item==null||item.isEmpty()?null:item;}
    private void sameItemData(Item expected,Item actual,String label) {
        require(components?sameComponents(expected.dataContainer(),actual.dataContainer()):Objects.equals(expected.tag(),actual.tag()),label+": "+expected+" -> "+actual);
    }
    private boolean sameComponents(StructuredDataContainer expected,StructuredDataContainer actual) {
        if(!expected.data().keySet().equals(actual.data().keySet()))return false;
        for(StructuredData<?> data:expected.data().values()) {
            StructuredData<?> other=actual.getData(data.key());if(data.isEmpty()!=other.isEmpty()||data.id()!=other.id())return false;
            if(data.isEmpty())continue;
            ByteBuf a=io.netty.buffer.Unpooled.buffer(),b=io.netty.buffer.Unpooled.buffer();
            try{data.write(a);other.write(b);if(!a.equals(b))return false;}finally{a.release();b.release();}
        }
        return true;
    }
    private Item componentItem(int id) {
        Item item=new StructuredItem(id,1);StructuredDataContainer data=item.dataContainer();data.setIdLookup(sulfur?Via.getManager().getProtocolManager().getProtocol(Protocol26_1To26_2.class):year26?Via.getManager().getProtocolManager().getProtocol(Protocol1_21_11To26_1.class):mounts?Via.getManager().getProtocolManager().getProtocol(Protocol1_21_9To1_21_11.class):copper?Via.getManager().getProtocolManager().getProtocol(Protocol1_21_7To1_21_9.class):summerPatch?Via.getManager().getProtocolManager().getProtocol(Protocol1_21_6To1_21_7.class):summer?Via.getManager().getProtocolManager().getProtocol(Protocol1_21_5To1_21_6.class):spring?Via.getManager().getProtocolManager().getProtocol(Protocol1_21_4To1_21_5.class):winter?Via.getManager().getProtocolManager().getProtocol(Protocol1_21_2To1_21_4.class):bundles?Via.getManager().getProtocolManager().getProtocol(Protocol1_21To1_21_2.class):tricky?Via.getManager().getProtocolManager().getProtocol(Protocol1_20_5To1_21.class):Via.getManager().getProtocolManager().getProtocol(Protocol1_20_3To1_20_5.class),true);
        return item;
    }
    private Item componentShield(int id) {
        Item item=componentItem(id);StructuredDataContainer data=item.dataContainer();
        CompoundTag custom=new CompoundTag();custom.putString("Custom","shield");data.set(StructuredDataKey.CUSTOM_DATA,custom);data.set(StructuredDataKey.DAMAGE,57);data.set(StructuredDataKey.MAX_DAMAGE,500);data.set(StructuredDataKey.MAX_STACK_SIZE,1);
        CompoundTag name=new CompoundTag();name.putString("text","Original shield");name.putString("color","gold");data.set(StructuredDataKey.CUSTOM_NAME,name);
        CompoundTag lore=new CompoundTag();lore.putString("text","Original component lore");lore.putBoolean("italic",false);data.set(StructuredDataKey.LORE,new Tag[]{lore});
        Enchantments enchant=new Enchantments(true);enchant.add(tricky?enchantmentIds.get("minecraft:mending"):com.viaversion.viaversion.protocols.v1_20_3to1_20_5.data.Enchantments1_20_5.keyToId("mending"),1);data.set(spring?StructuredDataKey.ENCHANTMENTS1_21_5:StructuredDataKey.ENCHANTMENTS1_20_5,enchant);data.set(StructuredDataKey.BASE_COLOR,3);
        data.setEmpty(summer?StructuredDataKey.ATTRIBUTE_MODIFIERS1_21_6:spring?StructuredDataKey.ATTRIBUTE_MODIFIERS1_21_5:tricky?StructuredDataKey.ATTRIBUTE_MODIFIERS1_21:StructuredDataKey.ATTRIBUTE_MODIFIERS1_20_5);
        if(winter)data.set(StructuredDataKey.CUSTOM_MODEL_DATA1_21_4,new com.viaversion.viaversion.api.minecraft.item.data.CustomModelData1_21_4(new float[]{1.25F,-2},new boolean[]{true,false},new String[]{"original","layer"},new int[]{0x123456,0xabcdef}));return item;
    }
    private void eggs()throws Exception {
        for(com.google.gson.JsonElement entry:ItemVariants.EGGS) {
            com.google.gson.JsonObject egg=entry.getAsJsonObject();String species=egg.get("name").getAsString();
            int flat=Protocol1_12_2To1_13.MAPPINGS.getNewItemId(com.viaversion.viaversion.protocols.v1_12_2to1_13.data.SpawnEggMappings1_13.getSpawnEggId("minecraft:"+species));
            int id=wireItem(flat);
            // Real servers send the species in the item ID, without a redundant EntityTag.
            Item original=components?new StructuredItem(id,1):new DataItem(id,(byte)1,(short)0,null);
            Item local=slot(original,36);
            net.minecraft.item.ItemStack displayed=nativeStack(local);
            require(egg.equals(ItemVariants.eggType(displayed)),"Server egg retains species "+species+": "+local);
            net.minecraft.item.ItemStack fresh=ItemVariants.egg(ServerEntitySmokeTest.stack(383,0),egg);
            require(Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(displayed)==Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(fresh),"Same egg model before/after inventory echo "+species);
            Item picked=sendItem(ServerItemSmokeTest.wire(fresh),true);
            require(picked.identifier()==id,"Fresh Creative egg has exact target species ID "+species+": "+picked.identifier()+" != "+id);
            require(egg.equals(ItemVariants.eggType(nativeStack(slot(picked,36)))),"Creative egg echo retains species "+species);
            Item returned=sendItem(local,true);sameItemData(original,returned,"Original egg data "+species);
            require(returned.identifier()==id,"Returned egg ID "+species);
            local.setAmount(7);
            if(spring)sendHashedItem(local,original);else require(sendItem(local,false).identifier()==id,"Survival egg ID "+species);
            if(sulfur) {
                Item typed=componentItem(id);CompoundTag entity=new CompoundTag();entity.putInt("Age",-123);
                String modern=SpawnEggNames.MODERN.get(species);
                int entityId=com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_2.valueOf(modern.toUpperCase(java.util.Locale.ROOT)).getId();
                typed.dataContainer().set(StructuredDataKey.ENTITY_DATA1_21_9,new com.viaversion.viaversion.api.minecraft.item.data.EntityData(entityId,entity));
                Item shown=slot(typed,36);
                require(egg.equals(ItemVariants.eggType(nativeStack(shown))),"Typed entity component retains egg appearance "+species+": "+shown);
                sameItemData(typed,sendItem(shown,true),"Exact typed entity component "+species);
                sendHashedItem(shown,typed);
                if(species.equals("shulker")) {
                    // Modern appearance follows the egg item, even if its component
                    // deliberately summons another species. Never rewrite that data.
                    typed.dataContainer().set(StructuredDataKey.ENTITY_DATA1_21_9,new com.viaversion.viaversion.api.minecraft.item.data.EntityData(com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_2.PIG.getId(),entity));
                    shown=slot(typed,36);
                    require(egg.equals(ItemVariants.eggType(nativeStack(shown))),"Shulker egg keeps its model with an overridden spawn species");
                    sameItemData(typed,sendItem(shown,true),"Custom spawn species remains server-owned");
                }
            }
        }
    }
    private static net.minecraft.item.ItemStack nativeStack(Item item)throws Exception {
        ByteBuf data=io.netty.buffer.Unpooled.buffer();
        try{Types.ITEM1_8.write(data,item);return new PacketBuffer(data).readItemStackFromBuffer();}finally{data.release();}
    }
    private void boatInput(WorldClient world)throws Exception {
        com.viaversion.viaforge.boats.ServerBoat boat=new com.viaversion.viaforge.boats.ServerBoat(world,target.serverProtocol());
        boat.setPosition(2.5,65,-3.25);boat.rotationYaw=27;boat.rotationPitch=-4;
        boatPacket(com.viaversion.viaforge.boats.BoatPackets.movement(boat));
        ByteBuf data=BlockPipelineSmokeTest.take(server,outgoing(ServerboundPackets1_13.MOVE_VEHICLE));
        try{require(data.readDouble()==2.5&&data.readDouble()==65&&data.readDouble()==-3.25&&data.readFloat()==27&&data.readFloat()==-4,"Exact boat movement");if(winter)require(data.readBoolean(),"Modern vehicle ground flag");require(!data.isReadable(),"Vehicle movement consumed");}finally{data.release();}
        for(int flags=0;flags<4;flags++) {
            boatPacket(com.viaversion.viaforge.boats.BoatPackets.rowing((flags&1)!=0,(flags&2)!=0));
            data=BlockPipelineSmokeTest.take(server,outgoing(ServerboundPackets1_13.PADDLE_BOAT));
            try{require(data.readBoolean()==((flags&1)!=0)&&data.readBoolean()==((flags&2)!=0)&&!data.isReadable(),"Independent paddle flags");}finally{data.release();}
            for(float sideways:new float[]{-1,0,1})for(float forward:new float[]{-1,0,1}) {
                boatPacket(com.viaversion.viaforge.boats.BoatPackets.input(sideways,forward,(flags&1)!=0,(flags&2)!=0));
                data=BlockPipelineSmokeTest.take(server,outgoing(ServerboundPackets1_13.PLAYER_INPUT));
                try{if(bundles){int expected=(forward>0?1:forward<0?2:0)|(sideways>0?4:sideways<0?8:0)|(flags<<4);require(data.readUnsignedByte()==expected,"Compact vehicle input including jump/dismount");}
                    else require(data.readFloat()==sideways&&data.readFloat()==forward&&data.readUnsignedByte()==flags,"Legacy vehicle input");
                    require(!data.isReadable(),"Boat input consumed");}finally{data.release();}
                drain(server);
            }
        }
    }
    private void boatPacket(net.minecraft.network.play.client.C17PacketCustomPayload packet)throws Exception {
        ByteBuf data=BlockPipelineSmokeTest.packet(0x17);packet.writePacketData(new PacketBuffer(data));send(data);
    }
    private Item slot(Item item,int slot)throws Exception {
        ByteBuf source=packet(ClientboundPackets1_13.CONTAINER_SET_SLOT);slotHeader(source,0,slot);writeItem(source,item.copy());receive(source);ByteBuf data=take(0x2f);
        try{S2FPacketSetSlot nativeSlot=new S2FPacketSetSlot();nativeSlot.readPacketData(new PacketBuffer(data.duplicate()));require(nativeSlot.func_149174_e()!=null,"Native item decoding");data.skipBytes(3);return Types.ITEM1_8.read(data);}finally{data.release();}
    }
    private Item sendItem(Item item,boolean creative)throws Exception {
        ByteBuf data=sendInventory(item,creative);
        try{Item result=spring&&creative?(sulfur?VersionedTypes.V26_2.lengthPrefixedItem():year26?VersionedTypes.V26_1.lengthPrefixedItem():mounts?VersionedTypes.V1_21_11.lengthPrefixedItem():copper?VersionedTypes.V1_21_9.lengthPrefixedItem():summer?VersionedTypes.V1_21_6.lengthPrefixedItem():VersionedTypes.V1_21_5.lengthPrefixedItem()).read(data):readItem(data);require(!data.isReadable(),"Target item codec fully consumed");return result;}finally{data.release();}
    }
    private void sendHashedItem(Item local,Item original)throws Exception {
        HashedItem expected=((com.viaversion.viaversion.data.item.ItemHasherBase)user.getItemHasher(sulfur?com.viaversion.viabackwards.protocol.v26_2to26_1.Protocol26_2To26_1.class:year26?com.viaversion.viabackwards.protocol.v26_1to1_21_11.Protocol26_1To1_21_11.class:mounts?com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.Protocol1_21_11To1_21_9.class:copper?com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.Protocol1_21_9To1_21_7.class:summerPatch?com.viaversion.viabackwards.protocol.v1_21_7to1_21_6.Protocol1_21_7To1_21_6.class:summer?com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.Protocol1_21_6To1_21_5.class:Protocol1_21_5To1_21_4.class)).toHashedItem(original,false);
        require(!expected.dataHashesById().containsValue(com.viaversion.viaversion.data.item.ItemHasherBase.UNKNOWN_HASH),"All original fixture components have real hash codecs");
        ByteBuf data=sendInventory(local,false);
        try{HashedItem actual=Types.HASHED_ITEM.read(data);require(!data.isReadable(),"Hash click consumed");require(actual.identifier()==original.identifier()&&actual.amount()==local.amount(),"Original hashed item ID and live count");require(expected.dataHashesById().equals(actual.dataHashesById())&&expected.removedDataIds().equals(actual.removedDataIds()),"Original component hashes and removed defaults survive the full pipeline: "+expected+" -> "+actual+" native hash record="+(local.tag()==null?null:local.tag().getCompoundTag("VV|original_hashes")));}finally{data.release();}
    }
    private ByteBuf sendInventory(Item item,boolean creative)throws Exception {
        ByteBuf source=BlockPipelineSmokeTest.packet(creative?0x10:0x0e);
        if(creative)source.writeShort(36);else source.writeByte(0).writeShort(36).writeByte(0).writeShort(17).writeByte(0);
        Types.ITEM1_8.write(source,item.copy());send(source);
        ByteBuf data=BlockPipelineSmokeTest.take(server,outgoing(creative?ServerboundPackets1_13.SET_CREATIVE_MODE_SLOT:ServerboundPackets1_13.CONTAINER_CLICK));
        try{if(creative)data.readShort();else{require((bundles?Types.VAR_INT.readPrimitive(data):data.readByte())==0,"Target click container");if(cavesPatch)require(Types.VAR_INT.readPrimitive(data)==103,"Original inventory state ID");
            require(data.readShort()==36&&data.readByte()==0,"Target clicked slot/button");if(!caves)require(data.readShort()==17,"Legacy transaction ID");require(Types.VAR_INT.readPrimitive(data)==0,"Target click mode");
            if(caves){require(Types.VAR_INT.readPrimitive(data)==1&&data.readShort()==36&&(spring?Types.HASHED_ITEM.read(data).isEmpty():readItem(data)==null),"Predicted empty slot, carried item follows");}}return data;}catch(Throwable failure){data.release();throw failure;}
    }
    private void events()throws Exception {
        ByteBuf player=packet(ClientboundPackets1_13.ADD_PLAYER);Types.VAR_INT.writePrimitive(player,2);Types.UUID.write(player,new UUID(0,2));if(config)Types.VAR_INT.writePrimitive(player,wireEntity(com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.EntityType.PLAYER.getId()));
        spawnCoordinates(player,1,65,2);player.writeByte(0).writeByte(0);if(config){objectData(player,0);spawnVelocity(player);}if(!bee)player.writeByte(255);receive(player);event(5).release();
        ByteBuf hand=packet(ClientboundPackets1_13.SET_ENTITY_DATA);Types.VAR_INT.writePrimitive(hand,2);
        hand.writeByte(caves?8:village?7:6).writeByte(0).writeByte(3).writeByte(255);receive(hand);ByteBuf handData=event(2);
        try{require(Types.VAR_INT.readPrimitive(handData)==2,"Remote player metadata");List<com.viaversion.viaversion.api.minecraft.entitydata.EntityData> entries=Types.ENTITY_DATA_LIST1_12.read(handData);require(entries.size()==1&&entries.get(0).id()==6&&((Byte)entries.get(0).value())==3,"Original player offhand-use flags normalized after pose removal");}finally{handData.release();}drain(client);
        com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.EntityType[] mobs={com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.EntityType.SHULKER,com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.EntityType.POLAR_BEAR,com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.EntityType.LLAMA,com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.EntityType.PARROT};
        int[] oldIds={69,102,103,105};
        for(int i=0;i<mobs.length;i++) {
            ByteBuf mob=packet(ClientboundPackets1_13.ADD_MOB);Types.VAR_INT.writePrimitive(mob,100+i);Types.UUID.write(mob,new UUID(0,100+i));Types.VAR_INT.writePrimitive(mob,wireEntity(mobs[i].getId()));
            spawnCoordinates(mob,2,65,2);mob.writeByte(0).writeByte(0).writeByte(0);if(wild)Types.VAR_INT.writePrimitive(mob,0);spawnVelocity(mob);if(!bee)mob.writeByte(255);receive(mob);
            ByteBuf converted=event(11);try{Types.VAR_INT.readPrimitive(converted);Types.UUID.read(converted);require(Types.VAR_INT.readPrimitive(converted)==oldIds[i],"Flattened mob registry "+mobs[i]);}finally{converted.release();}
        }
        if(mounts) {
            ByteBuf arm=packet(ClientboundPackets1_13.SET_ENTITY_DATA);Types.VAR_INT.writePrimitive(arm,2);
            arm.writeByte(15);Types.VAR_INT.writePrimitive(arm,year26?VersionedTypes.V26_1.entityDataTypes.humanoidArmType.typeId():VersionedTypes.V1_21_11.entityDataTypes.humanoidArmType.typeId());Types.VAR_INT.writePrimitive(arm,0);arm.writeByte(255);receive(arm);
            ByteBuf normalizedArm=event(2);try {
                require(Types.VAR_INT.readPrimitive(normalizedArm)==2,"Humanoid-arm entity");
                List<com.viaversion.viaversion.api.minecraft.entitydata.EntityData> values=Types.ENTITY_DATA_LIST1_12.read(normalizedArm);
                require(values.size()==1&&values.get(0).id()==14&&Byte.valueOf((byte)0).equals(values.get(0).value()),"1.21.11 humanoid arm becomes legacy left-hand byte: "+values);
            }finally{normalizedArm.release();}drain(client);
        }
        if(year26) {
            ByteBuf age=packet(ClientboundPackets1_13.SET_ENTITY_DATA);Types.VAR_INT.writePrimitive(age,101);
            age.writeByte(15).writeByte(0).writeByte(2);age.writeByte(16).writeByte(8).writeBoolean(true);age.writeByte(17).writeByte(8).writeBoolean(true);age.writeByte(255);receive(age);
            ByteBuf normalizedAge=event(2);try {
                require(Types.VAR_INT.readPrimitive(normalizedAge)==101,"26.1 ageable identity");
                List<com.viaversion.viaversion.api.minecraft.entitydata.EntityData> values=Types.ENTITY_DATA_LIST1_12.read(normalizedAge);
                require(values.size()==2&&values.get(0).id()==11&&Byte.valueOf((byte)2).equals(values.get(0).value())&&values.get(1).id()==12&&Boolean.TRUE.equals(values.get(1).value()),"26.1 age-locked field removed without shifting baby/handedness: "+values);
            }finally{normalizedAge.release();}drain(client);
        }
        ByteBuf cloud=packet(ClientboundPackets1_13.ADD_ENTITY);Types.VAR_INT.writePrimitive(cloud,79);Types.UUID.write(cloud,new UUID(0,79));objectType(cloud,3);spawnCoordinates(cloud,1,65,2);cloud.writeByte(0).writeByte(0);objectData(cloud,0);spawnVelocity(cloud);receive(cloud);event(1).release();
        ByteBuf cloudData=packet(ClientboundPackets1_13.SET_ENTITY_DATA);Types.VAR_INT.writePrimitive(cloudData,79);cloudData.writeByte(components?10:caves?11:village?10:9).writeByte(copper?16:wild4?17:wild3?16:15);Types.VAR_INT.writePrimitive(cloudData,particle(17));if(components)cloudData.writeInt(0xff336699);cloudData.writeByte(255);receive(cloudData);
        ByteBuf cloudEvent=event(2);try{Types.VAR_INT.readPrimitive(cloudEvent);List<com.viaversion.viaversion.api.minecraft.entitydata.EntityData> metadata=Types.ENTITY_DATA_LIST1_12.read(cloudEvent);require(metadata.size()==3&&((Integer)metadata.get(0).value())==15,"Cloud particle survives Via's cancelled metadata filter");}finally{cloudEvent.release();}drain(client);
        ByteBuf boat=packet(ClientboundPackets1_13.ADD_ENTITY);Types.VAR_INT.writePrimitive(boat,80);Types.UUID.write(boat,new UUID(0,80));objectType(boat,1);spawnCoordinates(boat,1.5,65,2.5);boat.writeByte(0).writeByte(0);objectData(boat,0);spawnVelocity(boat);receive(boat);
        ByteBuf data=event(20);try{require(Types.VAR_INT.readPrimitive(data)==80,"Boat retained");}finally{data.release();}
        if(bundles){data=event(2);try{require(Types.VAR_INT.readPrimitive(data)==80,"Split boat metadata entity");List<com.viaversion.viaversion.api.minecraft.entitydata.EntityData> boatData=Types.ENTITY_DATA_LIST1_12.read(data);boolean variant=false;for(com.viaversion.viaversion.api.minecraft.entitydata.EntityData entry:boatData)if(entry.id()==9&&Integer.valueOf(5).equals(entry.value()))variant=true;require(variant,"Dark oak entity ID becomes inherited dark oak boat variant");}finally{data.release();}}
        ByteBuf passengers=packet(ClientboundPackets1_13.SET_PASSENGERS);Types.VAR_INT.writePrimitive(passengers,80);Types.VAR_INT_ARRAY_PRIMITIVE.write(passengers,new int[]{1,2});receive(passengers);data=event(13);try{require(Types.VAR_INT.readPrimitive(data)==80&&Types.VAR_INT_ARRAY_PRIMITIVE.read(data).length==2,"Both boat passengers");}finally{data.release();}
        ByteBuf meta=packet(ClientboundPackets1_13.SET_ENTITY_DATA);Types.VAR_INT.writePrimitive(meta,80);if(!bundles){meta.writeByte(caves?11:village?10:9).writeByte(1);Types.VAR_INT.writePrimitive(meta,wild4?6:5);}meta.writeByte(bundles?11:caves?12:village?11:10).writeByte(wild3?8:7).writeBoolean(true).writeByte(bundles?12:caves?13:village?12:11).writeByte(wild3?8:7).writeBoolean(false).writeByte(255);receive(meta);data=event(2);
        try{require(Types.VAR_INT.readPrimitive(data)==80,"Boat metadata entity");List<com.viaversion.viaversion.api.minecraft.entitydata.EntityData> list=Types.ENTITY_DATA_LIST1_12.read(data);require(bundles?list.get(0).id()==10&&Boolean.TRUE.equals(list.get(0).value()):list.get(0).id()==9&&((Integer)list.get(0).value())==5&&list.get(1).dataType().typeId()==6,"Modern metadata types normalized");}finally{data.release();}
        ByteBuf cooldown=packet(ClientboundPackets1_13.COOLDOWN);if(bundles)Types.STRING.write(cooldown,"minecraft:shield");else Types.VAR_INT.writePrimitive(cooldown,wireItem(Protocol1_12_2To1_13.MAPPINGS.getNewItemId(442<<4)));Types.VAR_INT.writePrimitive(cooldown,40);receive(cooldown);data=event(19);try{require(Types.VAR_INT.readPrimitive(data)==442&&Types.VAR_INT.readPrimitive(data)==40,"Flattened cooldown identity");}finally{data.release();}
        ByteBuf totem=packet(ClientboundPackets1_13.ENTITY_EVENT);totem.writeInt(1).writeByte(35);receive(totem);data=event(8);data.release();require(client.readInbound()==null,"No duplicate Totem fallback");
        ByteBuf swing=packet(ClientboundPackets1_13.ANIMATE);Types.VAR_INT.writePrimitive(swing,1);swing.writeByte(3);receive(swing);data=event(27);data.release();require(client.readInbound()==null,"No duplicate offhand swing");
        for(int[] ids:new int[][]{{8,42},{16,43},{40,45},{41,47},{38,48}}) {
            ByteBuf particles=packet(ClientboundPackets1_13.LEVEL_PARTICLES);if(!components){if(wild)Types.VAR_INT.writePrimitive(particles,particle(ids[0]));else particles.writeInt(particle(ids[0]));}particles.writeBoolean(false);if(winter)particles.writeBoolean(true);for(int i=0;i<7;i++){if(bee&&i<3)particles.writeDouble(8);else particles.writeFloat(i<3?8:0);}particles.writeInt(3);if(components)(sulfur?VersionedTypes.V26_2.particle():year26?VersionedTypes.V26_1.particle():mounts?VersionedTypes.V1_21_11.particle():copper?VersionedTypes.V1_21_9.particle():summer?VersionedTypes.V1_21_6.particle():spring?VersionedTypes.V1_21_5.particle():winter?VersionedTypes.V1_21_4.particle():bundles?VersionedTypes.V1_21_2.particle():tricky?VersionedTypes.V1_21.particle():VersionedTypes.V1_20_5.particle()).write(particles,targetParticle(ids[0]));receive(particles);data=event(9);
            try{require(data.readInt()==ids[1],"Flattened particle registry "+ids[0]);}finally{data.release();}require(client.readInbound()==null,"No duplicate particle replacement");
        }
    }
    private com.viaversion.viaversion.api.minecraft.Particle targetParticle(int oldId) {
        com.viaversion.viaversion.api.minecraft.Particle result=new com.viaversion.viaversion.api.minecraft.Particle(particle(oldId));
        // The 1.21.9 dragon-breath particle added a float power argument.
        if(copper&&oldId==8)result.add(Types.FLOAT,1F);
        return result;
    }
    private void hands()throws Exception {
        for(int hand=0;hand<=1;hand++) {
            net.minecraft.network.play.client.C17PacketCustomPayload packet=com.viaversion.viaforge.hands.HandPackets.wrapped(new net.minecraft.network.play.client.C08PacketPlayerBlockPlacement((net.minecraft.item.ItemStack)null),hand);
            ByteBuf source=BlockPipelineSmokeTest.packet(0x17);packet.writePacketData(new PacketBuffer(source));send(source);
            ByteBuf data=BlockPipelineSmokeTest.take(server,outgoing(ServerboundPackets1_13.USE_ITEM));try{require(Types.VAR_INT.readPrimitive(data)==hand,"Target hand-aware use");if(wild)require(Types.VAR_INT.readPrimitive(data)==0,"Use-item sequence");if(tricky)require(data.readFloat()==0&&data.readFloat()==0,"1.21 use yaw and pitch");require(!data.isReadable(),"Use-item consumed");}finally{data.release();}
            packet=com.viaversion.viaforge.hands.HandPackets.wrapped(new net.minecraft.network.play.client.C08PacketPlayerBlockPlacement(new BlockPos(1,64,2),1,null,.25F,.5F,.75F),hand);
            source=BlockPipelineSmokeTest.packet(0x17);packet.writePacketData(new PacketBuffer(source));send(source);data=BlockPipelineSmokeTest.take(server,outgoing(ServerboundPackets1_13.USE_ITEM_ON));
            try{if(village)require(Types.VAR_INT.readPrimitive(data)==hand,"1.14 hand precedes block position");
                BlockPosition pos=positionType().read(data);require(pos.x()==1&&pos.y()==64&&pos.z()==2&&Types.VAR_INT.readPrimitive(data)==1,"Target block coordinates/face");
                if(!village)require(Types.VAR_INT.readPrimitive(data)==hand,"Target block hand");
                require(data.readFloat()==.25F&&data.readFloat()==.5F&&data.readFloat()==.75F,"Target block cursor");
                if(village)require(!data.readBoolean(),"1.14 inside-block flag");if(bundles)require(!data.readBoolean(),"World border flag");if(wild)require(Types.VAR_INT.readPrimitive(data)==0,"Block-use sequence");require(!data.isReadable(),"Target block use consumed");}finally{data.release();}
        }
        for(int selectedHand=0;selectedHand<2;selectedHand++) {
            ByteBuf encoded=client.alloc().buffer();Types.VAR_INT.writePrimitive(encoded,100);Types.VAR_INT.writePrimitive(encoded,2);encoded.writeFloat(.25F).writeFloat(.5F).writeFloat(-.75F);
            net.minecraft.network.play.client.C02PacketUseEntity nativeInteract=new net.minecraft.network.play.client.C02PacketUseEntity();
            try{nativeInteract.readPacketData(new PacketBuffer(encoded));}finally{encoded.release();}
            ByteBuf interact=BlockPipelineSmokeTest.packet(0x17);com.viaversion.viaforge.hands.HandPackets.wrapped(nativeInteract,selectedHand).writePacketData(new PacketBuffer(interact));send(interact);
            ByteBuf translated=BlockPipelineSmokeTest.take(server,outgoing(ServerboundPackets1_13.INTERACT));
            try {
                require(Types.VAR_INT.readPrimitive(translated)==100,"Interaction entity identity");
                if(year26) {
                    require(Types.VAR_INT.readPrimitive(translated)==selectedHand,"26.1 interaction hand");
                    com.viaversion.viaversion.api.minecraft.Vector3d hit=Types.LOW_PRECISION_VECTOR.read(translated);
                    require(Math.abs(hit.x()-.25)<.0001&&Math.abs(hit.y()-.5)<.0001&&Math.abs(hit.z()+.75)<.0001,"26.1 quantized interaction coordinates");
                } else {
                    require(Types.VAR_INT.readPrimitive(translated)==2&&translated.readFloat()==.25F&&translated.readFloat()==.5F&&translated.readFloat()==-.75F,"Target interact-at coordinates");
                    require(Types.VAR_INT.readPrimitive(translated)==selectedHand,"Target interaction hand");
                }
                if(nether)require(!translated.readBoolean(),"Secondary interaction action");require(!translated.isReadable(),"Interaction target packet fully consumed");
            }finally{translated.release();}
        }
        ByteBuf attack=BlockPipelineSmokeTest.packet(2);Types.VAR_INT.writePrimitive(attack,100);Types.VAR_INT.writePrimitive(attack,1);send(attack);
        ByteBuf attackData=BlockPipelineSmokeTest.take(server,year26?ServerboundPackets26_1.ATTACK.getId():outgoing(ServerboundPackets1_13.INTERACT));
        try{require(Types.VAR_INT.readPrimitive(attackData)==100,"Attack entity identity");if(!year26){require(Types.VAR_INT.readPrimitive(attackData)==1,"Attack action");if(nether)attackData.readBoolean();}require(!attackData.isReadable(),"Attack packet fully consumed");}finally{attackData.release();}
        ByteBuf source=BlockPipelineSmokeTest.packet(0x17);com.viaversion.viaforge.hands.HandPackets.action(6).writePacketData(new PacketBuffer(source));send(source);
        ByteBuf data=BlockPipelineSmokeTest.take(server,outgoing(ServerboundPackets1_13.PLAYER_ACTION));try{require(Types.VAR_INT.readPrimitive(data)==6,"Swap target action");positionType().read(data);data.readByte();if(wild)require(Types.VAR_INT.readPrimitive(data)==0,"Swap sequence");require(!data.isReadable(),"Swap target layout");}finally{data.release();}
    }
    private void editors()throws Exception {
        net.minecraft.nbt.NBTTagCompound tag=new net.minecraft.nbt.NBTTagCompound();
        tag.setString("name","viaforge:smoke");tag.setString("mode","LOAD");tag.setString("metadata","data");
        tag.setInteger("posX",-3);tag.setInteger("posY",2);tag.setInteger("posZ",7);tag.setInteger("sizeX",8);tag.setInteger("sizeY",9);tag.setInteger("sizeZ",10);
        tag.setBoolean("ignoreEntities",true);tag.setBoolean("showboundingbox",true);tag.setFloat("integrity",.75F);tag.setLong("seed",-12345L);
        String[] mirrors={"NONE","LEFT_RIGHT","FRONT_BACK"},rotations={"NONE","CLOCKWISE_90","CLOCKWISE_180","COUNTERCLOCKWISE_90"};
        for(int mirror=0;mirror<3;mirror++)for(int rotation=0;rotation<4;rotation++) {
            tag.setString("mirror",mirrors[mirror]);tag.setString("rotation",rotations[rotation]);
            ByteBuf source=BlockPipelineSmokeTest.packet(0x17);
            com.viaversion.viaforge.blocks.gui.BlockEditorPackets.structure(new BlockPos(-17,64,35),2,tag).writePacketData(new PacketBuffer(source));send(source);
            ByteBuf data=BlockPipelineSmokeTest.take(server,outgoing(ServerboundPackets1_13.SET_STRUCTURE_BLOCK));
            try {
                BlockPosition pos=positionType().read(data);require(pos.x()==-17&&pos.y()==64&&pos.z()==35,"Structure target position");
                require(Types.VAR_INT.readPrimitive(data)==1&&Types.VAR_INT.readPrimitive(data)==1&&"viaforge:smoke".equals(Types.STRING.read(data)),"Structure action/mode/name");
                for(int value:new int[]{-3,2,7,8,9,10})require(data.readByte()==value,"Structure bounds");
                require(Types.VAR_INT.readPrimitive(data)==mirror&&Types.VAR_INT.readPrimitive(data)==rotation,"Structure mirror/rotation fields survive conversion");
                require("data".equals(Types.STRING.read(data))&&data.readFloat()==.75F&&Types.VAR_LONG.readPrimitive(data)==-12345L&&data.readByte()==5&&!data.isReadable(),"Structure NBT/flags/seed");
            }finally{data.release();}
        }
    }
    private void resources(Path report)throws Exception {
        Minecraft mc=Minecraft.getMinecraft();Map<String,byte[]> original=BlockAssetCache.readAssets(mc.mcDataDir.toPath().resolve("ViaForge/block-assets/"+target.resources().version()+"-client.jar"));
        Map<String,byte[]> normalized=target.resources().normalize(original);
        RenderResourceSmokeTest.verify(target,original,normalized,report);
        original=com.viaversion.viaforge.blocks.resources.PngTextureConverter.normalize(original);
        if(bee)ChestResourceSmokeTest.verify(original,normalized);if(config)GuiSpriteSmokeTest.verify(original,normalized);
        if(sulfur)SignResourceSmokeTest.verify(original,normalized);
        if(year26) {
            for(String color:new String[]{"brown","creamy","gray","white"})require(Arrays.equals(original.get("textures/entity/llama/llama_"+color+".png"),normalized.get("textures/entity/llama/"+color+".png")),"26.1 original llama texture "+color);
            require(Arrays.equals(original.get("textures/entity/snow_golem/snow_golem.png"),normalized.get("textures/entity/snow_golem.png")),"26.1 original snow golem texture");
        }
        if(sulfur)for(String block:new String[]{"purpur_pillar","quartz_pillar"})require(Arrays.equals(original.get("textures/block/"+block+"_side.png"),normalized.get("textures/blocks/"+block+".png")),"26.2 original pillar sides "+block);
        if(spring) {
            for(com.google.gson.JsonElement entry:ItemVariants.EGGS) {
                com.google.gson.JsonObject egg=entry.getAsJsonObject();String name=egg.get("name").getAsString(),modern=SpawnEggNames.MODERN.get(name);
                net.minecraft.item.ItemStack stack=ItemVariants.egg(new net.minecraft.item.ItemStack(ClientItems.item(LegacyItemCatalog.ITEMS.stream().filter(item->item.kind==LegacyItemCatalog.Kind.EGG).findFirst().get())),egg);
                IBakedModel model=mc.getRenderItem().getItemModelMesher().getItemModel(stack);model(model,"original egg "+name);
                require(stack.getItem().getColorFromItemStack(stack,0)==0xffffff,"Original egg pixels are not tinted again");
                require(Arrays.equals(original.get("textures/item/"+modern+"_spawn_egg.png"),normalized.get("textures/items/"+modern+"_spawn_egg.png")),"Original egg texture "+name);
                require(model.getParticleTexture().getIconName().contains(modern+"_spawn_egg"),"Species-specific egg model "+name);
            }
        }
        if(bundles) {
            for(String material:new String[]{"leather","leather_overlay","chainmail","iron","gold","diamond"})for(int layer=1;layer<=2;layer++) {
                String name=material.endsWith("_overlay")?material.substring(0,material.length()-8)+"_layer_"+layer+"_overlay":material+"_layer_"+layer;
                byte[] expected=original.get("textures/entity/equipment/"+(layer==1?"humanoid/":"humanoid_leggings/")+material+".png");
                require(expected!=null&&Arrays.equals(expected,normalized.get("textures/models/armor/"+name+".png")),"Original armor layer "+name);
            }
            require(Arrays.equals(original.get("textures/entity/equipment/wings/elytra.png"),normalized.get("textures/entity/elytra.png")),"Original equipment Elytra");
            for(String color:new String[]{"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"})require(Arrays.equals(original.get("textures/entity/equipment/llama_body/"+color+".png"),normalized.get("textures/entity/llama/decor/"+color+".png")),"Original llama carpet "+color);
        }
        java.awt.image.BufferedImage atlas=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(normalized.get("textures/particle/particles.png")));
        require(atlas.getWidth()==128&&atlas.getHeight()==128,"Native 16-column particle atlas");
        if(!village) {
            java.awt.image.BufferedImage sourceAtlas=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(original.get("textures/particle/particles.png")));
            require(sourceAtlas.getWidth()==256,"Original 1.13 particle atlas");
            for(int y=0;y<128;y++)for(int x=0;x<128;x++)require(atlas.getRGB(x,y)==sourceAtlas.getRGB(x,y),"Original particle texel "+x+","+y);
        }else {
            for(int i=0;i<8;i++) {
                java.awt.image.BufferedImage sprite=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(original.get("textures/particle/glitter_"+i+".png")));
                for(int y=0;y<8;y++)for(int x=0;x<8;x++)require(atlas.getRGB(i*8+x,88+y)==sprite.getRGB(x,y),"Original Totem/end-rod sprite "+i);
            }
            for(String sheet:new String[]{"sweep","explosion"}) {
                java.awt.image.BufferedImage converted=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(normalized.get("textures/entity/"+sheet+".png")));
                boolean sweep=sheet.equals("sweep");int height=sweep?16:32;
                for(int i=0;i<(sweep?8:16);i++) {
                    java.awt.image.BufferedImage sprite=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(original.get("textures/particle/"+sheet+"_"+i+".png")));
                    for(int y=0;y<height;y++)for(int x=0;x<32;x++)require(converted.getRGB((i%4)*32+x,(i/4)*height+y)==sprite.getRGB(x,y+(sweep?8:0)),"Original "+sheet+" frame "+i);
                }
            }
        }
        for(String[] pair:new String[][]{{"textures/blocks/stone.png","textures/block/stone.png"},{"textures/blocks/log_oak.png","textures/block/oak_log.png"},{"textures/blocks/observer_back_lit.png","textures/block/observer_back_on.png"},{"textures/entity/bed/silver.png","textures/entity/bed/light_gray.png"}})
            if(!sulfur || !pair[0].contains("/bed/"))require(original.get(pair[1])!=null&&Arrays.equals(normalized.get(pair[0]),original.get(pair[1])),"Original target texture bytes "+pair[0]);
        for(LegacyBlockCatalog.Definition block:LegacyBlockCatalog.BLOCKS) {
            for(int meta=0;meta<16;meta++)if(block.acceptsMetadata(meta)) {
                net.minecraft.block.state.IBlockState state=Block.BLOCK_STATE_IDS.getByValue(ClientBlocks.localState(block.stateId(meta)));
                for(net.minecraft.block.state.IBlockState variant:state.getBlock().getBlockState().getValidStates())model(mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(variant),"block "+block.name);
            }
        }
        List<LegacyItemDefinition> definitions=new ArrayList<>(LegacyBlockCatalog.BLOCKS);definitions.addAll(LegacyItemCatalog.ITEMS);
        for(LegacyItemDefinition item:definitions)if(item.itemId()>=0)model(mc.getRenderItem().getItemModelMesher().getItemModel(new net.minecraft.item.ItemStack(net.minecraft.item.Item.getItemById(ClientItems.localItem(item.itemId(),item.itemData())))),"item "+item.itemId());
        require(mc.getResourceManager().getResource(new net.minecraft.util.ResourceLocation("minecraft:textures/blocks/log_oak.png")).getResourcePackName().equals("ViaForge versioned blocks"),"Native blocks use target texture pack");
    }
    private void lifecycle()throws Exception {
        int[] types={2,8,7};String[] names={"command_block","end_gateway","structure_block"};
        for(int i=0;i<types.length;i++) {
            int state=-1;
            for(Map.Entry<String,Integer> entry:states.entrySet())if(entry.getKey().equals(names[i])||entry.getKey().startsWith(names[i]+"[")){state=entry.getValue();break;}
            require(state>=0,"Original editor block registry");
            ByteBuf update=packet(ClientboundPackets1_13.BLOCK_UPDATE);positionType().write(update,new BlockPosition(2,65,3));Types.VAR_INT.writePrimitive(update,wireState(state));receive(update);drain(client);
            CompoundTag tag=new CompoundTag();tag.putString("id","minecraft:"+names[i]);tag.putString("Command","say retained");tag.putLong("Age",234);tag.putInt("x",2);tag.putInt("y",65);tag.putInt("z",3);
            ByteBuf nbt=packet(ClientboundPackets1_13.BLOCK_ENTITY_DATA);positionType().write(nbt,new BlockPosition(2,65,3));CompoundTag sourceTag=tag.copy();if(cliffs){Types.VAR_INT.writePrimitive(nbt,later(com.viaversion.viaversion.protocols.v1_17_1to1_18.data.BlockEntityMappings1_18.newId(types[i]),MappingData::getBlockEntityMappings));sourceTag.remove("id");sourceTag.remove("x");sourceTag.remove("y");sourceTag.remove("z");}else nbt.writeByte(types[i]);if(config)Types.COMPOUND_TAG.write(nbt,sourceTag);else Types.NAMED_COMPOUND_TAG.write(nbt,sourceTag);receive(nbt);
            ByteBuf retained=take(0x35);try{BlockPosition pos=Types.BLOCK_POSITION1_8.read(retained);require(pos.x()==2&&pos.y()==65&&pos.z()==3&&retained.readUnsignedByte()==types[i]&&tag.equals(Types.NAMED_COMPOUND_TAG.read(retained)),"Original "+names[i]+" NBT");}finally{retained.release();}drain(client);
        }
        require(cached(2,65,3),"Chunk state retained before unload");
        if(wild4)waterUpdate();
        ByteBuf unload=packet(ClientboundPackets1_13.FORGET_LEVEL_CHUNK);unload.writeInt(0).writeInt(0);receive(unload);drain(client);
        require(!cached(2,65,3),"Unloaded original state is discarded");
        require(user.get(FlattenedProtocolAdapter.class).waterColors.at(0,64,0,-1)==-1,"Unloaded water colors discarded");
        // A stray update must not recreate an unloaded column.
        ByteBuf update=packet(ClientboundPackets1_13.BLOCK_UPDATE);positionType().write(update,new BlockPosition(2,65,3));Types.VAR_INT.writePrimitive(update,wireState(states.get("purple_concrete")));receive(update);drain(client);
        require(!cached(2,65,3),"Stray update does not allocate a column");
        if(village) {
            ByteBuf light=lightHeader();
            if(caves)Types.VAR_INT.writePrimitive(light,1);Types.BYTE_ARRAY_PRIMITIVE.write(light,new byte[2048]);if(caves)Types.VAR_INT.writePrimitive(light,1);Types.BYTE_ARRAY_PRIMITIVE.write(light,new byte[2048]);receive(light);drain(client);
            require(user.get(com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage.ChunkLightStorage.class).getStoredLight(0,0)!=null,"Reload populated the Via light cache before dimension change");
        }
        ChunkSection[] sections=new ChunkSection[16];sections[4]=new ChunkSectionImpl(true);sections[4].palette(PaletteType.BLOCKS).addId(wireState(states.get("purple_concrete")));
        sections[4].getLight().setSkyLight(new byte[2048]);sections[4].getLight().setBlockLight(new byte[2048]);
        Chunk chunk=new BaseChunk(0,0,true,false,16,sections,new int[256],new ArrayList<>());ByteBuf reload=packet(ClientboundPackets1_13.LEVEL_CHUNK);
        if(village){chunk.setHeightMap(new CompoundTag());if(bee){chunk.setBiomeData(new int[1024]);writeModernChunk(reload,chunk);}else ChunkType1_14.TYPE.write(reload,chunk);}else new ChunkType1_13(true).write(reload,chunk);receive(reload);drain(client);
        require(cached(2,65,3),"Reloaded chunk has its own states");
        ByteBuf respawn=packet(ClientboundPackets1_13.RESPAWN);
        if(nether){if(components)Types.VAR_INT.writePrimitive(respawn,1);else if(wild)Types.STRING.write(respawn,"minecraft:the_nether");else if(netherPatch)Types.NAMED_COMPOUND_TAG.write(respawn,dimension("minecraft:the_nether"));else Types.STRING.write(respawn,"minecraft:the_nether");Types.STRING.write(respawn,"minecraft:the_nether");respawn.writeLong(12345L).writeByte(1).writeByte(1).writeBoolean(false).writeBoolean(false);if(!config)respawn.writeBoolean(false);if(wild)Types.OPTIONAL_GLOBAL_POSITION.write(respawn,null);if(trails)Types.VAR_INT.writePrimitive(respawn,0);if(bundles)Types.VAR_INT.writePrimitive(respawn,32);if(config)respawn.writeByte(0);}
        else {respawn.writeInt(-1);if(bee)respawn.writeLong(12345L);if(!village)respawn.writeByte(0);respawn.writeByte(1);Types.STRING.write(respawn,"default");}receive(respawn);drain(client);
        require(!cached(2,65,3),"Dimension switch discards source states");
        require(user.get(FlattenedProtocolAdapter.class).waterColors.at(0,64,0,-1)==-1,"Dimension switch discards water colors");
        if(village)require(user.get(com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage.ChunkLightStorage.class).getStoredLight(0,0)==null,"Dimension switch clears Via light cache");
    }
    private void waterUpdate()throws Exception {
        com.viaversion.viaversion.api.protocol.packet.PacketType type=null;
        for(com.viaversion.viaversion.api.protocol.Protocol protocol:user.getProtocolInfo().getPipeline().reversedPipes()) {
            if(!(protocol instanceof com.viaversion.viabackwards.api.BackwardsProtocol))continue;
            com.viaversion.viaversion.api.protocol.packet.provider.PacketTypeMap<?> packets=(com.viaversion.viaversion.api.protocol.packet.provider.PacketTypeMap<?>)protocol.getPacketTypesProvider().unmappedClientboundPacketTypes().get(State.PLAY);
            type=packets.typeByName("CHUNKS_BIOMES");break;
        }
        require(type!=null,"Exact target biome-update packet");
        ByteBuf update=BlockPipelineSmokeTest.packet(type.getId());Types.VAR_INT.writePrimitive(update,2);
        DataPalette[] palettes=new DataPalette[24];
        for(int y=0;y<24;y++){palettes[y]=new DataPaletteImpl(64);palettes[y].addId(1);}
        Type<DataPalette[]> codec=spring?new com.viaversion.viaversion.api.type.types.chunk.ChunkBiomesType1_21_5(24,2):new com.viaversion.viaversion.api.type.types.chunk.ChunkBiomesType1_19_4(24,2);
        for(int x:new int[]{0,123}){Types.CHUNK_POSITION.write(update,new com.viaversion.viaversion.api.minecraft.ChunkPosition(x,0));codec.write(update,palettes);}
        receive(update);drain(client);
        com.viaversion.viaforge.common.compatibility.WaterColors colors=user.get(FlattenedProtocolAdapter.class).waterColors;
        require(colors.at(0,64,0,-1)==(mounts?0x3F76E4:0x234567),"Original biome update survives Via cancellation");
        require(colors.pollUpdate()!=null&&colors.pollUpdate()==null,"Only loaded column needs a renderer refresh");
        require(colors.at(123*16,64,0,-1)==-1,"Stray biome update does not create an unloaded column");
    }
    private boolean cached(int x,int y,int z)throws Exception {
        ByteBuf probe=BlockPipelineSmokeTest.packet(0x23);Types.BLOCK_POSITION1_8.write(probe,new BlockPosition(x,y,z));Types.VAR_INT.writePrimitive(probe,16);
        try {
            ByteBuf result=user.get(FlattenedProtocolAdapter.class).restore(probe);if(result==null)return false;
            try{Types.VAR_INT.readPrimitive(result);Types.BLOCK_POSITION1_8.read(result);return Types.VAR_INT.readPrimitive(result)!=16;}
            finally{result.release();}
        }finally{probe.release();}
    }
    private static void model(IBakedModel model,String label){
        Minecraft mc=Minecraft.getMinecraft();require(model!=mc.getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getMissingModel(),"Imported "+label);
        if(!model.isBuiltInRenderer())BlockClientSmokeTest.checkFaceTextures(model,label);
    }
    private int later(int id,java.util.function.Function<MappingData,Mappings> select) {
        MappingData[] layers={wild3?Protocol1_19_1To1_19_3.MAPPINGS:null,wild4?Protocol1_19_3To1_19_4.MAPPINGS:null,trails?Protocol1_19_4To1_20.MAPPINGS:null,config?Protocol1_20To1_20_2.MAPPINGS:null,trials?Protocol1_20_2To1_20_3.MAPPINGS:null,components?Protocol1_20_3To1_20_5.MAPPINGS:null,tricky?Protocol1_20_5To1_21.MAPPINGS:null,bundles?Protocol1_21To1_21_2.MAPPINGS:null,winter?Protocol1_21_2To1_21_4.MAPPINGS:null,spring?Protocol1_21_4To1_21_5.MAPPINGS:null,summer?Protocol1_21_5To1_21_6.MAPPINGS:null,summerPatch?Protocol1_21_6To1_21_7.MAPPINGS:null,copper?Protocol1_21_7To1_21_9.MAPPINGS:null,mounts?Protocol1_21_9To1_21_11.MAPPINGS:null,year26?Protocol1_21_11To26_1.MAPPINGS:null,sulfur?Protocol26_1To26_2.MAPPINGS:null};
        for(MappingData layer:layers)if(layer!=null){Mappings map=select.apply(layer);if(map!=null)id=map.getNewId(id);}return id;
    }
    private int wireState(int id){int patch=target.serverProtocol()==393?id:Protocol1_13To1_13_1.MAPPINGS.getNewBlockStateId(id);int next=village?Protocol1_13_2To1_14.MAPPINGS.getNewBlockStateId(patch):patch;next=bee?Protocol1_14_4To1_15.MAPPINGS.getNewBlockStateId(next):next;next=nether?Protocol1_15_2To1_16.MAPPINGS.getNewBlockStateId(next):next;next=netherPatch?Protocol1_16_1To1_16_2.MAPPINGS.getNewBlockStateId(next):next;next=caves?Protocol1_16_4To1_17.MAPPINGS.getNewBlockStateId(next):next;next=wild?Protocol1_18_2To1_19.MAPPINGS.getNewBlockStateId(next):next;return later(next,MappingData::getBlockStateMappings);}
    private int wireItem(int id){int patch=target.serverProtocol()==393?id:Protocol1_13To1_13_1.MAPPINGS.getNewItemId(id);int next=village?Protocol1_13_2To1_14.MAPPINGS.getNewItemId(patch):patch;next=bee?Protocol1_14_4To1_15.MAPPINGS.getNewItemId(next):next;next=nether?Protocol1_15_2To1_16.MAPPINGS.getNewItemId(next):next;next=netherPatch?Protocol1_16_1To1_16_2.MAPPINGS.getNewItemId(next):next;next=caves?Protocol1_16_4To1_17.MAPPINGS.getNewItemId(next):next;next=cliffs?Protocol1_17_1To1_18.MAPPINGS.getNewItemId(next):next;next=wild?Protocol1_18_2To1_19.MAPPINGS.getNewItemId(next):next;return later(next,MappingData::getItemMappings);}
    private int particle(int id){int next=village?Protocol1_13_2To1_14.MAPPINGS.getParticleMappings().getNewId(id):id;next=bee?Protocol1_14_4To1_15.MAPPINGS.getParticleMappings().getNewId(next):next;next=nether?Protocol1_15_2To1_16.MAPPINGS.getParticleMappings().getNewId(next):next;next=netherPatch?Protocol1_16_1To1_16_2.MAPPINGS.getParticleMappings().getNewId(next):next;next=caves?Protocol1_16_4To1_17.MAPPINGS.getParticleMappings().getNewId(next):next;next=cliffs?Protocol1_17_1To1_18.MAPPINGS.getParticleMappings().getNewId(next):next;next=wild?Protocol1_18_2To1_19.MAPPINGS.getParticleMappings().getNewId(next):next;return later(next,MappingData::getParticleMappings);}
    private int wireEntity(int id){int next=village?Protocol1_13_2To1_14.MAPPINGS.getEntityMappings().getNewId(id):id;next=bee?Protocol1_14_4To1_15.MAPPINGS.getEntityMappings().getNewId(next):next;next=nether?Protocol1_15_2To1_16.MAPPINGS.getEntityMappings().getNewId(next):next;next=netherPatch?Protocol1_16_1To1_16_2.MAPPINGS.getEntityMappings().getNewId(next):next;next=caves?Protocol1_16_4To1_17.MAPPINGS.getEntityMappings().getNewId(next):next;next=wild?Protocol1_18_2To1_19.MAPPINGS.getEntityMappings().getNewId(next):next;return later(next,MappingData::getEntityMappings);}
    private Type<BlockPosition> positionType(){return village?Types.BLOCK_POSITION1_14:Types.BLOCK_POSITION1_8;}
    private void spawnCoordinates(ByteBuf packet,double x,double y,double z)throws Exception {packet.writeDouble(x).writeDouble(y).writeDouble(z);if(copper)Types.LOW_PRECISION_VECTOR.write(packet,new com.viaversion.viaversion.api.minecraft.Vector3d(0,0,0));}
    private void spawnVelocity(ByteBuf packet){if(!copper)packet.writeShort(0).writeShort(0).writeShort(0);}
    private void objectData(ByteBuf packet,int data){if(wild){packet.writeByte(0);Types.VAR_INT.writePrimitive(packet,data);}else packet.writeInt(data);}
    private void objectType(ByteBuf packet,int old){
        if(bundles&&old==1)Types.VAR_INT.writePrimitive(packet,sulfur?com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_2.DARK_OAK_BOAT.getId():mounts?com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_11.DARK_OAK_BOAT.getId():copper?com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_9.DARK_OAK_BOAT.getId():summer?com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_6.DARK_OAK_BOAT.getId():spring?com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_5.DARK_OAK_BOAT.getId():com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_2.DARK_OAK_BOAT.getId());else if(village)Types.VAR_INT.writePrimitive(packet,wireEntity(com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.ObjectType.findById(old,0).getType().getId()));else packet.writeByte(old);
    }
    private int outgoing(ServerboundPackets1_13 type){return year26?ServerboundPackets26_1.valueOf(type.name()).getId():summer?ServerboundPackets1_21_6.valueOf(type.name()).getId():spring?ServerboundPackets1_21_5.valueOf(type.name()).getId():winter?ServerboundPackets1_21_4.valueOf(type.name()).getId():bundles?ServerboundPackets1_21_2.valueOf(type.name()).getId():components?ServerboundPackets1_20_5.valueOf(type.name()).getId():trials?com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ServerboundPackets1_20_3.valueOf(type.name()).getId():config?ServerboundPackets1_20_2.valueOf(type.name()).getId():wild4?ServerboundPackets1_19_4.valueOf(type.name()).getId():wild3?ServerboundPackets1_19_3.valueOf(type.name()).getId():wildPatch?ServerboundPackets1_19_1.valueOf(type.name()).getId():wild?ServerboundPackets1_19.valueOf(type.name()).getId():caves?ServerboundPackets1_17.valueOf(type.name()).getId():netherPatch?ServerboundPackets1_16_2.valueOf(type.name()).getId():nether?ServerboundPackets1_16.valueOf(type.name()).getId():village?ServerboundPackets1_14.valueOf(type.name()).getId():type.getId();}
    private ByteBuf packet(ClientboundPackets1_13 type){
        String name=type.name();if(nether&&name.equals("SET_EQUIPPED_ITEM"))name="SET_EQUIPMENT";if(netherPatch&&name.equals("CHUNK_BLOCKS_UPDATE"))name="SECTION_BLOCKS_UPDATE";
        if(cliffs&&name.equals("LEVEL_CHUNK"))name="LEVEL_CHUNK_WITH_LIGHT";
        if(wild&&name.equals("ADD_MOB"))name="ADD_ENTITY";
        if(config&&name.equals("ADD_PLAYER"))name="ADD_ENTITY";
        int id=year26?ClientboundPackets26_1.valueOf(name).getId():mounts?ClientboundPackets1_21_11.valueOf(name).getId():copper?ClientboundPackets1_21_9.valueOf(name).getId():summer?ClientboundPackets1_21_6.valueOf(name).getId():spring?ClientboundPackets1_21_5.valueOf(name).getId():bundles?ClientboundPackets1_21_2.valueOf(name).getId():tricky?ClientboundPackets1_21.valueOf(name).getId():components?ClientboundPackets1_20_5.valueOf(name).getId():trials?ClientboundPackets1_20_3.valueOf(name).getId():config?ClientboundPackets1_20_2.valueOf(name).getId():wild4?ClientboundPackets1_19_4.valueOf(name).getId():wild3?ClientboundPackets1_19_3.valueOf(name).getId():wildPatch?ClientboundPackets1_19_1.valueOf(name).getId():wild?ClientboundPackets1_19.valueOf(name).getId():cliffs?ClientboundPackets1_18.valueOf(name).getId():cavesPatch?ClientboundPackets1_17_1.valueOf(name).getId():caves?ClientboundPackets1_17.valueOf(name).getId():netherPatch?ClientboundPackets1_16_2.valueOf(name).getId():nether?ClientboundPackets1_16.valueOf(name).getId():bee?ClientboundPackets1_15.valueOf(type.name()).getId():target.serverProtocol()==498?ClientboundPackets1_14_4.valueOf(type.name()).getId():village?ClientboundPackets1_14.valueOf(type.name()).getId():type.getId();return BlockPipelineSmokeTest.packet(id);
    }
    private void slotHeader(ByteBuf packet,int window,int slot){if(bundles)Types.VAR_INT.writePrimitive(packet,window);else packet.writeByte(window);if(cavesPatch)Types.VAR_INT.writePrimitive(packet,103);packet.writeShort(slot);}
    private ByteBuf lightHeader()throws Exception {
        int id=year26?ClientboundPackets26_1.LIGHT_UPDATE.getId():mounts?ClientboundPackets1_21_11.LIGHT_UPDATE.getId():copper?ClientboundPackets1_21_9.LIGHT_UPDATE.getId():summer?ClientboundPackets1_21_6.LIGHT_UPDATE.getId():spring?ClientboundPackets1_21_5.LIGHT_UPDATE.getId():bundles?ClientboundPackets1_21_2.LIGHT_UPDATE.getId():tricky?ClientboundPackets1_21.LIGHT_UPDATE.getId():components?ClientboundPackets1_20_5.LIGHT_UPDATE.getId():trials?ClientboundPackets1_20_3.LIGHT_UPDATE.getId():config?ClientboundPackets1_20_2.LIGHT_UPDATE.getId():wild4?ClientboundPackets1_19_4.LIGHT_UPDATE.getId():wild3?ClientboundPackets1_19_3.LIGHT_UPDATE.getId():wildPatch?ClientboundPackets1_19_1.LIGHT_UPDATE.getId():wild?ClientboundPackets1_19.LIGHT_UPDATE.getId():cliffs?ClientboundPackets1_18.LIGHT_UPDATE.getId():cavesPatch?ClientboundPackets1_17_1.LIGHT_UPDATE.getId():caves?ClientboundPackets1_17.LIGHT_UPDATE.getId():netherPatch?ClientboundPackets1_16_2.LIGHT_UPDATE.getId():nether?ClientboundPackets1_16.LIGHT_UPDATE.getId():bee?ClientboundPackets1_15.LIGHT_UPDATE.getId():ClientboundPackets1_14.LIGHT_UPDATE.getId();
        ByteBuf light=BlockPipelineSmokeTest.packet(id);Types.VAR_INT.writePrimitive(light,0);Types.VAR_INT.writePrimitive(light,0);if(nether&&!trails)light.writeBoolean(true);
        for(int value:new int[]{32,32,0,0}){if(caves)Types.LONG_ARRAY_PRIMITIVE.write(light,new long[]{(long)value<<4});else Types.VAR_INT.writePrimitive(light,value);}
        return light;
    }
    private void receive(ByteBuf packet){BlockPipelineSmokeTest.receiveCompressed(client,server,packet);}
    private ByteBuf take(int id){return BlockPipelineSmokeTest.take(client,id);}
    private ByteBuf event(int operation){
        ByteBuf packet;while((packet=client.readInbound())!=null){if(Types.VAR_INT.readPrimitive(packet)==0x3f&&"VF|entity".equals(Types.STRING.read(packet))){ClientEventEnvelope header=ClientEventEnvelope.read(packet);if(header.operation==operation){require(header.format.revision()==340,"Explicit normalized event schema");return packet;}}packet.release();}
        throw new AssertionError("Missing normalized event "+operation+" for "+target.serverProtocol());
    }
    private void send(ByteBuf packet){try{client.writeOutbound(packet);}catch(com.viaversion.viaversion.exception.CancelEncoderException expected){}client.runPendingTasks();ByteBuf data;while((data=client.readOutbound())!=null)server.writeInbound(data);}
    private static void drain(EmbeddedChannel channel){ByteBuf data;while((data=channel.readInbound())!=null)data.release();while((data=channel.readOutbound())!=null)data.release();}
    private void close(){client.close();client.runPendingTasks();client.checkException();drain(client);server.close();drain(server);}
}
