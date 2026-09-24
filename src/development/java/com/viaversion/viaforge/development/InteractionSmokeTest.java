package com.viaversion.viaforge.development;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.common.blocks.ComponentItemSnapshot;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

final class InteractionSmokeTest {
    static void verify(WorldClient world,EntityPlayerSP player,java.util.List<net.minecraft.network.Packet> sent)throws Exception {
        BlockPos pos=new BlockPos(5,200,5);IBlockState before=world.getBlockState(pos);
        try {
            for(EnumFacing facing:EnumFacing.Plane.HORIZONTAL) {
                IBlockState ladder=Blocks.ladder.getDefaultState().withProperty(BlockLadder.FACING,facing);
                world.setBlockState(pos,ladder,3);
                AxisAlignedBB box=Blocks.ladder.getCollisionBoundingBox(world,pos,ladder);
                double width=facing.getAxis()==EnumFacing.Axis.Z?box.maxZ-box.minZ:box.maxX-box.minX;
                require(width==(ServerSession.rule(ClientRule.MODERN_THIN_COLLISIONS)?.1875:.125),"Original ladder thickness for "+facing);
            }
            AxisAlignedBB lily=Blocks.waterlily.getCollisionBoundingBox(world,pos,Blocks.waterlily.getDefaultState());
            require(lily.maxY-lily.minY==(ServerSession.rule(ClientRule.MODERN_THIN_COLLISIONS)?.09375:.015625),"Original lily collision height");
            world.setBlockState(pos,Blocks.waterlily.getDefaultState(),3);
            AxisAlignedBB outline=Blocks.waterlily.getSelectedBoundingBox(world,pos);
            require(outline.maxY==lily.maxY&&outline.minX==lily.minX,"Lily outline matches collision");
            MovingObjectPosition hit=Blocks.waterlily.collisionRayTrace(world,pos,new Vec3(5.5,202,5.5),new Vec3(5.5,199,5.5));
            require(hit!=null&&hit.hitVec.yCoord==lily.maxY,"Lily right-click ray uses original collision surface");
            ItemStack sword=new ItemStack(Items.diamond_sword);NBTTagCompound tag=new NBTTagCompound(),snapshot=new NBTTagCompound();
            snapshot.setBoolean("blocking",true);snapshot.setInteger("format",ServerSession.profile().serverProtocol());tag.setTag(ComponentItemSnapshot.KEY,snapshot);sword.setTagCompound(tag);
            if(ServerSession.rule(ClientRule.COMPONENT_BLOCKING)) {
                require(sword.getItemUseAction()==EnumAction.BLOCK&&sword.getMaxItemUseDuration()==72000,"Original component blocking action and duration");
                sword.useItemRightClick(world,player);
                require(player.isUsingItem()&&player.getItemInUse()==sword,"Block component starts actual native item use");
                player.clearItemInUse();
                BlockingRenderSmokeTest.verify(sword);
                ItemStack plain=new ItemStack(Items.diamond_sword);plain.useItemRightClick(world,player);
                require(!player.isUsingItem()&&plain.getItemUseAction()==EnumAction.NONE,"Plain modern sword remains non-blocking");
            }
        }finally{player.clearItemInUse();world.setBlockState(pos,before,3);}
        // Invoke the actual Minecraft handler. No movement tick may be needed to
        // flush an attack's swing; a changed sneak/sprint packet can follow it.
        net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getMinecraft();
        MovingObjectPosition previous=mc.objectMouseOver;
        net.minecraft.entity.passive.EntityCow cow=new net.minecraft.entity.passive.EntityCow(world);cow.setEntityId(991);
        java.lang.reflect.Method click=mc.getClass().getDeclaredMethod("clickMouse");click.setAccessible(true);
        java.lang.reflect.Field cooldown=mc.getClass().getDeclaredField("leftClickCounter");cooldown.setAccessible(true);
        int previousCooldown=cooldown.getInt(mc);
        try {
            cooldown.setInt(mc,0);mc.objectMouseOver=new MovingObjectPosition(cow);sent.clear();click.invoke(mc);
            java.util.List<net.minecraft.network.Packet> actions=new java.util.ArrayList<>();
            for(net.minecraft.network.Packet packet:sent)if(packet instanceof net.minecraft.network.play.client.C02PacketUseEntity
                ||packet instanceof net.minecraft.network.play.client.C0APacketAnimation
                ||packet instanceof net.minecraft.network.play.client.C17PacketCustomPayload)actions.add(packet);
            require(actions.size()==2,"One attack and one swing from actual entity click: "+actions);
            if(ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.COMBAT)) {
                require(actions.get(0) instanceof net.minecraft.network.play.client.C02PacketUseEntity,"Modern click attacks before swing");
                require(actions.get(1) instanceof net.minecraft.network.play.client.C17PacketCustomPayload,"Modern swing uses immediate hand path, not delayed legacy queue");
            }else require(actions.get(0) instanceof net.minecraft.network.play.client.C0APacketAnimation,"Legacy click keeps swing before attack");
            world.setBlockState(pos,Blocks.stone.getDefaultState(),3);
            mc.objectMouseOver=new MovingObjectPosition(new Vec3(5.5,201,5.5),EnumFacing.UP,pos);
            cooldown.setInt(mc,0);sent.clear();click.invoke(mc);actions.clear();
            for(net.minecraft.network.Packet packet:sent)if(packet instanceof net.minecraft.network.play.client.C07PacketPlayerDigging
                ||packet instanceof net.minecraft.network.play.client.C0APacketAnimation
                ||packet instanceof net.minecraft.network.play.client.C17PacketCustomPayload)actions.add(packet);
            require(actions.size()==2,"One initial dig and one swing from actual block click: "+actions);
            require(actions.get(ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.COMBAT)?0:1)
                instanceof net.minecraft.network.play.client.C07PacketPlayerDigging,"Initial block click uses original-version dig/swing order");
        }finally{mc.playerController.resetBlockRemoving();world.setBlockState(pos,before,3);cooldown.setInt(mc,previousCooldown);mc.objectMouseOver=previous;sent.clear();}
        rejectedPlacement(world,player,sent);
    }

    private static void rejectedPlacement(WorldClient world,EntityPlayerSP player,java.util.List<net.minecraft.network.Packet> sent)throws Exception {
        if(!com.viaversion.viaforge.hands.Offhand.active())return;
        net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getMinecraft();
        BlockPos pos=new BlockPos(5,200,5),adjacent=pos.up();
        IBlockState before=world.getBlockState(pos),above=world.getBlockState(adjacent);
        ItemStack main=player.getHeldItem(),off=com.viaversion.viaforge.hands.Offhand.get();
        MovingObjectPosition oldHit=mc.objectMouseOver;
        net.minecraft.world.WorldSettings.GameType mode=mc.playerController.getCurrentGameType();
        java.lang.reflect.Method click=mc.getClass().getDeclaredMethod("rightClickMouse");click.setAccessible(true);
        try {
            mc.playerController.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
            ItemStack stone=new ItemStack(Blocks.stone,8);
            player.inventory.mainInventory[player.inventory.currentItem]=stone;
            com.viaversion.viaforge.hands.Offhand.set(null);
            world.setBlockState(pos,Blocks.stone.getDefaultState(),3);
            world.setBlockState(adjacent,Blocks.stone.getDefaultState(),3);
            require(!((ItemBlock)stone.getItem()).canPlaceBlockOnSide(world,pos,EnumFacing.UP,player,stone),"Rejected placement fixture really blocks placement");
            mc.objectMouseOver=new MovingObjectPosition(new Vec3(5.5,201,5.5),EnumFacing.UP,pos);
            sent.clear();click.invoke(mc);
            java.util.List<String> operations=new java.util.ArrayList<>();
            for(net.minecraft.network.Packet packet:sent)if(packet instanceof net.minecraft.network.play.client.C17PacketCustomPayload) {
                net.minecraft.network.PacketBuffer data=((net.minecraft.network.play.client.C17PacketCustomPayload)packet).getBufferData();
                operations.add(data.getUnsignedByte(0)+":"+data.getUnsignedByte(1));
            }
            require(operations.equals(ServerSession.rule(ClientRule.MODERN_BLOCK_USE_FAILURE)?java.util.Arrays.asList("1:0"):java.util.Arrays.asList("7:0","1:1")),
                    "Original rejected-placement hand/air/swing sequence: "+operations);
            require(stone.stackSize==8&&world.getBlockState(adjacent).getBlock()==Blocks.stone,"Rejected placement neither consumes nor changes blocks");
        }finally {
            world.setBlockState(pos,before,3);world.setBlockState(adjacent,above,3);
            player.inventory.mainInventory[player.inventory.currentItem]=main;com.viaversion.viaforge.hands.Offhand.set(off);
            mc.playerController.setGameType(mode);mc.objectMouseOver=oldHit;sent.clear();
        }
    }
}
