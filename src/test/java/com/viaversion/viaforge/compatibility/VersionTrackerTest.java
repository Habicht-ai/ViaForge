package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.platform.VersionTracker;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class VersionTrackerTest {
    @Test public void serverOverrideWinsAndNullInheritsGlobal() {
        assertSame(ProtocolVersion.v1_12_2, VersionTracker.resolve(ProtocolVersion.v1_12_2, ProtocolVersion.v1_8));
        assertSame(ProtocolVersion.v1_8, VersionTracker.resolve(null, ProtocolVersion.v1_8));
    }

    @Test public void concurrentConnectionsCannotStealEachOthersVersion() throws Exception {
        // All workers may connect to the same IP AND port. No address is needed
        // to isolate a connection, including two server-list rows for one proxy.
        int count = 12;
        ExecutorService workers = Executors.newFixedThreadPool(count);
        CyclicBarrier insideFactory = new CyclicBarrier(count);
        List<Future<ProtocolVersion>> results = new ArrayList<>();
        try {
            for (int i = 0; i < count; i++) {
                final ProtocolVersion target = i % 2 == 0 ? ProtocolVersion.v1_12_2 : ProtocolVersion.v1_20;
                results.add(workers.submit(() -> {
                    ProtocolVersion captured = VersionTracker.connect(target, () -> {
                        try { insideFactory.await(5, TimeUnit.SECONDS); }
                        catch (Exception e) { throw new AssertionError(e); }
                        return VersionTracker.currentOr(ProtocolVersion.v1_8);
                    });
                    assertSame(ProtocolVersion.v1_8, VersionTracker.currentOr(ProtocolVersion.v1_8));
                    return captured;
                }));
            }
            for (int i = 0; i < count; i++)
                assertSame(i % 2 == 0 ? ProtocolVersion.v1_12_2 : ProtocolVersion.v1_20, results.get(i).get(10, TimeUnit.SECONDS));
        } finally { workers.shutdownNow(); }
    }

    @Test public void nestedAndFailedFactoriesRestoreScope() {
        VersionTracker.connect(ProtocolVersion.v1_12_2, () -> {
            try {
                VersionTracker.connect(ProtocolVersion.v1_20, () -> {
                    assertSame(ProtocolVersion.v1_20, VersionTracker.currentOr(ProtocolVersion.v1_8));
                    throw new IllegalStateException("Connection refused");
                });
                fail("Expected refused connection");
            } catch (IllegalStateException expected) { }
            assertSame(ProtocolVersion.v1_12_2, VersionTracker.currentOr(ProtocolVersion.v1_8));
            return null;
        });
        assertSame(ProtocolVersion.v1_8, VersionTracker.currentOr(ProtocolVersion.v1_8));
    }
}
