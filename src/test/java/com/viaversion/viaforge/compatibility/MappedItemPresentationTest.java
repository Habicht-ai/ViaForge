package com.viaversion.viaforge.compatibility;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaforge.common.blocks.MappedItemPresentation;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import org.junit.Test;
import static org.junit.Assert.*;

public class MappedItemPresentationTest {
    @Test public void generatedPresentationDoesNotLeakIntoServerNbt() {
        Item item=new DataItem(299,(byte)7,(short)0,null);
        MappedItemPresentation.capture(item,"caves",299);
        item.tag().putInt("CustomModelData",299);item.tag().put("display",new CompoundTag());
        item.setAmount(2);MappedItemPresentation.restore(item,"caves");
        assertNull(item.tag());assertEquals(2,item.amount());
    }
    @Test public void realServerPresentationAndLiveEditsSurvive() {
        CompoundTag tag=new CompoundTag();tag.putInt("CustomModelData",299);tag.put("display",new CompoundTag());
        Item item=new DataItem(299,(byte)1,(short)0,tag.copy());
        MappedItemPresentation.capture(item,"caves",299);item.tag().putInt("Damage",17);
        MappedItemPresentation.restore(item,"caves");tag.putInt("Damage",17);assertEquals(tag,item.tag());
        item.setTag(null);MappedItemPresentation.capture(item,"caves",299);
        item.tag().putInt("CustomModelData",300);MappedItemPresentation.restore(item,"caves");
        assertEquals(300,item.tag().getInt("CustomModelData"));
    }
    @Test public void eachLayerRemovesOnlyItsOwnChanges() {
        Item item=new DataItem(299,(byte)1,(short)0,null);
        MappedItemPresentation.capture(item,"outer",299);item.tag().putInt("CustomModelData",299);
        MappedItemPresentation.capture(item,"inner",17);
        MappedItemPresentation.restore(item,"inner");assertEquals(299,item.tag().getInt("CustomModelData"));
        MappedItemPresentation.restore(item,"outer");assertNull(item.tag());
    }
}
