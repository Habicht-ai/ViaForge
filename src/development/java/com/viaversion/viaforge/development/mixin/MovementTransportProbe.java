package com.viaversion.viaforge.development.mixin;
import com.viaversion.viaforge.common.compatibility.CompatibilityDecodeHandler;
import com.viaversion.viaforge.development.MovementTrace;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=CompatibilityDecodeHandler.class,remap=false)
public abstract class MovementTransportProbe {
    @Inject(method="decode",at=@At("HEAD"))
    private void received(ChannelHandlerContext context,ByteBuf input,List<Object> output,CallbackInfo ci) {
        MovementTrace.transport(input);
    }
}
