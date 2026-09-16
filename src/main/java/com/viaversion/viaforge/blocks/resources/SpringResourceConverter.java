package com.viaversion.viaforge.blocks.resources;

import com.viaversion.viaforge.common.blocks.SpawnEggNames;
import java.util.*;

/** Each 1.21.5 egg uses its own original texture and model, without legacy tint. */
public final class SpringResourceConverter {
    public static Map<String,byte[]> convert(Map<String,byte[]> source) {
        Map<String,byte[]> assets=new HashMap<>(source);
        for(Map.Entry<String,String> egg:SpawnEggNames.MODERN.entrySet()) {
            String path="models/item/"+egg.getValue()+"_spawn_egg.json";
            byte[] model=source.get(path);if(model==null)throw new IllegalArgumentException("Missing original spawn egg model "+path);
            assets.put("models/item/egg/"+egg.getKey()+".json",model);
        }
        return EquipmentResourceConverter.convert(assets);
    }
    private SpringResourceConverter() { }
}
