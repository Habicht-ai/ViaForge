package com.viaversion.viaforge.compatibility;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.*;
import net.minecraft.util.ResourceLocation;

/** Ordered server overlays. A later push has priority; local packs stay in their repository. */
public final class ServerPackStack implements IResourcePack {
    private final List<IResourcePack> packs;
    public ServerPackStack(List<IResourcePack> packs){this.packs=new ArrayList<>(packs);}
    private ResourceLocation location(IResourcePack pack,ResourceLocation requested) {
        if(pack.resourceExists(requested))return requested;
        String path=requested.getResourcePath();
        // Static modern texture paths only. This does not claim modern model/atlas/overlay support.
        if(!path.startsWith("textures/"))return null;
        if(requested.getResourceDomain().equals("viaforge")) {
            ResourceLocation original=new ResourceLocation("minecraft",path);
            if(pack.resourceExists(original))return original;
        }
        if(requested.getResourceDomain().equals("minecraft")||requested.getResourceDomain().equals("viaforge")) {
            String modern=path.replace("textures/blocks/","textures/block/").replace("textures/items/","textures/item/");
            ResourceLocation original=new ResourceLocation("minecraft",modern);
            if(pack.resourceExists(original))return original;
        }
        return null;
    }
    @Override public boolean resourceExists(ResourceLocation location) {
        for(int i=packs.size()-1;i>=0;i--)if(location(packs.get(i),location)!=null)return true;return false;
    }
    @Override public InputStream getInputStream(ResourceLocation requested)throws IOException {
        for(int i=packs.size()-1;i>=0;i--) {
            IResourcePack pack=packs.get(i);ResourceLocation location=location(pack,requested);
            if(location!=null)return pack.getInputStream(location);
        }
        throw new FileNotFoundException(requested.toString());
    }
    @Override public Set<String> getResourceDomains() {
        Set<String> result=new HashSet<>();for(IResourcePack pack:packs)result.addAll(pack.getResourceDomains());
        if(result.contains("minecraft"))result.add("viaforge");return result;
    }
    @Override public <T extends IMetadataSection> T getPackMetadata(IMetadataSerializer serializer,String section){return null;}
    @Override public BufferedImage getPackImage(){return new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);}
    @Override public String getPackName(){return "ViaForge server packs ("+packs.size()+")";}
}
