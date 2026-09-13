package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ClientItems;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** The client must allow charging a bow when only the new arrow types are present. */
@Mixin(ItemBow.class)
public abstract class MixinBowArrows {
    @Redirect(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;hasItem(Lnet/minecraft/item/Item;)Z"))
    private boolean arrows(InventoryPlayer inventory, Item item) {
        if (inventory.hasItem(item)) return true;
        if (item == Items.arrow) for (ItemStack stack : inventory.mainInventory) if (ClientItems.arrow(stack) && stack.stackSize > 0) return true;
        return false;
    }
}
