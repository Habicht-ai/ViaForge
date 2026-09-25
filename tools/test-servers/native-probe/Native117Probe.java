package viaforge.lab;

import com.google.gson.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.security.ProtectionDomain;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

/** Official 1.17.1: real key input plus passive tick/packet observation. */
public final class Native117Probe {
    private static final Gson GSON=new Gson();
    private static Path directory;
    private static JsonObject names;
    private static final Queue<String> packets=new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static long started;
    private static int ticks,actionTick;
    private static boolean ready,done,connecting;
    private static String action="idle";
    private static String lastScreen="";
    private static final Map<String,Field> fieldCache=new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String,Method> methodCache=new java.util.concurrent.ConcurrentHashMap<>();
    public static void premain(String path,Instrumentation instrumentation)throws Exception {
        directory=Paths.get(path);started=System.currentTimeMillis();
        names=GSON.fromJson(Files.readString(directory.resolve("native-map.json")),JsonObject.class);
        instrumentation.addTransformer(new ClassFileTransformer(){
            @Override public byte[] transform(ClassLoader loader,String owner,Class<?> type,ProtectionDomain domain,byte[] bytes) {
                if(!owner.equals(className("net.minecraft.client.Minecraft").replace('.','/'))&&!owner.equals(className("net.minecraft.client.multiplayer.ClientPacketListener").replace('.','/')))return null;
                ClassReader reader=new ClassReader(bytes);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
                reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
                    @Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions) {
                        MethodVisitor visitor=super.visitMethod(access,name,desc,signature,exceptions);
                        JsonElement mapped=names.getAsJsonObject("hooks").get(owner+"|"+name+desc);
                        if(mapped==null)return visitor;
                        String hook=mapped.getAsString();
                        return new AdviceAdapter(Opcodes.ASM9,visitor,access,name,desc){
                            private int observed;
                            private void hook(boolean end) {
                                loadLocal(observed);
                                if(hook.equals("tick")) {
                                    push(end);
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC,"viaforge/lab/Native117Probe","tick","(Ljava/lang/Object;Z)V",false);
                                } else {
                                    push(hook+(end?"_END":"_BEGIN"));
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC,"viaforge/lab/Native117Probe","packet","(Ljava/lang/Object;Ljava/lang/String;)V",false);
                                }
                            }
                            @Override protected void onMethodEnter(){
                                // Mojang's frames may discard argument locals at a shared
                                // RETURN. Keep the observed reference in a tracked new local.
                                observed=newLocal(org.objectweb.asm.Type.getType(Object.class));
                                if(hook.equals("tick"))loadThis();else loadArg(0);
                                storeLocal(observed);hook(false);
                            }
                            @Override protected void onMethodExit(int opcode){if(opcode==Opcodes.RETURN)hook(true);}
                        };
                    }
                },ClassReader.EXPAND_FRAMES);
                try{Files.writeString(directory.resolve("hooks.txt"),owner+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new IllegalStateException(e);}
                return writer.toByteArray();
            }
        });
    }
    private static String className(String original){JsonElement e=names.getAsJsonObject("classes").get(original);return e==null?original:e.getAsString();}
    private static Field findField(Class<?> c,String name)throws Exception {
        String key=c.getName()+"|"+name;Field cached=fieldCache.get(key);if(cached!=null)return cached;
        for(;c!=null;c=c.getSuperclass()) {
            JsonElement e=names.getAsJsonObject("fields").get(c.getName()+"|"+name);
            if(e!=null){Field f=c.getDeclaredField(e.getAsString());f.setAccessible(true);fieldCache.put(key,f);return f;}
        }
        throw new NoSuchFieldException(name);
    }
    private static Object field(Object o,String name)throws Exception{return findField(o.getClass(),name).get(o);}
    private static boolean accepts(Class<?> type,Object value) {
        if(!type.isPrimitive())return value==null||type.isInstance(value);
        return type==boolean.class&&value instanceof Boolean||type==int.class&&value instanceof Integer
            ||type==float.class&&value instanceof Float||type==double.class&&value instanceof Double
            ||type==long.class&&value instanceof Long||type==byte.class&&value instanceof Byte
            ||type==short.class&&value instanceof Short||type==char.class&&value instanceof Character;
    }
    private static Object call(Object o,String name,Object... args)throws Exception {
        Class<?> owner=o instanceof Class?(Class<?>)o:o.getClass();String key=owner.getName()+"|"+name;
        for(Object a:args)key+="|"+(a==null?"null":a.getClass().getName());
        Method cached=methodCache.get(key);if(cached!=null)return cached.invoke(o instanceof Class?null:o,args);
        Queue<Class<?>> search=new ArrayDeque<>();search.add(owner);Set<Class<?>> seen=new HashSet<>();
        while(!search.isEmpty()) {
            Class<?> c=search.remove();if(!seen.add(c))continue;
            if(c.getSuperclass()!=null)search.add(c.getSuperclass());Collections.addAll(search,c.getInterfaces());
            JsonArray a=names.getAsJsonObject("methods").getAsJsonArray(c.getName()+"|"+name+"|"+args.length);
            if(a==null)continue;
            for(Method m:c.getDeclaredMethods())for(JsonElement n:a)if(m.getName().equals(n.getAsString())&&m.getParameterCount()==args.length) {
                boolean matches=true;Class<?>[] types=m.getParameterTypes();
                for(int i=0;i<args.length;i++)if(!accepts(types[i],args[i]))matches=false;
                if(matches){m.setAccessible(true);methodCache.put(key,m);return m.invoke(o instanceof Class?null:o,args);}
            }
        }
        throw new NoSuchMethodException(o.getClass().getName()+"."+name);
    }
    public static void packet(Object packet,String phase) {
        try {
            if(phase.startsWith("onDisconnect")) {
                Files.writeString(directory.resolve("disconnect.txt"),String.valueOf(packet)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                return;
            }
            String type=null;
            for(String name:new String[]{"ClientboundPingPacket","ServerboundPongPacket","ClientboundSetEntityDataPacket"})
                if(packet.getClass().getName().equals(className("net.minecraft.network.protocol.game."+name)))type=name;
            if(type==null)return;
            Map<String,Object> j=new LinkedHashMap<>();j.put("time_ms",System.currentTimeMillis());j.put("phase",phase);j.put("thread",Thread.currentThread().getName());j.put("packet",type);j.put("id",call(packet,"getId"));
            if(type.equals("ClientboundSetEntityDataPacket")) {
                List<String> values=new ArrayList<>();Object items=call(packet,"getUnpackedData");
                if(items!=null)for(Object item:(Iterable<?>)items)values.add("DataValue[id="+call(call(item,"getAccessor"),"getId")+", value="+call(item,"getValue")+"]");
                j.put("metadata",values.toString());
            }
            packets.add(GSON.toJson(j));
        }catch(Exception e){
            try{java.io.StringWriter text=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(text));Files.writeString(directory.resolve("packet-error.txt"),text.toString());state("FAIL",e.toString());}catch(Exception ignored){}
            throw new IllegalStateException("Original packet observation",e);
        }
    }
    private static void state(String value,String detail)throws Exception {
        Map<String,Object> j=new LinkedHashMap<>();j.put("state",value);j.put("detail",detail);j.put("name",System.getProperty("push.name"));j.put("protocol",756);
        Path tmp=directory.resolve("client-state.tmp");Files.writeString(tmp,GSON.toJson(j));Files.move(tmp,directory.resolve("client-state.json"),StandardCopyOption.REPLACE_EXISTING);
    }
    private static String keys() {
        if(!action.startsWith("sequence:"))return action;
        int tick=actionTick;String last="idle";
        for(String step:action.substring(9).split("\\|")){int p=step.indexOf(':');int count=Integer.parseInt(step.substring(0,p));last=step.substring(p+1);if(tick<count)return last;tick-=count;}
        return last;
    }
    private static Object pos(int x,int y,int z)throws Exception{return Class.forName(className("net.minecraft.core.BlockPos")).getConstructor(int.class,int.class,int.class).newInstance(x,y,z);}
    public static void tick(Object mc,boolean end) {
        if(done)return;
        try {
            if(end&&!packets.isEmpty()){List<String> batch=new ArrayList<>();String line;while((line=packets.poll())!=null)batch.add(line);Files.write(directory.resolve("packet-order.jsonl"),batch,StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
            if(!end) {
                Path p=directory.resolve("action.txt");String next=Files.exists(p)?Files.readString(p).trim():"idle";
                if(next.equals(action))actionTick++;else{action=next;actionTick=0;}
                if(action.equals("stop")){state("DONE","Original sequence recorded");done=true;call(mc,"stop");return;}
                if(System.currentTimeMillis()-started>Long.parseLong(System.getenv().getOrDefault("VIAFORGE_PROBE_TIMEOUT_MS","600000")))throw new IllegalStateException("Original timeout");
            }
            Object player=field(mc,"player"),level=field(mc,"level");
            if(player==null||level==null) {
                Object screen=field(mc,"screen");String detail=String.valueOf(screen);
                if(screen!=null) {
                    detail=screen.getClass().getName()+" "+call(screen,"getTitle");
                    if(screen.getClass().getName().equals(className("net.minecraft.client.gui.screens.DisconnectedScreen")))detail+=" "+field(screen,"reason");
                }
                if(!detail.equals(lastScreen)){Files.writeString(directory.resolve("screens.txt"),detail+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);lastScreen=detail;}
                // Use the normal multiplayer screen only after initial resources
                // loaded. The 1.17 CLI --server path can enter an unbaked world.
                if(!end&&!connecting&&screen!=null&&call(mc,"getOverlay")==null
                        &&screen.getClass().getName().equals(className("net.minecraft.client.gui.screens.TitleScreen"))) {
                    connecting=true;String address="127.0.0.1:"+System.getProperty("probe.port");
                    Object target=call(Class.forName(className("net.minecraft.client.multiplayer.resolver.ServerAddress")),"parseString",address);
                    Object data=Class.forName(className("net.minecraft.client.multiplayer.ServerData")).getConstructor(String.class,String.class,boolean.class).newInstance("Isolated lab",address,false);
                    call(Class.forName(className("net.minecraft.client.gui.screens.ConnectScreen")),"startConnecting",screen,mc,target,data);
                }
                return;
            }
            if(!ready){if(!end)return;ready=true;state("ready","Official Mojang1.17.1; mappings only name observer hooks");}
            String keys=keys();Object options=field(mc,"options");
            if(!end) {
                ticks++;if(keys.contains("close"))call(player,"closeContainer");
                String[] bindings={"keyUp","keyDown","keyLeft","keyRight","keyShift","keyJump","keySprint","keyUse","keyAttack"};
                String[] actions={"forward","back","left","right","sneak","jump","sprint","use","attack"};
                for(int i=0;i<bindings.length;i++) {
                    Object binding=field(options,bindings[i]);boolean down=keys.contains(actions[i]);
                    if(i>=7&&down&&!(Boolean)call(binding,"isDown")){Field count=findField(binding.getClass(),"clickCount");count.setInt(binding,count.getInt(binding)+1);}
                    call(binding,"setDown",down);
                }
            }
            Map<String,Object> j=new LinkedHashMap<>();j.put("time_ms",System.currentTimeMillis());j.put("tick",ticks);j.put("player_tick",field(player,"tickCount"));j.put("phase",end?"END":"START");j.put("action",action);j.put("action_tick",actionTick);j.put("keys",keys);
            double x=(Double)call(player,"getX"),y=(Double)call(player,"getY"),z=(Double)call(player,"getZ");
            j.put("player_x",x);j.put("player_y",y);j.put("player_z",z);j.put("elytra",call(player,"isFallFlying"));
            Object velocity=call(player,"getDeltaMovement");for(String axis:new String[]{"x","y","z"})j.put("player_v"+axis,field(velocity,axis));
            j.put("player_ground",call(player,"isOnGround"));j.put("player_spectator",call(player,"isSpectator"));j.put("player_alive",call(player,"isAlive"));j.put("player_health",call(player,"getHealth"));
            j.put("pose",String.valueOf(call(player,"getPose")));j.put("player_box",String.valueOf(call(player,"getBoundingBox")));j.put("eye_height",call(player,"getEyeHeight"));j.put("height",call(player,"getBbHeight"));
            j.put("swimming",call(player,"isSwimming"));j.put("water",call(player,"isInWater"));j.put("eye_water",call(player,"isUnderWater"));j.put("sprinting",call(player,"isSprinting"));j.put("sneaking",call(player,"isShiftKeyDown"));
            j.put("input_forward",field(player,"zza"));j.put("input_strafe",field(player,"xxa"));Object input=field(player,"input");j.put("raw_forward",field(input,"forwardImpulse"));j.put("raw_strafe",field(input,"leftImpulse"));
            j.put("collision_h",field(player,"horizontalCollision"));j.put("collision_v",field(player,"verticalCollision"));
            Object feet=pos((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(z));String block=String.valueOf(call(level,"getBlockState",feet));
            j.put("bubble",block.contains("bubble_column")?(block.contains("drag=true")?18:17):0);
            j.put("floor",String.valueOf(call(level,"getBlockState",pos((int)Math.floor(x),(int)Math.floor(y-.01),(int)Math.floor(z)))));
            Object screen=field(mc,"screen");
            boolean loading=screen!=null&&screen.getClass().getName().equals(className("net.minecraft.client.gui.screens.ReceivingLevelScreen"));
            j.put("loaded_column",call(level,"hasChunkAt",feet));j.put("loaded",(Boolean)call(level,"hasChunkAt",feet)&&!loading);j.put("paused",call(mc,"isPaused"));
            Object box=call(level,"getBlockEntity",pos(1,64,0));
            if(box!=null&&box.getClass().getName().equals(className("net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity")))j.put("shulker_progress",call(box,"getProgress",1F));
            j.put("nearby",new ArrayList<>());
            Files.writeString(directory.resolve("client.jsonl"),GSON.toJson(j)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }catch(Throwable e){try{state("FAIL",e.toString());e.printStackTrace();done=true;call(mc,"stop");}catch(Exception ignored){}}
    }
    private Native117Probe(){}
}
