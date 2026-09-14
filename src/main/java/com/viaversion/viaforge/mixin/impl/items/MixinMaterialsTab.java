package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class MixinMaterialsTab {
    @Inject(method = "getCreativeTab", at = @At("RETURN"), cancellable = true)
    private void materialCategory(CallbackInfoReturnable<CreativeTabs> ci) {
        if (ci.getReturnValue() == CreativeTabs.tabMaterials && ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.MERGED_MATERIALS_TAB)) ci.setReturnValue(CreativeTabs.tabMisc);
    }
}
