package com.viaversion.viaforge.mixin.impl.mobs;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.mobs.*;
import net.minecraft.entity.boss.*;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityDragon.class)
public abstract class MixinDragonVisuals extends net.minecraft.entity.EntityLiving {
    protected MixinDragonVisuals(World world) { super(world); }
    @Unique private EntityDragonPart viaForge$neck;
    @Unique private int viaForge$growl=100;
    @Unique private float viaForge$previousWing;
    @Unique private int viaForge$phase = -1, viaForge$phaseTicks;
    @Inject(method="<init>",at=@At("RETURN"))
    private void newerParts(World world,CallbackInfo ci) {
        if (!ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.MOBS)) return;
        EntityDragon dragon=(EntityDragon)(Object)this;
        viaForge$neck=new EntityDragonPart(dragon,"neck",6,6);
        dragon.dragonPartArray=new EntityDragonPart[]{dragon.dragonPartHead,viaForge$neck,dragon.dragonPartBody,dragon.dragonPartTail1,dragon.dragonPartTail2,dragon.dragonPartTail3,dragon.dragonPartWing1,dragon.dragonPartWing2};
        for(int i=0;i<dragon.dragonPartArray.length;i++) dragon.dragonPartArray[i].setEntityId(dragon.getEntityId()+i+1);
    }
    @Inject(method="onLivingUpdate",at=@At("HEAD"))
    private void wingStart(CallbackInfo ci) { viaForge$previousWing=((EntityDragon)(Object)this).animTime; }
    @Inject(method="onLivingUpdate",at=@At("RETURN"))
    private void phase(CallbackInfo ci) {
        EntityDragon dragon=(EntityDragon)(Object)this;
        int phase=DragonVisualState.phase(dragon);
        if (phase<0 || viaForge$neck==null) return;
        if (dragon.getHealth() <= 0) return;
        if (viaForge$phase != phase) { viaForge$phase = phase; viaForge$phaseTicks = 0; }
        ++viaForge$phaseTicks;
        if (DragonVisualState.sitting(phase)) dragon.animTime=viaForge$previousWing+.1F;
        float tilt=(float)(dragon.getMovementOffsets(5,1)[1]-dragon.getMovementOffsets(10,1)[1])*10*(float)Math.PI/180;
        float horizontal=MathHelper.cos(tilt), vertical=-MathHelper.sin(tilt);
        float yaw=dragon.rotationYaw*(float)Math.PI/180-randomYawVelocity*.01F;
        double offset=DragonVisualState.sitting(phase) ? -1 : dragon.getMovementOffsets(5,1)[1]-dragon.getMovementOffsets(0,1)[1];
        dragon.dragonPartHead.width=dragon.dragonPartHead.height=1;
        dragon.dragonPartHead.setLocationAndAngles(dragon.posX+MathHelper.sin(yaw)*6.5*horizontal,dragon.posY+offset+vertical*6.5,dragon.posZ-MathHelper.cos(yaw)*6.5*horizontal,0,0);
        viaForge$neck.width=viaForge$neck.height=3; viaForge$neck.onUpdate();
        viaForge$neck.setLocationAndAngles(dragon.posX+MathHelper.sin(yaw)*5.5*horizontal,dragon.posY+offset+vertical*5.5,dragon.posZ-MathHelper.cos(yaw)*5.5*horizontal,0,0);
        DragonVisualState.breath(dragon, phase, viaForge$phaseTicks);
        if (phase == 7) {
            String sound = ServerMobSounds.key("entity.enderdragon.growl", 5);
            if (sound != null) dragon.worldObj.playSound(dragon.posX, dragon.posY, dragon.posZ, sound, 2.5F, .8F + dragon.worldObj.rand.nextFloat() * .3F, false);
        }
        if (!DragonVisualState.sitting(phase) && --viaForge$growl<0 && !dragon.isSilent()) {
            String sound=ServerMobSounds.key("entity.enderdragon.growl",5);
            if(sound != null) dragon.worldObj.playSound(dragon.posX,dragon.posY,dragon.posZ,sound,2.5F,.8F+dragon.worldObj.rand.nextFloat()*.3F,false);
            viaForge$growl=200+dragon.worldObj.rand.nextInt(200);
        }
    }
    @ModifyArg(method="onLivingUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;playSound(DDDLjava/lang/String;FFZ)V"),index=3)
    private String wings(String original) {
        if(DragonVisualState.phase((EntityDragon)(Object)this)<0) return original;
        String sound=ServerMobSounds.key("entity.enderdragon.flap",5); return sound == null ? original : sound;
    }
}
