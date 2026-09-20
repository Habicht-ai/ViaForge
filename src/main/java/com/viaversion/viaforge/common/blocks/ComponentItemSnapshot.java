package com.viaversion.viaforge.common.blocks;

import com.viaversion.nbt.tag.*;
import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.rewriter.ItemRewriter;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.*;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import io.netty.buffer.*;

/** The structured-to-NBT boundary loses empty/default overrides and exact text tags.
 * Store its original component patch in the existing per-stack NBT bridge. No
 * protocol is replayed and no connection-global item identity cache is needed. */
public final class ComponentItemSnapshot {
    public static final String KEY="ViaForge|components";
    private static boolean enabled(UserConnection user,Item item) {
        return user!=null&&item!=null&&!item.isEmpty()
                &&CompatibilityRegistry.forUser(user).adapter() instanceof FlattenedProtocolAdapter.Factory;
    }
    public static CompoundTag capture(UserConnection user,Item item,Object rewriter) {
        if(!enabled(user,item))return null;
        int format=format(rewriter);if(CompatibilityRegistry.forUser(user).serverProtocol()!=format)return null;
        StructuredDataContainer data=item.dataContainer();
        CompoundTag custom=data.get(StructuredDataKey.CUSTOM_DATA);
        if(custom!=null&&custom.contains(KEY))return null;
        ByteBuf bytes=Unpooled.buffer();
        try {
            codec(format).write(bytes,item);
            byte[] encoded=new byte[bytes.readableBytes()];bytes.readBytes(encoded);
            CompoundTag snapshot=new CompoundTag();snapshot.put("item",new ByteArrayTag(encoded));snapshot.putInt("format",format);
            Integer stack=data.get(StructuredDataKey.MAX_STACK_SIZE),damage=data.get(StructuredDataKey.MAX_DAMAGE);
            if(stack!=null)snapshot.putInt("stack",stack);if(damage!=null)snapshot.putInt("damage",damage);
            return snapshot;
        }catch(Exception error){throw new IllegalStateException("Cannot retain original item components",error);}
        finally{bytes.release();}
    }
    public static void attach(Item result,CompoundTag snapshot,Object rewriter) {
        if(snapshot==null||result==null||result.isEmpty())return;
        if(result instanceof com.viaversion.viaversion.api.minecraft.item.DataItem) {
            if(result.tag()==null)result.setTag(new CompoundTag());result.tag().put(KEY,snapshot);
        }else {
            StructuredDataContainer data=result.dataContainer();CompoundTag custom=data.get(StructuredDataKey.CUSTOM_DATA);
            custom=custom==null?new CompoundTag():custom.copy();custom.put(KEY,snapshot);
            data.setIdLookup(((ItemRewriter<?,?,?>)rewriter).protocol(),false);data.set(StructuredDataKey.CUSTOM_DATA,custom);
        }
    }
    public static void restore(UserConnection user,Item old,Item result,Object rewriter) {
        if(!enabled(user,result))return;
        CompoundTag custom=result.dataContainer().get(StructuredDataKey.CUSTOM_DATA);
        CompoundTag snapshot=old instanceof com.viaversion.viaversion.api.minecraft.item.DataItem&&old.tag()!=null?old.tag().getCompoundTag(KEY):custom==null?null:custom.getCompoundTag(KEY);
        if(snapshot==null||snapshot.getInt("format")!=format(rewriter))return;
        ByteArrayTag encoded=snapshot.getByteArrayTag("item");if(encoded==null)return;
        ByteBuf bytes=Unpooled.wrappedBuffer(encoded.getValue());
        try {
            Item original=codec(snapshot.getInt("format")).read(bytes);
            if(bytes.isReadable()||original.isEmpty())throw new IllegalArgumentException("Invalid component snapshot");
            StructuredDataContainer restored=original.dataContainer();
            restored.setIdLookup(((ItemRewriter<?,?,?>)rewriter).protocol(),true);
            Integer damage=restored.get(StructuredDataKey.DAMAGE);
            Integer updatedDamage=result.dataContainer().get(StructuredDataKey.DAMAGE);
            int liveDamage=updatedDamage==null?0:updatedDamage;
            if(liveDamage!=(damage==null?0:damage))restored.set(StructuredDataKey.DAMAGE,liveDamage);
            result.setIdentifier(original.identifier());
            result.dataContainer().data().clear();result.dataContainer().data().putAll(restored.data());
            // The live amount belongs to the inventory operation, never the snapshot.
        }catch(Exception error){throw new IllegalStateException("Cannot restore original item components",error);}
        finally{bytes.release();}
    }
    public static boolean genericBoundary(Object rewriter) {return format(rewriter)>=770;}
    private static int format(Object rewriter) {
        if(rewriter instanceof com.viaversion.viabackwards.protocol.v26_2to26_1.rewriter.BlockItemPacketRewriter26_2)return 776;
        if(rewriter instanceof com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter.BlockItemPacketRewriter26_3)return 777;
        if(rewriter instanceof com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter.BlockItemPacketRewriter26_1)return 775;
        if(rewriter instanceof com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.rewriter.BlockItemPacketRewriter1_21_11)return 774;
        if(rewriter instanceof com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.rewriter.BlockItemPacketRewriter1_21_9)return 773;
        if(rewriter instanceof com.viaversion.viabackwards.protocol.v1_21_7to1_21_6.rewriter.BlockItemPacketRewriter1_21_7)return 772;
        if(rewriter instanceof com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.rewriter.BlockItemPacketRewriter1_21_6)return 771;
        if(rewriter instanceof com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.rewriter.BlockItemPacketRewriter1_21_5)return 770;
        if(rewriter instanceof com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.rewriter.BlockItemPacketRewriter1_21_4)return 769;
        if(rewriter instanceof com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter.BlockItemPacketRewriter1_21_2)return 768;
        if(rewriter instanceof com.viaversion.viabackwards.protocol.v1_21to1_20_5.rewriter.BlockItemPacketRewriter1_21)return 767;
        return 766;
    }
    private static Type<Item> codec(int format) {
        switch(format){case 766:return VersionedTypes.V1_20_5.item();case 767:return VersionedTypes.V1_21.item();case 768:return VersionedTypes.V1_21_2.item();case 769:return VersionedTypes.V1_21_4.item();case 770:return VersionedTypes.V1_21_5.item();case 771:case 772:return VersionedTypes.V1_21_6.item();case 773:return VersionedTypes.V1_21_9.item();case 774:return VersionedTypes.V1_21_11.item();case 775:return VersionedTypes.V26_1.item();case 776:return VersionedTypes.V26_2.item();case 777:return VersionedTypes.V26_3.item();default:throw new IllegalArgumentException("Unknown component snapshot format "+format);}
    }
    private ComponentItemSnapshot() { }
}
