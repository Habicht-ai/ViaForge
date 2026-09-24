package com.viaversion.viaforge.mixin.impl.compatibility;

import com.viaversion.viaforge.common.compatibility.InteractionSequence;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.Protocol1_19To1_18_2;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.packet.ClientboundPackets1_19;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Protocol1_19To1_18_2.class,remap=false)
public abstract class MixinInteractionSequence {
    @Inject(method="registerPackets",at=@At("TAIL"))
    private void sequences(CallbackInfo ci) {
        Protocol1_19To1_18_2 protocol=(Protocol1_19To1_18_2)(Object)this;
        // Observe every world replacement, even if no action occurs between two
        // dimension changes. Same-world respawn retains the original ClientLevel.
        protocol.appendClientbound(ClientboundPackets1_19.LOGIN,w->w.user().put(new InteractionSequence()));
        protocol.appendClientbound(ClientboundPackets1_19.RESPAWN,w->state(w).worldChanged(world(w)));
        protocol.appendServerbound(ServerboundPackets1_17.PLAYER_ACTION,w->{
            int action=w.get(Types.VAR_INT,0);
            if(action==0||action==2)w.set(Types.VAR_INT,1,next(w));
        });
        protocol.appendServerbound(ServerboundPackets1_17.USE_ITEM_ON,w->w.set(Types.VAR_INT,2,next(w)));
        protocol.appendServerbound(ServerboundPackets1_17.USE_ITEM,w->w.set(Types.VAR_INT,1,next(w)));
    }
    private static int next(PacketWrapper wrapper) {
        return state(wrapper).next(world(wrapper));
    }
    private static String world(PacketWrapper wrapper) {
        return wrapper.user().getEntityTracker(Protocol1_19To1_18_2.class).currentWorld();
    }
    private static InteractionSequence state(PacketWrapper wrapper) {
        InteractionSequence state=wrapper.user().get(InteractionSequence.class);
        if(state==null){state=new InteractionSequence();wrapper.user().put(state);}
        return state;
    }
}
