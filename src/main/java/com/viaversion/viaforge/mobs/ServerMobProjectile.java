package com.viaversion.viaforge.mobs;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;

/** Client movement of mob projectiles; hit decisions and removal still come from the server. */
public final class ServerMobProjectile extends Entity {
    public final int type;
    public int fangLife = 22;
    public boolean fangsStarted;
    public double accelerationX, accelerationY, accelerationZ;
    private boolean noGravity;
    public ServerMobProjectile(World world, int type) {
        super(world); this.type = type;
        if (type == 67) { setSize(.3125F,.3125F); noClip = true; }
        else if (type == 68) setSize(.25F,.25F);
        else if (type == 79) setSize(.5F,.8F);
        else setSize(1,1);
    }
    @Override protected void entityInit() { }
    @Override protected void readEntityFromNBT(NBTTagCompound nbt) { }
    @Override protected void writeEntityToNBT(NBTTagCompound nbt) { }
    public void applyMetadata(java.util.List<com.viaversion.viaversion.api.minecraft.entitydata.EntityData> entries) {
        // Via names its substitute witch "Shulker Bullet". Only the original
        // server metadata may supply a name or visibility flag for this entity.
        for (com.viaversion.viaversion.api.minecraft.entitydata.EntityData entry : entries) {
            Object value = entry.getValue();
            switch (entry.id()) {
                case 0: if (value instanceof Byte) dataWatcher.updateObject(0, value); break;
                case 2: if (value instanceof String) setCustomNameTag((String)value); break;
                case 3: if (value instanceof Boolean) setAlwaysRenderNameTag((Boolean)value); break;
                case 4: if (value instanceof Boolean) setSilent((Boolean)value); break;
                case 5: if (value instanceof Boolean) noGravity = (Boolean)value; break;
                default: break;
            }
        }
    }
    @Override public boolean canBeCollidedWith() { return type == 67; }
    @Override public int getBrightnessForRender(float partial) { return type == 67 || type == 93 ? 15728880 : super.getBrightnessForRender(partial); }
    @Override public boolean isInRangeToRenderDist(double distance) { return type == 67 ? distance < 16384 : super.isInRangeToRenderDist(distance); }
    public float fangProgress(float partial) { return !fangsStarted ? 0 : fangLife <= 2 ? 1 : 1 - (fangLife-2-partial)/20F; }
    @Override public void handleStatusUpdate(byte status) {
        if (type == 79 && status == 4 && !fangsStarted) {
            fangsStarted = true;
            String sound = ServerMobSounds.key("entity.evocation_fangs.attack",5);
            if (sound != null && !isSilent()) worldObj.playSound(posX,posY,posZ,sound,1,rand.nextFloat()*.2F+.85F,false);
        } else super.handleStatusUpdate(status);
    }
    @Override public void onUpdate() {
        super.onUpdate();
        if (type == 79) {
            if (fangsStarted && --fangLife == 14) for (int i=0;i<12;i++) worldObj.spawnParticle(EnumParticleTypes.CRIT,
                    posX+(rand.nextDouble()*2-1)*width*.5,posY+1.05+rand.nextDouble(),posZ+(rand.nextDouble()*2-1)*width*.5,
                    (rand.nextDouble()*2-1)*.3,.3+rand.nextDouble()*.3,(rand.nextDouble()*2-1)*.3);
            return;
        }
        setPosition(posX+motionX,posY+motionY,posZ+motionZ);
        float yaw = (float)(Math.atan2(motionX,motionZ)*180/Math.PI), pitch = (float)(Math.atan2(motionY,Math.sqrt(motionX*motionX+motionZ*motionZ))*180/Math.PI);
        rotationYaw = prevRotationYaw + MathHelper.wrapAngleTo180_float(yaw-prevRotationYaw)*(type == 67 ? .5F : .2F);
        rotationPitch = prevRotationPitch + MathHelper.wrapAngleTo180_float(pitch-prevRotationPitch)*(type == 67 ? .5F : .2F);
        if (type == 67) MobParticles.spawn(worldObj,43,posX-motionX,posY-motionY+.15,posZ-motionZ,0,0,0);
        else if (type == 68) { motionX *= .99F; motionY = motionY*.99F - (noGravity ? 0 : .06F); motionZ *= .99F; }
        else if (type == 93) {
            motionX = (motionX+accelerationX)*.95F; motionY = (motionY+accelerationY)*.95F; motionZ = (motionZ+accelerationZ)*.95F;
            MobParticles.spawn(worldObj,42,posX,posY+.5,posZ,0,0,0);
        }
    }
}
