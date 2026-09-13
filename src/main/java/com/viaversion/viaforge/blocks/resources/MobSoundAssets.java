package com.viaversion.viaforge.blocks.resources;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Downloads only the referenced recordings, verified against the pinned Mojang asset index. */
public final class MobSoundAssets {
    public static Map<String,byte[]> load(Path cache, String version) throws IOException {
        JsonObject catalog;
        try (InputStream input = MobSoundAssets.class.getResourceAsStream("/assets/viaforge/mob-asset-indexes.json")) {
            if (input == null) throw new IOException("Missing sound resource catalog");
            catalog = parse(input);
        }
        JsonObject pinned = catalog.getAsJsonObject(version);
        if (pinned == null) throw new IOException("Unknown sound resource version: "+version);
        Path indexes = cache.resolve("sounds"); Files.createDirectories(indexes);
        Path indexFile = cached(indexes,pinned.get("sha1").getAsString(),pinned.get("size").getAsLong(),pinned.get("url").getAsString());
        JsonObject index;
        try (InputStream input = Files.newInputStream(indexFile)) { index = parse(input).getAsJsonObject("objects"); }
        JsonObject definitions;
        try (InputStream input = Files.newInputStream(asset(indexes,index.getAsJsonObject("minecraft/sounds.json")))) { definitions = parse(input); }
        Set<String> events = new LinkedHashSet<>();
        for (Map.Entry<String,JsonElement> event : definitions.entrySet()) if (event.getKey().startsWith("entity.") || event.getKey().equals("enchant.thorns.hit")) events.add(event.getKey());
        Set<String> files = new LinkedHashSet<>(); JsonObject selected = new JsonObject();
        Deque<String> pending = new ArrayDeque<>(events);
        while (!pending.isEmpty()) {
            String name = pending.removeFirst();
            JsonObject event = definitions.getAsJsonObject(name);
            if (event == null) throw new IOException("Missing referenced sound event: "+name);
            selected.add(name,event);
            for (JsonElement value : event.getAsJsonArray("sounds")) {
                JsonObject sound = value.isJsonObject() ? value.getAsJsonObject() : null;
                String resource = sound == null ? value.getAsString() : sound.get("name").getAsString();
                if (sound != null && sound.has("type") && sound.get("type").getAsString().equals("event")) {
                    if (events.add(resource)) pending.addLast(resource);
                } else files.add("sounds/"+resource+".ogg");
            }
        }
        long total = 0;
        for (String file : files) {
            JsonObject entry = index.getAsJsonObject("minecraft/"+file);
            if (entry == null) throw new IOException("Missing sound recording: "+file);
            total += entry.get("size").getAsLong();
        }
        if (total > 64L*1024*1024) throw new IOException("Mob sounds exceed resource limit");
        Map<String,byte[]> result = new HashMap<>();
        ExecutorService downloads = Executors.newFixedThreadPool(6, task -> { Thread thread = new Thread(task,"ViaForge mob sounds"); thread.setDaemon(true); return thread; });
        List<Future<Map.Entry<String,byte[]>>> jobs = new ArrayList<>();
        try {
            for (String file : files) jobs.add(downloads.submit(() -> new AbstractMap.SimpleImmutableEntry<>(file,Files.readAllBytes(asset(indexes,index.getAsJsonObject("minecraft/"+file))))));
            for (Future<Map.Entry<String,byte[]>> job : jobs) {
                Map.Entry<String,byte[]> entry = job.get(); result.put(entry.getKey(),entry.getValue());
            }
        } catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IOException("Sound loading interrupted",error); }
        catch (ExecutionException error) { throw new IOException("Could not load mob sound",error.getCause()); }
        finally { downloads.shutdownNow(); }
        result.put("mob_sounds.json",selected.toString().getBytes(StandardCharsets.UTF_8));
        return result;
    }
    private static Path asset(Path cache, JsonObject entry) throws IOException {
        if (entry == null) throw new IOException("Missing asset index entry");
        String hash = entry.get("hash").getAsString();
        return cached(cache,hash,entry.get("size").getAsLong(),"https://resources.download.minecraft.net/"+hash.substring(0,2)+"/"+hash);
    }
    private static Path cached(Path cache,String hash,long size,String url) throws IOException {
        if (!hash.matches("[0-9a-f]{40}") || size < 0 || size > 8*1024*1024) throw new IOException("Invalid sound asset entry");
        Path file = cache.resolve(hash);
        if (!BlockAssetCache.valid(file,size,hash)) {
            Path temporary = Files.createTempFile(cache,"asset-",".part");
            try {
                Path installed = Paths.get(System.getProperty("user.home"),"AppData/Roaming/.minecraft/assets/objects",hash.substring(0,2),hash);
                if (BlockAssetCache.valid(installed,size,hash)) Files.copy(installed,temporary,StandardCopyOption.REPLACE_EXISTING);
                else BlockAssetCache.download(url,temporary,size);
                if (!BlockAssetCache.valid(temporary,size,hash)) throw new IOException("Sound asset checksum mismatch: "+hash);
                Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temporary); }
        }
        return file;
    }
    private static JsonObject parse(InputStream input) { return new JsonParser().parse(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject(); }
    private MobSoundAssets() { }
}
