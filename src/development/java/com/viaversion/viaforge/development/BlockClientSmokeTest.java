package com.viaversion.viaforge.development;

import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Opt-in test in a real Forge client; no accounts or external game servers are used. */
public final class BlockClientSmokeTest {
    private final Path report;
    private final List<String> checks = new ArrayList<>();
    private final BlockVersionProfile[] profiles = BlockVersionProfile.values();
    private Object connection;
    private int profileIndex = -1;
    private long started;
    private boolean finished;

    private BlockClientSmokeTest(Path report) { this.report = report; }

    public static boolean installIfRequested() {
        String output = System.getenv("VIAFORGE_BLOCK_SMOKE_TEST");
        if (output == null || output.isEmpty()) return false;
        FMLCommonHandler.instance().bus().register(new BlockClientSmokeTest(Paths.get(output)));
        return true;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            if (profileIndex == -1) {
                Files.createDirectories(report.toAbsolutePath().getParent());
                Files.write(report, java.util.Collections.singletonList("RUNNING"), StandardCharsets.UTF_8);
                checkFallbackModels();
                next();
            } else if (profiles[profileIndex].resourceVersion().equals(ServerBlockSession.getLoadedResourceVersion())) {
                verify(profiles[profileIndex]);
                if (profileIndex + 1 < profiles.length) next();
                else {
                    ServerBlockSession.unload();
                    require(ServerBlockSession.getLoadedResourceVersion() == null, "Resources cleared on unload");
                    require(!Minecraft.getMinecraft().getResourceManager().getResource(new net.minecraft.util.ResourceLocation("minecraft:textures/blocks/stone.png"))
                            .getResourcePackName().equals("ViaForge versioned blocks"), "Vanilla block textures restored");
                    checkFallbackModels();
                    finish(null);
                }
            } else if (System.currentTimeMillis() - started > 180000) {
                throw new AssertionError("Timed out loading " + profiles[profileIndex].resourceVersion());
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void next() {
        Object oldConnection = connection;
        connection = new Object();
        profileIndex++;
        started = System.currentTimeMillis();
        ServerBlockSession.join(connection, profiles[profileIndex]);
        if (oldConnection != null) ServerBlockSession.leave(oldConnection); // delayed close must not reset the next session
    }

    private void verify(BlockVersionProfile profile) throws Exception {
        int states = 0;
        for (int raw = 0; raw < com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.STATE_LIMIT; raw++) {
            if (!profile.supportsState(raw)) continue;
            IBlockState state = local(raw);
            com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition definition = ClientBlocks.definition(state.getBlock());
            require(model(state).getParticleTexture().getIconName().startsWith("viaforge:"), "Target texture for " + raw + " (" + definition.name + ")");
            float hardness = definition.hardness;
            float resistance = definition.resistance;
            require(Math.abs(state.getBlock().getBlockHardness(null, new BlockPos(0, 0, 0)) - hardness) < .00001F,
                    "Block hardness for " + raw);
            require(Math.abs(state.getBlock().getExplosionResistance(null) - resistance) < .00001F,
                    "Explosion resistance for " + raw);
            states++;
        }
        for (com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition definition : com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.BLOCKS) {
            if (definition.protocol > profile.protocol()) continue;
            int meta = definition.color < 0 || definition.kind == com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Kind.BED ? 0 : definition.color;
            Block block = local(definition.stateId(meta)).getBlock();
            for (IBlockState state : block.getBlockState().getValidStates()) model(state);
            if (definition.itemId() < 0 || definition.itemProtocol() > profile.protocol()) continue;
            int localId = ClientBlocks.localItem(definition.itemId(), definition.itemData());
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(net.minecraft.item.Item.getItemById(localId));
            IBakedModel itemModel = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(stack);
            require(itemModel != Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getMissingModel(), "Inventory model for " + definition.name);
            require(itemModel.getParticleTexture().getIconName().startsWith("viaforge:") || definition.kind == com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Kind.VOID,
                    "Inventory target texture for " + definition.name + ": " + itemModel.getParticleTexture().getIconName());
            checkFaceTextures(itemModel, "inventory " + definition.name);
        }
        IBlockState stair = local(203 << 4);
        for (IBlockState state : stair.getBlock().getBlockState().getValidStates()) model(state);

        WorldClient world = new WorldClient(null, new WorldSettings(0, WorldSettings.GameType.CREATIVE, false, false, WorldType.DEFAULT),
                0, EnumDifficulty.PEACEFUL, new Profiler());
        world.doPreChunk(0, 0, true);
        BlockPipelineSmokeTest.verify(profile, world);
        // Use an empty neighboring chunk for shape checks after the network fixture.
        world.doPreChunk(0, 0, false);
        world.doPreChunk(0, 0, true);
        LegacyShapeSmokeTest.verify(world);
        BlockHandSmokeTest.verify(profile, world, report.toAbsolutePath().getParent());
        ServerItemSmokeTest.models(profile, world, report.toAbsolutePath().getParent());
        CreativeCategorySmokeTest.verify(profile);
        if (profile.protocol() >= 335) BedModelSmokeTest.verify();
        BlockPos pos = new BlockPos(4, 64, 4);
        for (int meta = 0; meta < 8; meta++) {
            IBlockState state = local(203 << 4 | meta);
            world.setBlockState(pos, state, 0);
            List<AxisAlignedBB> boxes = new ArrayList<>();
            state.getBlock().addCollisionBoxesToList(world, pos, state, new AxisAlignedBB(3, 63, 3, 6, 66, 6), boxes, null);
            double volume = 0;
            for (AxisAlignedBB box : boxes) volume += (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
            require(Math.abs(volume - .75) < .00001, "Stair collision volume, metadata " + meta + ": " + volume);
            require(state.getBlock().collisionRayTrace(world, pos, new Vec3(4.5, 67, 4.5), new Vec3(4.5, 63, 4.5)) != null,
                    "Stair selection, metadata " + meta);
            net.minecraft.util.EnumFacing facing = state.getValue(BlockStairs.FACING);
            IBlockState turned = state.withProperty(BlockStairs.FACING, facing.rotateY());
            for (boolean outer : new boolean[]{true, false}) {
                BlockPos neighbor = pos.offset(outer ? facing : facing.getOpposite());
                world.setBlockState(neighbor, turned, 0);
                IBlockState actual = state.getBlock().getActualState(state, world, pos);
                require(actual.getValue(BlockStairs.SHAPE).getName().startsWith(outer ? "outer" : "inner"), "Connected stair shape");
                model(actual);
                boxes.clear();
                state.getBlock().addCollisionBoxesToList(world, pos, state, new AxisAlignedBB(3, 63, 3, 6, 66, 6), boxes, null);
                double cornerVolume = 0;
                for (AxisAlignedBB box : boxes) cornerVolume += (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
                require(Math.abs(cornerVolume - (outer ? .625 : .875)) < .00001, "Connected stair collision, metadata " + meta);
                world.setBlockToAir(neighbor);
            }
        }
        if (profile == BlockVersionProfile.V1_12_2) BlockRenderPreview.capture(report.toAbsolutePath().getParent(), world);
        world.doPreChunk(0, 0, false);
        require(Minecraft.getMinecraft().getResourceManager().getResource(new net.minecraft.util.ResourceLocation("minecraft:textures/blocks/stone.png"))
                .getResourcePackName().equals("ViaForge versioned blocks"), "Existing block texture overlay");
        checks.add(profile.resourceVersion() + ": compressed Via pipeline/repeated decoder reordering/native chunk+updates, " + states + " server states, all block/item models, first-person scale/position/swing, right-click/air-use packets, shulker container cycle where available, inventory/creative/click/NBT round trips, rod/slab/path/crop/64 chorus shapes, 24 stair collision shapes, vanilla texture overlay OK"
                + "; all available standalone item/potion/egg/book variants, durability/NBT/creative/click round trips, target item models, hand transforms, eating/drinking/shield/bow/equip actions OK"
                + "; original projectile/cloud/offhand packets, cloud RGB/radius/destroy/respawn, shield matrices for both hands/poses/skin widths, target creative categories/order/search OK"
                + "; 37 potion impact types/custom RGB without duplicate fallback effects, tipped/spectral arrow spawn/velocity/metadata/flight/ground/expiration/destroy, version-gated Totem particles/40-tick animation/original sound OK"
                + "; command editor activation/NBT/controls/cancel and target packets OK"
                + (profile.protocol() >= 210 ? "; structure SAVE/LOAD/CORNER/DATA, save/load/detect/cancel, numeric limits and target packets OK" : "")
                + (profile.protocol() >= 335 ? "; 16 bed colors: every world/item vertex, UV corner and face winding matches native ModelBed in all directions" : ""));
    }

    private static void checkFallbackModels() {
        CreativeCategorySmokeTest.disconnected();
        ServerItemSmokeTest.disconnected();
        for (int raw = 0; raw < com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.STATE_LIMIT; raw++) {
            if (BlockVersionProfile.V1_12_2.supportsState(raw)) model(local(raw));
        }
        for (com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition definition : com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.BLOCKS) {
            if (definition.itemId() < 0) continue;
            net.minecraft.item.Item item = net.minecraft.item.Item.getItemById(ClientBlocks.localItem(definition.itemId(), definition.itemData()));
            java.util.List<net.minecraft.item.ItemStack> entries = new ArrayList<>();
            item.getSubItems(item, item.getCreativeTab(), entries);
            require(entries.isEmpty(), "No server block inventory entries outside a server session");
        }
    }

    private static IBlockState local(int raw) {
        int id = ClientBlocks.localState(raw);
        require(id >= 0, "Registered state " + raw);
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(id);
        require(state != null, "Resolved state " + raw);
        return state;
    }

    private static IBakedModel model(IBlockState state) {
        Minecraft mc = Minecraft.getMinecraft();
        IBakedModel model = mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
        require(model != mc.getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getMissingModel(), "Baked model: " + state);
        require(!model.getParticleTexture().getIconName().equals("missingno"), "Model texture: " + state);
        checkFaceTextures(model, state.toString());
        return model;
    }

    private static void checkFaceTextures(IBakedModel model, String name) {
        net.minecraft.client.renderer.texture.TextureAtlasSprite missing = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
        java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads = new ArrayList<>(model.getGeneralQuads());
        for (net.minecraft.util.EnumFacing face : net.minecraft.util.EnumFacing.values()) quads.addAll(model.getFaceQuads(face));
        for (net.minecraft.client.renderer.block.model.BakedQuad quad : quads) {
            int[] data = quad.getVertexData();
            int stride = data.length / 4;
            float u = 0, v = 0;
            for (int vertex = 0; vertex < 4; vertex++) {
                u += Float.intBitsToFloat(data[vertex * stride + 4]) / 4;
                v += Float.intBitsToFloat(data[vertex * stride + 5]) / 4;
            }
            require(u < missing.getMinU() || u > missing.getMaxU() || v < missing.getMinV() || v > missing.getMaxV(), "Missing face texture: " + name);
        }
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private void finish(Throwable failure) {
        finished = true;
        try {
            Files.createDirectories(report.toAbsolutePath().getParent());
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(report, StandardCharsets.UTF_8))) {
                writer.println(failure == null ? "PASS" : "FAIL");
                for (String check : checks) writer.println(check);
                if (failure != null) failure.printStackTrace(writer);
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }
}
