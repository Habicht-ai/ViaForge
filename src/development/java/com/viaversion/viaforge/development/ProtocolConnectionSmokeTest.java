package com.viaversion.viaforge.development;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.viaversion.viaforge.common.ViaForgeCommon;
import com.viaversion.viaforge.common.extended.ExtendedNetworkManager;
import com.viaversion.viaforge.common.extended.ExtendedServerData;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.network.OldServerPinger;
import net.minecraft.util.Session;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Opt-in real menu / ping / login regression; never packaged in release. */
public final class ProtocolConnectionSmokeTest {
    private final Path report;
    private final int port;
    private final OldServerPinger pinger = new OldServerPinger();
    private final ExecutorService workers = Executors.newFixedThreadPool(8);
    private final List<Future<?>> pings = new ArrayList<>();
    private final List<ServerData> pingRows = new ArrayList<>();
    // global, override (-1 = inherit), host-only constructor
    private final int[][] cases = {{340,-1,0},{393,-1,0},{393,340,0},{340,393,0},{340,763,0},{393,777,0},{340,-1,1}};
    private final JsonArray results = new JsonArray();
    private ProtocolVersion original, expected;
    private boolean oldPause, finished;
    private int index, stage, ticks;
    private long started = System.currentTimeMillis();
    private String name;

