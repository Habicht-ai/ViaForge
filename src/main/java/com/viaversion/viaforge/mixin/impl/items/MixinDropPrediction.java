package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.compatibility.ServerSession;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Since 1.13.1, Q predicts removal locally. The server can suppress the slot
 * echo because it expects that prediction; native 1.8 only sends the action. */
@Mixin(EntityPlayerSP.class)
public abstract class MixinDropPrediction {
    @Inject(method="dropOneItem",at=@At("HEAD"))
    private void predictDrop(boolean all,CallbackInfoReturnable<EntityItem> ci) {
        if(!ServerSession.rule(ClientRule.PREDICT_HOTBAR_DROPS))return;
        EntityPlayerSP player=(EntityPlayerSP)(Object)this;
        ItemStack stack=player.inventory.getCurrentItem();
        if(stack!=null&&stack.stackSize>0)
            player.inventory.decrStackSize(player.inventory.currentItem,all?stack.stackSize:1);
        // Let the original method send its one drop action. Server slot updates
        // remain authoritative, including a plugin's rejection of the drop.
    }
}
