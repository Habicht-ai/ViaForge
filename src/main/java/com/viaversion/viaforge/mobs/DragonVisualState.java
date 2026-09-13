package com.viaversion.viaforge.mobs;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.util.BlockPos;
public final class DragonVisualState {
    public static int phase(EntityDragon dragon) {
        MobState state=ServerMobs.get(dragon); return state == null ? -1 : state.number(state.first,10);
    }
    public static boolean sitting(int phase) { return phase >= 5 && phase <= 7; }
    public static float headOffset(EntityDragon dragon,int segment,double[] reference,double[] sample) {
        int phase=phase(dragon);
        if (phase == 3 || phase == 4) {
            BlockPos fountain=dragon.worldObj.getTopSolidOrLiquidBlock(new BlockPos(0,0,0));
            return segment/Math.max((float)Math.sqrt(dragon.getDistanceSq(fountain))/4,1);
        }
        return sitting(phase) ? segment : segment == 6 ? 0 : (float)(sample[1]-reference[1]);
    }
    public static void breath(EntityDragon dragon, int phase, int ticks) {
        if (phase != 3 && (phase != 5 || ticks >= 10 || ticks % 2 != 0)) return;
        float pitch = dragon.rotationPitch;
        if (phase == 3) {
            BlockPos fountain = dragon.worldObj.getTopSolidOrLiquidBlock(new BlockPos(0, 0, 0));
            dragon.rotationPitch = -45 / Math.max((float)Math.sqrt(dragon.getDistanceSq(fountain)) / 4, 1);
        } else dragon.rotationPitch = -45;
        net.minecraft.util.Vec3 look = dragon.getLook(1).normalize();
        dragon.rotationPitch = pitch;
        // The original client's rotateYaw calls discard their returned vectors.
        for (int i = 0; i < 8; i++) {
            double x = dragon.dragonPartHead.posX + dragon.worldObj.rand.nextGaussian() / 2;
            double y = dragon.dragonPartHead.posY + dragon.dragonPartHead.height / 2 + dragon.worldObj.rand.nextGaussian() / 2;
            double z = dragon.dragonPartHead.posZ + dragon.worldObj.rand.nextGaussian() / 2;
            if (phase == 3) MobParticles.spawn(dragon.worldObj, 42, x, y, z,
                    -look.xCoord * .08F + dragon.motionX, -look.yCoord * .3F + dragon.motionY, -look.zCoord * .08F + dragon.motionZ);
            else for (int j = 0; j < 6; j++) MobParticles.spawn(dragon.worldObj, 42, x, y, z,
                    -look.xCoord * .08F * j, -look.yCoord * .6F, -look.zCoord * .08F * j);
        }
    }
    private DragonVisualState() { }
}
