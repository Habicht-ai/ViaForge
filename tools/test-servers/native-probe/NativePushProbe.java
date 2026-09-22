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
    private static int ticks;
    private static boolean ready,done;
    private static String action="idle";
    private static final Gson GSON=new Gson();
    public static void premain(String path,Instrumentation instrumentation) {
        directory=Paths.get(path);started=System.currentTimeMillis();
        instrumentation.addTransformer(new ClassFileTransformer(){
            @Override public byte[] transform(ClassLoader loader,String name,Class<?> type,ProtectionDomain domain,byte[] bytes){
                if(!name.equals("net/minecraft/client/Minecraft"))return null;
                ClassReader reader=new ClassReader(bytes);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
                reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
                    @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                        MethodVisitor method=super.visitMethod(access,name,descriptor,signature,exceptions);
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
            Path path=directory.resolve("action.txt");
            if(!end){
                action=Files.exists(path)?Files.readString(path).trim():"idle";
                if(action.equals("stop")){state("DONE","Input sequence recorded");done=true;call(mc,"stop");return;}
                if(System.currentTimeMillis()-started>600000)throw new IllegalStateException("Native probe timeout");
            }
            Object player=field(mc,"player"),level=field(mc,"level");if(player==null||level==null)return;
            if(!ready){if(!end)return;ready=true;state("ready","Official Mojang 26.2");}
            if(!end){
                ticks++;Object options=field(mc,"options");
                key(options,"keyUp",action.contains("forward"));key(options,"keyDown",action.contains("back"));
                key(options,"keyLeft",action.contains("left"));key(options,"keyRight",action.contains("right"));
                key(options,"keyShift",action.contains("sneak"));key(options,"keyJump",action.contains("jump"));key(options,"keySprint",action.contains("sprint"));
            }
            Map<String,Object> j=new LinkedHashMap<>();j.put("time_ms",System.currentTimeMillis());j.put("tick",ticks);j.put("phase",end?"END":"START");j.put("action",action);
            double x=(Double)call(player,"getX"),y=(Double)call(player,"getY"),z=(Double)call(player,"getZ");
            j.put("player_x",x);j.put("player_y",y);j.put("player_z",z);
            Object velocity=call(player,"getDeltaMovement");j.put("player_vx",field(velocity,"x"));j.put("player_vy",field(velocity,"y"));j.put("player_vz",field(velocity,"z"));
            j.put("player_ground",call(player,"onGround"));j.put("player_spectator",call(player,"isSpectator"));
            j.put("player_alive",call(player,"isAlive"));j.put("player_health",call(player,"getHealth"));
            if("1".equals(System.getenv("VIAFORGE_SWIM_PROBE"))) {
                j.put("swimming",call(player,"isSwimming"));j.put("height",call(player,"getBbHeight"));
                j.put("water",call(player,"isInWater"));j.put("eye_water",call(player,"isUnderWater"));
                j.put("sprint",call(player,"isSprinting"));j.put("yaw",call(player,"getYRot"));j.put("pitch",call(player,"getXRot"));
                Class<?> effects=Class.forName("net.minecraft.world.effect.MobEffects");Object dolphin=effects.getField("DOLPHINS_GRACE").get(null);
                j.put("dolphins_grace",player.getClass().getMethod("hasEffect",Class.forName("net.minecraft.core.Holder")).invoke(player,dolphin));
                j.put("conduit_power",player.getClass().getMethod("hasEffect",Class.forName("net.minecraft.core.Holder")).invoke(player,effects.getField("CONDUIT_POWER").get(null)));
                j.put("air",call(player,"getAirSupply"));
                Object efficiency=Class.forName("net.minecraft.world.entity.ai.attributes.Attributes").getField("WATER_MOVEMENT_EFFICIENCY").get(null);
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
