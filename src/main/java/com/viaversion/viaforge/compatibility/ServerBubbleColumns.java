package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.ClientRule;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;

/** Original axis-ordered block contacts, retained until the post-travel effect stage. */
public final class ServerBubbleColumns {
    private static Entity owner;
    private static final List<Movement> movements=new ArrayList<>();
    private static final class Movement {
        final Vec3 from,to;final boolean zFirst;
        Movement(Vec3 from,Vec3 to,boolean zFirst){this.from=from;this.to=to;this.zFirst=zFirst;}
    }
    public static void clear(){owner=null;movements.clear();}
    public static void record(Entity entity,Vec3 from,boolean zFirst){
        if(entity!=Minecraft.getMinecraft().thePlayer||!ServerSession.rule(ClientRule.PRECISE_BLOCK_EFFECTS))return;
        if(owner!=entity){clear();owner=entity;}
        Vec3 to=new Vec3(entity.posX,entity.posY,entity.posZ);
        if(from.squareDistanceTo(to)==0)return;
        if(movements.size()>=100){Movement first=movements.remove(0),second=movements.remove(0);movements.add(0,new Movement(first.from,second.to,second.zFirst));}
        movements.add(new Movement(from,to,zFirst));
    }
    public static void apply(Entity entity){
        Set<BlockPos> visited=new HashSet<>();
        if(owner!=entity||movements.isEmpty())scan(entity,new Vec3(entity.posX,entity.posY,entity.posZ),new Vec3(entity.posX,entity.posY,entity.posZ),visited);
        else for(Movement movement:movements){
            int remaining=16;
            Vec3 from=movement.from,to=movement.to;
            if(from.yCoord!=to.yCoord){Vec3 next=new Vec3(from.xCoord,to.yCoord,from.zCoord);remaining-=scan(entity,from,next,visited,remaining);from=next;}
            if(movement.zFirst){
                if(from.zCoord!=to.zCoord){Vec3 next=new Vec3(from.xCoord,from.yCoord,to.zCoord);remaining-=scan(entity,from,next,visited,remaining);from=next;}
                if(from.xCoord!=to.xCoord)remaining-=scan(entity,from,to,visited,remaining);
            }else{
                if(from.xCoord!=to.xCoord){Vec3 next=new Vec3(to.xCoord,from.yCoord,from.zCoord);remaining-=scan(entity,from,next,visited,remaining);from=next;}
                if(from.zCoord!=to.zCoord)remaining-=scan(entity,from,to,visited,remaining);
            }
            if(remaining<=0)scan(entity,to,to,visited,1);
        }
        movements.clear();
    }
    private static int scan(Entity entity,Vec3 from,Vec3 to,Set<BlockPos> visited){return scan(entity,from,to,visited,16);}
    private static int scan(Entity entity,Vec3 from,Vec3 to,Set<BlockPos> visited,int limit){
        if(limit<=0)return 1;
        double epsilon=(double)1e-5F;
        AxisAlignedBB end=entity.getEntityBoundingBox().offset(to.xCoord-entity.posX,to.yCoord-entity.posY,to.zCoord-entity.posZ).contract(epsilon,epsilon,epsilon);
        double[] delta={to.xCoord-from.xCoord,to.yCoord-from.yCoord,to.zCoord-from.zCoord};
        boolean still=from.squareDistanceTo(to)<(double)(1e-5F*1e-5F);
        Contact contact=new Contact(entity,end,visited,from.squareDistanceTo(to)>0.9999900000002526*0.9999900000002526,delta,still);
        if(still){contact.box(end);return 1;}
        contact.box(end.offset(-delta[0],-delta[1],-delta[2]));
        int axis=delta[1]!=0?1:delta[0]!=0?0:2,sign=delta[axis]>0?1:-1;
        // BlockGetter's furthest corner, specialized to one recorded move axis.
        int[] corner=axis==0?new int[]{1,-1,-sign}:new int[]{-1,-(axis==2?sign:1),axis==1?sign:1};
        double[] size={end.maxX-end.minX,end.maxY-end.minY,end.maxZ-end.minZ};
        double[] min={end.minX,end.minY,end.minZ},max={end.maxX,end.maxY,end.maxZ};
        double[] origin=new double[3];int[] cell=new int[3];
        for(int i=0;i<3;i++){origin[i]=(min[i]+max[i])*.5+size[i]*.5*corner[i]-delta[i];cell[i]=MathHelper.floor_double(origin[i]);}
        int iterations=0;
        while(true){
            cell[axis]+=sign;
            double plane=sign>0?cell[axis]:cell[axis]+1,t=(plane-origin[axis])/delta[axis];
            if(t>=1)break;
            if(t<=0)continue;
            if(++iterations>=limit)return limit;
            int[] opposite=new int[3];
            for(int i=0;i<3;i++){
                double hit=MathHelper.clamp_double(origin[i]+delta[i]*t,cell[i]+epsilon,cell[i]+1-epsilon);
                opposite[i]=MathHelper.floor_double(hit-size[i]*corner[i]);
            }
            contact.cells(cell,opposite);
        }
        if(iterations+1>=limit)return limit;
        contact.box(end);return iterations+2;
    }
    private static final class Contact {
        final Entity entity;final AxisAlignedBB end;final Set<BlockPos> visited,local=new HashSet<>();
        final boolean far,still;final double[] delta;
        Contact(Entity entity,AxisAlignedBB end,Set<BlockPos> visited,boolean far,double[] delta,boolean still){this.entity=entity;this.end=end;this.visited=visited;this.far=far;this.delta=delta;this.still=still;}
        void box(AxisAlignedBB box){cells(new int[]{MathHelper.floor_double(box.minX),MathHelper.floor_double(box.minY),MathHelper.floor_double(box.minZ)},new int[]{MathHelper.floor_double(box.maxX),MathHelper.floor_double(box.maxY),MathHelper.floor_double(box.maxZ)});}
        void cells(int[] a,int[] b){
            int[] min=new int[3],max=new int[3],p=new int[3];for(int i=0;i<3;i++){min[i]=Math.min(a[i],b[i]);max[i]=Math.max(a[i],b[i]);}
            // betweenClosed has X fastest; directional iteration has Y outermost.
            int[] order=still?new int[]{2,1,0}:Math.abs(delta[0])<Math.abs(delta[2])?new int[]{1,2,0}:new int[]{1,0,2};
            for(int i=0;i<=max[order[0]]-min[order[0]];i++)for(int j=0;j<=max[order[1]]-min[order[1]];j++)for(int k=0;k<=max[order[2]]-min[order[2]];k++){
                int[] offsets={i,j,k};for(int n=0;n<3;n++){int axis=order[n];p[axis]=!still&&delta[axis]<0?max[axis]-offsets[n]:min[axis]+offsets[n];}
                BlockPos pos=new BlockPos(p[0],p[1],p[2]);
                if(!local.add(pos)||ServerSwimming.fluids.get(p[0],p[1],p[2])<17||!visited.add(pos))continue;
                if(far||end.intersectsWith(new AxisAlignedBB(pos,pos.add(1,1,1))))ServerSwimming.bubbleAt(entity,pos);
            }
        }
    }
    private ServerBubbleColumns(){}
}
