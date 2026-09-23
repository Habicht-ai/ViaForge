package com.viaversion.viaforge.mixin.impl.compatibility;
import com.viaversion.viaforge.compatibility.ServerSwimming;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityPlayerSP.class)
public abstract class MixinSwimmingInput extends net.minecraft.entity.player.EntityPlayer {
    protected MixinSwimmingInput(net.minecraft.world.World world,com.mojang.authlib.GameProfile profile){super(world,profile);}
    @Redirect(method="onLivingUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/client/entity/EntityPlayerSP;setSprinting(Z)V"))
    private void originalSprint(EntityPlayerSP player,boolean sprint){
        if(!ServerSwimming.enabled(player)||player.isRiding())player.setSprinting(sprint);
    }
    @Inject(method="onLivingUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/client/entity/EntityPlayerSP;getFoodStats()Lnet/minecraft/util/FoodStats;"))
    private void targetSprint(CallbackInfo ci){ServerSwimming.sprintInput((EntityPlayerSP)(Object)this);}
    @Inject(method="updateEntityActionState",at=@At("RETURN"))
    private void targetInput(CallbackInfo ci){ServerSwimming.applyMovementInput((EntityPlayerSP)(Object)this);}
    @Inject(method="onLivingUpdate",at=@At("HEAD"))
    private void swimFlightPriority(CallbackInfo ci){
        EntityPlayerSP player=(EntityPlayerSP)(Object)this;
        if(ServerSwimming.swimming(player))flyToggleTimer=0;
    }
}
