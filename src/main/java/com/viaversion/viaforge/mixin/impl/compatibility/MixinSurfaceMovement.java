package com.viaversion.viaforge.mixin.impl.compatibility;

import com.viaversion.viaforge.compatibility.ServerSurfacePhysics;
import com.viaversion.viaforge.compatibility.ServerBubbleColumns;
import com.viaversion.viaforge.blocks.ShulkerBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinSurfaceMovement {
    @Unique private double viaForge$surfaceStartY;
    @Unique private Vec3 viaForge$effectStart;
    @Unique private boolean viaForge$zFirst;
    @Inject(method="moveEntity",at=@At("HEAD"))
    private void startMove(double x,double y,double z,CallbackInfo ci){
        Entity entity=(Entity)(Object)this;
        viaForge$surfaceStartY=entity.posY;
        viaForge$effectStart=new Vec3(entity.posX,entity.posY,entity.posZ);viaForge$zFirst=Math.abs(x)<Math.abs(z);
    }
    @Inject(method="moveEntity",at=@At("RETURN"))
    private void recordMove(double x,double y,double z,CallbackInfo ci){ServerBubbleColumns.record((Entity)(Object)this,viaForge$effectStart,viaForge$zFirst);}
    @Redirect(method="moveEntity",at=@At(value="INVOKE",target="Lnet/minecraft/block/Block;onLanded(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;)V"))
    private void originalBounce(Block block,World world,Entity entity) {
        if(!ServerSurfacePhysics.landed(entity,block,entity.posY-viaForge$surfaceStartY))block.onLanded(world,entity);
    }
    @Redirect(method="moveEntity",at=@At(value="INVOKE",target="Lnet/minecraft/block/Block;onEntityCollidedWithBlock(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/entity/Entity;)V"))
    private void stepStage(Block block,World world,BlockPos pos,Entity entity) {
        if(!ServerSurfacePhysics.deferredSlime(entity,block))block.onEntityCollidedWithBlock(world,pos,entity);
    }
    @Redirect(method="moveEntity",at=@At(value="INVOKE",target="Lnet/minecraft/entity/Entity;isSneaking()Z"))
    private boolean externalPushDoesNotBackOffEdge(Entity entity){return !ShulkerBlockEntity.pushing(entity)&&entity.isSneaking();}
}
