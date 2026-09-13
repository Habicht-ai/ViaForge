package com.viaversion.viaforge.items;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Definition;
import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.*;
import net.minecraft.item.*;

/** New enchantment variants use the same StoredEnchantments NBT as the target. */
final class ServerEnchantedBook extends ItemEnchantedBook {
    private final Definition definition;
    ServerEnchantedBook(Definition definition) { this.definition = definition; setMaxStackSize(1); setUnlocalizedName("enchantedBook"); setCreativeTab(CreativeTabs.tabAllSearch); }
    @Override public CreativeTabs[] getCreativeTabs() { return new CreativeTabs[]{CreativeTabs.tabTools, CreativeTabs.tabCombat}; }
    @Override public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> entries) {
        if (!ServerBlockSession.supportsItem(definition)) return;
        for (int id : new int[]{9, 70, 10, 71}) {
            if ((id == 10 || id == 71) && !ServerBlockSession.supportsProtocol(315)) continue;
            boolean search = tab == null || tab == CreativeTabs.tabAllSearch;
            if (!search && !((id == 9 || id == 10) && tab == CreativeTabs.tabCombat
                    || (id == 70 || id == 71) && (tab == CreativeTabs.tabTools || tab == CreativeTabs.tabCombat && ServerBlockSession.supportsProtocol(315)))) continue;
            Enchantment enchantment = Enchantment.getEnchantmentById(id);
            for (int level = search ? 1 : enchantment.getMaxLevel(); level <= enchantment.getMaxLevel(); level++) {
                ItemStack stack = new ItemStack(item); addEnchantment(stack, new EnchantmentData(enchantment, level)); entries.add(stack);
            }
        }
    }
}
