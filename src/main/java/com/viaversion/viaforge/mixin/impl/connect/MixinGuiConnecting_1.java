/*
 * This file is part of ViaForge - https://github.com/ViaVersion/ViaForge
 * Copyright (C) 2021-2026 Florian Reuth <git@florianreuth.de> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.viaversion.viaforge.mixin.impl.connect;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaforge.common.extended.ExtendedGuiConnecting;
import com.viaversion.viaforge.common.platform.VersionTracker;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;

@Mixin(targets = "net.minecraft.client.multiplayer.GuiConnecting$1")
public class MixinGuiConnecting_1 {

    @Unique
    private ProtocolVersion viaForge$version;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void captureSelection(GuiConnecting screen, String threadName, String host, int port, CallbackInfo ci) {
        // Constructed on the GUI thread before DNS / asynchronous connection work.
        viaForge$version = ((ExtendedGuiConnecting) screen).viaForge$getConnectionVersion();
    }

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;createNetworkManagerAndConnect(Ljava/net/InetAddress;IZ)Lnet/minecraft/network/NetworkManager;"))
    public NetworkManager trackServerVersion(InetAddress address, int port, boolean nativeTransport) {
        com.viaversion.viaforge.compatibility.ServerSession.prefetch(
                com.viaversion.viaforge.common.compatibility.CompatibilityRegistry.DEFAULT.resolve(viaForge$version.getVersion()));
        return VersionTracker.connect(viaForge$version,
                () -> NetworkManager.createNetworkManagerAndConnect(address, port, nativeTransport));
    }

}
