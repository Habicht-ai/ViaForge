package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.blocks.PlacementSounds;
import com.viaversion.viaforge.hands.Offhand;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class MixinPlacementSound {
    @Inject(method="playSoundEffect",at=@At("RETURN"))
    private void localPlacement(double x,double y,double z,String sound,float volume,float pitch,CallbackInfo ci) {
        // Only the local modern hand interaction. Server sound packets already
        // use WorldClient.playSound and must not be played twice.
        if((Object)this instanceof WorldClient&&Offhand.active()&&Offhand.context>=0)
            PlacementSounds.play((WorldClient)(Object)this,x,y,z,sound,volume,pitch);
    }
}
