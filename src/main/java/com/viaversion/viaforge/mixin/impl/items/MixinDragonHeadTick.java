package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.items.DragonHeadAnimation;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ITickable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TileEntitySkull.class)
public abstract class MixinDragonHeadTick implements ITickable, DragonHeadAnimation {
    @Unique private int viaForge$jawTicks;
    @Unique private boolean viaForge$powered;
    @Override public void update() {
        TileEntitySkull skull = (TileEntitySkull)(Object)this;
        viaForge$powered = ServerBlockSession.supportsProtocol(107) && skull.getSkullType() == 5
                && skull.getWorld() != null && skull.getWorld().isBlockPowered(skull.getPos());
        if (viaForge$powered) viaForge$jawTicks++;
    }
    @Override public float viaForge$jawTime(float partialTicks) {
        // Removing power freezes the current pose; it does not snap the jaw shut.
        return viaForge$jawTicks + (viaForge$powered ? partialTicks : 0);
    }
}
