package com.viaversion.viaforge.items;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Definition;
import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/** Native food classification and eating animation, with server-confirmed consumption. */
final class ServerFood extends ItemFood {
    private final Definition definition;
    ServerFood(Definition definition) {
        super(definition.id == 432 ? 4 : definition.id == 434 ? 1 : 6, definition.id == 432 ? .3F : .6F, false);
        this.definition = definition; setMaxStackSize(definition.stackSize); setUnlocalizedName("viaforge." + definition.name);
        if (definition.id == 432) setAlwaysEdible();
    }
    @Override public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> entries) { if (ServerBlockSession.supportsItem(definition)) entries.add(new ItemStack(item)); }
    @Override public String getItemStackDisplayName(ItemStack stack) { return StatCollector.translateToLocal("item.viaforge." + definition.name + ".name"); }
    @Override public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        return world.isRemote && ServerBlockSession.supportsItem(definition) ? super.onItemRightClick(stack, world, player) : stack;
    }
    @Override public ItemStack onItemUseFinish(ItemStack stack, World world, EntityPlayer player) { return stack; }
}
