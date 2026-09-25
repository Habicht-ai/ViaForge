package viaforge.lab;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Passive stack sampler; no transformation, input, network or physics changes. */
public final class ThreadStallProbe {
    private static final List<String> samples = Collections.synchronizedList(new ArrayList<String>());
    private static volatile boolean stopping;

    public static void premain(String destination, Instrumentation unused) {
        final Thread sampler = new Thread(() -> {
            Set<Thread> observed = new HashSet<>();
            int count = 0;
            while (!stopping) {
                long started = System.nanoTime();
                if (count++ % 20 == 0) {
                    for (Thread thread : Thread.getAllStackTraces().keySet()) {
                        String name = thread.getName();
                        if (name.equals("Client thread") || name.startsWith("Netty Client IO")) observed.add(thread);
                    }
                }
                for (Thread thread : observed) {
                    if (!thread.isAlive()) continue;
                    long timestamp = System.currentTimeMillis();
                    StackTraceElement[] stack = thread.getStackTrace();
                    StringBuilder line = new StringBuilder().append(timestamp).append('\t')
                            .append(System.nanoTime()).append('\t').append(thread.getName())
                            .append('\t').append(thread.getState());
                    for (StackTraceElement element : stack) line.append('\t').append(element);
                    samples.add(line.toString());
                }
                long spent = System.nanoTime() - started;
                samples.add(System.currentTimeMillis() + "\t" + System.nanoTime() + "\tSAMPLER_COST_NS\t" + spent);
                try { Thread.sleep(50); } catch (InterruptedException ignored) { }
            }
        }, "ViaForge passive stack sampler");
        sampler.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopping = true;
            try {
                sampler.join(2000);
                synchronized (samples) {
                    Files.write(Paths.get(destination), samples, StandardCharsets.UTF_8);
                }
            } catch (Exception error) { error.printStackTrace(); }
        }, "ViaForge passive stack export"));
        sampler.start();
    }
}
