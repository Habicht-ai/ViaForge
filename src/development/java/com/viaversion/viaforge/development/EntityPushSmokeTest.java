package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.compatibility.CollisionTeam;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.common.compatibility.EntityPushRules;
import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.BlockPos;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Real native entity ticks, including the separate remote-player override. */
final class EntityPushSmokeTest {
    static void verify(WorldClient world,EntityPlayerSP player) {
        Map<BlockPos,IBlockState> saved=new HashMap<>();
        for(int x=4;x<=7;x++)for(int z=4;z<=7;z++)for(int y=199;y<=202;y++) {
            BlockPos pos=new BlockPos(x,y,z);saved.put(pos,world.getBlockState(pos));
            world.setBlockState(pos,(y==199?Blocks.stone:Blocks.air).getDefaultState(),0);
        }
        net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getMinecraft();
        net.minecraft.entity.Entity oldCamera=mc.getRenderViewEntity();mc.setRenderViewEntity(player);
        try {
            EntityPig pig=new EntityPig(world);player.capabilities.allowFlying=false;player.capabilities.isFlying=false;
            reset(player,pig,.4,0);pig.onUpdate();
            double expected=EntityPushRules.impulse(pig.posX-player.posX,0,0,ServerSession.rule(ClientRule.PRECISE_ENTITY_PUSH))[0];
            require(player.motionX==-expected&&player.motionZ==0,"Mob tick applies exactly one original horizontal impulse");
            double x=player.posX;player.onUpdate();
            require(Math.abs(player.posX-(x-expected))<1e-12,"Received impulse travels on the next local tick: x="+player.posX+", expected="+(x-expected)+", motion="+player.motionX);
            reset(player,pig,.86,0);pig.onUpdate();require(player.motionX==0,"Nearby without box overlap does not push (no old .2 expansion)");
            reset(player,pig,.4,0);pig.setPosition(pig.posX,202,pig.posZ);pig.onUpdate();require(player.motionX==0,"No vertical box overlap does not push");
            reset(player,pig,.4,0);pig.noClip=true;pig.onUpdate();require(player.motionX==0,"No-clip source cannot push");pig.noClip=false;
            reset(player,pig,.4,0);net.minecraft.entity.item.EntityBoat boat=new net.minecraft.entity.item.EntityBoat(world);
            player.ridingEntity=boat;pig.ridingEntity=boat;
            com.viaversion.viaforge.compatibility.ClientEntityPush.tick(pig);
            require(player.motionX==0,"Entities sharing a vehicle never push each other");player.ridingEntity=null;pig.ridingEntity=null;
            reset(player,pig,.4,0);player.setHealth(0);pig.onUpdate();require(player.motionX==0,"Dead local receiver cannot be pushed");player.setHealth(20);
            reset(player,pig,.4,0);BlockPos ladder=new BlockPos(player);
            world.setBlockState(ladder,Blocks.ladder.getDefaultState(),0);pig.onUpdate();
            require(player.isOnLadder()&&player.motionX==0,"Climbing receiver is excluded by the original predicate");
            world.setBlockState(ladder,Blocks.air.getDefaultState(),0);
            EntityOtherPlayerMP remote=new EntityOtherPlayerMP(world,new GameProfile(new UUID(0,990),"PushPeer"));
            reset(player,remote,.4,0);remote.onUpdate();require(player.motionX==-expected,"Remote player pushes once after its interpolation tick");
            ScorePlayerTeam team=world.getScoreboard().createTeam("push_smoke");
            world.getScoreboard().addPlayerToTeam(player.getName(),team.getRegisteredName());
            ((CollisionTeam)team).viaForge$collisionRule(EntityPushRules.TeamRule.NEVER);
            reset(player,remote,.4,0);remote.onUpdate();require(player.motionX==0,"Native team object supplies retained collision rule");
            world.getScoreboard().removeTeam(team);
            reset(player,pig,2,0);player.motionX=.0042;player.motionZ=.0021;player.onUpdate();
            require(Math.abs(player.posX-5.5042)<1e-12,"Original X coast survives threshold");
            require(Math.abs(player.posZ-(ServerSession.rule(ClientRule.HORIZONTAL_MOTION_THRESHOLD)?5.5021:5.5))<1e-12,
                "Since 1.21.5, diagonal coast preserves a small axis while the horizontal vector survives");
            reset(player,pig,2,0);player.motionX=.0029;player.motionZ=.001;player.onUpdate();
            boolean vector=ServerSession.rule(ClientRule.HORIZONTAL_MOTION_THRESHOLD);
            require(Math.abs(player.posX-(vector?5.5029:5.5))<1e-12&&Math.abs(player.posZ-(vector?5.501:5.5))<1e-12,
                "Combined magnitude can survive even when both individual axes are below .003");
            reset(player,pig,2,0);player.motionX=.002;player.motionZ=.002;player.onUpdate();
            require(player.posX==5.5&&player.posZ==5.5,"Below the original squared threshold both axes stop");
            reset(player,remote,.4,0);remote.onUpdate();require(player.motionX==-expected,"Team removal discards its old rule");
            team=world.getScoreboard().createTeam("push_smoke");
            require(((CollisionTeam)team).viaForge$collisionRule()==EntityPushRules.TeamRule.ALWAYS,"Recreated team starts with original default");
            world.getScoreboard().removeTeam(team);
        } finally {
            ScorePlayerTeam team=world.getScoreboard().getTeam("push_smoke");if(team!=null)world.getScoreboard().removeTeam(team);
            for(Map.Entry<BlockPos,IBlockState> e:saved.entrySet())world.setBlockState(e.getKey(),e.getValue(),0);
            mc.setRenderViewEntity(oldCamera);player.setHealth(20);player.motionX=player.motionY=player.motionZ=0;
        }
    }
    private static void reset(EntityPlayerSP player,EntityLivingBase other,double dx,double dz) {
        player.setPosition(5.5,200,5.5);player.motionX=player.motionY=player.motionZ=0;player.onGround=true;
        player.movementInput=new net.minecraft.util.MovementInput();
        other.setPosition(5.5+dx,200,5.5+dz);other.motionX=other.motionY=other.motionZ=0;other.onGround=true;
    }
}
