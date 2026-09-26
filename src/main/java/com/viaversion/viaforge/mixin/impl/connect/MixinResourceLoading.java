package com.viaversion.viaforge.mixin.impl.connect;

import com.viaversion.viaforge.compatibility.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinResourceLoading {
    @Shadow private boolean doneLoadingTerrain;

    @Inject(method="handleJoinGame",at=@At(value="INVOKE",target="Lnet/minecraft/client/Minecraft;loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;)V"))
    private void beginWorld(CallbackInfo ci) {
        // A proxy can send another Join on this same play handler. The native
        // constructor's initial false no longer suffices for that new world.
        // The next real position packet still owns readiness and its confirmation.
        doneLoadingTerrain=false;
    }
    @Redirect(method="handlePlayerPosLook",at=@At(value="INVOKE",target="Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V"))
    private void awaitTextures(Minecraft mc,GuiScreen screen) {
        mc.displayGuiScreen(screen==null&&ServerSession.awaitingResources()?new ResourceLoadingScreen():screen);
    }
}
