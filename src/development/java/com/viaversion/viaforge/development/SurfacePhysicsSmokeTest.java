package com.viaversion.viaforge.development;

import com.viaversion.viaforge.blocks.*;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.compatibility.ServerSwimming;
import com.viaversion.viaforge.compatibility.ServerBubbleColumns;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.mixin.impl.mobs.MobSizeAccess;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Actual transformed moveEntity and ticking block entity, without another physics integrator. */
final class SurfacePhysicsSmokeTest {
    static void verify(WorldClient world,EntityPlayerSP player) throws Exception {
        Map<BlockPos,IBlockState> saved=new HashMap<>();
        for(int x=4;x<=8;x++)for(int z=4;z<=7;z++)for(int y=199;y<=202;y++) {
            BlockPos p=new BlockPos(x,y,z);saved.put(p,world.getBlockState(p));
            world.setBlockState(p,(y==199?Blocks.slime_block:Blocks.air).getDefaultState(),0);
        }
        EntityPig pig=null;
        try {
            if(ServerSession.profile().serverProtocol()>=773) {
                ByteBuf retained=Unpooled.buffer();
                double oldX=player.posX,oldY=player.posY,oldZ=player.posZ;
                try {
                    com.viaversion.viaforge.common.compatibility.ClientEventEnvelope.write(retained,
                            com.viaversion.viaforge.common.compatibility.ClientEventFormat.legacy(340),37);
                    com.viaversion.viaversion.api.type.Types.VAR_INT.writePrimitive(retained,player.getEntityId());
                    retained.writeDouble(4.25).writeDouble(-8.125).writeDouble(.34779954831227555);
                    com.viaversion.viaforge.items.ServerEntityViews.accept(retained);
                    require(player.motionX==4.25&&player.motionY==-8.125&&player.motionZ==.34779954831227555,"Native player receives original unrounded, unclipped motion");
                    require(player.posX==oldX&&player.posY==oldY&&player.posZ==oldZ,"Velocity update does not run an extra movement step");
                } finally {retained.release();}
            }
            player.movementInput=new MovementInput();player.noClip=false;
            player.capabilities.isFlying=false;player.capabilities.allowFlying=false;
            player.setPosition(5.5,200.25,5.5);player.motionX=player.motionZ=0;player.motionY=-1;
            player.moveEntity(0,-1,0);
            double expected=ServerSession.rule(ClientRule.COLLISION_PORTION_BOUNCE)?1.014900004863739:1;
            require(player.posY==200&&Math.abs(player.motionY-expected)<1e-12,"Actual slime collision uses original rebound: "+player.motionY);
            player.movementInput.sneak=true;player.setPosition(5.5,200.25,5.5);player.motionY=-1;
            player.moveEntity(0,-1,0);
            require(player.posY==200&&player.motionY==0,"Sneak suppresses actual slime rebound");player.movementInput.sneak=false;
            player.setPosition(5.5,200.02,5.5);player.motionY=-.08;
            player.moveEntity(0,-.08,0);
            require((player.motionY==0)==ServerSession.rule(ClientRule.SUPPRESS_GRAVITY_EQUAL_BOUNCE),"26.3 also suppresses rebound at exactly one gravity step");
            if(ServerSession.rule(ClientRule.PRECISE_BLOCK_EFFECTS))bubbleContacts(world,player);
            IBlockState shulker=Block.BLOCK_STATE_IDS.getByValue(ClientBlocks.localState(219<<4|5));
            if(shulker==null||!ServerBlockSession.supports(ClientBlocks.definition(shulker.getBlock())))return;
            BlockPos pos=new BlockPos(5,200,5);world.setBlockState(pos,shulker,0);
            ShulkerBlockEntity tile=(ShulkerBlockEntity)world.getTileEntity(pos);
            pig=new EntityPig(world);pig.setEntityId(993);pig.setPosition(6.25,200,5.5);world.addEntityToWorld(993,pig);
            tile.receiveClientEvent(1,1);
            for(int tick=0;tick<10;tick++) {
                double before=pig.posX;tile.update();
                if(ServerSession.rule(ClientRule.SHULKER_DELTA_PUSH))require(Math.abs(pig.posX-before-.11)<1e-6,"Opening lid pushes every original tick "+tick);
            }
            require(Math.abs(shulker.getBlock().getCollisionBoundingBox(world,pos,shulker).maxX-6.5)<1e-7,"Open lid participates in movement collisions");
            require(pig.posX>6.9,"Old and modern opening lids both displace overlapping entities");
            tile.receiveClientEvent(1,0);
            for(int tick=0;tick<10;tick++)tile.update();
            require(shulker.getBlock().getCollisionBoundingBox(world,pos,shulker).maxX==6,"Closed lid restores full cube");
            IBlockState upward=Block.BLOCK_STATE_IDS.getByValue(ClientBlocks.localState(219<<4|1));
            world.setBlockState(pos,upward,0);tile=(ShulkerBlockEntity)world.getTileEntity(pos);
            tile.receiveClientEvent(1,1);
            for(int tick=0;tick<4;tick++)tile.update();
            if(ServerSession.rule(ClientRule.EXPANDED_BLOCK_RAY)) {
                MovingObjectPosition hit=world.rayTraceBlocks(new Vec3(5.5,203,5.5),new Vec3(5.5,199,5.5),false,false,false);
                require(hit!=null&&hit.getBlockPos().equals(pos)&&hit.sideHit==EnumFacing.UP&&hit.hitVec.yCoord>201,"Whole modern ray hits expanded lid top, never its hidden underside");
                tile.receiveClientEvent(1,0);tile.update();
                hit=world.rayTraceBlocks(new Vec3(5.5,203,5.5),new Vec3(5.5,199,5.5),false,false,false);
                require(hit!=null&&hit.sideHit==EnumFacing.UP,"Closing lid keeps correct top-face ray intersection");
            }
        }finally {
            if(pig!=null)world.removeEntityFromWorld(993);
            for(Map.Entry<BlockPos,IBlockState> e:saved.entrySet())world.setBlockState(e.getKey(),e.getValue(),0);
            player.movementInput=new MovementInput();player.motionX=player.motionY=player.motionZ=0;
        }
    }

