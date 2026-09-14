package com.viaversion.viaforge.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.blocks.LegacyItemDefinition;
import com.viaversion.viaforge.mixin.impl.items.MinecraftItemTimer;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

/** The 1.9-1.12 tracker is keyed by item type, shared by all slots and stack variants. */
public final class ServerItemCooldowns {
    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();
    private static EntityPlayer owner;
    private static long tick;
    private static final class Entry {
        final long start, end;
        Entry(long start, int duration) { this.start = start; end = start + duration; }
    }
    public static void clear() { ENTRIES.clear(); owner = null; tick = 0; }
    private static boolean active() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.COOLDOWNS) || mc.thePlayer == null) return false;
        if (owner != mc.thePlayer) { clear(); owner = mc.thePlayer; }
        return true;
    }
    public static int itemId(ItemStack stack) {
        if (stack == null) return -1;
        int id = Item.getIdFromItem(stack.getItem());
        LegacyItemDefinition imported = ClientItems.serverItem(id);
        return imported == null ? id : imported.itemId();
    }
    public static void accept(ByteBuf input) { set(Types.VAR_INT.readPrimitive(input), Types.VAR_INT.readPrimitive(input)); }
    public static void set(int item, int duration) {
        if (!active() || item < 0) return;
        if (duration <= 0) ENTRIES.remove(item);
        else ENTRIES.put(item, new Entry(tick, duration));
    }
    public static void tick(EntityPlayer player) {
        if (player != Minecraft.getMinecraft().thePlayer || !active()) return;
        tick++;
        Iterator<Entry> entries = ENTRIES.values().iterator();
        while (entries.hasNext()) if (entries.next().end <= tick) entries.remove();
    }
    public static boolean cooling(ItemStack stack) { return fraction(stack, 0) > 0; }
    public static float fraction(ItemStack stack, float partial) {
        if (stack == null || !active()) return 0;
        Entry entry = ENTRIES.get(itemId(stack));
        return entry == null ? 0 : MathHelper.clamp_float((float)(entry.end - tick - partial) / (entry.end - entry.start), 0, 1);
    }
    public static void render(ItemStack stack, int x, int y) {
        float remaining = fraction(stack, ((MinecraftItemTimer)Minecraft.getMinecraft()).viaForge$itemTimer().renderPartialTicks);
        if (remaining <= 0) return;
        // Vanilla covers the remaining part with white at alpha 127, including
        // item counts/durability. The same hook serves hotbar and inventory slots.
        int top = y + MathHelper.floor_float(16 * (1 - remaining));
        int height = MathHelper.ceiling_float_int(16 * remaining);
        GlStateManager.disableLighting(); GlStateManager.disableDepth(); GlStateManager.disableTexture2D();
        GlStateManager.enableBlend(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        WorldRenderer vertices = Tessellator.getInstance().getWorldRenderer();
        vertices.begin(7, DefaultVertexFormats.POSITION_COLOR);
        vertices.pos(x, top, 0).color(255, 255, 255, 127).endVertex();
        vertices.pos(x, top + height, 0).color(255, 255, 255, 127).endVertex();
        vertices.pos(x + 16, top + height, 0).color(255, 255, 255, 127).endVertex();
        vertices.pos(x + 16, top, 0).color(255, 255, 255, 127).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D(); GlStateManager.enableLighting(); GlStateManager.enableDepth();
    }
    private ServerItemCooldowns() { }
}
