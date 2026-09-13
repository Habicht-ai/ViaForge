package com.viaversion.viaforge.development;

import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.potion.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import java.nio.file.Paths;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.*;

final class ServerPotionStatusSmokeTest {
    static void verify(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, NetHandlerPlayClient handler) throws Exception {
        Minecraft mc = Minecraft.getMinecraft(); int player = mc.thePlayer.getEntityId();
        try {
            for (int id = 24; id <= 27; id++) {
                apply(profile, client, server, handler, player, id, 1, 200, 3);
                PotionEffect effect = mc.thePlayer.getActivePotionEffect(Potion.potionTypes[id]);
                require(effect != null && effect.getAmplifier() == 1 && effect.getDuration() == 200 && effect.getIsAmbient() && effect.getIsShowParticles(), "Original mob effect reaches native inventory " + id);
                require(Potion.potionTypes[id].shouldRender(effect), "New effect is visible in inventory");
                for (int tick = 0; tick < 20; tick++) effect.onUpdate(mc.thePlayer);
                require(Potion.getDurationString(effect).equals("0:09"), "Native effect duration counts down");
                apply(profile, client, server, handler, player, id, 2, 600, 0);
                effect = mc.thePlayer.getActivePotionEffect(Potion.potionTypes[id]);
                require(effect.getAmplifier() == 2 && effect.getDuration() == 600 && !effect.getIsShowParticles(), "Updated effect level/duration/particle flag");
                ByteBuf remove = packet(profile, "REMOVE_MOB_EFFECT"); Types.VAR_INT.writePrimitive(remove, player); remove.writeByte(id); send(client, server, handler, remove);
                require(!mc.thePlayer.isPotionActive(id), "Original effect removal reaches client " + id);
            }
            apply(profile, client, server, handler, player, 25, 0, 32767, 2);
            require(mc.thePlayer.getActivePotionEffect(Potion.potionTypes[25]).getIsPotionDurationMax(), "Long effect keeps vanilla duration marker");
            mc.thePlayer.removePotionEffectClient(25);
            apply(profile, client, server, handler, player, 25, 0, 200, 2);
            try (java.io.InputStream icon = mc.getResourceManager().getResource(new ResourceLocation("viaforge", "textures/gui/container/inventory.png")).getInputStream()) {
                java.awt.image.BufferedImage atlas = javax.imageio.ImageIO.read(icon);
                require(atlas != null && atlas.getWidth() == 256 && atlas.getHeight() == 256, "Target inventory effect atlas loaded");
                int visible = 0;
                for (int x = 54; x < 72; x++) for (int y = 234; y < 252; y++) if ((atlas.getRGB(x, y) >>> 24) != 0) visible++;
                require(visible > 0, "Original levitation icon present");
            }
            if (profile == BlockVersionProfile.V1_9 || profile == BlockVersionProfile.V1_12_2) {
                GuiScreen previous = mc.currentScreen;
                PlayerControllerMP previousController = mc.playerController;
                try {
                    mc.playerController = new PlayerControllerMP(mc, handler);
                    mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
                    mc.getFramebuffer().framebufferClear(); mc.getFramebuffer().bindFramebuffer(true); mc.entityRenderer.setupOverlayRendering();
                    mc.currentScreen.drawScreen(0, 0, 0);
                    ScreenShotHelper.saveScreenshot(Paths.get(System.getenv("VIAFORGE_BLOCK_SMOKE_TEST")).toAbsolutePath().getParent().toFile(), "levitation-inventory-" + profile.resourceVersion() + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                } finally { mc.displayGuiScreen(previous); mc.playerController = previousController; }
            }
        } finally {
            for (int id = 24; id <= 27; id++) { ByteBuf remove = packet(profile, "REMOVE_MOB_EFFECT"); Types.VAR_INT.writePrimitive(remove, player); remove.writeByte(id); send(client, server, handler, remove); mc.thePlayer.removePotionEffectClient(id); }
        }
    }
    private static void apply(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, NetHandlerPlayClient handler, int player, int id, int amplifier, int duration, int flags) throws Exception {
        ByteBuf packet = packet(profile, "UPDATE_MOB_EFFECT"); Types.VAR_INT.writePrimitive(packet, player); packet.writeByte(id).writeByte(amplifier); Types.VAR_INT.writePrimitive(packet, duration); packet.writeByte(flags);
        send(client, server, handler, packet);
    }
}
