package com.viaversion.viaforge.mixin.impl.compatibility;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** The native ray tracer and outline must use the same lily shape as collision. */
@Mixin(Block.class)
public abstract class MixinThinBlockOutline {
    @Shadow public abstract void setBlockBounds(float x0,float y0,float z0,float x1,float y1,float z1);
    @Unique private void viaForge$lilyBounds() {
        if((Object)this!=Blocks.waterlily)return;
        if(ServerSession.rule(ClientRule.MODERN_THIN_COLLISIONS))setBlockBounds(.0625F,0,.0625F,.9375F,.09375F,.9375F);
        else setBlockBounds(0,0,0,1,.015625F,1);
    }
    @Inject(method="setBlockBoundsBasedOnState",at=@At("RETURN"))
    private void rayShape(IBlockAccess world,BlockPos pos,CallbackInfo ci){viaForge$lilyBounds();}
    @Inject(method="getSelectedBoundingBox",at=@At("HEAD"))
    private void selection(World world,BlockPos pos,CallbackInfoReturnable<AxisAlignedBB> ci){viaForge$lilyBounds();}
}
