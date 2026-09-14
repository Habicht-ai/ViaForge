package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.items.ServerCreativeOrder;
import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.*;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeTabs.class)
public abstract class MixinBookCategories {
    @Inject(method = "displayAllReleventItems", at = @At("RETURN"))
    private void combatUnbreaking(List<ItemStack> entries, CallbackInfo ci) {
        if (!ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.ITEMS)) return;
        // 1.11 also includes BREAKABLE enchantments in Combat.
        if ((Object)this == CreativeTabs.tabCombat && ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.COMBAT_CURSE_BOOKS)) {
            entries.add(Items.enchanted_book.getEnchantedItemStack(new EnchantmentData(Enchantment.unbreaking, 3)));
        }
        if (((CreativeTabs)(Object)this).getTabIndex() < 12) ServerCreativeOrder.sort(entries);
    }
}
