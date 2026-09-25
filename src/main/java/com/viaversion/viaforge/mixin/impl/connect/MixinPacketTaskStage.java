package com.viaversion.viaforge.mixin.impl.connect;

import com.viaversion.viaforge.compatibility.ClientPacketTasks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinPacketTaskStage {
    @Inject(method="runGameLoop", at=@At(value="FIELD",
            target="Lnet/minecraft/client/Minecraft;scheduledTasks:Ljava/util/Queue;", ordinal=0))
    private void processModernPackets(CallbackInfo ci) {
        ClientPacketTasks.process();
    }
}
