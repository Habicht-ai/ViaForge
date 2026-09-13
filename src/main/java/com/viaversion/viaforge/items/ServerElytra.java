package com.viaversion.viaforge.items;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Definition;
import java.util.List;
import net.minecraft.client.model.*;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;

/** Armor-slot compatibility without assigning chestplate armor points to elytra. */
public final class ServerElytra extends ItemArmor {
    private static final ArmorMaterial MATERIAL = EnumHelper.addArmorMaterial("VIAFORGE_ELYTRA", "viaforge:elytra", 1, new int[]{0,0,0,0}, 0);
    private final Definition definition;
    private final ModelBiped wings = new Wings();
    public ServerElytra(Definition definition) {
        super(MATERIAL, 0, 1); this.definition = definition;
        setMaxDamage(432); setUnlocalizedName("viaforge.elytra"); setCreativeTab(CreativeTabs.tabTransport);
    }
    @Override public String getItemStackDisplayName(ItemStack stack) { return "Elytra"; }
    @Override public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> entries) { if (ServerBlockSession.supportsItem(definition)) entries.add(new ItemStack(item)); }
    @Override public boolean getIsRepairable(ItemStack stack, ItemStack repair) { return repair.getItem() == Items.leather; }
    @Override public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        return world.isRemote && ServerBlockSession.supportsItem(definition) ? super.onItemRightClick(stack, world, player) : stack;
    }
    @Override public String getArmorTexture(ItemStack stack, Entity entity, int slot, String type) {
        if (ServerBlockSession.supportsItem(definition) && entity instanceof net.minecraft.client.entity.AbstractClientPlayer) {
            net.minecraft.client.entity.AbstractClientPlayer player = (net.minecraft.client.entity.AbstractClientPlayer)entity;
            if (player.hasPlayerInfo() && player.getLocationCape() != null && player.isWearing(net.minecraft.entity.player.EnumPlayerModelParts.CAPE)) return player.getLocationCape().toString();
        }
        return "viaforge:textures/entity/elytra.png";
    }
    @Override public ModelBiped getArmorModel(EntityLivingBase entity, ItemStack stack, int slot) { ((Wings)wings).pose(entity); return wings; }
    private static final class Wings extends ModelBiped {
        private final ModelRenderer left, right;
        private final java.util.Map<Entity, float[]> angles = new java.util.WeakHashMap<>();
        Wings() {
            super(0, 0, 64, 32);
            left = new ModelRenderer(this, 22, 0); left.addBox(-10, 0, 0, 10, 20, 2, 1);
            right = new ModelRenderer(this, 22, 0); right.mirror = true; right.addBox(0, 0, 0, 10, 20, 2, 1);
        }
        void pose(Entity entity) {
            float x = .2617994F, z = -.2617994F, y = 0, height = 0;
            ServerEntityViews.View view = ServerEntityViews.get(entity.getEntityId());
            if (view != null && view.fallFlying) {
                float blend = 1;
                if (entity.motionY < 0) {
                    double length = Math.sqrt(entity.motionX * entity.motionX + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ);
                    if (length > 0) blend = 1 - (float)Math.pow(-entity.motionY / length, 1.5);
                }
                x = blend * .34906584F + (1 - blend) * x; z = blend * -1.5707964F + (1 - blend) * z;
            } else if (entity.isSneaking()) { x = .6981317F; z = -.7853982F; height = 3; y = .08726646F; }
            if (entity instanceof net.minecraft.client.entity.AbstractClientPlayer) {
                float[] pose = angles.computeIfAbsent(entity, ignored -> new float[3]);
                pose[0] = (float)(pose[0] + (x - pose[0]) * .1); pose[1] = (float)(pose[1] + (y - pose[1]) * .1); pose[2] = (float)(pose[2] + (z - pose[2]) * .1);
                x = pose[0]; y = pose[1]; z = pose[2];
            }
            left.setRotationPoint(5, height, 0); left.rotateAngleX = x; left.rotateAngleZ = z; left.rotateAngleY = y;
            right.setRotationPoint(-5, height, 0); right.rotateAngleX = x; right.rotateAngleZ = -z; right.rotateAngleY = -y;
        }
        @Override public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.disableRescaleNormal(); net.minecraft.client.renderer.GlStateManager.disableCull();
            net.minecraft.client.renderer.GlStateManager.translate(0, 0, .125F);
            if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isChild()) {
                net.minecraft.client.renderer.GlStateManager.scale(.5F, .5F, .5F); net.minecraft.client.renderer.GlStateManager.translate(0, 1.5F, -.1F);
            }
            left.render(scale); right.render(scale); net.minecraft.client.renderer.GlStateManager.popMatrix();
        }
    }
}
