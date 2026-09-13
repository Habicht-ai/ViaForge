package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ServerCombatState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinCombatEquip {
    @Shadow private ItemStack itemToRender;
    @Shadow private float equippedProgress, prevEquippedProgress;
    @Shadow private int equippedItemSlot;
    @Inject(method = "updateEquippedItem", at = @At("HEAD"), cancellable = true)
    private void rechargeHand(CallbackInfo ci) {
        if (!ServerCombatState.active()) return;
        Minecraft mc = Minecraft.getMinecraft(); ItemStack held = mc.thePlayer.getHeldItem();
        prevEquippedProgress = equippedProgress;
        float strength = ServerCombatState.strength(1);
        boolean equal = ItemStack.areItemStacksEqual(itemToRender, held);
        float target = equal ? strength * strength * strength : 0;
        equippedProgress += MathHelper.clamp_float(target - equippedProgress, -.4F, .4F);
        if (equippedProgress < .1F) { itemToRender = held; equippedItemSlot = mc.thePlayer.inventory.currentItem; }
        ci.cancel();
    }
}
