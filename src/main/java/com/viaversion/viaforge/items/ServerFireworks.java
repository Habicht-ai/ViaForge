package com.viaversion.viaforge.items;

import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.common.compatibility.ElytraPhysics;
import com.viaversion.viaforge.common.compatibility.OriginalLookMath;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.hands.Offhand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/** Original attachment behavior: each rocket applies its boost in its own entity tick. */
public final class ServerFireworks {
    public static boolean attached(Entity rocket) {
        if (!ServerSession.rule(ClientRule.ELYTRA_FIREWORKS)) return false;
        ServerEntityViews.View view = ServerEntityViews.get(rocket.getEntityId());
        return view != null && view.type == 76 && view.boostedEntity >= 0;
    }
    public static void move(EntityFireworkRocket rocket, double x, double y, double z) {
        if (!attached(rocket)) { rocket.moveEntity(x, y, z); return; }
        Entity entity = rocket.worldObj.getEntityByID(ServerEntityViews.get(rocket.getEntityId()).boostedEntity);
        if (!(entity instanceof EntityLivingBase)) { rocket.motionX = rocket.motionY = rocket.motionZ = 0; return; }
        if(entity==net.minecraft.client.Minecraft.getMinecraft().thePlayer&&ServerElytraFlight.flying(entity)) {
            double[] look=OriginalLookMath.look(entity.rotationYaw,entity.rotationPitch,ServerSession.rules());
            entity.motionX=ElytraPhysics.boost(entity.motionX,look[0]);
            entity.motionY=ElytraPhysics.boost(entity.motionY,look[1]);
            entity.motionZ=ElytraPhysics.boost(entity.motionZ,look[2]);
        }
        double dx = 0, dz = 0;
        if (ServerSession.rule(ClientRule.FIREWORK_HAND_TRAIL) && entity instanceof EntityPlayer && ServerElytraFlight.flying(entity)) {
            EntityPlayer player = (EntityPlayer)entity;
            boolean offhandOnly = rocket(Offhand.of(player)) && !rocket(player.getHeldItem());
            boolean left = Offhand.mainLeft(player) ^ offhandOnly;
            double yaw = (player.rotationYaw + (left ? -80 : 80)) * Math.PI / 180;
            dx = -Math.sin(yaw) * .5; dz = Math.cos(yaw) * .5;
        }
        rocket.setPosition(entity.posX + dx, entity.posY, entity.posZ + dz);
        rocket.motionX = entity.motionX; rocket.motionY = entity.motionY; rocket.motionZ = entity.motionZ;
    }
    private static boolean rocket(ItemStack stack) { return stack != null && stack.getItem() == Items.fireworks; }
    private ServerFireworks() { }
}
