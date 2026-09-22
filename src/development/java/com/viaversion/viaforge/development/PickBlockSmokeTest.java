package com.viaversion.viaforge.development;

import com.viaversion.viaforge.hands.Offhand;
import com.viaversion.viaforge.mobs.ServerMobSounds;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraft.util.*;
import net.minecraft.world.WorldSettings;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Actual Minecraft pick handler with a full hotbar and an appended offhand. */
final class PickBlockSmokeTest {
    static void nativeBehavior()throws Exception {
        Minecraft mc=Minecraft.getMinecraft();WorldClient oldWorld=mc.theWorld;
        net.minecraft.client.entity.EntityPlayerSP oldPlayer=mc.thePlayer;
        net.minecraft.client.multiplayer.PlayerControllerMP oldController=mc.playerController;
        List<Packet> sent=new ArrayList<>();
        net.minecraft.network.NetworkManager network=new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.CLIENTBOUND){
            @Override public void sendPacket(Packet packet){sent.add(packet);}
        };
        net.minecraft.client.network.NetHandlerPlayClient handler=new net.minecraft.client.network.NetHandlerPlayClient(mc,null,network,new com.mojang.authlib.GameProfile(new UUID(0,991),"PickTest"));
        WorldClient world=new WorldClient(null,new WorldSettings(0,WorldSettings.GameType.CREATIVE,false,false,net.minecraft.world.WorldType.DEFAULT),0,net.minecraft.world.EnumDifficulty.PEACEFUL,new net.minecraft.profiler.Profiler());
        world.doPreChunk(0,0,true);mc.theWorld=world;
        mc.thePlayer=new net.minecraft.client.entity.EntityPlayerSP(mc,world,handler,new net.minecraft.stats.StatFileWriter());
        mc.playerController=new net.minecraft.client.multiplayer.PlayerControllerMP(mc,handler);
        try{verify(world,sent);}finally{Offhand.clear();mc.theWorld=oldWorld;mc.thePlayer=oldPlayer;mc.playerController=oldController;}
    }
    static void verify(WorldClient world,List<Packet> sent)throws Exception {
        Minecraft mc=Minecraft.getMinecraft();
        WorldSettings.GameType mode=mc.playerController.getCurrentGameType();
        ItemStack[] original=mc.thePlayer.inventory.mainInventory.clone();int selected=mc.thePlayer.inventory.currentItem;
        MovingObjectPosition hit=mc.objectMouseOver;
        BlockPos pos=new BlockPos(5,220,5);net.minecraft.block.state.IBlockState old=world.getBlockState(pos);
        Method pick=Minecraft.class.getDeclaredMethod("middleClickMouse");pick.setAccessible(true);
        try {
            Offhand.ensure();mc.playerController.setGameType(WorldSettings.GameType.CREATIVE);
            world.setBlockState(pos,Blocks.dirt.getDefaultState(),3);
            mc.objectMouseOver=new MovingObjectPosition(new Vec3(5.5,221,5.5),EnumFacing.UP,pos);
            for(int slot=0;slot<9;slot++) {
                Arrays.fill(mc.thePlayer.inventory.mainInventory,new ItemStack(Items.stick));
                mc.thePlayer.inventory.currentItem=slot;sent.clear();pick.invoke(mc);
                require(sent.size()==1&&sent.get(0) instanceof C10PacketCreativeInventoryAction,"Pick emits exactly one Creative write");
                C10PacketCreativeInventoryAction p=(C10PacketCreativeInventoryAction)sent.get(0);
                require(p.getSlotId()==36+slot,"Pick preserves main hotbar slot with appended offhand: "+slot+" -> "+p.getSlotId());
                require(p.getStack().getItem()==net.minecraft.item.Item.getItemFromBlock(Blocks.dirt),"Picked dirt identity");
            }
            // Existing matching hotbar stack is selected, not created in a neighbour.
            mc.thePlayer.inventory.currentItem=0;sent.clear();pick.invoke(mc);
            require(mc.thePlayer.inventory.currentItem==8&&((C10PacketCreativeInventoryAction)sent.get(0)).getSlotId()==44,"Pick selects existing last-slot block");
            mc.playerController.setGameType(WorldSettings.GameType.SURVIVAL);sent.clear();pick.invoke(mc);
            require(sent.isEmpty(),"Survival pick does not create a Creative inventory write");
            if(Offhand.active())for(String family:new String[]{"gravel","stone","wood","glass","metal"}) {
                String key=ServerMobSounds.key("block."+family+".place",4);
                require(key!=null,"Target placement event available: "+family);
                net.minecraft.client.audio.SoundEventAccessorComposite sound=mc.getSoundHandler().getSound(new ResourceLocation(key));
                require(sound!=null&&sound.getWeight()>0,"Placement event has recordings: "+family);
                ResourceLocation recording=sound.cloneEntry().getSoundPoolEntryLocation();
                try(java.io.InputStream input=mc.getResourceManager().getResource(recording).getInputStream()) {
                    byte[] header=new byte[4];require(input.read(header)==4&&Arrays.equals(header,new byte[]{'O','g','g','S'}),"Playable target Ogg recording: "+recording);
                }
            }
        }finally {
            world.setBlockState(pos,old,3);mc.objectMouseOver=hit;
            System.arraycopy(original,0,mc.thePlayer.inventory.mainInventory,0,original.length);
            mc.thePlayer.inventory.currentItem=selected;mc.playerController.setGameType(mode);sent.clear();
        }
    }
}
