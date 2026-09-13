package com.viaversion.viaforge.boats;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.world.World;

/** Client half of ItemBoat's 1.9–1.12.2 use action. The server creates the entity. */
public final class BoatPlacement {
    public static boolean use(ItemStack stack, World world, EntityPlayer player) {
        if (stack.stackSize <= 0) return false;
        Vec3 eye = player.getPositionEyes(1), look = player.getLook(1);
        MovingObjectPosition hit = world.rayTraceBlocks(eye, eye.addVector(look.xCoord * 5, look.yCoord * 5, look.zCoord * 5), true);
        if (hit == null) return false;
        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(player,
                player.getEntityBoundingBox().addCoord(look.xCoord * 5, look.yCoord * 5, look.zCoord * 5).expand(1, 1, 1))) {
            float border = entity.getCollisionBorderSize();
            if (entity.canBeCollidedWith() && entity.getEntityBoundingBox().expand(border, border, border).isVecInside(eye)) return false;
        }
        if (hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        net.minecraft.block.Block block = world.getBlockState(hit.getBlockPos()).getBlock();
        boolean water = block == Blocks.water || block == Blocks.flowing_water;
        ServerBoat candidate = new ServerBoat(world, 107);
        candidate.setPosition(hit.hitVec.xCoord, hit.hitVec.yCoord - (water ? .12 : 0), hit.hitVec.zCoord);
        if (!world.getCollidingBoundingBoxes(candidate, candidate.getEntityBoundingBox().expand(-.1, -.1, -.1)).isEmpty()) return false;
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        int id = net.minecraft.item.Item.getIdFromItem(stack.getItem());
        if (id < StatList.objectUseStats.length && StatList.objectUseStats[id] != null) player.triggerAchievement(StatList.objectUseStats[id]);
        return true;
    }
    private BoatPlacement() { }
}
