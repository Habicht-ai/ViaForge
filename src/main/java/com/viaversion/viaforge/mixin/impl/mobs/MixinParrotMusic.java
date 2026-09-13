package com.viaversion.viaforge.mixin.impl.mobs;
import com.viaversion.viaforge.mobs.ServerMob;
import com.viaversion.viaforge.common.blocks.MobKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RenderGlobal.class)
public abstract class MixinParrotMusic {
    @Inject(method="playRecord",at=@At("RETURN"))
    private void dance(String record,BlockPos pos,CallbackInfo ci) {
        if(Minecraft.getMinecraft().theWorld == null) return;
        for(Object value:Minecraft.getMinecraft().theWorld.loadedEntityList) {
            Entity entity=(Entity)value;
            if(entity instanceof ServerMob && ((ServerMob)entity).kind() == MobKind.PARROT && pos.distanceSq(entity.posX,entity.posY,entity.posZ) <= 12) ((ServerMob)entity).jukebox = record == null ? null : pos;
        }
    }
}
