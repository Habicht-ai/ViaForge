package com.viaversion.viaforge.blocks.resources;

import com.google.gson.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Reassembles the pre-1.20.2 GUI sheets from the target's original split sprites. */
public final class GuiSpriteResourceConverter {
    public static Map<String,byte[]> convert(Map<String,byte[]> original) {
        Map<String,byte[]> source=new HashMap<>(original);
        try(InputStream input=GuiSpriteResourceConverter.class.getResourceAsStream("/assets/viaforge/gui-atlas-placements.json")) {
            if(input==null)throw new IllegalStateException("Missing GUI atlas placements");
            JsonObject atlases=new JsonParser().parse(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            for(Map.Entry<String,JsonElement> atlas:atlases.entrySet()) {
                BufferedImage sheet=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
                for(JsonElement entry:atlas.getValue().getAsJsonArray()) {
                    JsonObject row=entry.getAsJsonObject();String path="textures/gui/sprites/"+row.get("sprite").getAsString()+".png";
                    BufferedImage sprite=ParticleAtlasConverter.read(original,path);
                    int x=row.get("x").getAsInt(),y=row.get("y").getAsInt(),w=row.get("w").getAsInt(),h=row.get("h").getAsInt();
                    if(sprite.getWidth()!=w||sprite.getHeight()!=h)throw new IllegalArgumentException("Unexpected sprite dimensions "+path);
                    for(int dy=0;dy<h;dy++)for(int dx=0;dx<w;dx++)sheet.setRGB(x+dx,y+dy,sprite.getRGB(dx,dy));
                }
                source.put("textures/gui/"+atlas.getKey()+".png",ParticleAtlasConverter.encode(sheet));
            }
        }catch(IOException error){throw new IllegalStateException("Cannot rebuild GUI sheets",error);}
        if(original.containsKey("textures/block/short_grass.png"))source.put("textures/block/grass.png",original.get("textures/block/short_grass.png"));
        return CavesResourceConverter.convert(source);
    }
    private GuiSpriteResourceConverter() { }
}
