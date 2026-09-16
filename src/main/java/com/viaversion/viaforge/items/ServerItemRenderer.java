package com.viaversion.viaforge.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.item.*;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.*;

/** Entity-backed item geometry, with the same UV layout as the target client. */
public final class ServerItemRenderer {
    private static final ModelBase SHIELD = new ModelBase() { };
    private static final ModelRenderer PLATE, HANDLE;
    private static final ModelBase DRAGON = new ModelBase() { };
    private static final ModelRenderer HEAD, JAW;
    private static final Map<String, ResourceLocation> PATTERNS = new LinkedHashMap<>();
    static {
        SHIELD.textureWidth = SHIELD.textureHeight = 64;
        PLATE = new ModelRenderer(SHIELD, 0, 0); PLATE.addBox(-6, -11, -2, 12, 22, 1);
        HANDLE = new ModelRenderer(SHIELD, 26, 0); HANDLE.addBox(-1, -3, -1, 2, 6, 6);
        DRAGON.textureWidth = DRAGON.textureHeight = 256;
        HEAD = new ModelRenderer(DRAGON, 176, 44); HEAD.addBox(-6, -1, -24, 12, 5, 16);
        HEAD.setTextureOffset(112, 30).addBox(-8, -8, -10, 16, 16, 16);
        HEAD.mirror = true;
        HEAD.setTextureOffset(0, 0).addBox(-5, -12, -4, 2, 4, 6);
        HEAD.setTextureOffset(112, 0).addBox(-5, -3, -22, 2, 2, 4);
        HEAD.mirror = false;
        HEAD.setTextureOffset(0, 0).addBox(3, -12, -4, 2, 4, 6);
        HEAD.setTextureOffset(112, 0).addBox(3, -3, -22, 2, 2, 4);
        JAW = new ModelRenderer(DRAGON, 176, 65); JAW.addBox(-6, 0, -16, 12, 4, 16); JAW.setRotationPoint(0, 4, -8); HEAD.addChild(JAW);
    }
    public static boolean special(ItemStack stack) { return ClientItems.is(stack, Kind.SHIELD) || ClientItems.is(stack, Kind.HEAD); }
    public static void clearPatterns() { PATTERNS.clear(); }
    public static void render(ItemStack stack) {
        GlStateManager.pushMatrix();
        try {
            // Outer callers compensate the native RenderItem half-scale after applying display transforms.
            GlStateManager.scale(.5F, .5F, .5F); GlStateManager.translate(-.5F, -.5F, -.5F);
            GlStateManager.color(1, 1, 1, 1); GlStateManager.enableRescaleNormal();
            if (ClientItems.is(stack, Kind.SHIELD)) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(shieldTexture(stack));
                GlStateManager.scale(1, -1, -1); PLATE.render(.0625F); HANDLE.render(.0625F);
            } else renderDragon(.0F, .0F, .0F, EnumFacing.UP, inventoryDragonYaw(), 0);
        } finally {
            GlStateManager.popMatrix(); Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        }
    }
    public static float inventoryDragonYaw() { return ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.REVERSED_DRAGON_HEAD_ITEM) ? 180 : 0; }
    public static void renderDragon(float x, float y, float z, EnumFacing facing, float yaw, float animation) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("viaforge:textures/entity/enderdragon/dragon.png"));
        GlStateManager.pushMatrix(); GlStateManager.disableCull();
        try {
            switch (facing) {
                case NORTH: GlStateManager.translate(x + .5F, y + .25F, z + .74F); break;
                case SOUTH: GlStateManager.translate(x + .5F, y + .25F, z + .26F); yaw = 180; break;
                case WEST: GlStateManager.translate(x + .74F, y + .25F, z + .5F); yaw = 270; break;
                case EAST: GlStateManager.translate(x + .26F, y + .25F, z + .5F); yaw = 90; break;
                default: GlStateManager.translate(x + .5F, y, z + .5F);
            }
            GlStateManager.enableRescaleNormal(); GlStateManager.scale(-1, -1, 1); GlStateManager.enableAlpha();
            GlStateManager.translate(0, -.374375F, 0); GlStateManager.scale(.75F, .75F, .75F);
            HEAD.rotateAngleY = yaw * (float)Math.PI / 180;
            JAW.rotateAngleX = ((float)Math.sin(animation * (float)Math.PI * .2F) + 1) * .2F;
            HEAD.render(.0625F);
        } finally { GlStateManager.enableCull(); GlStateManager.popMatrix(); }
    }
    private static ResourceLocation shieldTexture(ItemStack stack) {
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("BlockEntityTag", 10)) return new ResourceLocation("viaforge:textures/entity/shield_base_nopattern.png");
        TileEntityBanner banner = new TileEntityBanner();
        ItemStack pattern = stack.copy(); pattern.setItemDamage(0); banner.setItemValues(pattern);
        String key = ServerSession.getLoadedResourceVersion() + "/" + banner.getPatternResourceLocation();
        ResourceLocation texture = PATTERNS.get(key);
        if (texture == null) {
            if (PATTERNS.size() >= 128) { Iterator<ResourceLocation> old = PATTERNS.values().iterator(); Minecraft.getMinecraft().getTextureManager().deleteTexture(old.next()); old.remove(); }
            texture = new ResourceLocation("viaforge:shield_patterns/" + key);
            List<String> layers = new ArrayList<>();
            for (TileEntityBanner.EnumBannerPattern layer : banner.getPatternList()) layers.add("viaforge:textures/entity/shield/" + layer.getPatternName() + ".png");
            Minecraft.getMinecraft().getTextureManager().loadTexture(texture, new LayeredColorMaskTexture(new ResourceLocation("viaforge:textures/entity/shield_base.png"), layers, banner.getColorList()));
            PATTERNS.put(key, texture);
        }
        return texture;
    }
    private ServerItemRenderer() { }
}
