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
    public static void premain(String path,Instrumentation instrumentation) {
        directory=Paths.get(path);started=System.currentTimeMillis();
        instrumentation.addTransformer(new ClassFileTransformer(){
            @Override public byte[] transform(ClassLoader loader,String name,Class<?> type,ProtectionDomain domain,byte[] bytes){
                boolean player=name.equals("net/minecraft/client/player/LocalPlayer");
                boolean packets=name.equals("net/minecraft/client/multiplayer/ClientPacketListener")||name.equals("net/minecraft/client/multiplayer/ClientCommonPacketListenerImpl");
                if(!name.equals("net/minecraft/client/Minecraft")&&!player&&!packets)return null;
                ClassReader reader=new ClassReader(bytes);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
                reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
                    @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                        MethodVisitor method=super.visitMethod(access,name,descriptor,signature,exceptions);
                        if(packets) {
                            if(!name.equals("handlePing")&&!name.equals("handleSetEntityData")&&!(name.equals("send")&&descriptor.equals("(Lnet/minecraft/network/protocol/Packet;)V")))return method;
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
    public static void packet(Object packet,String phase) {
        String type=packet.getClass().getSimpleName();
        if(!type.equals("ClientboundPingPacket")&&!type.equals("ServerboundPongPacket")&&!type.equals("ClientboundSetEntityDataPacket"))return;
        try {
            Map<String,Object> j=new LinkedHashMap<>();j.put("time_ms",System.currentTimeMillis());j.put("phase",phase);j.put("thread",Thread.currentThread().getName());j.put("packet",type);
            j.put("id",call(packet,type.equals("ClientboundSetEntityDataPacket")?"id":"getId"));
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
    public static void tick(Object mc,boolean end) {
        if(done)return;
        try {
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
