package com.viaversion.viaforge.mixin.impl.connect;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.compatibility.ClientPacketTasks;
import com.viaversion.viaforge.compatibility.ServerSession;
import java.util.Queue;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** 1.13 removed the task-queue monitor from producers; ordinary task order stays intact. */
@Mixin(Minecraft.class)
public abstract class MixinConcurrentClientTasks implements ClientPacketTasks.GeneralTasks {
    @Shadow @Final @Mutable private Queue<FutureTask<?>> scheduledTasks;
    @Shadow public abstract boolean isCallingFromMinecraftThread();

    @Inject(method="<init>", at=@At("RETURN"))
    private void concurrentStorage(CallbackInfo ci) {
        // Old targets still use their original synchronized scheduling path.
        scheduledTasks = new ConcurrentLinkedQueue<>(scheduledTasks);
    }

    @Override public Queue<FutureTask<?>> viaForge$tasks() { return scheduledTasks; }

    @Inject(method="addScheduledTask(Ljava/util/concurrent/Callable;)Lcom/google/common/util/concurrent/ListenableFuture;",
            at=@At("HEAD"), cancellable=true)
    private <V> void concurrentProducer(Callable<V> callable, CallbackInfoReturnable<ListenableFuture<V>> ci) {
        if (isCallingFromMinecraftThread() || !ServerSession.rule(ClientRule.CONCURRENT_CLIENT_TASKS)) return;
        ListenableFutureTask<V> task = ListenableFutureTask.create(callable);
        scheduledTasks.add(task);
        ci.setReturnValue(task);
    }
}
