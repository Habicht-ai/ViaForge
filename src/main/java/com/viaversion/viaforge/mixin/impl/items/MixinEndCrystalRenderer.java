package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.items.ServerCrystalVisuals;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.tileentity.RenderEnderCrystal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderEnderCrystal.class)
public abstract class MixinEndCrystalRenderer extends Render<EntityEnderCrystal> {
    protected MixinEndCrystalRenderer(RenderManager manager) { super(manager); }
    @Redirect(method = "doRender(Lnet/minecraft/entity/item/EntityEnderCrystal;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V"))
    private void base(ModelBase model, Entity entity, float limb, float yaw, float bob, float head, float pitch, float scale) {
        ServerCrystalVisuals.model(model, entity).render(entity, limb, yaw, bob, head, pitch, scale);
    }
    @ModifyArg(method = "doRender(Lnet/minecraft/entity/item/EntityEnderCrystal;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/RenderEnderCrystal;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private ResourceLocation texture(ResourceLocation original) {
        return ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.ENTITY_VISUALS) ? new ResourceLocation("viaforge:textures/entity/endercrystal/endercrystal.png") : original;
    }
    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityEnderCrystal;DDDFF)V", at = @At("RETURN"))
    private void beam(EntityEnderCrystal entity, double x, double y, double z, float yaw, float partial, CallbackInfo ci) { ServerCrystalVisuals.beam(entity, x, y, z, partial); }
    @Override public boolean shouldRender(EntityEnderCrystal entity, ICamera camera, double x, double y, double z) {
        return super.shouldRender(entity, camera, x, y, z) || ServerCrystalVisuals.hasBeam(entity);
    }
}
