package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.blocks.resources.BlockAssetCache;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlockAssetCacheTest {
    @Test
    public void readsOnlyBlockAssetsWithoutExtractingArchivePaths() throws Exception {
        Path jar = Files.createTempFile("viaforge-block-assets-", ".jar");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
                add(zip, "assets/minecraft/textures/blocks/stone.png", new byte[]{1, 2, 3});
                add(zip, "assets/minecraft/models/block/purpur_stairs.json", new byte[]{4});
                add(zip, "assets/minecraft/models/item/purpur_stairs.json", new byte[]{8});
                add(zip, "assets/minecraft/textures/entity/shulker/shulker_purple.png", new byte[]{9});
                add(zip, "assets/minecraft/textures/entity/bed/blue.png", new byte[]{11});
                add(zip, "assets/minecraft/textures/items/beetroot_seeds.png", new byte[]{10});
                add(zip, "assets/minecraft/textures/items/apple.png", new byte[]{5});
                add(zip, "example/Client.class", new byte[]{6});
                add(zip, "../../outside.txt", new byte[]{7});
            }
            Map<String, byte[]> assets = BlockAssetCache.readAssets(jar);
            assertEquals(7, assets.size());
            assertArrayEquals(new byte[]{11}, assets.get("textures/entity/bed/blue.png"));
            assertArrayEquals(new byte[]{1, 2, 3}, assets.get("textures/blocks/stone.png"));
            assertArrayEquals(new byte[]{5}, assets.get("textures/items/apple.png"));
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    @Test(expected = IOException.class)
    public void rejectsArchivesWithoutBlockTextures() throws Exception {
        Path jar = Files.createTempFile("viaforge-empty-assets-", ".jar");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
                add(zip, "unrelated.txt", new byte[]{1});
            }
            BlockAssetCache.readAssets(jar);
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    private static void add(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }
}
