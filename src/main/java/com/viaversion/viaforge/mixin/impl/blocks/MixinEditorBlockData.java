package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.blocks.EditorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinEditorBlockData {
    @Inject(method = "handleUpdateTileEntity", at = @At("HEAD"), cancellable = true)
    private void editorData(S35PacketUpdateTileEntity packet, CallbackInfo ci) {
        if (packet.getTileEntityType() != 2 && packet.getTileEntityType() != 7) return;
        Minecraft mc = Minecraft.getMinecraft();
        PacketThreadUtil.checkThreadAndEnqueue(packet, (NetHandlerPlayClient) (Object) this, mc);
        if (mc.theWorld == null) return;
        TileEntity tile = mc.theWorld.getTileEntity(packet.getPos());
        if (tile instanceof EditorBlockEntity) {
            EditorBlockEntity editor = (EditorBlockEntity) tile;
            if (packet.getTileEntityType() == (editor.structure() ? 7 : 2)) editor.accept(packet.getNbtCompound());
            ci.cancel();
        }
    }
}
