package com.viaversion.viaforge.development;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.items.*;
import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.BlockPos;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Real input/jump/travel/collision, including a delayed server landing flag. */
final class ElytraMovementSmokeTest {
    static void verify(WorldClient world, EntityPlayerSP p, NetHandlerPlayClient handler,
                       List<Packet> sent, ElytraFlightSmokeTest.Wire wire) throws Exception {
        Map<BlockPos,IBlockState> saved=new LinkedHashMap<>();
        net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getMinecraft();
        net.minecraft.entity.Entity oldCamera=mc.getRenderViewEntity();mc.setRenderViewEntity(p);
        for(int x=4;x<=8;x++)for(int z=4;z<=10;z++)for(int y=199;y<=202;y++) {
            BlockPos pos=new BlockPos(x,y,z);saved.put(pos,world.getBlockState(pos));
            world.setBlockState(pos,(y==199?Blocks.stone:Blocks.air).getDefaultState(),3);
        }
        try {
            ServerElytraFlight.clear();sent.clear();p.inventory.armorInventory[2]=new ItemStack(Item.getItemById(ClientItems.localItem(443,0)));
            p.capabilities.allowFlying=false;p.capabilities.isFlying=false;
            p.setPosition(5.5,200,5.5);p.onGround=true;p.motionX=p.motionY=0;p.motionZ=.3;
            p.rotationYaw=p.rotationYawHead=0;p.rotationPitch=0;p.setSprinting(true);
            p.movementInput=new net.minecraft.util.MovementInput();p.movementInput.moveForward=1;p.movementInput.jump=true;
            p.onLivingUpdate();
            require(p.posY>200.4&&p.motionZ>.25&&!ServerElytraFlight.flying(p),"Native sprint jump supplies takeoff momentum before glide: y="+p.posY+", speed="+p.motionZ);
            p.movementInput.jump=false;p.onLivingUpdate();
            p.movementInput.jump=true;p.onLivingUpdate();
            boolean modern=ServerSession.rule(ClientRule.PREDICT_ELYTRA_START);
            require(ServerElytraFlight.flying(p)==modern,"Rising double-tap starts locally only since 1.15");
            if(modern) {
                require(p.motionY>0&&p.height==.6F,"Rising activation preserves upward velocity and shrinks hitbox after travel");
                start(sent,wire);
                ((com.viaversion.viaforge.mixin.impl.mobs.MobSizeAccess)p).viaForge$size(.6F,1.8F);
                wire.flag(1,false,handler);p.movementInput.jump=false;ServerElytraFlight.input(p);
                p.movementInput.jump=true;ServerElytraFlight.input(p);
                require(ServerElytraFlight.flying(p),"Server rejection can be retried on next key edge, without a 20-tick lockout");
                require(p.height==1.8F,"Start action alone must not change the pre-travel collision box");
                ServerElytraFlight.updatePose(p);
                start(sent,wire);
            } else {
                p.motionY=-.1;p.movementInput.jump=false;ServerElytraFlight.input(p);
                p.movementInput.jump=true;ServerElytraFlight.input(p);start(sent,wire);
                require(!ServerElytraFlight.flying(p),"Old falling activation still waits for actual server flag");
            }
            wire.flag(1,true,handler);
            ServerElytraFlight.updatePose(p);
            // A ground flag is sent before the next server metadata arrives. The
            // following jump must still use gliding drag rather than ground friction.
            p.movementInput.jump=false;ServerElytraFlight.input(p);
            p.setPosition(5.5,200,5.5);p.onGround=true;p.motionX=p.motionY=0;p.motionZ=.8;
            p.setSprinting(true);p.movementInput.jump=true;p.onLivingUpdate();
            require(ServerElytraFlight.flying(p)&&p.motionZ>.95&&p.posY>200.39,
                    "Sprint re-jump keeps landing momentum while server flight flag is still set: "+p.motionZ+", "+p.posY);
            p.movementInput.jump=false;p.movementInput.moveForward=1;
            p.setPosition(5.5,200,5.5);p.onGround=true;p.motionX=p.motionY=p.motionZ=0;
            wire.flag(1,false,handler);
            require(p.height==.6F&&ServerElytraFlight.crawling(p)==ServerSession.rule(ClientRule.CRAWLING_POSE),"Cleared flight flag retains the original visually swimming pose until post-travel update");
            ServerElytraFlight.beforeInput(p);
            ServerElytraFlight.slowInput(p);
            if(ServerSession.rule(ClientRule.SQUARE_SWIM_INPUT)) {
                com.viaversion.viaforge.compatibility.ServerSwimming.applyMovementInput(p);
                require(Math.abs(p.moveForward-.294)<1e-6,"Original landing transition has exactly one slow input tick");
            }else require(Math.abs(p.movementInput.moveForward-(ServerSession.rule(ClientRule.CRAWLING_POSE)?.3:1))<1e-6,"Original versioned landing input slowdown");
            p.onLivingUpdate();
            require(p.isSprinting()&&p.height==1.8F,"Sprint survives confirmed landing in open space for the next jump");
            wire.flag(1,true,handler);ServerElytraFlight.updatePose(p);
            // A one-block tunnel: land, receive the server's stop, and remain low.
            for(int x=4;x<=8;x++)for(int z=4;z<=10;z++)world.setBlockState(new BlockPos(x,201,z),Blocks.stone.getDefaultState(),3);
            p.setPosition(5.5,200,5.5);p.onGround=true;p.motionX=p.motionY=p.motionZ=0;
            p.movementInput.jump=false;p.movementInput.moveForward=0;wire.flag(1,false,handler);p.onLivingUpdate();
            require(!ServerElytraFlight.flying(p)&&p.height==.6F&&p.getEyeHeight()==.4F,"Landing under ceiling never expands box into solid blocks");
            require(ServerElytraFlight.crawling(p)==ServerSession.rule(ClientRule.CRAWLING_POSE),"Only 1.14+ gives the retained small box an actual crawling pose");
            require(Math.abs(p.posX-5.5)<1e-7&&Math.abs(p.posZ-5.5)<1e-7,"Native headspace probe does not eject the lying player");
            if(ServerSession.rule(ClientRule.CRAWLING_POSE)) {
                p.movementInput.moveForward=1;ServerElytraFlight.beforeInput(p);ServerElytraFlight.slowInput(p);
                if(ServerSession.rule(ClientRule.SQUARE_SWIM_INPUT)) {
                    require(p.movementInput.moveForward==1,"Since 1.21.5 raw input stays available for sprint decisions");
                    com.viaversion.viaforge.compatibility.ServerSwimming.applyMovementInput(p);
                    require(Math.abs(p.moveForward-.294)<1e-6,"Crawling slowdown applies at travel input since 1.21.5");
                }else require(Math.abs(p.movementInput.moveForward-.3)<1e-6,"Crawling uses vanilla slow input without requiring sneak");
                for(int i=0;i<12;i++)ServerElytraVisuals.tick(p);
                require(ServerElytraVisuals.crawlAmount(p,1)==1,"Crawling animation reaches the horizontal pose");
                net.minecraft.client.model.ModelPlayer model=new net.minecraft.client.model.ModelPlayer(0,false);
                p.isSwingInProgress=false;model.setRotationAngles(18,1,p.ticksExisted+1,0,30,.0625F,p);
                float head=-(float)Math.PI/4;
                require(Math.abs(model.bipedHead.rotateAngleX-head)<1e-6,"Crawling head follows version-specific original model");
                require(Math.abs(model.bipedRightArm.rotateAngleX-(float)Math.PI/4)<1e-6,"Native player model uses swimming stroke while crawling");
                require(model.bipedRightArmwear.rotateAngleX==model.bipedRightArm.rotateAngleX,"Crawling skin sleeves follow arm animation");
                net.minecraft.client.renderer.entity.RenderPlayer armRender=new net.minecraft.client.renderer.entity.RenderPlayer(mc.getRenderManager());
                armRender.renderRightArm(p);armRender.renderLeftArm(p);
                require(Math.abs(armRender.getMainModel().bipedRightArm.rotateAngleY)<1e-6
                        &&Math.abs(armRender.getMainModel().bipedLeftArm.rotateAngleY)<1e-6,"Fully blended crawl stroke does not leak into either first-person arm");
                for(int x=4;x<=8;x++)for(int z=4;z<=10;z++)world.setBlockState(new BlockPos(x,201,z),Blocks.stone_slab.getStateFromMeta(8),3);
                ServerElytraFlight.updatePose(p);
                require(p.height==1.5F&&p.getEyeHeight()==1.27F&&!ServerElytraFlight.crawling(p),"Half-block extra headroom selects crouching, without standing into ceiling");
            }
            p.setPosition(12,200,5);p.onGround=false;p.movementInput.moveForward=0;ServerElytraFlight.input(p);ServerElytraFlight.updatePose(p);
            require(p.height==1.8F&&!ServerElytraFlight.crawling(p),"Leaving low tunnel restores standing camera and box");
            // Creative double-tap keeps its native priority over Elytra start.
            ServerElytraFlight.clear();p.setPosition(12,230,5);p.onGround=false;p.motionX=p.motionY=p.motionZ=0;
            p.capabilities.allowFlying=true;p.movementInput.jump=false;
            java.lang.reflect.Field toggle=net.minecraft.entity.player.EntityPlayer.class.getDeclaredField("flyToggleTimer");toggle.setAccessible(true);toggle.setInt(p,0);
            final boolean[] key={false};
            p.movementInput=new net.minecraft.util.MovementInput(){@Override public void updatePlayerMoveState(){jump=key[0];}};
            p.onLivingUpdate();key[0]=true;p.onLivingUpdate();
            key[0]=false;p.onLivingUpdate();key[0]=true;p.onLivingUpdate();
            require(p.capabilities.isFlying&&!ServerElytraFlight.flying(p),"Native creative double-tap stays usable with Elytra equipped: creative="+p.capabilities.isFlying+", ground="+p.onGround+", timer="+toggle.getInt(p)+", y="+p.posY);
            p.capabilities.isFlying=false;
        } finally {
            mc.setRenderViewEntity(oldCamera);
            ServerElytraFlight.clear();
            for(Map.Entry<BlockPos,IBlockState> entry:saved.entrySet())world.setBlockState(entry.getKey(),entry.getValue(),3);
        }
    }
    private static void start(List<Packet> sent,ElytraFlightSmokeTest.Wire wire)throws Exception {
        C17PacketCustomPayload found=null;
        for(Iterator<Packet> it=sent.iterator();it.hasNext();) {
            Packet packet=it.next();
            if(packet instanceof C17PacketCustomPayload&&((C17PacketCustomPayload)packet).getChannelName().equals("VF|elytra")) {
                require(found==null,"One flight action per input edge");found=(C17PacketCustomPayload)packet;it.remove();
            }
        }
        require(found!=null,"Actual input emits START_FALL_FLYING");wire.start(found);
    }
    private ElytraMovementSmokeTest() { }
}
