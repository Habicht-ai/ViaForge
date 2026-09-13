package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.items.ServerItemCooldowns;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEnderPearl.class)
public abstract class MixinPearlCooldown {
    @Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
    private void modernPearl(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> ci) {
        if (!world.isRemote || !ServerBlockSession.supportsProtocol(107)) return;
        if (!ServerItemCooldowns.cooling(stack)) {
            if (!player.capabilities.isCreativeMode) stack.stackSize--;
            String sound = com.viaversion.viaforge.mobs.ServerMobSounds.key("entity.enderpearl.throw", 7);
            if (sound != null) world.playSoundAtEntity(player, sound, .5F, .4F / (world.rand.nextFloat() * .4F + .8F));
            // All released 1.9-1.12.2 clients predict a 20-tick pearl cooldown.
            // SET_COOLDOWN can replace it with the server's authoritative value.
            ServerItemCooldowns.set(368, 20);
            player.triggerAchievement(StatList.objectUseStats[368]);
        }
        ci.setReturnValue(stack);
    }
}
