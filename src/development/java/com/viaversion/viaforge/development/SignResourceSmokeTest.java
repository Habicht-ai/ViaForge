package com.viaversion.viaforge.development;

import com.google.gson.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.imageio.ImageIO;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** All visible ModelSign faces checked against the original 26.2 model's UVs. */
final class SignResourceSmokeTest {
    static void verify(Map<String,byte[]> original,Map<String,byte[]> converted)throws Exception {
        BufferedImage texture=ImageIO.read(new ByteArrayInputStream(original.get("textures/block/oak_sign.png")));
        BufferedImage atlas=ImageIO.read(new ByteArrayInputStream(converted.get("textures/entity/sign.png")));
        require(atlas.getWidth()==64&&atlas.getHeight()==32,"Native sign atlas dimensions");
        JsonArray elements=new JsonParser().parse(new String(original.get("models/block/template_sign_rot_0.json"),StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("elements");
        String[] faces={"south","north","west","east","up","down"};
        int[][] board={{2,2,24,12},{28,2,24,12},{0,2,2,12},{26,2,2,12},{2,0,24,2},{26,0,24,2}};
        int[][] post={{2,16,2,14},{6,16,2,14},{0,16,2,14},{4,16,2,14},null,{4,14,2,2}};
        for(int part=0;part<2;part++)for(int face=0;face<6;face++) {
            int[] nativeUv=(part==0?post:board)[face];if(nativeUv==null)continue; // post top lies inside the board
            JsonArray uv=elements.get(part).getAsJsonObject().getAsJsonObject("faces").getAsJsonObject(faces[face]).getAsJsonArray("uv");
            int x0=Math.round(uv.get(0).getAsFloat()*2),y0=Math.round(uv.get(1).getAsFloat()*2);
            require(Math.round((uv.get(2).getAsFloat()-uv.get(0).getAsFloat())*2)==nativeUv[2],"Original sign face width");
            require(Math.round((uv.get(3).getAsFloat()-uv.get(1).getAsFloat())*2)==nativeUv[3],"Original sign face height");
            for(int y=0;y<nativeUv[3];y++)for(int x=0;x<nativeUv[2];x++)
                require(atlas.getRGB(nativeUv[0]+x,nativeUv[1]+y)==texture.getRGB(x0+x,y0+(face==5?nativeUv[3]-1-y:y)),"Original sign face texel "+part+"/"+faces[face]);
        }
    }
    private SignResourceSmokeTest() { }
}
