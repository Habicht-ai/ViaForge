package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.compatibility.ClientPacketTasks;
import com.viaversion.viaforge.common.extended.ExtendedNetworkManager;
import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.*;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Exercises the transformed PacketThreadUtil, not a replacement network handler. */
final class PacketQueueSmokeTest {
    private static void process(boolean dedicated) {
        if (dedicated) ClientPacketTasks.process();
        else drainGeneral();
    }
    private static void drainGeneral() {
        FutureTask<?> task;
        while ((task=((ClientPacketTasks.GeneralTasks)Minecraft.getMinecraft()).viaForge$tasks().poll())!=null) task.run();
    }
    static void verify(int protocol) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        boolean dedicated=CompatibilityRegistry.DEFAULT.resolve(protocol).rules().enabled(ClientRule.DEDICATED_PACKET_QUEUE);
        boolean[] open = {true};
        NetworkManager manager = new NetworkManager(EnumPacketDirection.CLIENTBOUND) {
            @Override public boolean isChannelOpen() { return open[0]; }
        };
        ((ExtendedNetworkManager)manager).viaForge$setTrackedVersion(ProtocolVersion.getProtocol(protocol));
        NetHandlerPlayClient handler = new NetHandlerPlayClient(mc,null,manager,new GameProfile(new UUID(0,914),"PacketQueueTest"));
        List<Integer> received = new ArrayList<>();
        ExecutorService network = Executors.newSingleThreadExecutor();
        try {
            network.submit(() -> {
                require(ClientPacketTasks.beginPacket(dedicated),"Begin original packet");
                try {
                for (int id=0;id<2;id++) {
                    final int value=id;
                    Packet<INetHandlerPlayClient> packet = new Packet<INetHandlerPlayClient>() {
                        public void readPacketData(PacketBuffer data) { }
                        public void writePacketData(PacketBuffer data) { }
                        public void processPacket(INetHandlerPlayClient listener) {
                            PacketThreadUtil.checkThreadAndEnqueue(this,listener,mc);
                            require(mc.isCallingFromMinecraftThread(),"Packet must execute on client thread");
                            received.add(value);
                        }
                    };
                    try { packet.processPacket(handler); throw new AssertionError("Network thread must exit queued handler"); }
                    catch (ThreadQuickExitException expected) { }
                }
                } finally { ClientPacketTasks.endPacket(); }
            }).get(5,TimeUnit.SECONDS);
            require(received.isEmpty(),"Enqueue must not process on network thread");
            process(dedicated);
            require(received.equals(Arrays.asList(0,1)),"Transformed packet queue preserves FIFO");
            process(dedicated);
            require(received.size()==2,"Packets execute once");
            // Hold the original 1.8 drain monitor: a modern producer must not wait.
            com.google.common.util.concurrent.ListenableFuture<?> ordinary;
            synchronized (((ClientPacketTasks.GeneralTasks)mc).viaForge$tasks()) {
                ordinary=network.submit(() -> mc.addScheduledTask(() -> received.add(9))).get(5,TimeUnit.SECONDS);
            }
            drainGeneral();ordinary.get(5,TimeUnit.SECONDS);
            require(received.equals(Arrays.asList(0,1,9)),"Ordinary task producer stays concurrent and executes once");
            PacketBuffer buffer = new PacketBuffer(io.netty.buffer.Unpooled.buffer());
            buffer.writeInt(123);
            S3FPacketCustomPayload retained = new S3FPacketCustomPayload("VF|pong",buffer);
            network.submit(() -> {
                try { retained.processPacket(handler); throw new AssertionError("Expected queued retained packet"); }
                catch (ThreadQuickExitException expected) { }
            }).get(5,TimeUnit.SECONDS);
            open[0]=false;
            process(dedicated);
            require(buffer.refCnt()==0,"Disconnect releases queued retained payload without dispatching it");
        } finally { network.shutdownNow(); }
    }
    private PacketQueueSmokeTest() { }
}
