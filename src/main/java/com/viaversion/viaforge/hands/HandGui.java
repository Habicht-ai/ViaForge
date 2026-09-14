package com.viaversion.viaforge.hands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
public final class HandGui {
    public static void emptySlot(int x,int y){
        if(com.viaversion.viaforge.compatibility.ServerSession.getLoadedResourceVersion()==null)return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("viaforge:textures/items/empty_armor_slot_shield.png"));
        GlStateManager.color(1,1,1,1);GlStateManager.enableAlpha();
        Gui.drawModalRectWithCustomSizedTexture(x,y,0,0,16,16,16,16);
    }
    private HandGui(){ }
}
