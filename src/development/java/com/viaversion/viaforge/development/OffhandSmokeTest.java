package com.viaversion.viaforge.development;

import com.viaversion.viaforge.hands.*;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import io.netty.buffer.*;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.item.*;
import net.minecraft.init.Items;
import net.minecraft.network.*;
import net.minecraft.network.play.client.*;
import net.minecraft.util.*;
import java.util.*;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.*;

final class OffhandSmokeTest {
    static void verify(BlockVersionProfile profile,EmbeddedChannel client,EmbeddedChannel server,NetHandlerPlayClient handler,WorldClient world)throws Exception {
        Minecraft mc=Minecraft.getMinecraft();ItemStack main=mc.thePlayer.getHeldItem(),cursor=mc.thePlayer.inventory.getItemStack();
        PlayerControllerMP controller=mc.playerController;MovingObjectPosition hit=mc.objectMouseOver;
        boolean creative=mc.thePlayer.capabilities.isCreativeMode;
        net.minecraft.util.MovementInput input=mc.thePlayer.movementInput;double x=mc.thePlayer.posX,y=mc.thePlayer.posY,z=mc.thePlayer.posZ;
        try {
            Offhand.ensure();require(mc.thePlayer.inventoryContainer.inventorySlots.size()==46,"Player inventory exposes original offhand slot 45");
            com.viaversion.nbt.tag.CompoundTag nbt=new com.viaversion.nbt.tag.CompoundTag();nbt.putString("HandTest","unchanged");
            ByteBuf slot=packet(profile,"CONTAINER_SET_SLOT");slot.writeByte(0).writeShort(45);Types.ITEM1_8.write(slot,new DataItem(442,(byte)1,(short)43,nbt));send(client,server,handler,slot);
            require(ClientItems.is(Offhand.get(),LegacyItemCatalog.Kind.SHIELD)&&Offhand.get().getItemDamage()==43&&Offhand.get().getTagCompound().getString("HandTest").equals("unchanged"),"Original offhand slot and shield durability/NBT survive Via cancellation");
            com.viaversion.viaversion.api.minecraft.item.Item[] contents=new com.viaversion.viaversion.api.minecraft.item.Item[46];contents[45]=new DataItem(432,(byte)3,(short)0,null);
            ByteBuf content=packet(profile,"CONTAINER_SET_CONTENT");content.writeByte(0);Types.ITEM1_8_SHORT_ARRAY.write(content,contents);send(client,server,handler,content);
            require(ServerItemCooldowns.itemId(Offhand.get())==432&&Offhand.get().stackSize==3,"Complete original inventory includes the offhand");
            slot=packet(profile,"CONTAINER_SET_SLOT");slot.writeByte(-2).writeShort(40);Types.ITEM1_8.write(slot,new DataItem(442,(byte)1,(short)0,null));send(client,server,handler,slot);
            require(ServerItemCooldowns.itemId(Offhand.get())==442,"Direct inventory index 40 updates offhand");
            mc.thePlayer.inventory.setItemStack(null);
            ItemStack clicked=mc.thePlayer.inventoryContainer.slotClick(45,0,0,mc.thePlayer);
            require(Offhand.get()==null&&ServerItemCooldowns.itemId(mc.thePlayer.inventory.getItemStack())==442,"Left click picks up offhand into cursor");
            wire(profile,client,HandPackets.wrapped(new C0EPacketClickWindow(0,45,0,0,clicked,(short)17),0),"CONTAINER_CLICK",b->{require(b.readUnsignedByte()==0&&b.readShort()==45&&b.readByte()==0&&b.readShort()==17&&Types.VAR_INT.readPrimitive(b)==0,"Offhand click transaction fields");require(Types.ITEM1_8.read(b).identifier()==442,"Click retains target shield identity");});
            mc.thePlayer.inventoryContainer.slotClick(45,0,0,mc.thePlayer);
            require(Offhand.get()!=null&&mc.thePlayer.inventory.getItemStack()==null,"Click places cursor back in offhand");
            wire(profile,client,HandPackets.wrapped(new C10PacketCreativeInventoryAction(45,Offhand.get()),0),"SET_CREATIVE_MODE_SLOT",b->{require(b.readShort()==45&&Types.ITEM1_8.read(b).identifier()==442,"Creative slot 45 keeps original item id");});
            wire(profile,client,HandPackets.action(6),"PLAYER_ACTION",b->{require(Types.VAR_INT.readPrimitive(b)==6,"F sends swap action 6");Types.BLOCK_POSITION1_8.read(b);b.readByte();});
            wire(profile,client,HandPackets.wrapped(new C08PacketPlayerBlockPlacement(Offhand.get()),1),"USE_ITEM",b->require(Types.VAR_INT.readPrimitive(b)==1,"Air use specifies offhand"));
            wire(profile,client,HandPackets.wrapped(new C08PacketPlayerBlockPlacement(new BlockPos(8,75,8),1,Offhand.get(),.25F,.5F,.75F),1),"USE_ITEM_ON",b->{require(Types.BLOCK_POSITION1_8.read(b).y()==75&&Types.VAR_INT.readPrimitive(b)==1&&Types.VAR_INT.readPrimitive(b)==1,"Block use target and offhand");if(profile.protocol()>=315)require(b.readFloat()==.25F&&b.readFloat()==.5F&&b.readFloat()==.75F,"Newer float cursors");else require(b.readUnsignedByte()==4&&b.readUnsignedByte()==8&&b.readUnsignedByte()==12,"Earlier byte cursors");});
            wire(profile,client,HandPackets.wrapped(new C0APacketAnimation(),1),"SWING",b->require(Types.VAR_INT.readPrimitive(b)==1,"Swing specifies offhand"));
            network(profile,client);
            hold(new ItemStack(Items.diamond_sword));ItemStack sword=mc.thePlayer.getHeldItem();Offhand.set(stack(442,0));Offhand.swap();
            require(ServerItemCooldowns.itemId(mc.thePlayer.getHeldItem())==442&&Offhand.get()==sword,"F swaps the two local inventory references");Offhand.swap();
            mc.playerController=new PlayerControllerMP(mc,handler);mc.playerController.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);mc.objectMouseOver=new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS,new Vec3(0,0,0),EnumFacing.UP,new BlockPos(0,0,0));
            HandActions.rightClick();require(mc.thePlayer.getItemInUse()==Offhand.get()&&Offhand.useHand==1&&mc.thePlayer.getHeldItem()==sword,"Sword falls through to blocking shield without changing mainhand");
            mc.thePlayer.movementInput=new net.minecraft.util.MovementInput();
            mc.thePlayer.setPosition(8,80,8);int duration=mc.thePlayer.getItemInUseCount();mc.thePlayer.onUpdate();
            require(mc.thePlayer.isUsingItem()&&mc.thePlayer.getItemInUseCount()==duration-1,"Offhand use survives native held-item consistency check");
            mc.playerController.onStoppedUsingItem(mc.thePlayer);require(!mc.thePlayer.isUsingItem(),"Releasing use stops offhand action");
            hold(new ItemStack(Items.apple));mc.thePlayer.getFoodStats().setFoodLevel(20);HandActions.rightClick();require(mc.thePlayer.getItemInUse()==Offhand.get()&&Offhand.useHand==1,"Full hunger passes mainhand food through to shield");mc.thePlayer.clearItemInUse();hold(sword);
            Offhand.set(stack(432,0));HandActions.rightClick();require(Offhand.useHand==1&&mc.thePlayer.getItemInUse()==Offhand.get(),"Chorus eats from offhand");mc.thePlayer.clearItemInUse();
            hold(stack(432,0));Offhand.set(stack(442,0));HandActions.rightClick();require(Offhand.useHand==0&&mc.thePlayer.getItemInUse()==mc.thePlayer.getHeldItem(),"Mainhand eating takes precedence over offhand shield");mc.thePlayer.clearItemInUse();
            hold(new ItemStack(Items.bow));Offhand.set(new ItemStack(Items.arrow,2));HandActions.rightClick();require(mc.thePlayer.isUsingItem()&&Offhand.useHand==0,"Bow accepts ammunition in offhand");
            mc.thePlayer.getHeldItem().onPlayerStoppedUsing(world,mc.thePlayer,71980);require(Offhand.get().stackSize==1&&mc.thePlayer.getHeldItem().getItemDamage()==0,"Release predicts offhand arrow count while server owns bow durability");mc.thePlayer.clearItemInUse();
            hold(new ItemStack(Items.arrow,2));Offhand.set(new ItemStack(Items.bow));HandActions.rightClick();require(mc.thePlayer.getItemInUse()==Offhand.get()&&Offhand.useHand==1,"Offhand bow accepts arrows from real mainhand");
            Offhand.get().onPlayerStoppedUsing(world,mc.thePlayer,71980);require(mc.thePlayer.getHeldItem().stackSize==1,"Offhand bow consumes mainhand ammunition");mc.thePlayer.clearItemInUse();
            Offhand.set(null);mc.thePlayer.inventory.mainInventory[9]=stack(442,0);mc.thePlayer.inventoryContainer.transferStackInSlot(mc.thePlayer,9);
            require(ServerItemCooldowns.itemId(Offhand.get())==442&&mc.thePlayer.inventory.mainInventory[9]==null,"Shift-click equips shield in an empty offhand");
            mc.thePlayer.inventoryContainer.transferStackInSlot(mc.thePlayer,45);require(Offhand.get()==null,"Shift-click from offhand returns item to inventory");mc.thePlayer.inventory.mainInventory[9]=null;
            BlockPos chest=new BlockPos(9,78,9);net.minecraft.block.state.IBlockState oldChest=world.getBlockState(chest),oldAbove=world.getBlockState(chest.up());MovingObjectPosition oldHit=mc.objectMouseOver;
            try{world.setBlockState(chest,net.minecraft.init.Blocks.chest.getDefaultState());world.setBlockToAir(chest.up());hold(null);Offhand.set(new ItemStack(net.minecraft.init.Blocks.dirt,2));mc.thePlayer.movementInput.sneak=true;
                mc.objectMouseOver=new MovingObjectPosition(new Vec3(9.5,79,9.5),EnumFacing.UP,chest);HandActions.rightClick();
                require(world.getBlockState(chest.up()).getBlock()==net.minecraft.init.Blocks.dirt&&Offhand.get().stackSize==1&&mc.thePlayer.getHeldItem()==null,"Sneaking with empty mainhand places offhand block instead of activating chest");
            }finally{world.setBlockState(chest,oldChest);world.setBlockState(chest.up(),oldAbove);mc.thePlayer.movementInput.sneak=false;mc.objectMouseOver=oldHit;}
            hold(sword);Offhand.set(stack(442,0));OffhandRenderSmokeTest.verify(profile,world);
            OffhandGuiSmokeTest.verify(profile);
            require(mc.thePlayer.inventoryContainer.getSlot(45).xDisplayPosition==77&&mc.thePlayer.inventoryContainer.getSlot(45).yDisplayPosition==62,"Survival offhand position");
            slot=packet(profile,"CONTAINER_SET_SLOT");slot.writeByte(0).writeShort(45);Types.ITEM1_8.write(slot,null);send(client,server,handler,slot);require(Offhand.get()==null,"Server clears offhand");
        }finally{mc.thePlayer.clearItemInUse();Offhand.clear();require(mc.thePlayer.inventoryContainer.inventorySlots.size()==45,"Cleanup removes the extra slot from native container");hold(main);mc.thePlayer.inventory.setItemStack(cursor);mc.thePlayer.capabilities.isCreativeMode=creative;mc.playerController=controller;mc.objectMouseOver=hit;mc.thePlayer.movementInput=input;mc.thePlayer.setPosition(x,y,z);}
    }
    private static void network(BlockVersionProfile profile,EmbeddedChannel client)throws Exception{
        Minecraft mc=Minecraft.getMinecraft();java.util.List<Packet> sent=new java.util.ArrayList<>();
        NetworkManager manager=new NetworkManager(EnumPacketDirection.CLIENTBOUND){@Override public void sendPacket(Packet packet){sent.add(packet);}};
        NetHandlerPlayClient handler=new NetHandlerPlayClient(mc,null,manager,new com.mojang.authlib.GameProfile(new UUID(0,41),"HandTest"));
        int before=Offhand.context;
        try{
            Offhand.context=1;handler.addToSendQueue(new C08PacketPlayerBlockPlacement(Offhand.get()));require(sent.size()==1&&sent.get(0) instanceof C17PacketCustomPayload,"Native network mixin emits one hand-aware use packet");
            wire(profile,client,(C17PacketCustomPayload)sent.remove(0),"USE_ITEM",b->require(Types.VAR_INT.readPrimitive(b)==1,"Actual native outgoing use keeps offhand"));
            handler.addToSendQueue(new C0APacketAnimation());require(Offhand.swingHand==1,"Item-triggered swing selects the active offhand locally");
            wire(profile,client,(C17PacketCustomPayload)sent.remove(0),"SWING",b->require(Types.VAR_INT.readPrimitive(b)==1,"Item-triggered swing keeps offhand on server"));
            handler.addToSendQueue(new C02PacketUseEntity(mc.thePlayer,new Vec3(.25,.5,.75)));
            wire(profile,client,(C17PacketCustomPayload)sent.remove(0),"INTERACT",b->{require(Types.VAR_INT.readPrimitive(b)==mc.thePlayer.getEntityId()&&Types.VAR_INT.readPrimitive(b)==2,"Entity interact-at target/action");require(b.readFloat()==.25F&&b.readFloat()==.5F&&b.readFloat()==.75F&&Types.VAR_INT.readPrimitive(b)==1,"Entity hit position and offhand");});
            Offhand.context=-1;handler.addToSendQueue(new C15PacketClientSettings("en_US",8,net.minecraft.entity.player.EntityPlayer.EnumChatVisibility.FULL,true,127));
            wire(profile,client,(C17PacketCustomPayload)sent.remove(0),"CLIENT_INFORMATION",b->{require(Types.STRING.read(b).equals("en_US")&&b.readByte()==8&&Types.VAR_INT.readPrimitive(b)==0&&b.readBoolean()&&b.readUnsignedByte()==127&&Types.VAR_INT.readPrimitive(b)==1,"Native settings preserve skin/chat and right mainhand");});
            wire(profile,client,HandPackets.wrapped(new C15PacketClientSettings("de_DE",6,net.minecraft.entity.player.EntityPlayer.EnumChatVisibility.HIDDEN,false,63),0),"CLIENT_INFORMATION",b->{Types.STRING.read(b);b.readByte();Types.VAR_INT.readPrimitive(b);b.readBoolean();b.readByte();require(Types.VAR_INT.readPrimitive(b)==0,"Left mainhand reaches target server");});
            Offhand.swingHand=1;handler.addToSendQueue(new C0APacketAnimation());require(Offhand.swingHand==0&&sent.remove(0) instanceof C0APacketAnimation,"Native mining/attack swings return to mainhand");
        }finally{Offhand.context=before;}
    }
    private static void hold(ItemStack stack){Minecraft mc=Minecraft.getMinecraft();mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem]=stack;}
    interface Read {void accept(ByteBuf b)throws Exception;}
    static void wire(BlockVersionProfile p,EmbeddedChannel client,C17PacketCustomPayload packet,String name,Read read)throws Exception{
        ByteBuf old;while((old=client.readOutbound())!=null)old.release();PacketBuffer data=new PacketBuffer(Unpooled.buffer());Types.VAR_INT.writePrimitive(data,0x17);packet.writePacketData(data);
        try{client.writeOutbound(data);}catch(com.viaversion.viaversion.exception.CancelEncoderException expected){}client.runPendingTasks();
        ByteBuf compressed=client.readOutbound();require(compressed!=null,"Missing hand packet "+name);EmbeddedChannel decompress=new EmbeddedChannel(new NettyCompressionDecoder(256));
        try{decompress.writeInbound(compressed);ByteBuf wire=decompress.readInbound();try{require(Types.VAR_INT.readPrimitive(wire)==BlockItemPipelineSmokeTest.serverbound(p,name),"Target packet id "+name);read.accept(wire);require(!wire.isReadable(),"Exact hand packet length "+name);}finally{wire.release();}}finally{decompress.finish();}
        require(client.readOutbound()==null,"No duplicate hand packet "+name);
    }
}
