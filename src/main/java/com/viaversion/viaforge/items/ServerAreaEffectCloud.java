package com.viaversion.viaforge.items;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

/** Particle-only client entity. Radius, waiting phase and removal come from server packets. */
public final class ServerAreaEffectCloud extends Entity {
    public float radius = 3;
    public int color, particle = 15, argument1, argument2;
    public boolean waiting;
    public ServerAreaEffectCloud(World world) { super(world); noClip = true; setSize(6, .5F); }
    public void metadata(int index, Object value) {
        switch (index) {
            case 0:
                if (value instanceof Float && Float.isFinite((Float)value)) {
                    radius = Math.max(0, Math.min(32, (Float)value)); setSize(radius * 2, .5F);
                    setPosition(posX, posY, posZ); // 1.8 setSize keeps the old bounding-box minimum.
                }
                break;
            case 1: if (value instanceof Integer) color = (Integer)value; break;
            case 2: if (value instanceof Boolean) waiting = (Boolean)value; break;
            case 3: if (value instanceof Integer) particle = (Integer)value; break;
            case 4: if (value instanceof Integer) argument1 = (Integer)value; break;
            case 5: if (value instanceof Integer) argument2 = (Integer)value; break;
            default: break;
        }
    }
    @Override public void onUpdate() {
        ticksExisted++;
        if (waiting && rand.nextBoolean()) return;
        EnumParticleTypes effect = EnumParticleTypes.getParticleFromId(particle);
        if (effect == null) effect = EnumParticleTypes.SPELL_MOB;
        int count = waiting ? 2 : (int)Math.ceil(Math.PI * radius * radius);
        int[] args = effect.getArgumentCount() == 0 ? new int[0] : effect.getArgumentCount() == 1 ? new int[]{argument1} : new int[]{argument1, argument2};
        for (int i = 0; i < count; i++) {
            double angle = rand.nextFloat() * Math.PI * 2, distance = Math.sqrt(rand.nextFloat()) * (waiting ? .2 : radius);
            double dx, dy, dz;
            if (effect == EnumParticleTypes.SPELL_MOB) {
                int tint = waiting && rand.nextBoolean() ? 0xffffff : color;
                dx = (tint >> 16 & 255) / 255D; dy = (tint >> 8 & 255) / 255D; dz = (tint & 255) / 255D;
            } else { dx = waiting ? 0 : (.5 - rand.nextDouble()) * .15; dy = waiting ? 0 : .01; dz = waiting ? 0 : (.5 - rand.nextDouble()) * .15; }
            worldObj.spawnParticle(effect, posX + Math.cos(angle) * distance, posY, posZ + Math.sin(angle) * distance, dx, dy, dz, args);
        }
    }
    @Override protected void entityInit() { }
    @Override protected void readEntityFromNBT(NBTTagCompound tag) { }
    @Override protected void writeEntityToNBT(NBTTagCompound tag) { }
}
