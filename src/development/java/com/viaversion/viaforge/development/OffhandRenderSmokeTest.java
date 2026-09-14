package com.viaversion.viaforge.development;
import com.viaversion.viaforge.hands.*;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import java.nio.file.Paths;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;
final class OffhandRenderSmokeTest {
    static void verify(BlockVersionProfile profile,WorldClient world){
        com.viaversion.viaforge.common.platform.ViaForgeConfig config=com.viaversion.viaforge.common.ViaForgeCommon.getManager().getConfig();boolean before=config.isLeftMainHand();
        try{for(boolean left:new boolean[]{false,true}){config.set(com.viaversion.viaforge.common.platform.ViaForgeConfig.LEFT_MAIN_HAND,left);capture(profile,world);}}
        finally{config.set(com.viaversion.viaforge.common.platform.ViaForgeConfig.LEFT_MAIN_HAND,before);}
    }
    private static void capture(BlockVersionProfile profile,WorldClient world){
        Minecraft mc=Minecraft.getMinecraft();Framebuffer frame=new Framebuffer(1100,600,true);frame.setFramebufferColor(.12F,.16F,.22F,1);frame.framebufferClear();frame.bindFramebuffer(true);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.pushMatrix();GlStateManager.loadIdentity();GLU.gluPerspective(70,1100F/600,.05F,100);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.pushMatrix();GlStateManager.loadIdentity();
        try{GlStateManager.enableDepth();GlStateManager.enableTexture2D();GlStateManager.color(1,1,1,1);RenderHelper.enableStandardItemLighting();OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
            HandRenderer.draw(mc.getItemRenderer(),0,mc.thePlayer.getHeldItem(),0,0,.5F,0);HandRenderer.draw(mc.getItemRenderer(),1,Offhand.get(),0,0,.5F,0);
            require(GL11.glGetError()==0,"Two original hand models render without OpenGL errors");
            ScreenShotHelper.saveScreenshot(Paths.get(System.getenv("VIAFORGE_BLOCK_SMOKE_TEST")).toAbsolutePath().getParent().toFile(),"offhand-"+(Offhand.mainLeft()?"left-":"")+profile.resourceVersion()+".png",1100,600,frame);
        }finally{GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.popMatrix();GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.popMatrix();GlStateManager.matrixMode(GL11.GL_MODELVIEW);RenderHelper.disableStandardItemLighting();frame.deleteFramebuffer();mc.getFramebuffer().bindFramebuffer(true);}
    }
}
