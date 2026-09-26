package com.viaversion.viaforge.items;

import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaforge.compatibility.*;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.*;
import net.minecraft.world.World;

/** Original column particles; their motion never enters the player fluid simulation. */
public final class ServerBubbleParticle extends EntityFX {
    public final boolean downward;
    private final boolean expireFirst, groundFriction, floatRandom, floatAngle, doubleTrig;
    private float angle;
    private boolean stopped;

    public ServerBubbleParticle(World world,boolean downward,double x,double y,double z,double vx,double vy,double vz) {
        super(world,x,y,z);
        this.downward=downward;
        expireFirst=ServerSession.rule(ClientRule.BUBBLE_EXPIRE_FIRST);
        groundFriction=ServerSession.rule(ClientRule.BUBBLE_BASE_TICK);
        floatRandom=ServerSession.rule(ClientRule.BUBBLE_FLOAT_RANDOM);
        floatAngle=ServerSession.rule(ClientRule.BUBBLE_FLOAT_ANGLE);
        doubleTrig=ServerSession.rule(ClientRule.DOUBLE_TRIG_LOOKUP);
        setParticleTextureIndex(32);setSize(.02F,.02F);setPosition(x,y,z);
        particleScale*=rand.nextFloat()*.6F+.2F;
        if(downward) {
            particleMaxAge=(int)(floatRandom?rand.nextFloat()*60F:Math.random()*60)+30;
            noClip=true;motionX=motionZ=0;motionY=-.05;
        }else {
            motionX=vx*(double).2F+jitter();motionY=vy*(double).2F+jitter();motionZ=vz*(double).2F+jitter();
            particleMaxAge=(int)(40/((floatRandom?rand.nextFloat():Math.random())*.8+.2));
        }
    }
    private double jitter(){return floatRandom?(double)((rand.nextFloat()*2F-1F)*.02F):(Math.random()*2-1)*(double).02F;}

    @Override public void onUpdate() {
        prevPosX=posX;prevPosY=posY;prevPosZ=posZ;
        if(expireFirst&&particleAge++>=particleMaxAge){setDead();return;}
        if(downward) {
            motionX+=(double)(.6F*OriginalLookMath.cos(angle,doubleTrig));
            motionZ+=(double)(.6F*OriginalLookMath.sin(angle,doubleTrig));
            motionX*=.07;motionZ*=.07;
        }else motionY+=.005;
        moveEntity(motionX,motionY,motionZ);
        if(!downward) {
            motionX*=(double).85F;motionY*=(double).85F;motionZ*=(double).85F;
            if(groundFriction&&onGround){motionX*=(double).7F;motionZ*=(double).7F;}
        }
        if(ServerSwimming.waterLevel(worldObj,new BlockPos(posX,posY,posZ))<0||downward&&onGround)setDead();
        if(!expireFirst&&particleAge++>=particleMaxAge)setDead();
        if(downward)angle=floatAngle?angle+.08F:(float)((double)angle+.08);
        // BubbleColumnUp disappears on leaving water. It does not emit BubblePop.
    }

    @Override public void moveEntity(double x,double y,double z) {
        if(stopped)return;
        double originalX=x,originalY=y,originalZ=z;
        AxisAlignedBB box=getEntityBoundingBox();
        if(!noClip) {
            java.util.List<AxisAlignedBB> collisions=worldObj.getCollidingBoundingBoxes(this,box.addCoord(x,y,z));
            for(AxisAlignedBB other:collisions)y=other.calculateYOffset(box,y);box=box.offset(0,y,0);
            boolean zFirst=expireFirst&&Math.abs(x)<Math.abs(z);
            if(zFirst){for(AxisAlignedBB other:collisions)z=other.calculateZOffset(box,z);box=box.offset(0,0,z);}
            for(AxisAlignedBB other:collisions)x=other.calculateXOffset(box,x);box=box.offset(x,0,0);
            if(!zFirst){for(AxisAlignedBB other:collisions)z=other.calculateZOffset(box,z);box=box.offset(0,0,z);}
        }else box=box.offset(x,y,z);
        setEntityBoundingBox(box);posX=(box.minX+box.maxX)/2;posY=box.minY;posZ=(box.minZ+box.maxZ)/2;
        stopped=Math.abs(originalY)>=(double)1.0E-5F&&Math.abs(y)<(double)1.0E-5F;
        onGround=originalY!=y&&originalY<0;
        if(originalX!=x)motionX=0;if(originalZ!=z)motionZ=0;
    }
}
