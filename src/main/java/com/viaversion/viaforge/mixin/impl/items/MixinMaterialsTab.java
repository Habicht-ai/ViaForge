package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class MixinMaterialsTab {
    @Inject(method = "getCreativeTab", at = @At("RETURN"), cancellable = true)
    private void materialCategory(CallbackInfoReturnable<CreativeTabs> ci) {
        if (ci.getReturnValue() == CreativeTabs.tabMaterials && ServerBlockSession.supportsProtocol(335)) ci.setReturnValue(CreativeTabs.tabMisc);
    }
}
