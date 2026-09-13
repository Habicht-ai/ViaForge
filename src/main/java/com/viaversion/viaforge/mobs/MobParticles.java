package com.viaversion.viaforge.mobs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public final class MobParticles extends EntityFX {
    public final int type;
    private final float initialScale;
    private boolean touchedGround;
    public MobParticles(World world,int type,double x,double y,double z,double vx,double vy,double vz) {
        super(world,x,y,z); this.type = type; motionX = vx; motionY = vy; motionZ = vz;
        if (type == 43) { particleScale *= .75F; particleMaxAge = 60+rand.nextInt(12); noClip = true; }
        else if (type == 42) {
            particleRed = .7176471F+rand.nextFloat()*(.8745098F-.7176471F); particleGreen = 0; particleBlue = .8235294F+rand.nextFloat()*(.9764706F-.8235294F);
            particleScale *= .75F; particleMaxAge = (int)(20/(rand.nextFloat()*.8+.2));
        } else {
            motionX += (rand.nextDouble()*2-1)*.05; motionY += (rand.nextDouble()*2-1)*.05; motionZ += (rand.nextDouble()*2-1)*.05;
            particleRed = particleGreen = particleBlue = rand.nextFloat()*.3F+.7F;
            particleScale = rand.nextFloat()*rand.nextFloat()*6+1; particleMaxAge = (int)(16/(rand.nextFloat()*.8+.2))+2;
        }
        initialScale = particleScale;
    }
    public static EntityFX spawn(World world,int type,double x,double y,double z,double vx,double vy,double vz) {
        return com.viaversion.viaforge.items.ServerVisualParticles.spawn(world,type,false,x,y,z,vx,vy,vz);
    }
    @Override public void onUpdate() {
        prevPosX = posX; prevPosY = posY; prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) { setDead(); return; }
        if (type == 43) {
            if (particleAge > particleMaxAge/2) {
                particleAlpha = 1-(particleAge-particleMaxAge/2)/(float)particleMaxAge;
                particleRed += (242/255F-particleRed)*.2F; particleGreen += (222/255F-particleGreen)*.2F; particleBlue += (201/255F-particleBlue)*.2F;
            }
            setParticleTextureIndex(176+7-particleAge*8/particleMaxAge); motionY -= .0005F;
            moveEntity(motionX,motionY,motionZ); motionX *= .91F; motionY *= .91F; motionZ *= .91F;
        } else if (type == 42) {
            setParticleTextureIndex(3*particleAge/particleMaxAge+5);
            if (onGround) { motionY = 0; touchedGround = true; }
            if (touchedGround) motionY += .002;
            moveEntity(motionX,motionY,motionZ);
            if (posY == prevPosY) { motionX *= 1.1; motionZ *= 1.1; }
            motionX *= .96F; motionZ *= .96F; if (touchedGround) motionY *= .96F;
        } else {
            setParticleTextureIndex(7-particleAge*8/particleMaxAge); motionY += .004;
            moveEntity(motionX,motionY,motionZ); motionX *= .9F; motionY *= .9F; motionZ *= .9F;
            if (onGround) { motionX *= .7F; motionZ *= .7F; } motionY -= .024;
        }
    }
    @Override public void renderParticle(WorldRenderer renderer,Entity camera,float partial,float x,float z,float yz,float xy,float xz) {
        if (type == 42) particleScale = initialScale*MathHelper.clamp_float((particleAge+partial)/particleMaxAge*32,0,1);
        super.renderParticle(renderer,camera,partial,x,z,yz,xy,xz);
    }
    @Override public int getBrightnessForRender(float partial) { return type == 43 ? 0xf000f0 : super.getBrightnessForRender(partial); }
}
