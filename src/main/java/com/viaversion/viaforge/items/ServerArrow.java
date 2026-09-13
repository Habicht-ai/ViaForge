package com.viaversion.viaforge.items;

import com.viaversion.viaforge.mixin.impl.items.ArrowStateAccess;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.world.World;

/** Original 1.9-1.12 arrow color metadata, textures and client particle cadence. */
public final class ServerArrow extends EntityArrow {
    public final boolean spectral;
    public int color = -1;
    public ServerArrow(World world, boolean spectral) { super(world); this.spectral = spectral; }
    @Override public void onUpdate() {
        super.onUpdate();
        if (!worldObj.isRemote) return;
        ArrowStateAccess state = (ArrowStateAccess)(Object)this;
        if (spectral) {
            if (!state.viaForge$inGround()) ServerVisualParticles.spawn(worldObj, 14, false, posX, posY, posZ, 0, 0, 0);
        } else if (!state.viaForge$inGround()) particles(2);
        else if (state.viaForge$ticksInGround() % 5 == 0) particles(1);
    }
    private void particles(int count) {
        if (color == -1) return;
        for (int i = 0; i < count; i++) ServerVisualParticles.spawn(worldObj, 15, false,
                posX + (rand.nextDouble() - .5) * width, posY + rand.nextDouble() * height, posZ + (rand.nextDouble() - .5) * width,
                (color >> 16 & 255) / 255D, (color >> 8 & 255) / 255D, (color & 255) / 255D);
    }
    @Override public void handleStatusUpdate(byte status) {
        if (!spectral && status == 0) particles(20);
        else super.handleStatusUpdate(status);
    }
}
