package com.viaversion.viaforge.mobs;

import net.minecraft.client.gui.inventory.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;

public final class LlamaInventoryScreen extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("viaforge", "textures/gui/container/horse.png");
    private final LlamaContainer container;
    private final InventoryPlayer player;
    public LlamaInventoryScreen(InventoryPlayer player, IInventory inventory, ServerMob llama) {
        super(new LlamaContainer(player, inventory, llama));
        this.container = (LlamaContainer)inventorySlots; this.player = player; allowUserInput = false;
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(container.inventory.getDisplayName().getUnformattedText(), 8, 6, 0x404040);
        fontRendererObj.drawString(player.getDisplayName().getUnformattedText(), 8, ySize - 94, 0x404040);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(com.viaversion.viaforge.compatibility.ServerSession.getLoadedResourceVersion()==null
                ? new ResourceLocation("textures/gui/container/horse.png") : TEXTURE);
        int x = (width - xSize) / 2, y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
        if (container.columns > 0) drawTexturedModalRect(x + 79, y + 17, 0, ySize, container.columns * 18, 54);
        drawTexturedModalRect(x + 7, y + 35, 36, ySize + 54, 18, 18);
        GuiInventory.drawEntityOnScreen(x + 51, y + 60, 17, x + 51 - mouseX, y + 25 - mouseY, container.llama);
    }
}
