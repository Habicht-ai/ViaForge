package com.viaversion.viaforge.blocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ITickable;
import net.minecraft.util.EnumFacing;
import net.minecraft.entity.Entity;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;

/** Original client lid animation and collision push, driven by block events. */
public final class ShulkerBlockEntity extends TileEntity implements ITickable {
    private float previous, progress;
    private int status; // closed, opening, opened, closing
    private static Entity pushed;
    public static boolean pushing(Entity entity){return entity==pushed;}
    @Override public boolean receiveClientEvent(int id, int value) {
        if (id != 1) return super.receiveClientEvent(id, value);
        if(value==0)status=3;
        else if(value==1)status=1;
        return true;
    }
    @Override public void update() {
        previous = progress;
        boolean modern=ServerSession.rule(ClientRule.SHULKER_DELTA_PUSH);
        if(status==0)progress=0;
        else if(status==2)progress=1;
        else if(status==1) {
            progress+=.1F;
            if(progress>=1) {
                if(!modern)pushEntities(false);
                progress=1;status=2;
            }
            if(modern)pushEntities(true);
        }else {
            progress-=.1F;
            if(progress<=0){progress=0;status=0;}
        }
        if(!modern&&(status==1||status==3))pushEntities(false);
    }
    private void pushEntities(boolean modern) {
        if(worldObj==null||!(worldObj.getBlockState(pos).getBlock() instanceof LegacyClientBlockTypes.Shulker))return;
        EnumFacing face=worldObj.getBlockState(pos).getValue(LegacyClientBlockTypes.Facing.FACING);
        double from=modern?previous:0,to=modern?progress:(double)(.5F*progress);
        double[] min={0,0,0},max={1,1,1};
        int axis=face.getAxis()==EnumFacing.Axis.X?0:face.getAxis()==EnumFacing.Axis.Y?1:2;
        int sign=face.getAxisDirection()==EnumFacing.AxisDirection.POSITIVE?1:-1;
        min[axis]=sign>0?1+from:-to;max[axis]=sign>0?1+to:-from;
        AxisAlignedBB swept=new AxisAlignedBB(pos.getX()+min[0],pos.getY()+min[1],pos.getZ()+min[2],pos.getX()+max[0],pos.getY()+max[1],pos.getZ()+max[2]);
        for(Entity entity:worldObj.getEntitiesWithinAABBExcludingEntity(null,swept)) {
            if(entity.noClip)continue;
            AxisAlignedBB box=entity.getEntityBoundingBox();
            double[] eMin={box.minX-pos.getX(),box.minY-pos.getY(),box.minZ-pos.getZ()},eMax={box.maxX-pos.getX(),box.maxY-pos.getY(),box.maxZ-pos.getZ()};
            double distance=(modern?max[axis]-min[axis]:sign>0?max[axis]-eMin[axis]:eMax[axis]-min[axis])+.01;
            Entity old=pushed;pushed=entity;
            try{entity.moveEntity(distance*face.getFrontOffsetX(),distance*face.getFrontOffsetY(),distance*face.getFrontOffsetZ());}
            finally{pushed=old;}
        }
    }
    public float progress(float partialTicks) { return previous + (progress - previous) * partialTicks; }
    @Override public AxisAlignedBB getRenderBoundingBox() { return new AxisAlignedBB(getPos().add(-1, -1, -1), getPos().add(2, 2, 2)); }
}
