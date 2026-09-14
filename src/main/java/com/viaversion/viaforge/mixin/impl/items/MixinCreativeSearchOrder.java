package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.items.ServerCreativeOrder;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainerCreative.class)
public abstract class MixinCreativeSearchOrder {
    @Inject(method = "updateCreativeSearch", at = @At("RETURN"))
    private void orderSearch(CallbackInfo ci) {
        GuiContainerCreative screen = (GuiContainerCreative)(Object)this;
        if (!ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.ITEMS) || screen.getSelectedTabIndex() != CreativeTabs.tabAllSearch.getTabIndex()) return;
        // Search builds its own list and appends native books after filtering setup.
        ServerCreativeOrder.sortSearch(screen.inventorySlots);
    }
}
