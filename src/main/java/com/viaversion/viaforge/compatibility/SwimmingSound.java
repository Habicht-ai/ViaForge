package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.mobs.ServerMobSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/** Vanilla underwater entry/exit and the forty-tick ambient fade. */
public final class SwimmingSound extends MovingSound {
    private static SwimmingSound current;
    private static EntityPlayer owner;
    private static boolean underwater;
    private final EntityPlayer player;
    private int fade;
    private SwimmingSound(EntityPlayer player,String sound) {
        super(new ResourceLocation(sound));this.player=player;repeat=true;repeatDelay=0;volume=1;
        attenuationType=ISound.AttenuationType.NONE;
    }
    public static void clear() {
        if(current!=null){current.donePlaying=true;Minecraft.getMinecraft().getSoundHandler().stopSound(current);}
        current=null;owner=null;underwater=false;
    }
    public static void tick(EntityPlayer player) {
        if(!ServerSwimming.enabled(player))return;
        if(owner!=player){clear();owner=player;}
        boolean next=ServerSwimming.eye(player)&&player.isInWater();
        if(next!=underwater&&!player.isSpectator()) {
            String key=ServerMobSounds.key(next?"ambient.underwater.enter":"ambient.underwater.exit",8);
            if(key!=null)player.playSound(key,1,1);
            if(next){
                if(current!=null)Minecraft.getMinecraft().getSoundHandler().stopSound(current);
                key=ServerMobSounds.key("ambient.underwater.loop",8);
                if(key!=null){current=new SwimmingSound(player,key);Minecraft.getMinecraft().getSoundHandler().playSound(current);}
            }
        }
        underwater=next;
    }
    @Override public void update() {
        if(player.isDead||player!=Minecraft.getMinecraft().thePlayer||fade<0){donePlaying=true;return;}
        fade=Math.min(40,fade+(ServerSwimming.eye(player)&&player.isInWater()?1:-2));
        volume=Math.max(0,Math.min(fade/40F,1));
        xPosF=(float)player.posX;yPosF=(float)player.posY;zPosF=(float)player.posZ;
    }
}
