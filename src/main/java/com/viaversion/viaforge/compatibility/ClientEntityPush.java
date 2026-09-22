package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.common.compatibility.EntityPushRules;
import com.viaversion.viaforge.common.compatibility.EntityPushRules.TeamRule;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;

/** One push step in the other entity's original tick; never a second player simulation. */
public final class ClientEntityPush {
    public static void team(ByteBuf input) {
        String name=Types.STRING.read(input);
        TeamRule rule=TeamRule.read(Types.STRING.read(input));
        ScorePlayerTeam team=Minecraft.getMinecraft().theWorld.getScoreboard().getTeam(name);
        if(team!=null)((CollisionTeam)team).viaForge$collisionRule(rule);
    }
    private static ScorePlayerTeam team(Entity entity) {
        return entity.worldObj.getScoreboard().getPlayersTeam(entity instanceof EntityPlayer?entity.getName():entity.getUniqueID().toString());
    }
    private static TeamRule rule(ScorePlayerTeam team) {
        return team==null?TeamRule.ALWAYS:((CollisionTeam)team).viaForge$collisionRule();
    }
    private static boolean pushable(EntityLivingBase entity) {
        return entity.canBePushed()&&entity.isEntityAlive()&&!entity.isOnLadder()&&!(entity instanceof EntityPlayer&&((EntityPlayer)entity).isSpectator());
    }
    private static Entity root(Entity entity) {
        java.util.Set<Entity> seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Entity,Boolean>());
        while(entity.ridingEntity!=null&&seen.add(entity))entity=entity.ridingEntity;
        return entity;
    }
    public static void tick(EntityLivingBase source) {
        if(!source.worldObj.isRemote||!ServerSession.rule(ClientRule.CLIENT_ENTITY_PUSH))return;
        EntityPlayerSP player=Minecraft.getMinecraft().thePlayer;
        if(player==null||player==source||player.worldObj!=source.worldObj||!pushable(player))return;
        if(!source.getEntityBoundingBox().intersectsWith(player.getEntityBoundingBox()))return;
        ScorePlayerTeam own=team(source),other=team(player);
        if(!EntityPushRules.collides(rule(own),rule(other),own!=null&&own.isSameTeam(other)))return;
        if(source.noClip||player.noClip||root(source)==root(player))return;
        if(ServerSession.rule(ClientRule.SLEEPING_PUSH_IMMUNITY)&&player.isPlayerSleeping())return;
        double[] push=EntityPushRules.impulse(source.posX-player.posX,source.posZ-player.posZ,
                player.entityCollisionReduction,ServerSession.rule(ClientRule.PRECISE_ENTITY_PUSH));
        if(push==null)return;
        if(player.riddenByEntity==null)player.addVelocity(-push[0],0,-push[1]);
        if(source.riddenByEntity==null&&(!ServerSession.rule(ClientRule.PUSHABLE_IMPULSE_TARGETS)||pushable(source)))
            source.addVelocity(push[0],0,push[1]);
    }
    private ClientEntityPush(){}
}
