package com.viaversion.viaforge.blocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ITickable;

/** Client lid animation driven exclusively by server block events. */
public final class ShulkerBlockEntity extends TileEntity implements ITickable {
    private float previous, progress;
    private boolean open;
    @Override public boolean receiveClientEvent(int id, int value) {
        if (id != 1) return super.receiveClientEvent(id, value);
        open = value > 0;
        return true;
    }
    @Override public void update() {
        previous = progress;
        progress = Math.max(0, Math.min(1, progress + (open ? .1F : -.1F)));
    }
    public float progress(float partialTicks) { return previous + (progress - previous) * partialTicks; }
    @Override public AxisAlignedBB getRenderBoundingBox() { return new AxisAlignedBB(getPos().add(-1, -1, -1), getPos().add(2, 2, 2)); }
}
