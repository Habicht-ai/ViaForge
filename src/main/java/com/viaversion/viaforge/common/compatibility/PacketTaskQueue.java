package com.viaversion.viaforge.common.compatibility;

import java.util.Queue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Modern packet processing: the producer must remain free while handlers run. */
public final class PacketTaskQueue {
    private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();
    private final ThreadLocal<List<Runnable>> translating = new ThreadLocal<>();

    public void add(Runnable task) {
        List<Runnable> packet = translating.get();
        if (packet == null) pending.add(task); else packet.add(task);
    }

    /** One original packet may produce native packets plus retained events. */
    public boolean beginPacket() {
        if (translating.get() != null) return false;
        translating.set(new ArrayList<>());
        return true;
    }

    public void endPacket() {
        endPacket(pending::add);
    }

    public boolean inPacket() { return translating.get() != null; }

    /** Older originals share the ordinary task stage instead of a packet stage. */
    public void endPacket(java.util.function.Consumer<Runnable> publish) {
        List<Runnable> packet = translating.get();
        translating.remove();
        if (packet == null) throw new IllegalStateException("No original packet in progress");
        if (!packet.isEmpty()) publish.accept(() -> { for (Runnable task : packet) task.run(); });
    }

    /** Called once at the packet-processing stage, never an additional game tick. */
    public void drain() {
        Runnable task;
        while ((task = pending.poll()) != null) task.run();
    }
}
