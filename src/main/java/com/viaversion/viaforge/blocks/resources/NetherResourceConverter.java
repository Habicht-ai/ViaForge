package com.viaversion.viaforge.blocks.resources;

import java.util.HashMap;
import java.util.Map;

/** Nether-era target resources, composed over the chest and village conversions. */
public final class NetherResourceConverter {
    public static Map<String,byte[]> convert(Map<String,byte[]> original) {
        Map<String,byte[]> source=new HashMap<>(original);
        byte[] zombie=original.get("textures/entity/piglin/zombified_piglin.png");
        if(zombie==null)throw new IllegalArgumentException("Missing original zombified piglin texture");
        source.put("textures/entity/zombie_pigman.png",zombie);
        return BuzzyResourceConverter.convert(source);
    }
    private NetherResourceConverter() { }
}
