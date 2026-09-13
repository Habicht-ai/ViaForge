package com.viaversion.viaforge.mobs;
import com.viaversion.nbt.tag.*;
import com.viaversion.viaforge.items.ServerEntityViews;
import com.viaversion.viaforge.mobs.models.ParrotModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.world.World;
import java.util.UUID;

public final class ShoulderParrots implements LayerRenderer<AbstractClientPlayer> {
    private final ParrotModel model = new ParrotModel();
    public static ServerMob create(World world,Object value) {
        if (!(value instanceof CompoundTag)) return null;
        CompoundTag tag=(CompoundTag)value;
        Tag id=tag.get("id"); if (!(id instanceof StringTag) || !((StringTag)id).getValue().equals("minecraft:parrot")) return null;
        MobState state=new MobState(340,105);
        Tag variant=tag.get("Variant"); if (variant instanceof NumberTag) state.data.put(state.first+3,((NumberTag)variant).asInt());
        return new ServerMob(world,state,new UUID(0,0));
    }
    @Override public void doRenderLayer(AbstractClientPlayer player,float limb,float amount,float partial,float age,float yaw,float pitch,float scale) {
        ServerEntityViews.View view=ServerEntityViews.get(player.getEntityId());
        if (view == null) return;
        render(player,view.leftShoulder,true,limb,amount,scale); render(player,view.rightShoulder,false,limb,amount,scale);
    }
    private void render(AbstractClientPlayer player,ServerMob parrot,boolean left,float limb,float amount,float scale) {
        if (parrot == null) return;
        ServerMobRenderer renderer=(ServerMobRenderer)Minecraft.getMinecraft().getRenderManager().<ServerMob>getEntityRenderObject(parrot);
        GlStateManager.pushMatrix(); GlStateManager.color(1,1,1,1);
        try {
            GlStateManager.translate(left ? .4F : -.4F,player.isSneaking() ? -1.3F : -1.5F,0);
            Minecraft.getMinecraft().getTextureManager().bindTexture(renderer.texture(parrot));
            parrot.onGround = true; model.isChild = false;
            model.setLivingAnimations(parrot,limb,amount,0); model.setRotationAngles(limb,amount,0,0,0,scale,parrot);
            model.render(parrot,limb,amount,0,0,0,scale);
        } finally { GlStateManager.popMatrix(); }
    }
    @Override public boolean shouldCombineTextures() { return false; }
}
