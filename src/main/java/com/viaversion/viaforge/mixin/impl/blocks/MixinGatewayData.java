package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.blocks.GatewayBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinGatewayData {
    @Inject(method = "handleUpdateTileEntity", at = @At("HEAD"), cancellable = true)
    private void gateway(S35PacketUpdateTileEntity packet, CallbackInfo ci) {
        if (packet.getTileEntityType() != 8) return;
        Minecraft mc = Minecraft.getMinecraft();
        PacketThreadUtil.checkThreadAndEnqueue(packet, (NetHandlerPlayClient)(Object)this, mc);
        if (mc.theWorld == null) return;
        TileEntity tile = mc.theWorld.getTileEntity(packet.getPos());
        if (tile instanceof GatewayBlockEntity) { ((GatewayBlockEntity)tile).accept(packet.getNbtCompound()); ci.cancel(); }
    }
}
