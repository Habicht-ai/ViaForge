package com.viaversion.viaforge.items;
import com.viaversion.viaforge.compatibility.ServerSession;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Forge's crosshair event is also used by the actual GuiIngameForge HUD. */
public final class ServerCombatIndicator extends Gui {
    @SubscribeEvent public void overlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!ServerCombatState.active() || mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0
                || mc.gameSettings.showDebugInfo && !mc.thePlayer.hasReducedDebug() && !mc.gameSettings.reducedDebugInfo
                || mc.playerController.isSpectator() && mc.pointedEntity == null) return;
        draw(event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
    }
    public void draw(int width, int height) {
        // A session becomes active before its asynchronous target pack arrives.
        if (ServerSession.getLoadedResourceVersion() == null) return;
        Minecraft mc = Minecraft.getMinecraft(); float strength = ServerCombatState.strength(0);
        // The ready-to-sweep icon and the one-pixel centering correction arrived in 1.11.1.
        boolean modernIcon = com.viaversion.viaforge.compatibility.ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.MODERN_ATTACK_ICON);
        boolean ready = modernIcon && mc.pointedEntity instanceof EntityLivingBase && ((EntityLivingBase)mc.pointedEntity).isEntityAlive() && strength >= 1 && ServerCombatState.period() > 5;
        if (!ready && strength >= 1) return;
        int x = width / 2 - (modernIcon ? 8 : 7), y = height / 2 + 9;
        mc.getTextureManager().bindTexture(new ResourceLocation("viaforge", "textures/gui/icons.png"));
        GlStateManager.enableBlend(); GlStateManager.tryBlendFuncSeparate(775, 769, 1, 0); GlStateManager.enableAlpha();
        if (ready) drawTexturedModalRect(x, y, 68, 94, 16, 16);
        else { drawTexturedModalRect(x, y, 36, 94, 16, 4); drawTexturedModalRect(x, y, 52, 94, (int)(strength * 17), 4); }
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); GlStateManager.disableBlend(); mc.getTextureManager().bindTexture(icons);
    }
}
