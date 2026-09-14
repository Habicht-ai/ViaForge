package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
public abstract class MixinShieldArmPose {
    @Inject(method = "setRotationAngles", at = @At("HEAD"))
    private void shieldPose(float a, float b, float c, float d, float e, float f, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof EntityPlayer) || !ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.TWO_HANDS)) return;
        ModelBiped model = (ModelBiped)(Object)this;
        EntityPlayer player = (EntityPlayer)entity;
        ServerEntityViews.View view = ServerEntityViews.get(entity.getEntityId());
        model.heldItemLeft = 0;
        if (view != null && (view.handState & 3) == 3 && ClientItems.is(view.offhand, Kind.SHIELD)) model.heldItemRight = player.getHeldItem() == null ? 0 : 1;
        for (boolean other : new boolean[]{false, true}) {
            ItemStack stack = other ? com.viaversion.viaforge.hands.Offhand.of(player) : player.getHeldItem();
            if (stack==null) continue;
            int pose = ServerEntityViews.blocking(player, other, stack) ? 3 : 1;
            if (other ^ com.viaversion.viaforge.hands.Offhand.mainLeft(player)) {
                model.heldItemLeft = pose;
                if (!other) model.heldItemRight = 0;
            } else model.heldItemRight = pose;
        }
    }
    @Inject(method = "setRotationAngles", at = @At("RETURN"))
    private void leftShieldYaw(float a, float b, float c, float d, float e, float f, Entity entity, CallbackInfo ci) {
        ModelBiped model = (ModelBiped)(Object)this;
        if (entity instanceof EntityPlayer && ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.TWO_HANDS) && model.heldItemLeft == 3) model.bipedLeftArm.rotateAngleY += .5235988F;
        if(!(entity instanceof EntityPlayer)||!com.viaversion.viaforge.hands.Offhand.active())return;
        EntityPlayer player=(EntityPlayer)entity;ServerEntityViews.View view=ServerEntityViews.get(entity.getEntityId());
        boolean local=player==net.minecraft.client.Minecraft.getMinecraft().thePlayer;
        int hand=local?com.viaversion.viaforge.hands.Offhand.useHand:view!=null?(view.handState>>1)&1:0;
        boolean using=local?player.isUsingItem():view!=null&&(view.handState&1)!=0;
        ItemStack stack=hand==1?com.viaversion.viaforge.hands.Offhand.of(player):player.getHeldItem();
        if(!using||stack==null||!(stack.getItem() instanceof net.minecraft.item.ItemBow))return;
        boolean left=(hand==1)^com.viaversion.viaforge.hands.Offhand.mainLeft(player);float sign=left?-1:1;
        net.minecraft.client.model.ModelRenderer bow=left?model.bipedLeftArm:model.bipedRightArm,other=left?model.bipedRightArm:model.bipedLeftArm;
        bow.rotateAngleY=model.bipedHead.rotateAngleY-.1F*sign;other.rotateAngleY=model.bipedHead.rotateAngleY+.5F*sign;
        bow.rotateAngleX=other.rotateAngleX=-(float)Math.PI/2+model.bipedHead.rotateAngleX;
        float idle=net.minecraft.util.MathHelper.cos(c*.09F)*.05F+.05F;
        model.bipedRightArm.rotateAngleZ=idle;model.bipedLeftArm.rotateAngleZ=-idle;
        model.bipedRightArm.rotateAngleX+=net.minecraft.util.MathHelper.sin(c*.067F)*.05F;model.bipedLeftArm.rotateAngleX-=net.minecraft.util.MathHelper.sin(c*.067F)*.05F;
    }
}
