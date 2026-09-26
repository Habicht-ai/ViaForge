package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.items.ServerBubbleParticle;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Display ticks read the retained column state, never infer it from a substrate. */
public final class ServerBubbleVisuals {
    public static void animate(World world,int cx,int cy,int cz) {
        if(!ServerSession.rule(ClientRule.BUBBLE_PARTICLES)||Minecraft.getMinecraft().theWorld!=world)return;
        Random random=new Random();
        for(int i=0;i<667;i++) {
            sample(world,cx,cy,cz,16,random);
            sample(world,cx,cy,cz,32,random);
        }
    }
    private static void sample(World world,int cx,int cy,int cz,int radius,Random random) {
        int x=cx+world.rand.nextInt(radius)-world.rand.nextInt(radius);
        int y=cy+world.rand.nextInt(radius)-world.rand.nextInt(radius);
        int z=cz+world.rand.nextInt(radius)-world.rand.nextInt(radius);
        int fluid=ServerSwimming.fluids.get(x,y,z);
        if(fluid!=17&&fluid!=18)return;
        boolean down=fluid==18;
        if(down)spawn(world,true,true,false,x+.5,y+.8,z,0,0,0);
        else {
            spawn(world,false,true,false,x+.5,y,z+.5,0,.04,0);
            spawn(world,false,true,false,x+(double)random.nextFloat(),y+(double)random.nextFloat(),z+(double)random.nextFloat(),0,.04,0);
        }
        if(random.nextInt(200)==0) {
            String sound=com.viaversion.viaforge.mobs.ServerMobSounds.key("block.bubble_column."+(down?"whirlpool_ambient":"upwards_ambient"),3);
            if(sound!=null)world.playSound(x,y,z,sound,.2F+random.nextFloat()*.2F,.9F+random.nextFloat()*.15F,false);
        }
    }
    public static void spawn(World world,boolean down,boolean alwaysShow,boolean ignoreRange,double x,double y,double z,double vx,double vy,double vz) {
        Minecraft mc=Minecraft.getMinecraft();Entity camera=mc.getRenderViewEntity();
        if(camera==null||mc.theWorld!=world||!ServerSession.rule(ClientRule.BUBBLE_PARTICLES))return;
        int setting=mc.gameSettings.particleSetting;
        if(alwaysShow&&setting==2&&world.rand.nextInt(10)==0)setting=1;
        if(setting==1&&world.rand.nextInt(3)==0)setting=2;
        if(!ignoreRange&&(setting==2||camera.getDistanceSq(x,y,z)>1024))return;
        mc.effectRenderer.addEffect(new ServerBubbleParticle(world,down,x,y,z,vx,vy,vz));
    }
    private ServerBubbleVisuals(){}
}
