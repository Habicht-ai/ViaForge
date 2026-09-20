package com.viaversion.viaforge.development;

import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.items.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.file.Paths;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.*;
import net.minecraft.world.WorldSettings.GameType;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Actual inventory call sites, target pose matrices, clipping pixels and state restoration. */
final class InventoryFlightSmokeTest {
    static void verify()throws Exception {
        Minecraft mc=Minecraft.getMinecraft();AbstractClientPlayer p=mc.thePlayer;
        int width=mc.displayWidth,height=mc.displayHeight,scale=mc.gameSettings.guiScale;
        GuiScreen screen=mc.currentScreen;RenderManager manager=mc.getRenderManager();
        GameType mode=mc.playerController.getCurrentGameType();
        boolean allow=p.capabilities.allowFlying,fly=p.capabilities.isFlying,creative=p.capabilities.isCreativeMode;
        float[] angles={p.renderYawOffset,p.rotationYaw,p.rotationPitch,p.prevRotationYawHead,p.rotationYawHead};
        boolean flying=ServerElytraFlight.flying(p);
        // Exercise banking separately from the preview's mouse rotation.
        p.rotationYaw=37;p.rotationYawHead=17;p.rotationPitch=-24;
        Field skins=RenderManager.class.getDeclaredField("skinMap");skins.setAccessible(true);
        Map<String,RenderPlayer> renderers=(Map<String,RenderPlayer>)skins.get(manager);
        Map<String,RenderPlayer> saved=new HashMap<>(renderers);
        Recorder recorder=new Recorder(manager);for(String key:renderers.keySet())renderers.put(key,recorder);
        Framebuffer frame=new Framebuffer(960,720,true);
        mc.displayWidth=960;mc.displayHeight=720;
        GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.pushMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.pushMatrix();
        manager.cacheActiveRenderInfo(mc.theWorld,mc.fontRendererObj,p,null,mc.gameSettings,1);
        try {
            int[] scales=ServerSession.profile().serverProtocol()==777?new int[]{1,2,3}:new int[]{2};
            for(int guiScale:scales)for(boolean inventoryCreative:new boolean[]{false,true}) {
                mc.gameSettings.guiScale=guiScale;
                require(new ScaledResolution(mc).getScaleFactor()==guiScale,"Requested GUI scale actually fits the test window");
                mc.playerController.setGameType(inventoryCreative?GameType.CREATIVE:GameType.SURVIVAL);
                p.capabilities.isFlying=false;
                GuiContainer gui=inventoryCreative?new GuiContainerCreative(mc.thePlayer):new GuiInventory(mc.thePlayer);
                mc.currentScreen=gui;gui.setWorldAndResolution(mc,960/guiScale,720/guiScale);
                if(inventoryCreative){Method tab=GuiContainerCreative.class.getDeclaredMethod("setCurrentCreativeTab",CreativeTabs.class);tab.setAccessible(true);tab.invoke(gui,CreativeTabs.tabInventory);}
                int left=field(GuiContainer.class,"guiLeft").getInt(gui),top=field(GuiContainer.class,"guiTop").getInt(gui);
                int[] rectangle=inventoryCreative?new int[]{left+73,top+6,left+105,top+49}:new int[]{left+26,top+8,left+75,top+78};
                Method draw=gui.getClass().getDeclaredMethod("drawGuiContainerBackgroundLayer",float.class,int.class,int.class);draw.setAccessible(true);
                for(int[] mouse:new int[][]{{-300,-200},{960/guiScale,720/guiScale},{left+88,top+27}}) {
                    if(!inventoryCreative){field(GuiInventory.class,"oldMouseX").setFloat(gui,mouse[0]);field(GuiInventory.class,"oldMouseY").setFloat(gui,mouse[1]);}
                    recorder.skip=true;setup(frame,guiScale);draw.invoke(gui,1F,mouse[0],mouse[1]);ByteBuffer baseline=pixels();
                    recorder.skip=false;recorder.matrix=null;setup(frame,guiScale);
                    float oldYaw=p.rotationYaw,oldPitch=p.rotationPitch,oldBody=p.renderYawOffset,oldHead=p.rotationYawHead,oldPreviousHead=p.prevRotationYawHead;
                    double mx=p.motionX,my=p.motionY,mz=p.motionZ;float playerHeight=p.height,eye=p.getEyeHeight();int flightTicks=ServerElytraVisuals.ticks(p);
                    draw.invoke(gui,1F,mouse[0],mouse[1]);
                    require(recorder.matrix!=null,"Actual inventory renders its player preview");
                    require(p.rotationYaw==oldYaw&&p.rotationPitch==oldPitch&&p.renderYawOffset==oldBody&&p.rotationYawHead==oldHead&&p.prevRotationYawHead==oldPreviousHead,"Preview restores every temporary player angle");
                    require(p.motionX==mx&&p.motionY==my&&p.motionZ==mz&&p.height==playerHeight&&p.getEyeHeight()==eye&&ServerElytraVisuals.ticks(p)==flightTicks&&ServerElytraFlight.flying(p)==flying,"Opening inventory preserves movement, hitbox, camera and authoritative flight");
                    require(!InventoryEntityPreview.captured(p)&&!GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),"Preview context and clipping do not leak into subsequent drawing");
                    boolean bounded=ServerSession.rule(ClientRule.BOUNDED_INVENTORY_PREVIEW);
                    require(recorder.clipped==bounded,"Original version boundary for inventory clipping");
                    ByteBuffer actual=pixels();int inside=0,outside=0;
                    for(int y=0;y<720;y++)for(int x=0;x<960;x++)if(baseline.getInt((y*960+x)*4)!=actual.getInt((y*960+x)*4)) {
                        if(x>=rectangle[0]*guiScale&&x<rectangle[2]*guiScale&&y>=720-rectangle[3]*guiScale&&y<720-rectangle[1]*guiScale)inside++;else outside++;
                    }
                    require((bounded?inside:inside+outside)>5,"Visible avatar pixels: inside="+inside+", outside="+outside+", creative="+inventoryCreative+", scale="+guiScale+", flying="+flying+", mouse="+Arrays.toString(mouse));
                    if(bounded)require(outside==0,"Flight preview cannot paint over armor/inventory slots: "+outside+" pixels");
                    float[] expected=expected(p,inventoryCreative,left,top,mouse,flying);
                    for(int i=0;i<16;i++)require(Math.abs(expected[i]-recorder.matrix[i])<.002,"Original inventory flight matrix "+ServerSession.getLoadedResourceVersion()+" creative="+inventoryCreative+" element="+i+": "+recorder.matrix[i]+" != "+expected[i]);
                    if(flying&&guiScale==2&&mouse[0]==left+88) {
                        // Save full GUI, including inventory slots, for visual review.
                        setup(frame,guiScale);gui.drawScreen(mouse[0],mouse[1],1);
                        ScreenShotHelper.saveScreenshot(Paths.get(System.getenv("VIAFORGE_BLOCK_SMOKE_TEST")).toAbsolutePath().getParent().toFile(),"inventory-flight-"+(inventoryCreative?"creative-":"survival-")+ServerSession.getLoadedResourceVersion()+".png",960,720,frame);
                    }
                }
                if(ServerSession.rule(ClientRule.BOUNDED_INVENTORY_PREVIEW)) {
                    // Existing outer clipping is intersected, then restored byte-for-byte.
                    setup(frame,guiScale);GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(0,0,120,120);
                    draw.invoke(gui,1F,-10,-10);IntBuffer restored=BufferUtils.createIntBuffer(16);GL11.glGetInteger(GL11.GL_SCISSOR_BOX,restored);
                    require(GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)&&restored.get(0)==0&&restored.get(1)==0&&restored.get(2)==120&&restored.get(3)==120,"Nested preview scissor restored");GL11.glDisable(GL11.GL_SCISSOR_TEST);
                }
                if(inventoryCreative)gui.onGuiClosed();
            }
            require(GL11.glGetError()==0,"Inventory flight preview has no OpenGL errors");
        }finally{
            renderers.clear();renderers.putAll(saved);mc.currentScreen=screen;mc.playerController.setGameType(mode);
            p.capabilities.allowFlying=allow;p.capabilities.isFlying=fly;p.capabilities.isCreativeMode=creative;
            p.renderYawOffset=angles[0];p.rotationYaw=angles[1];p.rotationPitch=angles[2];p.prevRotationYawHead=angles[3];p.rotationYawHead=angles[4];
            mc.displayWidth=width;mc.displayHeight=height;mc.gameSettings.guiScale=scale;
            GL11.glDisable(GL11.GL_SCISSOR_TEST);GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.popMatrix();GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.popMatrix();GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            frame.deleteFramebuffer();mc.getFramebuffer().bindFramebuffer(true);
        }
    }
    private static float[] expected(AbstractClientPlayer p,boolean creative,int left,int top,int[] mouse,boolean flying) {
        boolean bounded=ServerSession.rule(ClientRule.BOUNDED_INVENTORY_PREVIEW),capture=ServerSession.rule(ClientRule.CAPTURED_INVENTORY_FLIGHT)&&flying;
        float centerX=left+(creative?(bounded?89:88):(bounded?50.5F:51));
        float centerY=top+(creative?(bounded?27.5F:45):(bounded?43:75));
        float ax=(float)Math.atan((centerX-mouse[0])/40F)*20;
        float ay=(float)Math.atan((centerY-(bounded?0:creative?30:50)-mouse[1])/40F)*20;
        int size=creative?20:30;
        GlStateManager.pushMatrix();GlStateManager.loadIdentity();
        try {
            GlStateManager.translate(centerX,centerY,50);
            GlStateManager.scale(bounded?size:-size,size,bounded?-size:size);
            if(bounded)GlStateManager.translate(0,p.height/2+.0625F,0);
            GlStateManager.rotate(180,0,0,1);GlStateManager.rotate(bounded?ay:-ay,1,0,0);
            GlStateManager.rotate(bounded?-ax:180-ax,0,1,0);
            if(flying) {
                float ticks=ServerElytraVisuals.ticks(p)+1;
                GlStateManager.rotate(Math.min(1,ticks*ticks/100)*(-90+(capture?0:ay)),1,0,0);
                float yaw=capture?p.rotationYawHead:(bounded?180:0)+ax*2;
                float pitch=capture?p.rotationPitch:-ay;
                float cos=MathHelper.cos(-pitch*.017453292F),lookX=MathHelper.sin(-yaw*.017453292F-(float)Math.PI)*-cos,lookZ=MathHelper.cos(-yaw*.017453292F-(float)Math.PI)*-cos;
                double speed=p.motionX*p.motionX+p.motionZ*p.motionZ,look=lookX*lookX+lookZ*lookZ;
                boolean limited=bounded&&ServerSession.rule(ClientRule.LIMITED_INVENTORY_FLIGHT_YAW);
                if(speed>(limited?1.0E-5F:0)&&look>(limited?1.0E-5F:0)) {
                    double dot=(p.motionX*lookX+p.motionZ*lookZ)/Math.sqrt(speed*look);
                    double cross=p.motionX*lookZ-p.motionZ*lookX;
                    GlStateManager.rotate((float)(Math.signum(cross)*Math.acos(limited?Math.min(1,Math.abs(dot)):Math.max(-1,Math.min(1,dot)))*180/Math.PI),0,1,0);
                }
            }
            return matrix();
        }finally{GlStateManager.popMatrix();}
    }
    private static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    private static void setup(Framebuffer frame,int scale){GL11.glDisable(GL11.GL_SCISSOR_TEST);frame.framebufferClear();frame.bindFramebuffer(true);GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.loadIdentity();GlStateManager.ortho(0,960F/scale,720F/scale,0,-1000,1000);GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.loadIdentity();GlStateManager.color(1,1,1,1);GlStateManager.enableAlpha();GlStateManager.enableTexture2D();GlStateManager.disableLighting();}
    private static ByteBuffer pixels(){ByteBuffer b=BufferUtils.createByteBuffer(960*720*4);GL11.glReadPixels(0,0,960,720,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,b);return b;}
    private static float[] matrix(){FloatBuffer b=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,b);float[] m=new float[16];b.get(m);return m;}
    private static final class Recorder extends RenderPlayer {
        boolean skip,clipped;float[] matrix;
        Recorder(RenderManager manager){super(manager,false);}
        @Override public void doRender(AbstractClientPlayer player,double x,double y,double z,float yaw,float partial){if(!skip)super.doRender(player,x,y,z,yaw,partial);}
        @Override protected void rotateCorpse(AbstractClientPlayer player,float age,float yaw,float partial){super.rotateCorpse(player,age,yaw,partial);matrix=InventoryFlightSmokeTest.matrix();clipped=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);}
    }
    private InventoryFlightSmokeTest(){ }
}
