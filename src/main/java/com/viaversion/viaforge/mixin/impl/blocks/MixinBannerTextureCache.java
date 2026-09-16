package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.compatibility.ServerSession;
import java.util.Map;
import net.minecraft.client.renderer.tileentity.TileEntityBannerRenderer;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileEntityBannerRenderer.class)
public abstract class MixinBannerTextureCache {
    @Shadow @Final private static Map<String,?> DESIGNS;
    @Unique private static long viaForge$generation=-1;
    @Inject(method="func_178463_a",at=@At("HEAD"))
    private void invalidateDesigns(TileEntityBanner banner,CallbackInfoReturnable<ResourceLocation> result) {
        long generation=ServerSession.resourceGeneration();
        if(viaForge$generation!=generation) { DESIGNS.clear();viaForge$generation=generation; }
    }
}
