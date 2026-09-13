package com.viaversion.viaforge.mixin.impl.items;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.items.ServerItemRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntitySkullRenderer.class)
public abstract class MixinDragonHead {
    @Inject(method = "renderSkull", at = @At("HEAD"), cancellable = true)
    private void dragon(float x, float y, float z, EnumFacing facing, float rotation, int type, GameProfile owner, int damage, CallbackInfo ci) {
        if (type == 5 && ServerBlockSession.supportsProtocol(107)) { ServerItemRenderer.renderDragon(x, y, z, facing, rotation, 0); ci.cancel(); }
    }
}
