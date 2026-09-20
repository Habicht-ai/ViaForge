package com.viaversion.viaforge.items;

import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.compatibility.ServerSession;
import java.nio.IntBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Original inventory-only framing; never changes flight authority, hitbox or movement. */
public final class InventoryEntityPreview {
    private static EntityLivingBase capturedEntity;
    private static float capturedYaw;

    public static boolean enabled() { return ServerSession.rule(ClientRule.BOUNDED_INVENTORY_PREVIEW); }
    public static boolean captured(EntityLivingBase entity) { return entity == capturedEntity; }
    public static float flightYaw() { return capturedYaw; }

    public static void draw(int x0,int y0,int x1,int y1,int size,float mouseX,float mouseY,EntityLivingBase entity) {
        Minecraft mc=Minecraft.getMinecraft();RenderManager manager=mc.getRenderManager();
        float body=entity.renderYawOffset,yaw=entity.rotationYaw,pitch=entity.rotationPitch;
        float head=entity.rotationYawHead,previousHead=entity.prevRotationYawHead;
        float viewYaw=manager.playerViewY,viewPitch=manager.playerViewX;
        boolean shadow=manager.isRenderShadow();
        EntityLivingBase previousEntity=capturedEntity;float previousYaw=capturedYaw;
        boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        IntBuffer bounds=BufferUtils.createIntBuffer(16);GL11.glGetInteger(GL11.GL_SCISSOR_BOX,bounds);
        GlStateManager.enableColorMaterial();GlStateManager.pushMatrix();
        try {
            int scale=new ScaledResolution(mc).getScaleFactor();
            int left=x0*scale,bottom=mc.displayHeight-y1*scale,right=x1*scale,top=mc.displayHeight-y0*scale;
            if(scissor){left=Math.max(left,bounds.get(0));bottom=Math.max(bottom,bounds.get(1));right=Math.min(right,bounds.get(0)+bounds.get(2));top=Math.min(top,bounds.get(1)+bounds.get(3));}
            GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(left,bottom,Math.max(0,right-left),Math.max(0,top-bottom));
            float centerX=(x0+x1)/2F,centerY=(y0+y1)/2F;
            float xAngle=(float)Math.atan((centerX-mouseX)/40F)*20;
            float yAngle=(float)Math.atan((centerY-mouseY)/40F)*20;
            boolean flying=ServerElytraFlight.flying(entity);
            boolean capture=ServerSession.rule(ClientRule.CAPTURED_INVENTORY_FLIGHT)&&flying;
            capturedEntity=flying?entity:null;
            // Since 1.21.11 flight data is extracted BEFORE the mouse-facing pose.
            Vec3 flightLook=capture?entity.getLook(1):null;
            GlStateManager.translate(centerX,centerY,50);
            GlStateManager.scale(size,size,-size);
            GlStateManager.translate(0,entity.height/2F+.0625F,0);
            GlStateManager.rotate(180,0,0,1);GlStateManager.rotate(yAngle,1,0,0);
            RenderHelper.enableStandardItemLighting();
            entity.renderYawOffset=180+xAngle;
            entity.rotationYaw=entity.rotationYawHead=entity.prevRotationYawHead=180+xAngle*2;
            entity.rotationPitch=capture?0:-yAngle;
            if(flying)capturedYaw=flightYaw(entity,capture?flightLook:entity.getLook(1));
            manager.setPlayerViewY(180);manager.playerViewX=yAngle;manager.setRenderShadow(false);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
            manager.renderEntityWithPosYaw(entity,0,0,0,0,1);
        } finally {
            entity.renderYawOffset=body;entity.rotationYaw=yaw;entity.rotationPitch=pitch;
            entity.rotationYawHead=head;entity.prevRotationYawHead=previousHead;
            manager.playerViewY=viewYaw;manager.playerViewX=viewPitch;manager.setRenderShadow(shadow);
            capturedEntity=previousEntity;capturedYaw=previousYaw;
            GlStateManager.popMatrix();RenderHelper.disableStandardItemLighting();GlStateManager.disableRescaleNormal();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);GlStateManager.disableTexture2D();GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glScissor(bounds.get(0),bounds.get(1),bounds.get(2),bounds.get(3));
            if(scissor)GL11.glEnable(GL11.GL_SCISSOR_TEST);else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }
    private static float flightYaw(EntityLivingBase entity,Vec3 look) {
        double motion=entity.motionX*entity.motionX+entity.motionZ*entity.motionZ;
        double horizontal=look.xCoord*look.xCoord+look.zCoord*look.zCoord;
        boolean limited=ServerSession.rule(ClientRule.LIMITED_INVENTORY_FLIGHT_YAW);
        double threshold=limited?1.0E-5F:0;
        if(motion<=threshold||horizontal<=threshold)return 0;
        double dot=(entity.motionX*look.xCoord+entity.motionZ*look.zCoord)/Math.sqrt(motion*horizontal);
        double cross=entity.motionX*look.zCoord-entity.motionZ*look.xCoord;
        return(float)(Math.signum(cross)*Math.acos(limited?Math.min(1,Math.abs(dot)):Math.max(-1,Math.min(1,dot)))*180/Math.PI);
    }
    private InventoryEntityPreview(){ }
}
