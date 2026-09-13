package com.viaversion.viaforge.development;

import com.viaversion.viaforge.boats.*;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import java.nio.file.Paths;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

final class BoatRenderSmokeTest {
    static void verify(BlockVersionProfile profile,WorldClient world) {
        Minecraft mc=Minecraft.getMinecraft(); Framebuffer frame=new Framebuffer(1200,600,true);frame.setFramebufferColor(.12F,.16F,.22F,1);frame.framebufferClear();frame.bindFramebuffer(true);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.pushMatrix();GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.pushMatrix();
        try {
            ServerBoat boat=new ServerBoat(world,profile.protocol()); ServerBoatRenderer renderer=(ServerBoatRenderer)mc.getRenderManager().<ServerBoat>getEntityRenderObject(boat);
            for(int wood=0;wood<6;wood++) {
                boat.wood=wood;GlStateManager.viewport(wood%3*400,300-wood/3*300,400,300);
                GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.loadIdentity();GLU.gluPerspective(38,4F/3,.05F,100);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.loadIdentity();GLU.gluLookAt(3,2.3F,3,0,0,0,0,1,0);
                GlStateManager.enableDepth();GlStateManager.enableTexture2D();GlStateManager.enableAlpha();GlStateManager.color(1,1,1,1);
                RenderHelper.enableStandardItemLighting();OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
                renderer.doRender(boat,0,0,0,0,.5F);
            }
            require(GL11.glGetError()==0,"Original boat geometry renders without OpenGL errors");
            ScreenShotHelper.saveScreenshot(Paths.get(System.getenv("VIAFORGE_BLOCK_SMOKE_TEST")).toAbsolutePath().getParent().toFile(),"boats-"+profile.resourceVersion()+".png",1200,600,frame);
        } finally {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.popMatrix();GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.popMatrix();GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            RenderHelper.disableStandardItemLighting();frame.deleteFramebuffer();mc.getFramebuffer().bindFramebuffer(true);
        }
    }
}
