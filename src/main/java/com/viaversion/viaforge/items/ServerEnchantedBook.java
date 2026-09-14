package com.viaversion.viaforge.items;

import com.viaversion.viaforge.compatibility.ServerSession;
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
        if (!ServerSession.supportsItem(definition)) return;
        for (int id : new int[]{9, 70, 10, 71, 22}) {
            if ((id == 10 || id == 71) && !ServerSession.contentSince(315)) continue;
            if (id == 22 && !ServerSession.contentSince(316)) continue;
            boolean search = tab == null || tab == CreativeTabs.tabAllSearch;
            if (!search && !((id == 9 || id == 10 || id == 22) && tab == CreativeTabs.tabCombat
                    || (id == 70 || id == 71) && (tab == CreativeTabs.tabTools || tab == CreativeTabs.tabCombat && ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.COMBAT_CURSE_BOOKS)))) continue;
            Enchantment enchantment = Enchantment.getEnchantmentById(id);
            for (int level = search ? 1 : enchantment.getMaxLevel(); level <= enchantment.getMaxLevel(); level++) {
                ItemStack stack = new ItemStack(item); addEnchantment(stack, new EnchantmentData(enchantment, level)); entries.add(stack);
            }
        }
    }
}
