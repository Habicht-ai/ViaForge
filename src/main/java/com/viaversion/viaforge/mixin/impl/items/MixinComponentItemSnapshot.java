package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.common.blocks.ComponentItemSnapshot;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.rewriter.BlockItemPacketRewriter1_20_5;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={BlockItemPacketRewriter1_20_5.class,
        com.viaversion.viabackwards.protocol.v1_21to1_20_5.rewriter.BlockItemPacketRewriter1_21.class,
        com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter.BlockItemPacketRewriter1_21_2.class,
        com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.rewriter.BlockItemPacketRewriter1_21_4.class},remap=false)
public abstract class MixinComponentItemSnapshot {
    @org.spongepowered.asm.mixin.Unique private final ThreadLocal<java.util.Deque<com.viaversion.nbt.tag.CompoundTag>> viaForge$snapshots=ThreadLocal.withInitial(java.util.LinkedList::new);
    @Inject(method="handleItemToClient",at=@At("HEAD"))
    private void capture(UserConnection user,Item item,CallbackInfoReturnable<Item> ci){viaForge$snapshots.get().push(ComponentItemSnapshot.capture(user,item,this));}
    @Inject(method="handleItemToClient",at=@At("RETURN"))
    private void attach(UserConnection user,Item item,CallbackInfoReturnable<Item> ci){java.util.Deque<com.viaversion.nbt.tag.CompoundTag> pending=viaForge$snapshots.get();ComponentItemSnapshot.attach(ci.getReturnValue(),pending.pop(),this);if(pending.isEmpty())viaForge$snapshots.remove();}
    @Inject(method="handleItemToServer",at=@At("RETURN"))
    private void restore(UserConnection user,Item item,CallbackInfoReturnable<Item> ci){ComponentItemSnapshot.restore(user,item,ci.getReturnValue(),this);}
}
