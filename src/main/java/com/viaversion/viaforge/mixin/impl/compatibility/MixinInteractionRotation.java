package com.viaversion.viaforge.mixin.impl.compatibility;

import com.viaversion.viaforge.common.compatibility.InteractionRotation;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.Protocol1_21To1_20_5;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Protocol1_21To1_20_5.class,remap=false)
public abstract class MixinInteractionRotation {
    @Inject(method="registerPackets",at=@At("TAIL"))
    private void clickRotation(CallbackInfo ci) {
        ((Protocol1_21To1_20_5)(Object)this).appendServerbound(ServerboundPackets1_20_5.USE_ITEM,w->{
            InteractionRotation rotation=w.user().get(InteractionRotation.class);
            if(rotation!=null){w.set(Types.FLOAT,0,rotation.yaw);w.set(Types.FLOAT,1,rotation.pitch);}
        });
    }
}
