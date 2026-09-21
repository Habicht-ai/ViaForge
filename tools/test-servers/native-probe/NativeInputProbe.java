package viaforge.lab;

import java.lang.instrument.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.security.ProtectionDomain;
import java.util.Locale;
import org.objectweb.asm.*;

/** Original 26.2 client with key inputs only: no movement/physics/network replacement. */
public final class NativeInputProbe {
    private static Path directory;
    private static long started, tick;
    private static String previous="";
    private static int actionTicks;
    public static void premain(String path, Instrumentation instrumentation) {
        directory=Paths.get(path);started=System.currentTimeMillis();
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader,String name,Class<?> type,ProtectionDomain domain,byte[] bytes) {
                if(!name.equals("net/minecraft/client/Minecraft"))return null;
                ClassReader reader=new ClassReader(bytes);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
                reader.accept(new ClassVisitor(Opcodes.ASM9,writer) {
                    @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                        MethodVisitor method=super.visitMethod(access,name,descriptor,signature,exceptions);
                        if(!name.equals("tick")||!descriptor.equals("()V"))return method;
                        return new MethodVisitor(Opcodes.ASM9,method) {
                            @Override public void visitCode() {
                                super.visitCode();visitVarInsn(Opcodes.ALOAD,0);
                                visitMethodInsn(Opcodes.INVOKESTATIC,"viaforge/lab/NativeInputProbe","tick","(Ljava/lang/Object;)V",false);
                            }
                        };
                    }
                },0);
                return writer.toByteArray();
            }
        });
    }
    private static Object field(Object object,String name)throws Exception{return object.getClass().getField(name).get(object);}
    private static Object call(Object object,String name)throws Exception{return object.getClass().getMethod(name).invoke(object);}
    private static void key(Object options,String name,boolean pressed)throws Exception {
        Object key=field(options,name);key.getClass().getMethod("setDown",boolean.class).invoke(key,pressed);
    }
    public static void tick(Object mc) {
        try {
            if(System.currentTimeMillis()-started>180000){call(mc,"stop");return;}
            Object player=field(mc,"player");if(player==null)return;
            String action=Files.exists(directory.resolve("action.txt"))?Files.readString(directory.resolve("action.txt")).trim():"idle";
            if(!action.equals(previous)){previous=action;actionTicks=0;}actionTicks++;tick++;
            Object options=field(mc,"options");
            key(options,"keyUp",action.equals("walk")||action.equals("sprint")||action.equals("jump")||action.equals("sneak"));
            key(options,"keySprint",action.equals("sprint")||action.equals("jump"));
            key(options,"keyShift",action.equals("sneak"));
            key(options,"keyJump",action.equals("jump")||(action.equals("glide")&&actionTicks<3));
            if(action.equals("rocket")&&actionTicks==1) {
                Method use=mc.getClass().getDeclaredMethod("startUseItem");use.setAccessible(true);use.invoke(mc);
            }
            String line=String.format(Locale.ROOT,"%d;%d;%s;%.9f;%.9f;%.9f;%s;%s;%s;%s%n",System.currentTimeMillis(),tick,action,
                call(player,"getX"),call(player,"getY"),call(player,"getZ"),call(player,"isFallFlying"),call(player,"onGround"),
                call(player,"isSprinting"),call(player,"isInWater"));
            Files.writeString(directory.resolve("positions.csv"),line,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            if(action.equals("stop"))call(mc,"stop");
        }catch(Throwable failure){
            try{Files.writeString(directory.resolve("error.txt"),failure.toString());call(mc,"stop");}catch(Exception ignored){}
        }
    }
}
