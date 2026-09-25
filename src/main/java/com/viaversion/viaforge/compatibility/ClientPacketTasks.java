package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.ViaForgeCommon;
import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaforge.common.extended.ExtendedNetworkManager;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.Channel;
import java.util.concurrent.FutureTask;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Original concurrent tasks (1.13+), with a separate packet stage from 1.21.9. */
public final class ClientPacketTasks {
    private static final PacketTaskQueue PACKETS = new PacketTaskQueue();
    private static final ThreadLocal<Boolean> DEDICATED = new ThreadLocal<>();
    private static final Logger LOGGER = LogManager.getLogger("ViaForge/Packets");

    private static boolean rule(NetworkManager connection, ClientRule rule) {
        ProtocolVersion version = ((ExtendedNetworkManager) connection).viaForge$getTrackedVersion();
        return version != null && CompatibilityRegistry.DEFAULT.resolve(version.getVersion()).rules()
                .enabled(rule);
    }

    public static boolean enabled(NetworkManager connection) {
        return rule(connection, ClientRule.CONCURRENT_CLIENT_TASKS);
    }

    public interface GeneralTasks { Queue<FutureTask<?>> viaForge$tasks(); }

    private static void general(Runnable task) {
        ((GeneralTasks) Minecraft.getMinecraft()).viaForge$tasks().add(new FutureTask<>(task, null));
    }

    public static void enqueue(NetworkManager connection, Runnable task) {
        enqueue(rule(connection, ClientRule.DEDICATED_PACKET_QUEUE), task);
    }

    private static void enqueue(boolean dedicated, Runnable task) {
        // Preserve 1.8's task exception reporting; no packet handler runs on Netty.
        FutureTask<?> future = new FutureTask<>(task, null);
        Runnable handler = () -> Util.runTask(future, LOGGER);
        if (dedicated || PACKETS.inPacket()) PACKETS.add(handler);
        else general(handler);
    }

    public static void process() { PACKETS.drain(); }
    public static boolean beginPacket(boolean dedicated) {
        if (!PACKETS.beginPacket()) return false;
        DEDICATED.set(dedicated);
        return true;
    }
    public static void endPacket() {
        try {
            if (Boolean.TRUE.equals(DEDICATED.get())) PACKETS.endPacket();
            else PACKETS.endPacket(ClientPacketTasks::general);
        } finally { DEDICATED.remove(); }
    }

    /** Retained connection events belong in the same FIFO as their play packets. */
    public static void connectionEvent(Object connection, Runnable task) {
        if (connection instanceof Channel) {
            UserConnection user = ((Channel) connection).attr(ViaForgeCommon.VF_VIA_USER).get();
            if (user != null && CompatibilityRegistry.forUser(user).rules().enabled(ClientRule.CONCURRENT_CLIENT_TASKS)) {
                enqueue(CompatibilityRegistry.forUser(user).rules().enabled(ClientRule.DEDICATED_PACKET_QUEUE),task);
                return;
            }
        }
        Minecraft.getMinecraft().addScheduledTask(task);
    }

    private ClientPacketTasks() { }
}
