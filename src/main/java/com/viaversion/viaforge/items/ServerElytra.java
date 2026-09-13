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
    @Override public String getArmorTexture(ItemStack stack, Entity entity, int slot, String type) { return "viaforge:textures/entity/elytra.png"; }
    @Override public ModelBiped getArmorModel(EntityLivingBase entity, ItemStack stack, int slot) { return wings; }
    private static final class Wings extends ModelBiped {
        private final ModelRenderer left, right;
        Wings() {
            super(0, 0, 64, 32);
            left = new ModelRenderer(this, 22, 0); left.addBox(-10, 0, 0, 10, 20, 2, 1);
            right = new ModelRenderer(this, 22, 0); right.mirror = true; right.addBox(0, 0, 0, 10, 20, 2, 1);
        }
        @Override public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
            float x = .2617994F, z = -.2617994F;
            if (entity.isSneaking()) { x = .6981317F; z = -.7853982F; }
            left.setRotationPoint(5, entity.isSneaking() ? 3 : 0, 2); left.rotateAngleX = x; left.rotateAngleZ = z; left.rotateAngleY = entity.isSneaking() ? .08726646F : 0;
            right.setRotationPoint(-5, left.rotationPointY, 2); right.rotateAngleX = x; right.rotateAngleZ = -z; right.rotateAngleY = -left.rotateAngleY;
            left.render(scale); right.render(scale);
        }
    }
}
