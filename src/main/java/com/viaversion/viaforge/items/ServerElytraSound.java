package com.viaversion.viaforge.items;

import com.viaversion.viaforge.mobs.ServerMobSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/** The original local elytra loop, backed by the selected client's Mojang sound assets. */
public final class ServerElytraSound extends MovingSound {
    private static ServerElytraSound current;
    private final EntityPlayer player;
    private int time;
    public ServerElytraSound(EntityPlayer player, ResourceLocation sound) {
        super(sound); this.player = player; repeat = true; repeatDelay = 0; volume = .1F;
    }
    public static void clear() {
        if (current != null) {
            current.donePlaying = true;
            Minecraft.getMinecraft().getSoundHandler().stopSound(current);
            current = null;
        }
    }
    public static void tick(EntityPlayer player) {
        if (!ServerElytraFlight.flying(player)) { clear(); return; }
        if (current != null && current.player != player) clear();
        if (current == null) {
            // Early releases without this event stay silent, just like their original client.
            String key = ServerMobSounds.key("item.elytra.flying", 7);
            if (key != null) {
                current = new ServerElytraSound(player, new ResourceLocation(key));
                Minecraft.getMinecraft().getSoundHandler().playSound(current);
            }
        }
    }
    @Override public void update() {
        time++;
        if (player.isDead || player != Minecraft.getMinecraft().thePlayer || player.worldObj != Minecraft.getMinecraft().theWorld
                || time > 20 && !ServerElytraFlight.flying(player)) { donePlaying = true; return; }
        xPosF = (float)player.posX; yPosF = (float)player.posY; zPosF = (float)player.posZ;
        double squared = player.motionX * player.motionX + player.motionY * player.motionY + player.motionZ * player.motionZ;
        volume = squared >= .0001 ? Math.min(1, (float)squared / 4) : 0;
        if (time < 20) volume = 0;
        else if (time < 40) volume *= (time - 20) / 20F;
        pitch = volume > .8F ? 1 + volume - .8F : 1;
    }
}
