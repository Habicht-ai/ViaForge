package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.blocks.BlockPreservingDecodeHandler;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipelineException;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlockPreservingDecodeHandlerTest {
    @Test
    public void bypassesLoginTrafficAndOwnsBuffersAndLifecyclePerConnection() {
        AtomicInteger joins = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        State[] state = {State.LOGIN};
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        ProtocolInfo info = (ProtocolInfo) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ProtocolInfo.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getServerState")) return state[0];
                    throw new UnsupportedOperationException(method.getName());
                });
        UserConnection user = (UserConnection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{UserConnection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getChannel": return channel;
                        case "getProtocolInfo": return info;
                        case "checkIncomingPacket": return true;
                        case "shouldTransformPacket": return state[0] == State.PLAY;
                        case "transformIncoming":
                            ByteBuf buffer = (ByteBuf) args[0];
                            buffer.setByte(buffer.readerIndex(), 0x01); // translated 1.8 join id
                            return null;
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
        channel.pipeline().addFirst("via-decoder", new BlockPreservingDecodeHandler(user, BlockVersionProfile.V1_12_2,
                original -> 6000, joins::incrementAndGet, closes::incrementAndGet));
        ByteBuf loginTraffic = Unpooled.buffer().writeByte(0x23);
        channel.writeInbound(loginTraffic);
        ByteBuf loginOutput = (ByteBuf) channel.readInbound();
        assertEquals(1, loginOutput.refCnt());
        loginOutput.release();
        assertEquals(0, loginTraffic.refCnt());
        assertEquals(0, joins.get());

        state[0] = State.PLAY;
        ByteBuf join = Unpooled.buffer();
        Types.VAR_INT.writePrimitive(join, 0x23);
        join.writeInt(1).writeByte(0).writeInt(0);
        channel.writeInbound(join);
        ByteBuf joinOutput = (ByteBuf) channel.readInbound();
        assertEquals(0x01, joinOutput.readUnsignedByte());
        joinOutput.release();
        assertEquals(1, joins.get());
        assertEquals(0, join.refCnt());
        channel.close();
        // Netty 4.0 dispatches channelInactive through the embedded event loop.
        channel.runPendingTasks();
        channel.checkException();
        assertEquals(1, closes.get());
    }

    @Test
    public void allowsRepeatedRemovalAndReinsertionOnItsConnection() {
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        AtomicInteger closes = new AtomicInteger();
        BlockPreservingDecodeHandler decoder = decoderFor(channel, closes);
        try {
            channel.pipeline().addFirst("via-decoder", decoder);
            for (int i = 0; i < 3; i++) {
                // The compression setup removes and reinserts the existing Via handler.
                assertSame(decoder, channel.pipeline().remove("via-decoder"));
                channel.pipeline().addFirst("via-decoder", decoder);
                channel.checkException();
                assertEquals(0, closes.get());
                ByteBuf input = Unpooled.buffer().writeByte(0x23);
                assertTrue(channel.writeInbound(input));
                ByteBuf output = (ByteBuf) channel.readInbound();
                assertEquals(0x23, output.readUnsignedByte());
                output.release();
                assertEquals(0, input.refCnt());
            }
        } finally {
            channel.close();
            channel.runPendingTasks();
            channel.checkException();
        }
        assertEquals(1, closes.get());
    }

    @Test
    public void rejectsReuseOnAnotherConnection() {
        EmbeddedChannel owner = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        EmbeddedChannel other = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        AtomicInteger closes = new AtomicInteger();
        BlockPreservingDecodeHandler decoder = decoderFor(owner, closes);
        try {
            owner.pipeline().addFirst("via-decoder", decoder);
            try {
                other.pipeline().addFirst("via-decoder", decoder);
                other.checkException();
                fail("A decoder must not share its block state with another connection");
            } catch (ChannelPipelineException expected) {
                assertTrue(expected.getCause() instanceof IllegalArgumentException);
            }
            assertSame(decoder, owner.pipeline().get("via-decoder"));
            assertNull(other.pipeline().get("via-decoder"));
            assertEquals(0, closes.get());
        } finally {
            other.close();
            other.runPendingTasks();
            owner.close();
            owner.runPendingTasks();
        }
        assertEquals(1, closes.get());
    }

    private BlockPreservingDecodeHandler decoderFor(EmbeddedChannel channel, AtomicInteger closes) {
        UserConnection user = (UserConnection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{UserConnection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getChannel": return channel;
                        case "checkIncomingPacket": return true;
                        case "shouldTransformPacket": return false;
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
        return new BlockPreservingDecodeHandler(user, BlockVersionProfile.V1_12_2, original -> 6000,
                () -> fail("Login traffic must not activate block resources"), closes::incrementAndGet);
    }
}