    private static void bubbleContacts(WorldClient world,EntityPlayerSP player) {
        Map<BlockPos,Integer> saved=new HashMap<>();
        float width=player.width,height=player.height;
        try {
            for(int y=200;y<=201;y++)for(int z=5;z<=6;z++) {
                BlockPos pos=new BlockPos(6,y,z);
                saved.put(pos,ServerSwimming.fluids.get(6,y,z));fluid(pos,17);
            }
            ((MobSizeAccess)player).viaForge$size(.6F,.6F);
            ServerBubbleColumns.clear();player.setPosition(6.5,200.7,5.85);
            player.motionY=.523227923;player.moveEntity(0,0,.2);
            ServerSwimming.bubbles(player);
            require(Math.abs(player.motionY-.843227923)<1e-12,
                    "Moving column exit applies both inside contacts before surface contacts: "+player.motionY);
            ServerBubbleColumns.clear();player.setPosition(6.5,200.65,5.5);
            player.motionY=.523227923;player.moveEntity(0,.05,.55);
            ServerSwimming.bubbles(player);
            require(Math.abs(player.motionY-.8)<1e-12,
                    "Y then horizontal contacts retain original per-axis caps and deduplicate blocks: "+player.motionY);
        }finally {
            for(Map.Entry<BlockPos,Integer> e:saved.entrySet())fluid(e.getKey(),Math.max(0,e.getValue()));
            ServerBubbleColumns.clear();((MobSizeAccess)player).viaForge$size(width,height);
        }
    }
    private static void fluid(BlockPos pos,int value) {
        ByteBuf input=Unpooled.buffer();
        try {ServerSwimming.fluids.accept(input.writeByte(1).writeInt(pos.getX()).writeInt(pos.getY()).writeInt(pos.getZ()).writeByte(value));}
        finally {input.release();}
    }
}
