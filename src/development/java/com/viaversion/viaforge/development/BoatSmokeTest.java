package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.boats.*;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.init.Blocks;
import net.minecraft.block.BlockLiquid;
import net.minecraft.network.*;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.*;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.*;

final class BoatSmokeTest {
    static void verify(BlockVersionProfile profile,EmbeddedChannel client,EmbeddedChannel server,NetHandlerPlayClient handler,WorldClient world) throws Exception {
        Minecraft mc=Minecraft.getMinecraft();
        EntityOtherPlayerMP a=new EntityOtherPlayerMP(world,new GameProfile(new UUID(0,881),"BoatDriver"));
        EntityOtherPlayerMP b=new EntityOtherPlayerMP(world,new GameProfile(new UUID(0,882),"BoatPassenger"));
        world.addEntityToWorld(881,a); world.addEntityToWorld(882,b);
        try {
            spawn(profile,client,server,handler,880,8.125,76.375,8.5,(byte)64);
            ServerBoat boat=(ServerBoat)world.getEntityByID(880);
            require(boat.posY==76.375 && boat.rotationYaw==90,"Original boat spawn avoids Via's old yaw/height offsets");
            require(boat.width==1.375F && boat.height==.5625F && !(Entity.class.cast(boat) instanceof EntityBoat),"Modern boat hitbox and separate physics class");
            int first=profile.protocol()>=210?6:5;
            for(int wood=0;wood<6;wood++) {
                ByteBuf data=packet(profile,"SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(data,880);
                data.writeByte(first+3).writeByte(1); Types.VAR_INT.writePrimitive(data,wood); data.writeByte(255); send(client,server,handler,data);
                require(boat.wood==wood,"Original six wood variants survive Via");
                ServerBoatRenderer render=(ServerBoatRenderer)mc.getRenderManager().<ServerBoat>getEntityRenderObject(boat);
                require(mc.getResourceManager().getResource(render.getEntityTexture(boat))!=null,"Original boat texture available");
                require(com.viaversion.viaforge.items.ServerItemCooldowns.itemId(boat.getPickedResult(null))==(wood==0?333:443+wood),"Boat picking retains wood item");
            }
            seats(profile,client,server,handler,880,881,882);
            require(boat.passengers().equals(Arrays.asList(a,b)) && a.ridingEntity==boat && b.ridingEntity==boat && a.riddenByEntity==null,"Two original boat seats, no stacked players");
            boat.rotationYaw=0; boat.updateRiderPosition();
            require(Math.abs(a.posZ-boat.posZ-.2)<.00001 && Math.abs(b.posZ-boat.posZ+.6)<.00001,"Both seats use original rotated offsets");
            int ageA=a.ticksExisted,ageB=b.ticksExisted;
            world.updateEntities();
            require(a.ridingEntity==boat && b.ridingEntity==boat && a.ticksExisted==ageA+1 && b.ticksExisted==ageB+1,"Native world ticks both passengers exactly once without detaching seat two");
            spawn(profile,client,server,handler,883,10,76,8,(byte)0);
            ServerBoat other=(ServerBoat)world.getEntityByID(883);
            seats(profile,client,server,handler,883,882);
            require(b.ridingEntity==other && !boat.contains(b),"Passenger changes boats without stale old seat");
            seats(profile,client,server,handler,880,882,881);
            require(boat.passengers().get(0)==b && a.ridingEntity==boat && other.passengers().isEmpty(),"Seat order and moving back to another boat");
            seats(profile,client,server,handler,880);
            require(a.ridingEntity==null && b.ridingEntity==null && boat.passengers().isEmpty(),"Server removes both passengers");
            seats(profile,client,server,handler,880,885,882);
            require(boat.passengers().isEmpty(),"Delayed driver does not promote a known second passenger");
            EntityOtherPlayerMP late=new EntityOtherPlayerMP(world,new GameProfile(new UUID(0,885),"DelayedDriver"));
            world.addEntityToWorld(885,late);ServerBoats.resolve(world);
            require(boat.passengers().equals(Arrays.asList(late,b)),"Deferred seat order resolves when driver arrives");
            seats(profile,client,server,handler,880);
            world.removeEntityFromWorld(885);ServerBoats.remove(world,885);
            ByteBuf teleport=packet(profile,"TELEPORT_ENTITY"); Types.VAR_INT.writePrimitive(teleport,880); teleport.writeDouble(8.125).writeDouble(78.25).writeDouble(8.5).writeByte(32).writeByte(0).writeBoolean(false); send(client,server,handler,teleport);
            for(int tick=0;tick<10;tick++) boat.onUpdate();
            require(boat.posY==78.25 && boat.rotationYaw==45,"Remote boat interpolates exact target position and rotation");
            ByteBuf move=packet(profile,"MOVE_ENTITY_POS_ROT"); Types.VAR_INT.writePrimitive(move,880); move.writeShort(1).writeShort(-2).writeShort(3).writeByte(48).writeByte(0).writeBoolean(false); send(client,server,handler,move);
            for(int tick=0;tick<10;tick++) boat.onUpdate();
            require(boat.posX==8.125+1/4096D && boat.posY==78.25-2/4096D && boat.rotationYaw==67.5F,"Relative boat coordinates retain 1/4096 precision");
            seats(profile,client,server,handler,880,881);
            boat.motionX=boat.motionZ=0;boat.deltaRotation=0;boat.rotationYaw=0;boat.input(false,false,true,false);boat.steer();
            require(Math.abs(boat.motionZ-.04F)<.00000001 && boat.paddle(0)&&boat.paddle(1),"Forward thrust and both paddles");
            boat.input(true,true,false,false);boat.steer();
            require(boat.paddle(0)==(profile.protocol()<315) && boat.paddle(1)==(profile.protocol()<315),"Opposing rudder keys change at 1.11");
            boat.input(true,false,false,false);boat.steer(); require(!boat.paddle(0)&&boat.paddle(1),"Turning uses the original side's oar");
            boat.onUpdate(); float phase=boat.paddlePhase(1,1);
            require(Math.abs(phase-(profile.protocol()>=335?.3926991F:.4F))<.00001,"Paddle animation step changes at 1.12");
            boat.seats(Arrays.asList(mc.thePlayer,b)); require(boat.controlled(),"First local passenger controls boat");
            require(mc.thePlayer.rotationYaw==boat.rotationYaw && mc.thePlayer.prevRotationYaw==boat.rotationYaw,"Boarding aligns local view with boat");
            boat.seats(Arrays.asList(a,mc.thePlayer)); require(!boat.controlled(),"Second local passenger cannot steer");
            boat.seats(Arrays.asList(mc.thePlayer,b));
            ByteBuf correction=packet(profile,"MOVE_VEHICLE"); correction.writeDouble(10.125).writeDouble(78.625).writeDouble(10.875).writeFloat(123.5F).writeFloat(0); send(client,server,handler,correction);
            require(boat.posY==78.625 && boat.rotationYaw==123.5F,"Server vehicle correction uses full doubles/floats");
            outbound(profile,client,server,BoatPackets.movement(boat),"MOVE_VEHICLE",boat);
            outbound(profile,client,server,BoatPackets.rowing(true,false),"PADDLE_BOAT",boat);
            outbound(profile,client,server,BoatPackets.input(.5F,1,false,true),"PLAYER_INPUT",boat);
            seats(profile,client,server,handler,880);
            physics(profile,world);
            placement(world,handler);
            controls(profile,world,a);
            BoatRenderSmokeTest.verify(profile,world);
            ByteBuf remove=packet(profile,"REMOVE_ENTITIES"); Types.VAR_INT.writePrimitive(remove,2);Types.VAR_INT.writePrimitive(remove,880);Types.VAR_INT.writePrimitive(remove,883);send(client,server,handler,remove);
            require(world.getEntityByID(880)==null && world.getEntityByID(883)==null,"Boat destroy cleans up client entities");
        } finally {
            for(int id:new int[]{880,881,882,883,885}) world.removeEntityFromWorld(id);
            if(mc.thePlayer.ridingEntity instanceof ServerBoat) ((ServerBoat)mc.thePlayer.ridingEntity).detach(mc.thePlayer);
            ServerBoats.clear();
        }
    }
    static void controls(BlockVersionProfile profile,WorldClient world,Entity other) {
        Minecraft mc=Minecraft.getMinecraft();net.minecraft.client.entity.EntityPlayerSP previous=mc.thePlayer;
        WorldClient previousWorld=mc.theWorld;
        Entity previousView=mc.getRenderViewEntity();
        net.minecraft.client.multiplayer.PlayerControllerMP previousController=mc.playerController;
        net.minecraft.client.gui.GuiScreen screen=mc.currentScreen;
        boolean forward=mc.gameSettings.keyBindForward.isKeyDown();List<Packet> sent=new ArrayList<>();
        NetHandlerPlayClient handler=new NetHandlerPlayClient(mc,null,new NetworkManager(EnumPacketDirection.CLIENTBOUND),new GameProfile(new UUID(0,889),"Driver")) {
            @Override public void addToSendQueue(Packet packet) { sent.add(packet); }
        };
        // This constructed fixture has no login/initial teleport; mark its prepared world ready.
        try {java.lang.reflect.Field ready=NetHandlerPlayClient.class.getDeclaredField("doneLoadingTerrain");ready.setAccessible(true);ready.setBoolean(handler,true);}
        catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
        net.minecraft.client.entity.EntityPlayerSP driver=new net.minecraft.client.entity.EntityPlayerSP(mc,world,handler,new net.minecraft.stats.StatFileWriter());
        driver.movementInput=new net.minecraft.util.MovementInputFromOptions(mc.gameSettings);
        ServerBoat boat=new ServerBoat(world,profile.protocol());boat.setPosition(8,90,8);
        Map<BlockPos,net.minecraft.block.state.IBlockState> initialWater=new HashMap<>();
        try {
            mc.thePlayer=driver;mc.theWorld=world;mc.setRenderViewEntity(driver);mc.playerController=new net.minecraft.client.multiplayer.PlayerControllerMP(mc,handler);mc.currentScreen=null;
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(),true);
            driver.setPosition(8,90,8);world.addEntityToWorld(889,driver);world.addEntityToWorld(887,boat);
            boat.seats(Arrays.asList(driver,other));sent.clear();world.updateEntities();
            tickBoundary(sent);
            require(boat.posZ==8,"Boat consumes previous passenger input, not this tick's raw key state");
            require(operations(sent).equals(Arrays.asList(1,2,0)),"First mount tick sends exactly one paddle/input/movement sequence");
            sent.clear();world.updateEntities();
            require(boat.posZ>8 && operations(sent).equals(Arrays.asList(1,2,0)),"Actual driver tick sends paddles, passenger input and vehicle movement in original order");
            double velocity=boat.motionZ;
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(),false);
            sent.clear();world.updateEntities();
            require(boat.motionZ==velocity*(double).9F+(double).04F,"Release tick retains input sampled by the previous passenger tick");
            require(operations(sent).equals(Arrays.asList(1,2,0)),"Release tick keeps the original packet order");
            velocity=boat.motionZ;sent.clear();world.updateEntities();
            require(boat.motionZ==velocity*(double).9F,"Next tick coasts with original water/air friction and no thrust");
            require(operations(sent).equals(Arrays.asList(1,2,0)),"Coasting still sends one vehicle update per tick");
            require(Math.abs(driver.posZ-boat.posZ-.2F)<.000001,"Driver seat follows this tick's boat movement without camera lag");
            driver.rotationYaw=boat.rotationYaw;driver.setAngles(2000,0);
            require(Math.abs(MathHelper.wrapAngleTo180_float(driver.rotationYaw-boat.rotationYaw))<=105,"Mouse movement clamps boat view immediately");
            boat.seats(Arrays.asList(other,driver));sent.clear();double x=boat.posX,z=boat.posZ;world.updateEntities();
            tickBoundary(sent);
            require(boat.posX==x&&boat.posZ==z&&operations(sent).equals(Collections.singletonList(2)),"Actual second passenger tick cannot steer or emit vehicle/paddle packets");
            boat.seats(Collections.emptyList());world.removeEntityFromWorld(887);
            for(int bx=7;bx<=9;bx++)for(int bz=7;bz<=9;bz++) {
                BlockPos pos=new BlockPos(bx,90,bz);initialWater.put(pos,world.getBlockState(pos));
                world.setBlockState(pos,Blocks.water.getDefaultState(),0);
            }
            boat=new ServerBoat(world,profile.protocol());boat.setPosition(8.5,90.6,8.5);
            world.addEntityToWorld(887,boat);boat.seats(Collections.singletonList(driver));sent.clear();
            world.updateEntities();
            require(boat.status==ServerBoat.Status.WATER&&boat.motionY<0&&boat.posY==90.6+boat.motionY,
                "First mounted tick in water uses buoyancy, without an invented air-entry position snap on reconnect");
            require(operations(sent).equals(Arrays.asList(1,2,0)),"Mounted water spawn ticks and sends movement once");
        } finally {
            boat.seats(Collections.emptyList());mc.thePlayer=previous;mc.theWorld=previousWorld;mc.setRenderViewEntity(previousView);mc.playerController=previousController;mc.currentScreen=screen;
            world.removeEntityFromWorld(887);world.removeEntityFromWorld(889);
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(),forward);
            for(Map.Entry<BlockPos,net.minecraft.block.state.IBlockState> entry:initialWater.entrySet())world.setBlockState(entry.getKey(),entry.getValue(),0);
        }
    }
    private static void tickBoundary(List<Packet> sent) {
        try {
            java.lang.reflect.Constructor<com.viaversion.viaforge.compatibility.NativeClientTicks> constructor=
                com.viaversion.viaforge.compatibility.NativeClientTicks.class.getDeclaredConstructor();constructor.setAccessible(true);
            com.viaversion.viaforge.compatibility.NativeClientTicks observer=constructor.newInstance();
            int before=sent.size();
            observer.end(new net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent(net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END));
            boolean modern=com.viaversion.viaforge.compatibility.ServerSession.profile().serverProtocol()>=768;
            require(sent.size()==before+(modern?1:0),"Ridden player schedules exactly one real client tick boundary on modern targets");
            if(modern) {
                C17PacketCustomPayload end=(C17PacketCustomPayload)sent.get(before);
                require(end.getChannelName().equals("VF|tick")&&end.getBufferData().readableBytes()==1
                    &&end.getBufferData().getByte(end.getBufferData().readerIndex())==1,"Tick end does not synthesize another passenger input");
            }
            int after=sent.size();observer.end(new net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent(net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END));
            require(sent.size()==after,"Already completed riding tick cannot send a duplicate boundary");
        } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
    }
    private static List<Integer> operations(List<Packet> sent) {
        List<Integer> operations=new ArrayList<>();
        for(Packet packet:sent) {
            require(!(packet instanceof net.minecraft.network.play.client.C0CPacketInput),"Modern passenger input bypasses Via's old synthesized paddles");
            if(packet instanceof C17PacketCustomPayload) {
                PacketBuffer data=((C17PacketCustomPayload)packet).getBufferData();
                if(BoatPackets.CHANNEL.equals(((C17PacketCustomPayload)packet).getChannelName()))operations.add((int)data.getUnsignedByte(data.readerIndex()));
                data.release();
            }
        }
        return operations;
    }
    static void disconnected() {
        Minecraft mc=Minecraft.getMinecraft();EntityBoat nativeBoat=new EntityBoat(null);
        require(nativeBoat.width==1.5F && nativeBoat.height==.6F && mc.getRenderManager().<EntityBoat>getEntityRenderObject(nativeBoat) instanceof net.minecraft.client.renderer.entity.RenderBoat,"Disconnected/1.8 native boat hitbox and renderer unchanged");
        require(!com.viaversion.viaforge.items.ClientItems.is(stack(333,0),com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind.BOAT),"Modern boat actions disabled outside a supported server");
    }
    private static void placement(WorldClient world,NetHandlerPlayClient handler) {
        Minecraft mc=Minecraft.getMinecraft(); EntityPlayerSPState saved=new EntityPlayerSPState(mc.thePlayer);
        Map<BlockPos,net.minecraft.block.state.IBlockState> old=new HashMap<>();
        try {
            for(int x=6;x<=10;x++) for(int z=6;z<=10;z++) for(int y=80;y<=86;y++) {
                BlockPos pos=new BlockPos(x,y,z);old.put(pos,world.getBlockState(pos));world.setBlockToAir(pos);
            }
            for(int x=6;x<=10;x++) for(int z=6;z<=10;z++) world.setBlockState(new BlockPos(x,80,z),Blocks.stone.getDefaultState(),0);
            mc.thePlayer.setPositionAndRotation(8.5,83,8.5,0,90);
            net.minecraft.client.multiplayer.PlayerControllerMP controller=new net.minecraft.client.multiplayer.PlayerControllerMP(mc,handler);
            for(int wood=0;wood<6;wood++) {
                net.minecraft.item.ItemStack item=stack(wood==0?333:443+wood,0);
                mc.thePlayer.capabilities.isCreativeMode=false;mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem]=item;
                require(controller.sendUseItem(mc.thePlayer,world,item) && mc.thePlayer.getHeldItem()==null,"Successful modern boat placement consumes survival item");
                item=stack(wood==0?333:443+wood,0);mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem]=item;
                mc.thePlayer.capabilities.isCreativeMode=true;mc.thePlayer.isSwingInProgress=false;
                require(controller.sendUseItem(mc.thePlayer,world,item)&&item.stackSize==1&&mc.thePlayer.isSwingInProgress,"Creative boat placement succeeds and swings without consuming");
            }
            mc.thePlayer.rotationPitch=-90; net.minecraft.item.ItemStack item=stack(333,0);
            require(!BoatPlacement.use(item,world,mc.thePlayer)&&item.stackSize==1,"No placement on a missed ray");
            mc.thePlayer.rotationPitch=90;
            ServerBoat obstruction=new ServerBoat(world,107);obstruction.setPosition(8.5,81,8.5);world.addEntityToWorld(884,obstruction);
            require(!BoatPlacement.use(item,world,mc.thePlayer),"Existing boat blocks overlapping placement");world.removeEntityFromWorld(884);
            require(net.minecraft.init.Items.boat instanceof net.minecraft.item.ItemBoat,"Native 1.8 boat item remains registered");
        } finally {
            world.removeEntityFromWorld(884);saved.restore(mc.thePlayer);
            for(Map.Entry<BlockPos,net.minecraft.block.state.IBlockState> entry:old.entrySet())world.setBlockState(entry.getKey(),entry.getValue(),0);
        }
    }
    private static final class EntityPlayerSPState {
        final double x,y,z;final float yaw,pitch;final boolean creative;final net.minecraft.item.ItemStack item;
        EntityPlayerSPState(net.minecraft.client.entity.EntityPlayerSP p){x=p.posX;y=p.posY;z=p.posZ;yaw=p.rotationYaw;pitch=p.rotationPitch;creative=p.capabilities.isCreativeMode;item=p.getHeldItem();}
        void restore(net.minecraft.client.entity.EntityPlayerSP p){p.setPositionAndRotation(x,y,z,yaw,pitch);p.capabilities.isCreativeMode=creative;p.inventory.mainInventory[p.inventory.currentItem]=item;}
    }
    private static void physics(BlockVersionProfile profile,WorldClient world) {
        ServerBoat boat=new ServerBoat(world,profile.protocol());
        Map<BlockPos,net.minecraft.block.state.IBlockState> old=new HashMap<>();
        try {
            for(int x=6;x<=10;x++) for(int z=6;z<=10;z++) for(int y=80;y<=83;y++) {BlockPos pos=new BlockPos(x,y,z);old.put(pos,world.getBlockState(pos));world.setBlockToAir(pos);}
            boat.setPosition(8.5,81,8.5); require(boat.environment()==ServerBoat.Status.AIR,"Boat air status");
            boat.status=ServerBoat.Status.AIR;boat.motionY=0;boat.physics();require(Math.abs(boat.motionY+.04F)<.0000001,"Original boat gravity");
            for(int x=6;x<=10;x++) for(int z=6;z<=10;z++) world.setBlockState(new BlockPos(x,80,z),Blocks.ice.getDefaultState(),0);
            boat.setPosition(8.5,81,8.5);require(boat.environment()==ServerBoat.Status.LAND && Math.abs(boat.landGlide-Blocks.ice.slipperiness)<.000001,"Boat ice friction from actual supporting collision boxes: "+boat.landGlide);
            for(int x=6;x<=10;x++) for(int z=6;z<=10;z++) world.setBlockState(new BlockPos(x,81,z),Blocks.water.getDefaultState(),0);
            boat.setPosition(8.5,81.6,8.5);require(boat.environment()==ServerBoat.Status.WATER,"Surface buoyancy status");
            boat.status=ServerBoat.Status.WATER;boat.motionY=0;boat.physics();require(boat.motionY<0 && boat.motionY>-.04F,"Partial submerged boat buoyancy opposes gravity");
            boat.setPosition(8.5,81.1,8.5);require(boat.environment()==ServerBoat.Status.UNDER_WATER,"Submerged still-water status");
            world.setBlockState(new BlockPos(8,81,8),Blocks.flowing_water.getDefaultState().withProperty(BlockLiquid.LEVEL,1),0);
            require(boat.environment()==ServerBoat.Status.FLOWING_WATER,"Flowing-water submerged status");
        } finally {for(Map.Entry<BlockPos,net.minecraft.block.state.IBlockState> entry:old.entrySet())world.setBlockState(entry.getKey(),entry.getValue(),0);}
    }
    private static void spawn(BlockVersionProfile p,EmbeddedChannel c,EmbeddedChannel s,NetHandlerPlayClient h,int id,double x,double y,double z,byte yaw) throws Exception {
        ByteBuf add=packet(p,"ADD_ENTITY");Types.VAR_INT.writePrimitive(add,id);add.writeLong(0).writeLong(id).writeByte(1).writeDouble(x).writeDouble(y).writeDouble(z).writeByte(0).writeByte(yaw).writeInt(0).writeShort(0).writeShort(0).writeShort(0);send(c,s,h,add);
        require(Minecraft.getMinecraft().theWorld.getEntityByID(id) instanceof ServerBoat,"Original boat spawn");
    }
    private static void seats(BlockVersionProfile p,EmbeddedChannel c,EmbeddedChannel s,NetHandlerPlayClient h,int id,int...riders) throws Exception {
        ByteBuf packet=packet(p,"SET_PASSENGERS");Types.VAR_INT.writePrimitive(packet,id);Types.VAR_INT.writePrimitive(packet,riders.length);for(int rider:riders)Types.VAR_INT.writePrimitive(packet,rider);send(c,s,h,packet);
        c.runPendingTasks(); ByteBuf pending;while((pending=c.readInbound())!=null) {try {int nativeId=Types.VAR_INT.readPrimitive(pending);Packet nativePacket=EnumConnectionState.PLAY.getPacket(EnumPacketDirection.CLIENTBOUND,nativeId);nativePacket.readPacketData(new PacketBuffer(pending));nativePacket.processPacket(h);}finally{pending.release();}}
    }
    private static void outbound(BlockVersionProfile p,EmbeddedChannel c,EmbeddedChannel s,C17PacketCustomPayload packet,String type,ServerBoat boat) throws Exception {
        ByteBuf old;while((old=c.readOutbound())!=null)old.release();
        PacketBuffer data=new PacketBuffer(Unpooled.buffer());Types.VAR_INT.writePrimitive(data,0x17);packet.writePacketData(data);
        try { c.writeOutbound(data); }
        catch(com.viaversion.viaversion.exception.CancelEncoderException expected) { /* The private payload was replaced by a real vehicle packet. */ }
        c.runPendingTasks();
        ByteBuf compressed=c.readOutbound();require(compressed!=null,"Modern boat packet emitted "+type);
        EmbeddedChannel decompress=new EmbeddedChannel(new NettyCompressionDecoder(256));
        try {decompress.writeInbound(compressed);ByteBuf wire=decompress.readInbound();try {
            require(Types.VAR_INT.readPrimitive(wire)==BlockItemPipelineSmokeTest.serverbound(p,type),"Correct target boat packet ID "+type);
            if(type.equals("MOVE_VEHICLE")) require(wire.readDouble()==boat.posX&&wire.readDouble()==boat.posY&&wire.readDouble()==boat.posZ&&wire.readFloat()==boat.rotationYaw&&wire.readFloat()==boat.rotationPitch,"Vehicle coordinates bypass legacy offsets");
            else if(type.equals("PADDLE_BOAT"))require(wire.readBoolean()&&!wire.readBoolean(),"Original paddle booleans");
            else require(wire.readFloat()==.5F&&wire.readFloat()==1&&wire.readUnsignedByte()==2,"Passenger dismount input without synthetic paddles");
            require(!wire.isReadable(),"No stray custom payload bytes");
        }finally{wire.release();}}finally{decompress.finish();}
        require(c.readOutbound()==null,"No duplicate fallback boat packet");
    }
}
