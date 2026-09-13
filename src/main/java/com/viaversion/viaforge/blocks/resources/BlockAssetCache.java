package com.viaversion.viaforge.blocks.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Downloads are pinned to Mojang's published hashes; no Minecraft assets enter the mod JAR. */
public final class BlockAssetCache {
    private final Path cache;

    public BlockAssetCache(Path cache) { this.cache = cache; }

    public Map<String, byte[]> load(String version) throws IOException {
        JsonObject entry;
        try (InputStream stream = BlockAssetCache.class.getResourceAsStream("/assets/viaforge/block-versions.json")) {
            if (stream == null) throw new IOException("Missing block resource catalog");
            JsonObject catalog = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!catalog.has(version)) throw new IOException("Unsupported block resource version: " + version);
            entry = catalog.getAsJsonObject(version);
        }
        String hash = entry.get("sha1").getAsString();
        long size = entry.get("size").getAsLong();
        Path jar = cache.resolve(version + "-client.jar");
        Files.createDirectories(cache);
        if (!valid(jar, size, hash)) {
            Path installed = new File(System.getProperty("user.home"), "AppData/Roaming/.minecraft/versions/"
                    + version + "/" + version + ".jar").toPath();
            Path temporary = Files.createTempFile(cache, version + "-", ".part");
            try {
                if (valid(installed, size, hash)) Files.copy(installed, temporary, StandardCopyOption.REPLACE_EXISTING);
                else download(entry.get("url").getAsString(), temporary, size);
                if (!valid(temporary, size, hash)) throw new IOException("Minecraft " + version + " resource checksum mismatch");
                Files.move(temporary, jar, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        Map<String, byte[]> assets = new HashMap<>(readAssets(jar));
        if (version.startsWith("1.11") || version.startsWith("1.12")) {
            // The 1.11 and 1.12 Mojang asset indexes reference this identical recording.
            String soundHash = "e7f0337931cdb05c4234d2a9bc1f38ead675db26";
            Path sound = cache.resolve(soundHash + ".ogg");
            if (!valid(sound, 35952, soundHash)) {
                Path temporary = Files.createTempFile(cache, "totem-", ".part");
                try {
                    download("https://resources.download.minecraft.net/e7/" + soundHash, temporary, 35952);
                    if (!valid(temporary, 35952, soundHash)) throw new IOException("Totem sound checksum mismatch");
                    Files.move(temporary, sound, StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(temporary); }
            }
            assets.put("sounds/item/totem/use_totem.ogg", Files.readAllBytes(sound));
        }
        return Collections.unmodifiableMap(assets);
    }

    public static Map<String, byte[]> readAssets(Path jar) throws IOException {
        Map<String, byte[]> assets = new HashMap<>();
        int total = 0;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith("assets/minecraft/")) continue;
                String path = name.substring("assets/minecraft/".length());
                if (!(path.startsWith("textures/blocks/") || path.startsWith("models/block/") || path.startsWith("blockstates/")
                        || path.startsWith("models/item/") || path.startsWith("textures/entity/shulker/") || path.startsWith("textures/entity/bed/")
                        || path.equals("textures/entity/end_portal.png") || path.equals("textures/environment/end_sky.png")
                        || path.startsWith("textures/items/") || path.startsWith("textures/entity/shield/")
                        || path.equals("textures/entity/shield_base.png") || path.equals("textures/entity/shield_base_nopattern.png")
                        || path.equals("textures/entity/elytra.png") || path.equals("textures/entity/enderdragon/dragon.png")
                        || path.startsWith("textures/entity/projectiles/") || path.equals("textures/particle/particles.png"))) continue;
                try (InputStream input = zip.getInputStream(entry)) {
                    byte[] data = readBounded(input, 2 * 1024 * 1024);
                    total += data.length;
                    if (total > 32 * 1024 * 1024) throw new IOException("Block resource archive exceeds size limit");
                    assets.put(path, data);
                }
            }
        }
        if (!assets.containsKey("textures/blocks/stone.png")) throw new IOException("Missing block textures in resource archive");
        return Collections.unmodifiableMap(assets);
    }

    private static boolean valid(Path path, long size, String hash) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) != size) return false;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[16384];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            StringBuilder actual = new StringBuilder();
            for (byte value : digest.digest()) actual.append(String.format("%02x", value & 255));
            return actual.toString().equals(hash);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void download(String address, Path target, long expectedSize) throws IOException {
        URL url = new URL(address);
        if (!"https".equals(url.getProtocol()) || !("piston-data.mojang.com".equals(url.getHost()) || "resources.download.minecraft.net".equals(url.getHost()))) {
            throw new IOException("Untrusted Minecraft resource address");
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(false);
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Minecraft resource download returned HTTP " + connection.getResponseCode());
            try (InputStream input = connection.getInputStream(); java.io.OutputStream output = Files.newOutputStream(target)) {
                byte[] buffer = new byte[16384];
                long total = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > expectedSize) throw new IOException("Minecraft resource download exceeds expected size");
                    output.write(buffer, 0, count);
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readBounded(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > limit) throw new IOException("Block resource exceeds size limit");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
}
