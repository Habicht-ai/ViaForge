package com.viaversion.viaforge.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind;
import com.viaversion.viaforge.mixin.impl.mobs.MobSizeAccess;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.Vec3;
import com.viaversion.viaforge.compatibility.ServerSwimming;

/** Main-thread vanilla flight prediction, reconciled by the original server metadata. */
public final class ServerElytraFlight {
    private static EntityPlayerSP owner;
    private static boolean active, previousJump, creativeBeforeInput, crawlingPose;
    private static int flyingTicks;
    public static void clear() {
        ServerElytraVisuals.clear();
        if (owner != null && owner.isEntityAlive() && !owner.isPlayerSleeping()) ((MobSizeAccess)owner).viaForge$size(.6F, 1.8F);
        owner = null; active = previousJump = creativeBeforeInput = crawlingPose = false; flyingTicks = 0;
    }
    public static boolean equipped(EntityPlayer player) {
        ItemStack stack = player.getCurrentArmor(2);
        return ClientItems.is(stack, Kind.ELYTRA) && ElytraPhysics.usable(stack.getItemDamage(), stack.getMaxDamage());
    }
    private static boolean eligible(EntityPlayer player) {
        return ServerSession.has(ClientFeature.ELYTRA) && player.isEntityAlive() && equipped(player)
                && !player.isInWater() && !player.isInLava() && !player.isRiding()
                && !player.capabilities.isFlying && !player.isSpectator();
    }
    public static void metadata(int id, boolean flying) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player != null && player.getEntityId() == id) {
            if (owner != player) clear();
            owner = player; active = flying;
            if (!flying) flyingTicks = 0;
        }
    }
    public static boolean flying(Entity entity) {
        if (!(entity instanceof EntityPlayer) || !ServerSession.has(ClientFeature.ELYTRA)) return false;
        if (entity == Minecraft.getMinecraft().thePlayer) return entity == owner && active && eligible((EntityPlayer)entity);
        ServerEntityViews.View view = ServerEntityViews.get(entity.getEntityId());
        return view != null && view.fallFlying;
    }
    public static int ticks() { return flyingTicks; }
    /** Run before native creative double-tap handling, without changing any abilities. */
    public static void beforeInput(EntityPlayerSP player) {
        ServerSwimming.beforeInput(player);
        creativeBeforeInput = player.capabilities.isFlying;
    }
    public static void slowInput(EntityPlayerSP player) {
        ServerSwimming.afterInput(player);
    }
    public static void input(EntityPlayerSP player) {
        ServerSwimming.input(player);
        if (!ServerSession.has(ClientFeature.ELYTRA)) { if (owner != null) clear(); return; }
        if (owner != player) { clear(); owner = player; }
        boolean jump = player.movementInput.jump;
        if (!eligible(player) || player.isOnLadder()) { active = false; flyingTicks = 0; }
        boolean predicts = ServerSession.rule(ClientRule.PREDICT_ELYTRA_START);
        if (jump && !previousJump && !active && eligible(player) && !player.onGround && !player.isOnLadder()
                && creativeBeforeInput == player.capabilities.isFlying && (predicts || player.motionY < 0)) {
            PacketBuffer data = new PacketBuffer(Unpooled.buffer());
            data.writeVarIntToBuffer(player.getEntityId());
            player.sendQueue.addToSendQueue(new C17PacketCustomPayload("VF|elytra", data));
            // Since 1.15 tryToStartFallFlying sets the local flag before the
            // response. Earlier clients only request it. Neither has a cooldown.
            if (predicts) active = true;
        }
        previousJump = jump;
        if (flying(player)) flyingTicks++; else flyingTicks = 0;
        // Player dimensions are updated after travel, like Player.tick in the
        // target clients. Shrinking here changes takeoff/ceiling collisions;
        // expanding here changes the first movement after a landing flag.
    }
    public static boolean crawling(Entity entity) {
        return entity == owner && ServerSession.rule(ClientRule.CRAWLING_POSE)
                && crawlingPose && !entity.isInWater() && !flying(entity) && entity.isEntityAlive();
    }
    public static boolean compact(Entity entity) {
        return entity == owner && ServerSession.has(ClientFeature.ELYTRA) && entity.height == .6F;
    }
    public static boolean crouching(Entity entity) {
        return entity == owner && ServerSession.has(ClientFeature.ELYTRA) && (entity.height == 1.5F || entity.height == 1.65F);
    }
    public static boolean fits(EntityPlayer player, float height) {
        double r = player.width / 2.0;
        net.minecraft.util.AxisAlignedBB box = new net.minecraft.util.AxisAlignedBB(
                player.posX-r,player.posY,player.posZ-r,player.posX+r,player.posY+height,player.posZ+r);
        return player.worldObj.getCollidingBoundingBoxes(player,box.contract(1e-7,1e-7,1e-7)).isEmpty();
    }
    public static void updatePose(EntityPlayerSP player) {
        if (!ServerSession.has(ClientFeature.ELYTRA) || !player.isEntityAlive() || player.isPlayerSleeping()) return;
        boolean modern = ServerSession.rule(ClientRule.CRAWLING_POSE);
        float crouch = modern ? 1.5F : 1.65F;
        float height = flying(player) || ServerSwimming.swimming(player) ? .6F : player.isSneaking() && !player.capabilities.isFlying ? crouch : 1.8F;
        if (modern && !player.isSpectator() && !player.isRiding()) {
            if (!fits(player,.6F)) return;
            if (!fits(player,height)) height = fits(player,crouch) ? crouch : .6F;
        }
        if (height != player.height && (player.isSpectator() || player.isRiding() || fits(player,height))) {
            ((MobSizeAccess)player).viaForge$size(.6F,height);
        }
        // FALL_FLYING and SWIMMING have the same dimensions, but only the
        // latter is crawling. A received landing flag does not change the pose
        // until this post-travel update. Inferring it from height prematurely
        // slows the input and cancels sprint during successive landing/jumps.
        // Retain the SWIMMING pose separately from the current swimming flag.
        // On the first dry tick vanilla still has this pose and uses crawling input.
        crawlingPose = modern && height == .6F && !flying(player);
    }
    public static boolean move(EntityPlayer player) {
        if (player != Minecraft.getMinecraft().thePlayer || !flying(player)) return false;
        Vec3 look = player.getLookVec();
        double[] velocity = ElytraPhysics.step(player.motionX, player.motionY, player.motionZ, look.xCoord, look.yCoord, look.zCoord, player.rotationPitch);
        player.motionX = velocity[0]; player.motionY = velocity[1]; player.motionZ = velocity[2];
        if (ServerSession.rule(ClientRule.ELYTRA_FIREWORKS)) {
            for (int i = 0; i < ServerEntityViews.boosts(player.getEntityId()); i++) {
                player.motionX = ElytraPhysics.boost(player.motionX, look.xCoord);
                player.motionY = ElytraPhysics.boost(player.motionY, look.yCoord);
                player.motionZ = ElytraPhysics.boost(player.motionZ, look.zCoord);
            }
        }
        if (player.motionY > -.5) player.fallDistance = 1;
        player.moveEntity(player.motionX, player.motionY, player.motionZ);
        // The replacement travel path must still advance the native walk-animation
        // inputs; ModelBiped applies the original flight-speed damping afterwards.
        player.prevLimbSwingAmount = player.limbSwingAmount;
        double dx = player.posX - player.prevPosX, dz = player.posZ - player.prevPosZ;
        float amount = Math.min(1, net.minecraft.util.MathHelper.sqrt_double(dx * dx + dz * dz) * 4);
        player.limbSwingAmount += (amount - player.limbSwingAmount) * .4F;
        player.limbSwing += player.limbSwingAmount;
        // Collision resolution stays native; impact damage and durability stay server-owned.
        // Vanilla clients retain the flag on ground contact until the server
        // clears it. Clearing here loses momentum on short landing/jump cycles.
        if (!eligible(player)) { active = false; flyingTicks = 0; }
        return true;
    }
    private ServerElytraFlight() { }
}