    private ProtocolConnectionSmokeTest(String path) {
        report=Paths.get(path);port=Integer.parseInt(System.getenv("VIAFORGE_PROTOCOL_PORT"));
    }
    public static boolean installIfRequested() {
        String path=System.getenv("VIAFORGE_PROTOCOL_PROBE");if(path==null)return false;
        FMLCommonHandler.instance().bus().register(new ProtocolConnectionSmokeTest(path));return true;
    }
    private void write(String state, String detail)throws Exception {
        JsonObject out=new JsonObject();out.addProperty("state",state);out.addProperty("detail",detail);
        out.addProperty("case",index);out.addProperty("name",name);out.add("joins",results);
        out.addProperty("time",System.currentTimeMillis());
        Files.createDirectories(report.toAbsolutePath().getParent());
        Path tmp=report.resolveSibling(report.getFileName()+".tmp");
        Files.write(tmp,Collections.singleton(out.toString()),StandardCharsets.UTF_8);
        Files.move(tmp,report,StandardCopyOption.REPLACE_EXISTING);
    }
    private void chooseGlobal(Minecraft mc, ProtocolVersion target)throws Exception {
        // Use the actual global screen's deferred save path.
        GuiProtocolSelectorAccess.choose(mc,target);
        require(ViaForgeCommon.getManager().getTargetVersion()==target,"Menu commits selected global protocol");
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(finished||event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();
        try {
            require(System.currentTimeMillis()-started<240000,"Connection regression timed out, case="+index+", stage="+stage);
            if(original==null) {
                require(mc.theWorld==null,"Probe must start in an unused client");
                original=ViaForgeCommon.getManager().getTargetVersion();oldPause=mc.gameSettings.pauseOnLostFocus;
                mc.gameSettings.pauseOnLostFocus=false;ProtocolSelectorSmokeTest.verify();
                net.minecraftforge.fml.client.FMLClientHandler.instance().setupServerList();
            }
            pinger.pingPendingNetworks();
            if(stage==0) {
                if(++ticks<10)return;
                if(index==cases.length){finish(mc,"PASS","");return;}
                int[] row=cases[index];ticks=0;
                chooseGlobal(mc,ProtocolVersion.getProtocol(row[0]));
                expected=ProtocolVersion.getProtocol(row[1]<0?row[0]:row[1]);
                name="VFproto"+(System.currentTimeMillis()%1000000)+index;
                java.lang.reflect.Field session=Minecraft.class.getDeclaredField("session");session.setAccessible(true);
                session.set(mc,new Session(name,UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes(StandardCharsets.UTF_8)).toString(),"0","legacy"));
                ServerData server=new ServerData("Protocol selection regression","127.0.0.1:"+port,false);
                ((ExtendedServerData)server).viaForge$setVersion(row[1]<0?null:expected);
                if(row[2]==1) {
                    ServerData stale=new ServerData("Stale previous server","127.0.0.1:1",false);
                    ((ExtendedServerData)stale).viaForge$setVersion(ProtocolVersion.v1_20);mc.setServerData(stale);
                }
                startPings();
                GuiConnecting connecting=row[2]==1?new GuiConnecting(new GuiMainMenu(),mc,"127.0.0.1",port)
                        :new GuiConnecting(new GuiMainMenu(),mc,server);
                // Changes made after the request must not change its captured version.
                ((ExtendedServerData)server).viaForge$setVersion(ProtocolVersion.v1_8);
                ViaForgeCommon.getManager().setTargetVersion(ProtocolVersion.v1_8);
                mc.displayGuiScreen(connecting);stage=1;write("connecting","");return;
            }
            require(!(mc.currentScreen instanceof GuiDisconnected),"Unexpected disconnect: "+mc.currentScreen);
            require(ViaForgeCommon.getManager().getTargetVersion()==ProtocolVersion.v1_8,"Pings / joins / channel close must not overwrite global selection");
            if(stage==1&&mc.thePlayer!=null&&mc.thePlayer.ticksExisted>=40) {
                for(Future<?> ping:pings){if(!ping.isDone())return;ping.get();}
                for(ServerData ping:pingRows) {
                    require(ping.pingToServer!=-1,"Status ping failed: "+ping.serverMOTD);
                    if(ping.pingToServer<0)return;
                }
                int actual=((ExtendedNetworkManager)mc.getNetHandler().getNetworkManager()).viaForge$getTrackedVersion().getVersion();
                require(actual==expected.getVersion(),"Login uses captured selected protocol, expected="+expected+", actual="+actual);
                if(ViaForgeCommon.getManager().getConfig().isShowProtocolVersionInF3()) {
                    GuiOverlayDebug overlay=new GuiOverlayDebug(mc);
                    java.lang.reflect.Method debug=GuiOverlayDebug.class.getDeclaredMethod("getDebugInfoRight");debug.setAccessible(true);
                    require(debug.invoke(overlay).toString().contains("ViaForge: "+expected),"F3 reports connection protocol even with another global selection");
                }
                JsonObject result=new JsonObject();result.addProperty("name",name);result.addProperty("global_at_request",cases[index][0]);
                result.addProperty("override",cases[index][1]);result.addProperty("host_constructor",cases[index][2]==1);
                result.addProperty("selected",expected.getVersion());result.addProperty("connection",actual);
                result.addProperty("global_after_pings",ViaForgeCommon.getManager().getTargetVersion().getVersion());
                result.addProperty("completed_concurrent_pings",pings.size());result.addProperty("player_ticks",mc.thePlayer.ticksExisted);
                result.addProperty("creative",mc.thePlayer.capabilities.isCreativeMode);results.add(result);
                stage=2;ticks=0;write("joined","");
            } else if(stage==2&&++ticks>=35) {
                mc.theWorld.sendQuittingDisconnectingPacket();mc.loadWorld(null);mc.setServerData(null);mc.displayGuiScreen(new GuiMainMenu());
                pinger.clearPendingNetworks();index++;stage=0;ticks=0;
            }
        }catch(Throwable failure){failure.printStackTrace();try{finish(mc,"FAIL",failure.toString());}catch(Exception e){e.printStackTrace();mc.shutdown();}}
    }
    private void startPings() {
        pings.clear();pingRows.clear();
        for(int i=0;i<24;i++) {
            ServerData row=new ServerData("Concurrent ping "+i,"127.0.0.1:"+port,false);
            ((ExtendedServerData)row).viaForge$setVersion(ProtocolVersion.getProtocol(new int[]{340,393,763,777}[i%4]));
            pingRows.add(row);pings.add(workers.submit(()->{try{pinger.ping(row);}catch(Exception e){throw new RuntimeException(e);}}));
        }
    }
    private void finish(Minecraft mc,String state,String detail)throws Exception {
        finished=true;workers.shutdownNow();pinger.clearPendingNetworks();
        if(mc.theWorld!=null){mc.theWorld.sendQuittingDisconnectingPacket();mc.loadWorld(null);}
        if(original!=null){ViaForgeCommon.getManager().setTargetVersion(original);mc.gameSettings.pauseOnLostFocus=oldPause;}
        write(state,detail);mc.shutdown();
    }
    private static final class GuiProtocolSelectorAccess {
        static void choose(Minecraft mc,ProtocolVersion target)throws Exception {
            com.viaversion.viaforge.gui.GuiProtocolSelector screen=new com.viaversion.viaforge.gui.GuiProtocolSelector(new GuiMainMenu());
            mc.displayGuiScreen(screen);
            java.lang.reflect.Field f=screen.getClass().getDeclaredField("list");f.setAccessible(true);Object list=f.get(screen);
            java.lang.reflect.Method click=list.getClass().getDeclaredMethod("elementClicked",int.class,boolean.class,int.class,int.class);click.setAccessible(true);
            click.invoke(list,com.viaversion.viaforge.common.ProtocolSelection.versions().indexOf(target),false,100,100);
            mc.displayGuiScreen(new GuiMainMenu());
        }
    }
}
