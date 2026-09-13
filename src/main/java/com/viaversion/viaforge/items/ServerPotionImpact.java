package com.viaversion.viaforge.items;

import com.google.gson.JsonObject;
import java.util.Random;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;

/** 1.9/1.10 send a PotionType id; 1.11+ send RGB and separate instant event 2007. */
public final class ServerPotionImpact {
    public static int color(int protocol, int data) {
        if (protocol >= 315) return data;
        JsonObject potion = ItemVariants.POTIONS.get(data >= 0 && data < ItemVariants.POTIONS.size() ? data : 0).getAsJsonObject();
        int effect = potion.get("effect").getAsInt();
        return effect > 0 ? Potion.potionTypes[effect].getLiquidColor() : 0x385dc6;
    }
    public static boolean instant(int protocol, int event, int data) {
        if (protocol >= 315) return event == 2007;
        if (data < 0 || data >= ItemVariants.POTIONS.size()) return false;
        int effect = ItemVariants.POTIONS.get(data).getAsJsonObject().get("effect").getAsInt();
        return effect > 0 && Potion.potionTypes[effect].isInstant();
    }
    public static void play(WorldClient world, int protocol, int event, BlockPos pos, int data) {
        Random random = world.rand;
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();
        // Vanilla uses an uncolored splash bottle for the eight glass fragments.
        for (int i = 0; i < 8; i++) ServerVisualParticles.spawn(world, 36, false, x, y, z,
                random.nextGaussian() * .15, random.nextDouble() * .2, random.nextGaussian() * .15,
                ClientItems.localItem(438, 0), 0);
        int color = color(protocol, data);
        EnumParticleTypes type = instant(protocol, event, data) ? EnumParticleTypes.SPELL_INSTANT : EnumParticleTypes.SPELL;
        for (int i = 0; i < 100; i++) {
            double speed = random.nextDouble() * 4, angle = random.nextDouble() * Math.PI * 2;
            double vx = Math.cos(angle) * speed, vy = .01 + random.nextDouble() * .5, vz = Math.sin(angle) * speed;
            EntityFX particle = ServerVisualParticles.spawn(world, type.getParticleID(), false, x + vx * .1, y + .3, z + vz * .1, vx, vy, vz);
            if (particle == null) continue;
            float shade = .75F + random.nextFloat() * .25F;
            particle.setRBGColorF((color >> 16 & 255) / 255F * shade, (color >> 8 & 255) / 255F * shade, (color & 255) / 255F * shade);
            particle.multiplyVelocity((float)speed);
        }
        world.playSoundAtPos(pos, "game.potion.smash", 1, random.nextFloat() * .1F + .9F, false);
    }
    private ServerPotionImpact() { }
}
