package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

/** Local surface effects at the original movement stage. */
public final class ServerSurfacePhysics {
    public static boolean deferredSlime(Entity entity,Block block) {
        return entity==Minecraft.getMinecraft().thePlayer&&block==Blocks.slime_block&&ServerSession.rule(ClientRule.DEFERRED_BLOCK_EFFECTS);
    }
    public static void afterTravel(EntityLivingBase entity) {
        if(entity!=Minecraft.getMinecraft().thePlayer||!ServerSession.rule(ClientRule.DEFERRED_BLOCK_EFFECTS)||!entity.onGround||entity.noClip)return;
        BlockPos pos=new BlockPos(entity.posX,entity.posY-.2,entity.posZ);
        Block block=entity.worldObj.getBlockState(pos).getBlock();
        if(block==Blocks.slime_block)block.onEntityCollidedWithBlock(entity.worldObj,pos,entity);
    }
    public static boolean landed(Entity entity,Block block,double movedY) {
        if(entity!=Minecraft.getMinecraft().thePlayer||block!=Blocks.slime_block||!ServerSession.rule(ClientRule.COLLISION_PORTION_BOUNCE))return false;
        double gravity=ServerSwimming.effectiveGravity((EntityLivingBase)entity),velocity=entity.motionY;
        if(!entity.onGround||entity.isSneaking()||-velocity<gravity||velocity>=0
                ||-velocity==gravity&&ServerSession.rule(ClientRule.SUPPRESS_GRAVITY_EQUAL_BOUNCE))entity.motionY=0;
        else {
            // 26.2 compensates the travelled fraction of the collision tick.
            double portion=movedY/velocity;
            entity.motionY=(portion*gravity-velocity)*(1+portion*((double).98F-1));
        }
        return true;
    }
    private ServerSurfacePhysics(){}
}
