package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.*;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColorHelper.class)
public abstract class MixinWaterColor {
    @Inject(method="getWaterColorAtPos",at=@At("HEAD"),cancellable=true)
    private static void modernWater(IBlockAccess world,BlockPos pos,CallbackInfoReturnable<Integer> result) {
        if (!ServerSession.rule(ClientRule.BIOME_WATER_COLORS)) return;
        WaterColors colors=ServerSession.waterColors();
        int r=0,g=0,b=0;
        for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++) {
            BlockPos sample=pos.add(x,0,z);
            int biome=world.getBiomeGenForCoords(sample).biomeID;
            int fallback=WaterColors.legacyBiomeColor(biome);
            int rgb=colors==null?fallback:colors.at(sample.getX(),sample.getY(),sample.getZ(),fallback);
            r+=rgb>>16&255;g+=rgb>>8&255;b+=rgb&255;
        }
        result.setReturnValue((r/9)<<16|(g/9)<<8|b/9);
    }
}
