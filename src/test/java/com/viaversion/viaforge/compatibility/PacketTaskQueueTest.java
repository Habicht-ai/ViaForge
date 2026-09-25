package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PacketTaskQueueTest {
    @Test public void oldConcurrentOriginalUsesOneOrdinaryTaskForAllOutputs() {
        PacketTaskQueue queue=new PacketTaskQueue();
        List<Runnable> ordinary=new ArrayList<>();
        List<Integer> observed=new ArrayList<>();
        assertTrue(queue.beginPacket());
        queue.add(()->observed.add(1));queue.add(()->observed.add(2));
        queue.endPacket(ordinary::add);
        assertFalse(queue.inPacket());
        queue.drain();assertTrue(observed.isEmpty());
        assertEquals(1,ordinary.size());ordinary.get(0).run();
        assertEquals(Arrays.asList(1,2),observed);
    }
    @Test public void originalPacketPublishesAllTranslatedHandlersTogether() throws Exception {
        PacketTaskQueue queue = new PacketTaskQueue();
        List<String> handled = new ArrayList<>();
        CountDownLatch firstOutput = new CountDownLatch(1), finishTranslation = new CountDownLatch(1);
        ExecutorService receiver = Executors.newSingleThreadExecutor();
        try {
            Future<?> received = receiver.submit(() -> {
                assertTrue(queue.beginPacket());
                assertFalse("Nested original dispatch belongs to its outer packet", queue.beginPacket());
                try {
                    queue.add(() -> handled.add("native flags"));
                    firstOutput.countDown();
                    try { assertTrue(finishTranslation.await(5, TimeUnit.SECONDS)); }
                    catch (InterruptedException e) { throw new AssertionError(e); }
                    queue.add(() -> handled.add("retained flight flag"));
                } finally { queue.endPacket(); }
                queue.add(() -> handled.add("next packet"));
            });
            assertTrue(firstOutput.await(5, TimeUnit.SECONDS));
            queue.drain();
            assertTrue("A partially translated packet must not change game state", handled.isEmpty());
            finishTranslation.countDown();received.get(5, TimeUnit.SECONDS);
            queue.drain();
            assertEquals(Arrays.asList("native flags", "retained flight flag", "next packet"), handled);
        } finally { finishTranslation.countDown();receiver.shutdownNow(); }
    }
    @Test public void receiveThreadCanQueueMetadataWhilePingIsBeingHandled() throws Exception {
        PacketTaskQueue queue = new PacketTaskQueue();
        List<String> handled = new ArrayList<>();
        CountDownLatch handlingPing = new CountDownLatch(1), queuedMetadata = new CountDownLatch(1);
        ExecutorService receiver = Executors.newSingleThreadExecutor();
        try {
            Future<?> received = receiver.submit(() -> {
                try { assertTrue(handlingPing.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { throw new AssertionError(e); }
                queue.add(() -> handled.add("metadata"));
                queue.add(() -> handled.add("post-pong"));
                queuedMetadata.countDown();
            });
            queue.add(() -> {
                handled.add("pre-pong");
                handlingPing.countDown();
                try { assertTrue("Receiver must not wait for the current handler", queuedMetadata.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { throw new AssertionError(e); }
            });
            queue.drain();
            received.get(5, TimeUnit.SECONDS);
            assertEquals(Arrays.asList("pre-pong", "metadata", "post-pong"), handled);
            queue.drain();
            assertEquals("Each handler runs exactly once", 3, handled.size());
        } finally { receiver.shutdownNow(); }
    }

    @Test public void packetProcessorStartsAtItsOriginalVersionBoundary() {
        for (int p : new int[]{47,340,393,477,756,758,770,771,772,773,774,775,776,777,778}) {
            assertEquals("protocol " + p, p >= 773 && p <= 777,
                    CompatibilityRegistry.DEFAULT.resolve(p).rules().enabled(ClientRule.DEDICATED_PACKET_QUEUE));
            assertEquals("Concurrent general tasks, protocol " + p, p>=393 && p<=777,
                    CompatibilityRegistry.DEFAULT.resolve(p).rules().enabled(ClientRule.CONCURRENT_CLIENT_TASKS));
        }
    }
}
