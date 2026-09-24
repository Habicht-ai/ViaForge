package com.viaversion.viaforge.development;

import com.google.gson.*;
import com.viaversion.viaforge.boats.ServerBoat;
import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.common.extended.ExtendedServerData;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.Session;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Input and read-only telemetry, packaged only in the development client. */
public final class LiveBoatProbe {
    private final Path folder;
    private final int protocol, port;
    private final String name;
    private final long started=System.currentTimeMillis();
    private int stage, tick, actionTick;
    private String action="idle";
    private boolean finished;
    private LiveBoatProbe(String path) {
        folder=Paths.get(path); protocol=Integer.parseInt(System.getenv("VIAFORGE_BOAT_PROTOCOL"));
        port=Integer.parseInt(System.getenv("VIAFORGE_BOAT_PORT"));
        name=("1".equals(System.getenv("VIAFORGE_PUSH_PROBE"))?"VFpush":"VFboat")+protocol+System.currentTimeMillis()%1000000;
    }
    public static boolean installIfRequested() {
        String path=System.getenv("VIAFORGE_BOAT_PROBE"); if(path==null)return false;
        LiveBoatProbe probe=new LiveBoatProbe(path);
        FMLCommonHandler.instance().bus().register(probe);
        if("1".equals(System.getenv("VIAFORGE_HOTBAR_PROBE")))net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(probe);
        return true;
    }
    @SubscribeEvent public void sound(net.minecraftforge.client.event.sound.PlaySoundEvent event) {
        if(finished||tick>=10000||event.sound==null)return;
        JsonObject j=new JsonObject();j.addProperty("phase","sound");j.addProperty("sound",event.sound.getSoundLocation().toString());
        j.addProperty("x",event.sound.getXPosF());j.addProperty("y",event.sound.getYPosF());j.addProperty("z",event.sound.getZPosF());
        j.addProperty("volume",event.sound.getVolume());j.addProperty("pitch",event.sound.getPitch());
        j.addProperty("category",event.category==null?"unknown":event.category.toString());
        j.addProperty("registered",Minecraft.getMinecraft().getSoundHandler().getSound(event.sound.getSoundLocation())!=null);
        try{log(j);}catch(Exception failure){throw new IllegalStateException(failure);}
    }
    @SubscribeEvent public void soundSource(net.minecraftforge.client.event.sound.PlaySoundSourceEvent event) {
        if(finished||tick>=10000||event.sound==null)return;
        JsonObject j=new JsonObject();j.addProperty("phase","sound_source");j.addProperty("sound",event.sound.getSoundLocation().toString());
        j.addProperty("x",event.sound.getXPosF());j.addProperty("y",event.sound.getYPosF());j.addProperty("z",event.sound.getZPosF());
        try{log(j);}catch(Exception failure){throw new IllegalStateException(failure);}
    }
    private synchronized void log(JsonObject data) throws Exception {
        data.addProperty("time_ms",System.currentTimeMillis());data.addProperty("tick",tick);
        Files.write(folder.resolve("client.jsonl"),Collections.singletonList(data.toString()),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    private void state(String status,String detail)throws Exception {
        JsonObject j=new JsonObject();j.addProperty("state",status);j.addProperty("name",name);j.addProperty("protocol",protocol);j.addProperty("detail",detail);
        Path tmp=folder.resolve("client-state.tmp");Files.write(tmp,Collections.singletonList(j.toString()),StandardCharsets.UTF_8);
        Files.move(tmp,folder.resolve("client-state.json"),StandardCopyOption.REPLACE_EXISTING);
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(finished)return; Minecraft mc=Minecraft.getMinecraft();
        try {
            Path requested=folder.resolve("action.txt");
            if(Files.exists(requested)&&new String(Files.readAllBytes(requested),StandardCharsets.UTF_8).trim().equals("stop")) {
                state("DONE","Requested probe shutdown");finished=true;mc.shutdown();return;
            }
            if(System.currentTimeMillis()-started>600000)throw new IllegalStateException("Probe timeout");
            if(mc.currentScreen instanceof GuiDisconnected) {
                String reason="";
                for(Field f:GuiDisconnected.class.getDeclaredFields()) {
                    f.setAccessible(true);Object v=f.get(mc.currentScreen);
                    if(v instanceof net.minecraft.util.IChatComponent)reason+=((net.minecraft.util.IChatComponent)v).getUnformattedText();
                }
                throw new IllegalStateException("Disconnected: "+reason);
            }
            if(stage==0&&event.phase==TickEvent.Phase.END) {
                if(mc.theWorld!=null)throw new IllegalStateException("Unused client required");
                mc.gameSettings.pauseOnLostFocus=false;mc.gameSettings.limitFramerate=60;
                Field f=Minecraft.class.getDeclaredField("session");f.setAccessible(true);
                f.set(mc,new Session(name,UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes(StandardCharsets.UTF_8)).toString(),"0","legacy"));
                ServerData server=new ServerData("Isolated boat probe","127.0.0.1:"+port,false);
                ((ExtendedServerData)server).viaForge$setVersion(ProtocolVersion.getProtocol(protocol));
                mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(),mc,server));stage=1;state("connecting","");return;
            }
            if(mc.thePlayer==null||ServerBlockSession.getLoadedResourceVersion()==null)return;
            if(stage==1&&com.viaversion.viaforge.compatibility.ServerSession.awaitingWorld(mc.getNetHandler()))return;
            if(stage==1&&event.phase==TickEvent.Phase.END) {
                mc.displayGuiScreen(null);mc.setIngameFocus();
                ChannelPipeline pipeline=mc.getNetHandler().getNetworkManager().channel().pipeline();
                for(String handler:pipeline.names()) if(pipeline.get(handler) instanceof com.viaversion.viaforge.common.compatibility.CompatibilityDecodeHandler) {
                    pipeline.addBefore(handler,"boat_wire_before",wire("wire_before"));
                    pipeline.addAfter(handler,"boat_wire_after",wire("wire_after"));break;
                }
                mc.getNetHandler().getNetworkManager().channel().pipeline().addBefore("packet_handler","boat_probe",new ChannelDuplexHandler(){
                    @Override public void write(ChannelHandlerContext ctx,Object msg,ChannelPromise promise)throws Exception {
                        JsonObject j=new JsonObject();j.addProperty("phase","send");j.addProperty("packet",msg.getClass().getSimpleName());
                        if(msg instanceof net.minecraft.network.play.client.C0BPacketEntityAction)j.addProperty("action",((net.minecraft.network.play.client.C0BPacketEntityAction)msg).getAction().toString());
                        if(msg instanceof net.minecraft.network.play.client.C09PacketHeldItemChange)j.addProperty("slot",((net.minecraft.network.play.client.C09PacketHeldItemChange)msg).getSlotId());
                        if(msg instanceof net.minecraft.network.play.client.C10PacketCreativeInventoryAction){net.minecraft.network.play.client.C10PacketCreativeInventoryAction p=(net.minecraft.network.play.client.C10PacketCreativeInventoryAction)msg;j.addProperty("slot",p.getSlotId());j.add("item",LiveHotbarActions.item(p.getStack()));}
                        if(msg instanceof C17PacketCustomPayload){C17PacketCustomPayload p=(C17PacketCustomPayload)msg;j.addProperty("channel",p.getChannelName());
                            if(p.getChannelName().equals("VF|boat")){net.minecraft.network.PacketBuffer b=p.getBufferData();j.addProperty("op",b.getUnsignedByte(b.readerIndex()));}}
                        if(tick<10000)log(j);super.write(ctx,msg,promise);
                    }
                    @Override public void channelRead(ChannelHandlerContext ctx,Object msg)throws Exception {
                        String n=msg.getClass().getSimpleName();
                        if(tick<10000&&(n.contains("Teleport")||n.contains("Position")||n.contains("Attach")||n.contains("Velocity")||n.contains("CustomPayload"))){JsonObject j=new JsonObject();j.addProperty("phase","receive");j.addProperty("packet",n);log(j);}
                        super.channelRead(ctx,msg);
                    }
                });
                stage=2;state("ready","Resource gate completed");
            }
            if(stage!=2)return;
            if(event.phase==TickEvent.Phase.START) {
                tick++;
                Path command=folder.resolve("action.txt");String next=Files.exists(command)?new String(Files.readAllBytes(command),StandardCharsets.UTF_8).trim():"idle";
                if(!next.equals(action)){action=next;actionTick=0;}else actionTick++;
                if(action.equals("stop")){state("DONE","Recorded input sequence");finished=true;mc.shutdown();return;}
                if(action.equals("reconnect")) {
                    Files.write(command,Collections.singletonList("idle"),StandardCharsets.UTF_8);
                    mc.theWorld.sendQuittingDisconnectingPacket();mc.loadWorld(null);mc.displayGuiScreen(new GuiMainMenu());
                    stage=0;state("reconnecting","Same account, normal disconnect and new connection");return;
                }
                if(action.equals("board")&&actionTick%10==0&&mc.thePlayer.ridingEntity==null) {
                    Entity nearest=null;double distance=16;
                    for(Object e:mc.theWorld.loadedEntityList)if(e instanceof ServerBoat){double d=mc.thePlayer.getDistanceSqToEntity((Entity)e);if(d<distance){nearest=(Entity)e;distance=d;}}
                    if(nearest!=null) {
                        double dx=nearest.posX-mc.thePlayer.posX,dz=nearest.posZ-mc.thePlayer.posZ;
                        double dy=nearest.posY+.25-(mc.thePlayer.posY+mc.thePlayer.getEyeHeight());
                        mc.thePlayer.rotationYaw=(float)(Math.atan2(dz,dx)*180/Math.PI)-90;
                        mc.thePlayer.rotationPitch=(float)(-Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))*180/Math.PI);
                        mc.entityRenderer.getMouseOver(1);
                        java.lang.reflect.Method click=Minecraft.class.getDeclaredMethod("rightClickMouse");click.setAccessible(true);click.invoke(mc);
                    }
                }
                LiveHotbarActions.act(action,actionTick);
                String keys=sequenceInput(action,actionTick);
                // Camera input only; the original tick emits movement/use packets.
                if(keys.contains("turn"))mc.thePlayer.setAngles(10F,0F);
                key(mc.gameSettings.keyBindForward,keys.contains("forward"));key(mc.gameSettings.keyBindBack,keys.contains("back"));
                key(mc.gameSettings.keyBindLeft,keys.contains("left"));key(mc.gameSettings.keyBindRight,keys.contains("right"));
                key(mc.gameSettings.keyBindSneak,action.equals("dismount")||keys.contains("sneak"));
                key(mc.gameSettings.keyBindSprint,keys.contains("sprint"));key(mc.gameSettings.keyBindJump,keys.contains("jump"));
                if("1".equals(System.getenv("VIAFORGE_INTERACTION_PROBE"))) {
                    if(keys.contains("photo")&&actionTick==30)net.minecraft.util.ScreenShotHelper.saveScreenshot(folder.toFile(),"blocking-"+tick+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                    for(net.minecraft.client.settings.KeyBinding binding:new net.minecraft.client.settings.KeyBinding[]{mc.gameSettings.keyBindUseItem,mc.gameSettings.keyBindAttack}) {
                        boolean down=keys.contains(binding==mc.gameSettings.keyBindUseItem?"use":"attack");
                        if(down&&!binding.isKeyDown())net.minecraft.client.settings.KeyBinding.onTick(binding.getKeyCode());
                        key(binding,down);
                    }
                }
            }
            JsonObject j=new JsonObject();j.addProperty("phase",event.phase.toString());j.addProperty("action",action);j.addProperty("action_tick",actionTick);
            j.addProperty("player_tick",mc.thePlayer.ticksExisted);j.addProperty("gamemode",mc.playerController.getCurrentGameType().toString());
            j.addProperty("player_x",mc.thePlayer.posX);j.addProperty("player_y",mc.thePlayer.posY);j.addProperty("player_z",mc.thePlayer.posZ);
            if("1".equals(System.getenv("VIAFORGE_INTERACTION_PROBE"))) {
                j.addProperty("using_item",mc.thePlayer.isUsingItem());j.addProperty("ladder",mc.thePlayer.isOnLadder());
                j.addProperty("use_ticks",mc.thePlayer.getItemInUseCount());
                j.addProperty("hit",String.valueOf(mc.objectMouseOver));
                j.addProperty("held",String.valueOf(mc.thePlayer.getHeldItem()));
                if(tick%20==0)j.addProperty("held_nbt",String.valueOf(mc.thePlayer.getHeldItem()==null?null:mc.thePlayer.getHeldItem().getTagCompound()));
            }
            if("1".equals(System.getenv("VIAFORGE_HOTBAR_PROBE")))LiveHotbarActions.observe(j);
            if("1".equals(System.getenv("VIAFORGE_SWIM_PROBE"))) {
                j.addProperty("loaded",mc.theWorld.isBlockLoaded(new net.minecraft.util.BlockPos(mc.thePlayer)));
                j.addProperty("paused",mc.isGamePaused());
                j.addProperty("water",mc.thePlayer.isInWater());j.addProperty("eye_water",mc.thePlayer.isInsideOfMaterial(net.minecraft.block.material.Material.water));
                j.addProperty("sneaking",mc.thePlayer.isSneaking());j.addProperty("input_sneak",mc.thePlayer.movementInput.sneak);
                j.addProperty("key_sneak",mc.gameSettings.keyBindSneak.isKeyDown());j.addProperty("key_sprint",mc.gameSettings.keyBindSprint.isKeyDown());
                j.addProperty("speed_attribute",mc.thePlayer.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed).getAttributeValue());
                j.addProperty("collision_h",mc.thePlayer.isCollidedHorizontally);j.addProperty("collision_v",mc.thePlayer.isCollidedVertically);
                j.addProperty("crawling",com.viaversion.viaforge.items.ServerElytraFlight.crawling(mc.thePlayer));
                j.addProperty("crouching",com.viaversion.viaforge.items.ServerElytraFlight.crouching(mc.thePlayer));
                j.addProperty("floor",mc.theWorld.getBlockState(new net.minecraft.util.BlockPos(mc.thePlayer.posX,mc.thePlayer.posY-.01,mc.thePlayer.posZ)).toString());
                j.addProperty("sprinting",mc.thePlayer.isSprinting());j.addProperty("height",mc.thePlayer.height);j.addProperty("eye_height",mc.thePlayer.getEyeHeight());
                j.addProperty("yaw",mc.thePlayer.rotationYaw);j.addProperty("pitch",mc.thePlayer.rotationPitch);
                j.addProperty("animation",mc.thePlayer.limbSwing);j.addProperty("swim_amount",com.viaversion.viaforge.items.ServerElytraVisuals.crawlAmount(mc.thePlayer,1));
                j.addProperty("swimming",com.viaversion.viaforge.compatibility.ServerSwimming.swimming(mc.thePlayer));
                j.addProperty("sneak_speed",com.viaversion.viaforge.compatibility.ServerSwimming.sneakSpeed(mc.thePlayer));
                if(tick%20==0)j.addProperty("leggings_tag",String.valueOf(mc.thePlayer.getCurrentArmor(1)==null?null:mc.thePlayer.getCurrentArmor(1).getTagCompound()));
                j.addProperty("water_depth",com.viaversion.viaforge.compatibility.ServerSwimming.depth(mc.thePlayer));
                j.addProperty("dolphins_grace",mc.thePlayer.isPotionActive(30));
                j.addProperty("conduit_power",mc.thePlayer.isPotionActive(29));j.addProperty("air",mc.thePlayer.getAir());
                j.addProperty("depth_strider",net.minecraft.enchantment.EnchantmentHelper.getDepthStriderModifier(mc.thePlayer));
                if(event.phase==TickEvent.Phase.END) {
                    JsonArray fluids=new JsonArray();net.minecraft.util.AxisAlignedBB box=mc.thePlayer.getEntityBoundingBox().contract(.001,.001,.001);
                    for(net.minecraft.util.BlockPos pos:net.minecraft.util.BlockPos.getAllInBox(new net.minecraft.util.BlockPos(box.minX,box.minY,box.minZ),new net.minecraft.util.BlockPos(box.maxX,box.maxY,box.maxZ))) {
                        JsonObject cell=new JsonObject();cell.addProperty("x",pos.getX());cell.addProperty("y",pos.getY());cell.addProperty("z",pos.getZ());
                        cell.addProperty("fluid",com.viaversion.viaforge.compatibility.ServerSwimming.fluids.get(pos.getX(),pos.getY(),pos.getZ()));
                        cell.addProperty("block",mc.theWorld.getBlockState(pos).toString());fluids.add(cell);
                    }
                    j.add("fluids",fluids);
                }
            }
            if("1".equals(System.getenv("VIAFORGE_PUSH_PROBE"))) {
                j.addProperty("player_vx",mc.thePlayer.motionX);j.addProperty("player_vy",mc.thePlayer.motionY);j.addProperty("player_vz",mc.thePlayer.motionZ);
                j.addProperty("player_alive",mc.thePlayer.isEntityAlive());j.addProperty("player_health",mc.thePlayer.getHealth());
                j.addProperty("player_box",mc.thePlayer.getEntityBoundingBox().toString());j.addProperty("ground",mc.thePlayer.onGround);
                j.addProperty("ladder",mc.thePlayer.isOnLadder());j.addProperty("spectator",mc.thePlayer.isSpectator());
                JsonArray nearby=new JsonArray();
                for(Object value:mc.theWorld.loadedEntityList) {
                    Entity e=(Entity)value;if(e==mc.thePlayer||mc.thePlayer.getDistanceSqToEntity(e)>16)continue;
                    JsonObject n=new JsonObject();n.addProperty("id",e.getEntityId());n.addProperty("type",e.getClass().getSimpleName());
                    n.addProperty("x",e.posX);n.addProperty("y",e.posY);n.addProperty("z",e.posZ);
                    n.addProperty("vx",e.motionX);n.addProperty("vz",e.motionZ);n.addProperty("box",e.getEntityBoundingBox().toString());
                    n.addProperty("overlap",e.getEntityBoundingBox().intersectsWith(mc.thePlayer.getEntityBoundingBox()));
                    nearby.add(n);
                }
                j.add("nearby",nearby);
            }
            j.addProperty("input_forward",mc.thePlayer.movementInput.moveForward);j.addProperty("input_strafe",mc.thePlayer.movementInput.moveStrafe);
            if(event.phase==TickEvent.Phase.END&&tick%20==0) {
                JsonObject setup=new JsonObject();setup.addProperty("tick",tick);setup.addProperty("time_ms",System.currentTimeMillis());
                setup.addProperty("gamemode",mc.playerController.getCurrentGameType().toString());
                setup.addProperty("player_y",mc.thePlayer.posY);
                setup.addProperty("fixture_water",mc.theWorld.getBlockState(new net.minecraft.util.BlockPos(-31,64,0)).getBlock().getMaterial()==net.minecraft.block.material.Material.water);
                setup.addProperty("boat_present",mc.theWorld.loadedEntityList.stream().anyMatch(e->e instanceof ServerBoat));
                setup.addProperty("driver",mc.thePlayer.ridingEntity instanceof ServerBoat&&((ServerBoat)mc.thePlayer.ridingEntity).controlled());
                Path tmp=folder.resolve("setup.tmp");Files.write(tmp,Collections.singletonList(setup.toString()),StandardCharsets.UTF_8);
                Files.move(tmp,folder.resolve("setup.json"),StandardCopyOption.REPLACE_EXISTING);
            }
            if(mc.thePlayer.ridingEntity instanceof ServerBoat) {
                ServerBoat b=(ServerBoat)mc.thePlayer.ridingEntity;j.addProperty("boat",b.getEntityId());j.addProperty("driver",b.controlled());
                j.addProperty("boat_tick",b.ticksExisted);j.addProperty("x",b.posX);j.addProperty("y",b.posY);j.addProperty("z",b.posZ);
                j.addProperty("vx",b.motionX);j.addProperty("vy",b.motionY);j.addProperty("vz",b.motionZ);j.addProperty("yaw",b.rotationYaw);j.addProperty("pitch",b.rotationPitch);
                j.addProperty("delta_rotation",b.deltaRotation);j.addProperty("status",String.valueOf(b.status));j.addProperty("land_glide",b.landGlide);
                j.addProperty("wood",b.wood);j.addProperty("seat",b.passengers().indexOf(mc.thePlayer));j.addProperty("passenger_count",b.passengers().size());
                j.addProperty("ground",b.onGround);j.addProperty("collision_h",b.isCollidedHorizontally);j.addProperty("collision_v",b.isCollidedVertically);
                j.addProperty("box",b.getEntityBoundingBox().toString());j.addProperty("paddle_left",b.paddle(0));j.addProperty("paddle_right",b.paddle(1));
                for(String field:new String[]{"waterLevel","lastYMotion","lerpSteps","previousStatus","targetX","targetY","targetZ","left","right","forward","back"}){
                    Field f=ServerBoat.class.getDeclaredField(field);f.setAccessible(true);Object v=f.get(b);
                    if(v instanceof Number)j.addProperty(field,(Number)v);else j.addProperty(field,String.valueOf(v));
                }
            }
            if(tick<10000)log(j);
        }catch(Throwable t){try{state("FAIL",t.toString());}catch(Exception ignored){}t.printStackTrace();finished=true;mc.shutdown();}
    }
    private static String sequenceInput(String action,int tick) {
        if(!action.startsWith("sequence:"))return action;
        String last="idle";
        for(String step:action.substring(9).split("\\|")) {
            int separator=step.indexOf(':');int count=Integer.parseInt(step.substring(0,separator));last=step.substring(separator+1);
            if(tick<count)return last;tick-=count;
        }
        return last;
    }
    private static void key(KeyBinding key,boolean down){KeyBinding.setKeyBindState(key.getKeyCode(),down);}
    private ChannelInboundHandlerAdapter wire(final String phase) {
        return new ChannelInboundHandlerAdapter(){
            private int count;
            @Override public void channelRead(ChannelHandlerContext ctx,Object msg)throws Exception {
                if(msg instanceof io.netty.buffer.ByteBuf&&count++<20000){io.netty.buffer.ByteBuf b=(io.netty.buffer.ByteBuf)msg;
                    if(b.readableBytes()<512){JsonObject j=new JsonObject();j.addProperty("phase",phase);j.addProperty("hex",io.netty.buffer.ByteBufUtil.hexDump(b));log(j);}}
                super.channelRead(ctx,msg);
            }
        };
    }
}
