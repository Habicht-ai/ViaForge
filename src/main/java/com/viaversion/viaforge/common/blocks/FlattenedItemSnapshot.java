package com.viaversion.viaforge.common.blocks;

import com.viaversion.nbt.tag.*;
import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.Protocol1_12_2To1_13;

/** Preserve exact server NBT before namespaced enchantments/JSON names are
 * normalized. Stacking/splitting changes the live amount, never the snapshot. */
public final class FlattenedItemSnapshot {
    private static final String KEY="ViaForge|flattenedItem";
    public static void capture(UserConnection user,Item item) {
        if(user==null||item==null||!(CompatibilityRegistry.forUser(user).adapter() instanceof FlattenedProtocolAdapter.Factory))return;
        if(item.tag()!=null&&item.tag().contains(KEY))return;
        int legacy=Protocol1_12_2To1_13.MAPPINGS.getItemMappings().inverse().getNewId(item.identifier());
        if(legacy<0)return;
        // Only existing identities can enter the shared native item catalog.
        CompoundTag snapshot=new CompoundTag();snapshot.putInt("id",item.identifier());
        if(item.tag()!=null)snapshot.put("tag",item.tag().copy());
        if(item.tag()==null)item.setTag(new CompoundTag());
        item.tag().put(KEY,snapshot);
    }
    public static void restore(UserConnection user,Item item) {
        if(user==null||item==null||item.tag()==null||!(CompatibilityRegistry.forUser(user).adapter() instanceof FlattenedProtocolAdapter.Factory))return;
        CompoundTag snapshot=item.tag().getCompoundTag(KEY);if(snapshot==null)return;
        CompoundTag tag=snapshot.getCompoundTag("tag");tag=tag==null?null:tag.copy();
        // Durability can be changed locally by a legitimate inventory operation.
        Tag damage=item.tag().get("Damage");
        if(damage!=null){if(tag==null)tag=new CompoundTag();tag.put("Damage",damage.copy());}
        item.setIdentifier(snapshot.getInt("id"));item.setData((short)0);item.setTag(tag);
    }
    private FlattenedItemSnapshot(){}
}
