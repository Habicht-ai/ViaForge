package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viabackwards.protocol.v1_12_2to1_12_1.Protocol1_12_2To1_12_1;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocol.packet.PacketWrapperImpl;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import io.netty.buffer.*;
import java.util.*;
import java.util.function.IntUnaryOperator;
import java.util.logging.*;

/** Explicit flattened families observing the one active Via translation pass. */
public final class FlattenedProtocolAdapter implements PacketAdapter, StorableObject {
    private final LegacyBlockPackets blocks=new LegacyBlockPackets(BlockVersionProfile.V1_12_2);
    private final LegacyEntityPackets entities=new LegacyEntityPackets(BlockVersionProfile.V1_12_2);
    private final IntUnaryOperator mapper;
    public final WaterColors waterColors = new WaterColors();
    private FlattenedBlockData flattened;
    private VillageBlockData village;
    private final Map<Class<?>,VillageBlockData> modernFamilies=new HashMap<>();
    private final List<ByteBuf> pending=new ArrayList<>();
    private final Set<PacketWrapper> captured=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<PacketWrapper> replaced=Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean active,failed,observedJoin;
    public FlattenedProtocolAdapter(IntUnaryOperator mapper){this.mapper=mapper;}
    public List<ByteBuf> replace(ByteBuf original){return null;}
    public boolean capture(ByteBuf original){
        active=!failed;
        observedJoin=false;
        return false; // Join IDs vary by target; the decoder recognizes normalized native LOGIN.
    }
    public ByteBuf event(ByteBuf original){return null;}
    /** Called only at declared boundaries by the mixin in AbstractProtocol. */
    public boolean beforeProtocol(Object protocol,PacketWrapper packet) {
        if(!active||failed)return false;
        boolean original=protocol instanceof Protocol1_13To1_12_2;
        boolean modern=protocol instanceof com.viaversion.viabackwards.protocol.v1_14to1_13_2.Protocol1_14To1_13_2;
        boolean legacy=protocol instanceof Protocol1_12_2To1_12_1;
        if(flattened==null)flattened=new FlattenedBlockData(waterColors);
        Class<?> type=protocol.getClass();
        if(!modernFamilies.containsKey(type))modernFamilies.put(type,ModernBlockFamilies.create(protocol,flattened,packet.user()));
        VillageBlockData family=modernFamilies.get(type);
        if(!original&&!legacy&&!modern&&family==null)return false;
        ByteBuf snapshot=null,normalized=null;
        try {
            snapshot=snapshot(packet);
            if(family!=null) {
                normalized=captured.contains(packet)?null:family.normalize(snapshot);
                if(normalized!=null){captureBlocks(packet,normalized);captured.add(packet);}
            }else if(modern) {
                if(flattened==null)flattened=new FlattenedBlockData();
                if(village==null)village=new VillageBlockData(flattened);
                normalized=captured.contains(packet)?null:village.normalize(snapshot);
                if(normalized!=null){captureBlocks(packet,normalized);captured.add(packet);}
            }else if(original) {
                if(flattened==null)flattened=new FlattenedBlockData();
                captureCloudParticle(snapshot,(Protocol1_13To1_12_2)protocol);
                normalized=captured.contains(packet)?null:flattened.normalize(snapshot);
                if(Types.VAR_INT.readPrimitive(snapshot.duplicate())==ClientboundPackets1_13.LOGIN.getId())observedJoin=true;
                if(normalized!=null){captureBlocks(packet,normalized);captured.add(packet);}
            }else {
                if(!captured.remove(packet))captureBlocks(packet,snapshot);
                ByteBuf replacement=entities.replacement(snapshot);
                if(replacement!=null){pending.add(replacement);replaced.add(packet);}
                else {ByteBuf event=entities.capture(snapshot);if(event!=null)pending.add(event);}
                if(replaced.remove(packet)){packet.cancel();return true;}
            }
        }catch(Exception error) {
            clear();failed=true;
            Logger.getLogger("ViaForge/Compatibility").log(Level.WARNING,"Flattened adapter failed; retaining ordinary Via translation",error);
        }finally{if(normalized!=null)normalized.release();if(snapshot!=null)snapshot.release();}
        return false;
    }
    private void captureCloudParticle(ByteBuf source,Protocol1_13To1_12_2 protocol)throws Exception {
        ByteBuf input=source.duplicate();
        if(Types.VAR_INT.readPrimitive(input)!=ClientboundPackets1_13.SET_ENTITY_DATA.getId())return;
        int entity=Types.VAR_INT.readPrimitive(input);
        for(com.viaversion.viaversion.api.minecraft.entitydata.EntityData data:com.viaversion.viaversion.api.type.types.version.Types1_13.ENTITY_DATA_LIST.read(input)) {
            if(data.id()!=9||data.dataType().typeId()!=15)continue;
            // ViaBackwards' generic particle filter cancels this entry before its
            // cloud-specific filter runs. Retain it before that lossy step.
            com.viaversion.viaversion.api.minecraft.Particle particle=data.value();
            com.viaversion.viabackwards.protocol.v1_13to1_12_2.data.ParticleIdMappings1_12_2.ParticleData mapping=
                    com.viaversion.viabackwards.protocol.v1_13to1_12_2.data.ParticleIdMappings1_12_2.getMapping(particle.id());
            int[] arguments=mapping.rewriteMeta(protocol,particle.getArguments());
            ByteBuf normalized=source.alloc().buffer();
            try {
                Types.VAR_INT.writePrimitive(normalized,com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1.SET_ENTITY_DATA.getId());
                Types.VAR_INT.writePrimitive(normalized,entity);
                for(int i=0;i<3;i++) {
                    normalized.writeByte(9+i).writeByte(1);
                    Types.VAR_INT.writePrimitive(normalized,i==0?mapping.getHistoryId():arguments!=null&&arguments.length>=i?arguments[i-1]:0);
                }
                normalized.writeByte(255);ByteBuf event=entities.capture(normalized);if(event!=null)pending.add(event);
            }finally{normalized.release();}
        }
    }
    private void captureBlocks(PacketWrapper packet,ByteBuf normalized)throws Exception {
        ByteBuf replacement=blocks.editorUpdate(normalized);
        if(replacement==null)replacement=blocks.blockEvent(normalized,mapper);
        if(replacement!=null){pending.add(replacement);replaced.add(packet);}
        blocks.capture(normalized);
        pending.addAll(blocks.editorChunkUpdates(normalized));
    }
    /** Preserve both typed fields and the unread wire tail, including its reader index. */
    public static ByteBuf snapshot(PacketWrapper packet)throws Exception {
        ByteBuf tail=((PacketWrapperImpl)packet).getInputBuffer();
        int reader=tail==null?0:tail.readerIndex();ByteBuf copy=Unpooled.buffer();
        try{packet.writeToBuffer(copy);return copy;}
        catch(Throwable error){copy.release();throw error;}
        finally{if(tail!=null)tail.readerIndex(reader);packet.resetReader();}
    }
    public ByteBuf restore(ByteBuf translated)throws Exception{return failed?null:blocks.restore(translated,mapper);}
    public boolean observedJoin(){return observedJoin;}
    public List<ByteBuf> afterTranslation(ByteBuf original){
        active=false;captured.clear();replaced.clear();
        List<ByteBuf> result=new ArrayList<>(pending);pending.clear();return result;
    }
    public void clear(){active=false;waterColors.clear();blocks.world().clear();entities.clear();captured.clear();replaced.clear();for(ByteBuf packet:pending)packet.release();pending.clear();}
    public static final class Factory implements ProtocolAdapterFactory {
        private final int protocol;
        public Factory(int protocol){if(protocol!=393&&protocol!=401&&protocol!=404&&protocol!=477&&protocol!=480&&protocol!=485&&protocol!=490&&protocol!=498&&protocol!=573&&protocol!=575&&protocol!=578&&protocol!=735&&protocol!=736&&protocol!=751&&protocol!=753&&protocol!=754&&protocol!=755&&protocol!=756&&protocol!=757&&protocol!=758&&protocol!=759&&protocol!=760&&protocol!=761&&protocol!=762&&protocol!=763&&protocol!=764&&protocol!=765&&protocol!=766&&protocol!=767&&protocol!=768&&protocol!=769&&protocol!=770&&protocol!=771&&protocol!=772&&protocol!=773&&protocol!=774&&protocol!=775&&protocol!=776)throw new IllegalArgumentException("Unverified flattened target "+protocol);this.protocol=protocol;}
        public String id(){return "flattened-"+protocol;}
        public Set<ClientFeature> capabilities(){return Collections.unmodifiableSet(EnumSet.allOf(ClientFeature.class));}
        public PacketAdapter create(CompatibilityProfile target,IntUnaryOperator mapper){if(target.serverProtocol()!=protocol)throw new IllegalArgumentException("Wrong target");return new FlattenedProtocolAdapter(mapper);}
        public ItemDataAdapter items(){return new FlattenedItemDataAdapter();}
        public void serverbound(String channel,PacketWrapper packet){LegacyServerboundPackets.translate(channel,packet);}
    }
}
