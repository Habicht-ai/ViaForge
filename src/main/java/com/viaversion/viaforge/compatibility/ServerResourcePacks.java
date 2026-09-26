package com.viaversion.viaforge.compatibility;

import com.google.common.util.concurrent.*;
import com.viaversion.viaforge.common.compatibility.ServerPackDownload;
import java.io.File;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePackRepository;

/** Repository-owned request generation; downloaded bytes alone are not a loaded pack. */
public final class ServerResourcePacks {
    private static final ExecutorService DOWNLOADS=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"ViaForge server pack");t.setDaemon(true);return t;});
    private long generation;
    private Future<?> worker;
    private SettableFuture<Object> pending;

    public void cancel() {
        generation++;
        if(worker!=null)worker.cancel(true);worker=null;
        if(pending!=null)pending.cancel(false);pending=null;
    }
    public ListenableFuture<Object> download(ResourcePackRepository repository,File directory,String url,String hash) {
        Minecraft mc=Minecraft.getMinecraft();SettableFuture<Object> result=SettableFuture.create();
        mc.addScheduledTask(()->{
            repository.clearResourcePack();
            long ticket=generation;pending=result;
            // Capture connection ownership before dispatching any background work.
            Object handler=mc.getNetHandler();
            worker=DOWNLOADS.submit(()->{
                try {
                    File file=ServerPackDownload.fetch(directory.toPath(),url,hash,mc.getProxy(),Minecraft.getSessionInfo()).toFile();
                    mc.addScheduledTask(()->{
                        if(ticket!=generation||result.isCancelled()||handler!=mc.getNetHandler()){result.cancel(false);return;}
                        try {
                            ServerPackReload.install(new net.minecraft.client.resources.FileResourcePack(file));
                            if(ticket==generation&&handler==mc.getNetHandler())result.set(null);else result.cancel(false);
                        }catch(Exception failure){result.setException(failure);}
                    });
                } catch(Exception failure){mc.addScheduledTask(()->{if(ticket==generation)result.setException(failure);else result.cancel(false);});}
            });
        });
        return result;
    }
}
