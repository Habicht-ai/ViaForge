package com.viaversion.viaforge.items;

import io.netty.buffer.ByteBuf;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;

/** Original particle packet semantics, without Via's replacement type or duplicate effects. */
public final class ServerParticlePackets {
    private static final Random RANDOM = new Random();
    public static void accept(WorldClient world, ByteBuf input) {
        int type = input.readInt();
        if (type != 45 && type != 47 && type != 42 && type != 43 && type != 48 && (type<1045||type>1047)) return;
        boolean longDistance = input.readBoolean();
        double x = input.readFloat(), y = input.readFloat(), z = input.readFloat();
        double dx = input.readFloat(), dy = input.readFloat(), dz = input.readFloat(), speed = input.readFloat();
        int count = input.readInt();
        if (count == 0) spawn(world, type, longDistance, x, y, z, dx * speed, dy * speed, dz * speed);
        else for (int i = 0; i < count; i++) spawn(world, type, longDistance,
                x + RANDOM.nextGaussian() * dx, y + RANDOM.nextGaussian() * dy, z + RANDOM.nextGaussian() * dz,
                RANDOM.nextGaussian() * speed, RANDOM.nextGaussian() * speed, RANDOM.nextGaussian() * speed);
    }
    private static void spawn(WorldClient world, int type, boolean longDistance, double x, double y, double z, double vx, double vy, double vz) {
        Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
        if (camera != null && camera.getDistanceSq(x, y, z) <= (longDistance ? 65536 : 1024)) ServerVisualParticles.spawn(world, type, longDistance, x, y, z, vx, vy, vz);
    }
    private ServerParticlePackets() { }
}
