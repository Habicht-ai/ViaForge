package com.viaversion.viaforge.mixin.impl.connect;

import com.viaversion.viaforge.compatibility.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinResourceLoading {
    @Redirect(method="handlePlayerPosLook",at=@At(value="INVOKE",target="Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V"))
    private void awaitTextures(Minecraft mc,GuiScreen screen) {
        mc.displayGuiScreen(screen==null&&ServerSession.awaitingResources()?new ResourceLoadingScreen():screen);
    }
}
