package com.viaversion.viaforge.blocks.resources;

import java.util.HashMap;
import java.util.Map;

/** Original 1.17 path textures, layered on the prior resource layouts. */
public final class CavesResourceConverter {
    public static Map<String,byte[]> convert(Map<String,byte[]> original) {
        Map<String,byte[]> source=new HashMap<>(original);
        for(String face:new String[]{"top","side"}) {
            String modern="textures/block/dirt_path_"+face+".png";
            if(!original.containsKey(modern))throw new IllegalArgumentException("Missing 1.17 texture "+modern);
            source.put("textures/block/grass_path_"+face+".png",original.get(modern));
        }
        for(String folder:new String[]{"blockstates/","models/block/","models/item/"}) {
            String modern=folder+"dirt_path.json";
            if(!original.containsKey(modern))throw new IllegalArgumentException("Missing 1.17 model "+modern);
            source.put(folder+"grass_path.json",original.get(modern));
        }
        return NetherResourceConverter.convert(source);
    }
    private CavesResourceConverter() { }
}
