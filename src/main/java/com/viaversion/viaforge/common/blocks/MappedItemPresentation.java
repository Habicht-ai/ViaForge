package com.viaversion.viaforge.common.blocks;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.api.minecraft.item.Item;

/** Removes only presentation fields introduced by a particular Via item layer.
 * Real server CustomModelData, edited names, counts and durability remain live. */
public final class MappedItemPresentation {
    private static final String KEY="ViaForge|mappedPresentation";
    public static void capture(Item item,String boundary,int modelData) {
        CompoundTag tag=item.tag();
        CompoundTag snapshots=tag==null?null:tag.getCompoundTag(KEY);
        if(snapshots!=null&&snapshots.contains(boundary))return;
        CompoundTag snapshot=new CompoundTag();
        snapshot.putBoolean("tag",tag!=null);snapshot.putBoolean("display",tag!=null&&tag.contains("display"));
        snapshot.putBoolean("model",tag!=null&&tag.contains("CustomModelData"));snapshot.putInt("generated",modelData);
        if(tag==null){tag=new CompoundTag();item.setTag(tag);}
        if(snapshots==null){snapshots=new CompoundTag();tag.put(KEY,snapshots);}
        snapshots.put(boundary,snapshot);
    }
    public static void restore(Item item,String boundary) {
        if(item==null||item.tag()==null)return;
        CompoundTag tag=item.tag(),snapshots=tag.getCompoundTag(KEY);
        if(snapshots==null)return;
        CompoundTag snapshot=snapshots.getCompoundTag(boundary);if(snapshot==null)return;
        if(!snapshot.getBoolean("model")&&tag.getInt("CustomModelData",Integer.MIN_VALUE)==snapshot.getInt("generated"))tag.remove("CustomModelData");
        CompoundTag display=tag.getCompoundTag("display");
        if(!snapshot.getBoolean("display")&&display!=null&&display.isEmpty())tag.remove("display");
        snapshots.remove(boundary);if(snapshots.isEmpty())tag.remove(KEY);
        if(!snapshot.getBoolean("tag")&&tag.isEmpty())item.setTag(null);
    }
    private MappedItemPresentation() { }
}
