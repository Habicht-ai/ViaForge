package com.viaversion.viaforge.mixin.impl.hands;

import com.viaversion.viaforge.hands.Offhand;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerControllerMP.class)
public abstract class MixinHandController {

    // From 1.13 the controller sends use-on before BlockItem evaluates its
    // placement result. ItemBlock.onItemUse still performs the actual placement
    // checks: only the obsolete controller preflight is skipped here.
    @Redirect(method="onPlayerRightClick",at=@At(value="INVOKE",target="Lnet/minecraft/item/ItemBlock;canPlaceBlockOnSide(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/util/EnumFacing;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)Z"))
    private boolean placementPreflight(ItemBlock item,World world,BlockPos pos,EnumFacing side,EntityPlayer player,ItemStack stack) {
        if(Offhand.active() && Offhand.context>=0 && ServerSession.rule(ClientRule.MODERN_BLOCK_USE_FAILURE))return true;
        return item.canPlaceBlockOnSide(world,pos,side,player,stack);
    }

    // Sneaking skips block activation whenever either hand holds an item.
    @Redirect(method="onPlayerRightClick",at=@At(value="INVOKE",target="Lnet/minecraft/client/entity/EntityPlayerSP;getHeldItem()Lnet/minecraft/item/ItemStack;"))
    private ItemStack occupiedHand(EntityPlayerSP player){
        ItemStack held=player.getHeldItem();
        if(held==null && Offhand.active() && Offhand.context>=0)held=Offhand.stack(1-Offhand.context);
        return held;
    }
}
