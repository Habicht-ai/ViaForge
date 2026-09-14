package com.viaversion.viaforge.mobs;

import com.viaversion.viaforge.common.blocks.MobKind;
import com.viaversion.viaforge.mobs.models.*;
import com.viaversion.viaforge.items.ServerHeldItemRenderer;
import java.util.*;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;

public final class ServerMobRenderer extends RenderLiving<ServerMob> {
    public static final String[] COLORS = {"white","orange","magenta","light_blue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black"};
    private final Map<MobKind, ModelBase> models = new EnumMap<>(MobKind.class);
    private final QuadrupedMobModel carpet = new QuadrupedMobModel(true, .5F);
    private final BipedMobModel strayClothes = new BipedMobModel(MobKind.STRAY, .25F, true);
    private final ModelBiped illagerGrip = new ModelBiped();
    public ServerMobRenderer(RenderManager manager) {
        super(manager, new ShulkerModel(), .5F);
        for (MobKind kind : MobKind.values()) if (kind.custom()) {
            ModelBase model;
            if (kind == MobKind.SHULKER) model = new ShulkerModel();
            else if (kind == MobKind.POLAR_BEAR || kind == MobKind.LLAMA) model = new QuadrupedMobModel(kind == MobKind.LLAMA, 0);
            else if (kind == MobKind.PARROT) model = new ParrotModel();
            else if (kind.illager()) model = new IllagerModel();
            else model = new BipedMobModel(kind, 0, false);
            models.put(kind, model);
        }
        addLayer(new DetailLayer()); addLayer(new ArmorLayer(this));
    }
    public ModelBase model(MobKind kind) { return models.get(kind); }
    @Override public void doRender(ServerMob mob, double x, double y, double z, float yaw, float partial) {
        mainModel = models.get(mob.kind());
        shadowSize = mob.kind() == MobKind.SHULKER ? 0 : mob.kind() == MobKind.PARROT || mob.kind() == MobKind.VEX ? .3F : mob.kind() == MobKind.POLAR_BEAR || mob.kind() == MobKind.LLAMA ? .7F : .5F;
        if (mob.kind() == MobKind.SHULKER && mob.teleportTicks > 0 && mob.attachment != null && mob.oldAttachment != null) {
            double progress = (mob.teleportTicks - partial) / 6; progress *= progress;
            x -= (mob.attachment.getX() - mob.oldAttachment.getX()) * progress;
            y -= (mob.attachment.getY() - mob.oldAttachment.getY()) * progress;
            z -= (mob.attachment.getZ() - mob.oldAttachment.getZ()) * progress;
        }
        if (mob.kind() == MobKind.ILLUSIONER && mob.isInvisible()) {
            float age = mob.ticksExisted+partial;
            double previous = mob.illusionTicks <= 0 ? 0 : Math.pow((mob.illusionTicks-partial)/3,.25);
            for(int i=0;i<4;i++) {
                Vec3 a = mob.illusions[0][i], b = mob.illusions[1][i];
                super.doRender(mob,x+b.xCoord*(1-previous)+a.xCoord*previous+MathHelper.cos(i+age*.5F)*.025,
                        y+b.yCoord*(1-previous)+a.yCoord*previous+MathHelper.cos(i+age*.75F)*.0125,
                        z+b.zCoord*(1-previous)+a.zCoord*previous+MathHelper.cos(i+age*.7F)*.025,yaw,partial);
            }
        } else super.doRender(mob, x,y,z,yaw,partial);
    }
    @Override protected void renderModel(ServerMob mob,float limb,float amount,float age,float yaw,float pitch,float scale) {
        if (mob.kind() == MobKind.ILLUSIONER && mob.isInvisible()) {
            if (bindEntityTexture(mob)) mainModel.render(mob,limb,amount,age,yaw,pitch,scale);
        } else super.renderModel(mob,limb,amount,age,yaw,pitch,scale);
    }
    public ResourceLocation texture(ServerMob mob) {
        String name;
        switch (mob.kind()) {
            case SHULKER: name = !com.viaversion.viaforge.compatibility.ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.COLORED_SHULKERS) ? "shulker/endergolem.png" : "shulker/shulker_" + COLORS[MathHelper.clamp_int(mob.state.number(mob.state.first+3,10),0,15)] + ".png"; break;
            case POLAR_BEAR: name = "bear/polarbear.png"; break;
            case LLAMA: name = "llama/llama_"+new String[]{"creamy","white","brown","gray"}[MathHelper.clamp_int(mob.state.number(mob.state.first+6,0),0,3)]+".png"; break;
            case PARROT: name = "parrot/parrot_"+new String[]{"red_blue","blue","green","yellow_blue","grey"}[MathHelper.clamp_int(mob.state.number(mob.state.first+3,0),0,4)]+".png"; break;
            case EVOKER: name = "illager/evoker.png"; break;
            case VINDICATOR: name = "illager/vindicator.png"; break;
            case ILLUSIONER: name = "illager/illusionist.png"; break;
            case VEX: name = "illager/vex"+(mob.charging() ? "_charging" : "")+".png"; break;
            case HUSK: name = "zombie/husk.png"; break;
            case ZOMBIE_VILLAGER:
                name = "zombie_villager/"+new String[]{"zombie_farmer","zombie_librarian","zombie_priest","zombie_smith","zombie_butcher","zombie_villager"}[MathHelper.clamp_int(mob.state.profession(),0,5)]+".png"; break;
            case STRAY: name = "skeleton/stray.png"; break;
            case WITHER_SKELETON: name = "skeleton/wither_skeleton.png"; break;
            case SKELETON: name = "skeleton/skeleton.png"; break;
            default: name = "zombie/zombie.png";
        }
        return new ResourceLocation("viaforge","textures/entity/"+name);
    }
    @Override protected ResourceLocation getEntityTexture(ServerMob mob) { return texture(mob); }
    @Override protected void preRenderCallback(ServerMob mob, float partial) {
        float scale = mob.kind() == MobKind.SHULKER ? .999F : mob.kind() == MobKind.POLAR_BEAR ? 1.2F : mob.kind() == MobKind.VEX ? .4F
                : mob.kind() == MobKind.HUSK ? 1.0625F : mob.kind() == MobKind.WITHER_SKELETON ? 1.2F : mob.kind().illager() ? .9375F : 1;
        GlStateManager.scale(scale,scale,scale);
    }
    @Override protected float handleRotationFloat(ServerMob mob, float partial) {
        if (mob.kind() == MobKind.PARROT) return (MathHelper.sin(mob.prevFlap + (mob.flap-mob.prevFlap)*partial)+1)*(mob.prevFlapSpeed+(mob.flapSpeed-mob.prevFlapSpeed)*partial);
        return super.handleRotationFloat(mob,partial);
    }
    @Override protected void rotateCorpse(ServerMob mob, float age, float yaw, float partial) {
        if (mob.kind() == MobKind.ZOMBIE_VILLAGER && mob.state.converting()) yaw += MathHelper.cos(mob.ticksExisted*3.25F)*(float)Math.PI*.25F;
        super.rotateCorpse(mob,age,yaw,partial);
        if (mob.kind() == MobKind.SHULKER) {
            switch (EnumFacing.getFront(mob.state.number(mob.state.first,0))) {
                case EAST: GlStateManager.translate(.5F,.5F,0); GlStateManager.rotate(90,1,0,0); GlStateManager.rotate(90,0,0,1); break;
                case WEST: GlStateManager.translate(-.5F,.5F,0); GlStateManager.rotate(90,1,0,0); GlStateManager.rotate(-90,0,0,1); break;
                case NORTH: GlStateManager.translate(0,.5F,-.5F); GlStateManager.rotate(90,1,0,0); break;
                case SOUTH: GlStateManager.translate(0,.5F,.5F); GlStateManager.rotate(90,1,0,0); GlStateManager.rotate(180,0,0,1); break;
                case UP: GlStateManager.translate(0,1,0); GlStateManager.rotate(180,1,0,0); break;
                default: break;
            }
        }
    }
    private final class DetailLayer implements LayerRenderer<ServerMob> {
        @Override public void doRenderLayer(ServerMob mob,float limb,float amount,float partial,float age,float yaw,float pitch,float scale) {
            if (mob.kind() == MobKind.SHULKER) {
                GlStateManager.pushMatrix();
                switch (EnumFacing.getFront(mob.state.number(mob.state.first,0))) {
                    case EAST: GlStateManager.rotate(90,0,0,1); GlStateManager.rotate(90,1,0,0); GlStateManager.translate(1,-1,0); GlStateManager.rotate(180,0,1,0); break;
                    case WEST: GlStateManager.rotate(-90,0,0,1); GlStateManager.rotate(90,1,0,0); GlStateManager.translate(-1,-1,0); GlStateManager.rotate(180,0,1,0); break;
                    case NORTH: GlStateManager.rotate(90,1,0,0); GlStateManager.translate(0,-1,-1); break;
                    case SOUTH: GlStateManager.rotate(180,0,0,1); GlStateManager.rotate(90,1,0,0); GlStateManager.translate(0,-1,1); break;
                    case UP: GlStateManager.rotate(180,1,0,0); GlStateManager.translate(0,-2,0); break;
                    default: break;
                }
                bindEntityTexture(mob); ((ShulkerModel)mainModel).head.render(scale); GlStateManager.popMatrix();
            } else if (mob.kind() == MobKind.LLAMA) {
                int color = mob.state.number(mob.state.first+5,-1);
                if (color >= 0 && color < 16) {
                    bindTexture(new ResourceLocation("viaforge","textures/entity/llama/decor/decor_"+COLORS[color]+".png"));
                    carpet.setModelAttributes(mainModel); carpet.render(mob,limb,amount,age,yaw,pitch,scale);
                }
            } else if (mob.kind() == MobKind.STRAY) {
                bindTexture(new ResourceLocation("viaforge","textures/entity/skeleton/stray_overlay.png"));
                strayClothes.setModelAttributes(mainModel); strayClothes.setLivingAnimations(mob,limb,amount,partial); strayClothes.render(mob,limb,amount,age,yaw,pitch,scale);
            }
            if (mainModel instanceof ModelBiped) {
                ServerHeldItemRenderer.render((ModelBiped)mainModel,mob,mob.getHeldItem(),false);
                ServerHeldItemRenderer.render((ModelBiped)mainModel,mob,mob.offhand,true);
                new LayerCustomHead(((ModelBiped)mainModel).bipedHead).doRenderLayer(mob,limb,amount,partial,age,yaw,pitch,scale);
            } else if (mainModel instanceof IllagerModel && (mob.state.spell() != 0 || mob.state.armsRaised())) {
                IllagerModel model = (IllagerModel)mainModel;
                illagerGrip.bipedRightArm = model.rightArm; illagerGrip.bipedLeftArm = model.leftArm; illagerGrip.isChild = false;
                ServerHeldItemRenderer.render(illagerGrip,mob,mob.getHeldItem(),false);
                ServerHeldItemRenderer.render(illagerGrip,mob,mob.offhand,true);
            }
        }
        @Override public boolean shouldCombineTextures() { return false; }
    }
    private static final class ArmorLayer extends LayerBipedArmor {
        private final Map<MobKind, ModelBiped[]> armorModels = new EnumMap<>(MobKind.class);
        ArmorLayer(ServerMobRenderer renderer) { super(renderer); }
        @Override public void doRenderLayer(EntityLivingBase entity,float limb,float amount,float partial,float age,float yaw,float pitch,float scale) {
            ServerMob mob = (ServerMob)entity;
            if (!mob.kind().zombie() && !mob.kind().skeleton()) return;
            ModelBiped[] pair = armorModels.get(mob.kind());
            if (pair == null) { pair = new ModelBiped[]{new BipedMobModel(mob.kind(),.5F,true),new BipedMobModel(mob.kind(),1,true)}; armorModels.put(mob.kind(),pair); }
            modelLeggings = pair[0]; modelArmor = pair[1];
            super.doRenderLayer(entity,limb,amount,partial,age,yaw,pitch,scale);
        }
    }
}
