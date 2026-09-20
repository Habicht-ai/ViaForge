package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientFeature;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** New server attributes hit 1.8's invalid zero-default / positive-minimum fallback. */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinServerAttributes {
    @Redirect(method="handleEntityProperties",at=@At(value="NEW",target="net/minecraft/entity/ai/attributes/RangedAttribute"))
    private RangedAttribute unknownAttribute(IAttribute parent,String name,double value,double minimum,double maximum) {
        return new RangedAttribute(parent,name,value,ServerSession.has(ClientFeature.ENTITY_VISUALS)?Math.min(value,minimum):minimum,maximum);
    }
}
