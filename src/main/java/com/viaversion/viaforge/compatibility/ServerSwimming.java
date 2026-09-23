package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.*;
import java.util.*;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraft.world.World;

/** Main-thread swimming state. Inputs, fluid samples, travel and poses are separate tick steps. */
public final class ServerSwimming {
    private static final Map<Entity,State> STATES=new WeakHashMap<>();
    public static final SwimmingFluids fluids=new SwimmingFluids();
    private static final class State {
        boolean swimming,eye,previousEye,previousForward,previousSneak,slowMovement;
        double depth;
        double efficiency=Double.NaN,gravity=.08,sneakSpeed=.3;
        int pose=-1,sprintWindow;
    }
    public static void clear(){STATES.clear();fluids.clear();SwimmingSound.clear();}
    public static boolean enabled(Entity entity){return entity instanceof EntityPlayer&&entity.worldObj.isRemote&&ServerSession.rule(ClientRule.SWIMMING);}
    private static State state(Entity entity){return STATES.computeIfAbsent(entity,e->new State());}
    public static boolean swimming(Entity entity){return enabled(entity)&&state(entity).swimming&&!((EntityPlayer)entity).capabilities.isFlying&&!entity.isRiding();}
    public static boolean pose(Entity entity){return enabled(entity)&&(entity==Minecraft.getMinecraft().thePlayer?
            swimming(entity)||ServerSession.rule(ClientRule.CRAWLING_POSE)&&entity.height==.6F&&!com.viaversion.viaforge.items.ServerElytraFlight.flying(entity):
            state(entity).pose==3||state(entity).pose<0&&swimming(entity));}
    public static double depth(Entity entity){return state(entity).depth;}
    public static void attribute(Entity entity,int kind,double value){if(kind==0)state(entity).efficiency=value;else if(kind==1)state(entity).gravity=value;else if(kind==2)state(entity).sneakSpeed=value;}
    public static float sneakSpeed(EntityPlayer player) {
        if(ServerSession.rule(ClientRule.SNEAK_SPEED_ATTRIBUTE))return (float)state(player).sneakSpeed;
        if(!ServerSession.rule(ClientRule.SWIFT_SNEAK))return .3F;
        net.minecraft.item.ItemStack leggings=player.getCurrentArmor(1);
        int level=0;
        if(leggings!=null&&leggings.hasTagCompound()) {
            // Our flattened snapshot retains the original namespaced list before
            // 1.13 normalizes it into native enchantments/lore. Higher Via layers
            // need not create their own backup (observed on actual 1.19.4 items).
            // Read the snapshot; never replay a stateful item rewriter.
            net.minecraft.nbt.NBTTagList list=leggings.getTagCompound().getCompoundTag("ViaForge|flattenedItem")
                    .getCompoundTag("tag").getTagList("Enchantments",10);
            for(int i=0;i<list.tagCount();i++) {
                net.minecraft.nbt.NBTTagCompound entry=list.getCompoundTagAt(i);
                if("minecraft:swift_sneak".equals(entry.getString("id")))level=Math.max(level,entry.getShort("lvl"));
            }
        }
        return MathHelper.clamp_float(.3F+level*.15F,0,1);
    }
    public static boolean eye(Entity entity){return enabled(entity)&&state(entity).eye;}
    public static void remotePose(EntityPlayer player) {
        if(!enabled(player)||player==Minecraft.getMinecraft().thePlayer||!player.isEntityAlive()||player.isPlayerSleeping())return;
        float height=pose(player)||com.viaversion.viaforge.items.ServerElytraFlight.flying(player)?.6F:
                player.isSneaking()?(ServerSession.rule(ClientRule.CRAWLING_POSE)?1.5F:1.65F):1.8F;
        if(player.height!=height)((com.viaversion.viaforge.mixin.impl.mobs.MobSizeAccess)player).viaForge$size(.6F,height);
    }
    public static void metadata(Entity entity,int swimming,int pose){
        if(!enabled(entity))return;State state=state(entity);
        if(swimming>=0)state.swimming=swimming!=0;
        if(pose>=0)state.pose=pose;
    }
    public static int waterLevel(World world,BlockPos pos){
        int fluid=fluids.get(pos.getX(),pos.getY(),pos.getZ());
        if(fluid>=0)return fluid==0?-1:fluid>16?0:fluid-1;
        IBlockState block=world.getBlockState(pos);
        return block.getBlock().getMaterial()==Material.water?block.getValue(BlockLiquid.LEVEL):-1;
    }
    private static float ownHeight(World world,BlockPos pos){int level=waterLevel(world,pos);return level<0?0:(level>=8?8:8-level)/9F;}
    public static float waterHeight(World world,BlockPos pos){return waterLevel(world,pos)<0?0:waterLevel(world,pos.up())>=0?1:ownHeight(world,pos);}
    private static double lengthSquared(Vec3 vector){return vector.xCoord*vector.xCoord+vector.yCoord*vector.yCoord+vector.zCoord*vector.zCoord;}
    private static Vec3 normalized(Vec3 vector){double length=Math.sqrt(lengthSquared(vector));return length<1.0E-4?new Vec3(0,0,0):new Vec3(vector.xCoord/length,vector.yCoord/length,vector.zCoord/length);}
    private static boolean flowFace(World world,BlockPos pos,EnumFacing direction) {
        IBlockState block=world.getBlockState(pos);
        if(waterLevel(world,pos)>=0||block.getBlock() instanceof net.minecraft.block.BlockIce)return false;
        return block.getBlock().isSideSolid(world,pos,direction);
    }
    private static Vec3 flow(World world,BlockPos pos){
        double x=0,z=0;float own=ownHeight(world,pos);
        for(EnumFacing side:EnumFacing.Plane.HORIZONTAL){BlockPos next=pos.offset(side);float height=ownHeight(world,next),difference=0;
            if(world.getBlockState(next).getBlock().getMaterial()==Material.lava)continue;
            if(height==0&&!world.getBlockState(next).getBlock().getMaterial().blocksMovement()){
                height=ownHeight(world,next.down());if(height>0)difference=own-(height-.8888889F);
            }else if(height>0)difference=own-height;
            x+=(double)(side.getFrontOffsetX()*difference);z+=(double)(side.getFrontOffsetZ()*difference);
        }
        Vec3 flow=new Vec3(x,0,z);
        if(waterLevel(world,pos)>=8)for(EnumFacing side:EnumFacing.Plane.HORIZONTAL){BlockPos next=pos.offset(side);
            if(flowFace(world,next,side)||flowFace(world,next.up(),side)){
                flow=normalized(flow).addVector(0,-6,0);break;
            }
        }
        return normalized(flow);
    }
    /** Replaces the native 1.8 fluid scan, so current is applied exactly once. */
    public static boolean water(EntityPlayer player){
        State state=state(player);state.depth=0;
        AxisAlignedBB box=player.getEntityBoundingBox().contract(.001,.001,.001);
        if(!player.worldObj.isAreaLoaded(new BlockPos(box.minX,box.minY,box.minZ),new BlockPos(box.maxX,box.maxY,box.maxZ)))return false;
        boolean tracker=ServerSession.rule(ClientRule.ENTITY_FLUID_TRACKER),modern=ServerSession.rule(ClientRule.DOUBLE_SWIM_INPUT);
        double feet=tracker?player.getEntityBoundingBox().minY:box.minY;
        boolean touched=false;int count=0;Vec3 current=new Vec3(0,0,0);
        for(int x=MathHelper.floor_double(box.minX);x<MathHelper.ceiling_double_int(box.maxX);x++)
        for(int y=MathHelper.floor_double(box.minY);y<MathHelper.ceiling_double_int(box.maxY);y++)
        for(int z=MathHelper.floor_double(box.minZ);z<MathHelper.ceiling_double_int(box.maxZ);z++){
            BlockPos pos=new BlockPos(x,y,z);float height=modern?waterHeight(player.worldObj,pos):ownHeight(player.worldObj,pos);
            double top=modern?y+(double)height:(double)(y+height);
            if(height==0||top<box.minY)continue;
            touched=true;state.depth=Math.max(state.depth,top-feet);
            Vec3 flow=flow(player.worldObj,pos);if(state.depth<.4)flow=new Vec3(flow.xCoord*state.depth,flow.yCoord*state.depth,flow.zCoord*state.depth);
            current=current.add(flow);count++;
        }
        boolean dryBoat=player.ridingEntity instanceof com.viaversion.viaforge.boats.ServerBoat
            &&((com.viaversion.viaforge.boats.ServerBoat)player.ridingEntity).status!=com.viaversion.viaforge.boats.ServerBoat.Status.UNDER_WATER
            &&((com.viaversion.viaforge.boats.ServerBoat)player.ridingEntity).status!=com.viaversion.viaforge.boats.ServerBoat.Status.FLOWING_WATER;
        if(dryBoat){touched=false;state.depth=0;}
        else if((tracker?lengthSquared(current)>=(double)1.0E-5F:lengthSquared(current)>0)&&!player.capabilities.isFlying){
            current=new Vec3(current.xCoord/count*.014,current.yCoord/count*.014,current.zCoord/count*.014);
            if(ServerSession.rule(ClientRule.MINIMUM_FLUID_CURRENT)&&Math.abs(player.motionX)<.003&&Math.abs(player.motionZ)<.003&&Math.sqrt(lengthSquared(current))<.0045000000000000005){
                Vec3 normal=normalized(current);current=new Vec3(normal.xCoord*.0045000000000000005,normal.yCoord*.0045000000000000005,normal.zCoord*.0045000000000000005);
            }
            player.motionX+=current.xCoord;player.motionY+=current.yCoord;player.motionZ+=current.zCoord;
        }
        state.previousEye=state.eye;
        double eyeY=player.posY+player.getEyeHeight();
        boolean recent=ServerSession.rule(ClientRule.DELAYED_WATER_EYES);
        if(recent&&!tracker)eyeY-=.1111111119389534;
        BlockPos eyes=new BlockPos(player.posX,eyeY,player.posZ);float height=waterHeight(player.worldObj,eyes);
        state.eye=!dryBoat&&height>0&&(tracker?eyeY<=eyes.getY()+(double)height:eyeY<(recent?eyes.getY()+(double)height:(double)(eyes.getY()+height+.11111111F)));
        if(player==Minecraft.getMinecraft().thePlayer){
            boolean wetEye=recent?state.previousEye:state.eye;
            state.swimming=!player.capabilities.isFlying&&!player.isSpectator()&&!player.isRiding()&&player.isSprinting()&&touched
                &&(state.swimming||wetEye&&(!ServerSession.rule(ClientRule.SWIM_FEET_IN_WATER)||waterLevel(player.worldObj,new BlockPos(player))>=0));
        }
        return touched;
    }
    public static void beforeInput(EntityPlayerSP player){
        if(!enabled(player))return;State state=state(player);
        state.previousForward=(ServerSession.rule(ClientRule.SQUARE_SWIM_INPUT)||ServerSession.rule(ClientRule.DOUBLE_SWIM_INPUT)&&state.eye)?player.movementInput.moveForward>1.0E-5F:player.movementInput.moveForward>=.8F;
        state.previousSneak=player.movementInput.sneak;
        // LocalPlayer determines crouching before KeyboardInput reads the next keys.
        // In particular, the first simultaneous sprint+shift tick is not slowed.
        state.slowMovement=com.viaversion.viaforge.items.ServerElytraFlight.crawling(player)
            || !player.capabilities.isFlying&&!swimming(player)&&!player.isRiding()
            &&com.viaversion.viaforge.items.ServerElytraFlight.fits(player,1.5F)
            &&(state.previousSneak||!player.isPlayerSleeping()&&!com.viaversion.viaforge.items.ServerElytraFlight.fits(player,1.8F));
        if(state.sprintWindow>0)state.sprintWindow--;
    }
    /** LocalPlayer's pose slowdown applies on land too, using the pre-sample pose. */
    public static void afterInput(EntityPlayerSP player) {
        if(!enabled(player)||!ServerSession.rule(ClientRule.CRAWLING_POSE)||player.isRiding())return;
        // Native MovementInputFromOptions has already applied the current shift key.
        // Recover keyboard impulses once; do not apply both old and new slowdown.
        float side=Math.signum(player.movementInput.moveStrafe),ahead=Math.signum(player.movementInput.moveForward);
        if(ServerSession.rule(ClientRule.SQUARE_SWIM_INPUT)) {
            float length=(float)Math.sqrt(side*side+ahead*ahead);
            if(length>0){side/=length;ahead/=length;}
        }else if(!ServerSession.rule(ClientRule.SQUARE_SWIM_INPUT)
                &&(state(player).slowMovement||ServerSession.rule(ClientRule.SHIFT_SWIM_INPUT)&&player.movementInput.sneak)) {
            if(ServerSession.rule(ClientRule.SWIFT_SNEAK)){float speed=sneakSpeed(player);side*=speed;ahead*=speed;}
            else {side=(float)(side*.3D);ahead=(float)(ahead*.3D);}
        }
        player.movementInput.moveStrafe=side;player.movementInput.moveForward=ahead;
    }
    /** Runs at the original sprint decision, before creative flight and travel. */
    public static void sprintInput(EntityPlayerSP player) {
        if(!enabled(player)||player.isRiding())return;State state=state(player);
        boolean modern=ServerSession.rule(ClientRule.DOUBLE_SWIM_INPUT),square=ServerSession.rule(ClientRule.SQUARE_SWIM_INPUT);
        boolean forward=(square||modern&&state.eye)?player.movementInput.moveForward>1.0E-5F:player.movementInput.moveForward>=.8F;
        boolean food=player.getFoodStats().getFoodLevel()>6||player.capabilities.allowFlying;
        boolean blind=player.isPotionActive(Potion.blindness),wet=player.isInWater();
        if(player.isUsingItem()||square&&(state.previousSneak||player.movementInput.moveForward<0))state.sprintWindow=0;
        boolean canStart=!player.isSprinting()&&forward&&food&&!player.isUsingItem()&&!blind&&(!wet||state.eye)
            &&(!square||(!state.slowMovement||state.eye)&&(!com.viaversion.viaforge.items.ServerElytraFlight.flying(player)||state.eye));
        if(canStart) {
            if(!state.previousForward&&!state.previousSneak&&(square||player.onGround||state.eye)) {
                if(state.sprintWindow>0)player.setSprinting(true);else state.sprintWindow=7;
            }
            if(Minecraft.getMinecraft().gameSettings.keyBindSprint.isKeyDown())player.setSprinting(true);
        }
        if(player.isSprinting()) {
            boolean noForward=modern?player.movementInput.moveForward<=1.0E-5F:!forward;
            boolean stop=swimming(player)?!wet||!player.onGround&&!player.movementInput.sneak&&(noForward||!food)
                :noForward||!food||player.isCollidedHorizontally||wet&&!state.eye;
            if(stop||square&&blind)player.setSprinting(false);
        }
    }
    public static void input(EntityPlayerSP player) {
        if(enabled(player)&&player.isInWater()&&player.movementInput.sneak&&!player.capabilities.isFlying&&!player.isRiding())player.motionY-=(double).04F;
    }
    /** 1.21.5 moved slowdown after sprint decisions, into LocalPlayer.applyInput. */
    public static void applyMovementInput(EntityPlayerSP player) {
        if(!enabled(player)||!ServerSession.rule(ClientRule.SQUARE_SWIM_INPUT)||player.isRiding())return;
        float[] input=SwimmingPhysics.squareInput(Math.signum(player.movementInput.moveStrafe),Math.signum(player.movementInput.moveForward),
                player.isUsingItem()?.2F:1,state(player).slowMovement?sneakSpeed(player):1);
        player.moveStrafing=input[0];player.moveForward=input[1];
    }
    public static boolean move(EntityPlayer player,float strafe,float forward){
        if(!enabled(player)||player!=Minecraft.getMinecraft().thePlayer||!player.isInWater()||player.capabilities.isFlying||player.isRiding())return false;
        if(swimming(player))player.motionY=SwimmingPhysics.lookY(player.motionY,player.getLookVec().yCoord,((EntityPlayerSP)player).movementInput.jump,waterLevel(player.worldObj,new BlockPos(player.posX,player.posY+.9,player.posZ))>=0);
        boolean falling=player.motionY<=0;double oldY=player.posY;
        double gravity=state(player).gravity;
        if(player.isPotionActive(28)&&falling)gravity=Math.min(gravity,.01);
        float[] factors=SwimmingPhysics.waterFactors(player.isSprinting(),EnchantmentHelper.getDepthStriderModifier(player),player.onGround,player.getAIMoveSpeed(),player.isPotionActive(30));
        if(ServerSession.rule(ClientRule.WATER_EFFICIENCY_ATTRIBUTE)) {
            // Modern vanilla reads the synchronized attribute, whose default is zero.
            // Reading the boots early races the server's equipment/attribute update.
            float efficiency=Double.isNaN(state(player).efficiency)?0:(float)state(player).efficiency;if(!player.onGround)efficiency*=.5F;
            float drag=player.isSprinting()?.9F:.8F,speed=.02F;
            if(efficiency>0){drag+=(.54600006F-drag)*efficiency;speed+=(player.getAIMoveSpeed()-speed)*efficiency;}
            factors=new float[]{player.isPotionActive(30)?.96F:drag,speed};
        }
        if(ServerSession.rule(ClientRule.DOUBLE_SWIM_INPUT)){
            double length=strafe*(double)strafe+forward*(double)forward;
            if(length>=1.0E-7){double divisor=length>1?Math.sqrt(length):1;
                double side=(strafe/divisor)*(double)factors[1],ahead=(forward/divisor)*(double)factors[1];float sin=MathHelper.sin(player.rotationYaw*((float)Math.PI/180)),cos=MathHelper.cos(player.rotationYaw*((float)Math.PI/180));
                player.motionX+=side*cos-ahead*sin;player.motionZ+=ahead*cos+side*sin;
            }
        }else player.moveFlying(strafe,forward,factors[1]);
        player.moveEntity(player.motionX,player.motionY,player.motionZ);
        if(ServerSession.rule(ClientRule.DOUBLE_SWIM_INPUT)&&player.isCollidedHorizontally&&player.isOnLadder())player.motionY=.2;
        player.motionX*=(double)factors[0];player.motionY*=(double).8F;player.motionZ*=(double)factors[0];
        player.motionY=SwimmingPhysics.falling(player.motionY,gravity,ServerSession.rule(ClientRule.DOUBLE_SWIM_INPUT)?falling:player.motionY<=0,player.isSprinting());
        if(player.isCollidedHorizontally&&player.isOffsetPositionInLiquid(player.motionX,player.motionY+(double).6F-player.posY+oldY,player.motionZ))player.motionY=(double).3F;
        player.prevLimbSwingAmount=player.limbSwingAmount;
        double dx=player.posX-player.prevPosX,dy=player.posY-player.prevPosY,dz=player.posZ-player.prevPosZ;
        float animation=Math.min(1,MathHelper.sqrt_double(dx*dx+dz*dz+(swimming(player)?dy*dy:0))*4);
        player.limbSwingAmount+=(animation-player.limbSwingAmount)*.4F;player.limbSwing+=player.limbSwingAmount;
        return true;
    }
    /** Bubble callbacks at the original pre-/post-travel stage of the target. */
    public static void bubbles(Entity entity) {
        if(!enabled(entity)||entity!=Minecraft.getMinecraft().thePlayer||entity.noClip)return;
        AxisAlignedBB box=entity.getEntityBoundingBox().contract(.001,.001,.001);
        for(int x=MathHelper.floor_double(box.minX);x<=MathHelper.floor_double(box.maxX);x++)
        for(int y=MathHelper.floor_double(box.minY);y<=MathHelper.floor_double(box.maxY);y++)
        for(int z=MathHelper.floor_double(box.minZ);z<=MathHelper.floor_double(box.maxZ);z++) {
            int fluid=fluids.get(x,y,z);if(fluid<17)continue;
            BlockPos above=new BlockPos(x,y+1,z);boolean surface=entity.worldObj.isAirBlock(above)&&waterLevel(entity.worldObj,above)<0;
            entity.motionY=fluid==18?Math.max(surface?-.9:-.3,entity.motionY-.03):Math.min(surface?1.8:.7,entity.motionY+(surface?.1:.06));
            entity.fallDistance=0;
        }
    }
    private ServerSwimming(){}
}
