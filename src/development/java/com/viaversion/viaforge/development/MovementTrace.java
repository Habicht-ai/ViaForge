package com.viaversion.viaforge.development;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.SharedMonsterAttributes;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;

/** Development-only observation. Never writes player state or sends packets. */
public final class MovementTrace {
    public static void sample(Object entity,String phase) {
        String folder=System.getenv("VIAFORGE_SNEAK_PROBE");
        if(folder==null||entity!=Minecraft.getMinecraft().thePlayer)return;
        EntityPlayerSP p=(EntityPlayerSP)entity;
        if(p.ticksExisted>12000)return;
        try {
            JsonObject j=new JsonObject();j.addProperty("phase",phase);j.addProperty("time_ms",System.currentTimeMillis());j.addProperty("player_tick",p.ticksExisted);
            j.addProperty("key_forward",Minecraft.getMinecraft().gameSettings.keyBindForward.isKeyDown());j.addProperty("key_back",Minecraft.getMinecraft().gameSettings.keyBindBack.isKeyDown());
            j.addProperty("key_left",Minecraft.getMinecraft().gameSettings.keyBindLeft.isKeyDown());j.addProperty("key_right",Minecraft.getMinecraft().gameSettings.keyBindRight.isKeyDown());
            j.addProperty("key_sneak",Minecraft.getMinecraft().gameSettings.keyBindSneak.isKeyDown());j.addProperty("key_sprint",Minecraft.getMinecraft().gameSettings.keyBindSprint.isKeyDown());j.addProperty("key_jump",Minecraft.getMinecraft().gameSettings.keyBindJump.isKeyDown());
            j.addProperty("input_forward",p.movementInput.moveForward);j.addProperty("input_strafe",p.movementInput.moveStrafe);j.addProperty("input_sneak",p.movementInput.sneak);
            j.addProperty("travel_forward",p.moveForward);j.addProperty("travel_strafe",p.moveStrafing);
            j.addProperty("sprinting",p.isSprinting());j.addProperty("sneaking",p.isSneaking());j.addProperty("native_sprint_ticks",p.sprintingTicksLeft);
            j.addProperty("speed_attribute",p.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue());
            j.addProperty("height",p.height);j.addProperty("eye_height",p.getEyeHeight());j.addProperty("water",p.isInWater());j.addProperty("eye_water",com.viaversion.viaforge.compatibility.ServerSwimming.eye(p));
            j.addProperty("swimming",com.viaversion.viaforge.compatibility.ServerSwimming.swimming(p));j.addProperty("crawling",com.viaversion.viaforge.items.ServerElytraFlight.crawling(p));
            j.addProperty("x",p.posX);j.addProperty("y",p.posY);j.addProperty("z",p.posZ);j.addProperty("vx",p.motionX);j.addProperty("vy",p.motionY);j.addProperty("vz",p.motionZ);
            j.addProperty("box",p.getEntityBoundingBox().toString());j.addProperty("ground",p.onGround);j.addProperty("collision_h",p.isCollidedHorizontally);j.addProperty("collision_v",p.isCollidedVertically);
            java.lang.reflect.Field states=com.viaversion.viaforge.compatibility.ServerSwimming.class.getDeclaredField("STATES");states.setAccessible(true);
            Object state=((java.util.Map<?,?>)states.get(null)).get(p);
            if(state!=null)for(String name:new String[]{"previousSneak","previousForward","slowMovement","sprintWindow"}) {
                java.lang.reflect.Field f=state.getClass().getDeclaredField(name);f.setAccessible(true);Object value=f.get(state);
                if(value instanceof Boolean)j.addProperty(name,(Boolean)value);else if(value instanceof Number)j.addProperty(name,(Number)value);
            }
            Files.write(Paths.get(folder,"movement.jsonl"),Collections.singleton(j.toString()),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }catch(Exception failure){throw new IllegalStateException("Movement observation failed",failure);}
    }
    private MovementTrace(){}
}
