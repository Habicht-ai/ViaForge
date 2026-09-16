package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaforge.common.blocks.ComponentItemSnapshot;
import com.viaversion.viaversion.rewriter.StructuredItemRewriter;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;
import com.viaversion.viaversion.api.minecraft.item.HashedItem;
import com.viaversion.viaversion.data.item.ItemHasherBase;
import com.viaversion.viaversion.rewriter.ItemRewriter;

/** Families whose item implementation is inherited from Via's structured rewriter. */
@Mixin(value=StructuredItemRewriter.class,remap=false)
public abstract class MixinStructuredComponentSnapshot {
    @Shadow protected abstract void storeOriginalHashedItemInTag(UserConnection user,Item item,ItemHasherBase hasher,HashedItem original);
    @Unique private final ThreadLocal<Deque<HashedItem>> viaForge$originalHashes=ThreadLocal.withInitial(LinkedList::new);
    @Unique private final ThreadLocal<Deque<CompoundTag>> viaForge$componentRecords=ThreadLocal.withInitial(LinkedList::new);
    @Inject(method="handleItemToClient",at=@At("HEAD"))
    private void capture(UserConnection user,Item item,CallbackInfoReturnable<Item> ci){if(!ComponentItemSnapshot.genericBoundary(this))return;
        CompoundTag snapshot=ComponentItemSnapshot.capture(user,item,this);viaForge$componentRecords.get().push(snapshot);
        ItemHasherBase hasher=(ItemHasherBase)user.getItemHasher(((ItemRewriter<?,?,?>)(Object)this).protocol().getClass());
        viaForge$originalHashes.get().push(snapshot!=null&&hasher!=null&&hasher.isProcessingClientboundInventoryPacket()?hasher.toHashedItem(item,false):null);}
    @Inject(method="handleItemToClient",at=@At("RETURN"))
    private void attach(UserConnection user,Item item,CallbackInfoReturnable<Item> ci){if(!ComponentItemSnapshot.genericBoundary(this))return;Deque<CompoundTag> records=viaForge$componentRecords.get();ComponentItemSnapshot.attach(ci.getReturnValue(),records.pop(),this);
        HashedItem original=viaForge$originalHashes.get().pop();
        // Our snapshot is itself a custom_data change, including on originally
        // component-free items. Preserve Via's real pre-change hash at this boundary.
        if(original!=null)storeOriginalHashedItemInTag(user,ci.getReturnValue(),(ItemHasherBase)user.getItemHasher(((ItemRewriter<?,?,?>)(Object)this).protocol().getClass()),original);
        if(records.isEmpty()){viaForge$componentRecords.remove();viaForge$originalHashes.remove();}}
    @Inject(method="handleItemToServer",at=@At("RETURN"))
    private void restore(UserConnection user,Item item,CallbackInfoReturnable<Item> ci){if(ComponentItemSnapshot.genericBoundary(this))ComponentItemSnapshot.restore(user,item,ci.getReturnValue(),this);}
}
