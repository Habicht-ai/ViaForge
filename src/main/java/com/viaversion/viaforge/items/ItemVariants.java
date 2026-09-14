package com.viaversion.viaforge.items;

import com.google.gson.*;
import com.viaversion.viaforge.compatibility.ServerSession;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.StatCollector;

/** Vanilla potion recipes and spawn-egg registry data; NBT remains server-owned. */
public final class ItemVariants {
    public static final JsonArray POTIONS, EGGS;
    static {
        try (Reader reader = new InputStreamReader(ItemVariants.class.getResourceAsStream("/assets/viaforge/item-variants.json"), StandardCharsets.UTF_8)) {
            JsonObject data = new JsonParser().parse(reader).getAsJsonObject();
            POTIONS = data.getAsJsonArray("potions"); EGGS = data.getAsJsonArray("eggs");
        } catch (IOException error) { throw new ExceptionInInitializerError(error); }
    }
    public static ItemStack potion(ItemStack stack, String name) {
        NBTTagCompound tag = new NBTTagCompound(); tag.setString("Potion", "minecraft:" + name); stack.setTagCompound(tag); return stack;
    }
    public static ItemStack egg(ItemStack stack, JsonObject egg) {
        NBTTagCompound tag = new NBTTagCompound(), entity = new NBTTagCompound();
        entity.setString("id", ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.NAMESPACED_ENTITY_IDS) ? "minecraft:" + egg.get("name").getAsString() : egg.get("legacy").getAsString());
        tag.setTag("EntityTag", entity); stack.setTagCompound(tag); return stack;
    }
    public static String potionName(ItemStack stack) {
        String name = stack.hasTagCompound() ? stack.getTagCompound().getString("Potion") : "empty";
        return name.startsWith("minecraft:") ? name.substring(10) : name;
    }
    public static JsonObject eggType(ItemStack stack) {
        if (!stack.hasTagCompound()) return null;
        String name = stack.getTagCompound().getCompoundTag("EntityTag").getString("id");
        for (JsonElement element : EGGS) {
            JsonObject egg = element.getAsJsonObject();
            if (name.equals("minecraft:" + egg.get("name").getAsString()) || name.equals(egg.get("legacy").getAsString())) return egg;
        }
        return null;
    }
    public static List<PotionEffect> effects(ItemStack stack) {
        List<PotionEffect> effects = new ArrayList<>();
        for (JsonElement element : POTIONS) {
            JsonObject potion = element.getAsJsonObject();
            if (!potion.get("name").getAsString().equals(potionName(stack))) continue;
            int id = potion.get("effect").getAsInt();
            if (id > 0 && Potion.potionTypes[id] != null) effects.add(new PotionEffect(id, potion.get("duration").getAsInt(), potion.get("amplifier").getAsInt()));
            break;
        }
        if (stack.hasTagCompound()) {
            NBTTagList custom = stack.getTagCompound().getTagList("CustomPotionEffects", 10);
            for (int i = 0; i < custom.tagCount(); i++) {
                PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(custom.getCompoundTagAt(i));
                if (effect != null) effects.add(effect);
            }
        }
        return effects;
    }
    public static int potionColor(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("CustomPotionColor", 99)) return stack.getTagCompound().getInteger("CustomPotionColor");
        List<PotionEffect> effects = effects(stack);
        if (effects.isEmpty()) return potionName(stack).equals("empty") ? 0xf800f8 : 0x385dc6;
        float red = 0, green = 0, blue = 0; int weight = 0;
        for (PotionEffect effect : effects) {
            if (!effect.getIsShowParticles()) continue;
            int color = Potion.potionTypes[effect.getPotionID()].getLiquidColor(), count = effect.getAmplifier() + 1;
            red += count * ((color >> 16) & 255); green += count * ((color >> 8) & 255); blue += count * (color & 255); weight += count;
        }
        return weight == 0 ? 0 : ((int)(red / weight) << 16) | ((int)(green / weight) << 8) | (int)(blue / weight);
    }
    public static String title(String name) {
        StringBuilder result = new StringBuilder();
        for (String part : name.split("_")) { if (part.isEmpty()) continue; if (result.length() > 0) result.append(' '); result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)); }
        return result.toString();
    }
    public static String effectTitle(PotionEffect effect) {
        String name = StatCollector.translateToLocal(effect.getEffectName());
        if (effect.getAmplifier() > 0) name += " " + StatCollector.translateToLocal("potion.potency." + effect.getAmplifier());
        if (effect.getDuration() > 20) name += " (" + Potion.getDurationString(effect) + ")";
        return name;
    }
    private ItemVariants() { }
}
