package com.viaversion.viaforge.mixin.impl.hands;
import com.viaversion.viaforge.hands.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityPlayer.class)
public abstract class MixinActiveHand {
    @Unique private boolean viaForge$finishing;
    @Shadow protected abstract void onItemUseFinish();
    @Redirect(method="onUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"))
    private ItemStack activeStack(InventoryPlayer inventory){return Offhand.local((EntityPlayer)(Object)this)&&Offhand.useHand==1?Offhand.get():inventory.getCurrentItem();}
    @Inject(method="setItemInUse",at=@At("HEAD"))
    private void start(ItemStack stack,int ticks,CallbackInfo ci){if(Offhand.local((EntityPlayer)(Object)this))Offhand.useHand=Math.max(0,Offhand.context);}
    @Inject(method="onItemUseFinish",at=@At("HEAD"),cancellable=true)
    private void finishOtherHand(CallbackInfo ci){
        EntityPlayer player=(EntityPlayer)(Object)this;
        if(viaForge$finishing||!Offhand.local(player)||Offhand.useHand!=1)return;
        ItemStack main=player.getHeldItem();player.inventory.mainInventory[player.inventory.currentItem]=Offhand.get();
        viaForge$finishing=true;
        try{onItemUseFinish();Offhand.set(player.getHeldItem());}
        finally{player.inventory.mainInventory[player.inventory.currentItem]=main;viaForge$finishing=false;Offhand.useHand=0;}
        ci.cancel();
    }
}
