package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.common.blocks.ComponentItemSnapshot;
import com.viaversion.viaforge.common.compatibility.ClientFeature;
import com.viaversion.viaforge.compatibility.ServerSession;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinComponentItemLimits {
    private int viaForge$limit(String name) {
        if(!ServerSession.has(ClientFeature.ITEMS))return -1;
        NBTTagCompound tag=((ItemStack)(Object)this).getTagCompound();
        if(tag==null)return -1;
        NBTTagCompound snapshot=tag.getCompoundTag(ComponentItemSnapshot.KEY);
        return snapshot.hasKey(name,3)?snapshot.getInteger(name):-1;
    }
    @Inject(method="getMaxStackSize",at=@At("HEAD"),cancellable=true)
    private void stackSize(CallbackInfoReturnable<Integer> ci){int size=viaForge$limit("stack");if(size>=1&&size<=99)ci.setReturnValue(size);}
    @Inject(method="getMaxDamage",at=@At("HEAD"),cancellable=true)
    private void maxDamage(CallbackInfoReturnable<Integer> ci){int damage=viaForge$limit("damage");if(damage>0)ci.setReturnValue(damage);}
}
