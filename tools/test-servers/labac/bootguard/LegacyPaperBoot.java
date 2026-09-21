package viaforge.lab;

import java.lang.instrument.*;
import java.security.*;
import java.nio.ByteBuffer;
import java.util.HexFormat;

/** Allows the reviewed legacy Paper boot guard to admit Java 17. No game/check code changes. */
public final class LegacyPaperBoot {
    public static void premain(String arguments, Instrumentation instrumentation) {
        String[] values = arguments.split(":");
        if (values.length != 3) throw new IllegalArgumentException("Expected Main SHA256:constant offset:old maximum");
        String expected = values[0];
        int offset = Integer.parseInt(values[1]);
        double maximum = Double.parseDouble(values[2]);
        if (Runtime.version().feature() != 17 || maximum < 52 || maximum >= 61)
            throw new IllegalStateException("Reviewed for Java 17 only");
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> redefined,
                                               ProtectionDomain domain, byte[] bytes) throws IllegalClassFormatException {
                if (!"org/bukkit/craftbukkit/Main".equals(name)) return null;
                try {
                    String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                    if (!expected.equals(actual) || bytes[offset - 1] != 6
                            || ByteBuffer.wrap(bytes).getDouble(offset) != maximum)
                        throw new IllegalStateException("Unreviewed Paper Main; refusing boot guard adaptation");
                    byte[] patched = bytes.clone();
                    ByteBuffer.wrap(patched).putDouble(offset, 61.0);
                    System.out.println("[ViaForgeLab] Reviewed Paper Java guard " + maximum + " -> 61.0; actual Java "
                        + Runtime.version() + "; original Main SHA256=" + actual);
                    return patched;
                } catch (Exception failure) {
                    failure.printStackTrace();
                    throw new IllegalClassFormatException(failure.toString());
                }
            }
        });
    }
}
