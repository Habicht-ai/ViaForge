package com.viaversion.viaforge.development;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
@IFMLLoadingPlugin.MCVersion("1.8.9")
public final class MovementProbeLoader extends com.viaversion.viaforge.mixin.MixinLoader {
    public MovementProbeLoader(){
        if(System.getenv("VIAFORGE_SNEAK_PROBE")!=null)org.spongepowered.asm.mixin.Mixins.addConfiguration("mixins.movement-probe.json");
    }
}
