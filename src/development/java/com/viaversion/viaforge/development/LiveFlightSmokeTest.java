package com.viaversion.viaforge.development;

import com.google.gson.JsonObject;
import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.common.extended.ExtendedServerData;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.*;
import net.minecraft.util.Session;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Opt-in actual loopback connection, driven by live_flight_probe.py. Only in the development JAR. */
public final class LiveFlightSmokeTest {
    private final Path state;
    private final int protocol, port;
    private final String name;
    private int stage, ticks, flightTicks;
    private long started=System.currentTimeMillis();
    private double startX, startY;
    private boolean boosted, finished, jump, attachedVisual, flightSound, handAnimation, inventoryFlight;
    private float forward;
    private boolean risingStart, groundConfirmed, standingAfterLanding;
    private int groundCycles, jumpPhase;
    private boolean oldPause;
    private int oldPerspective, oldParticles;
    private LiveFlightSmokeTest(String output) {
        state=Paths.get(output);protocol=Integer.parseInt(System.getenv("VIAFORGE_LIVE_PROTOCOL"));
        port=Integer.parseInt(System.getenv("VIAFORGE_LIVE_PORT"));
        name="VFfly"+protocol+(System.currentTimeMillis()%1000000);
    }
    public static boolean installIfRequested() {
        String output=System.getenv("VIAFORGE_LIVE_FLIGHT");if(output==null)return false;
        FMLCommonHandler.instance().bus().register(new LiveFlightSmokeTest(output));return true;
    }
    private void write(String status, String detail)throws Exception {
        JsonObject json=new JsonObject();json.addProperty("state",status);json.addProperty("name",name);
        json.addProperty("protocol",protocol);json.addProperty("boosted",boosted);json.addProperty("flight_ticks",flightTicks);
        json.addProperty("attached_rocket_visual",attachedVisual);json.addProperty("original_flight_sound_playing",flightSound);
        json.addProperty("rocket_hand_animation",handAnimation);
        json.addProperty("inventory_open_during_flight",inventoryFlight);
        json.addProperty("rising_elytra_start",risingStart);
        json.addProperty("sprint_glide_cycles",groundCycles);
        json.addProperty("ground_glide_server_confirmed",groundConfirmed);
        json.addProperty("standing_after_landing",standingAfterLanding);
        json.addProperty("detail",detail);json.addProperty("time",System.currentTimeMillis());
        EntityPlayerSP player=Minecraft.getMinecraft().thePlayer;
        if(player!=null){json.addProperty("x",player.posX);json.addProperty("y",player.posY);json.addProperty("z",player.posZ);json.addProperty("start_x",startX);json.addProperty("start_y",startY);}
        Files.createDirectories(state.toAbsolutePath().getParent());
        Path temporary=state.resolveSibling(state.getFileName()+".tmp");Files.write(temporary,Collections.singletonList(json.toString()),StandardCharsets.UTF_8);
        Files.move(temporary,state,StandardCopyOption.REPLACE_EXISTING);
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||finished)return;
        Minecraft mc=Minecraft.getMinecraft();
        try {
            require(System.currentTimeMillis()-started<150000,"Live flight timed out at stage "+stage);
            require(!(mc.currentScreen instanceof net.minecraft.client.gui.GuiDisconnected),"Server disconnected the live probe");
            if(stage==0) {
                require(mc.theWorld==null,"Live probe starts from an unused client");
                oldPause=mc.gameSettings.pauseOnLostFocus;oldPerspective=mc.gameSettings.thirdPersonView;oldParticles=mc.gameSettings.particleSetting;
                mc.gameSettings.pauseOnLostFocus=false;mc.gameSettings.particleSetting=0;
                java.lang.reflect.Field session=Minecraft.class.getDeclaredField("session");session.setAccessible(true);
                session.set(mc,new Session(name,UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes(StandardCharsets.UTF_8)).toString(),"0","legacy"));
                ServerData server=new ServerData("ViaForge flight probe","127.0.0.1:"+port,false);
                ((ExtendedServerData)server).viaForge$setVersion(ProtocolVersion.getProtocol(protocol));
                mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(),mc,server));stage=1;write("connecting","");return;
            }
            EntityPlayerSP p=mc.thePlayer;if(p==null)return;
            if(stage==1&&ServerBlockSession.getLoadedResourceVersion()!=null&&p.posY>170) {
                p.movementInput=new net.minecraft.util.MovementInput(){@Override public void updatePlayerMoveState(){this.jump=LiveFlightSmokeTest.this.jump;this.moveForward=forward;}};
                write("ready","original login and resources loaded");stage=2;
            }else if(stage==2&&ServerElytraFlight.equipped(p)&&!p.capabilities.allowFlying&&p.posY>200) {
                mc.displayGuiScreen(null);mc.setIngameFocus();p.rotationYaw=-90;p.rotationPitch=0;
                // Let the authoritative teleport and its motion reset settle before pressing jump.
                if(p.posY<219.5&&p.motionY<-.2&&!p.onGround){jump=true;stage=3;ticks=0;}
            }else if(stage==3) {
                if(++ticks==3)jump=false;
                if(ServerElytraFlight.flying(p)){stage=4;startX=p.posX;startY=p.posY;}
                require(ticks<60,"Server did not confirm falling-flight start");
            }else if(stage==4) {
                jump=false;
                mc.gameSettings.thirdPersonView=1;
                require(ServerElytraFlight.flying(p),"Server-confirmed flight stopped in free air");
                require(!p.capabilities.isFlying&&!p.capabilities.allowFlying,"No creative abilities during survival flight");
                require(p.height==.6F&&p.getEyeHeight()==.4F,"Live collision box/camera");
                flightTicks++;p.rotationPitch=flightTicks<25?-8:8;p.rotationYaw=flightTicks<40?-90:-75;
                if(flightTicks==10){
                    require(p.posY<startY,"Unpowered glide descends");
                    if(protocol>=316){
                        java.lang.reflect.Method use=Minecraft.class.getDeclaredMethod("rightClickMouse");use.setAccessible(true);use.invoke(mc);
                        java.lang.reflect.Field equip=net.minecraft.client.renderer.ItemRenderer.class.getDeclaredField("equippedProgress");equip.setAccessible(true);
                        require(equip.getFloat(mc.getItemRenderer())==0,"Live successful rocket use dips the actual hand renderer");
                        require(protocol<573||p.isSwingInProgress,"Live modern rocket use swings the player arm");handAnimation=true;
                    }
                }
                boosted|=ServerEntityViews.boosts(p.getEntityId())>0;
                if(flightTicks==25)mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiInventory(p));
                if(flightTicks>25&&flightTicks<32) {
                    require(mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiInventory,"Inventory remains open during live flight");
                    require(!InventoryEntityPreview.captured(p),"Inventory render context is restored before next game tick");inventoryFlight=true;
                }
                if(flightTicks==30)net.minecraft.util.ScreenShotHelper.saveScreenshot(state.toAbsolutePath().getParent().toFile(),"live-inventory-flight-"+protocol+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                if(flightTicks==32){mc.displayGuiScreen(null);mc.setIngameFocus();}
                for(Object value:mc.theWorld.loadedEntityList) {
                    if(!(value instanceof net.minecraft.entity.item.EntityFireworkRocket))continue;
                    net.minecraft.entity.item.EntityFireworkRocket rocket=(net.minecraft.entity.item.EntityFireworkRocket)value;
                    if(!ServerFireworks.attached(rocket)||rocket.ticksExisted<2)continue;
                    require(!rocket.isInRangeToRenderDist(1),"Actual attached rocket is hidden by native renderer predicate");
                    require(rocket.getDistanceToEntity(p)<3,"Actual rocket trail follows player instead of ascending independently");
                    attachedVisual=true;
                }
                if(flightTicks>=45) {
                    java.lang.reflect.Field current=ServerElytraSound.class.getDeclaredField("current");current.setAccessible(true);
                    ServerElytraSound loop=(ServerElytraSound)current.get(null);
                    require(loop!=null&&!loop.isDonePlaying()&&loop.getVolume()>0,"Original flight loop follows active glide");
                    flightSound|=mc.getSoundHandler().isSoundPlaying(loop);
                    net.minecraft.client.model.ModelBiped model=new net.minecraft.client.model.ModelBiped();
                    model.setRotationAngles(0,1,0,0,p.rotationPitch,.0625F,p);
                    require(Math.abs(model.bipedHead.rotateAngleX+(float)Math.PI/4)<1e-6,"Actual player uses original gliding head pose");
                }
                if(flightTicks==50)net.minecraft.util.ScreenShotHelper.saveScreenshot(state.toAbsolutePath().getParent().toFile(),"live-flight-"+protocol+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                if(flightTicks==70){require(p.posX>startX+10,"Flight advances with steering");require(protocol<316||boosted&&attachedVisual,"Server rocket attached, powered flight and followed player invisibly");require(flightSound&&inventoryFlight,"Sound plays and flight survives opening and closing inventory");write("flying","movement, inventory flight preview, original sound and attached rocket presentation observed");stage=5;}
            }else if(stage==5&&!ServerElytraFlight.flying(p)&&p.isInWater()) {
                require(!p.capabilities.isFlying&&p.height==1.8F,"Water landing restores standing pose without creative flight");
                write("water_landed","water correctly ends flight; ready for sprint/glide cycles on existing navigation floor");stage=6;ticks=0;
            }else if(stage==6&&p.posY<70&&p.onGround&&!p.isInWater()) {
                forward=1;p.rotationYaw=p.rotationYawHead=-90;p.rotationPitch=0;p.setSprinting(true);
                stage=7;ticks=0;jumpPhase=0;startX=p.posX;
            }else if(stage==7) {
                ticks++;require(ticks<160,"Sprint/glide cycles timed out");
                require(!p.capabilities.isFlying&&!p.capabilities.allowFlying,"Ground glide does not enable creative flight");
                p.rotationYaw=-90;p.rotationPitch=0;
                ServerEntityViews.View view=ServerEntityViews.get(p.getEntityId());
                groundConfirmed|=view!=null&&view.fallFlying;
                if(jumpPhase==0&&ticks>=5&&p.onGround) {p.setSprinting(true);jump=true;jumpPhase=1;}
                else if(jumpPhase==1) {jump=false;jumpPhase=2;}
                else if(jumpPhase==2) {jump=true;jumpPhase=3;}
                else if(jumpPhase==3) {
                    jump=false;
                    if(protocol>=573) {
                        require(ServerElytraFlight.flying(p)&&p.motionY>0,"Actual rising sprint jump starts Elytra immediately");
                        risingStart=true;jumpPhase=5;
                    }else {
                        require(!ServerElytraFlight.flying(p),"Old version does not start during ascent");jumpPhase=4;
                    }
                }else if(jumpPhase==4&&p.motionY<0) {jump=true;jumpPhase=5;}
                else if(jumpPhase==5) {
                    jump=false;
                    if(p.onGround) {groundCycles++;jumpPhase=0;}
                }
                if(groundCycles>=3) {
                    require(groundConfirmed,"Server accepted ground-level gliding commands");
                    require(p.posX>startX+5,"Repeated unpowered sprint/glide cycles actually travel");
                    forward=0;jump=false;
                    stage=8;ticks=0;
                }
            }else if(stage==8) {
                ticks++;
                require(ticks<80,"Final landing never settled after the server response");
                if(ticks>=20&&p.onGround&&!ServerElytraFlight.flying(p)) {
                    require(p.height==1.8F&&!ServerElytraFlight.crawling(p),"Final landing restores standing pose without false crawl");
                    require(!p.capabilities.isFlying&&!p.capabilities.allowFlying,"No persistent flight abilities after final landing");
                    standingAfterLanding=true;
                    write("PASS","actual Forge login, free flight/rocket, water landing, three unpowered sprint/glide cycles and confirmed final standing landing");
                    finished=true;mc.theWorld.sendQuittingDisconnectingPacket();mc.loadWorld(null);mc.displayGuiScreen(new GuiMainMenu());restoreSettings(mc);mc.shutdown();
                }
            }
        }catch(Throwable failure){try{write("FAIL",failure.toString());}catch(Exception ignored){}failure.printStackTrace();finished=true;restoreSettings(mc);mc.shutdown();}
    }
    private void restoreSettings(Minecraft mc) {mc.gameSettings.pauseOnLostFocus=oldPause;mc.gameSettings.thirdPersonView=oldPerspective;mc.gameSettings.particleSetting=oldParticles;}
}
