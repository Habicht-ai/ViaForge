package viaforge.lab;

import java.lang.instrument.*;
import java.security.*;
import org.objectweb.asm.*;

/** Diagnostic only: report the exception hidden by old NetworkManager.close's NPE. */
public final class LegacyNettyTrace {
    public static void premain(String hash,Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            public byte[] transform(ClassLoader loader,String name,Class<?> type,ProtectionDomain domain,byte[] bytes) {
                if(!name.equals("net/minecraft/server/v1_8_R3/NetworkManager"))return null;
                try {
                    StringBuilder actual=new StringBuilder();
                    for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))actual.append(String.format("%02x",b));
                    if(!actual.toString().equals(hash))throw new IllegalArgumentException("Unreviewed NetworkManager");
                    ClassWriter writer=new ClassWriter(0);
                    new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9,writer) {
                        public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions) {
                            MethodVisitor original=super.visitMethod(access,method,descriptor,signature,exceptions);
                            if(!method.equals("exceptionCaught")||!descriptor.equals("(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Throwable;)V"))return original;
                            return new MethodVisitor(Opcodes.ASM9,original) {
                                public void visitCode(){super.visitCode();visitVarInsn(Opcodes.ALOAD,2);visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/Throwable","printStackTrace","()V",false);}
                            };
                        }
                    },0);
                    return writer.toByteArray();
                }catch(Exception error){throw new IllegalStateException(error);}
            }
        });
    }
}
