package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind;
import com.viaversion.viaforge.items.*;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerCustomHead.class)
public abstract class MixinWornDragonHead {
    @Shadow @Final private ModelRenderer field_177209_a;
    @Inject(method = "doRenderLayer", at = @At("HEAD"), cancellable = true)
    private void dragon(EntityLivingBase entity, float limb, float amount, float partial, float age, float yaw, float pitch, float scale, CallbackInfo ci) {
        if (!ClientItems.is(entity.getCurrentArmor(3), Kind.HEAD)) return;
        GlStateManager.pushMatrix();
        if (entity.isSneaking()) GlStateManager.translate(0, .2F, 0);
        boolean villager = entity instanceof net.minecraft.entity.passive.EntityVillager;
        boolean villagerHead = villager || entity instanceof net.minecraft.entity.monster.EntityZombie && ((net.minecraft.entity.monster.EntityZombie)entity).isVillager()
                || entity instanceof com.viaversion.viaforge.mobs.ServerMob && ((com.viaversion.viaforge.mobs.ServerMob)entity).state.kind == com.viaversion.viaforge.common.blocks.MobKind.ZOMBIE_VILLAGER;
        if (entity.isChild() && !villager) {
            GlStateManager.translate(0, .5F * scale, 0);
            GlStateManager.scale(.7F, .7F, .7F); GlStateManager.translate(0, 16 * scale, 0);
        }
        field_177209_a.postRender(.0625F); GlStateManager.color(1, 1, 1, 1); GlStateManager.scale(1.1875F, -1.1875F, -1.1875F);
        if (villagerHead) GlStateManager.translate(0, .0625F, 0);
        // All 1.9-1.12.2 head layers pass limb swing, not age in ticks. This
        // also keeps heads on idle players, passengers and armor stands still.
        ServerItemRenderer.renderDragon(-.5F, 0, -.5F, EnumFacing.UP, 180, limb);
        GlStateManager.popMatrix(); ci.cancel();
    }
}
