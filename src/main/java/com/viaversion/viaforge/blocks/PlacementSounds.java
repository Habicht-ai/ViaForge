package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.mobs.ServerMobSounds;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.BlockPos;

/** The 1.8 renderer ignores World.playSoundEffect; newer servers exclude the
 * placing player from their sound broadcast and expect local playback. */
public final class PlacementSounds {
    public static void play(WorldClient world,double x,double y,double z,String original,float volume,float pitch) {
        Block block=world.getBlockState(new BlockPos(x,y,z)).getBlock();
        if(!original.equals(block.stepSound.getPlaceSound()))return;
        String type=block.stepSound.soundName;
        if(block.stepSound==Block.soundTypeMetal)type="metal";
        else if(block.stepSound==Block.soundTypeGlass)type="glass";
        else if(block.stepSound==Block.soundTypePiston)type="stone";
        String event="block."+type+".place";
        if(type.equals("cloth")&&ServerMobSounds.has("block.wool.place"))event="block.wool.place";
        if(type.equals("slime")&&ServerMobSounds.has("block.slime_block.place"))event="block.slime_block.place";
        String sound=ServerMobSounds.key(event,4); // BLOCKS category
        world.playSound(x,y,z,sound==null?original:sound,volume,pitch,false);
    }
    private PlacementSounds() { }
}
