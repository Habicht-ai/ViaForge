package com.viaversion.viaforge.mixin.impl.hands;
import com.viaversion.viaforge.hands.Offhand;
import com.viaversion.viaforge.items.ClientItems;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog;
import net.minecraft.inventory.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ContainerPlayer.class)
public abstract class MixinShieldTransfer {
    @Inject(method="transferStackInSlot",at=@At("HEAD"),cancellable=true)
    private void equip(EntityPlayer player,int index,CallbackInfoReturnable<ItemStack> ci){
        if(!Offhand.local(player)||index<9||index>=45||Offhand.get()!=null)return;
        Slot slot=((ContainerPlayer)(Object)this).getSlot(index);ItemStack stack=slot.getStack();
        if(!ClientItems.is(stack,LegacyItemCatalog.Kind.SHIELD))return;
        ItemStack original=stack.copy();Offhand.set(slot.decrStackSize(1));slot.onSlotChanged();slot.onPickupFromSlot(player,stack);ci.setReturnValue(original);
    }
}
