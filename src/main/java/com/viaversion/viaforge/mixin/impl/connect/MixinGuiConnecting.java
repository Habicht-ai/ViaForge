package com.viaversion.viaforge.mixin.impl.connect;

import com.viaversion.viaforge.common.ViaForgeCommon;
import com.viaversion.viaforge.common.extended.ExtendedGuiConnecting;
import com.viaversion.viaforge.common.extended.ExtendedServerData;
import com.viaversion.viaforge.common.platform.VersionTracker;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Snapshot the actual request, including launches that have no ServerData. */
@Mixin(GuiConnecting.class)
public class MixinGuiConnecting implements ExtendedGuiConnecting {
    @Unique private ProtocolVersion viaForge$version;
    @Shadow private void connect(String host, int port) { throw new AssertionError(); }

    @Redirect(method = "<init>(Lnet/minecraft/client/gui/GuiScreen;Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/ServerData;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/GuiConnecting;connect(Ljava/lang/String;I)V"))
    private void captureServer(GuiConnecting self, String host, int port, GuiScreen parent, Minecraft mc, ServerData server) {
        viaForge$version = VersionTracker.resolve(((ExtendedServerData) server).viaForge$getVersion(),
                ViaForgeCommon.getManager().getTargetVersion());
        connect(host, port);
    }

    @Redirect(method = "<init>(Lnet/minecraft/client/gui/GuiScreen;Lnet/minecraft/client/Minecraft;Ljava/lang/String;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/GuiConnecting;connect(Ljava/lang/String;I)V"))
    private void captureAddress(GuiConnecting self, String host, int port, GuiScreen parent, Minecraft mc, String requestedHost, int requestedPort) {
        viaForge$version = ViaForgeCommon.getManager().getTargetVersion();
        connect(host, port);
    }

    @Override public ProtocolVersion viaForge$getConnectionVersion() { return viaForge$version; }
}
