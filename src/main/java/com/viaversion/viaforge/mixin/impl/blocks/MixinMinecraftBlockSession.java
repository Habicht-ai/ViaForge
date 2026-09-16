package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.compatibility.ServerSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftBlockSession {
    @Inject(method="runTick",at=@At("RETURN"))
    private void updateWaterColors(CallbackInfo ci) {
        com.viaversion.viaforge.common.compatibility.WaterColors colors=ServerSession.waterColors();
        Minecraft mc=(Minecraft)(Object)this;
        if(colors==null||mc.theWorld==null)return;
        Long position;
        while((position=colors.pollUpdate())!=null) {
            int x=(int)(position>>32)*16,z=position.intValue()*16;
            mc.theWorld.markBlockRangeForRenderUpdate(x-1,0,z-1,x+16,255,z+16);
        }
    }
    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void resetBlockResources(WorldClient world, String message, CallbackInfo ci) {
        if (world == null) ServerSession.unload();
    }
}
