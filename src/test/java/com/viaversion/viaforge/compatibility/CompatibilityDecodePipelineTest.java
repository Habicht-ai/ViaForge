package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaversion.api.connection.*;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.exception.CancelDecoderException;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic wire bytes deliberately differ from Minecraft's legacy wire format. */
public class CompatibilityDecodePipelineTest {
    @Test public void allOutputsOfAnOriginalPacketBecomeVisibleTogether() {
        PacketTaskQueue queue=new PacketTaskQueue();List<Integer> handled=new ArrayList<>();
        Fixture fixture=new Fixture(new Probe(),false,true,queue);
        fixture.channel.pipeline().addAfter("compatibility","client",new ChannelInboundHandlerAdapter(){
            @Override public void channelRead(ChannelHandlerContext ctx,Object message) {
                ByteBuf buffer=(ByteBuf)message;int value=buffer.getUnsignedByte(buffer.readerIndex());buffer.release();
                queue.add(()->handled.add(value));queue.drain();
                assertTrue("No partial original packet may execute",handled.isEmpty());
            }
        });
        try {
            fixture.channel.writeInbound(Unpooled.buffer().writeByte(42).writeByte(93));
            assertTrue(handled.isEmpty());queue.drain();
            assertEquals(3,handled.size());assertEquals(Integer.valueOf(0x70),handled.get(0));
            assertEquals(Integer.valueOf(0x72),handled.get(2));assertEquals(1,fixture.translations);
        } finally {fixture.close();}
    }
    @Test public void originalDataSurvivesLossyTranslationAndUsesTheSharedRestoreStage(){
        Probe codec=new Probe();Fixture fixture=new Fixture(codec,false,true);
        try{
            ByteBuf input=Unpooled.buffer().writeByte(42).writeByte(93);fixture.channel.writeInbound(input);
            ByteBuf translated=(ByteBuf)fixture.channel.readInbound(),event=(ByteBuf)fixture.channel.readInbound(),after=(ByteBuf)fixture.channel.readInbound();
            assertEquals(93,codec.original);assertEquals(Arrays.asList("replace","capture","event","restore","restore","after"),codec.calls);
            assertEquals(0x70,translated.readUnsignedByte());assertEquals(93,translated.readUnsignedByte());
            ClientEventEnvelope envelope=ClientEventEnvelope.read(event);assertEquals(340,envelope.format.revision());assertEquals(26,envelope.operation);assertEquals(93,event.readUnsignedByte());
            assertEquals(0x72,after.readUnsignedByte());translated.release();event.release();after.release();
            assertEquals(0,input.refCnt());assertEquals(1,fixture.joins);assertNull(fixture.channel.readInbound());
        }finally{fixture.close();}assertEquals(1,fixture.closes);assertEquals(1,codec.clears);
    }
    @Test public void retainedEventsSurviveViaCancellationExactlyOnce(){
        Probe codec=new Probe();Fixture fixture=new Fixture(codec,true,true);
        try{
            ByteBuf input=Unpooled.buffer().writeByte(42).writeByte(11);fixture.channel.writeInbound(input);
            ByteBuf event=(ByteBuf)fixture.channel.readInbound(),after=(ByteBuf)fixture.channel.readInbound();
            assertEquals(26,ClientEventEnvelope.read(event).operation);assertEquals(11,event.readUnsignedByte());event.release();after.release();assertNull(fixture.channel.readInbound());assertEquals(0,input.refCnt());
        }finally{fixture.close();}
    }
    @Test public void cancelledPacketWithoutRetainedEventDoesNotClearConnectionState(){
        Probe codec=new Probe(){@Override public ByteBuf event(ByteBuf input){return null;}};
        Fixture fixture=new Fixture(codec,true,true);
        try {
            ByteBuf input=Unpooled.buffer().writeByte(42).writeByte(11);fixture.channel.writeInbound(input);
            assertNull(fixture.channel.readInbound());assertEquals(0,input.refCnt());assertEquals(1,fixture.translations);
            assertEquals("Via cancellation is normal traffic, not a world reset",0,codec.clears);
        }finally{fixture.close();}
    }
    @Test public void failedAdapterIsDisabledWhileViaKeepsTranslating(){
        Probe codec=new Probe(){@Override public ByteBuf event(ByteBuf input){throw new IllegalArgumentException("synthetic corrupt payload");}};Fixture fixture=new Fixture(codec,false,true);
        try{
            for(int i=0;i<2;i++){
                ByteBuf input=Unpooled.buffer().writeByte(42).writeByte(11);fixture.channel.writeInbound(input);ByteBuf output=(ByteBuf)fixture.channel.readInbound();assertEquals(0x70,output.readUnsignedByte());assertEquals(0,output.readUnsignedByte());output.release();assertEquals(0,input.refCnt());
            }
            assertEquals(Arrays.asList("replace","capture"),codec.calls);assertEquals(1,codec.clears);assertEquals(0,fixture.joins);
        }finally{fixture.close();}
    }
    @Test public void rejectedReplacementBuffersAreReleasedAndNeverReachVia(){
        ByteBuf replacement=Unpooled.buffer().writeByte(7);
        Probe codec=new Probe(){@Override public List<ByteBuf> replace(ByteBuf input){return Collections.singletonList(replacement);}};
        Fixture fixture=new Fixture(codec,false,false);
        try{ByteBuf input=Unpooled.buffer().writeByte(42);fixture.channel.writeInbound(input);assertNull(fixture.channel.readInbound());assertEquals(0,replacement.refCnt());assertEquals(0,input.refCnt());assertEquals(0,fixture.translations);}finally{fixture.close();}
    }
    @Test public void decoderReorderingRetainsItsCodecAndDoesNotReinitializeState(){
        Probe codec=new Probe();Fixture fixture=new Fixture(codec,false,true);
        try{
            for(int i=0;i<3;i++){ChannelHandler decoder=fixture.channel.pipeline().remove("compatibility");fixture.channel.pipeline().addFirst("compatibility",decoder);}
            assertEquals(0,codec.clears);assertEquals(0,fixture.closes);
        }finally{fixture.close();}
    }
    static class Probe implements PacketAdapter {
        final List<String> calls=new ArrayList<>();int original,clears;
        public List<ByteBuf> replace(ByteBuf input){calls.add("replace");return null;}
        public boolean capture(ByteBuf input){calls.add("capture");original=input.getUnsignedByte(input.readerIndex()+1);return true;}
        public ByteBuf event(ByteBuf input){calls.add("event");ByteBuf event=input.alloc().buffer();ClientEventEnvelope.write(event,ClientEventFormat.legacy(340),26);return event.writeByte(original);}
        public ByteBuf restore(ByteBuf translated){calls.add("restore");return translated.getUnsignedByte(translated.readerIndex())==0x70?translated.alloc().buffer().writeByte(0x70).writeByte(original):null;}
        public List<ByteBuf> afterTranslation(ByteBuf original){calls.add("after");return Collections.singletonList(original.alloc().buffer().writeByte(0x72));}
        public void clear(){clears++;}
    }
    static class Fixture {
        final EmbeddedChannel channel=new EmbeddedChannel(new ChannelInboundHandlerAdapter());int joins,closes,translations;
        Fixture(Probe codec,boolean cancel,boolean allowed){
            this(codec,cancel,allowed,null);
        }
        Fixture(Probe codec,boolean cancel,boolean allowed,PacketTaskQueue queue){
            ProtocolInfo info=(ProtocolInfo)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ProtocolInfo.class},(proxy,method,args)->{if(method.getName().equals("getServerState"))return State.PLAY;throw new UnsupportedOperationException(method.getName());});
            UserConnection user=(UserConnection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{UserConnection.class},(proxy,method,args)->{
                switch(method.getName()){
                    case "getChannel":return channel;
                    case "getProtocolInfo":return info;
                    case "shouldTransformPacket":return true;
                    case "checkIncomingPacket":return allowed;
                    case "transformIncoming":translations++;if(cancel)throw new CancelDecoderException();ByteBuf data=(ByteBuf)args[0];data.setByte(data.readerIndex(),0x70);data.setByte(data.readerIndex()+1,0);return null;
                    default:throw new UnsupportedOperationException(method.getName());
                }
            });
            channel.pipeline().addFirst("compatibility",new CompatibilityDecodeHandler(user,codec,()->joins++,()->closes++,
                    ()->queue!=null&&queue.beginPacket(),()->queue.endPacket()));
        }
        void close(){channel.close();channel.runPendingTasks();channel.checkException();Object next;while((next=channel.readInbound())!=null)((ByteBuf)next).release();}
    }
}
