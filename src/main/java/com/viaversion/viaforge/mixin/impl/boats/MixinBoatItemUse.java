package com.viaversion.viaforge.mixin.impl.boats;

import com.viaversion.viaforge.boats.BoatPlacement;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog;
import com.viaversion.viaforge.items.ClientItems;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public abstract class MixinBoatItemUse {
    @Inject(method = "sendUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;useItemRightClick(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void boatUse(EntityPlayer player, World world, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (!ClientItems.is(stack, LegacyItemCatalog.Kind.BOAT)) return;
        // The normal use packet has already been sent. Modern SUCCESS also swings
        // in creative mode, where 1.8's stack-count comparison cannot detect it.
        boolean success = BoatPlacement.use(stack, world, player);
        if (success) {
            if (stack.stackSize <= 0) {
                player.inventory.mainInventory[player.inventory.currentItem] = null;
                net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, stack);
            }
            player.swingItem();
        }
        ci.setReturnValue(success);
    }
}
