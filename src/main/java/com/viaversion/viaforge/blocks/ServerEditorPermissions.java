package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.compatibility.ClientFeature;
import com.viaversion.viaforge.compatibility.ServerSession;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

/** Main-thread copy of the server's local-player permission level (entity status 24-28). */
public final class ServerEditorPermissions {
    private static EntityPlayer owner;
    private static int level;

    public static void accept(ByteBuf input) {
        int entityId = input.readInt();
        int status = input.readUnsignedByte();
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (status < 24 || status > 28 || player == null || player.getEntityId() != entityId) return;
        owner = player;
        level = status - 24;
    }

    public static boolean canEdit(EntityPlayer player) {
        return ServerSession.has(ClientFeature.BLOCKS) && player != null
                && player == Minecraft.getMinecraft().thePlayer && player == owner
                && player.worldObj == Minecraft.getMinecraft().theWorld
                && level >= 2 && player.capabilities.isCreativeMode && player.capabilities.allowEdit;
    }

    public static void clear() { owner = null; level = 0; }

    private ServerEditorPermissions() { }
}
