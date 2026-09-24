package com.viaversion.viaforge.mixin.impl.compatibility;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.block.BlockLadder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(BlockLadder.class)
public abstract class MixinLadderShape {
    @ModifyConstant(method="setBlockBoundsBasedOnState",constant=@Constant(floatValue=.125F))
    private float thickness(float original) {
        return ServerSession.rule(ClientRule.MODERN_THIN_COLLISIONS)?.1875F:original;
    }
}
