package com.viaversion.viaforge.development;

import com.viaversion.viaforge.compatibility.*;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.items.*;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.multiplayer.WorldClient;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Actual mixed player/model classes: remote pose, skin overlay and fade restoration. */
final class SwimmingSmokeTest {
    static void verify(WorldClient world) {
        if(!ServerSession.rule(ClientRule.SWIMMING))return;
        EntityOtherPlayerMP player=new EntityOtherPlayerMP(world,new GameProfile(new UUID(0,985),"SwimVisual"));
        player.setEntityId(985);player.setPosition(8,200,8);
        try {
            ServerSwimming.metadata(player,1,ServerSession.rule(ClientRule.CRAWLING_POSE)?3:-1);
            ServerSwimming.remotePose(player);
            require(player.height==.6F&&player.getEyeHeight()==.4F,"Remote swimming box and eyes follow original pose");
            for(int tick=0;tick<13;tick++)ServerElytraVisuals.tick(player);
            require(ServerElytraVisuals.crawlAmount(player,1)==1,"Swimming animation reaches full amount");
            ModelPlayer model=new ModelPlayer(0,false);
            model.setRotationAngles(8,1,player.ticksExisted,0,0,.0625F,player);
            require(Math.abs(model.bipedHead.rotateAngleX+.7853982F)<1e-6,"Prone swimming head points ahead");
            float arc=1.8707964F*(-65*8+8*8)/(-65*14+14*14);
            require(Math.abs(Math.sin(model.bipedLeftArm.rotateAngleZ)-Math.sin((float)Math.PI+arc))<1e-6
                    &&Math.abs(model.bipedRightArm.rotateAngleZ-((float)Math.PI-arc))<1e-6,"Original asymmetric 26-phase swimming stroke");
            require(model.bipedLeftArmwear.rotateAngleZ==model.bipedLeftArm.rotateAngleZ,"Sleeve follows swimming arm");
            float before=model.bipedLeftArm.rotateAngleZ;
            model.setRotationAngles(18,1,player.ticksExisted,0,0,.0625F,player);
            require(model.bipedLeftArm.rotateAngleZ!=before,"Stroke advances with limb phase");
            ServerSwimming.metadata(player,0,0);ServerSwimming.remotePose(player);
            for(int tick=0;tick<12;tick++)ServerElytraVisuals.tick(player);
            require(ServerElytraVisuals.crawlAmount(player,1)==0&&ServerElytraVisuals.crawlAmount(player,0)>0,
                    "Last fade tick retains the previous animation sample for interpolation");
            ServerElytraVisuals.tick(player);
            require(ServerElytraVisuals.crawlAmount(player,0)==0,"Following tick clears the entire interpolated pose");
            model.setRotationAngles(0,0,player.ticksExisted,0,30,.0625F,player);
            require(player.height==1.8F&&ServerElytraVisuals.crawlAmount(player,1)==0,"Exit restores standing box and animation");
            require(Math.abs(model.bipedHead.rotateAngleX-(float)Math.PI/6)<1e-6,"Look pitch restored after leaving swimming");
        }finally{ServerSwimming.clear();ServerElytraVisuals.clear();}
    }
}
