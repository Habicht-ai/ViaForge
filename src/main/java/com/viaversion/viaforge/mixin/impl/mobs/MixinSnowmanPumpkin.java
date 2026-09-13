package com.viaversion.viaforge.mixin.impl.mobs;
import com.viaversion.viaforge.mobs.*;
import net.minecraft.client.renderer.entity.layers.LayerSnowmanHead;
import net.minecraft.entity.monster.EntitySnowman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LayerSnowmanHead.class)
public abstract class MixinSnowmanPumpkin {
    @Inject(method="doRenderLayer(Lnet/minecraft/entity/monster/EntitySnowman;FFFFFFF)V",at=@At("HEAD"),cancellable=true)
    private void pumpkin(EntitySnowman entity,float limb,float amount,float partial,float age,float yaw,float pitch,float scale,CallbackInfo ci) {
        MobState state=ServerMobs.get(entity);
        if(state != null && (state.number(state.first,16)&16) == 0) ci.cancel();
    }
}
