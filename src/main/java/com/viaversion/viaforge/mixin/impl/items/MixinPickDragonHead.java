package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.items.ClientItems;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class MixinPickDragonHead {
    // Forge-added method: its name has no MCP/SRG mapping.
    @Inject(method = "getPickBlock(Lnet/minecraft/util/MovingObjectPosition;Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;", at = @At("RETURN"), cancellable = true, remap = false)
    private void head(MovingObjectPosition hit, World world, BlockPos pos, EntityPlayer player, CallbackInfoReturnable<ItemStack> ci) {
        ItemStack result = ci.getReturnValue();
        if (!ServerBlockSession.supportsProtocol(107) || result == null || result.getItem() != Items.skull || result.getMetadata() != 5) return;
        ItemStack imported = new ItemStack(Item.getItemById(ClientItems.localItem(397, 5)), result.stackSize);
        if (result.hasTagCompound()) imported.setTagCompound((net.minecraft.nbt.NBTTagCompound)result.getTagCompound().copy());
        ci.setReturnValue(imported);
    }
}
