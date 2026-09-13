package com.viaversion.viaforge.mobs;

import com.google.gson.*;
import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaforge.mixin.impl.items.SoundResourceAccess;
import io.netty.buffer.ByteBuf;
import com.viaversion.viaversion.api.type.Types;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public final class ServerMobSounds {
    private static JsonObject events = new JsonObject();
    private static final Set<String> registered = new HashSet<>();
    public static void reload() {
        registered.clear(); events = new JsonObject();
        try (InputStream input = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("viaforge","mob_sounds.json")).getInputStream()) {
            events = new JsonParser().parse(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException beforeJoin) { }
    }
    public static boolean has(String name) { return events.has(name); }
    public static String key(String name, int category) {
        if (!events.has(name)) return null;
        String key = "mob_sound."+Math.max(0,Math.min(8,category))+"."+name;
        if (registered.add(key)) {
            SoundList list = new SoundList(); list.setReplaceExisting(true); list.setSoundCategory(SoundCategory.values()[Math.max(0,Math.min(8,category))]);
            for (JsonElement element : events.getAsJsonObject(name).getAsJsonArray("sounds")) {
                SoundList.SoundEntry sound = new SoundList.SoundEntry();
                if (element.isJsonPrimitive()) sound.setSoundEntryName("viaforge:"+element.getAsString());
                else {
                    JsonObject definition = element.getAsJsonObject();
                    String path = definition.get("name").getAsString();
                    if (definition.has("type") && definition.get("type").getAsString().equals("event")) {
                        String child = key(path,category); if (child == null) continue;
                        sound.setSoundEntryType(SoundList.SoundEntry.Type.SOUND_EVENT); sound.setSoundEntryName(child);
                    } else sound.setSoundEntryName("viaforge:"+path);
                    if (definition.has("volume")) sound.setSoundEntryVolume(definition.get("volume").getAsFloat());
                    if (definition.has("pitch")) sound.setSoundEntryPitch(definition.get("pitch").getAsFloat());
                    if (definition.has("weight")) sound.setSoundEntryWeight(definition.get("weight").getAsInt());
                    if (definition.has("stream")) sound.setStreaming(definition.get("stream").getAsBoolean());
                }
                list.getSoundList().add(sound);
            }
            ((SoundResourceAccess)Minecraft.getMinecraft().getSoundHandler()).viaForge$loadSound(new ResourceLocation("viaforge",key),list);
        }
        return "viaforge:"+key;
    }
    public static void packet(WorldClient world,int protocol,int operation,ByteBuf input) throws Exception {
        String name = operation == 14 ? MobSoundCatalog.name(BlockVersionProfile.forProtocol(protocol).resourceVersion(),Types.VAR_INT.readPrimitive(input)) : Types.STRING.read(input);
        if (name != null && name.startsWith("minecraft:")) name = name.substring(10);
        int category = Types.VAR_INT.readPrimitive(input);
        double x = input.readInt()/8D, y = input.readInt()/8D, z = input.readInt()/8D;
        float volume = input.readFloat(), pitch = protocol >= 210 ? input.readFloat() : input.readUnsignedByte()/63F;
        String sound = name == null ? null : key(name,category);
        if (sound != null) world.playSound(x,y,z,sound,volume,pitch,false);
    }
    public static String entity(Entity entity,String event) {
        MobState state = entity instanceof ServerMob ? ((ServerMob)entity).state : ServerMobs.get(entity);
        if (state == null || state.kind == null) return null;
        String name = state.kind.name().toLowerCase(Locale.ROOT);
        switch (state.kind) {
            case EVOKER: name = "evocation_illager"; break;
            case VINDICATOR: name = "vindication_illager"; break;
            case ILLUSIONER: name = "illusion_illager"; break;
            case MOOSHROOM: name = "cow"; break;
            case CAVE_SPIDER: name = "spider"; break;
            case OCELOT: name = "cat"; break;
            case ZOMBIE_PIGMAN: name = "zombie_pig"; break;
            case MAGMA_CUBE: name = "magmacube"; break;
            case ELDER_GUARDIAN: name = "elder_guardian"; break;
            case IRON_GOLEM: name = "irongolem"; break;
            case SNOW_GOLEM: name = "snowman"; break;
            case ENDER_DRAGON: name = "enderdragon"; break;
            default: break;
        }
        if (entity instanceof net.minecraft.entity.monster.EntitySlime && ((net.minecraft.entity.monster.EntitySlime)entity).getSlimeSize() <= 1) name = "small_" + name;
        if (state.kind == MobKind.SHULKER && event.equals("hurt") && state.number(state.first+2,0) == 0) event = "hurt_closed";
        if (state.kind == MobKind.POLAR_BEAR && state.baby() && event.equals("ambient")) event = "baby_ambient";
        if (state.kind == MobKind.GUARDIAN || state.kind == MobKind.ELDER_GUARDIAN) if (!entity.isInWater() && (event.equals("hurt") || event.equals("death") || event.equals("ambient"))) event += "_land";
        int category = state.kind.id >= 90 && state.kind != MobKind.IRON_GOLEM || state.kind == MobKind.BAT ? 6 : 5;
        return key("entity."+name+"."+event,category);
    }
    private ServerMobSounds() { }
}
