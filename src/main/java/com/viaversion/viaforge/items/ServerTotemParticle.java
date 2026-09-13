package com.viaversion.viaforge.items;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

/** Vanilla ParticleTotem + ParticleSimpleAnimated (1.11/1.12), using the target particle atlas. */
public final class ServerTotemParticle extends EntityFX {
    public ServerTotemParticle(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z);
        motionX = vx; motionY = vy; motionZ = vz;
        particleScale *= .75F; particleMaxAge = 60 + rand.nextInt(12);
        if (rand.nextInt(4) == 0) setRBGColorF(.6F + rand.nextFloat() * .2F, .6F + rand.nextFloat() * .3F, rand.nextFloat() * .2F);
        else setRBGColorF(.1F + rand.nextFloat() * .2F, .4F + rand.nextFloat() * .3F, rand.nextFloat() * .2F);
    }
    @Override public void onUpdate() {
        prevPosX = posX; prevPosY = posY; prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) setDead();
        if (particleAge > particleMaxAge / 2) particleAlpha = 1F - (particleAge - particleMaxAge / 2) / (float)particleMaxAge;
        setParticleTextureIndex(176 + 7 - particleAge * 8 / particleMaxAge);
        motionY += (double)-.05F;
        moveEntity(motionX, motionY, motionZ);
        motionX *= (double).6F; motionY *= (double).6F; motionZ *= (double).6F;
        if (onGround) { motionX *= (double).7F; motionZ *= (double).7F; }
    }
    @Override public int getBrightnessForRender(float partialTicks) { return 0xf000f0; }
}
