package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ClientItems;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.viaversion.viaforge.hands.Offhand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/** The client must allow charging a bow when only the new arrow types are present. */
@Mixin(ItemBow.class)
public abstract class MixinBowArrows {
    private static boolean ammunition(ItemStack stack){return stack!=null&&stack.stackSize>0&&(stack.getItem()==Items.arrow||ClientItems.arrow(stack));}
    private static ItemStack findArrow(EntityPlayer player){
        if(ammunition(Offhand.get()))return Offhand.get();
        if(ammunition(Offhand.stack(0)))return Offhand.stack(0);
        for(ItemStack stack:player.inventory.mainInventory)if(ammunition(stack))return stack;
        return null;
    }
    @Inject(method="onPlayerStoppedUsing",at=@At("HEAD"),cancellable=true)
    private void release(ItemStack bow,World world,EntityPlayer player,int remaining,CallbackInfo ci){
        if(!Offhand.local(player)||!world.isRemote)return;
        ci.cancel();
        boolean infinite=player.capabilities.isCreativeMode||net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.infinity.effectId,bow)>0;
        ItemStack arrow=findArrow(player);if(arrow==null&&!infinite)return;
        net.minecraftforge.event.entity.player.ArrowLooseEvent event=new net.minecraftforge.event.entity.player.ArrowLooseEvent(player,bow,bow.getMaxItemUseDuration()-remaining);
        if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event))return;
        float pull=event.charge/20F;if((pull*pull+2*pull)/3<.1F)return;
        // Modern clients predict ammunition only. Durability, projectile and
        // release sound arrive from the authoritative server.
        if(arrow!=null&&!player.capabilities.isCreativeMode&&!(infinite&&arrow.getItem()==Items.arrow)){
            if(--arrow.stackSize<=0){
                if(Offhand.get()==arrow)Offhand.set(null);
                for(int i=0;i<player.inventory.mainInventory.length;i++)if(player.inventory.mainInventory[i]==arrow)player.inventory.mainInventory[i]=null;
            }
        }
    }
    @Redirect(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;hasItem(Lnet/minecraft/item/Item;)Z"))
    private boolean arrows(InventoryPlayer inventory, Item item) {
        if(item==Items.arrow && Offhand.active() && findArrow(net.minecraft.client.Minecraft.getMinecraft().thePlayer)!=null)return true;
        if (inventory.hasItem(item)) return true;
        if (item == Items.arrow) for (ItemStack stack : inventory.mainInventory) if (ClientItems.arrow(stack) && stack.stackSize > 0) return true;
        return false;
    }
}
