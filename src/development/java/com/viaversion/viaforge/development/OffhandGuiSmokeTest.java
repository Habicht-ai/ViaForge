package com.viaversion.viaforge.development;

import com.viaversion.viaforge.hands.Offhand;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings.GameType;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

final class OffhandGuiSmokeTest {
    static void verify(BlockVersionProfile profile)throws Exception{
        Minecraft mc=Minecraft.getMinecraft();int width=mc.displayWidth,height=mc.displayHeight,scale=mc.gameSettings.guiScale;
        GuiScreen old=mc.currentScreen;ItemStack shield=Offhand.get();
        Framebuffer frame=new Framebuffer(800,480,true);mc.displayWidth=800;mc.displayHeight=480;mc.gameSettings.guiScale=2;
        GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.pushMatrix();GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.pushMatrix();
        mc.getRenderManager().cacheActiveRenderInfo(mc.theWorld,mc.fontRendererObj,mc.thePlayer,null,mc.gameSettings,0);
        try{
            GuiInventory survival=new GuiInventory(mc.thePlayer);mc.currentScreen=survival;survival.setWorldAndResolution(mc,400,240);
            require(survival.inventorySlots.getSlot(0).xDisplayPosition==154&&survival.inventorySlots.getSlot(0).yDisplayPosition==28&&survival.inventorySlots.getSlot(1).xDisplayPosition==98&&survival.inventorySlots.getSlot(1).yDisplayPosition==18,"Crafting hitboxes follow target inventory sheet");
            render(frame);survival.drawScreen(-1,-1,0);save(frame,profile,"survival");
            Offhand.set(null);render(frame);mc.ingameGUI.renderGameOverlay(0);ByteBuffer empty=pixels();
            Offhand.set(shield);render(frame);mc.ingameGUI.renderGameOverlay(0);ByteBuffer filled=pixels();
            int changed=0;for(int y=2;y<44;y++)for(int x=160;x<216;x++){int p=(y*800+x)*4;if(empty.getInt(p)!=filled.getInt(p))changed++;}
            require(changed>100,"Forge hotbar draws the offhand slot and item at the original position");save(frame,profile,"hotbar");
            mc.playerController.setGameType(GameType.CREATIVE);
            GuiContainerCreative creative=new GuiContainerCreative(mc.thePlayer);mc.currentScreen=creative;creative.setWorldAndResolution(mc,400,240);
            Method tab=GuiContainerCreative.class.getDeclaredMethod("setCurrentCreativeTab",CreativeTabs.class);tab.setAccessible(true);tab.invoke(creative,CreativeTabs.tabInventory);
            Slot off=creative.inventorySlots.getSlot(45);require(off.xDisplayPosition==35&&off.yDisplayPosition==20&&off.getStack()==shield,"Creative inventory wraps offhand slot at target position");
            require(creative.inventorySlots.getSlot(5).xDisplayPosition==54&&creative.inventorySlots.getSlot(7).xDisplayPosition==108,"Target armor columns leave room for offhand and avatar");
            Method click=GuiContainerCreative.class.getDeclaredMethod("handleMouseClick",Slot.class,int.class,int.class,int.class);click.setAccessible(true);
            click.invoke(creative,off,45,0,0);require(Offhand.get()==null&&mc.thePlayer.inventory.getItemStack()==shield,"Creative click removes offhand to cursor");
            click.invoke(creative,off,45,0,0);require(Offhand.get()!=null&&Offhand.get().getItem()==shield.getItem()&&Offhand.get().stackSize==1&&mc.thePlayer.inventory.getItemStack()==null,"Creative click restores offhand");shield=Offhand.get();
            render(frame);creative.drawScreen(-1,-1,0);save(frame,profile,"creative");creative.onGuiClosed();
            require(GL11.glGetError()==0,"Survival/creative/hotbar draw with valid OpenGL state");
        }finally{
            mc.currentScreen=old;mc.playerController.setGameType(GameType.SURVIVAL);Offhand.set(shield);
            mc.displayWidth=width;mc.displayHeight=height;mc.gameSettings.guiScale=scale;
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.popMatrix();GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.popMatrix();GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            frame.deleteFramebuffer();mc.getFramebuffer().bindFramebuffer(true);
        }
    }
    private static void render(Framebuffer frame){frame.framebufferClear();frame.bindFramebuffer(true);GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.loadIdentity();GlStateManager.ortho(0,400,240,0,-1000,1000);GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.loadIdentity();GlStateManager.color(1,1,1,1);GlStateManager.enableAlpha();GlStateManager.enableTexture2D();GlStateManager.disableLighting();}
    private static ByteBuffer pixels(){ByteBuffer pixels=BufferUtils.createByteBuffer(800*480*4);GL11.glReadPixels(0,0,800,480,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);return pixels;}
    private static void save(Framebuffer frame,BlockVersionProfile profile,String kind){ScreenShotHelper.saveScreenshot(Paths.get(System.getenv("VIAFORGE_BLOCK_SMOKE_TEST")).toAbsolutePath().getParent().toFile(),"offhand-"+kind+"-"+profile.resourceVersion()+".png",800,480,frame);}
}
