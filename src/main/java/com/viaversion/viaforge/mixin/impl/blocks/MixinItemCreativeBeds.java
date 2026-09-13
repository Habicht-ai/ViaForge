package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog;
import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public abstract class MixinItemCreativeBeds {
    @Inject(method = "getSubItems", at = @At("HEAD"), cancellable = true)
    private void replaceLegacyBedEntry(Item item, CreativeTabs tab, List<ItemStack> items, CallbackInfo ci) {
        if ((Object) this == Items.bed && ServerBlockSession.supportsItem(LegacyBlockCatalog.state(26 << 4))) ci.cancel();
    }
}
