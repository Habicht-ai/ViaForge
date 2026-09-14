package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.hands.HandGui;
import com.viaversion.viaforge.items.ServerCombatIndicator;
import com.viaversion.viaforge.items.ServerCombatState;
import com.viaversion.viaforge.mixin.impl.blocks.VersionTextureCache;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.profiler.Profiler;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Exercises the main-thread join interval before a downloaded pack is published. */
final class PendingResourceSmokeTest {
    static void verify() {
        Minecraft mc=Minecraft.getMinecraft();
        require(ServerSession.getLoadedResourceVersion()==null,"Test precedes target resource publication");
        EntityPlayerSP previous=mc.thePlayer;
        WorldClient world=new WorldClient(null,new WorldSettings(0,WorldSettings.GameType.CREATIVE,false,false,WorldType.DEFAULT),0,EnumDifficulty.NORMAL,new Profiler());
        NetHandlerPlayClient handler=new NetHandlerPlayClient(mc,null,new NetworkManager(EnumPacketDirection.CLIENTBOUND),new GameProfile(new UUID(0,17),"PendingResources"));
        Map<ResourceLocation,ITextureObject> textures=((VersionTextureCache)mc.getTextureManager()).viaForge$textures();
        ResourceLocation icons=new ResourceLocation("viaforge:textures/gui/icons.png");
        ResourceLocation shield=new ResourceLocation("viaforge:textures/items/empty_armor_slot_shield.png");
        require(!textures.containsKey(icons)&&!textures.containsKey(shield),"Old target HUD textures were evicted on join");
        try {
            mc.thePlayer=new EntityPlayerSP(mc,world,handler,new StatFileWriter());
            ServerCombatState.attack();
            require(ServerCombatState.strength(0)<1,"Attack indicator would draw while pack is pending");
            new ServerCombatIndicator().draw(320,240);
            HandGui.emptySlot(0,0);
            require(!textures.containsKey(icons)&&!textures.containsKey(shield),"Pending HUD assets must not be cached as missing textures");
        }finally{mc.thePlayer=previous;ServerCombatState.clear();}
    }
    private PendingResourceSmokeTest(){}
}
