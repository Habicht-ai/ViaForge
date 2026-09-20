package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.nbt.tag.*;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class WaterColorsTest {
    @Test public void originalAquaticBiomesSurviveAndUnload() {
        WaterColors colors=new WaterColors();int[] biomes=new int[256];biomes[0]=44;biomes[1]=6;biomes[2]=50;
        colors.capture(chunk(biomes),0);
        assertEquals(0x43D5EE,colors.at(-16,64,32,0));assertEquals(0x617B64,colors.at(-15,64,32,0));assertEquals(0x3938C9,colors.at(-14,64,32,0));
        colors.unload(-1,2);assertEquals(-1,colors.at(-16,64,32,-1));
    }
    @Test public void negativeWorldHeightDoesNotShiftVisibleBiomeColors() {
        WaterColors colors=new WaterColors();int[] biomes=new int[24*64];Arrays.fill(biomes,44);
        Arrays.fill(biomes,4*64,4*64+16,6);Arrays.fill(biomes,4*64+16,4*64+32,46);
        colors.capture(chunk(biomes),-64);
        assertEquals(0x617B64,colors.at(-16,0,32,0));assertEquals(0x3D57D6,colors.at(-16,4,32,0));
        colors.clear();assertEquals(-1,colors.at(-16,0,32,-1));
    }
    @Test public void serverRegistryOverridesVanillaAndAcceptsModernHexColors() {
        CompoundTag root=new CompoundTag(),biomes=new CompoundTag(),entry=new CompoundTag(),element=new CompoundTag(),effects=new CompoundTag();
        effects.putInt("water_color",0x123456);element.put("effects",effects);entry.put("element",element);entry.putInt("id",44);entry.putString("name","minecraft:warm_ocean");
        ListTag<CompoundTag> entries=new ListTag<>(CompoundTag.class);entries.add(entry);biomes.put("value",entries);root.put("minecraft:worldgen/biome",biomes);
        WaterColors colors=new WaterColors();colors.registry(root);int[] ids=new int[256];Arrays.fill(ids,44);colors.capture(chunk(ids),0);
        assertEquals(0x123456,colors.at(-16,64,32,0));effects.putString("water_color","#a4b5c6");assertEquals(0xA4B5C6,WaterColors.color("custom:river",element));
    }
    @Test public void nativeAndLegacyProfilesKeepTheirExistingWater() {
        for(int version:new int[]{47,107,340,778})assertFalse(CompatibilityRegistry.DEFAULT.resolve(version).rules().enabled(ClientRule.BIOME_WATER_COLORS));
        for(int version:new int[]{393,404,735,776})assertTrue(CompatibilityRegistry.DEFAULT.resolve(version).rules().enabled(ClientRule.BIOME_WATER_COLORS));
    }
    private static Chunk chunk(int[] biomes){return new BaseChunk(-1,2,true,false,0,new ChunkSection[16],biomes,new ArrayList<>());}
}
