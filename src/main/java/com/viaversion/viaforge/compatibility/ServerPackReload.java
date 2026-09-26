package com.viaversion.viaforge.compatibility;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import com.viaversion.viaforge.mixin.impl.blocks.MinecraftResourcePacks;
import com.viaversion.viaforge.mixin.impl.connect.ServerPackRepositoryAccess;

/** A failed server reload must not clear the user's selected resource packs. */
public final class ServerPackReload {
    public static void install(IResourcePack pack) {
        Minecraft mc=Minecraft.getMinecraft();ResourcePackRepository repository=mc.getResourcePackRepository();
        IResourcePack previous=repository.getResourcePackInstance();
        List<IResourcePack> base=new ArrayList<>(((MinecraftResourcePacks)mc).viaForge$defaultResourcePacks());
        for(ResourcePackRepository.Entry entry:repository.getRepositoryEntries())base.add(entry.getResourcePack());
        List<IResourcePack> requested=new ArrayList<>(base);if(pack!=null)requested.add(pack);
        SimpleReloadableResourceManager resources=(SimpleReloadableResourceManager)mc.getResourceManager();
        try {resources.reloadResources(requested);}
        catch(RuntimeException failure) {
            if(previous!=null)base.add(previous);
            try {resources.reloadResources(base);}catch(RuntimeException rollback){failure.addSuppressed(rollback);}
            throw failure;
        }
        ((ServerPackRepositoryAccess)repository).viaForge$serverPack(pack);
        mc.getLanguageManager().parseLanguageMetadata(requested);
        if(mc.renderGlobal!=null)mc.renderGlobal.loadRenderers();
    }
    private ServerPackReload(){}
}
