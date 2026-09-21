package com.viaversion.viaforge.mixin.impl.connect;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(NetHandlerPlayClient.class)
public interface ClientTerrainReady {
    @Accessor("doneLoadingTerrain") boolean viaForge$terrainReady();
}
