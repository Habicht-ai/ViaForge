package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.common.blocks.LegacyItemSnapshot;
import com.viaversion.viarewind.protocol.v1_9to1_8.rewriter.BlockItemPacketRewriter1_9;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {BlockItemPacketRewriter1_9.class,
        com.viaversion.viabackwards.protocol.v1_11to1_10.rewriter.BlockItemPacketRewriter1_11.class,
        com.viaversion.viabackwards.protocol.v1_11_1to1_11.rewriter.ItemPacketRewriter1_11_1.class,
        com.viaversion.viabackwards.protocol.v1_12to1_11_1.rewriter.BlockItemPacketRewriter1_12.class}, remap = false)
public abstract class MixinRewindItemSnapshot {
    @Inject(method = "handleItemToClient", at = @At("HEAD"))
    private void capture(UserConnection user, Item item, CallbackInfoReturnable<Item> ci) { LegacyItemSnapshot.capture(user, item, this); }
    @Inject(method = "handleItemToServer", at = @At("RETURN"))
    private void restore(UserConnection user, Item item, CallbackInfoReturnable<Item> ci) { LegacyItemSnapshot.restore(ci.getReturnValue(), this); }
}
