package com.viaversion.viaforge.blocks.resources;

import java.util.*;

/** Original 26.1 entity paths and 26.2 pillar side names in the inherited layout. */
public final class Year26ResourceConverter {
    public static Map<String,byte[]> convert(Map<String,byte[]> source) {
        Map<String,byte[]> assets=new HashMap<>(source);
        for (String texture : new String[]{"shield_base", "shield_base_nopattern", "banner_base"}) {
            String folder = texture.startsWith("shield") ? "shield" : "banner";
            String path = "textures/entity/" + folder + "/" + texture + ".png";
            if (source.containsKey(path)) copy(assets, "textures/entity/" + texture + ".png", path);
        }
        for(String color:new String[]{"brown","creamy","gray","white"})
            copy(assets,"textures/entity/llama/"+color+".png","textures/entity/llama/llama_"+color+".png");
        copy(assets,"textures/entity/cow/red_mooshroom.png","textures/entity/cow/mooshroom_red.png");
        copy(assets,"textures/entity/snow_golem.png","textures/entity/snow_golem/snow_golem.png");
        for(String block:new String[]{"purpur_pillar","quartz_pillar"})
            if(source.containsKey("textures/block/"+block+"_side.png"))
                copy(assets,"textures/block/"+block+".png","textures/block/"+block+"_side.png");
        if(source.containsKey("textures/block/oak_sign.png"))assets.put("textures/entity/signs/oak.png",SignTextureConverter.from26_2(source));
        return SpringResourceConverter.convert(assets);
    }
    private static void copy(Map<String,byte[]> assets,String old,String original) {
        byte[] data=assets.get(original);
        if(data==null)throw new IllegalArgumentException("Missing original 26.x texture "+original);
        assets.put(old,data);
    }
    private Year26ResourceConverter() { }
}
