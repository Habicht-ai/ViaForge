package com.viaversion.viaforge.mobs;

import com.viaversion.viaforge.common.blocks.MobKind;
import java.util.UUID;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;

/** Server-driven living entity. No local spawning, pathfinding, attacks or inventory simulation. */
public final class ServerMob extends EntityLiving {
    public final MobState state;
    public ItemStack offhand;
    public float peek, prevPeek, standing, prevStanding, flap, prevFlap, flapSpeed, prevFlapSpeed, flapVelocity = 1;
    public BlockPos attachment, oldAttachment, jukebox;
    public int teleportTicks;
    public final Vec3[][] illusions = {{new Vec3(0,0,0),new Vec3(0,0,0),new Vec3(0,0,0),new Vec3(0,0,0)}, {new Vec3(0,0,0),new Vec3(0,0,0),new Vec3(0,0,0),new Vec3(0,0,0)}};
    public int illusionTicks;

    public ServerMob(World world, MobState state, UUID uuid) {
        super(world); this.state = state; this.entityUniqueID = uuid;
        getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.maxHealth).setBaseValue(1024);
        setHealth(20); applyMetadata();
    }
    public MobKind kind() { return state.kind; }
    @Override public String getName() {
        if (hasCustomName()) return getCustomNameTag();
        if (state == null) return super.getName();
        String name;
        switch (kind()) {
            case EVOKER: name = "EvocationIllager"; break;
            case VINDICATOR: name = "VindicationIllager"; break;
            case ILLUSIONER: name = "IllusionIllager"; break;
            default:
                StringBuilder key = new StringBuilder();
                for (String part : kind().name().toLowerCase(java.util.Locale.ROOT).split("_")) key.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                name = key.toString();
        }
        return StatCollector.translateToLocal("entity." + name + ".name");
    }
    @Override protected String getHurtSound() { return ServerMobSounds.entity(this,"hurt"); }
    @Override protected String getDeathSound() { return ServerMobSounds.entity(this,"death"); }
    @Override protected String getLivingSound() { return null; } // Ambient calls arrive from the server.
    @Override public ItemStack getPickedResult(MovingObjectPosition target) {
        int type = state.protocol < 315 ? state.wireType : kind().id;
        for (com.google.gson.JsonElement entry : com.viaversion.viaforge.items.ItemVariants.EGGS) {
            com.google.gson.JsonObject egg = entry.getAsJsonObject();
            if (egg.get("number").getAsInt() == type && egg.get("protocol").getAsInt() <= state.protocol)
                return com.viaversion.viaforge.items.ItemVariants.egg(new ItemStack(net.minecraft.init.Items.spawn_egg), egg);
        }
        return null;
    }
    @Override protected void playStepSound(BlockPos pos, net.minecraft.block.Block block) {
        String sound = ServerMobSounds.entity(this,"step");
        if (sound != null) playSound(sound,.15F,1); else super.playStepSound(pos,block);
    }
    @Override public boolean isChild() { return state != null && state.baby(); }
    public void applyMetadata() {
        int base = state.first - 6;
        dataWatcher.updateObject(0, (byte)state.number(0, 0));
        if (state.data.containsKey(2)) setCustomNameTag(String.valueOf(state.data.get(2)));
        setAlwaysRenderNameTag(state.flag(3)); setSilent(state.flag(4));
        Object health = state.data.get(base + 1);
        if (health instanceof Float) setHealth((Float)health);
        dataWatcher.updateObject(7, state.number(base + 2, 0));
        dataWatcher.updateObject(8, (byte)(state.flag(base + 3) ? 1 : 0));
        setArrowCountInEntity(state.number(base + 4, 0));
        noClip = kind() == MobKind.VEX;
        isImmuneToFire = kind() == MobKind.VEX || kind() == MobKind.SHULKER || kind() == MobKind.WITHER_SKELETON;
        float width = .6F, height = 1.95F;
        switch (kind()) {
            case SHULKER: width = height = 1; break;
            case POLAR_BEAR: width = 1.3F; height = 1.4F; break;
            case LLAMA: width = .9F; height = 1.87F; break;
            case PARROT: width = .5F; height = .9F; break;
            case VEX: width = .4F; height = .8F; break;
            case SKELETON: case STRAY: height = 1.99F; break;
            case WITHER_SKELETON: width = .7F; height = 2.4F; break;
            default: break;
        }
        if (isChild()) { width *= .5F; height *= .5F; }
        setSize(width, height); setPosition(posX, posY, posZ);
        if (kind() == MobKind.SHULKER) {
            Object position = state.data.get(state.first + 1);
            if (position instanceof com.viaversion.viaversion.api.minecraft.BlockPosition) {
                com.viaversion.viaversion.api.minecraft.BlockPosition p = (com.viaversion.viaversion.api.minecraft.BlockPosition)position;
                BlockPos next = new BlockPos(p.x(), p.y(), p.z());
                if (!next.equals(attachment)) {
                    oldAttachment = attachment == null ? next : attachment; teleportTicks = attachment == null ? 0 : 6; attachment = next;
                    setPosition(p.x() + .5, p.y(), p.z() + .5);
                    prevPosX = lastTickPosX = posX; prevPosY = lastTickPosY = posY; prevPosZ = lastTickPosZ = posZ;
                }
            }
            shulkerBounds();
        }
    }
    @Override public void onUpdate() {
        super.onUpdate();
        prevPeek = peek; float target = state.number(state.first + 2, 0) * .01F;
        peek = peek < target ? Math.min(target, peek + .05F) : Math.max(target, peek - .05F);
        prevStanding = standing; standing = MathHelper.clamp_float(standing + (state.flag(state.first + 1) ? 1 : -1), 0, 6);
        if (teleportTicks > 0) --teleportTicks;
        if (kind() == MobKind.SHULKER) { renderYawOffset = prevRenderYawOffset = rotationYaw = 180; shulkerBounds(); }
        if (kind() == MobKind.PARROT) {
            prevFlap = flap; prevFlapSpeed = flapSpeed;
            flapSpeed = MathHelper.clamp_float(flapSpeed + (onGround ? -1 : 4) * .3F, 0, 1);
            if (!onGround && flapVelocity < 1) flapVelocity = 1;
            flapVelocity *= .9F;
            if (!onGround && motionY < 0) motionY *= .6;
            flap += flapVelocity * 2;
            if (jukebox != null && (jukebox.distanceSq(posX, posY, posZ) > 12 || worldObj.getBlockState(jukebox).getBlock() != net.minecraft.init.Blocks.jukebox)) jukebox = null;
        }
        if (kind().illager() && state.spell() != 0) spellParticles();
        if (kind() == MobKind.ILLUSIONER && isInvisible()) {
            illusionTicks = Math.max(0,illusionTicks-1);
            boolean spread = hurtTime == 1 || ticksExisted%1200 == 0;
            if (spread || hurtTime == maxHurtTime-1) {
                illusionTicks = 3;
                for(int i=0;i<4;i++) { illusions[0][i] = illusions[1][i]; illusions[1][i] = spread ? new Vec3((-6+rand.nextInt(13))*.5,Math.max(0,rand.nextInt(6)-4),(-6+rand.nextInt(13))*.5) : new Vec3(0,0,0); }
                if (spread) {
                    for(int i=0;i<16;i++) worldObj.spawnParticle(EnumParticleTypes.CLOUD,posX+(rand.nextDouble()-.5)*width,posY+rand.nextDouble()*height,posZ+(rand.nextDouble()-.5)*width,0,0,0);
                    String sound = ServerMobSounds.key("entity.illusion_illager.mirror_move",5); if(sound != null) worldObj.playSound(posX,posY,posZ,sound,1,1,false);
                }
            }
        }
    }
    @Override public void moveEntityWithHeading(float strafe, float forward) {
        if (state != null && kind() == MobKind.SHULKER) { motionX = motionY = motionZ = 0; return; }
        if (state != null && (kind() == MobKind.VEX || state.protocol >= 210 && state.flag(5))) {
            moveEntity(motionX, motionY, motionZ); motionX *= .91; motionY *= .91; motionZ *= .91;
        } else super.moveEntityWithHeading(strafe, forward);
    }
    private void shulkerBounds() {
        double extension = .5 - MathHelper.sin((.5F + peek) * (float)Math.PI) * .5;
        EnumFacing open = EnumFacing.getFront(state.number(state.first, 0)).getOpposite();
        setEntityBoundingBox(new AxisAlignedBB(posX - .5 + Math.min(0, open.getFrontOffsetX() * extension), posY + Math.min(0, open.getFrontOffsetY() * extension), posZ - .5 + Math.min(0, open.getFrontOffsetZ() * extension),
                posX + .5 + Math.max(0, open.getFrontOffsetX() * extension), posY + 1 + Math.max(0, open.getFrontOffsetY() * extension), posZ + .5 + Math.max(0, open.getFrontOffsetZ() * extension)));
    }
    private void spellParticles() {
        double[][] colors = {{0,0,0},{.7,.7,.8},{.4,.3,.35},{.7,.5,.2},{.3,.3,.8},{.1,.1,.2}};
        int spell = MathHelper.clamp_int(state.spell(), 0, colors.length - 1);
        double angle = renderYawOffset * Math.PI / 180 + MathHelper.cos(ticksExisted * .6662F) * .25;
        for (int side : new int[]{-1,1}) worldObj.spawnParticle(EnumParticleTypes.SPELL_MOB, posX + Math.cos(angle) * .6 * side, posY + 1.8, posZ + Math.sin(angle) * .6 * side, colors[spell][0], colors[spell][1], colors[spell][2]);
    }
    public float peek(float partial) { return prevPeek + (peek - prevPeek) * partial; }
    public float standing(float partial) { return (prevStanding + (standing - prevStanding) * partial) / 6; }
    public boolean sitting() { return kind() == MobKind.PARROT && (state.number(state.first + 1, 0) & 1) != 0; }
    public boolean charging() { return kind() == MobKind.VEX && (state.number(state.first, 0) & 1) != 0; }
    @Override public float getEyeHeight() {
        if (state == null) return super.getEyeHeight();
        if (kind() == MobKind.SHULKER) return .5F;
        if (kind() == MobKind.PARROT) return height * .6F;
        if (kind() == MobKind.VEX) return .4F;
        if (kind() == MobKind.WITHER_SKELETON) return 2.1F;
        if (kind().skeleton()) return 1.74F;
        if (kind().zombie()) return isChild() ? .93F : 1.74F;
        return super.getEyeHeight();
    }
    @Override public double getMountedYOffset() { return kind() == MobKind.LLAMA ? height * .67 : super.getMountedYOffset(); }
    @Override public void handleStatusUpdate(byte status) {
        if ((kind() == MobKind.PARROT || kind() == MobKind.LLAMA) && (status == 6 || status == 7)) {
            for (int i = 0; i < 7; i++) worldObj.spawnParticle(status == 7 ? EnumParticleTypes.HEART : EnumParticleTypes.SMOKE_NORMAL,
                    posX + rand.nextFloat() * width * 2 - width, posY + .5 + rand.nextFloat() * height, posZ + rand.nextFloat() * width * 2 - width,
                    rand.nextGaussian() * .02, rand.nextGaussian() * .02, rand.nextGaussian() * .02);
        } else super.handleStatusUpdate(status);
    }
}
