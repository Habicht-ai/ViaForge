package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.common.blocks.MappedItemPresentation;
import com.viaversion.viaforge.common.compatibility.CompatibilityRegistry;
import com.viaversion.viaforge.common.compatibility.FlattenedProtocolAdapter;
import com.viaversion.viabackwards.api.data.MappedItem;
import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=BackwardsItemRewriter.class,remap=false)
public abstract class MixinMappedItemPresentation {
    @Inject(method="handleItemToClient",at=@At("HEAD"))
    private void rememberPresentation(UserConnection user,Item item,CallbackInfoReturnable<Item> ci) {
        if(user==null||item==null||!(CompatibilityRegistry.forUser(user).adapter() instanceof FlattenedProtocolAdapter.Factory))return;
        BackwardsItemRewriter<?,?,?> rewriter=(BackwardsItemRewriter<?,?,?>)(Object)this;
        if(rewriter.protocol().getMappingData()==null)return;
        MappedItem mapped=rewriter.protocol().getMappingData().getMappedItem(item.identifier());
        if(mapped!=null)MappedItemPresentation.capture(item,rewriter.protocol().getClass().getSimpleName(),mapped.customModelData());
    }
    @Inject(method="handleItemToServer",at=@At("RETURN"))
    private void removePresentation(UserConnection user,Item item,CallbackInfoReturnable<Item> ci) {
        if(user==null||!(CompatibilityRegistry.forUser(user).adapter() instanceof FlattenedProtocolAdapter.Factory))return;
        BackwardsItemRewriter<?,?,?> rewriter=(BackwardsItemRewriter<?,?,?>)(Object)this;
        MappedItemPresentation.restore(ci.getReturnValue(),rewriter.protocol().getClass().getSimpleName());
    }
}
