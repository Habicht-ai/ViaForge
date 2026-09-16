package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.texture.LayeredColorMaskTexture;
import net.minecraft.item.EnumDyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;

/** Banner/shield texture dyes changed in 1.12 independently of map colors. */
@Mixin(LayeredColorMaskTexture.class)
public abstract class MixinBannerDyeColors {
    @Unique private static final int[] viaForge$dyes={0xF9FFFE,16351261,13061821,3847130,16701501,8439583,15961002,4673362,0x9D9D97,1481884,8991416,3949738,8606770,6192150,11546150,0x1D1D21};
    @Unique private static final java.util.Map<MapColor,Integer> viaForge$colors=new java.util.IdentityHashMap<>();
    static { for(EnumDyeColor dye:EnumDyeColor.values())viaForge$colors.put(dye.getMapColor(),viaForge$dyes[dye.getMetadata()]); }
    @Redirect(method="loadTexture",at=@At(value="FIELD",target="Lnet/minecraft/block/material/MapColor;colorValue:I"))
    private int textureDye(MapColor color) {
        return ServerSession.rule(ClientRule.UPDATED_DYE_COLORS)?viaForge$colors.getOrDefault(color,color.colorValue):color.colorValue;
    }
}
