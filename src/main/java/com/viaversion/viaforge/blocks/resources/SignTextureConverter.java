package com.viaversion.viaforge.blocks.resources;

import java.awt.image.BufferedImage;
import java.util.Map;

/** Projects 26.2's block-model sign atlas onto native ModelSign's board/post UVs. */
public final class SignTextureConverter {
    public static byte[] from26_2(Map<String,byte[]> source) {
        BufferedImage modern=ParticleAtlasConverter.read(source,"textures/block/oak_sign.png");
        int scale=modern.getWidth()/32;
        if(scale<1||modern.getHeight()!=32*scale)throw new IllegalArgumentException("Invalid original sign texture");
        BufferedImage nativeAtlas=new BufferedImage(64*scale,32*scale,BufferedImage.TYPE_INT_ARGB);
        // native x/y, original x/y, width/height, flip V. Native rendering flips
        // the Z axis, so the board's north/south UV regions exchange world faces.
        int[][] faces={
            {2,2,0,2,24,12,0},{28,2,0,16,24,12,0},
            {0,2,24,16,2,12,0},{26,2,24,2,2,12,0},
            {2,0,0,0,24,2,0},{26,0,0,28,24,2,1},
            {2,16,28,0,2,14,0},{6,16,28,16,2,14,0},
            {0,16,30,16,2,14,0},{4,16,30,0,2,14,0},
            {4,14,28,30,2,2,1}
        };
        for(int[] face:faces)for(int y=0;y<face[5]*scale;y++)for(int x=0;x<face[4]*scale;x++)
            nativeAtlas.setRGB(face[0]*scale+x,face[1]*scale+y,modern.getRGB(face[2]*scale+x,face[3]*scale+(face[6]==0?y:face[5]*scale-1-y)));
        return ParticleAtlasConverter.encode(nativeAtlas);
    }
    private SignTextureConverter() { }
}
