package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.hands.Offhand;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.*;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldSettings;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Calls the real Q-key player method. No server slot echo is simulated until
 * prediction has been checked; modern servers normally suppress that echo. */
final class DropItemSmokeTest {
    static void nativeBehavior() throws Exception {
        WorldClient world=new WorldClient(null,new WorldSettings(0,WorldSettings.GameType.SURVIVAL,false,false,net.minecraft.world.WorldType.DEFAULT),0,net.minecraft.world.EnumDifficulty.PEACEFUL,new net.minecraft.profiler.Profiler());
        verify(world,false,new ItemStack(net.minecraft.init.Items.iron_sword),new ItemStack(net.minecraft.init.Blocks.stone),null);
    }
    interface Wire {
        void action(C07PacketPlayerDigging packet) throws Exception;
        void correct(NetHandlerPlayClient handler, ItemStack stack, boolean direct) throws Exception;
    }
    static void verify(WorldClient world, boolean predicts, ItemStack shield, ItemStack blocks, Wire wire) throws Exception {
        Minecraft mc=Minecraft.getMinecraft();
        WorldClient oldWorld=mc.theWorld;EntityPlayerSP oldPlayer=mc.thePlayer;PlayerControllerMP oldController=mc.playerController;
        List<Packet> sent=new ArrayList<>();
        NetHandlerPlayClient handler=new NetHandlerPlayClient(mc,null,new NetworkManager(EnumPacketDirection.CLIENTBOUND),new GameProfile(new UUID(0,821),"DropTest")) {
            @Override public void addToSendQueue(Packet packet){sent.add(packet);}
        };
        mc.theWorld=world;mc.thePlayer=new EntityPlayerSP(mc,world,handler,new StatFileWriter());
        mc.playerController=new PlayerControllerMP(mc,handler);
        try {
            Offhand.ensure();mc.thePlayer.inventory.currentItem=4;
            ItemStack other=blocks.copy();mc.thePlayer.inventory.mainInventory[5]=other;
            ItemStack offhand=shield.copy();Offhand.set(offhand);
            for(boolean creative:new boolean[]{false,true})for(boolean all:new boolean[]{false,true})for(ItemStack fixture:new ItemStack[]{shield,blocks}) {
                mc.playerController.setGameType(creative?WorldSettings.GameType.CREATIVE:WorldSettings.GameType.SURVIVAL);
                ItemStack held=fixture.copy();if(fixture==blocks)held.stackSize=5;
                mc.thePlayer.inventory.mainInventory[4]=held;sent.clear();
                mc.thePlayer.dropOneItem(all);
                int remaining=predicts?(all?0:fixture==blocks?4:0):(fixture==blocks?5:1);
                ItemStack actual=mc.thePlayer.getHeldItem();
                require(remaining==0?actual==null:actual!=null&&actual.stackSize==remaining,"Q drop predicts selected inventory exactly once; creative="+creative+", all="+all+", item="+fixture.getDisplayName());
                if(actual!=null)require(ItemStack.areItemStackTagsEqual(actual,fixture),"Remaining stack keeps all NBT/components");
                require(mc.thePlayer.inventory.mainInventory[5]==other&&(!Offhand.active()||Offhand.get()==offhand),"Q never removes adjacent or offhand items");
                require(sent.size()==1&&sent.get(0) instanceof C07PacketPlayerDigging,"Exactly one original drop action, no creative inventory overwrite");
                C07PacketPlayerDigging action=(C07PacketPlayerDigging)sent.get(0);
                require(action.getStatus()==(all?C07PacketPlayerDigging.Action.DROP_ALL_ITEMS:C07PacketPlayerDigging.Action.DROP_ITEM)&&action.getPosition().equals(BlockPos.ORIGIN)&&action.getFacing()==EnumFacing.DOWN,"Original Q/Ctrl-Q action");
                if(wire!=null) {
                    wire.action(action);wire.correct(handler,fixture.copy(),false);
                    ItemStack restored=mc.thePlayer.getHeldItem();
                    require(restored!=null&&restored.getItem()==fixture.getItem()&&restored.stackSize==fixture.stackSize&&restored.getItemDamage()==fixture.getItemDamage(),"Server rejection/correction restores dropped stack");
                    if(fixture==shield)require(ItemStack.areItemStackTagsEqual(restored,fixture),"Server correction restores exact shield data");
                    net.minecraft.inventory.Container otherWindow=new net.minecraft.inventory.Container(){public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player){return true;}};
                    otherWindow.windowId=7;mc.thePlayer.openContainer=otherWindow;
                    wire.correct(handler,null,true);
                    require(mc.thePlayer.getHeldItem()==null&&mc.thePlayer.openContainer==otherWindow,"Direct empty-slot update clears inventory even with another window open");
                    wire.correct(handler,fixture.copy(),true);
                    require(mc.thePlayer.getHeldItem()!=null&&mc.thePlayer.getHeldItem().getItem()==fixture.getItem(),"Direct server correction restores the selected item");
                    mc.thePlayer.openContainer=mc.thePlayer.inventoryContainer;
                    wire.correct(handler,null,false);
                    require(mc.thePlayer.getHeldItem()==null,"Ordinary empty-slot confirmation remains authoritative");
                }
                else mc.thePlayer.inventory.mainInventory[4]=null;
            }
            sent.clear();mc.thePlayer.dropOneItem(false);
            require(mc.thePlayer.getHeldItem()==null&&sent.size()==1,"Empty-hand Q remains safe");
            if(wire!=null)wire.action((C07PacketPlayerDigging)sent.get(0));
        }finally {
            Offhand.clear();mc.theWorld=oldWorld;mc.thePlayer=oldPlayer;mc.playerController=oldController;
        }
    }
    private DropItemSmokeTest(){}
}
