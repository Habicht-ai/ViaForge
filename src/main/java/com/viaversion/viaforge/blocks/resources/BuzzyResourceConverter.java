package com.viaversion.viaforge.blocks.resources;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Original 1.15 chest surfaces projected back onto the native chest UV layout. */
public final class BuzzyResourceConverter {
    // old x/y, modern x/y, width/height, horizontal/vertical reflection.
    // Inverse of the original client's format-4 chest compatibility projection.
    private static final int[][] SINGLE = {
        {14,0,28,0,14,14,0,1},{28,0,14,0,14,14,0,1},
        {0,14,0,14,14,5,1,1},{14,14,42,14,14,5,1,1},{28,14,28,14,14,5,1,1},{42,14,14,14,14,5,1,1},
        {14,19,28,19,14,14,0,1},{28,19,14,19,14,14,0,1},
        {0,33,0,33,14,10,1,1},{14,33,42,33,14,10,1,1},{28,33,28,33,14,10,1,1},{42,33,14,33,14,10,1,1},
        {1,0,3,0,2,1,0,1},{3,0,1,0,2,1,0,1},{0,1,0,1,1,4,1,1},{1,1,4,1,2,4,1,1},{3,1,3,1,1,4,1,1},{4,1,1,1,2,4,1,1}
    };
    private static final int[][] LEFT = {
        {29,0,29,0,15,14,0,1},{59,0,14,0,15,14,0,1},
        {29,14,43,14,15,5,1,1},{44,14,29,14,14,5,1,1},{58,14,14,14,15,5,1,1},
        {29,19,29,19,15,14,0,1},{59,19,14,19,15,14,0,1},
        {29,33,43,33,15,10,1,1},{44,33,29,33,14,10,1,1},{58,33,14,33,15,10,1,1},
        {2,0,2,0,1,1,0,1},{4,0,1,0,1,1,0,1},{2,1,3,1,1,4,1,1},{3,1,2,1,1,4,1,1},{4,1,1,1,1,4,1,1}
    };
    private static final int[][] RIGHT = {
        {14,0,29,0,15,14,0,1},{44,0,14,0,15,14,0,1},
        {0,14,0,14,14,5,1,1},{14,14,43,14,15,5,1,1},{73,14,14,14,15,5,1,1},
        {14,19,29,19,15,14,0,1},{44,19,14,19,15,14,0,1},
        {0,33,0,33,14,10,1,1},{14,33,43,33,15,10,1,1},{73,33,14,33,15,10,1,1},
        {1,0,2,0,1,1,0,1},{3,0,1,0,1,1,0,1},{0,1,0,1,1,4,1,1},{1,1,3,1,1,4,1,1},{5,1,1,1,1,4,1,1}
    };
    public static Map<String,byte[]> convert(Map<String,byte[]> original) {
        Map<String,byte[]> source = new HashMap<>(original);
        source.put("textures/entity/iron_golem.png", required(original, "textures/entity/iron_golem/iron_golem.png"));
        for (String name : new String[]{"normal", "trapped", "christmas", "ender"}) {
            String path = "textures/entity/chest/" + name;
            BufferedImage single = ParticleAtlasConverter.read(original, path + ".png");
            int scale = single.getWidth() / 64;
            if (scale < 1 || single.getHeight() != 64 * scale) throw new IllegalArgumentException("Invalid chest texture " + path);
            BufferedImage old = new BufferedImage(64 * scale, 64 * scale, BufferedImage.TYPE_INT_ARGB);
            project(single, old, SINGLE, scale);
            source.put(path + ".png", ParticleAtlasConverter.encode(old));
            if (!name.equals("ender")) {
                BufferedImage left = ParticleAtlasConverter.read(original, path + "_left.png");
                BufferedImage right = ParticleAtlasConverter.read(original, path + "_right.png");
                BufferedImage doubled = new BufferedImage(128 * scale, 64 * scale, BufferedImage.TYPE_INT_ARGB);
                project(left, doubled, LEFT, scale); project(right, doubled, RIGHT, scale);
                source.put(path + "_double.png", ParticleAtlasConverter.encode(doubled));
            }
        }
        return VillageResourceConverter.convert(source);
    }
    private static byte[] required(Map<String,byte[]> source, String path) {
        byte[] value=source.get(path); if(value==null)throw new IllegalArgumentException("Missing 1.15 resource " + path); return value;
    }
    private static void project(BufferedImage modern, BufferedImage old, int[][] faces, int scale) {
        if(modern.getWidth()!=64*scale||modern.getHeight()!=64*scale)throw new IllegalArgumentException("Mismatched chest half resolution");
        for(int[] face:faces)for(int y=0;y<face[5]*scale;y++)for(int x=0;x<face[4]*scale;x++) {
            int oldX=face[0]*scale+(face[6]==1?face[4]*scale-1-x:x);
            int oldY=face[1]*scale+(face[7]==1?face[5]*scale-1-y:y);
            old.setRGB(oldX,oldY,modern.getRGB(face[2]*scale+x,face[3]*scale+y));
        }
    }
    private BuzzyResourceConverter() { }
}
