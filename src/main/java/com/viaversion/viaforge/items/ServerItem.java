package com.viaversion.viaforge.items;

import com.google.gson.*;
import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog.*;
import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.world.World;

/** Main-hand actions use the normal 1.8 packet path; the target server applies results. */
public class ServerItem extends Item {
    protected final Definition definition;
    public ServerItem(Definition definition) {
        this.definition = definition;
        setUnlocalizedName("viaforge." + definition.name); setMaxStackSize(definition.stackSize); setMaxDamage(definition.durability);
        setCreativeTab(ServerCreativeTabs.item(definition));
        if (definition.id == 437) setContainerItem(Items.glass_bottle);
    }
    @Override public CreativeTabs getCreativeTab() { return ServerCreativeTabs.item(definition); }
    @Override public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> entries) {
        if (!ServerBlockSession.supportsItem(definition)) return;
        if (potion()) {
            for (JsonElement element : ItemVariants.POTIONS) {
                JsonObject potion = element.getAsJsonObject(); String name = potion.get("name").getAsString();
                if (name.equals("empty") || definition.kind == Kind.TIPPED_ARROW && potion.get("effect").getAsInt() == 0) continue;
                entries.add(ItemVariants.potion(new ItemStack(item), name));
            }
        } else if (definition.kind == Kind.EGG) {
            for (JsonElement element : ItemVariants.EGGS) if (ServerBlockSession.supportsProtocol(element.getAsJsonObject().get("protocol").getAsInt())) entries.add(ItemVariants.egg(new ItemStack(item), element.getAsJsonObject()));
        } else entries.add(new ItemStack(item));
    }
    private boolean potion() { return definition.kind == Kind.POTION || definition.kind == Kind.SPLASH || definition.kind == Kind.LINGERING || definition.kind == Kind.TIPPED_ARROW; }
    @Override public String getItemStackDisplayName(ItemStack stack) {
        if (definition.kind == Kind.EGG) {
            JsonObject egg = ItemVariants.eggType(stack);
            if (egg != null) {
                String key = "entity." + egg.get("legacy").getAsString() + ".name";
                String entity = StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key) : ItemVariants.title(egg.get("name").getAsString());
                return StatCollector.translateToLocalFormatted("viaforge.item.spawn", entity);
            }
        }
        if (potion()) {
            String name = ItemVariants.potionName(stack).replace("long_", "").replace("strong_", "");
            String key = definition.name + ".effect." + name;
            return StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key) : ItemVariants.title(definition.name);
        }
        String key = "item.viaforge." + definition.name + ".name";
        return StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key) : ItemVariants.title(definition.name);
    }
    @Override public int getColorFromItemStack(ItemStack stack, int pass) {
        if (potion()) return pass == 0 ? ItemVariants.potionColor(stack) : 0xffffff;
        if (definition.kind == Kind.EGG) { JsonObject egg = ItemVariants.eggType(stack); if (egg != null) return egg.get(pass == 0 ? "base" : "overlay").getAsInt(); }
        return 0xffffff;
    }
    @Override public boolean hasEffect(ItemStack stack) { return super.hasEffect(stack) || potion() && definition.kind != Kind.TIPPED_ARROW && !ItemVariants.effects(stack).isEmpty(); }
    @Override public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        if (potion()) for (PotionEffect effect : ItemVariants.effects(stack)) {
            PotionEffect shown = new PotionEffect(effect);
            if (definition.kind == Kind.LINGERING) shown = new PotionEffect(effect.getPotionID(), effect.getDuration() / 4, effect.getAmplifier());
            if (definition.kind == Kind.TIPPED_ARROW) shown = new PotionEffect(effect.getPotionID(), Math.max(effect.getDuration() / 8, 1), effect.getAmplifier());
            lines.add((net.minecraft.potion.Potion.potionTypes[effect.getPotionID()].isBadEffect() ? EnumChatFormatting.RED : EnumChatFormatting.BLUE) + ItemVariants.effectTitle(shown));
        }
        if (definition.kind == Kind.SHIELD) Items.banner.addInformation(stack, player, lines, advanced);
    }
    @Override public EnumAction getItemUseAction(ItemStack stack) {
        switch (definition.kind) { case FOOD: case SOUP: return EnumAction.EAT; case POTION: return EnumAction.DRINK; case SHIELD: return EnumAction.BLOCK; default: return EnumAction.NONE; }
    }
    @Override public int getMaxItemUseDuration(ItemStack stack) {
        return definition.kind == Kind.SHIELD ? 72000 : getItemUseAction(stack) != EnumAction.NONE ? 32 : 0;
    }
    @Override public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote || !ServerBlockSession.supportsItem(definition)) return stack;
        EnumAction action = getItemUseAction(stack);
        if (action != EnumAction.NONE && (action != EnumAction.EAT || player.canEat(definition.id == 432))) player.setItemInUse(stack, getMaxItemUseDuration(stack));
        return stack;
    }
    @Override public ItemStack onItemUseFinish(ItemStack stack, World world, EntityPlayer player) {
        // Consumption, bowls/bottles, potion effects and chorus teleport are sent by the server.
        return stack;
    }
    @Override public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float x, float y, float z) {
        if (!world.isRemote || !ServerBlockSession.supportsItem(definition) || stack.stackSize <= 0) return false;
        switch (definition.kind) {
            case CRYSTAL:
                return (world.getBlockState(pos).getBlock() == Blocks.obsidian || world.getBlockState(pos).getBlock() == Blocks.bedrock)
                        && world.isAirBlock(pos.up()) && world.isAirBlock(pos.up(2));
            case EGG: return player.canPlayerEdit(pos.offset(side), side, stack);
            case HEAD: return side != EnumFacing.DOWN && player.canPlayerEdit(pos.offset(side), side, stack);
            default: return false;
        }
    }
    @Override public boolean isValidArmor(ItemStack stack, int armorType, net.minecraft.entity.Entity entity) { return definition.kind == Kind.HEAD && armorType == 0; }
    @Override public boolean getIsRepairable(ItemStack stack, ItemStack repair) { return definition.kind == Kind.SHIELD && repair.getItem() == Item.getItemFromBlock(Blocks.planks); }
}
