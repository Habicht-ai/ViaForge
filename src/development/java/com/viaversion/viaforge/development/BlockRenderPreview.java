package com.viaversion.viaforge.development;

import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.blocks.ShulkerBlockEntity;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Kind;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ScreenShotHelper;

/** Review artifact drawn by Minecraft's actual block, item and tile-entity renderers. */
final class BlockRenderPreview {
    static void capture(Path directory, WorldClient world) throws Exception {
        capture(directory, world, false);
        capture(directory, world, true);
    }
    private static void capture(Path directory, WorldClient world, boolean beds) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        TileEntityRendererDispatcher tileRenderer = TileEntityRendererDispatcher.instance;
        TextureManager previousRenderEngine = tileRenderer.renderEngine;
        tileRenderer.renderEngine = mc.getTextureManager();
        int width = 1440, height = 880;
        Framebuffer target = new Framebuffer(width, height, true);
        target.setFramebufferColor(.08F, .09F, .12F, 1);
        target.framebufferClear(); target.bindFramebuffer(true);
        GlStateManager.matrixMode(5889); GlStateManager.pushMatrix(); GlStateManager.loadIdentity();
        GlStateManager.ortho(0, width, height, 0, -1000, 1000);
        GlStateManager.matrixMode(5888); GlStateManager.pushMatrix(); GlStateManager.loadIdentity();
        try {
            RenderHelper.disableStandardItemLighting();
            mc.fontRendererObj.drawString("Forge 1.8.9 | Minecraft 1.12.2 " + (beds ? "colored beds" : "block resources") + " | left: world model / right: inventory", 20, 14, 0xffffff);
            int[] raws = {198 << 4 | 1, 199 << 4, 200 << 4, 201 << 4, 202 << 4, 203 << 4,
                    205 << 4, 206 << 4, 207 << 4 | 3, 208 << 4, 212 << 4 | 2, 213 << 4,
                    214 << 4, 215 << 4, 216 << 4, 218 << 4 | 2, 219 << 4 | 1, 229 << 4 | 1,
                    235 << 4, 241 << 4 | 1, 251 << 4, 251 << 4 | 14, 252 << 4 | 5, 255 << 4 | 3};
            if (beds) {
                raws = new int[16];
                for (int color = 0; color < 16; color++) raws[color] = com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.bedState(10, color);
            }
            BlockPos pos = new BlockPos(4, 70, 4);
            for (int i = 0; i < raws.length; i++) {
                int columns = beds ? 4 : 6, cellWidth = width / columns;
                int x = i % columns * cellWidth, y = 40 + i / columns * 205;
                RenderHelper.disableStandardItemLighting(); GlStateManager.disableDepth();
                Gui.drawRect(x + 8, y, x + cellWidth - 8, y + 194, 0xff252b35);
                IBlockState state = Block.BLOCK_STATE_IDS.getByValue(ClientBlocks.localState(raws[i]));
                Definition definition = ClientBlocks.definition(state.getBlock());
                mc.fontRendererObj.drawString(definition.name, x + 16, y + 10, 0xe4e8ef);
                mc.fontRendererObj.drawString("world", x + 30, y + 174, 0xb0bbc8);
                mc.fontRendererObj.drawString(definition.itemId() < 0 ? "no item" : "inventory", x + cellWidth - 96, y + 174, 0xb0bbc8);
                world.setBlockState(pos, state, 0);
                if (definition.kind == Kind.CHORUS) {
                    for (EnumFacing face : new EnumFacing[]{EnumFacing.UP, EnumFacing.DOWN, EnumFacing.EAST}) world.setBlockState(pos.offset(face), state, 0);
                }
                state = state.getBlock().getActualState(state, world, pos);
                GlStateManager.enableDepth(); GlStateManager.enableAlpha(); GlStateManager.enableRescaleNormal();
                RenderHelper.enableGUIStandardItemLighting();
                mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
                GlStateManager.pushMatrix(); GlStateManager.translate(x + (beds ? 104 : 62), y + 107, 0);
                float size = beds ? 68 : 78;
                GlStateManager.scale(size, -size, size); GlStateManager.rotate(25, 1, 0, 0); GlStateManager.rotate(225, 0, 1, 0);
                if (definition.kind == Kind.SHULKER) {
                    ShulkerBlockEntity tile = (ShulkerBlockEntity) world.getTileEntity(pos);
                    if (definition.id == 229) { tile.receiveClientEvent(1, 1); for (int tick = 0; tick < 10; tick++) tile.update(); }
                    GlStateManager.translate(-.5, -.5, -.5);
                    tileRenderer.<ShulkerBlockEntity>getSpecialRenderer(tile).renderTileEntityAt(tile, 0, 0, 0, 1, -1);
                } else {
                    GlStateManager.scale(2, 2, 2);
                    if (beds) GlStateManager.translate(0, 0, -.25);
                    mc.getRenderItem().renderItem(new ItemStack(Blocks.stone), mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state));
                    if (beds) {
                        IBlockState foot = state.withProperty(net.minecraft.block.BlockBed.PART, net.minecraft.block.BlockBed.EnumPartType.FOOT);
                        GlStateManager.translate(0, 0, .5);
                        mc.getRenderItem().renderItem(new ItemStack(Blocks.stone), mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(foot));
                    }
                }
                GlStateManager.popMatrix();
                for (EnumFacing face : EnumFacing.values()) world.setBlockToAir(pos.offset(face));
                world.setBlockToAir(pos);
                if (definition.itemId() >= 0) {
                    RenderHelper.enableGUIStandardItemLighting();
                    ItemStack stack = new ItemStack(Item.getItemById(ClientBlocks.localItem(definition.itemId(), definition.itemData())));
                    GlStateManager.pushMatrix(); GlStateManager.translate(x + cellWidth - 101, y + 66, 0); GlStateManager.scale(4, 4, 4);
                    mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
                    GlStateManager.popMatrix();
                }
            }
            RenderHelper.disableStandardItemLighting();
            Files.createDirectories(directory);
            String filename = beds ? "beds-preview-1.12.2.png" : "block-preview-1.12.2.png";
            ScreenShotHelper.saveScreenshot(directory.toFile(), filename, width, height, target);
            if (!Files.isRegularFile(directory.resolve("screenshots/" + filename))) throw new AssertionError("Missing render preview");
        } finally {
            tileRenderer.renderEngine = previousRenderEngine;
            GlStateManager.matrixMode(5888); GlStateManager.popMatrix();
            GlStateManager.matrixMode(5889); GlStateManager.popMatrix(); GlStateManager.matrixMode(5888);
            target.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true);
            GlStateManager.color(1, 1, 1, 1);
        }
    }
}
