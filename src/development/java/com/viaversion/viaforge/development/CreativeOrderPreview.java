package com.viaversion.viaforge.development;

import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.mixin.impl.items.CreativeContainerAccess;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;

/** Exercises the real creative screen's separate search list and scrollable item grid. */
final class CreativeOrderPreview {
    static void verify(BlockVersionProfile profile, Path directory) throws Exception {
        Minecraft mc = Minecraft.getMinecraft(); PlayerControllerMP previousController = mc.playerController;
        Container previousContainer = mc.thePlayer.openContainer;
        Field selected = GuiContainerCreative.class.getDeclaredField("selectedTabIndex"); selected.setAccessible(true); int oldTab = selected.getInt(null);
        Method select = GuiContainerCreative.class.getDeclaredMethod("setCurrentCreativeTab", CreativeTabs.class); select.setAccessible(true);
        Method search = GuiContainerCreative.class.getDeclaredMethod("updateCreativeSearch"); search.setAccessible(true);
        Field searchField = GuiContainerCreative.class.getDeclaredField("searchField"); searchField.setAccessible(true);
        GuiContainerCreative screen = null;
        try {
            mc.playerController = new PlayerControllerMP(mc, mc.thePlayer.sendQueue); mc.playerController.setGameType(WorldSettings.GameType.CREATIVE);
            screen = new GuiContainerCreative(mc.thePlayer); screen.setWorldAndResolution(mc, 340, 240);
            select.invoke(screen, CreativeTabs.tabAllSearch);
            CreativeContainerAccess container = (CreativeContainerAccess)screen.inventorySlots;
            ((GuiTextField)searchField.get(screen)).setText(""); search.invoke(screen);
            List<ItemStack> entries = container.viaForge$items();
            int previousId = -1, previousBook = -1;
            for (ItemStack stack : entries) {
                int id = Integer.parseInt(CreativeCategorySmokeTest.identity(stack).split(":")[0]);
                int rank = id == 403 && profile.protocol() < 335 ? Integer.MAX_VALUE : id;
                ServerEntitySmokeTest.require(rank >= previousId, "Creative search uses target registry order " + profile);
                previousId = rank;
                if (id == 403) {
                    net.minecraft.nbt.NBTTagCompound book = stack.getTagCompound().getTagList("StoredEnchantments", 10).getCompoundTagAt(0);
                    int rankBook = book.getShort("id") * 256 + book.getShort("lvl");
                    ServerEntitySmokeTest.require(rankBook > previousBook, "Search merges old/new enchantments and all levels"); previousBook = rankBook;
                }
            }
            if (profile.protocol() >= 335) {
                ((GuiTextField)searchField.get(screen)).setText("bed"); search.invoke(screen);
                List<String> beds = new ArrayList<>();
                for (ItemStack stack : container.viaForge$items()) if (CreativeCategorySmokeTest.identity(stack).startsWith("355:")) beds.add(CreativeCategorySmokeTest.identity(stack));
                List<String> expected = new ArrayList<>(); for (int color = 0; color < 16; color++) expected.add("355:" + color);
                ServerEntitySmokeTest.require(beds.equals(expected), "Filtered creative search preserves bed color order");
            }
            select.invoke(screen, CreativeTabs.tabDecorations);
            if (profile == BlockVersionProfile.V1_12_2) capture(screen, container, directory);
        } finally {
            if (screen != null) screen.onGuiClosed();
            selected.setInt(null, oldTab); mc.thePlayer.openContainer = previousContainer; mc.playerController = previousController;
        }
    }
    private static void capture(GuiContainerCreative screen, CreativeContainerAccess container, Path directory) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        int first = CreativeCategorySmokeTest.identities(container.viaForge$items()).indexOf("223:0"); // lime shulker, as in the comparison
        int rows = (container.viaForge$items().size() + 8) / 9 - 5;
        float scroll = (first / 9) / (float)rows;
        container.viaForge$scrollTo(scroll);
        Field current = GuiContainerCreative.class.getDeclaredField("currentScroll"); current.setAccessible(true); current.setFloat(screen, scroll);
        Framebuffer target = new Framebuffer(680, 480, true); target.setFramebufferColor(.08F, .09F, .12F, 1); target.framebufferClear(); target.bindFramebuffer(true);
        GlStateManager.matrixMode(5889); GlStateManager.pushMatrix(); GlStateManager.loadIdentity(); GlStateManager.ortho(0, 340, 240, 0, -1000, 1000);
        GlStateManager.matrixMode(5888); GlStateManager.pushMatrix(); GlStateManager.loadIdentity();
        try {
            GlStateManager.enableTexture2D(); GlStateManager.enableAlpha(); GlStateManager.enableDepth();
            screen.drawScreen(-100, -100, 0);
            ScreenShotHelper.saveScreenshot(directory.toFile(), "creative-order-1.12.2.png", 680, 480, target);
        } finally {
            GlStateManager.matrixMode(5888); GlStateManager.popMatrix(); GlStateManager.matrixMode(5889); GlStateManager.popMatrix(); GlStateManager.matrixMode(5888);
            target.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true); GlStateManager.color(1, 1, 1, 1);
        }
    }
}
