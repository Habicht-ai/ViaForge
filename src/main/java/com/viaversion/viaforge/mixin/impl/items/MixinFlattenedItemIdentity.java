package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaforge.items.ClientItems;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.rewriter.BlockItemPacketRewriter1_13;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.Protocol1_12_2To1_13;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prefer an exact inherited catalog identity over ViaBackwards' approximation
 * (notably the five non-oak boats), retaining its complete NBT conversion. */
@Mixin(value=BlockItemPacketRewriter1_13.class,remap=false)
public abstract class MixinFlattenedItemIdentity {
    @Shadow @Final private String extraNbtTag;
    @Inject(method="handleItemToClient",at=@At("HEAD"))
    private void exactIdentity(UserConnection user,Item item,CallbackInfoReturnable<Item> ci) {
        if(user==null||item==null||!(CompatibilityRegistry.forUser(user).adapter() instanceof FlattenedProtocolAdapter.Factory))return;
        com.viaversion.viaforge.common.blocks.FlattenedItemSnapshot.capture(user,item);
        int exact=Protocol1_12_2To1_13.MAPPINGS.getItemMappings().inverse().getNewId(item.identifier());
        // Flattened eggs choose their appearance by item ID, even when entity_data
        // names another mob or contains a modern renamed ID. Preserve that choice
        // separately; never replace the server-owned EntityTag to repair rendering.
        java.util.Optional<String> entity=com.viaversion.viaversion.protocols.v1_12_2to1_13.data.SpawnEggMappings1_13.getEntityId(exact);
        if(entity.isPresent()) {
            String name=entity.get().replace("minecraft:","");
            if(com.viaversion.viaforge.common.blocks.SpawnEggNames.MODERN.containsKey(name))
                item.tag().putString(com.viaversion.viaforge.common.blocks.SpawnEggNames.CLIENT_ID,name);
            return;
        }
        if(exact<0||exact==Protocol1_13To1_12_2.MAPPINGS.getItemMappings().getNewId(item.identifier())
                ||ClientItems.localItem(exact>>4,exact&15)<0)return;
        if(item.tag()==null)item.setTag(new CompoundTag());
        int id=exact>>4,data=exact&15;
        if(BlockItemPacketRewriter1_13.isDamageable(id))data=item.tag().getInt("Damage");
        item.tag().putInt(extraNbtTag,(id<<16)|(data&65535));
    }
    @Inject(method="handleItemToServer",at=@At("RETURN"))
    private void originalNbt(UserConnection user,Item item,CallbackInfoReturnable<Item> ci) {
        com.viaversion.viaforge.common.blocks.FlattenedItemSnapshot.restore(user,ci.getReturnValue());
    }
}
