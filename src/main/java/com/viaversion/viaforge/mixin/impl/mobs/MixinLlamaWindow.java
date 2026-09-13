package com.viaversion.viaforge.mixin.impl.mobs;

import com.viaversion.viaforge.common.blocks.MobKind;
import com.viaversion.viaforge.mobs.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.AnimalChest;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinLlamaWindow {
    @Inject(method = "handleOpenWindow", at = @At("HEAD"), cancellable = true)
    private void llamaInventory(S2DPacketOpenWindow packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        PacketThreadUtil.checkThreadAndEnqueue(packet, (NetHandlerPlayClient)(Object)this, mc);
        if (!"EntityHorse".equals(packet.getGuiId()) || mc.theWorld == null || mc.thePlayer == null) return;
        Entity entity = mc.theWorld.getEntityByID(packet.getEntityId());
        if (!(entity instanceof ServerMob) || ((ServerMob)entity).kind() != MobKind.LLAMA) return;
        LlamaInventoryScreen screen = new LlamaInventoryScreen(mc.thePlayer.inventory,
                new AnimalChest(packet.getWindowTitle(), packet.getSlotCount()), (ServerMob)entity);
        mc.thePlayer.openContainer = screen.inventorySlots;
        screen.inventorySlots.windowId = packet.getWindowId();
        mc.displayGuiScreen(screen);
        ci.cancel();
    }
}
