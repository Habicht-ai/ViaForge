package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.blocks.EditorBlockEntity;
import com.viaversion.viaforge.blocks.StructureBlockRenderer;
import com.viaversion.viaforge.blocks.gui.*;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import com.viaversion.viaversion.api.protocol.packet.PacketType;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_1;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_9;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ServerboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ServerboundPackets1_12_1;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.network.*;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.*;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.*;

/** Real tile decoding, editor controls, and compressed outgoing packets at the target protocol. */
final class BlockEditorSmokeTest {
    static void verify(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, WorldClient world) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient previousWorld = mc.theWorld;
        EntityPlayerSP previousPlayer = mc.thePlayer;
        GuiScreen previousScreen = mc.currentScreen;
        boolean previousFocus = mc.inGameHasFocus;
        net.minecraft.entity.Entity previousView = mc.getRenderViewEntity();
        List<C17PacketCustomPayload> sent = new ArrayList<>();
        NetHandlerPlayClient handler = new NetHandlerPlayClient(mc, null, new NetworkManager(EnumPacketDirection.CLIENTBOUND),
                new GameProfile(new UUID(0, 7), "BlockEditorTest")) {
            @Override public void addToSendQueue(Packet packet) {
                if (packet instanceof C17PacketCustomPayload) sent.add((C17PacketCustomPayload) packet);
            }
        };
        mc.theWorld = world;
        mc.thePlayer = new EntityPlayerSP(mc, world, handler, new StatFileWriter());
        mc.thePlayer.capabilities.isCreativeMode = true; mc.thePlayer.capabilities.allowEdit = true;
        try {
            int count = profile.protocol() >= 210 ? 4 : 3;
            ChunkSection[] sections = new ChunkSection[16]; sections[7] = new ChunkSectionImpl(true);
            sections[7].palette(PaletteType.BLOCKS).addId(0);
            sections[7].getLight().setBlockLight(new byte[2048]); sections[7].getLight().setSkyLight(new byte[2048]);
            List<CompoundTag> tags = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                BlockPos pos = pos(i);
                sections[7].palette(PaletteType.BLOCKS).setIdAt((pos.getZ() & 15) * 16 + pos.getX(), raw(i));
                tags.add(tag(i));
            }
            ByteBuf chunk = BlockPipelineSmokeTest.packet(0x20);
            BaseChunk value = new BaseChunk(0, 0, true, false, 128, sections, new int[256], tags);
            if (profile.hasChunkBlockEntities()) new ChunkType1_9_3(true).write(chunk, value); else new ChunkType1_9_1(true).write(chunk, value);
            BlockPipelineSmokeTest.receiveCompressed(client, server, chunk);
            ByteBuf nativeChunk = BlockPipelineSmokeTest.take(client, 0x21);
            try {
                S21PacketChunkData packet = new S21PacketChunkData(); packet.readPacketData(new PacketBuffer(nativeChunk));
                world.getChunkFromChunkCoords(0, 0).fillChunk(packet.getExtractedDataBytes(), packet.getExtractedSize(), true);
            } finally { nativeChunk.release(); }
            apply(client, handler);
            for (int i = 0; i < count; i++) {
                EditorBlockEntity tile = (EditorBlockEntity) world.getTileEntity(pos(i));
                require(tile != null && tile.received() == profile.hasChunkBlockEntities(), "Editor tile created with chunk NBT");
                IBlockState state = world.getBlockState(pos(i));
                mc.thePlayer.capabilities.isCreativeMode = false;
                require(!activate(world, state, pos(i)), "Creative-only editor activation");
                mc.thePlayer.capabilities.isCreativeMode = true;
                require(activate(world, state, pos(i)), "Editor opens for block " + tile.serverBlockId());
                require(mc.currentScreen instanceof BlockEditorScreen, "Native GUI was opened");
                if (!tile.received()) require(!button(mc.currentScreen, 0).enabled, "Cannot save before server data");
                ByteBuf update = BlockPipelineSmokeTest.packet(0x09);
                Types.BLOCK_POSITION1_8.write(update, new BlockPosition(pos(i).getX(), pos(i).getY(), pos(i).getZ()));
                update.writeByte(i == 3 ? 7 : 2); Types.NAMED_COMPOUND_TAG.write(update, tag(i));
                BlockPipelineSmokeTest.receiveCompressed(client, server, update);
                require(apply(client, handler) > 0, "Original tile update reaches the native client");
                require(tile.received() && button(mc.currentScreen, 0).enabled, "Server data enables the open editor");
                if (i < 3) command(profile, client, server, tile, sent, i);
                else structure(profile, client, server, tile, sent);
            }
            if (profile == BlockVersionProfile.V1_12_2) capture(world);
            // Command mode changes retain the tile, while replacing it with air clears it.
            EditorBlockEntity retained = (EditorBlockEntity) world.getTileEntity(pos(0));
            world.setBlockState(pos(0), net.minecraft.block.Block.BLOCK_STATE_IDS.getByValue(ClientBlocks.localState(211 << 4 | 2)), 3);
            require(world.getTileEntity(pos(0)) == retained, "Command mode switch retains received editor data");
            world.setBlockToAir(pos(0));
            require(world.getTileEntity(pos(0)) == null, "Removed editor data does not survive its block");
        } finally {
            for (C17PacketCustomPayload packet : sent) packet.getBufferData().release();
            if (mc.currentScreen instanceof BlockEditorScreen) mc.currentScreen.onGuiClosed();
            mc.currentScreen = previousScreen; mc.thePlayer = previousPlayer; mc.theWorld = previousWorld;
            mc.setRenderViewEntity(previousView);
            if (!previousFocus) mc.setIngameNotInFocus();
        }
    }

    private static void command(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, EditorBlockEntity tile,
            List<C17PacketCustomPayload> sent, int index) throws Exception {
        Minecraft mc = Minecraft.getMinecraft(); GuiScreen screen = mc.currentScreen;
        require(screen instanceof CommandBlockScreen && field(screen, "command").getText().equals("say original " + index),
                "Command text restored: " + profile + " block=" + index + " actual=" + field(screen, "command").getText() + " snapshot=" + tile.snapshot());
        require(field(screen, "output").getText().equals("previous result"), "JSON command output decoded");
        field(screen, "command").setText("say Gr\u00fcn " + index);
        click(screen, 2); click(screen, 3); click(screen, 4); click(screen, 5); click(screen, 0);
        require(mc.currentScreen == null && sent.size() == 1, "Done sends exactly one command update");
        ByteBuf packet = send(profile, client, server, sent.remove(0));
        try {
            require(Types.STRING.read(packet).equals("MC|AutoCmd"), "Native command channel preserved");
            position(packet, tile.getPos());
            require(Types.STRING.read(packet).equals("say Gr\u00fcn " + index) && !packet.readBoolean(), "Command and output flag preserved");
            require(Types.STRING.read(packet).equals(new String[]{"SEQUENCE", "REDSTONE", "AUTO"}[index]), "Command mode preserved");
            require(packet.readBoolean() == (index != 1) && packet.readBoolean() == (index != 2) && !packet.isReadable(), "Condition/redstone flags preserved");
        } finally { packet.release(); }
        require(tile.snapshot().getString("Command").equals("say original " + index), "No client-side command execution or prediction");
        net.minecraft.nbt.NBTTagCompound placeholder = new net.minecraft.nbt.NBTTagCompound();
        placeholder.setString("id", "Control"); tile.accept(placeholder);
        require(tile.snapshot().getString("Command").equals("say original " + index), "Via placeholder cannot erase the command");
        mc.displayGuiScreen(new CommandBlockScreen(tile)); screen = mc.currentScreen;
        field(screen, "command").setText("discard me");
        Method keyboard = BlockEditorScreen.class.getDeclaredMethod("keyTyped", char.class, int.class); keyboard.setAccessible(true);
        keyboard.invoke(screen, '!', org.lwjgl.input.Keyboard.KEY_1);
        net.minecraft.nbt.NBTTagCompound refreshed = tile.snapshot(); refreshed.setString("Command", "say server update"); tile.accept(refreshed);
        require(field(screen, "command").getText().contains("discard me"), "Server updates preserve unsaved typing");
        click(screen, 1);
        require(sent.isEmpty(), "Cancel sends no command update");
        require(EditorBlockEntity.open(tile.getWorld(), tile.getPos(), mc.thePlayer), "Command editor reopens");
        require(!button(mc.currentScreen, 0).enabled, "Reopen waits for fresh command permissions and data");
        refreshed.setString("Command", "say original " + index); tile.accept(refreshed);
        require(button(mc.currentScreen, 0).enabled && field(mc.currentScreen, "command").getText().equals("say original " + index), "Reopen receives the current server command");
        click(mc.currentScreen, 1);
    }

    private static void structure(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, EditorBlockEntity tile,
            List<C17PacketCustomPayload> sent) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        for (String mode : new String[]{"SAVE", "LOAD", "CORNER", "DATA"}) {
            net.minecraft.nbt.NBTTagCompound data = tile.snapshot(); data.setString("mode", mode); tile.accept(data);
            for (int action : mode.equals("SAVE") ? new int[]{1, 2, 4} : mode.equals("LOAD") ? new int[]{1, 3} : new int[]{1}) {
                mc.displayGuiScreen(new StructureBlockScreen(tile)); GuiScreen screen = mc.currentScreen;
                field(screen, "name").setText("test_structure"); field(screen, "metadata").setText("marker");
                field(screen, "posX").setText("-2"); field(screen, "posY").setText("1"); field(screen, "posZ").setText("3");
                field(screen, "sizeX").setText("4"); field(screen, "sizeY").setText("5"); field(screen, "sizeZ").setText("6");
                field(screen, "integrity").setText("0.75"); field(screen, "seed").setText("-22");
                if (mode.equals("LOAD")) { click(screen, 3); click(screen, 4); }
                if (mode.equals("LOAD") || mode.equals("SAVE")) click(screen, 5);
                if (mode.equals("SAVE")) click(screen, 6);
                if (mode.equals("LOAD")) click(screen, 7);
                click(screen, action == 1 ? 0 : action + 18);
                require(sent.size() == 1, "Structure action sends one message");
                ByteBuf packet = send(profile, client, server, sent.remove(0));
                try {
                    require(Types.STRING.read(packet).equals("MC|Struct"), "Native structure channel preserved");
                    position(packet, tile.getPos());
                    require(packet.readByte() == action && Types.STRING.read(packet).equals(mode), "Structure action/mode preserved");
                    require(Types.STRING.read(packet).equals("test_structure"), "Structure name preserved");
                    for (int expected : new int[]{-2, 1, 3, 4, 5, 6}) require(packet.readInt() == expected, "Structure offset/size preserved");
                    require(Types.STRING.read(packet).equals(mode.equals("LOAD") ? "LEFT_RIGHT" : "NONE"), "Mirror preserved");
                    require(Types.STRING.read(packet).equals(mode.equals("LOAD") ? "CLOCKWISE_90" : "NONE"), "Rotation preserved");
                    require(Types.STRING.read(packet).equals("marker"), "Structure data preserved");
                    require(packet.readBoolean() == (!mode.equals("LOAD") && !mode.equals("SAVE")), "Entity flag preserved");
                    require(packet.readBoolean() == mode.equals("SAVE") && packet.readBoolean() == !mode.equals("LOAD"), "Preview flags preserved");
                    require(packet.readFloat() == .75F && Types.VAR_LONG.readPrimitive(packet) == -22 && !packet.isReadable(), "Integrity/signed seed preserved");
                } finally { packet.release(); }
            }
        }
        net.minecraft.nbt.NBTTagCompound preview = tile.snapshot();
        preview.setString("mode", "LOAD"); preview.setInteger("sizeX", 4); preview.setInteger("sizeY", 5); preview.setInteger("sizeZ", 6);
        preview.setString("rotation", "CLOCKWISE_90"); preview.setString("mirror", "NONE");
        AxisAlignedBB box = StructureBlockRenderer.bounds(preview);
        require(box.minX == -5 && box.maxX == 1 && box.minZ == 0 && box.maxZ == 4 && box.minY == 1 && box.maxY == 6, "Rotated load preview footprint");
        mc.displayGuiScreen(new StructureBlockScreen(tile)); GuiScreen screen = mc.currentScreen;
        field(screen, "sizeX").setText("999"); click(screen, 0);
        require(sent.isEmpty() && mc.currentScreen == screen, "Invalid structure range is not sent");
        click(screen, 1); require(sent.isEmpty(), "Cancel sends no structure action");
    }

    private static boolean activate(WorldClient world, IBlockState state, BlockPos pos) {
        return state.getBlock().onBlockActivated(world, pos, state, Minecraft.getMinecraft().thePlayer, EnumFacing.UP, .5F, 1, .5F);
    }
    private static int apply(EmbeddedChannel client, NetHandlerPlayClient handler) throws Exception {
        int count = 0; ByteBuf input;
        while ((input = client.readInbound()) != null) {
            try {
                if (Types.VAR_INT.readPrimitive(input) != 0x35) continue;
                S35PacketUpdateTileEntity packet = new S35PacketUpdateTileEntity(); packet.readPacketData(new PacketBuffer(input));
                handler.handleUpdateTileEntity(packet); count++;
            } finally { input.release(); }
        }
        return count;
    }
    private static ByteBuf send(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, C17PacketCustomPayload packet) throws Exception {
        ByteBuf wire = BlockPipelineSmokeTest.packet(0x17);
        try { packet.writePacketData(new PacketBuffer(wire)); } finally { packet.getBufferData().release(); }
        client.writeOutbound(wire); client.runPendingTasks();
        ByteBuf compressed; while ((compressed = client.readOutbound()) != null) server.writeInbound(compressed);
        PacketType[] types = profile.protocol() >= 338 ? ServerboundPackets1_12_1.values() : profile.protocol() >= 335 ? ServerboundPackets1_12.values()
                : profile.protocol() >= 110 ? ServerboundPackets1_9_3.values() : ServerboundPackets1_9.values();
        for (PacketType type : types) if (type.getName().equals("CUSTOM_PAYLOAD")) return BlockPipelineSmokeTest.take(server, type.getId());
        throw new AssertionError("Missing plugin message type");
    }
    private static void position(ByteBuf data, BlockPos pos) {
        require(data.readInt() == pos.getX() && data.readInt() == pos.getY() && data.readInt() == pos.getZ(), "Editor coordinates preserved");
    }
    @SuppressWarnings("unchecked") private static GuiTextField field(GuiScreen screen, String name) throws Exception {
        Field fields = BlockEditorScreen.class.getDeclaredField("fields"); fields.setAccessible(true);
        return ((Map<String, GuiTextField>) fields.get(screen)).get(name);
    }
    @SuppressWarnings("unchecked") private static GuiButton button(GuiScreen screen, int id) throws Exception {
        Field buttons = GuiScreen.class.getDeclaredField("buttonList"); buttons.setAccessible(true);
        for (GuiButton button : (List<GuiButton>) buttons.get(screen)) if (button.id == id) return button;
        throw new AssertionError("Missing editor button " + id);
    }
    private static void click(GuiScreen screen, int id) throws Exception {
        Method action = BlockEditorScreen.class.getDeclaredMethod("actionPerformed", GuiButton.class); action.setAccessible(true);
        action.invoke(screen, button(screen, id));
    }
    private static BlockPos pos(int index) { return new BlockPos(2 + index * 3, 112, 2); }
    private static int raw(int index) { return new int[]{137 << 4 | 2, 210 << 4 | 10, 211 << 4 | 3, 255 << 4}[index]; }
    private static CompoundTag tag(int index) {
        CompoundTag tag = new CompoundTag(); BlockPos pos = pos(index);
        tag.putString("id", index == 3 ? "Structure" : "Control"); tag.putInt("x", pos.getX()); tag.putInt("y", pos.getY()); tag.putInt("z", pos.getZ());
        if (index == 3) {
            tag.putString("name", "existing_structure"); tag.putString("mode", "SAVE"); tag.putString("mirror", "NONE"); tag.putString("rotation", "NONE");
            tag.putInt("posY", 1); tag.putInt("sizeX", 4); tag.putInt("sizeY", 5); tag.putInt("sizeZ", 6);
            tag.putBoolean("ignoreEntities", true); tag.putBoolean("showboundingbox", true); tag.putFloat("integrity", 1);
        } else {
            tag.putString("Command", "say original " + index); tag.putBoolean("TrackOutput", true); tag.putBoolean("auto", index == 2);
            tag.putString("LastOutput", "{\"text\":\"previous result\"}");
        }
        return tag;
    }

    private static void capture(WorldClient world) throws Exception {
        Minecraft mc = Minecraft.getMinecraft(); int width = 1920, height = 640;
        Framebuffer target = new Framebuffer(width, height, true);
        target.setFramebufferColor(.08F, .09F, .12F, 1); target.framebufferClear(); target.bindFramebuffer(true);
        GlStateManager.matrixMode(5889); GlStateManager.pushMatrix(); GlStateManager.loadIdentity(); GlStateManager.ortho(0, width, height, 0, -1000, 1000);
        GlStateManager.matrixMode(5888); GlStateManager.pushMatrix(); GlStateManager.loadIdentity();
        try {
            GlStateManager.disableLighting();
            mc.fontRendererObj.drawString("Forge 1.8.9 | 1.12.2 command and structure editors | actual Minecraft GUI", 16, 12, 0xffffff);
            for (int i = 0; i < 7; i++) {
                EditorBlockEntity tile = (EditorBlockEntity) world.getTileEntity(pos(i < 3 ? i : 3));
                if (i >= 3) { net.minecraft.nbt.NBTTagCompound data = tile.snapshot(); data.setString("mode", new String[]{"SAVE", "LOAD", "CORNER", "DATA"}[i - 3]); tile.accept(data); }
                GuiScreen screen = i < 3 ? new CommandBlockScreen(tile) : new StructureBlockScreen(tile);
                mc.currentScreen = screen; screen.setWorldAndResolution(mc, 480, 300);
                GlStateManager.pushMatrix(); GlStateManager.translate(i % 4 * 480, 30 + i / 4 * 300, 0);
                screen.drawScreen(-1, -1, 0); GlStateManager.popMatrix(); screen.onGuiClosed();
            }
            Path directory = Paths.get(System.getenv("VIAFORGE_BLOCK_SMOKE_TEST")).toAbsolutePath().getParent();
            ScreenShotHelper.saveScreenshot(directory.toFile(), "block-editors-preview-1.12.2.png", width, height, target);
        } finally {
            mc.currentScreen = null;
            GlStateManager.matrixMode(5888); GlStateManager.popMatrix(); GlStateManager.matrixMode(5889); GlStateManager.popMatrix(); GlStateManager.matrixMode(5888);
            target.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true);
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
