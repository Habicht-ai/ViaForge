package viaforge.lab;

import com.google.gson.Gson;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.ProtectionDomain;
import java.util.*;
import org.objectweb.asm.*;

/** Exact official 1.12.2 names. Adds observation/key callbacks to Minecraft ticks only. */
public final class NativeBoatProbe {
    private static Path directory;
    private static long started;
    private static int tick,actionTicks;
    private static String action="idle";
    private static boolean ready,done;
    private static final Gson GSON=new Gson();
    public static void premain(String path,Instrumentation instrumentation) {
        directory=Paths.get(path);started=System.currentTimeMillis();
        instrumentation.addTransformer(new ClassFileTransformer(){
            @Override public byte[] transform(ClassLoader loader,String name,Class<?> type,ProtectionDomain domain,byte[] bytes){
                if(!name.equals("bib"))return null;
                ClassReader reader=new ClassReader(bytes);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
                reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
                    @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                        MethodVisitor method=super.visitMethod(access,name,descriptor,signature,exceptions);
                        if(!name.equals("t")||!descriptor.equals("()V"))return method;
                        return new MethodVisitor(Opcodes.ASM9,method){
                            private void hook(boolean end){visitVarInsn(Opcodes.ALOAD,0);visitInsn(end?Opcodes.ICONST_1:Opcodes.ICONST_0);visitMethodInsn(Opcodes.INVOKESTATIC,"viaforge/lab/NativeBoatProbe","tick","(Ljava/lang/Object;Z)V",false);}
                            @Override public void visitCode(){super.visitCode();hook(false);}
                            @Override public void visitInsn(int opcode){if(opcode==Opcodes.RETURN)hook(true);super.visitInsn(opcode);}
                        };
                    }
                },0);return writer.toByteArray();
            }
        });
    }
    private static Object field(Object o,String name)throws Exception{return o.getClass().getField(name).get(o);}
    private static Object entity(Object o,String name)throws Exception{return Class.forName("vg").getField(name).get(o);}
    private static Object call(Object o,String name)throws Exception{return o.getClass().getMethod(name).invoke(o);}
    private static void state(String status,String detail)throws Exception {
        Map<String,Object> m=new LinkedHashMap<>();m.put("state",status);m.put("detail",detail);
        m.put("name",System.getProperty("boat.name"));m.put("protocol",340);
        Path tmp=directory.resolve("client-state.tmp");Files.write(tmp,GSON.toJson(m).getBytes(StandardCharsets.UTF_8));Files.move(tmp,directory.resolve("client-state.json"),StandardCopyOption.REPLACE_EXISTING);
    }
    private static void key(Object options,String name,boolean down)throws Exception {
        Object key=field(options,name);int code=(Integer)call(key,"j");key.getClass().getMethod("a",int.class,boolean.class).invoke(null,code,down);
    }
    public static void tick(Object mc,boolean end) {
        if(done)return;
        try {
            if(System.currentTimeMillis()-started>600000)throw new IllegalStateException("Native probe timeout");
            Object player=field(mc,"h");if(player==null)return;
            if(!ready){if(field(mc,"m")!=null)return;ready=true;state("ready","Official 1.12.2 client");}
            if(!end){
                tick++;Path path=directory.resolve("action.txt");String next=Files.exists(path)?new String(Files.readAllBytes(path),StandardCharsets.UTF_8).trim():"idle";
                if(!next.equals(action)){action=next;actionTicks=0;}else actionTicks++;
                if(action.equals("stop")){state("DONE","Input sequence recorded");done=true;call(mc,"n");return;}
                Object options=field(mc,"t");
                key(options,"T",action.contains("forward"));key(options,"V",action.contains("back"));
                key(options,"U",action.contains("left"));key(options,"W",action.contains("right"));key(options,"Y",action.equals("dismount"));
                if(action.equals("board")&&actionTicks%10==0&&call(player,"bJ")==null){
                    Object world=field(mc,"f"),nearest=null;double distance=16;
                    for(Object e:(List<?>)field(world,"e"))if(e.getClass().getName().equals("afd")){
                        double dx=(Double)entity(e,"p")-(Double)entity(player,"p"),dy=(Double)entity(e,"q")-(Double)entity(player,"q"),dz=(Double)entity(e,"r")-(Double)entity(player,"r");
                        double d=dx*dx+dy*dy+dz*dz;if(d<distance){nearest=e;distance=d;}
                    }
                    if(nearest!=null){
                        double dx=(Double)entity(nearest,"p")-(Double)entity(player,"p"),dz=(Double)entity(nearest,"r")-(Double)entity(player,"r");
                        double dy=(Double)entity(nearest,"q")+.25-((Double)entity(player,"q")+1.62);
                        Class.forName("vg").getField("v").setFloat(player,(float)(Math.atan2(dz,dx)*180/Math.PI)-90);
                        Class.forName("vg").getField("w").setFloat(player,(float)(-Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))*180/Math.PI));
                        Object renderer=field(mc,"o");renderer.getClass().getMethod("a",float.class).invoke(renderer,1f);
                        Method click=mc.getClass().getDeclaredMethod("aB");click.setAccessible(true);click.invoke(mc);
                    }
                }
            }
            Map<String,Object> j=new LinkedHashMap<>();j.put("time_ms",System.currentTimeMillis());j.put("tick",tick);j.put("phase",end?"END":"START");j.put("action",action);j.put("action_tick",actionTicks);
            j.put("player_x",entity(player,"p"));j.put("player_y",entity(player,"q"));j.put("player_z",entity(player,"r"));
            Object boat=call(player,"bJ");
            if(boat!=null&&boat.getClass().getName().equals("afd")){
                j.put("boat",call(boat,"S"));j.put("driver",call(boat,"bI"));j.put("boat_tick",entity(boat,"T"));
                List<?> passengers=(List<?>)call(boat,"bF");j.put("seat",passengers.indexOf(player));j.put("passenger_count",passengers.size());
                String[] names={"x","y","z","vx","vy","vz","yaw","pitch","ground","collision_h","collision_v"};
                String[] fields={"p","q","r","s","t","u","v","w","z","A","B"};
                for(int i=0;i<names.length;i++)j.put(names[i],entity(boat,fields[i]));
                j.put("box",call(boat,"bw").toString());
                String[] hidden={"g","at","au","aE","aF","aG","aH","aI","aA","aB","aC","aD"};
                String[] labels={"friction","delta_rotation","lerpSteps","waterLevel","land_glide","status","previousStatus","lastYMotion","left","right","forward","back"};
                for(int i=0;i<hidden.length;i++){Field f=boat.getClass().getDeclaredField(hidden[i]);f.setAccessible(true);Object v=f.get(boat);j.put(labels[i],v instanceof Enum?String.valueOf(v):v);}
                Enum<?> status=(Enum<?>)privateField(boat,"aG");
                j.put("status",status==null?"null":new String[]{"WATER","UNDER_WATER","FLOWING_WATER","LAND","AIR"}[status.ordinal()]);
            }
            if(tick<10000)Files.write(directory.resolve("client.jsonl"),(GSON.toJson(j)+"\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }catch(Throwable e){try{state("FAIL",e.toString());e.printStackTrace();done=true;call(mc,"n");}catch(Exception ignored){}}
    }
    private static Object privateField(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
}
