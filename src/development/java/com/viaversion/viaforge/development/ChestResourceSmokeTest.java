package com.viaversion.viaforge.development;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Map;
import javax.imageio.ImageIO;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Checks original target chest pixels on the native lid/base/latch UV surfaces. */
final class ChestResourceSmokeTest {
    static void verify(Map<String,byte[]> original,Map<String,byte[]> converted)throws Exception {
        for(String name:new String[]{"normal","trapped","christmas","ender"}) {
            String path="textures/entity/chest/"+name;
            BufferedImage modern=read(original,path+".png"),old=read(converted,path+".png");
            require(old.getWidth()==64&&old.getHeight()==64,"Native single chest atlas size");
            // Upper/lower lid faces and the front of each independently animated piece.
            for(int x=0;x<14;x++)for(int y=0;y<14;y++) {
                pixel(old,14+x,y,modern,28+x,13-y,name+" lid top");
                pixel(old,28+x,y,modern,14+x,13-y,name+" lid bottom");
            }
            for(int x=0;x<14;x++)for(int y=0;y<5;y++)pixel(old,14+x,14+y,modern,55-x,18-y,name+" lid front");
            for(int x=0;x<14;x++)for(int y=0;y<10;y++)pixel(old,14+x,33+y,modern,55-x,42-y,name+" base front");
            for(int x=0;x<2;x++)for(int y=0;y<4;y++)pixel(old,1+x,1+y,modern,5-x,4-y,name+" latch");
            if(name.equals("ender"))continue;
            BufferedImage left=read(original,path+"_left.png"),right=read(original,path+"_right.png");
            BufferedImage large=read(converted,path+"_double.png");
            require(large.getWidth()==128&&large.getHeight()==64,"Native double chest atlas size");
            // The old 30-pixel lid must span both original 15-pixel halves without
            // a duplicated half, swapped exterior wall or seam at the latch.
            for(int x=0;x<30;x++)for(int y=0;y<14;y++)pixel(large,14+x,y,x<15?right:left,29+x%15,13-y,name+" double top");
            for(int x=0;x<30;x++)for(int y=0;y<5;y++)pixel(large,14+x,14+y,x<15?right:left,57-x%15,18-y,name+" double lid front");
            for(int x=0;x<30;x++)for(int y=0;y<10;y++)pixel(large,14+x,33+y,x<15?right:left,57-x%15,42-y,name+" double base front");
        }
    }
    private static BufferedImage read(Map<String,byte[]> assets,String path)throws Exception {
        require(assets.containsKey(path),"Original/converted chest asset "+path);return ImageIO.read(new ByteArrayInputStream(assets.get(path)));
    }
    private static void pixel(BufferedImage old,int x,int y,BufferedImage modern,int xx,int yy,String label) {
        require(old.getRGB(x,y)==modern.getRGB(xx,yy),"Original target chest texel "+label+" "+x+","+y);
    }
    private ChestResourceSmokeTest() { }
}
