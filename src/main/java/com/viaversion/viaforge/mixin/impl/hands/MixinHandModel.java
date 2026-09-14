package com.viaversion.viaforge.mixin.impl.hands;
import com.viaversion.viaforge.hands.HandModels;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.resources.model.IBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(RenderItem.class)
public abstract class MixinHandModel {
    @Redirect(method="renderItemModelTransform",at=@At(value="INVOKE",target="Lnet/minecraftforge/client/ForgeHooksClient;handleCameraTransforms(Lnet/minecraft/client/resources/model/IBakedModel;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)Lnet/minecraft/client/resources/model/IBakedModel;"))
    private IBakedModel display(IBakedModel model,ItemCameraTransforms.TransformType type){
        if(HandModels.current==null)return net.minecraftforge.client.ForgeHooksClient.handleCameraTransforms(model,type);
        HandModels.apply(model,type);return model;
    }
}
