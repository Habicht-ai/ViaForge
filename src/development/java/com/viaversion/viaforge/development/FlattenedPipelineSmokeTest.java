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
    private final Map<String,Integer> states=new HashMap<>();
    private final List<String> stateNames=new ArrayList<>();
    private int joins;
    private FlattenedPipelineSmokeTest(CompatibilityProfile target)throws Exception {
        this.target=target;village=target.serverProtocol()>=477;itemType=target.serverProtocol()>=404?Types.ITEM1_13_2:Types.ITEM1_13;
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
            test.join();int count=test.blocks(world);test.items();test.events();test.hands();test.editors();test.resources(report);test.lifecycle();
            return target.resources().version()+": real flattened chunks ("+count+" inherited states), single/multi updates, bed colors without tile NBT, shulker events, falling blocks; all inherited block/item inventory round trips, Damage/enchantments/banner NBT, split counts, fresh Creative picks; offhand/full/direct/equipment, boat/player/mob metadata, passengers, cooldowns/particles/Totem, both hand directions; original target resource aliases, particle atlas texels and all inherited block/item models; structure mirror/rotation/flags/seed, command/gateway/structure NBT, chunk unload and dimension cleanup"+(test.village?", separate 1.14 sky/block light and JSON Lore":"")+" PASS";
        }finally{test.close();}
    }
    private void compression(){
        if(client.pipeline().get("decompress")!=null)client.pipeline().remove("decompress");
        if(client.pipeline().get("compress")!=null)client.pipeline().remove("compress");
        client.pipeline().addBefore("decoder","decompress",new NettyCompressionDecoder(256));client.pipeline().addBefore("encoder","compress",new NettyCompressionEncoder(256));
        ViaChannelInitializer.reorderPipeline(client.pipeline(),"compress","decompress");client.checkException();
    }
    private void join(){
        ByteBuf join=packet(ClientboundPackets1_13.LOGIN);join.writeInt(1).writeByte(1).writeInt(0);if(!village)join.writeByte(0);join.writeByte(20);Types.STRING.write(join,"default");if(village)Types.VAR_INT.writePrimitive(join,8);join.writeBoolean(false);
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
        ByteBuf source=packet(ClientboundPackets1_13.LEVEL_CHUNK);if(village) {
            ByteBuf light=BlockPipelineSmokeTest.packet(ClientboundPackets1_14.LIGHT_UPDATE.getId());
            for(int value:new int[]{0,0,32,32,0,0})Types.VAR_INT.writePrimitive(light,value);
            byte[] sky=new byte[2048],block=new byte[2048];Arrays.fill(sky,(byte)0xaa);Arrays.fill(block,(byte)0x55);
            Types.BYTE_ARRAY_PRIMITIVE.write(light,sky);Types.BYTE_ARRAY_PRIMITIVE.write(light,block);receive(light);
            require(client.readInbound()==null,"Separate light update is owned by Via");
            chunk.setHeightMap(new CompoundTag());ChunkType1_14.TYPE.write(source,chunk);
        }else new ChunkType1_13(true).write(source,chunk);receive(source);
        S21PacketChunkData nativeChunk=new S21PacketChunkData();ByteBuf data=take(0x21);
        try{nativeChunk.readPacketData(new PacketBuffer(data));}finally{data.release();}
        world.getChunkFromChunkCoords(0,0).fillChunk(nativeChunk.getExtractedDataBytes(),nativeChunk.getExtractedSize(),true);
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
        ByteBuf multi=packet(ClientboundPackets1_13.CHUNK_BLOCKS_UPDATE);multi.writeInt(0).writeInt(0);
        Types.BLOCK_CHANGE_ARRAY.write(multi,new com.viaversion.viaversion.api.minecraft.BlockChangeRecord[]{new com.viaversion.viaversion.api.minecraft.BlockChangeRecord1_8(1,64,1,wireState(states.get("purple_concrete")))});receive(multi);
        data=take(0x22);try{require(data.readInt()==0&&data.readInt()==0,"Multi-block coordinates");require(Types.BLOCK_CHANGE_ARRAY.read(data)[0].getBlockId()==ClientBlocks.localState(251<<4|10),"Flattened multi-block state");}finally{data.release();}
        int powder=states.get("red_concrete_powder");
        ByteBuf falling=packet(ClientboundPackets1_13.ADD_ENTITY);Types.VAR_INT.writePrimitive(falling,77);Types.UUID.write(falling,new UUID(0,77));objectType(falling,70);falling.writeDouble(8).writeDouble(80).writeDouble(8).writeByte(0).writeByte(0).writeInt(wireState(powder)).writeShort(0).writeShort(0).writeShort(0);receive(falling);
        data=take(0x0e);try{Types.VAR_INT.readPrimitive(data);data.skipBytes(15);int raw=data.readInt();require(((raw&4095)<<4|(raw>>12&15))==ClientBlocks.localState(252<<4|14),"Flattened falling powder");}finally{data.release();}
        ByteBuf event=packet(ClientboundPackets1_13.BLOCK_EVENT);positionType().write(event,new BlockPosition(0,64,0));event.writeByte(1).writeByte(1);Types.VAR_INT.writePrimitive(event,village?Protocol1_13_2To1_14.MAPPINGS.getNewBlockId(483):483);receive(event);
        data=take(0x24);try{Types.BLOCK_POSITION1_8.read(data);data.skipBytes(2);require(Types.VAR_INT.readPrimitive(data)==ClientBlocks.localState(219<<4)>>4,"Flattened shulker block event");}finally{data.release();}
        int unsupported=-1;
        if(village) {
            com.viaversion.viaversion.api.data.Mappings inverse=Protocol1_13_2To1_14.MAPPINGS.getBlockStateMappings().inverse();
            for(int id=0;id<inverse.size();id++)if(inverse.getNewId(id)<0){unsupported=id;break;}
        }else for(int id=0;id<stateNames.size();id++)if(mapping.state(id)==65535){unsupported=wireState(id);break;}
        require(unsupported>=0,"Actual target contains a state outside the inherited catalog");
        for(int next:new int[]{wireState(states.get("purpur_block")),unsupported}) {
            ByteBuf update=packet(ClientboundPackets1_13.BLOCK_UPDATE);positionType().write(update,new BlockPosition(0,64,0));Types.VAR_INT.writePrimitive(update,next);receive(update);
            data=take(0x23);data.release();
            require(cached(0,64,0)==(next!=unsupported),"Unsupported target update invalidates previous Purpur state");
        }
        drain(client);return expected.size();
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
                Item returned=sendItem(local,creative);require(returned.identifier()==original.identifier(),"Target item ID "+definition.itemId());
                require(returned.amount()==original.amount(),"Target amount");
                require(Objects.equals(returned.tag(),original.tag()),"Target NBT "+definition.itemId()+": "+original.tag()+" -> "+returned.tag());
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
        Item original=new DataItem(shield,(byte)1,(short)0,tag);Item local=slot(original,36);
        require(local.data()==57&&local.tag().getListTag("ench",CompoundTag.class).get(0).getShort("id")==70,"Direct modern Damage and namespaced enchantment");
        Item result=sendItem(local,false);require(tag.equals(result.tag()),"Exact original shield JSON/banner/enchantment NBT: "+result.tag());
        ByteBuf off=packet(ClientboundPackets1_13.CONTAINER_SET_SLOT);off.writeByte(0).writeShort(45);itemType.write(off,original);receive(off);ByteBuf event=event(26);
        try{Item held=Types.ITEM1_8.read(event);require(held.identifier()==442&&held.data()==57,"Original offhand shield after flattened codec");require(ClientItems.is(ServerEntityViews.item(held),LegacyItemCatalog.Kind.SHIELD),"Native shield implementation selected");}finally{event.release();}
        ByteBuf full=packet(ClientboundPackets1_13.CONTAINER_SET_CONTENT);full.writeByte(0).writeShort(46);
        for(int i=0;i<46;i++)itemType.write(full,i==45?original.copy():null);receive(full);event=event(26);
        try{require(Types.ITEM1_8.read(event).identifier()==442,"Full flattened inventory includes shield in offhand");}finally{event.release();}
        ByteBuf direct=packet(ClientboundPackets1_13.CONTAINER_SET_SLOT);direct.writeByte(-2).writeShort(40);itemType.write(direct,original.copy());receive(direct);event=event(26);
        try{require(Types.ITEM1_8.read(event).data()==57,"Direct offhand index retains Damage");}finally{event.release();}
        ByteBuf equipment=packet(ClientboundPackets1_13.SET_EQUIPPED_ITEM);Types.VAR_INT.writePrimitive(equipment,1);Types.VAR_INT.writePrimitive(equipment,1);itemType.write(equipment,original.copy());receive(equipment);event=event(3);
        try{require(Types.VAR_INT.readPrimitive(event)==1&&Types.VAR_INT.readPrimitive(event)==1&&Types.ITEM1_8.read(event).identifier()==442,"Offhand equipment identity");}finally{event.release();}
        // Count changes must not be undone by snapshots.
        Item fruit=slot(new DataItem(wireItem(Protocol1_12_2To1_13.MAPPINGS.getNewItemId(432<<4)),(byte)5,(short)0,null),36);fruit.setAmount(2);require(sendItem(fruit,false).amount()==2,"Split inventory count");
        drain(client);
    }
    private Item slot(Item item,int slot)throws Exception {
        ByteBuf source=packet(ClientboundPackets1_13.CONTAINER_SET_SLOT);source.writeByte(0).writeShort(slot);itemType.write(source,item.copy());receive(source);ByteBuf data=take(0x2f);
        try{S2FPacketSetSlot nativeSlot=new S2FPacketSetSlot();nativeSlot.readPacketData(new PacketBuffer(data.duplicate()));require(nativeSlot.func_149174_e()!=null,"Native item decoding");data.skipBytes(3);return Types.ITEM1_8.read(data);}finally{data.release();}
    }
    private Item sendItem(Item item,boolean creative)throws Exception {
        ByteBuf source=BlockPipelineSmokeTest.packet(creative?0x10:0x0e);
        if(creative)source.writeShort(36);else source.writeByte(0).writeShort(36).writeByte(0).writeShort(17).writeByte(0);
        Types.ITEM1_8.write(source,item.copy());send(source);
        ByteBuf data=BlockPipelineSmokeTest.take(server,outgoing(creative?ServerboundPackets1_13.SET_CREATIVE_MODE_SLOT:ServerboundPackets1_13.CONTAINER_CLICK));
        try{if(creative)data.readShort();else{require(data.readByte()==0&&data.readShort()==36&&data.readByte()==0&&data.readShort()==17&&Types.VAR_INT.readPrimitive(data)==0,"Target click layout");}Item result=itemType.read(data);require(!data.isReadable(),"Target item codec fully consumed");return result;}finally{data.release();}
    }
    private void events()throws Exception {
        ByteBuf player=packet(ClientboundPackets1_13.ADD_PLAYER);Types.VAR_INT.writePrimitive(player,2);Types.UUID.write(player,new UUID(0,2));
        player.writeDouble(1).writeDouble(65).writeDouble(2).writeByte(0).writeByte(0).writeByte(255);receive(player);event(5).release();
        ByteBuf hand=packet(ClientboundPackets1_13.SET_ENTITY_DATA);Types.VAR_INT.writePrimitive(hand,2);
        hand.writeByte(village?7:6).writeByte(0).writeByte(3).writeByte(255);receive(hand);ByteBuf handData=event(2);
        try{require(Types.VAR_INT.readPrimitive(handData)==2,"Remote player metadata");List<com.viaversion.viaversion.api.minecraft.entitydata.EntityData> entries=Types.ENTITY_DATA_LIST1_12.read(handData);require(entries.size()==1&&entries.get(0).id()==6&&((Byte)entries.get(0).value())==3,"Original player offhand-use flags normalized after pose removal");}finally{handData.release();}drain(client);
        com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.EntityType[] mobs={com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.EntityType.SHULKER,com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.EntityType.POLAR_BEAR,com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.EntityType.LLAMA,com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.EntityType.PARROT};
        int[] oldIds={69,102,103,105};
        for(int i=0;i<mobs.length;i++) {
            ByteBuf mob=packet(ClientboundPackets1_13.ADD_MOB);Types.VAR_INT.writePrimitive(mob,100+i);Types.UUID.write(mob,new UUID(0,100+i));Types.VAR_INT.writePrimitive(mob,village?Protocol1_13_2To1_14.MAPPINGS.getEntityMappings().getNewId(mobs[i].getId()):mobs[i].getId());
            mob.writeDouble(2).writeDouble(65).writeDouble(2).writeByte(0).writeByte(0).writeByte(0).writeShort(0).writeShort(0).writeShort(0).writeByte(255);receive(mob);
            ByteBuf converted=event(11);try{Types.VAR_INT.readPrimitive(converted);Types.UUID.read(converted);require(Types.VAR_INT.readPrimitive(converted)==oldIds[i],"Flattened mob registry "+mobs[i]);}finally{converted.release();}
        }
        ByteBuf cloud=packet(ClientboundPackets1_13.ADD_ENTITY);Types.VAR_INT.writePrimitive(cloud,79);Types.UUID.write(cloud,new UUID(0,79));objectType(cloud,3);cloud.writeDouble(1).writeDouble(65).writeDouble(2).writeByte(0).writeByte(0).writeInt(0).writeShort(0).writeShort(0).writeShort(0);receive(cloud);event(1).release();
        ByteBuf cloudData=packet(ClientboundPackets1_13.SET_ENTITY_DATA);Types.VAR_INT.writePrimitive(cloudData,79);cloudData.writeByte(village?10:9).writeByte(15);Types.VAR_INT.writePrimitive(cloudData,particle(17));cloudData.writeByte(255);receive(cloudData);
        ByteBuf cloudEvent=event(2);try{Types.VAR_INT.readPrimitive(cloudEvent);List<com.viaversion.viaversion.api.minecraft.entitydata.EntityData> metadata=Types.ENTITY_DATA_LIST1_12.read(cloudEvent);require(metadata.size()==3&&((Integer)metadata.get(0).value())==15,"Cloud particle survives Via's cancelled metadata filter");}finally{cloudEvent.release();}drain(client);
        ByteBuf boat=packet(ClientboundPackets1_13.ADD_ENTITY);Types.VAR_INT.writePrimitive(boat,80);Types.UUID.write(boat,new UUID(0,80));objectType(boat,1);boat.writeDouble(1.5).writeDouble(65).writeDouble(2.5).writeByte(0).writeByte(0).writeInt(0).writeShort(0).writeShort(0).writeShort(0);receive(boat);
        ByteBuf data=event(20);try{require(Types.VAR_INT.readPrimitive(data)==80,"Boat retained");}finally{data.release();}
        ByteBuf passengers=packet(ClientboundPackets1_13.SET_PASSENGERS);Types.VAR_INT.writePrimitive(passengers,80);Types.VAR_INT_ARRAY_PRIMITIVE.write(passengers,new int[]{1,2});receive(passengers);data=event(13);try{require(Types.VAR_INT.readPrimitive(data)==80&&Types.VAR_INT_ARRAY_PRIMITIVE.read(data).length==2,"Both boat passengers");}finally{data.release();}
        ByteBuf meta=packet(ClientboundPackets1_13.SET_ENTITY_DATA);Types.VAR_INT.writePrimitive(meta,80);meta.writeByte(village?10:9).writeByte(1);Types.VAR_INT.writePrimitive(meta,5);meta.writeByte(village?11:10).writeByte(7).writeBoolean(true).writeByte(village?12:11).writeByte(7).writeBoolean(false).writeByte(255);receive(meta);data=event(2);
        try{require(Types.VAR_INT.readPrimitive(data)==80,"Boat metadata entity");List<com.viaversion.viaversion.api.minecraft.entitydata.EntityData> list=Types.ENTITY_DATA_LIST1_12.read(data);require(list.get(0).id()==9&&((Integer)list.get(0).value())==5&&list.get(1).dataType().typeId()==6,"Modern metadata types normalized");}finally{data.release();}
        ByteBuf cooldown=packet(ClientboundPackets1_13.COOLDOWN);Types.VAR_INT.writePrimitive(cooldown,wireItem(Protocol1_12_2To1_13.MAPPINGS.getNewItemId(442<<4)));Types.VAR_INT.writePrimitive(cooldown,40);receive(cooldown);data=event(19);try{require(Types.VAR_INT.readPrimitive(data)==442&&Types.VAR_INT.readPrimitive(data)==40,"Flattened cooldown identity");}finally{data.release();}
        ByteBuf totem=packet(ClientboundPackets1_13.ENTITY_EVENT);totem.writeInt(1).writeByte(35);receive(totem);data=event(8);data.release();require(client.readInbound()==null,"No duplicate Totem fallback");
        ByteBuf swing=packet(ClientboundPackets1_13.ANIMATE);Types.VAR_INT.writePrimitive(swing,1);swing.writeByte(3);receive(swing);data=event(27);data.release();require(client.readInbound()==null,"No duplicate offhand swing");
        for(int[] ids:new int[][]{{8,42},{16,43},{40,45},{41,47},{38,48}}) {
            ByteBuf particles=packet(ClientboundPackets1_13.LEVEL_PARTICLES);particles.writeInt(particle(ids[0])).writeBoolean(false);for(int i=0;i<7;i++)particles.writeFloat(i<3?8:0);particles.writeInt(3);receive(particles);data=event(9);
            try{require(data.readInt()==ids[1],"Flattened particle registry "+ids[0]);}finally{data.release();}require(client.readInbound()==null,"No duplicate particle replacement");
        }
    }
    private void hands()throws Exception {
        for(int hand=0;hand<=1;hand++) {
            net.minecraft.network.play.client.C17PacketCustomPayload packet=com.viaversion.viaforge.hands.HandPackets.wrapped(new net.minecraft.network.play.client.C08PacketPlayerBlockPlacement((net.minecraft.item.ItemStack)null),hand);
            ByteBuf source=BlockPipelineSmokeTest.packet(0x17);packet.writePacketData(new PacketBuffer(source));send(source);
            ByteBuf data=BlockPipelineSmokeTest.take(server,outgoing(ServerboundPackets1_13.USE_ITEM));try{require(Types.VAR_INT.readPrimitive(data)==hand&&!data.isReadable(),"Target hand-aware use");}finally{data.release();}
            packet=com.viaversion.viaforge.hands.HandPackets.wrapped(new net.minecraft.network.play.client.C08PacketPlayerBlockPlacement(new BlockPos(1,64,2),1,null,.25F,.5F,.75F),hand);
            source=BlockPipelineSmokeTest.packet(0x17);packet.writePacketData(new PacketBuffer(source));send(source);data=BlockPipelineSmokeTest.take(server,outgoing(ServerboundPackets1_13.USE_ITEM_ON));
            try{if(village)require(Types.VAR_INT.readPrimitive(data)==hand,"1.14 hand precedes block position");
                BlockPosition pos=positionType().read(data);require(pos.x()==1&&pos.y()==64&&pos.z()==2&&Types.VAR_INT.readPrimitive(data)==1,"Target block coordinates/face");
                if(!village)require(Types.VAR_INT.readPrimitive(data)==hand,"Target block hand");
                require(data.readFloat()==.25F&&data.readFloat()==.5F&&data.readFloat()==.75F,"Target block cursor");
                if(village)require(!data.readBoolean(),"1.14 inside-block flag");require(!data.isReadable(),"Target block use consumed");}finally{data.release();}
        }
        ByteBuf source=BlockPipelineSmokeTest.packet(0x17);com.viaversion.viaforge.hands.HandPackets.action(6).writePacketData(new PacketBuffer(source));send(source);
        ByteBuf data=BlockPipelineSmokeTest.take(server,outgoing(ServerboundPackets1_13.PLAYER_ACTION));try{require(Types.VAR_INT.readPrimitive(data)==6,"Swap target action");positionType().read(data);data.readByte();require(!data.isReadable(),"Swap target layout");}finally{data.release();}
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
            require(Arrays.equals(normalized.get(pair[0]),original.get(pair[1])),"Original target texture bytes "+pair[0]);
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
            ByteBuf nbt=packet(ClientboundPackets1_13.BLOCK_ENTITY_DATA);positionType().write(nbt,new BlockPosition(2,65,3));nbt.writeByte(types[i]);Types.NAMED_COMPOUND_TAG.write(nbt,tag);receive(nbt);
            ByteBuf retained=take(0x35);try{BlockPosition pos=Types.BLOCK_POSITION1_8.read(retained);require(pos.x()==2&&pos.y()==65&&pos.z()==3&&retained.readUnsignedByte()==types[i]&&tag.equals(Types.NAMED_COMPOUND_TAG.read(retained)),"Original "+names[i]+" NBT");}finally{retained.release();}drain(client);
        }
        require(cached(2,65,3),"Chunk state retained before unload");
        ByteBuf unload=packet(ClientboundPackets1_13.FORGET_LEVEL_CHUNK);unload.writeInt(0).writeInt(0);receive(unload);drain(client);
        require(!cached(2,65,3),"Unloaded original state is discarded");
        // A stray update must not recreate an unloaded column.
        ByteBuf update=packet(ClientboundPackets1_13.BLOCK_UPDATE);positionType().write(update,new BlockPosition(2,65,3));Types.VAR_INT.writePrimitive(update,wireState(states.get("purple_concrete")));receive(update);drain(client);
        require(!cached(2,65,3),"Stray update does not allocate a column");
        if(village) {
            ByteBuf light=BlockPipelineSmokeTest.packet(ClientboundPackets1_14.LIGHT_UPDATE.getId());
            for(int value:new int[]{0,0,32,32,0,0})Types.VAR_INT.writePrimitive(light,value);
            Types.BYTE_ARRAY_PRIMITIVE.write(light,new byte[2048]);Types.BYTE_ARRAY_PRIMITIVE.write(light,new byte[2048]);receive(light);drain(client);
            require(user.get(com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage.ChunkLightStorage.class).getStoredLight(0,0)!=null,"Reload populated the Via light cache before dimension change");
        }
        ChunkSection[] sections=new ChunkSection[16];sections[4]=new ChunkSectionImpl(true);sections[4].palette(PaletteType.BLOCKS).addId(wireState(states.get("purple_concrete")));
        sections[4].getLight().setSkyLight(new byte[2048]);sections[4].getLight().setBlockLight(new byte[2048]);
        Chunk chunk=new BaseChunk(0,0,true,false,16,sections,new int[256],new ArrayList<>());ByteBuf reload=packet(ClientboundPackets1_13.LEVEL_CHUNK);
        if(village){chunk.setHeightMap(new CompoundTag());ChunkType1_14.TYPE.write(reload,chunk);}else new ChunkType1_13(true).write(reload,chunk);receive(reload);drain(client);
        require(cached(2,65,3),"Reloaded chunk has its own states");
        ByteBuf respawn=packet(ClientboundPackets1_13.RESPAWN);respawn.writeInt(-1);if(!village)respawn.writeByte(0);respawn.writeByte(1);Types.STRING.write(respawn,"default");receive(respawn);drain(client);
        require(!cached(2,65,3),"Dimension switch discards source states");
        if(village)require(user.get(com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage.ChunkLightStorage.class).getStoredLight(0,0)==null,"Dimension switch clears Via light cache");
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
    private int wireState(int id){int patch=target.serverProtocol()==393?id:Protocol1_13To1_13_1.MAPPINGS.getNewBlockStateId(id);return village?Protocol1_13_2To1_14.MAPPINGS.getNewBlockStateId(patch):patch;}
    private int wireItem(int id){int patch=target.serverProtocol()==393?id:Protocol1_13To1_13_1.MAPPINGS.getNewItemId(id);return village?Protocol1_13_2To1_14.MAPPINGS.getNewItemId(patch):patch;}
    private int particle(int id){return village?Protocol1_13_2To1_14.MAPPINGS.getParticleMappings().getNewId(id):id;}
    private Type<BlockPosition> positionType(){return village?Types.BLOCK_POSITION1_14:Types.BLOCK_POSITION1_8;}
    private void objectType(ByteBuf packet,int old){
        if(village)Types.VAR_INT.writePrimitive(packet,Protocol1_13_2To1_14.MAPPINGS.getEntityMappings().getNewId(com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13.ObjectType.findById(old,0).getType().getId()));else packet.writeByte(old);
    }
    private int outgoing(ServerboundPackets1_13 type){return village?ServerboundPackets1_14.valueOf(type.name()).getId():type.getId();}
    private ByteBuf packet(ClientboundPackets1_13 type){
        int id=target.serverProtocol()==498?ClientboundPackets1_14_4.valueOf(type.name()).getId():village?ClientboundPackets1_14.valueOf(type.name()).getId():type.getId();return BlockPipelineSmokeTest.packet(id);
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
