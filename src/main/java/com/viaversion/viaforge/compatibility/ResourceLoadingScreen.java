package com.viaversion.viaforge.compatibility;

import java.io.IOException;
import net.minecraft.client.gui.*;
import net.minecraft.util.ChatComponentText;

/** Keep the world covered until its texture atlas is ready. Network ticks continue. */
public final class ResourceLoadingScreen extends GuiScreen {
    private final String version=ServerSession.profile().resources().version();
    @Override public void initGui() { buttonList.add(new GuiButton(0,width/2-100,height/2+35,net.minecraft.client.resources.I18n.format("gui.cancel"))); }
    @Override public void updateScreen() {
        if(!ServerSession.awaitingResources())mc.displayGuiScreen(null);
        else if(ServerSession.resourceFailure()!=null)disconnect("Server textures could not be loaded. Please reconnect to retry.");
    }
    @Override public void drawScreen(int mouseX,int mouseY,float ticks) {
        drawBackground(0);
        drawCenteredString(fontRendererObj,"Loading textures for Minecraft "+version+"...",width/2,height/2-10,0xFFFFFF);
        super.drawScreen(mouseX,mouseY,ticks);
    }
    @Override protected void actionPerformed(GuiButton button) { disconnect("Connection cancelled"); }
    @Override protected void keyTyped(char character,int key)throws IOException { if(key==1)disconnect("Connection cancelled"); }
    private void disconnect(String reason) {
        if(mc.getNetHandler()!=null)mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText(reason));
        mc.loadWorld(null);
        mc.displayGuiScreen(new GuiDisconnected(new GuiMultiplayer(new GuiMainMenu()),"connect.failed",new ChatComponentText(reason)));
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
