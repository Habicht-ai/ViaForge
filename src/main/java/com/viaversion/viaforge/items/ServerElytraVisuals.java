package com.viaversion.viaforge.items;

import com.viaversion.viaforge.common.compatibility.ClientFeature;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.compatibility.ServerSession;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/** Client animation history, independent of the server's metadata and movement authority. */
public final class ServerElytraVisuals {
    private static final Map<Entity, State> STATES = new WeakHashMap<>();
    private static final class State {
        int flightTicks;
        float crawl, previousCrawl;
        final float[] angles = new float[3], previous = new float[3];
    }
    public static void clear() { STATES.clear(); ServerElytraSound.clear(); }
    public static int ticks(Entity entity) {
        if (!ServerElytraFlight.flying(entity)) return 0;
        if (entity == Minecraft.getMinecraft().thePlayer) return ServerElytraFlight.ticks();
        State state = STATES.get(entity);
        return state == null ? 0 : state.flightTicks;
    }
    public static void tick(EntityPlayer player) {
        if (!player.worldObj.isRemote || !ServerSession.has(ClientFeature.ELYTRA)) return;
        State state = STATES.computeIfAbsent(player, ignored -> new State());
        state.flightTicks = ServerElytraFlight.flying(player) ? state.flightTicks + 1 : 0;
        state.previousCrawl = state.crawl;
        state.crawl = ServerElytraFlight.crawling(player) ? Math.min(1,state.crawl+.09F) : Math.max(0,state.crawl-.09F);
        if (ServerSession.rule(ClientRule.TICKED_ELYTRA_WINGS)) {
            float[] target = target(player);
            for (int i = 0; i < 3; i++) {
                state.previous[i] = state.angles[i];
                state.angles[i] += (target[i] - state.angles[i]) * .3F;
            }
        }
        if (player == Minecraft.getMinecraft().thePlayer) ServerElytraSound.tick(player);
    }
    private static float[] target(Entity entity) {
        float x = .2617994F, y = 0, z = -.2617994F;
        if (ServerElytraFlight.flying(entity)) {
            float blend = 1;
            if (entity.motionY < 0) {
                double length = Math.sqrt(entity.motionX * entity.motionX + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ);
                if (length > 0) blend = 1 - (float)Math.pow(-entity.motionY / length, 1.5);
            }
            x += blend * (.34906584F - x); z += blend * (-1.5707964F - z);
        } else if (entity.isSneaking() || ServerElytraFlight.crouching(entity)) { x = .6981317F; y = .08726646F; z = -.7853982F; }
        return new float[]{x, y, z};
    }
    public static float[] wings(Entity entity, float partial) {
        float[] target = target(entity);
        if (!(entity instanceof net.minecraft.client.entity.AbstractClientPlayer)) return target;
        State state = STATES.computeIfAbsent(entity, ignored -> new State());
        for (int i = 0; i < 3; i++) {
            if (ServerSession.rule(ClientRule.TICKED_ELYTRA_WINGS)) {
                target[i] = state.previous[i] + (state.angles[i] - state.previous[i]) * partial;
            } else {
                // Before 1.21.2 this intentionally advances once per render, like ModelElytra.
                state.angles[i] += (target[i] - state.angles[i]) * .1F;
                target[i] = state.angles[i];
            }
        }
        return target;
    }
    public static float limbAmount(Entity entity, float amount) {
        if (ticks(entity) <= 4) return amount;
        float speed = (float)(entity.motionX * entity.motionX + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ) / .2F;
        return amount / Math.max(1, speed * speed * speed);
    }
    public static float crawlAmount(Entity entity, float partial) {
        State state=STATES.get(entity);
        return state==null?0:state.previousCrawl+(state.crawl-state.previousCrawl)*partial;
    }
    private ServerElytraVisuals() { }
}
