package com.viaversion.viaforge.common.compatibility;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.IntUnaryOperator;

/** Original wire-state fluid facts, generated with pinned report provenance. */
public final class SwimmingFluidRegistry {
    private static final Map<String,byte[]> TABLES=new HashMap<>();
    public static synchronized IntUnaryOperator forVersion(String version) {
        if(TABLES.isEmpty()) {
            Map<String,byte[]> loaded=new HashMap<>();
            try(InputStream stream=SwimmingFluidRegistry.class.getResourceAsStream("/assets/viaforge/fluid-states.json")) {
                if(stream==null)throw new IOException("Missing fluid state registry");
                JsonObject catalog=new JsonParser().parse(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
                for(Map.Entry<String,JsonElement> row:catalog.entrySet()) {
                    JsonObject entry=row.getValue().getAsJsonObject();byte[] table=new byte[entry.get("states").getAsInt()];
                    for(JsonElement element:entry.getAsJsonArray("fluids")) {
                        JsonArray run=element.getAsJsonArray();
                        Arrays.fill(table,run.get(0).getAsInt(),run.get(1).getAsInt()+1,run.get(2).getAsByte());
                    }
                    loaded.put(row.getKey(),table);
                }
            }catch(IOException e){throw new IllegalStateException(e);}
            TABLES.putAll(loaded);
        }
        final byte[] values=TABLES.get(version);
        if(values==null)throw new IllegalArgumentException("Unknown fluid registry "+version);
        return id->id>=0&&id<values.length?values[id]&255:0;
    }
    public static IntUnaryOperator forBoundary(Class<?> protocol) {
        String name=protocol.getSimpleName();
        if(!name.startsWith("Protocol")||!name.contains("To"))throw new IllegalArgumentException(name);
        return forVersion(name.substring(8,name.indexOf("To")).replace('_','.'));
    }
    private SwimmingFluidRegistry(){}
}
