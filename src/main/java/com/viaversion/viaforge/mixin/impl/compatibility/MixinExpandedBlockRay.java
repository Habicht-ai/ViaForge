package com.viaversion.viaforge.mixin.impl.compatibility;

import com.viaversion.viaforge.blocks.ShulkerBlockEntity;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.compatibility.ServerSession;
import net.minecraft.block.Block;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Modern shapes are clipped against the whole ray, including lids outside their voxel. */
@Mixin(World.class)
public abstract class MixinExpandedBlockRay {
    @Unique private Vec3 viaForge$rayOrigin;
    @Inject(method="rayTraceBlocks(Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;ZZZ)Lnet/minecraft/util/MovingObjectPosition;",at=@At("HEAD"))
    private void rayOrigin(Vec3 start,Vec3 end,boolean liquid,boolean ignore,boolean last,CallbackInfoReturnable<MovingObjectPosition> ci){viaForge$rayOrigin=start;}
    @Redirect(method="rayTraceBlocks(Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;ZZZ)Lnet/minecraft/util/MovingObjectPosition;",
            at=@At(value="INVOKE",target="Lnet/minecraft/block/Block;collisionRayTrace(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;)Lnet/minecraft/util/MovingObjectPosition;"))
    private MovingObjectPosition fullShapeRay(Block block,World world,BlockPos pos,Vec3 steppedStart,Vec3 end){
        boolean expanded=ServerSession.rule(ClientRule.EXPANDED_BLOCK_RAY)&&world.getTileEntity(pos) instanceof ShulkerBlockEntity;
        return block.collisionRayTrace(world,pos,expanded?viaForge$rayOrigin:steppedStart,end);
    }
}
