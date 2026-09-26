package com.viaversion.viaforge.mixin.impl.connect;

import com.google.common.util.concurrent.ListenableFuture;
import com.viaversion.viaforge.compatibility.ServerResourcePacks;
import java.io.File;
import net.minecraft.client.resources.ResourcePackRepository;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ResourcePackRepository.class)
public abstract class MixinServerResourcePacks {
    @Shadow @Final private File dirServerResourcepacks;
    @Unique private final ServerResourcePacks viaForge$downloads=new ServerResourcePacks();
    @Inject(method="downloadResourcePack",at=@At("HEAD"),cancellable=true)
    private void download(String url,String hash,CallbackInfoReturnable<ListenableFuture<Object>> ci) {
        ci.setReturnValue(viaForge$downloads.download((ResourcePackRepository)(Object)this,dirServerResourcepacks,url,hash));
    }
    @Inject(method="clearResourcePack",at=@At("HEAD"))
    private void invalidate(CallbackInfo ci){viaForge$downloads.cancel();com.viaversion.viaforge.compatibility.ModernServerPacks.clear();}
}
