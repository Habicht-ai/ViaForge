package com.viaversion.viaforge.mixin.impl.mobs;
import com.viaversion.viaforge.mobs.ServerMob;
import com.viaversion.viaforge.mobs.ServerMobProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinMobMetadata {
    @Inject(method = "handleEntityMetadata", at = @At("HEAD"), cancellable = true)
    private void originalMetadata(S1CPacketEntityMetadata packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        PacketThreadUtil.checkThreadAndEnqueue(packet, (NetHandlerPlayClient)(Object)this, mc);
        if (mc.theWorld != null) {
            net.minecraft.entity.Entity entity = mc.theWorld.getEntityByID(packet.getEntityId());
            if (entity instanceof ServerMob || entity instanceof ServerMobProjectile) ci.cancel();
        }
    }
}
