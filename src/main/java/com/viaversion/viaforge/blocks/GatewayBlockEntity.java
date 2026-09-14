package com.viaversion.viaforge.blocks;
import com.viaversion.viaforge.compatibility.ServerSession;

import java.util.Random;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;

/** Visual state only: the server owns destinations, teleportation and cooldown events. */
public final class GatewayBlockEntity extends TileEntity implements ITickable {
    private long age;
    private int cooldown;
    private NBTTagCompound snapshot = new NBTTagCompound();
    public long age() { return age; }
    public int cooldown() { return cooldown; }
    public boolean spawning() { return age < 200; }
    public boolean cooling() { return cooldown > 0; }
    public int cooldownLength() { return ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.INTERPOLATED_GATEWAY_COOLDOWN) ? 40 : 20; }
    public float progress(float partial) {
        float fraction = ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.INTERPOLATED_GATEWAY_COOLDOWN) ? partial : 0;
        return spawning() ? MathHelper.clamp_float((age + fraction) / 200F, 0, 1)
                : 1 - MathHelper.clamp_float((cooldown - fraction) / cooldownLength(), 0, 1);
    }
    public void accept(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey("Age", 99)) return;
        snapshot = (NBTTagCompound)tag.copy(); age = tag.getLong("Age");
    }
    @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); accept(tag); }
    @Override public void writeToNBT(NBTTagCompound tag) { tag.merge(snapshot); super.writeToNBT(tag); tag.setLong("Age", age); }
    @Override public void update() { age++; if (cooldown > 0) cooldown--; }
    @Override public boolean receiveClientEvent(int id, int value) {
        if (id != 1) return super.receiveClientEvent(id, value);
        cooldown = cooldownLength(); return true;
    }
    public boolean visible(EnumFacing face) {
        return worldObj != null && getBlockType().shouldSideBeRendered(worldObj, pos.offset(face), face);
    }
    public void particles(Random random) {
        int count = 0; for (EnumFacing face : EnumFacing.values()) if (visible(face)) count++;
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + random.nextFloat(), y = pos.getY() + random.nextFloat(), z = pos.getZ() + random.nextFloat();
            double vx = (random.nextFloat() - .5D) * .5D, vy = (random.nextFloat() - .5D) * .5D, vz = (random.nextFloat() - .5D) * .5D;
            int direction = random.nextInt(2) * 2 - 1;
            if (random.nextBoolean()) { z = pos.getZ() + .5D + .25D * direction; vz = random.nextFloat() * 2F * direction; }
            else { x = pos.getX() + .5D + .25D * direction; vx = random.nextFloat() * 2F * direction; }
            worldObj.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, vx, vy, vz);
        }
    }
    @Override public double getMaxRenderDistanceSquared() { return 65536; }
    @Override public AxisAlignedBB getRenderBoundingBox() { return INFINITE_EXTENT_AABB; }
}
