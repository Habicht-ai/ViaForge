package com.viaversion.viaforge.mixin.impl.compatibility;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(EntityLivingBase.class)
public abstract class MixinClimbing {
    @Shadow protected boolean isJumping;
    @Redirect(method="moveEntityWithHeading",at=@At(value="FIELD",target="Lnet/minecraft/entity/EntityLivingBase;isCollidedHorizontally:Z",opcode=org.objectweb.asm.Opcodes.GETFIELD))
    private boolean jumpClimb(EntityLivingBase entity) {
        // In the land branch this field guards the single post-move ladder boost.
        // Water uses ServerSwimming's own original-version travel implementation.
        return entity.isCollidedHorizontally || ServerSession.rule(ClientRule.JUMP_CLIMBING)
            && entity==net.minecraft.client.Minecraft.getMinecraft().thePlayer && !entity.isInWater() && !entity.isInLava() && isJumping;
    }
}
