package com.viaversion.viaforge.development;

import com.viaversion.viaforge.hands.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Compare actual native and two-hand blocking paths with the original 26.2 matrices. */
final class BlockingRenderSmokeTest {
    static void verify(ItemStack sword)throws Exception {
        Minecraft mc=Minecraft.getMinecraft();RecordingRenderer renderer=new RecordingRenderer(mc);
        java.lang.reflect.Field item=ItemRenderer.class.getDeclaredField("itemToRender");item.setAccessible(true);item.set(renderer,sword);
        java.lang.reflect.Method block=ItemRenderer.class.getDeclaredMethod("doBlockTransformations");block.setAccessible(true);
        int previousHand=Offhand.useHand;
        GlStateManager.pushMatrix();
        try {
            mc.thePlayer.setItemInUse(sword,72000);
            float[] expected=reference(1,false);
            GlStateManager.loadIdentity();block.invoke(renderer);compare(expected,matrix(),"native main hand");
            for(int hand=0;hand<2;hand++) {
                int sign=((hand==1)^Offhand.mainLeft())?-1:1;
                expected=reference(sign,true);Offhand.useHand=hand;
                GlStateManager.loadIdentity();HandRenderer.draw(renderer,hand,sword,0,0,.5F,0);
                compare(expected,renderer.matrix,"hand "+hand);
            }
        }finally{mc.thePlayer.clearItemInUse();Offhand.useHand=previousHand;GlStateManager.popMatrix();}
    }
    private static float[] reference(int sign,boolean arm) {
        GlStateManager.loadIdentity();
        if(arm)GlStateManager.translate(sign*.56F,-.52F,-.72F);
        // ItemInHandRenderer.submitArmWithItem, official 26.2 BLOCK/non-ShieldItem.
        GlStateManager.translate(sign*-.14142136F,.08F,.14142136F);
        GlStateManager.rotate(-102.25F,1,0,0);GlStateManager.rotate(sign*13.365F,0,1,0);GlStateManager.rotate(sign*78.05F,0,0,1);
        return matrix();
    }
    private static float[] matrix(){java.nio.FloatBuffer b=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,b);float[] values=new float[16];b.get(values);return values;}
    private static void compare(float[] expected,float[] actual,String path) {
        require(actual!=null,"Blocking sword rendered: "+path);
        for(int i=0;i<16;i++)require(Math.abs(expected[i]-actual[i])<.00001,"Original blocking transform "+path+" element "+i);
    }
    private static final class RecordingRenderer extends ItemRenderer {
        float[] matrix;
        RecordingRenderer(Minecraft mc){super(mc);}
        @Override public void renderItem(EntityLivingBase entity,ItemStack stack,ItemCameraTransforms.TransformType type){matrix=BlockingRenderSmokeTest.matrix();}
    }
}
