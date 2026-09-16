package com.viaversion.viaforge.blocks.resources;

import java.util.*;

/** 1.21.2 equipment textures keep their UVs but moved to equipment layer paths. */
public final class EquipmentResourceConverter {
    public static Map<String,byte[]> convert(Map<String,byte[]> source) {
        Map<String,byte[]> normalized=ItemDefinitionResourceConverter.convert(source);
        for(Map.Entry<String,byte[]> entry:source.entrySet()) {
            String path=entry.getKey();
            if(path.startsWith("textures/entity/equipment/llama_body/"))normalized.put(path.replace("equipment/llama_body/","llama/decor/"),entry.getValue());
            for(int layer=1;layer<=2;layer++) {
                String prefix="textures/entity/equipment/"+(layer==1?"humanoid/":"humanoid_leggings/");
                if(!path.startsWith(prefix)||!path.endsWith(".png"))continue;
                String name=path.substring(prefix.length(),path.length()-4);
                boolean overlay=name.endsWith("_overlay");if(overlay)name=name.substring(0,name.length()-8);
                normalized.put("textures/models/armor/"+name+"_layer_"+layer+(overlay?"_overlay":"")+".png",entry.getValue());
            }
        }
        required(normalized,"textures/entity/elytra.png","textures/entity/equipment/wings/elytra.png");
        if(source.containsKey("textures/item/elytra_broken.png")) {
            required(normalized,"textures/item/broken_elytra.png","textures/item/elytra_broken.png");
            required(normalized,"models/item/broken_elytra.json","models/item/elytra_broken.json");
            for(String slot:new String[]{"helmet","chestplate","leggings","boots","shield"})required(normalized,"textures/item/empty_armor_slot_"+slot+".png","textures/gui/sprites/container/slot/"+slot+".png");
        }
        return GuiSpriteResourceConverter.convert(normalized);
    }
    private static void required(Map<String,byte[]> assets,String destination,String source){byte[] data=assets.get(source);if(data==null)throw new IllegalArgumentException("Missing equipment texture "+source);assets.put(destination,data);}
    private EquipmentResourceConverter() { }
}
