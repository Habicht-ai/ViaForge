package com.viaversion.viaforge.development;

import com.google.gson.*;
import com.viaversion.nbt.tag.*;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import net.minecraft.client.Minecraft;

/** Full original data-pack entries, using a deterministic server registry order. */
final class OriginalRegistryFixtures {
    static RegistryEntry[] load(String version,String registry)throws IOException {
        File archive=new File(Minecraft.getMinecraft().mcDataDir,"ViaForge/block-assets/"+version+"-client.jar");
        String prefix="data/minecraft/"+registry+"/";Map<String,RegistryEntry> entries=new TreeMap<>();
        try(ZipFile zip=new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> files=zip.entries();
            while(files.hasMoreElements()) {
                ZipEntry entry=files.nextElement();String name=entry.getName();
                if(!name.startsWith(prefix)||!name.endsWith(".json"))continue;
                String key="minecraft:"+name.substring(prefix.length(),name.length()-5);
                try(Reader reader=new InputStreamReader(zip.getInputStream(entry),StandardCharsets.UTF_8)) {
                    entries.put(key,new RegistryEntry(key,nbt(new JsonParser().parse(reader))));
                }
            }
        }
        if(entries.isEmpty())throw new IOException("Missing original registry "+version+"/"+registry);
        return entries.values().toArray(new RegistryEntry[0]);
    }
    private static Tag nbt(JsonElement value) {
        if(value.isJsonObject()){CompoundTag tag=new CompoundTag();for(Map.Entry<String,JsonElement> entry:value.getAsJsonObject().entrySet())tag.put(entry.getKey(),nbt(entry.getValue()));return tag;}
        if(value.isJsonArray()){ListTag<Tag> tag=new ListTag<>();for(JsonElement child:value.getAsJsonArray())tag.add(nbt(child));return tag;}
        JsonPrimitive primitive=value.getAsJsonPrimitive();
        if(primitive.isBoolean())return new ByteTag(primitive.getAsBoolean());
        if(primitive.isNumber()){double number=primitive.getAsDouble();return number==(int)number?new IntTag((int)number):new DoubleTag(number);}
        return new StringTag(primitive.getAsString());
    }
    private OriginalRegistryFixtures() { }
}
