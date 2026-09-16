package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.common.compatibility.ClientFeature;
import com.viaversion.viaforge.compatibility.ServerSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Via normalizes modern direct inventory updates to window -2. Unlike newer
 * clients, 1.8 ignores that window instead of indexing InventoryPlayer directly. */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinDirectInventoryUpdate {
    @Inject(method="handleSetSlot",at=@At("HEAD"),cancellable=true)
    private void playerInventory(S2FPacketSetSlot packet,CallbackInfo ci) {
        if(packet.func_149175_c()!=-2||!ServerSession.has(ClientFeature.ITEMS))return;
        Minecraft mc=Minecraft.getMinecraft();
        PacketThreadUtil.checkThreadAndEnqueue(packet,(NetHandlerPlayClient)(Object)this,mc);
        if(mc.thePlayer!=null) {
            InventoryPlayer inventory=mc.thePlayer.inventory;
            int slot=packet.func_149173_d();
            // Slot 40 belongs to the separately captured offhand event. Native
            // main/armor arrays cover 0..39, regardless of any open container.
            if(slot>=0&&slot<inventory.mainInventory.length+inventory.armorInventory.length)
                inventory.setInventorySlotContents(slot,packet.func_149174_e());
        }
        ci.cancel();
    }
}
