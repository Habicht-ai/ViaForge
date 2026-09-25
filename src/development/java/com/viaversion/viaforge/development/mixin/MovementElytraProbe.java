package com.viaversion.viaforge.development.mixin;

import com.viaversion.viaforge.development.MovementTrace;
import com.viaversion.viaforge.items.ServerElytraFlight;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ServerElytraFlight.class,remap=false)
public abstract class MovementElytraProbe {
    @Inject(method="metadata",at=@At("RETURN"))
    private static void applied(int id,boolean flying,CallbackInfo ci) {
        if(Minecraft.getMinecraft().thePlayer!=null&&Minecraft.getMinecraft().thePlayer.getEntityId()==id)
            MovementTrace.sample(Minecraft.getMinecraft().thePlayer,"FLIGHT_METADATA_"+flying);
    }
}
