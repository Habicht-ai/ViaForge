package viaforge.lab;

import com.google.gson.Gson;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.security.ProtectionDomain;
import java.util.*;
import org.objectweb.asm.*;

/** Official 26.2. Only keys and observation at the two ends of Minecraft.tick. */
public final class NativePushProbe {
    private static Path directory;
    private static long started;
    private static int ticks,actionTick;
    private static boolean ready,done;
    private static String action="idle";
    private static final Gson GSON=new Gson();
    private static final java.util.Queue<String> packetEvents=new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static long visualCommand=-1;
    public static void premain(String path,Instrumentation instrumentation) {
        directory=Paths.get(path);started=System.currentTimeMillis();
        instrumentation.addTransformer(new ClassFileTransformer(){
            @Override public byte[] transform(ClassLoader loader,String name,Class<?> type,ProtectionDomain domain,byte[] bytes){
                boolean visual="1".equals(System.getenv("VIAFORGE_VISUAL_SESSION"));
                boolean particle=visual&&(name.equals("net/minecraft/client/particle/BubbleColumnUpParticle")||name.equals("net/minecraft/client/particle/WaterCurrentDownParticle")||name.equals("net/minecraft/client/particle/BubblePopParticle"));
                boolean arms=visual&&name.equals("net/minecraft/client/model/HumanoidModel");
                boolean player=name.equals("net/minecraft/client/player/LocalPlayer");
                boolean packets=name.equals("net/minecraft/client/multiplayer/ClientPacketListener")||name.equals("net/minecraft/client/multiplayer/ClientCommonPacketListenerImpl");
                if(!name.equals("net/minecraft/client/Minecraft")&&!player&&!packets&&!particle&&!arms)return null;
                ClassReader reader=new ClassReader(bytes);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
                reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
                    @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                        MethodVisitor method=super.visitMethod(access,name,descriptor,signature,exceptions);
                        if(particle||arms) {
                            if(!(particle&&(name.equals("<init>")||name.equals("tick")))&&!(arms&&name.equals("setupAnim")&&descriptor.contains("HumanoidRenderState")))return method;
                            return new MethodVisitor(Opcodes.ASM9,method){
                                @Override public void visitInsn(int opcode){
                                    if(opcode==Opcodes.RETURN){super.visitVarInsn(Opcodes.ALOAD,0);super.visitLdcInsn(name);super.visitMethodInsn(Opcodes.INVOKESTATIC,"viaforge/lab/NativePushProbe","visual","(Ljava/lang/Object;Ljava/lang/String;)V",false);}
                                    super.visitInsn(opcode);
                                }
                            };
                        }
                        if(packets) {
                            if(!name.equals("handlePing")&&!name.equals("handleSetEntityData")&&!(visual&&(name.equals("handleResourcePackPush")||name.equals("handleResourcePackPop")))&&!(name.equals("send")&&descriptor.equals("(Lnet/minecraft/network/protocol/Packet;)V")))return method;
                            return new MethodVisitor(Opcodes.ASM9,method){
                                private void log(String phase){super.visitVarInsn(Opcodes.ALOAD,1);super.visitLdcInsn(phase);super.visitMethodInsn(Opcodes.INVOKESTATIC,"viaforge/lab/NativePushProbe","packet","(Ljava/lang/Object;Ljava/lang/String;)V",false);}
                                @Override public void visitCode(){super.visitCode();log(name+"_BEGIN");}
                                @Override public void visitInsn(int opcode){if(opcode==Opcodes.RETURN)log(name+"_END");super.visitInsn(opcode);}
                            };
                        }
                        if(player) {
                            if(!name.equals("aiStep")&&!name.equals("applyInput")&&!name.equals("sendPosition"))return method;
                            return new MethodVisitor(Opcodes.ASM9,method){
                                private void observe(String phase){super.visitVarInsn(Opcodes.ALOAD,0);super.visitLdcInsn(phase);super.visitMethodInsn(Opcodes.INVOKESTATIC,"viaforge/lab/NativePushProbe","observe","(Ljava/lang/Object;Ljava/lang/String;)V",false);}
                                @Override public void visitCode(){super.visitCode();observe(name+"_BEGIN");}
                                @Override public void visitInsn(int opcode){if(opcode==Opcodes.RETURN)observe(name+"_END");super.visitInsn(opcode);}
                                @Override public void visitMethodInsn(int opcode,String owner,String invoked,String desc,boolean itf){
                                    boolean input=owner.equals("net/minecraft/client/player/ClientInput")&&invoked.equals("tick");
                                    if(input)observe("INPUT_BEFORE");super.visitMethodInsn(opcode,owner,invoked,desc,itf);if(input)observe("INPUT_AFTER");
                                }
                            };
                        }
                        if(!name.equals("tick")||!descriptor.equals("()V"))return method;
                        return new MethodVisitor(Opcodes.ASM9,method){
                            private void hook(boolean end){visitVarInsn(Opcodes.ALOAD,0);visitInsn(end?Opcodes.ICONST_1:Opcodes.ICONST_0);visitMethodInsn(Opcodes.INVOKESTATIC,"viaforge/lab/NativePushProbe","tick","(Ljava/lang/Object;Z)V",false);}
                            @Override public void visitCode(){super.visitCode();hook(false);}
                            @Override public void visitInsn(int opcode){if(opcode==Opcodes.RETURN)hook(true);super.visitInsn(opcode);}
                        };
                    }
                },0);return writer.toByteArray();
            }
        });
    }
    private static Object field(Object o,String name)throws Exception {
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static Object call(Object o,String name)throws Exception{return o.getClass().getMethod(name).invoke(o);}
    public static void visual(Object object,String phase) {
        try {
            Map<String,Object> row=new LinkedHashMap<>();row.put("time_ms",System.currentTimeMillis());row.put("tick",ticks);row.put("action",action);
            row.put("visual",object.getClass().getSimpleName());row.put("phase",phase);row.put("id",System.identityHashCode(object));
            if(phase.equals("setupAnim")) {
                if(ticks%10!=0)return;
                for(String side:new String[]{"rightArm","leftArm"})for(String axis:new String[]{"xRot","yRot","zRot"})row.put(side+"_"+axis,field(field(object,side),axis));
            }else {
                for(String name:new String[]{"x","y","z","xd","yd","zd","age","lifetime","removed"})row.put(name,field(object,name));
                Method light=Class.forName("net.minecraft.client.particle.Particle").getDeclaredMethod("getLightCoords",float.class);
                light.setAccessible(true);row.put("light",light.invoke(object,0F));
            }
            packetEvents.add(GSON.toJson(row));
        }catch(Exception error){throw new IllegalStateException("Native visual observation",error);}
    }
    public static void packet(Object packet,String phase) {
        String type=packet.getClass().getSimpleName();
        boolean pack="1".equals(System.getenv("VIAFORGE_VISUAL_SESSION"))&&type.contains("ResourcePack");
        if(!pack&&!type.equals("ClientboundPingPacket")&&!type.equals("ServerboundPongPacket")&&!type.equals("ClientboundSetEntityDataPacket"))return;
        try {
            Map<String,Object> j=new LinkedHashMap<>();j.put("time_ms",System.currentTimeMillis());j.put("phase",phase);j.put("thread",Thread.currentThread().getName());j.put("packet",type);
            j.put("id",pack?String.valueOf(call(packet,"id")):call(packet,type.equals("ClientboundSetEntityDataPacket")?"id":"getId"));
            if(pack)j.put("details",String.valueOf(packet));
            if(type.equals("ClientboundSetEntityDataPacket"))j.put("metadata",String.valueOf(call(packet,"packedItems")));
            packetEvents.add(GSON.toJson(j));
        }catch(Exception failure){throw new IllegalStateException("Native packet observation failed",failure);}
    }
    public static void observe(Object player,String phase) {
        if(System.getenv("VIAFORGE_SNEAK_PROBE")==null||done||ticks>36000)return;
        try {
            Map<String,Object> j=new LinkedHashMap<>();j.put("time_ms",System.currentTimeMillis());j.put("tick",ticks);j.put("phase",phase);j.put("action",action);
            j.put("player_tick",field(player,"tickCount"));j.put("sprinting",call(player,"isSprinting"));j.put("sneaking",call(player,"isShiftKeyDown"));
            j.put("crouching",call(player,"isCrouching"));j.put("slowMovement",call(player,"isMovingSlowly"));j.put("swimming",call(player,"isSwimming"));
            j.put("pose",String.valueOf(call(player,"getPose")));j.put("height",call(player,"getBbHeight"));j.put("eye_height",call(player,"getEyeHeight"));j.put("sprintWindow",field(player,"sprintTriggerTime"));
            Object input=field(player,"input"),vector=call(input,"getMoveVector");j.put("input_forward",field(vector,"y"));j.put("input_strafe",field(vector,"x"));j.put("keys",String.valueOf(field(input,"keyPresses")));
            j.put("travel_forward",field(player,"zza"));j.put("travel_strafe",field(player,"xxa"));
            j.put("x",call(player,"getX"));j.put("y",call(player,"getY"));j.put("z",call(player,"getZ"));j.put("velocity",String.valueOf(call(player,"getDeltaMovement")));
            j.put("box",String.valueOf(call(player,"getBoundingBox")));j.put("water",call(player,"isInWater"));j.put("eye_water",call(player,"isUnderWater"));
            j.put("ground",call(player,"onGround"));j.put("collision_h",field(player,"horizontalCollision"));
            Files.writeString(directory.resolve("movement.jsonl"),GSON.toJson(j)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }catch(Exception failure){throw new IllegalStateException("Native observation failed",failure);}
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
    private static void key(Object options,String name,boolean down)throws Exception {
        Object key=field(options,name);key.getClass().getMethod("setDown",boolean.class).invoke(key,down);
    }
    private static void state(String status,String detail)throws Exception {
        Map<String,Object> m=new LinkedHashMap<>();m.put("state",status);m.put("detail",detail);
        m.put("name",System.getProperty("push.name"));m.put("protocol",776);
        Path tmp=directory.resolve("client-state.tmp");Files.writeString(tmp,GSON.toJson(m));Files.move(tmp,directory.resolve("client-state.json"),StandardCopyOption.REPLACE_EXISTING);
    }
    private static void visualCommand(Object mc)throws Exception {
        Object screen=call(field(mc,"gui"),"screen");
        Path path=directory.resolve("command.json");
        if(Files.exists(path)) {
            com.google.gson.JsonObject r=GSON.fromJson(Files.readString(path),com.google.gson.JsonObject.class);
            if(r.get("id").getAsLong()!=visualCommand) {
                visualCommand=r.get("id").getAsLong();String op=r.get("op").getAsString();
                if(op.equals("button")) {
                    int i=0,index=r.get("button").getAsInt();Object chosen=null;
                    for(Object child:(Iterable<?>)call(screen,"children"))if(Class.forName("net.minecraft.client.gui.components.Button").isInstance(child)) {if(i++==index){chosen=child;break;}}
                    if(chosen==null)throw new IllegalStateException("Missing original button");
                    double x=((Number)call(chosen,"getX")).doubleValue()+((Number)call(chosen,"getWidth")).doubleValue()/2;
                    double y=((Number)call(chosen,"getY")).doubleValue()+((Number)call(chosen,"getHeight")).doubleValue()/2;
                    Class<?> info=Class.forName("net.minecraft.client.input.MouseButtonInfo"),event=Class.forName("net.minecraft.client.input.MouseButtonEvent");
                    Object click=event.getConstructor(double.class,double.class,info).newInstance(x,y,info.getConstructor(int.class,int.class).newInstance(0,0));
                    screen.getClass().getMethod("mouseClicked",event,boolean.class).invoke(screen,click,false);
                }else if(op.equals("chat")) {
                    String text=r.get("text").getAsString();Object connection=field(field(mc,"player"),"connection");
                    connection.getClass().getMethod(text.startsWith("/")?"sendCommand":"sendChat",String.class).invoke(connection,text.startsWith("/")?text.substring(1):text);
                }
                else if(op.equals("look")) {
                    Object player=field(mc,"player");
                    double yaw=r.get("yaw").getAsDouble()-((Number)call(player,"getYRot")).doubleValue();
                    double pitch=r.get("pitch").getAsDouble()-((Number)call(player,"getXRot")).doubleValue();
                    player.getClass().getMethod("turn",double.class,double.class).invoke(player,yaw/.15,pitch/.15);
                }
                else if(op.equals("resource")) {
                    Class<?> id=Class.forName("net.minecraft.resources.Identifier");Object location=id.getMethod("parse",String.class).invoke(null,r.get("path").getAsString());
                    Object resource=call(mc,"getResourceManager").getClass().getMethod("getResourceOrThrow",id).invoke(call(mc,"getResourceManager"),location);
                    int pixel;try(java.io.InputStream stream=(java.io.InputStream)call(resource,"open")){pixel=javax.imageio.ImageIO.read(stream).getRGB(0,0);}
                    packetEvents.add("{\"resource\":\""+r.get("path").getAsString()+"\",\"argb\":"+pixel+",\"command\":"+visualCommand+"}");
                }else if(op.equals("stop"))call(mc,"stop");
                else throw new IllegalArgumentException(op);
            }
        }
        Map<String,Object> row=new LinkedHashMap<>();row.put("command",visualCommand);row.put("time_ms",System.currentTimeMillis());row.put("screen",screen==null?"none":screen.getClass().getSimpleName());
        row.put("packs",String.valueOf(call(call(mc,"getResourcePackRepository"),"getSelectedIds")));
        try {Files.writeString(directory.resolve("ui-state.json"),GSON.toJson(row));}catch(FileSystemException busySnapshot){}
    }
    public static void tick(Object mc,boolean end) {
        if(done)return;
        try {
            if(!end&&"1".equals(System.getenv("VIAFORGE_VISUAL_SESSION")))visualCommand(mc);
            if(end&&!packetEvents.isEmpty()) {
                List<String> batch=new ArrayList<>();String entry;
                while((entry=packetEvents.poll())!=null)batch.add(entry);
                Files.write(directory.resolve("packet-order.jsonl"),batch,java.nio.charset.StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }
            Path path=directory.resolve("action.txt");
            if(!end){
                String next=Files.exists(path)?Files.readString(path).trim():"idle";
                if(!next.equals(action)){action=next;actionTick=0;}else actionTick++;
                if(action.equals("stop")){state("DONE","Input sequence recorded");done=true;call(mc,"stop");return;}
                if(System.currentTimeMillis()-started>Long.parseLong(System.getenv().getOrDefault("VIAFORGE_PROBE_TIMEOUT_MS","600000")))throw new IllegalStateException("Native probe timeout");
            }
            Object player=field(mc,"player"),level=field(mc,"level");if(player==null||level==null)return;
            if(!ready){if(!end)return;ready=true;state("ready","Official Mojang 26.2");}
            if(!end){
                ticks++;Object options=field(mc,"options");
                String keys=sequenceInput(action,actionTick);
                if("1".equals(System.getenv("VIAFORGE_VISUAL_SESSION"))) {
                    Class camera=Class.forName("net.minecraft.client.CameraType");
                    options.getClass().getMethod("setCameraType",camera).invoke(options,Enum.valueOf(camera,keys.contains("f5front")?"THIRD_PERSON_FRONT":keys.contains("f5back")?"THIRD_PERSON_BACK":"FIRST_PERSON"));
                    Object particles=call(options,"particles");Class status=Class.forName("net.minecraft.server.level.ParticleStatus");
                    particles.getClass().getMethod("set",Object.class).invoke(particles,Enum.valueOf(status,keys.contains("minimal")?"MINIMAL":keys.contains("decreased")?"DECREASED":"ALL"));
                    if(actionTick==0)for(int i=0;i<9;i++)if(keys.contains("slot"+i)) {
                        Object binding=((Object[])field(options,"keyHotbarSlots"))[i];Field click=Class.forName("net.minecraft.client.KeyMapping").getDeclaredField("clickCount");click.setAccessible(true);click.setInt(binding,click.getInt(binding)+1);
                    }
                }
                if(keys.contains("turn"))player.getClass().getMethod("turn",double.class,double.class).invoke(player,10D,0D);
                if(keys.contains("lookup"))player.getClass().getMethod("turn",double.class,double.class).invoke(player,0D,(-(keys.contains("near")?89.9:90)-(Float)call(player,"getXRot"))/.15);
                if(keys.contains("close"))call(player,"closeContainer");
                if(keys.contains("photo")&&actionTick==30)Class.forName("net.minecraft.client.Screenshot").getMethod("grab",java.io.File.class,String.class,Class.forName("com.mojang.blaze3d.pipeline.RenderTarget"),int.class,java.util.function.Consumer.class)
                    .invoke(null,directory.toFile(),"blocking-"+ticks+".png",call(field(mc,"gameRenderer"),"mainRenderTarget"),1,(java.util.function.Consumer<Object>)(message)->{});
                key(options,"keyUp",keys.contains("forward"));key(options,"keyDown",keys.contains("back"));
                key(options,"keyLeft",keys.contains("left"));key(options,"keyRight",keys.contains("right"));
                key(options,"keyShift",keys.contains("sneak"));key(options,"keyJump",keys.contains("jump"));key(options,"keySprint",keys.contains("sprint"));
                if("1".equals(System.getenv("VIAFORGE_INTERACTION_PROBE"))) {
                    for(String n:new String[]{"keyUse","keyAttack"}) {
                        Object binding=field(options,n);boolean down=keys.contains(n.equals("keyUse")?"use":"attack");
                        if(down&&!(Boolean)call(binding,"isDown")) {
                            Field f=Class.forName("net.minecraft.client.KeyMapping").getDeclaredField("clickCount");f.setAccessible(true);f.setInt(binding,f.getInt(binding)+1);
                        }
                        key(options,n,down);
                    }
                }
            }
            Map<String,Object> j=new LinkedHashMap<>();j.put("time_ms",System.currentTimeMillis());j.put("tick",ticks);j.put("phase",end?"END":"START");j.put("action",action);j.put("action_tick",actionTick);
            double x=(Double)call(player,"getX"),y=(Double)call(player,"getY"),z=(Double)call(player,"getZ");
            j.put("player_x",x);j.put("player_y",y);j.put("player_z",z);
            if("1".equals(System.getenv("VIAFORGE_VISUAL_SESSION"))) {
                j.put("yaw",call(player,"getYRot"));j.put("pitch",call(player,"getXRot"));
                if(ticks%10==0) {
                    List<Map<String,Object>> remote=new ArrayList<>();
                    for(Object other:(Iterable<?>)call(level,"entitiesForRendering"))if(other.getClass().getSimpleName().equals("RemotePlayer")) {
                        Map<String,Object> p=new LinkedHashMap<>();p.put("name",String.valueOf(call(other,"getName")));p.put("x",call(other,"getX"));p.put("y",call(other,"getY"));p.put("z",call(other,"getZ"));p.put("invisible",call(other,"isInvisible"));p.put("using",call(other,"isUsingItem"));p.put("held",String.valueOf(call(other,"getMainHandItem")));remote.add(p);
                    }
                    j.put("remote_players",remote);
                }
            }
            if("1".equals(System.getenv("VIAFORGE_INTERACTION_PROBE"))) {
                j.put("elytra",call(player,"isFallFlying"));
                int boosts=0;
                for(Object entity:(Iterable<?>)call(level,"entitiesForRendering"))
                    if(entity.getClass().getName().endsWith("FireworkRocketEntity")&&field(entity,"attachedToEntity")==player)boosts++;
                j.put("boosts",boosts);
                Class<?> bp=Class.forName("net.minecraft.core.BlockPos");Object pos=bp.getConstructor(int.class,int.class,int.class).newInstance(1,64,0);
                Object column=bp.getConstructor(int.class,int.class,int.class).newInstance((int)Math.floor(x),63,(int)Math.floor(z));
                j.put("loaded_column",level.getClass().getMethod("hasChunkAt",bp).invoke(level,column));
                Object feet=bp.getConstructor(int.class,int.class,int.class).newInstance((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(z));
                String feetState=String.valueOf(level.getClass().getMethod("getBlockState",bp).invoke(level,feet));
                j.put("bubble",feetState.contains("bubble_column")?(feetState.contains("drag=true")?18:17):0);
                for(String key:new String[]{"floor","substrate"}) {
                    Object below=bp.getConstructor(int.class,int.class,int.class).newInstance((int)Math.floor(x),key.equals("floor")?(int)Math.floor(y-.01):63,(int)Math.floor(z));
                    j.put(key,String.valueOf(level.getClass().getMethod("getBlockState",bp).invoke(level,below)));
                }
                Object box=level.getClass().getMethod("getBlockEntity",bp).invoke(level,pos);
                if(box!=null&&box.getClass().getName().endsWith("ShulkerBoxBlockEntity"))j.put("shulker_progress",box.getClass().getMethod("getProgress",float.class).invoke(box,1F));
            }
            Object velocity=call(player,"getDeltaMovement");j.put("player_vx",field(velocity,"x"));j.put("player_vy",field(velocity,"y"));j.put("player_vz",field(velocity,"z"));
            j.put("player_ground",call(player,"onGround"));j.put("player_spectator",call(player,"isSpectator"));
            j.put("player_alive",call(player,"isAlive"));j.put("player_health",call(player,"getHealth"));
            if("1".equals(System.getenv("VIAFORGE_INTERACTION_PROBE"))) {
                j.put("using_item",call(player,"isUsingItem"));j.put("ladder",call(player,"onClimbable"));
                j.put("use_ticks",call(player,"getUseItemRemainingTicks"));j.put("hit",String.valueOf(field(mc,"hitResult")));
                j.put("held",String.valueOf(call(player,"getMainHandItem")));
            }
            if("1".equals(System.getenv("VIAFORGE_SWIM_PROBE"))) {
                j.put("swimming",call(player,"isSwimming"));j.put("height",call(player,"getBbHeight"));
                j.put("water",call(player,"isInWater"));j.put("eye_water",call(player,"isUnderWater"));
                j.put("sprinting",call(player,"isSprinting"));j.put("sneaking",call(player,"isShiftKeyDown"));
                j.put("crouching",call(player,"isCrouching"));j.put("crawling",call(player,"isVisuallyCrawling"));j.put("slow_movement",call(player,"isMovingSlowly"));
                j.put("eye_height",call(player,"getEyeHeight"));j.put("sprint_timer",field(player,"sprintTriggerTime"));
                j.put("input_forward",field(player,"zza"));j.put("input_strafe",field(player,"xxa"));
                Object input=field(player,"input"),vector=call(input,"getMoveVector");j.put("raw_forward",field(vector,"y"));j.put("raw_strafe",field(vector,"x"));
                j.put("collision_h",field(player,"horizontalCollision"));j.put("collision_minor",field(player,"minorHorizontalCollision"));
                Object speed=Class.forName("net.minecraft.world.entity.ai.attributes.Attributes").getField("MOVEMENT_SPEED").get(null);
                j.put("speed_attribute",player.getClass().getMethod("getAttributeValue",Class.forName("net.minecraft.core.Holder")).invoke(player,speed));j.put("yaw",call(player,"getYRot"));j.put("pitch",call(player,"getXRot"));
                Class<?> effects=Class.forName("net.minecraft.world.effect.MobEffects");Object dolphin=effects.getField("DOLPHINS_GRACE").get(null);
                j.put("dolphins_grace",player.getClass().getMethod("hasEffect",Class.forName("net.minecraft.core.Holder")).invoke(player,dolphin));
                j.put("conduit_power",player.getClass().getMethod("hasEffect",Class.forName("net.minecraft.core.Holder")).invoke(player,effects.getField("CONDUIT_POWER").get(null)));
                j.put("air",call(player,"getAirSupply"));
                Object efficiency=Class.forName("net.minecraft.world.entity.ai.attributes.Attributes").getField("WATER_MOVEMENT_EFFICIENCY").get(null);
                Object sneakSpeed=Class.forName("net.minecraft.world.entity.ai.attributes.Attributes").getField("SNEAKING_SPEED").get(null);
                j.put("sneak_speed",player.getClass().getMethod("getAttributeValue",Class.forName("net.minecraft.core.Holder")).invoke(player,sneakSpeed));
                j.put("water_efficiency",player.getClass().getMethod("getAttributeValue",Class.forName("net.minecraft.core.Holder")).invoke(player,efficiency));
            }
            j.put("loaded",call(field(player,"connection"),"hasClientLoaded"));j.put("paused",call(mc,"isPaused"));
            if("1".equals(System.getenv("VIAFORGE_VISUAL_SESSION"))) {
                Object screen=call(field(mc,"gui"),"screen");j.put("screen",screen==null?"none":screen.getClass().getSimpleName());
                j.put("using",call(player,"isUsingItem"));j.put("active_hand",String.valueOf(call(player,"getUsedItemHand")));
                Object held=call(player,"getMainHandItem");j.put("use_action",String.valueOf(call(held,"getUseAnimation")));j.put("components",String.valueOf(call(held,"getComponents")));
                if(end)try{Files.writeString(directory.resolve("visual-state.json"),GSON.toJson(j));}catch(FileSystemException busySnapshot){}
            }
            Object playerBox=call(player,"getBoundingBox");j.put("player_box",playerBox.toString());
            List<Map<String,Object>> nearby=new ArrayList<>();
            for(Object e:(Iterable<?>)call(level,"entitiesForRendering")) {
                if(e==player)continue;
                double ex=(Double)call(e,"getX"),ey=(Double)call(e,"getY"),ez=(Double)call(e,"getZ");
                if((ex-x)*(ex-x)+(ey-y)*(ey-y)+(ez-z)*(ez-z)>16)continue;
                Map<String,Object> n=new LinkedHashMap<>();n.put("type",e.getClass().getName());n.put("id",call(e,"getId"));
                n.put("x",ex);n.put("y",ey);n.put("z",ez);Object box=call(e,"getBoundingBox");n.put("box",box.toString());
                n.put("overlap",box.getClass().getMethod("intersects",box.getClass()).invoke(box,playerBox));nearby.add(n);
            }
            j.put("nearby",nearby);
            if(ticks<10000)Files.writeString(directory.resolve("client.jsonl"),GSON.toJson(j)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }catch(Throwable e){try{state("FAIL",e.toString());e.printStackTrace();done=true;call(mc,"stop");}catch(Exception ignored){}}
    }
}
