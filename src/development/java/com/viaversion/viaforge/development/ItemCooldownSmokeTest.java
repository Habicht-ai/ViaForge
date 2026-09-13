package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.network.*;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.*;
import net.minecraft.world.WorldSettings;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.*;

final class ItemCooldownSmokeTest {
    static void verify(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, NetHandlerPlayClient handler, WorldClient world) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        ItemStack previous = mc.thePlayer.getHeldItem(); PlayerControllerMP oldController = mc.playerController;
        boolean creative = mc.thePlayer.capabilities.isCreativeMode;
        List<Packet> outgoing = new ArrayList<>();
        NetHandlerPlayClient sender = new NetHandlerPlayClient(mc, null, new NetworkManager(EnumPacketDirection.CLIENTBOUND), new GameProfile(new UUID(0, 73), "CooldownTest")) {
            @Override public void addToSendQueue(Packet packet) { outgoing.add(packet); }
        };
        mc.playerController = new PlayerControllerMP(mc, sender);
        mc.playerController.setGameType(WorldSettings.GameType.SURVIVAL);
        BlockPos pos = new BlockPos(8, 75, 8);
        net.minecraft.block.state.IBlockState oldBlock = world.getBlockState(pos), oldAbove = world.getBlockState(pos.up());
        ServerItemCooldowns.clear();
        try {
            ItemStack chorus = stack(432, 0), pearl = new ItemStack(Items.ender_pearl, 8), shield = stack(442, 0);
            cooldown(profile, client, server, handler, 432, 20);
            require(ServerItemCooldowns.fraction(chorus, 0) == 1 && !ServerItemCooldowns.cooling(pearl), "Original cooldown targets the imported item only");
            for (int tick = 0; tick < 10; tick++) ServerItemCooldowns.tick(mc.thePlayer);
            require(ServerItemCooldowns.fraction(chorus, 0) == .5F && Math.abs(ServerItemCooldowns.fraction(chorus, .5F) - .475F) < .00001F, "Vanilla tick and render interpolation");
            ItemStack second = chorus.copy(); second.setStackDisplayName("Second slot");
            require(ServerItemCooldowns.cooling(second), "Cooldown shared across stacks/NBT/slots");
            hold(mc, second); mc.thePlayer.clearItemInUse(); outgoing.clear();
            require(!mc.playerController.sendUseItem(mc.thePlayer, world, second) && !mc.thePlayer.isUsingItem(), "Cooling chorus cannot restart eating");
            require(outgoing.stream().anyMatch(packet -> packet instanceof C08PacketPlayerBlockPlacement), "Vanilla still sends use packet during cooldown");
            for (int tick = 0; tick < 10; tick++) ServerItemCooldowns.tick(mc.thePlayer);
            require(!ServerItemCooldowns.cooling(second), "Cooldown expires on exact twentieth tick");
            mc.playerController.sendUseItem(mc.thePlayer, world, second);
            require(mc.thePlayer.getItemInUse() == second && mc.thePlayer.getItemInUseCount() == 32, "Chorus can eat again for vanilla 32 ticks");
            mc.thePlayer.clearItemInUse();
            second.getItem().onItemUseFinish(second, world, mc.thePlayer);
            require(!ServerItemCooldowns.cooling(second), "Chorus cooldown waits for server confirmation, unlike pearl prediction");
            cooldown(profile, client, server, handler, 442, 100); hold(mc, shield);
            require(!mc.playerController.sendUseItem(mc.thePlayer, world, shield) && !mc.thePlayer.isUsingItem(), "Disabled shield cannot start blocking");
            cooldown(profile, client, server, handler, 442, 5);
            for (int tick = 0; tick < 4; tick++) ServerItemCooldowns.tick(mc.thePlayer);
            require(ServerItemCooldowns.cooling(shield), "Server replacement duration takes effect");
            cooldown(profile, client, server, handler, 442, 0);
            require(!ServerItemCooldowns.cooling(shield), "Server zero duration removes cooldown immediately");
            mc.playerController.sendUseItem(mc.thePlayer, world, shield);
            require(mc.thePlayer.getItemInUse() == shield, "Shield can block after removal"); mc.thePlayer.clearItemInUse();
            for (boolean mode : new boolean[]{false, true}) {
                ServerItemCooldowns.clear(); mc.thePlayer.capabilities.isCreativeMode = mode;
                pearl.stackSize = 8; hold(mc, pearl);
                require(mc.playerController.sendUseItem(mc.thePlayer, world, pearl), "Pearl reports successful use in both game modes");
                require(ServerItemCooldowns.fraction(pearl, 0) == 1 && pearl.stackSize == (mode ? 8 : 7), "Pearl predicts cooldown in survival and creative");
                mc.playerController.sendUseItem(mc.thePlayer, world, pearl);
                require(pearl.stackSize == (mode ? 8 : 7), "Cooling pearl does not consume another item");
                cooldown(profile, client, server, handler, 368, 40);
                for (int tick = 0; tick < 20; tick++) ServerItemCooldowns.tick(mc.thePlayer);
                require(ServerItemCooldowns.fraction(pearl, 0) == .5F, "Server duration replaces pearl prediction");
            }
            mc.thePlayer.capabilities.isCreativeMode = false;
            ItemStack flint = new ItemStack(Items.flint_and_steel); hold(mc, flint);
            cooldown(profile, client, server, handler, 259, 40);
            world.setBlockState(pos, Blocks.stone.getDefaultState(), 0); world.setBlockToAir(pos.up());
            require(!mc.playerController.onPlayerRightClick(mc.thePlayer, world, flint, pos, EnumFacing.UP, new Vec3(8.5, 76, 8.5)), "Item-on-block action observes cooldown");
            require(world.isAirBlock(pos.up()) && flint.getItemDamage() == 0, "Cooling action cannot predict fire or damage");
            world.setBlockState(pos, Blocks.lever.getDefaultState(), 0);
            require(mc.playerController.onPlayerRightClick(mc.thePlayer, world, flint, pos, EnumFacing.UP, new Vec3(8.5, 76, 8.5)), "Block activation remains available with cooling item");
            cooldown(profile, client, server, handler, 397, 20);
            require(ServerItemCooldowns.cooling(stack(397, 5)) && ServerItemCooldowns.cooling(new ItemStack(Items.skull)), "Native and imported variants share original item ID");
            ByteBuf respawn = packet(profile, "RESPAWN"); respawn.writeInt(0).writeByte(0).writeByte(1); Types.STRING.write(respawn, "default"); send(client, server, handler, respawn);
            require(!ServerItemCooldowns.cooling(flint), "Same-world respawn clears item cooldowns");
            ItemCooldownRenderSmokeTest.verify(profile, chorus, pearl, shield);
        } finally {
            mc.thePlayer.clearItemInUse(); hold(mc, previous); mc.thePlayer.capabilities.isCreativeMode = creative;
            mc.playerController = oldController; ServerItemCooldowns.clear();
            world.setBlockState(pos, oldBlock, 0); world.setBlockState(pos.up(), oldAbove, 0);
        }
    }
    private static void hold(Minecraft mc, ItemStack stack) { mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = stack; }
    private static void cooldown(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, NetHandlerPlayClient handler, int item, int duration) throws Exception {
        ByteBuf packet = packet(profile, "COOLDOWN"); Types.VAR_INT.writePrimitive(packet, item); Types.VAR_INT.writePrimitive(packet, duration); send(client, server, handler, packet);
    }
}
