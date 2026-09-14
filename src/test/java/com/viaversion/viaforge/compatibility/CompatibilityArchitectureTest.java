package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.api.minecraft.item.*;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import java.util.function.IntUnaryOperator;
import org.junit.Test;
import static org.junit.Assert.*;

public class CompatibilityArchitectureTest {
    private static CompatibilityProfile original(){return CompatibilityRegistry.DEFAULT.resolve(340);}
    @Test public void allExistingProfilesKeepExactAssetsAndCumulativeFeatures(){
        for(BlockVersionProfile legacy:BlockVersionProfile.values()){
            CompatibilityProfile p=CompatibilityRegistry.DEFAULT.resolve(legacy.protocol());
            assertEquals(legacy.protocol(),p.serverProtocol());assertEquals(legacy.resourceVersion(),p.resources().version());
            assertTrue(p.has(ClientFeature.TWO_HANDS));assertTrue(p.has(ClientFeature.BOATS));assertTrue(p.has(ClientFeature.COMBAT));
            assertEquals(legacy.protocol()>=315,p.has(ClientFeature.TOTEM));
            assertEquals(legacy.protocol()>=335,p.rules().enabled(ClientRule.FAST_PADDLE_CYCLE));
            assertEquals(legacy.protocol()>=316,p.rules().enabled(ClientRule.REVERSED_DRAGON_HEAD_ITEM));
            for(LegacyBlockCatalog.Definition block:LegacyBlockCatalog.BLOCKS)assertEquals(legacy.protocol()>=block.protocol,p.rules().contentSince(block.protocol));
        }
    }
    @Test public void newerUnknownAndNativeTargetsNeverPretendToBeLegacyWire(){
        for(int protocol:new int[]{47,106,111,341,394,402,405,763,767,Integer.MAX_VALUE}){
            CompatibilityProfile p=CompatibilityRegistry.DEFAULT.resolve(protocol);
            assertEquals(protocol,p.serverProtocol());assertFalse(p.extended());assertNull(p.adapter());assertNull(p.resources());assertFalse(p.has(ClientFeature.TWO_HANDS));
        }
    }
    @Test public void targetResourcesWireAndBehaviorCanVaryIndependently(){
        VersionRules parent=original().rules();
        VersionRules child=parent.derive().rule(ClientRule.REVERSED_DRAGON_HEAD_ITEM,false).disable(ClientFeature.TOTEM).build();
        ProbeFactory adapter=new ProbeFactory();ResourceProfile resources=new ResourceProfile("test-newer-assets",assets->Collections.singletonMap("normalized/stone",assets.get("new/stone")));
        CompatibilityProfile newer=new CompatibilityProfile(404,child,resources,adapter);
        CompatibilityRegistry registry=new CompatibilityRegistry(Arrays.asList(original(),newer));
        assertSame(newer,registry.resolve(404));assertSame(adapter,newer.adapter());assertEquals("test-newer-assets",newer.resources().version());
        assertTrue(newer.has(ClientFeature.TWO_HANDS));assertTrue(newer.has(ClientFeature.BOATS));assertFalse(newer.has(ClientFeature.TOTEM));
        assertFalse(newer.rules().enabled(ClientRule.REVERSED_DRAGON_HEAD_ITEM));assertTrue(parent.enabled(ClientRule.REVERSED_DRAGON_HEAD_ITEM));assertTrue(parent.has(ClientFeature.TOTEM));
        Map<String,byte[]> assets=new HashMap<>();assets.put("new/stone",new byte[]{8});assertArrayEquals(new byte[]{8},resources.normalize(assets).get("normalized/stone"));
        try{resources.normalize(assets).clear();fail();}catch(UnsupportedOperationException expected){}
    }
    @Test public void incompleteCodecAndMissingDependenciesAreRejected(){
        try{new CompatibilityProfile(404,original().rules(),ResourceProfile.legacy("1.12.2"),null);fail();}catch(IllegalArgumentException expected){}
        ProbeFactory adapter=new ProbeFactory(){public Set<ClientFeature> capabilities(){return EnumSet.of(ClientFeature.ITEMS);}};
        try{new CompatibilityProfile(404,original().rules(),ResourceProfile.legacy("1.12.2"),adapter);fail();}catch(IllegalArgumentException expected){}
        try{VersionRules.NATIVE.derive().enable(ClientFeature.TWO_HANDS).build();fail();}catch(IllegalStateException expected){}
        try{new CompatibilityRegistry(Arrays.asList(original(),original()));fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void outdatedResourceCompletionsCannotCrossRejoinOrServerSwitch(){
        SessionEpoch session=new SessionEpoch();Object a=new Object(),b=new Object();
        SessionEpoch.Ticket first=session.begin(a,original());SessionEpoch.Ticket rejoin=session.begin(a,original());
        assertFalse(session.current(first));assertTrue(session.current(rejoin));
        SessionEpoch.Ticket next=session.begin(b,CompatibilityRegistry.DEFAULT.resolve(107));assertFalse(session.current(rejoin));assertFalse(session.leave(a));assertTrue(session.current(next));
        assertTrue(session.leave(b));assertFalse(session.current(next));assertFalse(session.profile().extended());
        SessionEpoch.Ticket disconnected=session.begin(a,original());session.clear();assertFalse(session.current(disconnected));
    }
    @Test public void bothOutgoingDirectionsUseTheConnectionBoundCodec(){
        EmbeddedChannel channel=new EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());UserConnectionImpl user=new UserConnectionImpl(channel,true);user.getProtocolInfo().setServerProtocolVersion(ProtocolVersion.v1_13_2);
        ProbeFactory adapter=new ProbeFactory();CompatibilityProfile profile=new CompatibilityProfile(404,original().rules(),ResourceProfile.legacy("1.12.2"),adapter);user.put(profile);
        try{
            assertSame(profile,CompatibilityRegistry.forUser(user));
            PacketWrapper hand=new com.viaversion.viaversion.protocol.packet.PacketWrapperImpl(0,null,user),boat=new com.viaversion.viaversion.protocol.packet.PacketWrapperImpl(0,null,user);ExtensionPackets.translate("VF|hands",hand);ExtensionPackets.translate("VF|boat",boat);
            assertEquals(Arrays.asList("VF|hands","VF|boat"),adapter.sent);assertTrue(hand.isCancelled());assertTrue(boat.isCancelled());
            user.getProtocolInfo().setServerProtocolVersion(ProtocolVersion.v1_8);assertFalse(CompatibilityRegistry.forUser(user).extended());
            ExtensionPackets.translate("VF|hands",new com.viaversion.viaversion.protocol.packet.PacketWrapperImpl(0,null,user));assertEquals(2,adapter.sent.size());
        }finally{channel.finish();}
    }
    @Test public void normalizedEventsDoNotMistakeNewServerProtocolForClientPayloadSchema(){
        ByteBuf b=Unpooled.buffer();try{
            ClientEventEnvelope.write(b,ClientEventFormat.legacy(340),26);Types.ITEM1_8.write(b,new DataItem(442,(byte)1,(short)12,null));
            ClientEventEnvelope event=ClientEventEnvelope.read(b);assertEquals(340,event.format.revision());assertEquals(ClientFeature.TWO_HANDS,ClientEventEnvelope.feature(event.operation));assertEquals(442,Types.ITEM1_8.read(b).identifier());
            try{ClientEventFormat.legacy(404);fail();}catch(IllegalArgumentException expected){}
        }finally{b.release();}
    }
    @Test public void itemIdentityConversionDoesNotMutateSourceOrLoseCountDamageAndNbt(){
        com.viaversion.nbt.tag.CompoundTag tag=new com.viaversion.nbt.tag.CompoundTag();tag.putString("Custom","unchanged");
        Item original=new DataItem(442,(byte)3,(short)18,tag);Item client=ItemDataAdapter.LEGACY.toClientData(original);assertNotSame(original,client);client.setAmount(1);client.tag().putString("Custom","edited");
        assertEquals(3,original.amount());assertEquals("unchanged",original.tag().getString("Custom"));
        Item restored=ItemDataAdapter.LEGACY.toServerData(client);assertEquals(442,restored.identifier());assertEquals(18,restored.data());assertEquals(1,restored.amount());assertEquals("edited",restored.tag().getString("Custom"));
    }
    @Test public void sharedItemBridgeUsesDifferentServerIdsInBothDirections(){
        EmbeddedChannel channel=new EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        UserConnectionImpl user=new UserConnectionImpl(channel,true);user.getProtocolInfo().setServerProtocolVersion(ProtocolVersion.v1_13_2);
        user.getProtocolInfo().setPipeline((com.viaversion.viaversion.api.protocol.ProtocolPipeline)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{com.viaversion.viaversion.api.protocol.ProtocolPipeline.class},(proxy,method,args)->{
            if(method.getName().equals("pipes"))return Collections.emptyList();throw new UnsupportedOperationException(method.getName());
        }));
        ProbeFactory adapter=new ProbeFactory(){@Override public ItemDataAdapter items(){return new ItemDataAdapter(){
            public Item toClientData(Item item){Item copy=item.copy();copy.setIdentifier(442);return copy;}
            public Item toServerData(Item item){Item copy=item.copy();copy.setIdentifier(9000);return copy;}
        };}};
        user.put(new CompatibilityProfile(404,original().rules(),ResourceProfile.legacy("1.12.2"),adapter));
        LegacyItemDefinition shield=null;for(LegacyItemCatalog.Definition item:LegacyItemCatalog.ITEMS)if(item.id==442)shield=item;
        final LegacyItemDefinition definition=shield;
        LegacyBlockItemBridge bridge=new LegacyBlockItemBridge((id,data)->id==442?20000:-1,id->id==20000?definition:null);
        try{
            com.viaversion.nbt.tag.CompoundTag tag=new com.viaversion.nbt.tag.CompoundTag();tag.putString("Name","server shield");
            Item server=new DataItem(9000,(byte)2,(short)29,tag),local=bridge.toClient(user,server);
            assertEquals(20000,local.identifier());assertEquals(29,local.data());assertEquals(9000,server.identifier());
            local.setAmount(1);Item returned=bridge.toServer(user,local);
            assertEquals(9000,returned.identifier());assertEquals(29,returned.data());assertEquals(1,returned.amount());assertEquals("server shield",returned.tag().getString("Name"));
            user.getProtocolInfo().setServerProtocolVersion(ProtocolVersion.v1_8);assertNull(bridge.toServer(user,local));
        }finally{channel.finish();}
    }
    static class ProbeFactory implements ProtocolAdapterFactory {
        final List<String> sent=new ArrayList<>();
        public String id(){return "synthetic-new-format";}
        public Set<ClientFeature> capabilities(){return EnumSet.allOf(ClientFeature.class);}
        public PacketAdapter create(CompatibilityProfile target,IntUnaryOperator mapper){throw new UnsupportedOperationException();}
        public ItemDataAdapter items(){return ItemDataAdapter.LEGACY;}
        public void serverbound(String channel,PacketWrapper packet){sent.add(channel);}
    }
}
