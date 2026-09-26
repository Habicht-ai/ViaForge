package com.viaversion.viaforge.items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Newer clients use a 32-block particle radius, with the usual video settings. */
public final class ServerVisualParticles {
    public static EntityFX spawn(World world, int id, boolean ignoreRange, double x, double y, double z, double vx, double vy, double vz, int... args) {
        Minecraft mc = Minecraft.getMinecraft(); Entity camera = mc.getRenderViewEntity();
        if (mc.theWorld != world || camera == null || mc.effectRenderer == null) return null;
        int setting = mc.gameSettings.particleSetting;
        if (setting == 1 && world.rand.nextInt(3) == 0) setting = 2;
        if (!ignoreRange && (setting > 1 || camera.getDistanceSq(x, y, z) > 1024)) return null;
        if(id>=1045&&id<=1047&&com.viaversion.viaforge.compatibility.ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.BUBBLE_PARTICLES)) {
            EntityFX particle=id==1045?new ServerBubblePopParticle(world,x,y,z,vx,vy,vz):new ServerBubbleParticle(world,id==1046,x,y,z,vx,vy,vz);
            mc.effectRenderer.addEffect(particle);return particle;
        }
        if (id == 42 || id == 43 || id == 48) {
            EntityFX particle = new com.viaversion.viaforge.mobs.MobParticles(world,id,x,y,z,vx,vy,vz);
            mc.effectRenderer.addEffect(particle); return particle;
        }
        if (id == 47 || id == 45) {
            EntityFX particle = id == 47 ? new ServerTotemParticle(world, x, y, z, vx, vy, vz) : new ServerSweepParticle(world, x, y, z, vx);
            mc.effectRenderer.addEffect(particle); return particle;
        }
        return mc.effectRenderer.spawnEffectParticle(id, x, y, z, vx, vy, vz, args);
    }
    private ServerVisualParticles() { }
}
