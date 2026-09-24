package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.*;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.stats.StatFileWriter;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Real native player/movement mixins with flags and action passed through the actual target pipeline. */
final class ElytraFlightSmokeTest {
    static int flags(int id,boolean flying) {
        EntityPlayerSP p=Minecraft.getMinecraft().thePlayer;
        // Change only the tested flight bit, retaining the other server flags
        // (especially sprint); zeroing the byte also stops native sprinting.
        int flags=p!=null&&p.getEntityId()==id?p.getDataWatcher().getWatchableObjectByte(0)&127:0;
        return flags|(flying?128:0);
    }
    interface Wire {
        void start(C17PacketCustomPayload packet) throws Exception;
        void use(C17PacketCustomPayload packet, boolean swing, int hand) throws Exception;
        void flag(int entityId, boolean flying, NetHandlerPlayClient handler) throws Exception;
        void rocket(int entityId, int targetId, NetHandlerPlayClient handler) throws Exception;
    }
    static void verify(WorldClient world, Wire wire) throws Exception {
        Minecraft mc=Minecraft.getMinecraft();WorldClient oldWorld=mc.theWorld;EntityPlayerSP oldPlayer=mc.thePlayer;
        PlayerControllerMP oldController=mc.playerController;
        List<Packet> sent=new ArrayList<>();
        NetworkManager manager=new NetworkManager(EnumPacketDirection.CLIENTBOUND){@Override public void sendPacket(Packet packet){sent.add(packet);}};
        NetHandlerPlayClient handler=new NetHandlerPlayClient(mc,null,manager,new GameProfile(new UUID(0,912),"FlightTest"));
        java.lang.reflect.Field worldField=NetHandlerPlayClient.class.getDeclaredField("clientWorldController");worldField.setAccessible(true);worldField.set(handler,world);
        java.lang.reflect.Field ready=NetHandlerPlayClient.class.getDeclaredField("doneLoadingTerrain");ready.setAccessible(true);ready.set(handler,true);
        mc.theWorld=world;mc.thePlayer=new EntityPlayerSP(mc,world,handler,new StatFileWriter());mc.playerController=new PlayerControllerMP(mc,handler);
        // WorldClient resolves the local player directly via Minecraft.thePlayer.
        // addEntityToWorld would find and kill that same player as a duplicate.
        EntityPlayerSP player=mc.thePlayer;player.movementInput=new net.minecraft.util.MovementInput();player.setEntityId(1);
        try {
            EntityPushSmokeTest.verify(world,player);
            SwimmingSmokeTest.verify(world);
            SneakMovementSmokeTest.verify(world,player);
            InteractionSmokeTest.verify(world,player,sent);
            PickBlockSmokeTest.verify(world,sent);
            sent.clear();
            ServerEntityViews.clear();player.setHealth(20);player.setPosition(5,200,5);player.motionY=-.2;player.onGround=false;
            PacketBuffer attributes=new PacketBuffer(io.netty.buffer.Unpooled.buffer());
            try {
                attributes.writeVarIntToBuffer(1);attributes.writeInt(1);attributes.writeString("generic.flyingSpeed");
                attributes.writeDouble(.4);attributes.writeVarIntToBuffer(0);
                net.minecraft.network.play.server.S20PacketEntityProperties packet=new net.minecraft.network.play.server.S20PacketEntityProperties();
                packet.readPacketData(attributes);handler.handleEntityProperties(packet);
                require(player.getAttributeMap().getAttributeInstanceByName("generic.flyingSpeed").getBaseValue()==.4,"New server attribute survives native zero-default fallback");
            } finally {attributes.release();}
            player.inventory.armorInventory[2]=new ItemStack(net.minecraft.item.Item.getItemById(ClientItems.localItem(443,0)));
            player.capabilities.allowFlying=true;player.capabilities.isFlying=false;
            player.movementInput.jump=true;ServerElytraFlight.input(player);
            require(ServerElytraFlight.flying(player)==ServerSession.rule(ClientRule.PREDICT_ELYTRA_START),"Only 1.15+ predicts flight before the server flag");
            require(sent.size()==1 && sent.get(0) instanceof C17PacketCustomPayload,"One start request in falling state: packets="+sent+", equipped="+ServerElytraFlight.equipped(player)+", active="+com.viaversion.viaforge.compatibility.ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.ELYTRA)+", alive="+player.isEntityAlive()+", spectator="+player.isSpectator()+", water="+player.isInWater()+", lava="+player.isInLava()+", damage="+player.getCurrentArmor(2).getMaxDamage());
            wire.start((C17PacketCustomPayload)sent.remove(0));
            ServerElytraFlight.input(player);require(sent.isEmpty(),"Held jump does not spam start");
            wire.flag(1,true,handler);require(ServerElytraFlight.flying(player),"Original server flag authorizes local flight");
            HandUseSmokeTest.rockets(sent,wire);
            ServerElytraFlight.input(player);
            ServerElytraFlight.updatePose(player);
            require(player.height==.6F&&player.getEyeHeight()==.4F,"Flight collision box and camera use prone pose");
            player.motionX=0;player.motionZ=1;player.rotationPitch=0;player.rotationYaw=0;
            double before=player.posZ;player.moveEntityWithHeading(1,1);
            require(player.posZ>before+.9&&player.motionY>-.2,"Native movement invokes glide instead of walking gravity");
            require(player.limbSwingAmount>0&&player.limbSwing>0,"Glide movement advances animation instead of freezing the walking frame");
            require(player.capabilities.allowFlying&&!player.capabilities.isFlying,"Gliding never grants/toggles creative flight");
            for(int i=0;i<6;i++){ServerElytraFlight.input(player);ServerElytraVisuals.tick(player);}
            net.minecraft.client.model.ModelPlayer model=new net.minecraft.client.model.ModelPlayer(0,false);
            player.motionX=0;player.motionY=0;player.motionZ=2;
            model.setRotationAngles(0,1,0,0,30,.0625F,player);
            require(Math.abs(model.bipedHead.rotateAngleX+(float)Math.PI/4)<1e-6,"Original flying head angle replaces looking down");
            require(Math.abs(model.bipedRightLeg.rotateAngleX-1.4F/8000)<1e-6,"Speed cubed damps walking legs during flight");
            require(Math.abs(model.bipedRightArm.rotateAngleX+1F/8000)<1e-6,"Speed cubed damps walking arms during flight");
            require(model.bipedHeadwear.rotateAngleX==model.bipedHead.rotateAngleX,"Skin overlay follows flying head");
            float[] wingsA=ServerElytraVisuals.wings(player,.5F),wingsB=ServerElytraVisuals.wings(player,.5F);
            require(ServerSession.rule(ClientRule.TICKED_ELYTRA_WINGS)==(wingsA[2]==wingsB[2]),"Version-specific tick versus render wing animation");
            InventoryFlightSmokeTest.verify();
            require(com.viaversion.viaforge.mobs.ServerMobSounds.has("item.elytra.flying"),"Original elytra recording imported for target version");
            String key=com.viaversion.viaforge.mobs.ServerMobSounds.key("item.elytra.flying",7);
            try(java.io.InputStream recording=mc.getResourceManager().getResource(new net.minecraft.util.ResourceLocation("viaforge","sounds/item/elytra/elytra_loop.ogg")).getInputStream()) {
                require(recording.read()=='O'&&recording.read()=='g'&&recording.read()=='g'&&recording.read()=='S',"Original Ogg recording resolves through active resource pack");
            }
            ServerElytraSound sound=new ServerElytraSound(player,new net.minecraft.util.ResourceLocation(key));
            for(int i=0;i<20;i++)sound.update();require(sound.getVolume()==0,"Original 20-tick silent lead-in");
            for(int i=0;i<10;i++)sound.update();require(Math.abs(sound.getVolume()-.5F)<1e-6,"Original 20-tick fade-in");
            for(int i=0;i<10;i++)sound.update();require(sound.getVolume()==1&&Math.abs(sound.getPitch()-1.2F)<1e-6,"Original speed-dependent volume/pitch");
            if(ServerSession.rule(ClientRule.ELYTRA_FIREWORKS)) {
                wire.rocket(941,1,handler);
                require(world.getEntityByID(941) instanceof net.minecraft.entity.item.EntityFireworkRocket,"Native rocket survives original spawn/attachment metadata");
                net.minecraft.entity.item.EntityFireworkRocket rocket=(net.minecraft.entity.item.EntityFireworkRocket)world.getEntityByID(941);
                require(ServerFireworks.attached(rocket)&&ServerEntityViews.boosts(1)==1,"Original rocket target survives Via translation");
                List<Object[]> particles=new ArrayList<>();
                net.minecraft.world.IWorldAccess observer=(net.minecraft.world.IWorldAccess)java.lang.reflect.Proxy.newProxyInstance(
                        net.minecraft.world.IWorldAccess.class.getClassLoader(),new Class<?>[]{net.minecraft.world.IWorldAccess.class},(proxy,method,args)->{
                    if(method.getName().equals("equals"))return proxy==args[0];
                    if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);
                    if(method.getName().equals("spawnParticle"))particles.add(args);return null;
                });
                world.addWorldAccess(observer);try{rocket.onUpdate();}finally{world.removeWorldAccess(observer);}
                double offset=ServerSession.rule(ClientRule.FIREWORK_HAND_TRAIL)?.5:0;
                require(Math.abs(rocket.getDistanceToEntity(player)-offset)<1e-5&&rocket.posY==player.posY,"Rocket follows player, with version-specific hand offset");
                require(!rocket.isInRangeToRenderDist(1),"Attached rocket model is invisible, like original");
                require(player.motionZ==2&&rocket.motionZ==2,"Rocket visual update does not apply a second boost");
                require(particles.size()==1&&(Integer)particles.get(0)[0]==net.minecraft.util.EnumParticleTypes.FIREWORKS_SPARK.getParticleID(),"Exactly one original firework spark per attached-rocket tick");
                Object[] spark=particles.get(0);
                require(Math.abs((Double)spark[2]-rocket.posX)<1e-7&&Math.abs((Double)spark[4]-rocket.posZ)<1e-7
                        &&Math.abs((Double)spark[3]-rocket.posY+(offset==0?.3:0))<1e-7,"Spark originates at version-correct attached trail position");
                if(ServerSession.rule(ClientRule.FIREWORK_HAND_TRAIL)) {
                    double rightX=rocket.posX;
                    com.viaversion.viaforge.hands.Offhand.set(new ItemStack(net.minecraft.init.Items.fireworks));rocket.onUpdate();
                    require((rightX-player.posX)*(rocket.posX-player.posX)<0,"Offhand-only rocket trail changes side");
                    com.viaversion.viaforge.hands.Offhand.set(null);
                }
                wire.rocket(941,-1,handler);rocket.onUpdate();
                require(rocket.isInRangeToRenderDist(1)&&rocket.posY>player.posY,"Unattached rocket keeps normal ascending behavior");
                world.removeEntityFromWorld(941);
            }
            wire.flag(1,false,handler);ServerElytraFlight.input(player);
            ServerElytraFlight.updatePose(player);
            sound.update();require(sound.isDonePlaying(),"Flight loop terminates on server rejection");
            require(!ServerElytraFlight.flying(player)&&player.height==1.8F,"Server rejection restores standing pose");
            InventoryFlightSmokeTest.verify();
            HandUseSmokeTest.shields(sent,wire);
            HandUseSmokeTest.presentation(sent);
            model.setRotationAngles(0,1,0,0,30,.0625F,player);
            require(Math.abs(model.bipedHead.rotateAngleX-(float)Math.PI/6)<1e-6&&Math.abs(model.bipedRightLeg.rotateAngleX-1.4F)<1e-6,"Walking model restored after flight ends: head="+model.bipedHead.rotateAngleX+", leg="+model.bipedRightLeg.rotateAngleX+", height="+player.height+", swimming="+com.viaversion.viaforge.compatibility.ServerSwimming.swimming(player)+", pose="+com.viaversion.viaforge.compatibility.ServerSwimming.pose(player)+", blend="+ServerElytraVisuals.crawlAmount(player,1)+", flying="+ServerElytraFlight.flying(player));
            wire.flag(1,true,handler);player.inventory.armorInventory[2].setItemDamage(431);ServerElytraFlight.input(player);
            require(!ServerElytraFlight.flying(player),"Broken elytra stops flight");
            player.inventory.armorInventory[2].setItemDamage(0);wire.flag(1,true,handler);
            player.capabilities.isFlying=true;ServerElytraFlight.input(player);
            require(!ServerElytraFlight.flying(player)&&player.capabilities.isFlying,"Creative flight remains independent");
            player.capabilities.isFlying=false;wire.flag(1,true,handler);player.onGround=true;ServerElytraFlight.input(player);
            require(ServerElytraFlight.flying(player),"Ground contact retains flight until server metadata, like vanilla");
            wire.flag(1,false,handler);ServerElytraFlight.input(player);
            require(!ServerElytraFlight.flying(player),"Server landing flag terminates flight");
            player.onGround=false;wire.flag(1,true,handler);player.inventory.armorInventory[2]=null;ServerElytraFlight.input(player);
            require(!ServerElytraFlight.flying(player),"Equipment removal terminates flight");
            ElytraMovementSmokeTest.verify(world,player,handler,sent,wire);
            ServerEntityViews.clear();require(!ServerElytraFlight.flying(player),"Disconnect/respawn reset flight");
        } finally {world.removeEntityFromWorld(941);ServerEntityViews.clear();mc.theWorld=oldWorld;mc.thePlayer=oldPlayer;mc.playerController=oldController;}
    }
}
