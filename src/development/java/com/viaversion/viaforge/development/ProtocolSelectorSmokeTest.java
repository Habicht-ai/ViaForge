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

            ProtocolVersion override=ProtocolVersion.v1_12_2;
            final ProtocolVersion[] chosen={override};int[] serverCallbacks={0};
            GuiProtocolSelector server=new GuiProtocolSelector(null,override,(version,parent)->{chosen[0]=version;serverCallbacks[0]++;});
            server.setWorldAndResolution(mc,360,240);
            Method selected=list(server).getClass().getDeclaredMethod("isSelected",int.class);selected.setAccessible(true);
            require((Boolean)selected.invoke(list(server),versions.indexOf(override)),"Per-server menu highlights its saved override");
            Method action=GuiProtocolSelector.class.getDeclaredMethod("actionPerformed",GuiButton.class);action.setAccessible(true);
            action.invoke(server,new GuiButton(4,0,0,"Use global"));
            require(serverCallbacks[0]==1&&chosen[0]==null,"Use global removes the override, without pinning today's global version");
            require(!(Boolean)selected.invoke(list(server),versions.indexOf(override)),"Inherited mode has no pinned row");
            require(ViaForgeCommon.getManager().getTargetVersion()==current,"Per-server edits leave global choice intact");

            net.minecraft.client.multiplayer.ServerData data=new net.minecraft.client.multiplayer.ServerData("probe","127.0.0.1",false);
            com.viaversion.viaforge.common.extended.ExtendedServerData extended=(com.viaversion.viaforge.common.extended.ExtendedServerData)data;
            extended.viaForge$setVersion(override);
            net.minecraft.client.multiplayer.ServerData restored=net.minecraft.client.multiplayer.ServerData.getServerDataFromNBTCompound(data.getNBTCompound());
            require(((com.viaversion.viaforge.common.extended.ExtendedServerData)restored).viaForge$getVersion()==override,"Server override survives NBT save/reload");
            extended.viaForge$setVersion(null);
            restored.copyFrom(data);
            require(((com.viaversion.viaforge.common.extended.ExtendedServerData)restored).viaForge$getVersion()==null,"Removing override survives server-data copy");
            require(!restored.getNBTCompound().hasKey("viaForge$version"),"Inherited mode is persisted without a stale override");
            queuedMouseInput(mc,versions);
        }finally{mc.fontRendererObj=old;}
    }
    private static Object list(GuiProtocolSelector screen)throws Exception {
        Field field=GuiProtocolSelector.class.getDeclaredField("list");field.setAccessible(true);return field.get(screen);
    }
    private static void queuedMouseInput(Minecraft mc,List<ProtocolVersion> versions)throws Exception {
        // Feed the real LWJGL event reader and native GuiSlot hit testing. No OS
        // mouse injection, cursor movement or interference with another client.
        java.util.Map<Field,Object> saved=new java.util.LinkedHashMap<>();
        for(String name:new String[]{"readBuffer","buttons","isGrabbed","eventButton","eventState","event_dx","event_dy",
                "event_dwheel","event_x","event_y","event_nanos","last_event_raw_x","last_event_raw_y"}) {
            Field f=org.lwjgl.input.Mouse.class.getDeclaredField(name);f.setAccessible(true);saved.put(f,f.get(null));
        }
        try {
            java.util.List<ProtocolVersion> clicks=new java.util.ArrayList<>();
            GuiProtocolSelector screen=new GuiProtocolSelector(null,true,(version,parent)->clicks.add(version));
            screen.setWorldAndResolution(mc,360,240);GuiSlot slot=(GuiSlot)list(screen);
            slot.scrollBy(-100000);slot.scrollBy(37);
            Field top=GuiSlot.class.getDeclaredField("top");top.setAccessible(true);
            Field height=GuiSlot.class.getDeclaredField("slotHeight");height.setAccessible(true);
            // Previous rendered frame deliberately points to another location.
            for(String field:new String[]{"mouseX","mouseY"}) {
                Field f=GuiSlot.class.getDeclaredField(field);f.setAccessible(true);f.setInt(slot,0);
            }
            java.nio.ByteBuffer events=java.nio.ByteBuffer.allocate(4*org.lwjgl.input.Mouse.EVENT_SIZE);
            long nanos=System.nanoTime();
            for(int index:new int[]{5,8}) {
                int y=top.getInt(slot)+4+index*height.getInt(slot)-slot.getAmountScrolled()+height.getInt(slot)/2;
                for(int state:new int[]{1,0}) {
                    events.put((byte)0).put((byte)state).putInt(180*mc.displayWidth/360)
                            .putInt((240-y-1)*mc.displayHeight/240).putInt(0).putLong(++nanos);
                }
            }
            events.flip();
            for(Field f:saved.keySet()) {
                if(f.getName().equals("readBuffer"))f.set(null,events);
                if(f.getName().equals("buttons"))f.set(null,java.nio.ByteBuffer.allocate(16));
                if(f.getName().equals("isGrabbed"))f.setBoolean(null,false);
            }
            while(org.lwjgl.input.Mouse.next())screen.handleMouseInput();
            require(clicks.equals(java.util.Arrays.asList(versions.get(5),versions.get(8))),
                    "Queued clicks use their own coordinates and scrolled row, exactly once per press: "+clicks);
        }finally{for(java.util.Map.Entry<Field,Object> entry:saved.entrySet())entry.getKey().set(null,entry.getValue());}
    }
    private ProtocolSelectorSmokeTest() { }
}
