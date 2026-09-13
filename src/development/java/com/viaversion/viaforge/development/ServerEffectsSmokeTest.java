package com.viaversion.viaforge.development;

import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.particle.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.network.*;
import net.minecraft.util.*;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.*;

/** Real translated wire packets and real particle factories, including server version boundaries. */
final class ServerEffectsSmokeTest {
    static void pipeline(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, NetHandlerPlayClient handler, WorldClient world) throws Exception {
        Minecraft mc = Minecraft.getMinecraft(); EffectRenderer previous = mc.effectRenderer; Entity camera = mc.getRenderViewEntity();
        int settings = mc.gameSettings.particleSetting; Recorder effects = new Recorder(world); mc.effectRenderer = effects;
        mc.thePlayer.setPosition(4, 75, 4); mc.setRenderViewEntity(mc.thePlayer); mc.gameSettings.particleSetting = 0;
        try {
            for (int type = 0; type < ItemVariants.POTIONS.size(); type++) {
                int color = ServerPotionImpact.color(107, type);
                boolean instant = ServerPotionImpact.instant(107, 2002, type);
                effects.reset(); ByteBuf event = packet(profile, "LEVEL_EVENT");
                event.writeInt(profile.protocol() >= 315 && instant ? 2007 : 2002).writeLong(new BlockPos(4, 75, 4).toLong());
                event.writeInt(profile.protocol() >= 315 ? color : type).writeBoolean(false);
                singleVisual(client, server, handler, event);
                require(effects.created.size() == 108, "Exactly one vanilla potion burst (8 fragments + 100 spells), " + profile + "/" + type + ": " + effects.created.size());
                for (int i = 0; i < 108; i++) {
                    int id = effects.ids.get(i);
                    require(id == (i < 8 ? 36 : instant ? 14 : 13), "Original instant/normal potion particle type");
                    if (i < 8) continue;
                    EntityFX fx = effects.created.get(i);
                    double[] rgb = {fx.getRedColorF(), fx.getGreenColorF(), fx.getBlueColorF()};
                    int[] expected = {color >> 16 & 255, color >> 8 & 255, color & 255};
                    for (int c = 0; c < 3; c++) require(expected[c] == 0 ? rgb[c] == 0 : rgb[c] * 255 / expected[c] >= .74999 && rgb[c] * 255 / expected[c] <= 1.00001, "Original potion RGB, no blue replacement");
                }
            }
            if (profile.protocol() >= 315) {
                effects.reset(); ByteBuf event = packet(profile, "LEVEL_EVENT");
                event.writeInt(2007).writeLong(new BlockPos(4, 75, 4).toLong()).writeInt(0xff0000).writeBoolean(false);
                singleVisual(client, server, handler, event);
                for (int i = 8; i < 108; i++) require(effects.created.get(i).getGreenColorF() == 0 && effects.created.get(i).getBlueColorF() == 0, "Custom red impact remains red");
            }
            for (int type : new int[]{60, 91}) {
                int id = 800 + type; ByteBuf add = packet(profile, "ADD_ENTITY"); Types.VAR_INT.writePrimitive(add, id);
                add.writeLong(0).writeLong(id).writeByte(type).writeDouble(4.5).writeDouble(76).writeDouble(4.5);
                add.writeByte(0).writeByte(0).writeInt(1).writeShort(1600).writeShort(800).writeShort(-400);
                send(client, server, handler, add);
                require(world.getEntityByID(id) instanceof ServerArrow, "Arrow spawn retained " + type + " / " + profile);
                ServerArrow arrow = (ServerArrow)world.getEntityByID(id);
                require(arrow.spectral == (type == 91) && arrow.motionX == .2 && arrow.motionY == .1 && arrow.motionZ == -.05, "Original arrow spawn velocity (including owner=0)");
                require(mc.getRenderManager().<net.minecraft.entity.projectile.EntityArrow>getEntityRenderObject(arrow) instanceof ServerArrowRenderer, "Native projectile mesh registered");
                effects.reset(); arrow.onUpdate();
                require(effects.created.size() == (type == 91 ? 1 : 0), "Normal arrows have no potion particles; spectral emits once");
                if (type == 60) {
                    int first = profile.protocol() >= 210 ? 6 : 5;
                    ByteBuf data = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(data, id);
                    data.writeByte(first + 1).writeByte(1); Types.VAR_INT.writePrimitive(data, 0x9612bc); data.writeByte(255);
                    send(client, server, handler, data); require(arrow.color == 0x9612bc, "Tipped arrow original color metadata");
                    effects.reset(); arrow.onUpdate(); require(effects.created.size() == 2, "Two flight particles per tipped arrow tick");
                    for (EntityFX fx : effects.created) require(Math.abs(fx.getRedColorF() - 150 / 255F) < .0001 && Math.abs(fx.getBlueColorF() - 188 / 255F) < .0001, "Arrow particle RGB");
                    effects.reset(); ByteBuf expire = packet(profile, "ENTITY_EVENT"); expire.writeInt(id).writeByte(0);
                    send(client, server, handler, expire); require(effects.created.size() == 20, "Potion expiration burst through original status packet");
                }
                world.setBlockState(new BlockPos(4, 74, 4), Blocks.stone.getDefaultState());
                arrow.setPosition(4.5, 76, 4.5); arrow.setVelocity(0, -1, 0);
                com.viaversion.viaforge.mixin.impl.items.ArrowStateAccess state = (com.viaversion.viaforge.mixin.impl.items.ArrowStateAccess)(Object)arrow;
                for (int i = 0; i < 5 && !state.viaForge$inGround(); i++) arrow.onUpdate();
                require(state.viaForge$inGround(), "Arrow actually collides with native block");
                effects.reset(); for (int i = 0; i < 10; i++) arrow.onUpdate();
                require(effects.created.size() == (type == 60 ? 2 : 0), "Grounded arrow particle cadence " + type);
                ByteBuf remove = packet(profile, "REMOVE_ENTITIES"); Types.VAR_INT.writePrimitive(remove, 1); Types.VAR_INT.writePrimitive(remove, id);
                send(client, server, handler, remove); require(world.getEntityByID(id) == null && ServerEntityViews.get(id) == null, "Arrow cleanup");
            }
            if (profile.protocol() >= 315) {
                ServerTotemAnimation.clear(); effects.reset();
                ByteBuf status = packet(profile, "ENTITY_EVENT"); status.writeInt(612).writeByte(35);
                singleVisual(client, server, handler, status);
                require(effects.totems > 0 && timer() == 0, "Remote activation has particles without local overlay");
                effects.reset(); status = packet(profile, "ENTITY_EVENT"); status.writeInt(mc.thePlayer.getEntityId()).writeByte(35);
                singleVisual(client, server, handler, status);
                require(effects.totems > 0 && timer() == 40, "Local activation starts original 40-tick animation");
                for (int i = 0; i < 29; i++) ServerTotemAnimation.tick();
                int count = effects.totems; ServerTotemAnimation.tick(); require(effects.totems == count, "Emitter stops after 30 ticks");
                for (int i = 0; i < 10; i++) ServerTotemAnimation.tick(); require(timer() == 0, "Animation ends at tick 40");
                require(mc.getSoundHandler().getSound(new ResourceLocation("viaforge", "totem_use")).getWeight() == 1, "Original Totem recording registered once");
                effects.reset(); ByteBuf particle = packet(profile, "LEVEL_PARTICLES");
                particle.writeInt(47).writeBoolean(false).writeFloat(4).writeFloat(75).writeFloat(4);
                particle.writeFloat(.1F).writeFloat(.1F).writeFloat(.1F).writeFloat(.2F).writeInt(5);
                singleVisual(client, server, handler, particle); require(effects.totems == 5, "Explicit Totem particle packets retain original type");
            } else {
                ServerTotemAnimation.activate(mc.thePlayer); require(timer() == 0, "No totem animation before 1.11");
            }
        } finally {
            mc.effectRenderer = previous; mc.setRenderViewEntity(camera); mc.gameSettings.particleSetting = settings;
            for (int id : new int[]{860, 891}) world.removeEntityFromWorld(id); ServerTotemAnimation.clear();
        }
    }
    static int timer() throws Exception { Field field = ServerTotemAnimation.class.getDeclaredField("remaining"); field.setAccessible(true); return field.getInt(null); }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void singleVisual(EmbeddedChannel client, EmbeddedChannel server, NetHandlerPlayClient handler, ByteBuf source) throws Exception {
        BlockPipelineSmokeTest.receiveCompressed(client, server, source); ByteBuf data = client.readInbound();
        require(data != null, "Visual event delivered");
        try {
            require(Types.VAR_INT.readPrimitive(data) == 0x3f, "Original effect replaces Via's fallback");
            Packet packet = EnumConnectionState.PLAY.getPacket(EnumPacketDirection.CLIENTBOUND, 0x3f);
            packet.readPacketData(new PacketBuffer(data)); packet.processPacket(handler);
        } finally { data.release(); }
        ByteBuf extra = client.readInbound(); if (extra != null) { extra.release(); throw new AssertionError("Duplicate replacement particles/sounds"); }
    }
    private static final class Recorder extends EffectRenderer {
        final List<EntityFX> created = new ArrayList<>(); final List<Integer> ids = new ArrayList<>(); int totems;
        Recorder(WorldClient world) { super(world, Minecraft.getMinecraft().getTextureManager()); }
        void reset() { created.clear(); ids.clear(); totems = 0; }
        @Override public EntityFX spawnEffectParticle(int id, double x, double y, double z, double vx, double vy, double vz, int... args) {
            EntityFX effect = super.spawnEffectParticle(id, x, y, z, vx, vy, vz, args);
            if (effect != null) { created.add(effect); ids.add(id); } return effect;
        }
        @Override public void addEffect(EntityFX effect) {
            if (effect instanceof ServerTotemParticle) { totems++; require(effect.getBrightnessForRender(0) == 0xf000f0, "Fullbright Totem spark"); }
            super.addEffect(effect);
        }
    }
}
