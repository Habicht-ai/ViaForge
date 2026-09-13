package com.viaversion.viaforge.mobs;

import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.init.Blocks;

/** The original horse inventory's slot numbering, with llama carpet and strength columns. */
public final class LlamaContainer extends Container {
    public final IInventory inventory;
    public final ServerMob llama;
    public final int columns;
    public LlamaContainer(InventoryPlayer player, IInventory inventory, ServerMob llama) {
        this.inventory = inventory; this.llama = llama;
        columns = Math.max(0, Math.min(5, (inventory.getSizeInventory() - 2) / 3));
        inventory.openInventory(player.player);
        addSlotToContainer(new Slot(inventory, 0, 8, 18) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
            @Override public boolean canBeHovered() { return false; }
        });
        addSlotToContainer(new Slot(inventory, 1, 8, 36) {
            @Override public boolean isItemValid(ItemStack stack) { return stack != null && stack.getItem() == Item.getItemFromBlock(Blocks.carpet); }
            @Override public int getSlotStackLimit() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < columns; col++)
            addSlotToContainer(new Slot(inventory, 2 + col + row * columns, 80 + col * 18, 18 + row * 18));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player, col, 8 + col * 18, 142));
    }
    @Override public boolean canInteractWith(EntityPlayer player) { return !llama.isDead && llama.getDistanceToEntity(player) < 8 && inventory.isUseableByPlayer(player); }
    @Override public void onContainerClosed(EntityPlayer player) { super.onContainerClosed(player); inventory.closeInventory(player); }
    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = getSlot(index);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack(), original = stack.copy();
        int size = inventory.getSizeInventory();
        if (index < size) {
            if (!mergeItemStack(stack, size, inventorySlots.size(), true)) return null;
        } else if (getSlot(1).isItemValid(stack) && !getSlot(1).getHasStack()) {
            if (!mergeItemStack(stack, 1, 2, false)) return null;
        } else if (size <= 2 || !mergeItemStack(stack, 2, size, false)) return null;
        if (stack.stackSize == 0) slot.putStack(null); else slot.onSlotChanged();
        return original;
    }
}
