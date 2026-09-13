package com.viaversion.viaforge.items;

import com.viaversion.viaforge.mixin.impl.items.SoundResourceAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.util.ResourceLocation;

public final class ServerTotemSound {
    public static void reload() {
        Minecraft mc = Minecraft.getMinecraft();
        try (java.io.InputStream stream = mc.getResourceManager().getResource(new ResourceLocation("viaforge", "sounds/item/totem/use_totem.ogg")).getInputStream()) {
            SoundList sounds = new SoundList(); sounds.setReplaceExisting(true); sounds.setSoundCategory(SoundCategory.PLAYERS);
            SoundList.SoundEntry entry = new SoundList.SoundEntry(); entry.setSoundEntryName("viaforge:item/totem/use_totem"); sounds.getSoundList().add(entry);
            ((SoundResourceAccess)mc.getSoundHandler()).viaForge$loadSound(new ResourceLocation("viaforge", "totem_use"), sounds);
        } catch (java.io.IOException missingBeforeJoin) { }
    }
    private ServerTotemSound() { }
}
