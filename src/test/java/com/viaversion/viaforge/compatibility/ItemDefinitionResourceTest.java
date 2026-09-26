package com.viaversion.viaforge.compatibility;

import com.google.gson.*;
import com.viaversion.viaforge.blocks.resources.ItemDefinitionResourceConverter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemDefinitionResourceTest {
    private Map<String,byte[]> assets(String definition) {
        Map<String,byte[]> assets=new HashMap<>();
        assets.put("items/compass.json",definition.getBytes(StandardCharsets.UTF_8));
        for(String name:new String[]{"zero","later","fallback"})assets.put("models/item/"+name+".json","{\"parent\":\"item/generated\"}".getBytes(StandardCharsets.UTF_8));
        return assets;
    }
    private String parent(Map<String,byte[]> assets) {
        return new JsonParser().parse(new String(ItemDefinitionResourceConverter.convert(assets).get("models/item/compass.json"),StandardCharsets.UTF_8)).getAsJsonObject().get("parent").getAsString();
    }
    @Test public void angularFirstFrameExistsWithoutFallback() {
        assertEquals("minecraft:item/zero",parent(assets("{\"model\":{\"type\":\"minecraft:condition\",\"on_false\":{\"type\":\"minecraft:range_dispatch\",\"entries\":[{\"threshold\":0,\"model\":{\"type\":\"minecraft:model\",\"model\":\"item/zero\"}},{\"threshold\":0.5,\"model\":{\"type\":\"minecraft:model\",\"model\":\"item/later\"}}]}}}")));
    }
    @Test public void positiveThresholdDoesNotOverrideZeroFallback() {
        assertEquals("minecraft:item/fallback",parent(assets("{\"model\":{\"type\":\"minecraft:range_dispatch\",\"entries\":[{\"threshold\":0.5,\"model\":{\"type\":\"minecraft:model\",\"model\":\"item/later\"}}],\"fallback\":{\"type\":\"minecraft:model\",\"model\":\"item/fallback\"}}}")));
    }
}
