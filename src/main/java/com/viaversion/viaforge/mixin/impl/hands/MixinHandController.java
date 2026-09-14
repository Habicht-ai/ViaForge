package com.viaversion.viaforge.mixin.impl.hands;

import com.viaversion.viaforge.hands.Offhand;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerControllerMP.class)
public abstract class MixinHandController {

    // Sneaking skips block activation whenever either hand holds an item.
    @Redirect(method="onPlayerRightClick",at=@At(value="INVOKE",target="Lnet/minecraft/client/entity/EntityPlayerSP;getHeldItem()Lnet/minecraft/item/ItemStack;"))
    private ItemStack occupiedHand(EntityPlayerSP player){
        ItemStack held=player.getHeldItem();
        if(held==null && Offhand.active() && Offhand.context>=0)held=Offhand.stack(1-Offhand.context);
        return held;
    }
}
