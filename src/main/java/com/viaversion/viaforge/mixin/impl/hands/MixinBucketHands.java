package com.viaversion.viaforge.mixin.impl.hands;
import com.viaversion.viaforge.hands.*;
import net.minecraft.item.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemBucket.class)
public abstract class MixinBucketHands {
    @Inject(method="fillBucket",at=@At("HEAD"))
    private void fill(ItemStack empty,EntityPlayer player,Item full,CallbackInfoReturnable<ItemStack> ci){if(Offhand.local(player)&&Offhand.context>=0)HandActions.bucketUsed=true;}
    @Inject(method="tryPlaceContainedLiquid",at=@At("RETURN"))
    private void place(World world,BlockPos pos,CallbackInfoReturnable<Boolean> ci){if(Offhand.active()&&Offhand.context>=0&&ci.getReturnValue())HandActions.bucketUsed=true;}
}
