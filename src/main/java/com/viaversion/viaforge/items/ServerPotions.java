package com.viaversion.viaforge.items;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;

/** Newer server effects and inventory icons; Via continues to handle their movement translation. */
final class ServerPotions extends Potion {
    private static final ResourceLocation ICONS = new ResourceLocation("viaforge", "textures/gui/container/inventory.png");
    private ServerPotions(int id, String name, boolean bad, int color) { super(id, new ResourceLocation(name), bad, color); setPotionName("effect." + name); }
    static void register() {
        add(24, "glowing", false, 9740385); add(25, "levitation", true, 0xceffff);
        add(26, "luck", false, 0x339900); add(27, "unluck", true, 12624973);
    }
    private static void add(int id, String name, boolean bad, int color) { if (Potion.potionTypes[id] == null) new ServerPotions(id, name, bad, color); }
    static void accept(WorldClient world, int operation, ByteBuf input) {
        int entityId = Types.VAR_INT.readPrimitive(input), id = input.readUnsignedByte();
        if (id < 24 || id > 27 || !(Potion.potionTypes[id] instanceof ServerPotions)) return;
        Entity entity = world.getEntityByID(entityId);
        if (entity == null && Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().thePlayer.getEntityId() == entityId) entity = Minecraft.getMinecraft().thePlayer;
        if (!(entity instanceof EntityLivingBase)) return;
        if (operation == 18) ((EntityLivingBase)entity).removePotionEffectClient(id);
        else {
            int amplifier = input.readUnsignedByte(), duration = Types.VAR_INT.readPrimitive(input), flags = input.readUnsignedByte();
            PotionEffect effect = new PotionEffect(id, duration, amplifier, (flags & 1) != 0, (flags & 2) != 0);
            effect.setPotionDurationMax(duration == 32767);
            ((EntityLivingBase)entity).addPotionEffect(effect);
        }
    }
    @Override public boolean shouldRender(PotionEffect effect) { return ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.ENTITY_VISUALS); }
    @Override public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        if (ServerSession.getLoadedResourceVersion() == null) return;
        // These slots are absent from the 1.8 inventory atlas. Use the target
        // atlas in Forge's custom icon hook while retaining the native text/timer.
        int column = getId() == 25 ? 3 : getId() == 24 ? 4 : getId() == 26 ? 5 : 6;
        mc.getTextureManager().bindTexture(ICONS); GlStateManager.color(1, 1, 1, 1);
        Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 7, column * 18, 234, 18, 18, 256, 256);
    }
}
