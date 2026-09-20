package com.viaversion.viaforge.hands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import com.viaversion.viaforge.items.ServerCombatState;
import com.viaversion.viaforge.mixin.impl.hands.HandRenderAccess;

/** Separate equip/swing/use transforms, matching the original two-hand renderer. */
public final class HandRenderer {
    private static final ItemStack[] rendered=new ItemStack[2];
    private static final float[] equipped=new float[2],previous=new float[2];
    public static void clear(){for(int i=0;i<2;i++){rendered[i]=null;equipped[i]=previous[i]=0;}}
    public static void reset(int hand){
        equipped[hand]=0;
        // A right-handed player with no offhand uses the native main-hand renderer.
        if(hand==0)Minecraft.getMinecraft().getItemRenderer().resetEquippedProgress();
    }
    public static void tick(){
        for(int hand=0;hand<2;hand++){
            previous[hand]=equipped[hand];ItemStack held=Offhand.stack(hand);
            if(ItemStack.areItemStacksEqual(rendered[hand],held))rendered[hand]=held;
            float strength=ServerCombatState.strength(1),target=rendered[hand]==held?(hand==0?strength*strength*strength:1):0;
            equipped[hand]+=MathHelper.clamp_float(target-equipped[hand],-.4F,.4F);
            if(equipped[hand]<.1F)rendered[hand]=held;
        }
    }
    public static boolean needed(){
        return Offhand.active() && (Offhand.get()!=null || rendered[1]!=null
                || rocket(Offhand.stack(0)) || rocket(rendered[0])
                || Offhand.mainLeft() && (Offhand.stack(0)==null || !(Offhand.stack(0).getItem() instanceof ItemMap)));
    }
    private static boolean rocket(ItemStack stack){return stack!=null && stack.getItem()==net.minecraft.init.Items.fireworks;}
    public static void render(ItemRenderer renderer,float partial){
        Minecraft mc=Minecraft.getMinecraft();AbstractClientPlayer player=mc.thePlayer;HandRenderAccess access=(HandRenderAccess)renderer;
        float pitch=player.prevRotationPitch+(player.rotationPitch-player.prevRotationPitch)*partial;
        float yaw=player.prevRotationYaw+(player.rotationYaw-player.prevRotationYaw)*partial;
        access.viaForge$rotate(pitch,yaw);access.viaForge$light(player);access.viaForge$armLag(mc.thePlayer,partial);
        GlStateManager.enableRescaleNormal();
        try{
            for(int hand=0;hand<2;hand++){
                if(player.isUsingItem()&&player.getItemInUse().getItemUseAction()==EnumAction.BOW&&Offhand.useHand!=hand)continue;
                float swing=Offhand.swingHand==hand?player.getSwingProgress(partial):0;
                draw(renderer,hand,rendered[hand],1-(previous[hand]+(equipped[hand]-previous[hand])*partial),swing,partial,pitch);
            }
        }finally{GlStateManager.disableRescaleNormal();RenderHelper.disableStandardItemLighting();}
    }
    public static void draw(ItemRenderer renderer,int hand,ItemStack stack,float equip,float swing,float partial,float pitch){
        Minecraft mc=Minecraft.getMinecraft();AbstractClientPlayer p=mc.thePlayer;boolean left=(hand==1)^Offhand.mainLeft();int sign=left?-1:1;
        GlStateManager.pushMatrix();
        try{
            if(stack==null){if(hand==0&&!p.isInvisible())arm(sign,equip,swing);return;}
            if(stack.getItem() instanceof ItemMap){map(stack,sign,equip,swing);return;}
            if(p.isUsingItem()&&Offhand.useHand==hand&&p.getItemInUseCount()>0){
                EnumAction action=stack.getItemUseAction();
                if(action==EnumAction.EAT||action==EnumAction.DRINK){
                    float ticks=p.getItemInUseCount()-partial+1,progress=ticks/stack.getMaxItemUseDuration();
                    if(progress<.8F)GlStateManager.translate(0,MathHelper.abs(MathHelper.cos(ticks/4*(float)Math.PI)*.1F),0);
                    float eat=1-(float)Math.pow(progress,27);GlStateManager.translate(sign*eat*.6F,-eat*.5F,0);GlStateManager.rotate(sign*eat*90,0,1,0);GlStateManager.rotate(eat*10,1,0,0);GlStateManager.rotate(sign*eat*30,0,0,1);
                }
                base(sign,equip);
                if(action==EnumAction.BOW){
                    GlStateManager.translate(sign*-.2785682F,.18344387F,.15731531F);GlStateManager.rotate(-13.935F,1,0,0);GlStateManager.rotate(sign*35.3F,0,1,0);GlStateManager.rotate(sign*-9.785F,0,0,1);
                    float ticks=stack.getMaxItemUseDuration()-(p.getItemInUseCount()-partial+1),pull=ticks/20;pull=Math.min(1,(pull*pull+pull*2)/3);
                    if(pull>.1)GlStateManager.translate(0,MathHelper.sin((ticks-.1F)*1.3F)*(pull-.1F)*.004F,0);
                    GlStateManager.translate(0,0,pull*.04F);GlStateManager.scale(1,1,1+pull*.2F);GlStateManager.rotate(sign*45,0,-1,0);
                }
            }else{
                float root=MathHelper.sqrt_float(swing),wave=MathHelper.sin(root*(float)Math.PI);
                GlStateManager.translate(sign*-.4F*wave,.2F*MathHelper.sin(root*(float)Math.PI*2),-.2F*MathHelper.sin(swing*(float)Math.PI));base(sign,equip);
                GlStateManager.rotate(sign*(45-MathHelper.sin(swing*swing*(float)Math.PI)*20),0,1,0);GlStateManager.rotate(sign*-20*wave,0,0,1);GlStateManager.rotate(-80*wave,1,0,0);GlStateManager.rotate(sign*-45,0,1,0);
            }
            ItemStack old=HandModels.current;boolean previousLeft=HandModels.left;HandModels.current=stack;HandModels.left=left;
            try{renderer.renderItem(p,stack,ItemCameraTransforms.TransformType.FIRST_PERSON);}finally{HandModels.current=old;HandModels.left=previousLeft;}
        }finally{GlStateManager.popMatrix();}
    }
    private static void base(int sign,float equip){GlStateManager.translate(sign*.56F,-.52F-equip*.6F,-.72F);}
    private static void arm(int sign,float equip,float swing){
        Minecraft mc=Minecraft.getMinecraft();float root=MathHelper.sqrt_float(swing),wave=MathHelper.sin(root*(float)Math.PI);
        GlStateManager.translate(sign*(-.3F*wave+.64000005F),.4F*MathHelper.sin(root*(float)Math.PI*2)-.6F-equip*.6F,-.4F*MathHelper.sin(swing*(float)Math.PI)-.71999997F);
        GlStateManager.rotate(sign*45,0,1,0);GlStateManager.rotate(sign*MathHelper.sin(root*(float)Math.PI)*70,0,1,0);GlStateManager.rotate(sign*MathHelper.sin(swing*swing*(float)Math.PI)*-20,0,0,1);
        mc.getTextureManager().bindTexture(mc.thePlayer.getLocationSkin());GlStateManager.translate(sign*-1,3.6F,3.5F);GlStateManager.rotate(sign*120,0,0,1);GlStateManager.rotate(200,1,0,0);GlStateManager.rotate(sign*-135,0,1,0);GlStateManager.translate(sign*5.6F,0,0);
        RenderPlayer render=(RenderPlayer)mc.getRenderManager().<AbstractClientPlayer>getEntityRenderObject(mc.thePlayer);GlStateManager.disableCull();
        if(sign==1)render.renderRightArm(mc.thePlayer);else render.renderLeftArm(mc.thePlayer);GlStateManager.enableCull();
    }
    private static void map(ItemStack stack,int sign,float equip,float swing){
        Minecraft mc=Minecraft.getMinecraft();GlStateManager.translate(sign*.125F,-.125F,0);
        if(!mc.thePlayer.isInvisible()){GlStateManager.pushMatrix();GlStateManager.rotate(sign*10,0,0,1);arm(sign,equip,swing);GlStateManager.popMatrix();}
        GlStateManager.translate(sign*.51F,-.08F-equip*1.2F,-.75F);float root=MathHelper.sqrt_float(swing),wave=MathHelper.sin(root*(float)Math.PI);
        GlStateManager.translate(sign*-.5F*wave,.4F*MathHelper.sin(root*(float)Math.PI*2)-.3F*wave,-.3F*MathHelper.sin(swing*(float)Math.PI));GlStateManager.rotate(wave*-45,1,0,0);GlStateManager.rotate(sign*wave*-30,0,1,0);
        GlStateManager.rotate(180,0,1,0);GlStateManager.rotate(180,0,0,1);GlStateManager.scale(.38F,.38F,.38F);GlStateManager.disableLighting();
        mc.getTextureManager().bindTexture(new ResourceLocation("textures/map/map_background.png"));GlStateManager.translate(-.5F,-.5F,0);GlStateManager.scale(1/128F,1/128F,1/128F);
        WorldRenderer v=Tessellator.getInstance().getWorldRenderer();v.begin(7,net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);v.pos(-7,135,0).tex(0,1).endVertex();v.pos(135,135,0).tex(1,1).endVertex();v.pos(135,-7,0).tex(1,0).endVertex();v.pos(-7,-7,0).tex(0,0).endVertex();Tessellator.getInstance().draw();
        net.minecraft.world.storage.MapData data=((ItemMap)stack.getItem()).getMapData(stack,mc.theWorld);if(data!=null)mc.entityRenderer.getMapItemRenderer().renderMap(data,false);GlStateManager.enableLighting();
    }
    private HandRenderer(){ }
}
