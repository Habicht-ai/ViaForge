package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemPotion.class, ItemMonsterPlacer.class})
public abstract class MixinCreativeItemVariants {
    @Inject(method = "getSubItems", at = @At("HEAD"), cancellable = true)
    private void replace(Item item, CreativeTabs tab, List<ItemStack> entries, CallbackInfo ci) {
        if (ServerBlockSession.supportsProtocol(107)) ci.cancel();
    }
}
