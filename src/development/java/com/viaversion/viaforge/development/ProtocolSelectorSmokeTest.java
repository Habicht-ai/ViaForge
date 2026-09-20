package com.viaversion.viaforge.development;

import com.viaversion.viaforge.common.ProtocolSelection;
import com.viaversion.viaforge.common.ViaForgeCommon;
import com.viaversion.viaforge.gui.GuiProtocolSelector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.lang.reflect.*;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.util.ResourceLocation;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Actual GuiSlot callbacks and font rendering, including off-screen work. */
final class ProtocolSelectorSmokeTest {
    static void verify()throws Exception {
        Minecraft mc=Minecraft.getMinecraft();FontRenderer old=mc.fontRendererObj;
        final int[] strings={0};
        FontRenderer counted=new FontRenderer(mc.gameSettings,new ResourceLocation("textures/font/ascii.png"),mc.getTextureManager(),false) {
            @Override public int drawString(String text,float x,float y,int color,boolean shadow) {
                strings[0]++;return super.drawString(text,x,y,color,shadow);
            }
        };
        mc.fontRendererObj=counted;
        try {
            List<ProtocolVersion> versions=ProtocolSelection.versions();
            ProtocolVersion current=ViaForgeCommon.getManager().getTargetVersion();
            int index=versions.get(0)==current?1:0;
            for(boolean simple:new boolean[]{false,true}) {
                int[] callbacks={0};
                GuiProtocolSelector screen=new GuiProtocolSelector(null,simple,(version,parent)->{
                    require(version==versions.get(index),"Clicked row maps to correct filtered protocol");callbacks[0]++;
                });
                screen.setWorldAndResolution(mc,360,240);
                Object list=list(screen);
                Method click=list.getClass().getDeclaredMethod("elementClicked",int.class,boolean.class,int.class,int.class);click.setAccessible(true);
                click.invoke(list,index,false,100,100);click.invoke(list,index,false,100,100);
                require(callbacks[0]==1,"Native duplicate press callback completes selection only once, simple="+simple);
                Method draw=list.getClass().getDeclaredMethod("drawSlot",int.class,int.class,int.class,int.class,int.class,int.class);draw.setAccessible(true);
                strings[0]=0;
                for(int row=0;row<versions.size();row++)draw.invoke(list,row,0,60+row*11,11,100,100);
                require(strings[0]>0&&strings[0]<20,"Only visible protocol labels reach FontRenderer: "+strings[0]+" of "+versions.size());
                ((GuiSlot)list).scrollBy(123);
                int before=((GuiSlot)list).getAmountScrolled();
                screen.setWorldAndResolution(mc,370,250);
                require(((GuiSlot)list(screen)).getAmountScrolled()==before,"Resizing preserves protocol list scroll");
            }
            Constructor<GuiProtocolSelector> ctor=GuiProtocolSelector.class.getDeclaredConstructor(GuiScreen.class,boolean.class,GuiProtocolSelector.FinishedCallback.class,boolean.class);
            ctor.setAccessible(true);int[] writes={0};
            GuiProtocolSelector deferred=ctor.newInstance(null,false,(GuiProtocolSelector.FinishedCallback)(version,parent)->writes[0]++,true);
            deferred.setWorldAndResolution(mc,360,240);
            Object list=list(deferred);Method click=list.getClass().getDeclaredMethod("elementClicked",int.class,boolean.class,int.class,int.class);click.setAccessible(true);
            click.invoke(list,index,false,100,100);
            require(writes[0]==0&&ViaForgeCommon.getManager().getTargetVersion()==current,"Browsing selection never writes configuration on the drawing/input thread");
            deferred.onGuiClosed();require(writes[0]==1,"Selected global version is committed when leaving the menu");
        }finally{mc.fontRendererObj=old;}
    }
    private static Object list(GuiProtocolSelector screen)throws Exception {
        Field field=GuiProtocolSelector.class.getDeclaredField("list");field.setAccessible(true);return field.get(screen);
    }
    private ProtocolSelectorSmokeTest() { }
}
