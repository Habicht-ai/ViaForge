package com.viaversion.viaforge.mixin.impl.hands;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.entity.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(ItemRenderer.class)
public interface HandRenderAccess {
    @Invoker("rotateArroundXAndY") void viaForge$rotate(float pitch,float yaw);
    @Invoker("setLightMapFromPlayer") void viaForge$light(AbstractClientPlayer player);
    @Invoker("rotateWithPlayerRotations") void viaForge$armLag(net.minecraft.client.entity.EntityPlayerSP player,float partial);
}
