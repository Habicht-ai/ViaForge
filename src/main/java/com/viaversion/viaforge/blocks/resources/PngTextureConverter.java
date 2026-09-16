package com.viaversion.viaforge.blocks.resources;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Java 8 ignores grayscale/RGB PNG color-key transparency. Its gray ColorSpace
 * also changes the samples' brightness. Expand these PNGs before any atlas or
 * entity texture consumer sees them, retaining the original encoded samples. */
public final class PngTextureConverter {
    public static Map<String, byte[]> normalize(Map<String, byte[]> source) {
        Map<String, byte[]> result = new HashMap<>(source);
        for (Map.Entry<String, byte[]> entry : source.entrySet()) {
            if (!entry.getKey().endsWith(".png")) continue;
            String path = entry.getKey();
            // LayeredColorMaskTexture accepts only TYPE_4BYTE_ABGR masks and
            // allocates its result using the base image's type (never a palette).
            boolean mask = path.startsWith("textures/entity/banner") || path.startsWith("textures/entity/shield");
            try { result.put(path, normalize(entry.getValue(), mask)); }
            catch (IOException error) { throw new IllegalArgumentException("Invalid texture " + entry.getKey(), error); }
        }
        return result;
    }

    public static byte[] normalize(byte[] png) throws IOException {
        return normalize(png, false);
    }
    public static byte[] normalize(byte[] png, boolean forceRgba) throws IOException {
        if (png.length < 33 || readInt(png, 0) != 0x89504E47 || readInt(png, 4) != 0x0D0A1A0A)
            throw new IOException("Invalid PNG header");
        int depth = png[24] & 255, type = png[25] & 255;
        if (type == 6 || !forceRgba && type != 0 && type != 2 && type != 4) return png;
        byte[] transparent = null;
        for (int offset = 8; offset <= png.length - 12;) {
            int size = readInt(png, offset);
            if (size < 0 || size > png.length - offset - 12) throw new IOException("Invalid PNG chunk");
            if (readInt(png, offset + 4) == 0x74524E53) transparent = Arrays.copyOfRange(png, offset + 8, offset + 8 + size);
            offset += size + 12;
        }
        if (type == 2 && transparent == null && !forceRgba) return png;
        if (type != 3 && transparent != null && transparent.length != (type == 0 ? 2 : 6)) throw new IOException("Invalid PNG transparency");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(png));
        if (decoded == null) throw new IOException("Unreadable PNG");
        Raster samples = decoded.getRaster();
        BufferedImage rgba = new BufferedImage(decoded.getWidth(), decoded.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int max = (1 << depth) - 1;
        for (int y = 0; y < rgba.getHeight(); y++) for (int x = 0; x < rgba.getWidth(); x++) {
            if (type == 3) { rgba.setRGB(x, y, decoded.getRGB(x, y)); continue; }
            int r = samples.getSample(x, y, 0), g = r, b = r, a = 255;
            if (type == 2) { g = samples.getSample(x, y, 1); b = samples.getSample(x, y, 2); }
            if (type == 4) a = samples.getSample(x, y, 1) * 255 / max;
            if (transparent != null && r == readShort(transparent, 0)
                    && (type == 0 || g == readShort(transparent, 2) && b == readShort(transparent, 4))) a = 0;
            rgba.setRGB(x, y, a << 24 | (r * 255 / max) << 16 | (g * 255 / max) << 8 | b * 255 / max);
        }
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        if (!ImageIO.write(rgba, "PNG", encoded)) throw new IOException("No PNG encoder");
        return encoded.toByteArray();
    }
    private static int readShort(byte[] data, int offset) { return (data[offset] & 255) << 8 | data[offset + 1] & 255; }
    private static int readInt(byte[] data, int offset) { return readShort(data, offset) << 16 | readShort(data, offset + 2); }
    private PngTextureConverter() { }
}
