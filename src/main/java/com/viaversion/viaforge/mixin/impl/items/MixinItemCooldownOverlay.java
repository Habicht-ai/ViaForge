package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ServerItemCooldowns;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public abstract class MixinItemCooldownOverlay {
    @Inject(method = "renderItemOverlayIntoGUI", at = @At("RETURN"))
    private void cooldown(FontRenderer font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        ServerItemCooldowns.render(stack, x, y);
    }
}
