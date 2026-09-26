package com.viaversion.viaforge.development;

import com.google.gson.*;
import com.viaversion.viaforge.common.extended.ExtendedServerData;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.mixin.impl.connect.ClientTerrainReady;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.lang.reflect.*;
import io.netty.channel.*;
import net.minecraft.network.play.server.*;
import net.minecraft.network.play.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** UI/key driver and passive observation only. No replacement packet handlers or physics. */
public final class SessionVisualProbe {
    private final Path folder;
    private long command = -1;
    private int tick;
    private String photo;
    private final Queue<String> pending = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private Channel channel;
    private SessionVisualProbe(String path) { folder=Paths.get(path); }
    public static boolean installIfRequested() {
        String path=System.getenv("VIAFORGE_SESSION_PROBE");
        if(path==null)return false;
        SessionVisualProbe probe=new SessionVisualProbe(path);
        FMLCommonHandler.instance().bus().register(probe);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(probe);return true;
    }
    private void record(JsonObject row) {
        row.addProperty("time_ms",System.currentTimeMillis());row.addProperty("tick",tick);pending.add(row.toString());
    }
    private void packet(Object packet,String direction) {
        String name=packet.getClass().getSimpleName();
        if(!(packet instanceof S01PacketJoinGame)&&!(packet instanceof S07PacketRespawn)&&!(packet instanceof S08PacketPlayerPosLook)
                &&!(packet instanceof S48PacketResourcePackSend)&&!(packet instanceof C19PacketResourcePackStatus))return;
        JsonObject row=new JsonObject();row.addProperty("packet",name);row.addProperty("direction",direction);
        row.addProperty("thread",Thread.currentThread().getName());
        for(Field f:packet.getClass().getDeclaredFields())try {
            if(Modifier.isStatic(f.getModifiers()))continue;f.setAccessible(true);
            row.addProperty(f.getName(),String.valueOf(f.get(packet)));
        }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        record(row);
    }
    @SubscribeEvent public void player(net.minecraftforge.client.event.RenderPlayerEvent.Post event) {
        if(tick%10!=0)return;
        net.minecraft.client.model.ModelBiped model=event.renderer.getMainModel();
        JsonObject row=new JsonObject();row.addProperty("render_player",event.entityPlayer.getName());
        row.addProperty("left_pose",model.heldItemLeft);row.addProperty("right_pose",model.heldItemRight);
        row.addProperty("left_x",model.bipedLeftArm.rotateAngleX);row.addProperty("left_y",model.bipedLeftArm.rotateAngleY);
        row.addProperty("right_x",model.bipedRightArm.rotateAngleX);row.addProperty("right_y",model.bipedRightArm.rotateAngleY);record(row);
    }
    @SubscribeEvent public void render(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||photo==null)return;
        Minecraft mc=Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(folder.toFile(),photo+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
        JsonObject row=new JsonObject();row.addProperty("photo",photo);record(row);photo=null;
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();tick++;
        try {
            Files.createDirectories(folder);
            if(mc.getNetHandler()!=null && channel!=mc.getNetHandler().getNetworkManager().channel()) {
                channel=mc.getNetHandler().getNetworkManager().channel();
                channel.pipeline().addBefore("packet_handler","session_probe",new ChannelDuplexHandler(){
                    @Override public void channelRead(ChannelHandlerContext ctx,Object msg)throws Exception {packet(msg,"receive");super.channelRead(ctx,msg);}
                    @Override public void write(ChannelHandlerContext ctx,Object msg,ChannelPromise promise)throws Exception {packet(msg,"send");super.write(ctx,msg,promise);}
                });
            }
            Path input=folder.resolve("command.json");
            if(Files.exists(input)) {
                JsonObject request=new JsonParser().parse(new String(Files.readAllBytes(input),StandardCharsets.UTF_8)).getAsJsonObject();
                if(request.get("id").getAsLong()!=command) {
                    command=request.get("id").getAsLong();execute(mc,request);
                    JsonObject ack=new JsonObject();ack.addProperty("command",command);ack.add("request",request);record(ack);
                }
            }
            JsonObject state=new JsonObject();state.addProperty("command",command);
            state.addProperty("screen",mc.currentScreen==null?"none":mc.currentScreen.getClass().getSimpleName());
            state.addProperty("profile",ServerSession.profile().serverProtocol());
            state.addProperty("resources",ServerSession.getLoadedResourceVersion());
            state.addProperty("resource_generation",ServerSession.resourceGeneration());
            state.addProperty("resource_failure",String.valueOf(ServerSession.resourceFailure()));
            state.addProperty("game_dir",mc.mcDataDir.getCanonicalPath());
            state.addProperty("server_pack",String.valueOf(mc.getResourcePackRepository().getResourcePackInstance()));
            if(mc.thePlayer!=null) {
                state.addProperty("player",mc.thePlayer.getName());state.addProperty("entity",mc.thePlayer.getEntityId());
                state.addProperty("world",System.identityHashCode(mc.theWorld));state.addProperty("dimension",mc.thePlayer.dimension);
                state.addProperty("terrain_ready",((ClientTerrainReady)mc.getNetHandler()).viaForge$terrainReady());
                state.addProperty("awaiting_world",ServerSession.awaitingWorld(mc.getNetHandler()));
                state.addProperty("loaded",mc.theWorld.isBlockLoaded(new BlockPos(mc.thePlayer.posX,64,mc.thePlayer.posZ)));
                state.addProperty("x",mc.thePlayer.posX);state.addProperty("y",mc.thePlayer.posY);state.addProperty("z",mc.thePlayer.posZ);
                state.addProperty("using",mc.thePlayer.isUsingItem());state.addProperty("use_ticks",mc.thePlayer.getItemInUseCount());
                state.addProperty("hand",com.viaversion.viaforge.hands.Offhand.useHand);
                state.addProperty("held",String.valueOf(mc.thePlayer.getHeldItem()));
                state.addProperty("held_nbt",mc.thePlayer.getHeldItem()==null?"":String.valueOf(mc.thePlayer.getHeldItem().getTagCompound()));
                state.addProperty("use_action",mc.thePlayer.getHeldItem()==null?"":String.valueOf(mc.thePlayer.getHeldItem().getItemUseAction()));
                state.addProperty("brand",mc.thePlayer.getClientBrand());
                state.addProperty("mode",mc.playerController.getCurrentGameType().toString());
                state.addProperty("bubble",com.viaversion.viaforge.compatibility.ServerSwimming.fluids.get((int)Math.floor(mc.thePlayer.posX),(int)Math.floor(mc.thePlayer.posY),(int)Math.floor(mc.thePlayer.posZ)));
                state.addProperty("camera",mc.gameSettings.thirdPersonView);state.addProperty("particles",mc.gameSettings.particleSetting);
                if(tick%10==0) {
                    Field layers=mc.effectRenderer.getClass().getDeclaredField("fxLayers");layers.setAccessible(true);
                    JsonArray samples=new JsonArray();int bubbles=0,pops=0;
                    for(Object layer:(Object[])layers.get(mc.effectRenderer))for(Object list:(Object[])layer)for(Object particle:(Iterable<?>)list) {
                        String kind=particle.getClass().getSimpleName();
                        if(!kind.equals("ServerBubbleParticle")&&!kind.equals("ServerBubblePopParticle"))continue;
                        boolean pop=kind.equals("ServerBubblePopParticle");
                        net.minecraft.client.particle.EntityFX fx=(net.minecraft.client.particle.EntityFX)particle;if(pop)pops++;else bubbles++;
                        if(samples.size()>=40)continue;
                        JsonObject sample=new JsonObject();sample.addProperty("id",System.identityHashCode(fx));
                        sample.addProperty("kind",kind);
                        if(!pop)sample.addProperty("downward",particle.getClass().getField("downward").getBoolean(particle));
                        sample.addProperty("x",fx.posX);sample.addProperty("y",fx.posY);sample.addProperty("z",fx.posZ);
                        sample.addProperty("light",fx.getBrightnessForRender(0));
                        if(pop){Field frame=particle.getClass().getDeclaredField("frame");frame.setAccessible(true);sample.addProperty("frame",frame.getInt(particle));}
                        sample.addProperty("vx",fx.motionX);sample.addProperty("vy",fx.motionY);sample.addProperty("vz",fx.motionZ);samples.add(sample);
                    }
                    JsonObject row=new JsonObject();row.addProperty("particle_count",bubbles);row.addProperty("pop_count",pops);row.add("samples",samples);record(row);
                }
            }
            record(state);
            // A short-lived Windows reader can deny replacement. JSONL remains
            // authoritative; the reader tolerates an incomplete latest snapshot.
            try { Files.write(folder.resolve("state.json"),Collections.singleton(state.toString()),StandardCharsets.UTF_8); }
            catch (FileSystemException busySnapshot) { /* Advisory snapshot; keep the authoritative tick stream. */ }
            List<String> batch=new ArrayList<>();String line;while((line=pending.poll())!=null)batch.add(line);
            Files.write(folder.resolve("observations.jsonl"),batch,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        } catch(Throwable failure) {
            failure.printStackTrace();
            try {Files.write(folder.resolve("error.txt"),Collections.singleton(failure.toString()),StandardCharsets.UTF_8);}catch(Exception ignored){}
            mc.shutdown();
        }
    }
    private void execute(Minecraft mc,JsonObject r)throws Exception {
        String op=r.get("op").getAsString();
        if(op.equals("connect")) {
            if(mc.theWorld!=null)throw new IllegalStateException("Disconnect before reconnect");
            String name=r.get("name").getAsString();Field session=Minecraft.class.getDeclaredField("session");session.setAccessible(true);
            session.set(mc,new Session(name,UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes(StandardCharsets.UTF_8)).toString(),"0","legacy"));
            mc.gameSettings.pauseOnLostFocus=false;mc.gameSettings.limitFramerate=60;
            ServerData server=new ServerData("Isolated session regression","127.0.0.1:"+r.get("port").getAsInt(),false);
            if(r.has("saved")) {
                ServerList list=new ServerList(mc);ServerData stored=null;
                for(int i=0;i<list.countServers();i++)if(list.getServerData(i).serverIP.equals(server.serverIP))stored=list.getServerData(i);
                if(stored==null)throw new IllegalStateException("Missing isolated saved server");server=stored;
            }
            if(r.has("pack_mode"))server.setResourceMode(ServerData.ServerResourceMode.valueOf(r.get("pack_mode").getAsString()));
            ((ExtendedServerData)server).viaForge$setVersion(ProtocolVersion.getProtocol(r.get("protocol").getAsInt()));
            mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(),mc,server));
        } else if(op.equals("disconnect")) {
            if(mc.theWorld!=null)mc.theWorld.sendQuittingDisconnectingPacket();mc.loadWorld(null);mc.displayGuiScreen(new GuiMainMenu());
        } else if(op.equals("chat")) mc.thePlayer.sendChatMessage(r.get("text").getAsString());
        else if(op.equals("button")) {
            Field buttons=GuiScreen.class.getDeclaredField("buttonList");buttons.setAccessible(true);
            int id=r.get("button").getAsInt();GuiButton selected=null;
            for(Object b:(List<?>)buttons.get(mc.currentScreen))if(((GuiButton)b).id==id)selected=(GuiButton)b;
            if(selected==null||!selected.enabled)throw new IllegalStateException("Button unavailable on "+mc.currentScreen);
            Method click=GuiScreen.class.getDeclaredMethod("mouseClicked",int.class,int.class,int.class);click.setAccessible(true);
            click.invoke(mc.currentScreen,selected.xPosition+selected.width/2,selected.yPosition+selected.height/2,0);
        } else if(op.equals("input")) {
            String keys=r.get("keys").getAsString();
            for(KeyBinding key:new KeyBinding[]{mc.gameSettings.keyBindUseItem,mc.gameSettings.keyBindAttack,mc.gameSettings.keyBindForward,mc.gameSettings.keyBindJump,mc.gameSettings.keyBindSneak,mc.gameSettings.keyBindSprint}) {
                String name=key==mc.gameSettings.keyBindUseItem?"use":key==mc.gameSettings.keyBindAttack?"attack":key==mc.gameSettings.keyBindForward?"forward":key==mc.gameSettings.keyBindJump?"jump":key==mc.gameSettings.keyBindSneak?"sneak":"sprint";
                boolean down=keys.contains(name);if(down&&!key.isKeyDown())KeyBinding.onTick(key.getKeyCode());KeyBinding.setKeyBindState(key.getKeyCode(),down);
            }
        } else if(op.equals("view")) { mc.gameSettings.thirdPersonView=r.get("camera").getAsInt();if(r.has("particles"))mc.gameSettings.particleSetting=r.get("particles").getAsInt(); }
        else if(op.equals("look"))mc.thePlayer.setAngles(r.get("yaw").getAsFloat()/.15F,-r.get("pitch").getAsFloat()/.15F);
        else if(op.equals("slot"))KeyBinding.onTick(mc.gameSettings.keyBindsHotbar[r.get("slot").getAsInt()].getKeyCode());
        else if(op.equals("model")) {
            net.minecraft.client.resources.model.IBakedModel model=mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(net.minecraft.init.Blocks.stone.getDefaultState());
            java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads=new ArrayList<>(model.getGeneralQuads());
            for(EnumFacing side:EnumFacing.values())quads.addAll(model.getFaceQuads(side));
            float minY=Float.POSITIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY;
            for(net.minecraft.client.renderer.block.model.BakedQuad quad:quads) {
                int[] data=quad.getVertexData();int stride=data.length/4;
                for(int vertex=0;vertex<4;vertex++){float y=Float.intBitsToFloat(data[vertex*stride+1]);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}
            }
            JsonObject result=new JsonObject();result.addProperty("model","stone");result.addProperty("quads",quads.size());result.addProperty("min_y",minY);result.addProperty("max_y",maxY);record(result);
        }
        else if(op.equals("resource")) {
            ResourceLocation location=new ResourceLocation(r.get("path").getAsString());
            JsonObject result=new JsonObject();result.addProperty("resource",location.toString());
            try(java.io.InputStream stream=mc.getResourceManager().getResource(location).getInputStream()) {
                java.awt.image.BufferedImage texture=javax.imageio.ImageIO.read(stream);result.addProperty("argb",texture.getRGB(0,0));
                result.addProperty("width",texture.getWidth());result.addProperty("height",texture.getHeight());
                if(r.has("snapshot"))javax.imageio.ImageIO.write(texture,"png",folder.resolve("resource-snapshot.png").toFile());
            }record(result);
        }
        else if(op.equals("photo"))photo=r.get("name").getAsString();
        else if(op.equals("stop"))mc.shutdown();
        else throw new IllegalArgumentException(op);
    }
}
