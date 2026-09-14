package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import io.netty.buffer.ByteBuf;
import java.util.*;
import java.util.function.IntUnaryOperator;

/** Old wire codecs are confined to this adapter; renderers do not select them. */
public final class LegacyProtocolAdapter implements PacketAdapter {
    private final LegacyBlockPackets blocks;
    private final LegacyEntityPackets entities;
    private final IntUnaryOperator mapper;
    private final VersionRules rules;
    public LegacyProtocolAdapter(BlockVersionProfile wire,IntUnaryOperator mapper) { this(wire,CompatibilityRegistry.DEFAULT.resolve(wire.protocol()).rules(),mapper); }
    private LegacyProtocolAdapter(BlockVersionProfile wire,VersionRules rules,IntUnaryOperator mapper) { this.rules=rules;blocks=new LegacyBlockPackets(wire);entities=new LegacyEntityPackets(wire);this.mapper=mapper; }
    public List<ByteBuf> replace(ByteBuf input) {
        ByteBuf event=filter(entities.replacement(input));if(event!=null)return Collections.singletonList(event);
        if(!rules.has(ClientFeature.BLOCKS))return null;
        event=blocks.editorUpdate(input);if(event!=null)return Collections.singletonList(event);
        List<ByteBuf> beds=blocks.bedUpdate(input,mapper);if(beds!=null)return beds;
        event=blocks.blockEvent(input,mapper);return event==null?null:Collections.singletonList(event);
    }
    public boolean capture(ByteBuf input) throws Exception { return blocks.capture(input); }
    public ByteBuf event(ByteBuf input) { return filter(entities.capture(input)); }
    public ByteBuf restore(ByteBuf translated) throws Exception { return rules.has(ClientFeature.BLOCKS)?blocks.restore(translated,mapper):null; }
    public List<ByteBuf> afterTranslation(ByteBuf input) { return rules.has(ClientFeature.BLOCKS)?blocks.editorChunkUpdates(input):Collections.emptyList(); }
    private ByteBuf filter(ByteBuf event) {
        if(event==null)return null;
        io.netty.buffer.ByteBuf header=event.duplicate();
        com.viaversion.viaversion.api.type.Types.VAR_INT.readPrimitive(header);com.viaversion.viaversion.api.type.Types.STRING.read(header);
        ClientEventEnvelope envelope=ClientEventEnvelope.read(header);
        // Passenger events are shared by boats and mobs.
        if(rules.has(ClientEventEnvelope.feature(envelope.operation))||envelope.operation==13&&rules.has(ClientFeature.BOATS))return event;
        event.release();return null;
    }
    public void clear() { blocks.world().clear();entities.clear(); }
    public static final class Factory implements ProtocolAdapterFactory {
        private final BlockVersionProfile wire;
        public Factory(BlockVersionProfile wire) { this.wire=Objects.requireNonNull(wire); }
        public String id() { return "legacy-"+wire.protocol(); }
        public Set<ClientFeature> capabilities() { return Collections.unmodifiableSet(EnumSet.allOf(ClientFeature.class)); }
        public PacketAdapter create(CompatibilityProfile target,IntUnaryOperator mapper) { return new LegacyProtocolAdapter(wire,target.rules(),mapper); }
        public ItemDataAdapter items() { return ItemDataAdapter.LEGACY; }
        public void serverbound(String channel,PacketWrapper packet) { LegacyServerboundPackets.translate(channel,packet); }
    }
}
