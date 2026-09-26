package com.viaversion.viaforge.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Original five 16x16 frames, emitted only for an actual bubble_pop packet. */
public final class ServerBubblePopParticle extends EntityFX {
    private final boolean expireFirst=ServerSession.rule(ClientRule.BUBBLE_EXPIRE_FIRST);
    private int frame;
    public ServerBubblePopParticle(World world,double x,double y,double z,double vx,double vy,double vz) {
        super(world,x,y,z);particleMaxAge=4;motionX=vx;motionY=vy;motionZ=vz;
    }
    @Override public void onUpdate() {
        prevPosX=posX;prevPosY=posY;prevPosZ=posZ;
        if(expireFirst&&particleAge++>=particleMaxAge){setDead();return;}
        motionY-=(double).008F;moveEntity(motionX,motionY,motionZ);
        if(!expireFirst&&particleAge++>=particleMaxAge){setDead();return;}
        int next=expireFirst?particleAge:particleAge*5/4;if(next<=4)frame=next;
    }
    @Override public void renderParticle(WorldRenderer buffer,Entity camera,float partial,float rx,float rz,float ryz,float rxy,float rxz) {
        float u=frame/8F,v=.75F,u1=u+.124875F,v1=v+.124875F,size=.1F*particleScale;
        float x=(float)(prevPosX+(posX-prevPosX)*partial-interpPosX),y=(float)(prevPosY+(posY-prevPosY)*partial-interpPosY),z=(float)(prevPosZ+(posZ-prevPosZ)*partial-interpPosZ);
        int light=getBrightnessForRender(partial),hi=light>>16&65535,lo=light&65535;
        buffer.pos(x-rx*size-rxy*size,y-rz*size,z-ryz*size-rxz*size).tex(u1,v1).color(1F,1F,1F,1F).lightmap(hi,lo).endVertex();
        buffer.pos(x-rx*size+rxy*size,y+rz*size,z-ryz*size+rxz*size).tex(u1,v).color(1F,1F,1F,1F).lightmap(hi,lo).endVertex();
        buffer.pos(x+rx*size+rxy*size,y+rz*size,z+ryz*size+rxz*size).tex(u,v).color(1F,1F,1F,1F).lightmap(hi,lo).endVertex();
        buffer.pos(x+rx*size-rxy*size,y-rz*size,z+ryz*size-rxz*size).tex(u,v1).color(1F,1F,1F,1F).lightmap(hi,lo).endVertex();
    }
}
