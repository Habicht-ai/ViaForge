package com.viaversion.viaforge.mixin.impl.mobs;
import com.viaversion.viaforge.mobs.DragonVisualState;
import com.viaversion.viaforge.mobs.models.ServerDragonModel;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.entity.RenderDragon;
import net.minecraft.entity.boss.EntityDragon;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RenderDragon.class)
public abstract class MixinDragonModel extends net.minecraft.client.renderer.entity.RenderLiving<EntityDragon> {
    protected MixinDragonModel(net.minecraft.client.renderer.entity.RenderManager manager, ModelBase model, float shadow) { super(manager, model, shadow); }
    @Shadow protected ModelDragon modelDragon;
    @Unique private ModelDragon viaForge$vanillaModel,viaForge$serverModel;
    @Inject(method="doRender(Lnet/minecraft/entity/boss/EntityDragon;DDDFF)V",at=@At("HEAD"))
    private void phaseModel(EntityDragon dragon,double x,double y,double z,float yaw,float partial,CallbackInfo ci) {
        if(viaForge$vanillaModel==null) viaForge$vanillaModel=modelDragon;
        if(DragonVisualState.phase(dragon)>=0) {
            if(viaForge$serverModel==null) viaForge$serverModel=new ServerDragonModel();
            modelDragon=viaForge$serverModel;
        } else modelDragon=viaForge$vanillaModel;
        mainModel=modelDragon;
    }
}
