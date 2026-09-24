package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.blocks.ComponentItemSnapshot;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinComponentBlocking {
    @Unique private boolean viaForge$blocking() {
        ItemStack stack=(ItemStack)(Object)this;
        return ServerSession.rule(ClientRule.COMPONENT_BLOCKING)&&stack.getItem() instanceof ItemSword&&stack.hasTagCompound()
            &&stack.getTagCompound().getCompoundTag(ComponentItemSnapshot.KEY).getBoolean("blocking");
    }
    @Inject(method="getItemUseAction",at=@At("HEAD"),cancellable=true)
    private void animation(CallbackInfoReturnable<EnumAction> ci){if(viaForge$blocking())ci.setReturnValue(EnumAction.BLOCK);}
    @Inject(method="getMaxItemUseDuration",at=@At("HEAD"),cancellable=true)
    private void duration(CallbackInfoReturnable<Integer> ci){if(viaForge$blocking())ci.setReturnValue(72000);}
    @Inject(method="useItemRightClick",at=@At("HEAD"),cancellable=true)
    private void use(World world,EntityPlayer player,CallbackInfoReturnable<ItemStack> ci) {
        if(viaForge$blocking()){ItemStack stack=(ItemStack)(Object)this;player.setItemInUse(stack,72000);ci.setReturnValue(stack);}
    }
}
