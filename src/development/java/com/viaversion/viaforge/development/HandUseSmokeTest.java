package com.viaversion.viaforge.development;

import com.viaversion.viaforge.common.ViaForgeCommon;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.common.platform.ViaForgeConfig;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.hands.*;
import com.viaversion.viaforge.items.*;
import java.lang.reflect.*;
import java.nio.FloatBuffer;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Real mouse input, native network mixin, target wire and actual GL/model poses. */
final class HandUseSmokeTest {
    static void presentation(List<Packet> sent)throws Exception {
        Minecraft mc=Minecraft.getMinecraft();EntityPlayerSP p=mc.thePlayer;
        ViaForgeConfig config=ViaForgeCommon.getManager().getConfig();boolean oldLeft=config.isLeftMainHand();
        ItemStack oldMain=p.getHeldItem(),oldOff=Offhand.get();float height=p.height;
        boolean sneak=p.movementInput.sneak;
        Field renderer=ItemRenderer.class.getDeclaredField("itemRenderer");renderer.setAccessible(true);
        Object original=renderer.get(mc.getItemRenderer());Recorder recorder=new Recorder(mc);renderer.set(mc.getItemRenderer(),recorder);
        GlStateManager.pushMatrix();
        try {
            hold(null);Offhand.set(null);
            // Use RenderPlayer's real first-person entry points, not just a
            // fresh ModelBiped: body poses previously leaked through this call.
            for(boolean slim:new boolean[]{false,true}) {
                net.minecraft.client.renderer.entity.RenderPlayer render=new net.minecraft.client.renderer.entity.RenderPlayer(mc.getRenderManager(),slim);
                for(float poseHeight:new float[]{1.8F,1.65F,1.5F,.6F}) {
                    ((com.viaversion.viaforge.mixin.impl.mobs.MobSizeAccess)p).viaForge$size(.6F,poseHeight);
                    p.movementInput.sneak=poseHeight!=1.8F;
                    for(int i=0;i<12;i++)ServerElytraVisuals.tick(p);
                    for(boolean left:new boolean[]{false,true}) {
                        GlStateManager.loadIdentity();
                        if(left)render.renderLeftArm(p);else render.renderRightArm(p);
                        ModelPlayer model=render.getMainModel();ModelRenderer arm=left?model.bipedLeftArm:model.bipedRightArm;
                        require(!model.isSneak&&Math.abs(arm.rotateAngleX)<1e-6&&Math.abs(arm.rotateAngleY)<1e-6,
                                "First-person arm stays neutral while standing/crouching/crawling, including slim: "+poseHeight+", "+arm.rotateAngleX+", "+arm.rotateAngleY);
                        require(!FirstPersonArm.active(),"Arm context restored after actual render");
                    }
                    if(poseHeight==1.5F) {
                        render.getMainModel().setRotationAngles(0,0,0,0,0,.0625F,p);
                        require(render.getMainModel().isSneak,"Following third-person draw still has its crouching pose");
                    }
                }
            }
            ((com.viaversion.viaforge.mixin.impl.mobs.MobSizeAccess)p).viaForge$size(.6F,1.8F);p.movementInput.sneak=false;
            ItemStack rocket=new ItemStack(Items.fireworks,8);hold(rocket);recorder.watched=rocket;
            for(boolean left:new boolean[]{false,true}) {
                config.set(ViaForgeConfig.LEFT_MAIN_HAND,left);Offhand.set(null);ready(sent);
                float[] alone=firstPerson(recorder);
                Offhand.set(new ItemStack(Items.stick));ready(sent);
                float[] withOther=firstPerson(recorder);
                require(Arrays.equals(alone,withOther),"Rocket grip is identical with empty or occupied opposite hand");
                double size=Math.sqrt(alone[0]*alone[0]+alone[1]*alone[1]+alone[2]*alone[2]);
                require(Math.abs(size-1.36)<1e-5,"Original generated-item .68 scale before native geometry half-scale: "+size);
                // Target resources must supply the actual rocket sprite/model.
                IBakedModel model=mc.getRenderItem().getItemModelMesher().getItemModel(rocket);
                require(model!=mc.getRenderItem().getItemModelMesher().getModelManager().getMissingModel(),"Rocket target model baked");
                require(model.getParticleTexture().getIconName().contains("firework"),"Rocket model uses original texture, not a fallback");
            }
            require(GL11.glGetError()==0,"First-person presentation has no GL errors");
        }finally {
            renderer.set(mc.getItemRenderer(),original);GlStateManager.popMatrix();
            ((com.viaversion.viaforge.mixin.impl.mobs.MobSizeAccess)p).viaForge$size(.6F,height);p.movementInput.sneak=sneak;
            // Twelve ticks clear the current .09/tick blend; the previous value
            // needs the thirteenth tick before a partial=0 model draw is neutral.
            for(int i=0;i<13;i++)ServerElytraVisuals.tick(p);
            hold(oldMain);Offhand.set(oldOff);HandRenderer.clear();config.set(ViaForgeConfig.LEFT_MAIN_HAND,oldLeft);sent.clear();
        }
    }
    private static float[] firstPerson(Recorder recorder) {
        GlStateManager.loadIdentity();recorder.matrix=null;
        Minecraft.getMinecraft().getItemRenderer().renderItemInFirstPerson(1);
        require(recorder.matrix!=null,"Rocket reaches actual first-person entry point");return recorder.matrix;
    }
    static void rockets(List<Packet> sent,ElytraFlightSmokeTest.Wire wire)throws Exception {
        if(!ServerSession.rule(ClientRule.ELYTRA_FIREWORKS))return;
        Minecraft mc=Minecraft.getMinecraft();EntityPlayerSP p=mc.thePlayer;
        MovingObjectPosition oldHit=mc.objectMouseOver;boolean creative=p.capabilities.isCreativeMode;
        ViaForgeConfig config=ViaForgeCommon.getManager().getConfig();boolean oldLeft=config.isLeftMainHand();
        ItemStack oldMain=p.getHeldItem(),oldOff=Offhand.get();
        try {
            miss(mc);
            for(boolean left:new boolean[]{false,true})for(boolean mode:new boolean[]{false,true})for(int hand=0;hand<2;hand++) {
                config.set(ViaForgeConfig.LEFT_MAIN_HAND,left);p.capabilities.isCreativeMode=mode;
                ItemStack rocket=new ItemStack(Items.fireworks,8),other=hand==0?ServerEntitySmokeTest.stack(442,0):null;
                hold(hand==0?rocket:other);Offhand.set(hand==1?rocket:other);
                ready(sent);click();
                require(ServerElytraFlight.flying(p)&&!p.isUsingItem(),"Rocket use consumes interaction instead of raising other-hand shield");
                require(Offhand.stack(hand)==rocket&&Offhand.stack(1-hand)==other&&rocket.stackSize==8,"Use preserves both inventory references and server-owned count");
                boolean swing=ServerSession.rule(ClientRule.ROCKET_USE_SWING);
                require(p.isSwingInProgress==swing&&(!swing||Offhand.swingHand==hand),"Original rocket swing boundary and selected arm");
                require(equipped()[hand]==0&&equipped()[1-hand]==1,"Only used hand dips after successful rocket use");
                if(hand==0)require(nativeEquip()==0,"Single main-hand renderer also receives equip reset");
                packets(sent,wire,hand,swing&&!ServerSession.rule(ClientRule.SERVER_OWNS_USE_SWING));
                require(Offhand.context==-1&&!Offhand.localUseSwing(),"Temporary hand/network context restored");
            }
            // Cooldowns and an ordinary grounded use must not predict a successful boost.
            hold(new ItemStack(Items.fireworks,8));Offhand.set(null);ready(sent);
            ServerItemCooldowns.set(401,20);click();
            require(!p.isSwingInProgress&&equipped()[0]==1&&sent.isEmpty(),"Cooling rocket has no use packet or animation");
            ServerItemCooldowns.set(401,0);p.onGround=true;wire.flag(p.getEntityId(),false,p.sendQueue);ready(sent);click();
            require(!p.isSwingInProgress&&equipped()[0]==1,"Grounded air use has no successful-flight animation");
            p.onGround=false;wire.flag(p.getEntityId(),true,p.sendQueue);
            // A genuine attack still has its native network action, including 26.3.
            sent.clear();p.isSwingInProgress=false;p.swingItem();
            require(sent.size()==1&&sent.get(0) instanceof C0APacketAnimation,"Use-only suppression does not remove native attacks");
        }finally{p.onGround=false;p.clearItemInUse();p.isSwingInProgress=false;p.swingProgress=p.prevSwingProgress=0;hold(oldMain);Offhand.set(oldOff);HandRenderer.clear();ServerItemCooldowns.clear();p.capabilities.isCreativeMode=creative;config.set(ViaForgeConfig.LEFT_MAIN_HAND,oldLeft);mc.objectMouseOver=oldHit;sent.clear();}
    }
    static void shields(List<Packet> sent,ElytraFlightSmokeTest.Wire wire)throws Exception {
        Minecraft mc=Minecraft.getMinecraft();EntityPlayerSP p=mc.thePlayer;
        MovingObjectPosition oldHit=mc.objectMouseOver;ViaForgeConfig config=ViaForgeCommon.getManager().getConfig();boolean oldLeft=config.isLeftMainHand();
        ItemStack oldMain=p.getHeldItem(),oldOff=Offhand.get();
        Field renderer=ItemRenderer.class.getDeclaredField("itemRenderer");renderer.setAccessible(true);
        Object original=renderer.get(mc.getItemRenderer());Recorder recorder=new Recorder(mc);renderer.set(mc.getItemRenderer(),recorder);
        GlStateManager.pushMatrix();
        try {
            miss(mc);
            for(boolean left:new boolean[]{false,true})for(int hand=0;hand<2;hand++) {
                config.set(ViaForgeConfig.LEFT_MAIN_HAND,left);ItemStack shield=ServerEntitySmokeTest.stack(442,0);
                hold(hand==0?shield:null);Offhand.set(hand==1?shield:null);ready(sent);
                float[] idle=render(recorder,hand,shield,0);
                click();require(p.getItemInUse()==shield&&Offhand.useHand==hand,"Mouse press raises the selected shield");
                packets(sent,wire,hand,false);
                float[] blocking=render(recorder,hand,shield,0);
                require(!Arrays.equals(idle,blocking),"Actual first-person renderer selects distinct original blocking transform");
                for(boolean slim:new boolean[]{false,true})for(float pitch:new float[]{-90,0,90})for(float yaw:new float[]{-90,0,90}) {
                    ModelPlayer model=new ModelPlayer(0,slim);model.setRotationAngles(0,0,0,yaw,pitch,.0625F,p);
                    boolean armLeft=(hand==1)^left;ModelRenderer arm=armLeft?model.bipedLeftArm:model.bipedRightArm;
                    boolean follow=ServerSession.rule(ClientRule.SHIELD_FOLLOWS_LOOK);
                    float x=-.9424779F+(follow?MathHelper.clamp_float(pitch*(float)Math.PI/180,-1.3962634F,.43633232F):0);
                    float y=(armLeft?1:-1)*.5235988F+(follow?MathHelper.clamp_float(yaw*(float)Math.PI/180,-.5235988F,.5235988F):0);
                    require(Math.abs(arm.rotateAngleX-x)<.00001&&Math.abs(arm.rotateAngleY-y)<.00001,"Original shield look clamps, handedness and slim/normal pose: "+arm.rotateAngleX+","+arm.rotateAngleY+" != "+x+","+y);
                    ModelRenderer sleeve=armLeft?model.bipedLeftArmwear:model.bipedRightArmwear;
                    require(sleeve.rotateAngleX==arm.rotateAngleX&&sleeve.rotateAngleY==arm.rotateAngleY,"Skin sleeve follows blocking arm");
                    p.clearItemInUse();hold(null);Offhand.set(null);model.setRotationAngles(0,0,0,90,90,.0625F,p);
                    require(model.heldItemLeft==0&&model.heldItemRight==0&&Math.abs(arm.rotateAngleX)<.00001,"Same model clears blocking pose after equipment removal");
                    hold(hand==0?shield:null);Offhand.set(hand==1?shield:null);p.setItemInUse(shield,72000);Offhand.useHand=hand;
                }
                mc.playerController.onStoppedUsingItem(p);
                require(Arrays.equals(idle,render(recorder,hand,shield,0)),"Releasing use restores actual first-person idle transform");
                require(!Arrays.equals(idle,render(recorder,hand,shield,.4F)),"Selected hand's swing changes actual rendered transform");
                ready(sent);ServerItemCooldowns.set(442,20);click();
                require(!p.isUsingItem()&&sent.isEmpty(),"Disabled shield cannot raise or send use");ServerItemCooldowns.set(442,0);
            }
            require(GL11.glGetError()==0,"Shield render transitions have no OpenGL errors");
        }finally{renderer.set(mc.getItemRenderer(),original);GlStateManager.popMatrix();p.clearItemInUse();hold(oldMain);Offhand.set(oldOff);HandRenderer.clear();ServerItemCooldowns.clear();config.set(ViaForgeConfig.LEFT_MAIN_HAND,oldLeft);mc.objectMouseOver=oldHit;sent.clear();}
    }
    private static void ready(List<Packet> sent)throws Exception {
        Minecraft mc=Minecraft.getMinecraft();mc.thePlayer.clearItemInUse();mc.thePlayer.isSwingInProgress=false;mc.thePlayer.swingProgressInt=0;
        mc.thePlayer.swingProgress=mc.thePlayer.prevSwingProgress=0;
        for(int i=0;i<15;i++){ServerCombatState.tick(mc.thePlayer);HandRenderer.tick();mc.getItemRenderer().updateEquippedItem();}
        require(equipped()[0]==1&&equipped()[1]==1&&nativeEquip()==1,"Equip animation settled before input");sent.clear();
    }
    private static void packets(List<Packet> sent,ElytraFlightSmokeTest.Wire wire,int hand,boolean swing)throws Exception {
        int uses=0,swings=0;
        for(Packet packet:sent) {
            if(packet instanceof C09PacketHeldItemChange)continue;
            require(packet instanceof C17PacketCustomPayload,"Input emitted only hand-aware packets: "+packet);
            C17PacketCustomPayload custom=(C17PacketCustomPayload)packet;
            int operation=custom.getBufferData().getUnsignedByte(custom.getBufferData().readerIndex());
            require(operation==1||operation==3,"Expected use/swing operation, got "+operation);
            if(operation==1)uses++;else swings++;
            wire.use(custom,operation==3,hand);
        }
        require(uses==1&&swings==(swing?1:0),"Exactly one use and version-correct swing, no extra 26.3 attack: "+uses+"/"+swings);sent.clear();
    }
    private static void click()throws Exception {Method click=Minecraft.class.getDeclaredMethod("rightClickMouse");click.setAccessible(true);click.invoke(Minecraft.getMinecraft());}
    private static void miss(Minecraft mc){mc.objectMouseOver=new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS,new Vec3(0,0,0),EnumFacing.UP,BlockPos.ORIGIN);}
    private static void hold(ItemStack stack){EntityPlayerSP p=Minecraft.getMinecraft().thePlayer;p.inventory.mainInventory[p.inventory.currentItem]=stack;}
    private static float[] equipped()throws Exception {Field f=HandRenderer.class.getDeclaredField("equipped");f.setAccessible(true);return(float[])f.get(null);}
    private static float nativeEquip()throws Exception {Field f=ItemRenderer.class.getDeclaredField("equippedProgress");f.setAccessible(true);return f.getFloat(Minecraft.getMinecraft().getItemRenderer());}
    private static float[] render(Recorder recorder,int hand,ItemStack stack,float swing) {
        GlStateManager.loadIdentity();recorder.matrix=null;
        HandRenderer.draw(Minecraft.getMinecraft().getItemRenderer(),hand,stack,0,swing,.5F,0);
        require(recorder.matrix!=null,"Held shield actually reaches renderer");return recorder.matrix;
    }
    private static final class Recorder extends RenderItem {
        float[] matrix;
        ItemStack watched;
        Recorder(Minecraft mc){super(mc.getTextureManager(),mc.getRenderItem().getItemModelMesher().getModelManager());}
        @Override public void renderItem(ItemStack stack,IBakedModel model){if(watched!=null&&stack!=watched)return;FloatBuffer buffer=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,buffer);matrix=new float[16];buffer.get(matrix);}
    }
    private HandUseSmokeTest(){ }
}
