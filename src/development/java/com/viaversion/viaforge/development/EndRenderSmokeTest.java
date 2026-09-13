package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.blocks.*;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.items.ServerEntityViews;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.*;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.IWorldAccess;
import org.lwjgl.util.glu.GLU;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Screenshots from the actual crystal mixin and gateway TESR, including projected portal layers. */
final class EndRenderSmokeTest {
    static void verify(BlockVersionProfile profile, WorldClient world, Path directory) throws Exception {
        if (profile != BlockVersionProfile.V1_9 && profile != BlockVersionProfile.V1_12_2) return;
        Minecraft mc = Minecraft.getMinecraft(); TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TextureManager engine = dispatcher.renderEngine; dispatcher.renderEngine = mc.getTextureManager();
        Framebuffer frame = new Framebuffer(1200, 720, true); frame.setFramebufferColor(.07F, .08F, .11F, 1);
        GlStateManager.matrixMode(5889); GlStateManager.pushMatrix(); GlStateManager.matrixMode(5888); GlStateManager.pushMatrix();
        EntityEnderCrystal crystal = new EntityEnderCrystal(world); world.addEntityToWorld(940, crystal); crystal.innerRotation = 120;
        ByteBuf view = Unpooled.buffer();
        try {
            view.writeShort(profile.protocol()).writeByte(1); Types.VAR_INT.writePrimitive(view, 940); view.writeLong(0).writeLong(940).writeByte(51).writeDouble(0).writeDouble(1).writeDouble(0);
            ServerEntityViews.accept(view);
        } finally { view.release(); }
        BlockPos pos = new BlockPos(4, 78, 4);
        try {
            frame.framebufferClear(); frame.bindFramebuffer(true);
            for (int i = 0; i < 2; i++) {
                scene(i, false); ServerEntityViews.get(940).crystalBase = i == 1;
                block(Blocks.obsidian, -.5, 0, -.5);
                mc.getRenderManager().getEntityRenderObject(crystal).doRender(crystal, 0, 1, 0, 0, .5F);
            }
            labels("Server crystal base = false", "Server crystal base = true", "Original crystal model | " + profile.resourceVersion());
            ScreenShotHelper.saveScreenshot(directory.toFile(), "end-crystals-" + profile.resourceVersion() + ".png", 1200, 720, frame);
            frame.framebufferClear(); frame.bindFramebuffer(true); crystal.setPosition(0, 1, 0);
            for (int i = 0; i < 2; i++) {
                scene(i, false); ServerEntityViews.get(940).crystalBase = false;
                ServerEntityViews.get(940).crystalBeam = i == 0 ? new BlockPos(3, 3, 0) : null;
                block(Blocks.obsidian, -.5, 0, -.5);
                mc.getRenderManager().getEntityRenderObject(crystal).doRender(crystal, 0, 1, 0, 0, .5F);
                require(org.lwjgl.opengl.GL11.glGetError() == 0, "Crystal beam has valid render state");
            }
            labels("Server beam target", "Beam target cleared", "Original crystal beam | " + profile.resourceVersion());
            ScreenShotHelper.saveScreenshot(directory.toFile(), "end-crystal-beam-" + profile.resourceVersion() + ".png", 1200, 720, frame);
            world.setBlockState(pos, Block.BLOCK_STATE_IDS.getByValue(ClientBlocks.localState(209 << 4)), 0);
            GatewayBlockEntity gateway = (GatewayBlockEntity)world.getTileEntity(pos); NBTTagCompound tag = new NBTTagCompound(); tag.setLong("Age", 6000); gateway.accept(tag);
            world.setBlockState(pos.up(), Blocks.bedrock.getDefaultState(), 0); world.setBlockState(pos.down(), Blocks.bedrock.getDefaultState(), 0);
            GatewayBlockRenderer renderer = (GatewayBlockRenderer)dispatcher.<GatewayBlockEntity>getSpecialRenderer(gateway);
            require(renderer != null && gateway.getBlockType().getRenderType() == -1, "Gateway uses animated TESR without a second baked cube");
            frame.framebufferClear(); frame.bindFramebuffer(true);
            for (int i = 0; i < 2; i++) {
                scene(i, true);
                for (int y : new int[]{-1, 1}) {
                    block(Blocks.bedrock, 0, y, 0);
                    for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) block(Blocks.bedrock, side.getFrontOffsetX(), y, side.getFrontOffsetZ());
                }
                if (i == 1) { gateway.receiveClientEvent(1, 0); for (int tick = 0; tick < gateway.cooldownLength() / 2; tick++) gateway.update(); }
                renderer.renderTileEntityAt(gateway, 0, 0, 0, .5F, -1);
                particles(world, gateway);
                require(org.lwjgl.opengl.GL11.glGetError() == 0, "Gateway render leaves a valid OpenGL state");
            }
            labels("Idle gateway", "Server activation", "Animated gateway / original particles | " + profile.resourceVersion());
            ScreenShotHelper.saveScreenshot(directory.toFile(), "end-gateway-" + profile.resourceVersion() + ".png", 1200, 720, frame);
        } finally {
            world.removeEntityFromWorld(940); ServerEntityViews.clear(); world.setBlockToAir(pos); world.setBlockToAir(pos.up()); world.setBlockToAir(pos.down());
            dispatcher.renderEngine = engine; GlStateManager.matrixMode(5888); GlStateManager.popMatrix(); GlStateManager.matrixMode(5889); GlStateManager.popMatrix(); GlStateManager.matrixMode(5888);
            frame.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true); GlStateManager.color(1,1,1,1);
        }
    }
    private static void scene(int panel, boolean gateway) {
        GlStateManager.viewport(panel * 600, 0, 600, 680);
        GlStateManager.matrixMode(5889); GlStateManager.loadIdentity(); GLU.gluPerspective(48, 600F/680, .05F, 300);
        GlStateManager.matrixMode(5888); GlStateManager.loadIdentity();
        GLU.gluLookAt(4.5F, gateway ? 2.7F : 3.5F, 6, .5F, gateway ? .5F : 1.3F, .5F, 0,1,0);
        GlStateManager.enableDepth(); GlStateManager.depthMask(true); GlStateManager.enableAlpha(); GlStateManager.enableTexture2D(); GlStateManager.enableCull(); GlStateManager.disableFog();
        GlStateManager.color(1,1,1,1); RenderHelper.enableStandardItemLighting(); OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
    }
    private static void block(Block block, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft(); mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GlStateManager.pushMatrix(); GlStateManager.translate(x + .5, y + .5, z + .5); GlStateManager.scale(2,2,2);
        mc.getRenderItem().renderItem(new ItemStack(Blocks.stone), mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(block.getDefaultState())); GlStateManager.popMatrix();
    }
    private static void particles(WorldClient world, GatewayBlockEntity gateway) {
        Minecraft mc = Minecraft.getMinecraft(); EffectRenderer effects = new EffectRenderer(world, mc.getTextureManager());
        IWorldAccess access = (IWorldAccess)Proxy.newProxyInstance(IWorldAccess.class.getClassLoader(), new Class<?>[]{IWorldAccess.class}, (proxy, method, args) -> {
            if (method.getName().equals("equals")) return proxy == args[0];
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("spawnParticle")) effects.spawnEffectParticle((Integer)args[0], (Double)args[2], (Double)args[3], (Double)args[4], (Double)args[5], (Double)args[6], (Double)args[7]);
            return null;
        });
        world.addWorldAccess(access);
        try { Random random = new Random(31); for (int i = 0; i < 8; i++) { gateway.particles(random); effects.updateEffects(); } }
        finally { world.removeWorldAccess(access); }
        EntityOtherPlayerMP camera = new EntityOtherPlayerMP(world, new GameProfile(new UUID(0, 941), "PortalCamera"));
        BlockPos pos = gateway.getPos(); camera.setPosition(pos.getX(), pos.getY(), pos.getZ());
        camera.prevPosX = camera.lastTickPosX = camera.posX; camera.prevPosY = camera.lastTickPosY = camera.posY; camera.prevPosZ = camera.lastTickPosZ = camera.posZ;
        GlStateManager.disableLighting(); GlStateManager.disableFog(); ActiveRenderInfo.updateRenderInfo(camera, false); effects.renderParticles(camera, .5F);
    }
    private static void labels(String left, String right, String title) {
        GlStateManager.viewport(0,0,1200,720); GlStateManager.matrixMode(5889); GlStateManager.loadIdentity(); GlStateManager.ortho(0,1200,720,0,-1000,1000);
        GlStateManager.matrixMode(5888); GlStateManager.loadIdentity(); GlStateManager.disableDepth(); GlStateManager.disableLighting(); GlStateManager.disableFog(); GlStateManager.color(1,1,1,1);
        Minecraft mc = Minecraft.getMinecraft(); mc.fontRendererObj.drawString(title,20,15,0xffffff); mc.fontRendererObj.drawString(left,125,685,0xffffff); mc.fontRendererObj.drawString(right,755,685,0xffffff);
    }
}
