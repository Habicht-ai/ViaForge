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
    private final BlockVersionProfile[] profiles = java.util.Arrays.stream(BlockVersionProfile.values()).filter(p -> selected(p.protocol())).toArray(BlockVersionProfile[]::new);
    private final int[] flattened = java.util.Arrays.stream(new int[]{393,401,404,477,480,485,490,498,573,575,578,735,736,751,753,754,755,756,757,758,759,760,761,762,763,764,765,766,767,768,769,770,771,772,773,774,775,776}).filter(BlockClientSmokeTest::selected).toArray();
    private Object connection;
    private int profileIndex = -1;
    private long started;
    private boolean finished;

    private BlockClientSmokeTest(Path report) { this.report = report; if(profiles.length+flattened.length==0)throw new IllegalArgumentException("No smoke profile matches VIAFORGE_SMOKE_PROTOCOL"); }

    private static boolean selected(int protocol) {
        String filter=System.getenv("VIAFORGE_SMOKE_PROTOCOL");
        return filter==null||filter.isEmpty()||Integer.parseInt(filter)==protocol;
    }

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
                checkViaPaths();
                next();
            } else if (target().resources().version().equals(ServerBlockSession.getLoadedResourceVersion())) {
                if(profileIndex<profiles.length)verify(profiles[profileIndex]);
                else {
                    WorldClient world=new WorldClient(null,new WorldSettings(0,WorldSettings.GameType.CREATIVE,false,false,WorldType.DEFAULT),0,EnumDifficulty.PEACEFUL,new Profiler());
                    world.doPreChunk(0,0,true);
                    checks.add(FlattenedPipelineSmokeTest.verify(target(),world,report.toAbsolutePath().getParent()));
                }
                ShulkerItemRenderSmokeTest.verify(target(), report.toAbsolutePath().getParent());
                if (target().serverProtocol() >= 315) checks.add(target().resources().version()
                        + ": all 16 shulker item colors"+(target().serverProtocol()>=335?" and all 16 bed colors (baked geometry fits a 16px GUI slot)":"")+"; original target matrices for third person (both hands, slim/normal arms, sneaking), first person, GUI, fixed and dropped items PASS");
                if (profileIndex + 1 < profiles.length + flattened.length) next();
                else {
                    ServerBlockSession.unload();
                    RenderResourceSmokeTest.restored();
                    DropItemSmokeTest.nativeBehavior();
                    require(ServerBlockSession.getLoadedResourceVersion() == null, "Resources cleared on unload");
                    require(!Minecraft.getMinecraft().getResourceManager().getResource(new net.minecraft.util.ResourceLocation("minecraft:textures/blocks/stone.png"))
                            .getResourcePackName().equals("ViaForge versioned blocks"), "Vanilla block textures restored");
                    checkFallbackModels();
                    checks.add("All profiles: original dropped Purpur/seed/shield/sword matrices and native stone scale; pending resource HUD avoids missing-texture caching; native resources restored after disconnect");
                    checks.add("Drop regression: actual Q/Ctrl-Q in Survival and Creative, shield and stackable blocks, single/full/empty drops, selected/offhand/adjacent slot isolation, original target action packets and authoritative correction/empty packets; native drop behavior restored on disconnect PASS");
                    checks.add("Render regressions: original 1.13+ aquatic biomes and 1.16+ registry water colors, tall-source biome coordinates, 1.19.4+ biome update packets before Via cancellation, disconnect cleanup; first position packet waits for resources; 26.2 independently decoded RGBA hashes, actual atlas transparency and shield GPU texels, original bed faces/UVs in all 16 colors, 16 distinct rendered banner dyes; native water and texture caches restored PASS");
                    finish(null);
                }
            } else if (com.viaversion.viaforge.compatibility.ServerSession.resourceFailure()!=null) {
                throw new AssertionError("Target resource conversion failed: "+target().resources().version(),com.viaversion.viaforge.compatibility.ServerSession.resourceFailure());
            } else if (System.currentTimeMillis() - started > 180000) {
                throw new AssertionError("Timed out loading " + target().resources().version());
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
        com.viaversion.viaforge.compatibility.ServerSession.join(connection, target());
        PendingResourceSmokeTest.verify();
        if (oldConnection != null) ServerBlockSession.leave(oldConnection); // delayed close must not reset the next session
    }

    private com.viaversion.viaforge.common.compatibility.CompatibilityProfile target() {
        return com.viaversion.viaforge.common.compatibility.CompatibilityRegistry.DEFAULT.resolve(profileIndex<profiles.length?profiles[profileIndex].protocol():flattened[profileIndex-profiles.length]);
    }
    private void checkViaPaths() {
        java.util.List<String> supported=new java.util.ArrayList<>();
        for(com.viaversion.viaversion.api.protocol.version.ProtocolVersion version:com.viaversion.viaversion.api.protocol.version.ProtocolVersion.getProtocols()) {
            if(version.getVersion()<393||version.getVersion()>776||version.isSnapshot())continue;
            java.util.List<com.viaversion.viaversion.api.protocol.ProtocolPathEntry> path=com.viaversion.viaversion.api.Via.getManager().getProtocolManager().getProtocolPath(com.viaversion.viaversion.api.protocol.version.ProtocolVersion.v1_8,version);
            require(path!=null,"Bundled Via path to "+version.getName());supported.add(version.getName()+" ("+version.getVersion()+", "+path.size()+" layers)");
        }
        require(com.viaversion.viaversion.api.protocol.version.ProtocolVersion.v26_2.getVersion()==776,"Bundled 26.2 protocol identifier");
        checks.add("Bundled ViaVersion/ViaBackwards 5.11.0 + ViaRewind 4.1.3 paths (connectivity only, not client feature certification): "+supported);
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
        ShulkerBoxRenderSmokeTest.verify(profile, world, report.toAbsolutePath().getParent());
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
        checks.add(profile.resourceVersion() + ": compressed Via pipeline/repeated decoder reordering/native chunk+updates, " + states + " server states" + (profile.protocol() >= 335 ? ", state retained after cancelled recipe traffic" : "") + ", Purpur/shulker placement confirmations in all colors/orientations and multi-block batches, all block/item models, first-person scale/position/swing, right-click/air-use packets, shulker container cycle where available, inventory/creative/click/NBT round trips, rod/slab/path/crop/64 chorus shapes, 24 stair collision shapes, vanilla texture overlay OK"
                + "; all available standalone item/potion/egg/book variants, durability/NBT/creative/click round trips, target item models, hand transforms, eating/drinking/shield/bow/equip actions OK"
                + "; original projectile/cloud/offhand packets, cloud RGB/radius/destroy/respawn, shield matrices for both hands/poses/skin widths, target creative categories/order/search OK"
                + "; 37 potion impact types/custom RGB without duplicate fallback effects, tipped/spectral arrow spawn/velocity/metadata/flight/ground/expiration/destroy, version-gated Totem particles/40-tick animation/original sound OK"
                + "; Sweeping Edge I-III version/category/tooltip/NBT, sweep packet/lifetime, server attack-speed modifiers/timer/cubic hand recharge, 25 native tool hand models, cape texture selection/suppression/glint pose OK"
                + "; crystal base/beam metadata and destroy, gateway NBT/chunk age/server events/versioned cooldown/exposed portal particles, native crystal/gateway rendering OK"
                + "; native mob spawns/variants/partial metadata/movement/health/equipment/hurt, target textures/models and sounds, shulker attachment/bounds, baby sizes, dragon phases/8 part IDs, mob projectiles, versioned llama inventory slots and shoulder parrots OK"
                + "; original projectile names/partial updates/clear, new status effects with level/timer/flags/removal and target inventory icons OK"
                + "; original item cooldown packets/slot sharing/replacement/removal/respawn, pearl prediction in both modes, use gates and native overlay pixels OK; worn dragon head limb swing and placed redstone clock/freeze/resume in all orientations OK"
                + "; modern boat models/six woods, original coordinates/metadata, both seats/ticking/transfers, driver-only movement and rowing packets, versioned paddles, water/ice physics, placement in both modes OK"
                + "; two hands: original slot 45/full/direct updates, NBT, click/creative/swap/use/interact/swing/settings packets, hand priority/continuous shield and food use, both bow hands/ammunition, actual inventory/creative/Forge hotbar pixels and first-person models OK"
                + (profile.protocol() >= 315 ? "; shulker box interior rendering independent of incoming face culling and support surface draw order, six facings/three lid stages, render state restored OK" : "")
                + "; command editor activation/NBT/controls/cancel and target packets OK"
                + (profile.protocol() >= 210 ? "; structure SAVE/LOAD/CORNER/DATA, save/load/detect/cancel, numeric limits and target packets OK" : "")
                + (profile.protocol() >= 335 ? "; 16 bed colors: every world/item vertex, UV corner and face winding matches native ModelBed in all directions" : ""));
    }

    private static void checkFallbackModels() {
        ShulkerItemRenderSmokeTest.disconnected();
        require(!com.viaversion.viaforge.hands.Offhand.active()&&com.viaversion.viaforge.hands.Offhand.get()==null&&!com.viaversion.viaforge.hands.HandRenderer.needed(),"Native/disconnected sessions do not enable two hands");
        BoatSmokeTest.disconnected();
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

    static void checkFaceTextures(IBakedModel model, String name) {
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
