package com.viaversion.viaforge.blocks.resources;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.util.Map;

/** Pixel-preserving conversion of the target sprite layout to EntityFX's 16-column atlas. */
public final class ParticleAtlasConverter {
    public static byte[] from1_13(byte[] png) {
        try {
            BufferedImage source=ImageIO.read(new ByteArrayInputStream(png));
            if(source==null||source.getWidth()!=source.getHeight()||source.getWidth()%32!=0)throw new IOException("Invalid 1.13 particle atlas");
            int cell=source.getWidth()/32;
            BufferedImage result=new BufferedImage(16*cell,16*cell,BufferedImage.TYPE_INT_ARGB);
            // 1.13 doubled the atlas grid to 32 columns; the existing 16 columns
            // of each of the first 16 rows retain their old semantic positions.
            for(int y=0;y<16*cell;y++)for(int x=0;x<16*cell;x++)result.setRGB(x,y,source.getRGB(x,y));
            // Five 2x2-cell bubble-pop frames live beyond the copied 1.13 rows.
            for(int frame=0;frame<5;frame++)copy(source,result,frame*2*cell,16*cell,frame*2*cell,12*cell,2*cell,2*cell);
            ByteArrayOutputStream output=new ByteArrayOutputStream();ImageIO.write(result,"png",output);return output.toByteArray();
        }catch(IOException error){throw new IllegalArgumentException("Cannot convert particle atlas",error);}
    }
    public static void from1_14(Map<String,byte[]> source, Map<String,byte[]> result) {
        BufferedImage atlas=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
        for(int i=0;i<8;i++) {
            sprite(source,atlas,i,"generic_"+i);
            sprite(source,atlas,128+i,"effect_"+i);
            sprite(source,atlas,144+i,"spell_"+i);
            sprite(source,atlas,160+i,"spark_"+i);
            sprite(source,atlas,176+i,"glitter_"+i);
        }
        int[] slots={16,19,20,21,22,32,48,49,64,65,66,67,80,81,82,112,113,114};
        String[] names={"splash_0","splash_0","splash_1","splash_2","splash_3","bubble","flame","lava","note","critical_hit","enchanted_hit","damage","heart","angry","glint","drip_hang","drip_fall","drip_land"};
        for(int i=0;i<slots.length;i++)sprite(source,atlas,slots[i],names[i]);
        for(int i=0;i<5;i++)copy(read(source,"textures/particle/bubble_pop_"+i+".png"),atlas,0,0,i*16,96,16,16);
        for(int i=0;i<26;i++)sprite(source,atlas,225+i,"sga_"+(char)('a'+i));
        result.put("textures/particle/particles.png",encode(atlas));
        result.put("textures/entity/explosion.png",frames(source,"explosion_",16,32,32,0));
        // Target sweep sprites add transparent vertical margins. Their central
        // 32x16 texels retain the legacy sheet geometry without resampling.
        result.put("textures/entity/sweep.png",frames(source,"sweep_",8,32,16,8));
        result.put("textures/particle/footprint.png",source.get("textures/particle/footprint.png"));
    }
    private static void sprite(Map<String,byte[]> source,BufferedImage atlas,int slot,String name) {
        BufferedImage image=read(source,"textures/particle/"+name+".png");
        if(image.getWidth()!=8||image.getHeight()!=8)throw new IllegalArgumentException("Unexpected sprite dimensions: "+name);
        copy(image,atlas,0,0,(slot%16)*8,(slot/16)*8,8,8);
    }
    private static byte[] frames(Map<String,byte[]> source,String name,int count,int width,int height,int offsetY) {
        BufferedImage atlas=new BufferedImage(width*4,height*(count/4),BufferedImage.TYPE_INT_ARGB);
        for(int i=0;i<count;i++) {
            BufferedImage frame=read(source,"textures/particle/"+name+i+".png");
            if(frame.getWidth()!=32||frame.getHeight()!=32)throw new IllegalArgumentException("Unexpected particle frame: "+name+i);
            copy(frame,atlas,0,offsetY,(i%4)*width,(i/4)*height,width,height);
        }
        return encode(atlas);
    }
    static BufferedImage read(Map<String,byte[]> source,String name) {
        byte[] data=source.get(name);if(data==null)throw new IllegalArgumentException("Missing target image: "+name);
        try {BufferedImage image=ImageIO.read(new ByteArrayInputStream(data));if(image==null)throw new IOException("Invalid PNG");return image;}
        catch(IOException error){throw new IllegalArgumentException("Cannot read "+name,error);}
    }
    private static void copy(BufferedImage source,BufferedImage target,int sx,int sy,int tx,int ty,int w,int h) {
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)target.setRGB(tx+x,ty+y,source.getRGB(sx+x,sy+y));
    }
    static byte[] encode(BufferedImage image) {
        try{ByteArrayOutputStream output=new ByteArrayOutputStream();ImageIO.write(image,"png",output);return output.toByteArray();}
        catch(IOException error){throw new IllegalStateException("Cannot write converted image",error);}
    }
    private ParticleAtlasConverter(){}
}
