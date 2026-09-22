package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.*;

/** Original living-entity coordinates; Via still maintains its own trackers once. */
public final class ClientEntityMotion {
    private static final Map<Entity,double[]> POSITIONS=new IdentityHashMap<>();
    public static void clear(){POSITIONS.clear();}
    public static void remove(int id){POSITIONS.keySet().removeIf(e->e.getEntityId()==id);}
    public static boolean tracks(Entity entity){return POSITIONS.containsKey(entity);}
    public static void spawn(Entity entity,double x,double y,double z,float yaw,float pitch) {
        if(!(entity instanceof EntityLivingBase)||!ServerSession.rule(ClientRule.CLIENT_ENTITY_PUSH))return;
        remove(entity.getEntityId());POSITIONS.put(entity,new double[]{x,y,z});
        entity.setPositionAndRotation(x,y,z,yaw,pitch);
        entity.prevPosX=entity.lastTickPosX=x;entity.prevPosY=entity.lastTickPosY=y;entity.prevPosZ=entity.lastTickPosZ=z;
        entity.prevRotationYaw=yaw;entity.prevRotationPitch=pitch;
    }
    public static void motion(WorldClient world,int operation,ByteBuf input) {
        Entity entity=world.getEntityByID(Types.VAR_INT.readPrimitive(input));double[] base=POSITIONS.get(entity);
        if(base==null)return;
        boolean teleport=operation==33,position=operation==30||operation==31;
        boolean separate=ServerSession.rule(ClientRule.ROTATION_ONLY_ENTITY_UPDATE);
        boolean keepZero=ServerSession.rule(ClientRule.KEEP_ZERO_ENTITY_DELTA),round=ServerSession.rule(ClientRule.ROUND_ENTITY_DELTA);
        if(teleport){for(int i=0;i<3;i++)base[i]=input.readDouble();}
        else if(position||!separate)for(int i=0;i<3;i++)base[i]=EntityPositionRules.relative(base[i],position?input.readShort():0,keepZero,round);
        float yaw=entity.rotationYaw,pitch=entity.rotationPitch;
        if(operation!=30){yaw=input.readByte()*360F/256F;pitch=input.readByte()*360F/256F;}
        boolean ground=input.readBoolean();
        double x=base[0],y=base[1],z=base[2];int steps=3;
        if(!teleport&&!position&&separate){x=entity.posX;y=entity.posY;z=entity.posZ;}
        // The old client deliberately retains the current position for tiny teleports.
        // This test disappeared in 1.16.2. The constants here are original vanilla values.
        if(teleport&&!keepZero&&Math.abs(entity.posX-x)<.03125&&Math.abs(entity.posY-y)<.015625&&Math.abs(entity.posZ-z)<.03125){
            x=entity.posX;y=entity.posY;z=entity.posZ;steps=separate?3:0;
        }
        entity.setPositionAndRotation2(x,y,z,yaw,pitch,steps,teleport);entity.onGround=ground;
        // Keep native fields coherent for code which reads them; they are no longer
        // the source of movement. No rounded packet can overwrite this interpolation.
        entity.serverPosX=(int)Math.floor(base[0]*32);entity.serverPosY=(int)Math.floor(base[1]*32);entity.serverPosZ=(int)Math.floor(base[2]*32);
    }
    private ClientEntityMotion(){}
}
