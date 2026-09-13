package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ServerItemCooldowns;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public abstract class MixinItemCooldownUse {
    @Inject(method = "sendUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;useItemRightClick(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void cooldown(EntityPlayer player, World world, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (ServerItemCooldowns.cooling(stack)) ci.setReturnValue(false);
        else if (com.viaversion.viaforge.blocks.ServerBlockSession.supportsProtocol(107)
                && stack.getItem() == net.minecraft.init.Items.ender_pearl && player.capabilities.isCreativeMode) {
            stack.useItemRightClick(world, player);
            // Modern creative pearl use succeeds without changing the stack count.
            ci.setReturnValue(true);
        }
    }
    @Redirect(method = "onPlayerRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;onItemUse(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/util/EnumFacing;FFF)Z"))
    private boolean cooldownOnBlock(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float x, float y, float z) {
        // Activating a chest/door and sending the use packet still happen first,
        // just as in the target controller. Only the item's action is delayed.
        return !ServerItemCooldowns.cooling(stack) && stack.onItemUse(player, world, pos, side, x, y, z);
    }
}
