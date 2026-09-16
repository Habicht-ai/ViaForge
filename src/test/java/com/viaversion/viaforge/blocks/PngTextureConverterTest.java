package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.blocks.resources.PngTextureConverter;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

public class PngTextureConverterTest {
    @Test public void grayscaleColorKeyAndBrightnessSurviveJava8() throws Exception {
        byte[] source = png(8, 0, new byte[]{0, 0, (byte)128, (byte)255}, new byte[]{0,0});
        BufferedImage image = read(PngTextureConverter.normalize(source));
        assertEquals(0x00000000, image.getRGB(0,0));
        assertEquals(0xFF808080, image.getRGB(1,0));
        assertEquals(0xFFFFFFFF, image.getRGB(2,0));
        assertEquals(BufferedImage.TYPE_4BYTE_ABGR, image.getType());
    }
    @Test public void rgbTransparencyDoesNotEraseOtherColors() throws Exception {
        byte[] source = png(8,2,new byte[]{0,1,2,3,1,2,4,4,5,6},new byte[]{0,1,0,2,0,3});
        BufferedImage image = read(PngTextureConverter.normalize(source));
        assertEquals(0x00010203, image.getRGB(0,0));
        assertEquals(0xFF010204, image.getRGB(1,0));
    }
    @Test public void packedGraySamplesAndPartialAlphaKeepTheirValues() throws Exception {
        BufferedImage packed = read(PngTextureConverter.normalize(png(2,0,new byte[]{0,0x1C},new byte[]{0,1})));
        assertEquals(0xFF000000,packed.getRGB(0,0));
        assertEquals(0x00555555,packed.getRGB(1,0));
        assertEquals(0xFFFFFFFF,packed.getRGB(2,0));
        BufferedImage alpha = read(PngTextureConverter.normalize(png(8,4,new byte[]{0,64,(byte)128,(byte)128,64,(byte)255,(byte)255},null)));
        assertEquals(0x80404040,alpha.getRGB(0,0));
        assertEquals(0x40808080,alpha.getRGB(1,0));
    }
    @Test public void indexedBannerMasksBecomeAbgrForNativeLayeredRenderer() throws Exception {
        BufferedImage indexed = new BufferedImage(3,1,BufferedImage.TYPE_BYTE_INDEXED);
        indexed.setRGB(0,0,0xFF669933);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(indexed,"png",bytes);
        BufferedImage converted = read(PngTextureConverter.normalize(bytes.toByteArray(),true));
        assertEquals(BufferedImage.TYPE_4BYTE_ABGR,converted.getType());
        assertEquals(indexed.getRGB(0,0),converted.getRGB(0,0));
    }
    private static BufferedImage read(byte[] png) throws IOException { return ImageIO.read(new ByteArrayInputStream(png)); }
    private static byte[] png(int depth,int type,byte[] row,byte[] transparency) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes);
        out.writeLong(0x89504E470D0A1A0AL);
        ByteArrayOutputStream header = new ByteArrayOutputStream(); DataOutputStream ihdr = new DataOutputStream(header);
        ihdr.writeInt(3);ihdr.writeInt(1);ihdr.writeByte(depth);ihdr.writeByte(type);ihdr.write(new byte[3]);
        chunk(out,"IHDR",header.toByteArray());
        if(transparency!=null)chunk(out,"tRNS",transparency);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try(DeflaterOutputStream deflater=new DeflaterOutputStream(compressed)){deflater.write(row);}
        chunk(out,"IDAT",compressed.toByteArray());chunk(out,"IEND",new byte[0]);return bytes.toByteArray();
    }
    private static void chunk(DataOutputStream out,String type,byte[] data)throws IOException {
        byte[] name=type.getBytes("US-ASCII");CRC32 crc=new CRC32();crc.update(name);crc.update(data);
        out.writeInt(data.length);out.write(name);out.write(data);out.writeInt((int)crc.getValue());
    }
}
