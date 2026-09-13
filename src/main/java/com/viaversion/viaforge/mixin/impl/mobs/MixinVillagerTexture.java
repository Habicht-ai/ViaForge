package com.viaversion.viaforge.mixin.impl.mobs;
import com.viaversion.viaforge.mobs.*;
import net.minecraft.client.renderer.entity.RenderVillager;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RenderVillager.class)
public abstract class MixinVillagerTexture {
    @Inject(method="getEntityTexture(Lnet/minecraft/entity/passive/EntityVillager;)Lnet/minecraft/util/ResourceLocation;",at=@At("HEAD"),cancellable=true)
    private void profession(EntityVillager entity,CallbackInfoReturnable<ResourceLocation> ci) {
        MobState state = ServerMobs.get(entity);
        if (state != null && state.protocol >= 315 && state.number(state.first+1,0) == 5) ci.setReturnValue(new ResourceLocation("viaforge","textures/entity/villager/villager.png"));
    }
}
