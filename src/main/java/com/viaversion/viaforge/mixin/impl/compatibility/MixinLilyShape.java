package com.viaversion.viaforge.mixin.impl.compatibility;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLilyPad.class)
public abstract class MixinLilyShape {
    @Inject(method="getCollisionBoundingBox",at=@At("HEAD"),cancellable=true)
    private void collision(World world,BlockPos pos,IBlockState state,CallbackInfoReturnable<AxisAlignedBB> ci) {
        // Block instances are shared across connections. Do not inherit bounds
        // left by a previous modern world's outline when returning to 1.8.
        AxisAlignedBB shape=ServerSession.rule(ClientRule.MODERN_THIN_COLLISIONS)
            ?new AxisAlignedBB(.0625,0,.0625,.9375,.09375,.9375)
            :new AxisAlignedBB(0,0,0,1,.015625,1);
        ci.setReturnValue(shape.offset(pos.getX(),pos.getY(),pos.getZ()));
    }
}
