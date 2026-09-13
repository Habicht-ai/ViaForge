package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ServerCombatState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemSword.class)
public abstract class MixinSwordUse {
    @Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
    private void newerSword(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> ci) { if (ServerCombatState.active()) ci.setReturnValue(stack); }
    @Inject(method = "getItemUseAction", at = @At("HEAD"), cancellable = true)
    private void newerAction(ItemStack stack, CallbackInfoReturnable<EnumAction> ci) { if (ServerCombatState.active()) ci.setReturnValue(EnumAction.NONE); }
}
