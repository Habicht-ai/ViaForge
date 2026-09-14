package com.viaversion.viaforge.boats;

import com.viaversion.viaforge.items.ClientItems;
import com.viaversion.viaforge.mobs.ServerMobSounds;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import java.util.*;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;

/** Original modern boat simulation on the controlling client; server owns seats and damage. */
public final class ServerBoat extends Entity {
    public enum Status { WATER, UNDER_WATER, FLOWING_WATER, LAND, AIR }
    public final int protocol; // Internal event data format, not the negotiated server version.
    private final com.viaversion.viaforge.common.compatibility.VersionRules behavior;
    public int wood, hurtTime, hurtDirection = 1;
    public float damage, deltaRotation, landGlide;
    public Status status = Status.AIR;
    private Status previousStatus;
    private final List<Entity> passengers = new ArrayList<>();
    private final boolean[] paddles = new boolean[2];
    private final float[] paddleTime = new float[2];
    private boolean left, right, forward, back, noGravity;
    private double waterLevel, lastYMotion, targetX, targetY, targetZ, targetYaw, targetPitch;
    private int lerpSteps;
    public double wireX, wireY, wireZ;
    public float wireYaw, wirePitch;
    public ServerBoat(World world, int protocol) { super(world); this.protocol = protocol; this.behavior=com.viaversion.viaforge.compatibility.ServerSession.profile().extended()?com.viaversion.viaforge.compatibility.ServerSession.rules():com.viaversion.viaforge.common.compatibility.CompatibilityRegistry.DEFAULT.resolve(protocol).rules(); preventEntitySpawning = true; setSize(1.375F, .5625F); }
    @Override protected void entityInit() { }
    @Override protected void readEntityFromNBT(NBTTagCompound nbt) { wood = MathHelper.clamp_int(nbt.getInteger("ViaForgeWood"), 0, 5); }
    @Override protected void writeEntityToNBT(NBTTagCompound nbt) { nbt.setInteger("ViaForgeWood", wood); }
    public List<Entity> passengers() { return Collections.unmodifiableList(passengers); }
    public boolean contains(Entity entity) { return passengers.contains(entity); }
    public boolean controlled() { return !passengers.isEmpty() && passengers.get(0) == Minecraft.getMinecraft().thePlayer; }
    public void seats(List<Entity> incoming) {
        for (Entity old : new ArrayList<>(passengers)) if (!incoming.contains(old)) detach(old);
        passengers.clear();
        for (Entity rider : incoming) {
            if (rider == this || passengers.contains(rider)) continue;
            boolean boarding = rider.ridingEntity != this;
            if (rider.ridingEntity instanceof ServerBoat && rider.ridingEntity != this) ((ServerBoat)rider.ridingEntity).detach(rider);
            else if (rider.ridingEntity != null && rider.ridingEntity != this) rider.mountEntity(null);
            rider.ridingEntity = this; passengers.add(rider);
            if (boarding && rider == Minecraft.getMinecraft().thePlayer) {
                rider.rotationYaw = rider.prevRotationYaw = rotationYaw;
                ((EntityLivingBase)rider).rotationYawHead = rotationYaw;
                Minecraft mc = Minecraft.getMinecraft();
                mc.ingameGUI.setRecordPlaying(new ChatComponentTranslation("mount.onboard", net.minecraft.client.settings.GameSettings.getKeyDisplayString(mc.gameSettings.keyBindSneak.getKeyCode())), false);
            }
        }
        riddenByEntity = passengers.isEmpty() ? null : passengers.get(0);
        if (controlled() && lerpSteps > 0) { lerpSteps = 0; setPositionAndRotation(targetX, targetY, targetZ, (float)targetYaw, (float)targetPitch); }
        for (Entity rider : passengers) positionPassenger(rider, false);
    }
    public void detach(Entity rider) {
        passengers.remove(rider);
        if (rider.ridingEntity == this) {
            rider.ridingEntity = null;
            rider.setPosition(posX, posY + height, posZ);
        }
        riddenByEntity = passengers.isEmpty() ? null : passengers.get(0);
    }
    @Override public void setDead() { for (Entity rider : new ArrayList<>(passengers)) detach(rider); super.setDead(); }
    @Override public double getMountedYOffset() { return -.1; }
    @Override public AxisAlignedBB getCollisionBoundingBox() { return getEntityBoundingBox(); }
    @Override public AxisAlignedBB getCollisionBox(Entity entity) { return contains(entity) ? null : entity.canBePushed() ? entity.getEntityBoundingBox() : null; }
    @Override public boolean canBeCollidedWith() { return !isDead; }
    @Override public boolean canBePushed() { return true; }
    @Override protected boolean canTriggerWalking() { return false; }
    @Override public boolean isPushedByWater() { return false; }
    @Override public boolean interactFirst(EntityPlayer player) { return !player.isSneaking(); }
    @Override public boolean attackEntityFrom(DamageSource source, float amount) { return !isEntityInvulnerable(source); }
    @Override public void performHurtAnimation() { hurtDirection = -hurtDirection; hurtTime = 10; damage *= 11; }
    @Override public ItemStack getPickedResult(MovingObjectPosition hit) { return new ItemStack(Item.getItemById(ClientItems.localItem(wood == 0 ? 333 : 443 + wood, 0))); }
    @Override public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int increments, boolean teleport) {
        targetX = x; targetY = y; targetZ = z; targetYaw = yaw; targetPitch = pitch; lerpSteps = 10;
    }
    public void correction(double x, double y, double z, float yaw, float pitch) {
        lerpSteps = 0; wireX = x; wireY = y; wireZ = z; wireYaw = yaw; wirePitch = pitch;
        setPositionAndRotation(x, y, z, yaw, pitch);
        for (Entity rider : passengers) positionPassenger(rider, false);
    }
    public void metadata(List<EntityData> data) {
        int first = protocol >= 210 ? 6 : 5;
        for (EntityData entry : data) {
            Object value = entry.getValue(); int id = entry.id();
            if (id == 0 && value instanceof Byte) dataWatcher.updateObject(0, value);
            else if (id == 2 && value instanceof String) setCustomNameTag((String)value);
            else if (id == 3 && value instanceof Boolean) setAlwaysRenderNameTag((Boolean)value);
            else if (id == 4 && value instanceof Boolean) setSilent((Boolean)value);
            else if (protocol >= 210 && id == 5 && value instanceof Boolean) noGravity = (Boolean)value;
            else if (id == first && value instanceof Number) hurtTime = ((Number)value).intValue();
            else if (id == first+1 && value instanceof Number) hurtDirection = ((Number)value).intValue();
            else if (id == first+2 && value instanceof Number) damage = ((Number)value).floatValue();
            else if (id == first+3 && value instanceof Number) wood = MathHelper.clamp_int(((Number)value).intValue(), 0, 5);
            else if ((id == first+4 || id == first+5) && value instanceof Boolean) paddles[id-first-4] = (Boolean)value;
        }
    }
    public void input(boolean left, boolean right, boolean forward, boolean back) { this.left=left; this.right=right; this.forward=forward; this.back=back; }
    public boolean paddle(int side) { return !passengers.isEmpty() && paddles[side]; }
    public float paddlePhase(int side, float partial) {
        if (!paddle(side)) return 0;
        double step = behavior.enabled(com.viaversion.viaforge.common.compatibility.ClientRule.FAST_PADDLE_CYCLE) ? (double).3926991F : .01;
        float phase = (float)(paddleTime[side] - step + step * partial);
        return behavior.enabled(com.viaversion.viaforge.common.compatibility.ClientRule.FAST_PADDLE_CYCLE) ? phase : phase * 40;
    }
    @Override public void onUpdate() {
        previousStatus = status; status = environment();
        if (hurtTime > 0) hurtTime--; if (damage > 0) damage--;
        super.onUpdate();
        passengers.removeIf(rider -> rider.isDead || rider.ridingEntity != this);
        riddenByEntity = passengers.isEmpty() ? null : passengers.get(0);
        if (controlled()) {
            lerpSteps = 0;
            net.minecraft.client.settings.GameSettings keys = Minecraft.getMinecraft().gameSettings;
            if (Minecraft.getMinecraft().currentScreen == null) input(keys.keyBindLeft.isKeyDown(), keys.keyBindRight.isKeyDown(), keys.keyBindForward.isKeyDown(), keys.keyBindBack.isKeyDown());
            else input(false, false, false, false);
            physics(); steer();
            BoatPackets.paddles(this);
            moveEntity(motionX, motionY, motionZ);
        } else {
            if (lerpSteps > 0) {
                setPosition(posX+(targetX-posX)/lerpSteps, posY+(targetY-posY)/lerpSteps, posZ+(targetZ-posZ)/lerpSteps);
                rotationYaw += MathHelper.wrapAngleTo180_float((float)targetYaw-rotationYaw)/lerpSteps;
                rotationPitch += ((float)targetPitch-rotationPitch)/lerpSteps; lerpSteps--;
            }
            motionX = motionY = motionZ = 0;
        }
        for (int side = 0; side < 2; side++) {
            if (!paddle(side)) { paddleTime[side] = 0; continue; }
            if (behavior.enabled(com.viaversion.viaforge.common.compatibility.ClientRule.FAST_PADDLE_CYCLE) && !isSilent() && paddleTime[side] % ((float)Math.PI*2) <= .7853981852531433
                    && (paddleTime[side] + (double).3926991F) % 6.2831854820251465 >= .7853981852531433) {
                Status soundStatus = environment();
                String sound = soundStatus == Status.AIR ? null : ServerMobSounds.key(soundStatus == Status.LAND ? "entity.boat.paddle_land" : "entity.boat.paddle_water", 7);
                if (sound != null) { Vec3 look = getLook(1); worldObj.playSound(posX+(side == 1 ? -look.zCoord : look.zCoord), posY, posZ+(side == 1 ? look.xCoord : -look.xCoord), sound, 1, .8F+rand.nextFloat()*.4F, false); }
            }
            paddleTime[side] = (float)(paddleTime[side] + (behavior.enabled(com.viaversion.viaforge.common.compatibility.ClientRule.FAST_PADDLE_CYCLE) ? (double).3926991F : .01));
        }
        for (Entity entity : worldObj.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox().expand(.2, -.01, .2))) {
            if (contains(entity) || entity.ridingEntity == this || !entity.canBePushed()) continue;
            if (entity instanceof ServerBoat ? entity.getEntityBoundingBox().minY < getEntityBoundingBox().maxY : entity.getEntityBoundingBox().minY <= getEntityBoundingBox().minY) applyEntityCollision(entity);
        }
    }
    public void steer() {
        float thrust = 0;
        if (left) deltaRotation--; if (right) deltaRotation++;
        if (left != right && !forward && !back) thrust += .005F;
        rotationYaw += deltaRotation;
        if (forward) thrust += .04F; if (back) thrust -= .005F;
        motionX += MathHelper.sin(-rotationYaw * (float)Math.PI/180) * thrust;
        motionZ += MathHelper.cos(rotationYaw * (float)Math.PI/180) * thrust;
        paddles[0] = (right && (!behavior.enabled(com.viaversion.viaforge.common.compatibility.ClientRule.EXCLUSIVE_TURN_PADDLES) || !left)) || forward;
        paddles[1] = (left && (!behavior.enabled(com.viaversion.viaforge.common.compatibility.ClientRule.EXCLUSIVE_TURN_PADDLES) || !right)) || forward;
    }
    public void physics() {
        double gravity = noGravity ? 0 : (double)-.04F, buoyancy = 0; float friction = .05F;
        if (previousStatus == Status.AIR && status != Status.AIR && status != Status.LAND) {
            waterLevel = getEntityBoundingBox().minY + height;
            setPosition(posX, waterAbove() - height + .101, posZ); motionY = lastYMotion = 0; status = Status.WATER;
        } else {
            switch (status) {
                case WATER: buoyancy=(waterLevel-getEntityBoundingBox().minY)/height; friction=.9F; break;
                case FLOWING_WATER: gravity=-.0007; friction=.9F; break;
                case UNDER_WATER: buoyancy=.01F; friction=.45F; break;
                case AIR: friction=.9F; break;
                case LAND: friction=landGlide; if (!passengers.isEmpty() && passengers.get(0) instanceof EntityPlayer) landGlide /= 2; break;
            }
            motionX *= friction; motionZ *= friction; deltaRotation *= friction; motionY += gravity;
            if (buoyancy > 0) { motionY += buoyancy * .06153846016296973; motionY *= .75; }
        }
    }
    @Override protected void updateFallState(double y, boolean grounded, net.minecraft.block.Block block, BlockPos pos) {
        // Sample before Entity.moveEntity applies its on-landed response. Breaking
        // and drops, including falls above three blocks, are decided by the server.
        lastYMotion = motionY;
        if (ridingEntity != null) return;
        if (grounded) fallDistance = 0;
        else if (worldObj.getBlockState(new BlockPos(this).down()).getBlock().getMaterial() != Material.water && y < 0) fallDistance -= y;
    }
    private float waterHeight(BlockPos pos, IBlockState state) {
        int level=state.getValue(BlockLiquid.LEVEL);
        if ((level & 7)==0 && worldObj.getBlockState(pos.up()).getBlock().getMaterial() == Material.water) return 1;
        return 1 - BlockLiquid.getLiquidHeightPercent(level);
    }
    public Status environment() {
        AxisAlignedBB box = getEntityBoundingBox(); boolean submerged=false, wet=false;
        double top=box.maxY+.001; waterLevel=-Double.MAX_VALUE;
        for (int x=MathHelper.floor_double(box.minX); x<MathHelper.ceiling_double_int(box.maxX); x++)
            for (int z=MathHelper.floor_double(box.minZ); z<MathHelper.ceiling_double_int(box.maxZ); z++) {
                BlockPos upper=new BlockPos(x,MathHelper.floor_double(box.maxY),z); IBlockState state=worldObj.getBlockState(upper);
                if (state.getBlock().getMaterial()==Material.water && top < upper.getY()+waterHeight(upper,state)) {
                    if (state.getValue(BlockLiquid.LEVEL) != 0) { waterLevel=box.maxY; return Status.FLOWING_WATER; }
                    submerged=true;
                }
                BlockPos lower=new BlockPos(x,MathHelper.floor_double(box.minY),z); state=worldObj.getBlockState(lower);
                if (state.getBlock().getMaterial()==Material.water) { double level=lower.getY()+waterHeight(lower,state); waterLevel=Math.max(level,waterLevel); wet |= box.minY<level; }
            }
        if (submerged) { waterLevel=box.maxY; return Status.UNDER_WATER; }
        if (wet) return Status.WATER;
        AxisAlignedBB floor=new AxisAlignedBB(box.minX,box.minY-.001,box.minZ,box.maxX,box.minY,box.maxZ);
        float glide=0; int count=0; List<AxisAlignedBB> collisions=new ArrayList<>();
        for (int x=MathHelper.floor_double(floor.minX)-1; x<MathHelper.ceiling_double_int(floor.maxX)+1; x++)
            for (int z=MathHelper.floor_double(floor.minZ)-1; z<MathHelper.ceiling_double_int(floor.maxZ)+1; z++)
                for (int y=MathHelper.floor_double(floor.minY)-1; y<MathHelper.ceiling_double_int(floor.maxY)+1; y++) {
                    int edge=(x==MathHelper.floor_double(floor.minX)-1 || x==MathHelper.ceiling_double_int(floor.maxX) ? 1:0)+(z==MathHelper.floor_double(floor.minZ)-1 || z==MathHelper.ceiling_double_int(floor.maxZ) ? 1:0);
                    if (edge==2 || edge>0 && (y==MathHelper.floor_double(floor.minY)-1 || y==MathHelper.ceiling_double_int(floor.maxY))) continue;
                    BlockPos pos=new BlockPos(x,y,z); IBlockState state=worldObj.getBlockState(pos);
                    state.getBlock().addCollisionBoxesToList(worldObj,pos,state,floor,collisions,this);
                    if (!collisions.isEmpty()) { glide+=state.getBlock().slipperiness; count++; collisions.clear(); }
                }
        landGlide=count==0 ? 0:glide/count; return landGlide>0 ? Status.LAND:Status.AIR;
    }
    private float waterAbove() {
        AxisAlignedBB box=getEntityBoundingBox(); int end=MathHelper.ceiling_double_int(box.maxY-lastYMotion);
        for (int y=MathHelper.floor_double(box.maxY);y<end;y++) {
            float level=0;
            for(int x=MathHelper.floor_double(box.minX);x<MathHelper.ceiling_double_int(box.maxX);x++) for(int z=MathHelper.floor_double(box.minZ);z<MathHelper.ceiling_double_int(box.maxZ);z++) {
                BlockPos pos=new BlockPos(x,y,z); IBlockState state=worldObj.getBlockState(pos);
                if(state.getBlock().getMaterial()==Material.water) level=Math.max(level,waterHeight(pos,state));
            }
            if(level<1) return y+level;
        }
        return end+1;
    }
    @Override public void updateRiderPosition() { for(Entity rider:passengers) positionPassenger(rider,false); }
    public void positionPassenger(Entity rider, boolean rotate) {
        if (!contains(rider)) return;
        float offset=0;
        if(passengers.size()>1) { offset=passengers.indexOf(rider)==0 ? .2F:-.6F; if(animal(rider)) offset+=.2F; }
        Vec3 seat=new Vec3(offset,0,0).rotateYaw(-rotationYaw*(float)Math.PI/180-(float)Math.PI/2);
        rider.setPosition(posX+seat.xCoord,posY+getMountedYOffset()+rider.getYOffset(),posZ+seat.zCoord);
        if(rotate) rider.rotationYaw+=deltaRotation;
        orientPassenger(rider);
        if(animal(rider) && passengers.size()>1 && rider instanceof EntityLivingBase) {
            EntityLivingBase living=(EntityLivingBase)rider;
            float side=rider.getEntityId()%2==0?90:270; living.renderYawOffset+=side; living.rotationYawHead+=side;
        }
    }
    public void orientPassenger(Entity rider) {
        float difference=MathHelper.wrapAngleTo180_float(rider.rotationYaw-rotationYaw), clamped=MathHelper.clamp_float(difference,-105,105);
        rider.prevRotationYaw+=clamped-difference; rider.rotationYaw+=clamped-difference;
        if(rider instanceof EntityLivingBase) { EntityLivingBase living=(EntityLivingBase)rider; living.renderYawOffset=rotationYaw; living.rotationYawHead=rider.rotationYaw;
        }
    }
    private static boolean animal(Entity entity) {
        if (entity instanceof EntityAnimal) return true;
        if (!(entity instanceof com.viaversion.viaforge.mobs.ServerMob)) return false;
        switch (((com.viaversion.viaforge.mobs.ServerMob)entity).kind()) {
            case POLAR_BEAR: case LLAMA: case PARROT: return true;
            default: return false;
        }
    }
}
