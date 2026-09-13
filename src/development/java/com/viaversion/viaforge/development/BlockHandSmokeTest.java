package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.ScreenShotHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

/** Exercises the actual first-person renderer, including equip/swing and model transforms. */
final class BlockHandSmokeTest {
    static void verify(BlockVersionProfile profile, WorldClient world, Path directory) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient previousWorld = mc.theWorld;
        EntityPlayerSP previousPlayer = mc.thePlayer;
        EntityPlayerSP player = new EntityPlayerSP(mc, world,
                new NetHandlerPlayClient(mc, null, new NetworkManager(EnumPacketDirection.CLIENTBOUND),
                        new GameProfile(new UUID(0, 3), "BlockHandTest")), new StatFileWriter());
        ItemRenderer hand = new ItemRenderer(mc);
        RecordingRenderer recorder = new RecordingRenderer(mc);
        setField(ItemRenderer.class, hand, "itemRenderer", recorder);
        setField(ItemRenderer.class, hand, "equippedProgress", 1F);
        setField(ItemRenderer.class, hand, "prevEquippedProgress", 1F);
        mc.theWorld = world; mc.thePlayer = player;
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.pushMatrix();
        try {
            for (LegacyBlockCatalog.Definition definition : LegacyBlockCatalog.BLOCKS) {
                if (definition.itemId() < 0 || definition.itemProtocol() > profile.protocol()) continue;
                ItemStack stack = stack(definition.itemId(), definition.itemData());
                IBakedModel model = mc.getRenderItem().getItemModelMesher().getItemModel(stack);
                GlStateManager.loadIdentity();
                // Forge-generated flat icons expose their camera through perspective
                // handling rather than IBakedModel's legacy camera-transform fields.
                net.minecraftforge.client.ForgeHooksClient.handleCameraTransforms(model,
                        net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.FIRST_PERSON);
                FloatBuffer reference = BufferUtils.createFloatBuffer(16);
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, reference);
                float[] display = new float[16]; reference.get(display);
                for (float swing : new float[]{0, .35F}) {
                    player.swingProgress = player.prevSwingProgress = swing;
                    GlStateManager.loadIdentity();
                    render(hand, recorder, player, stack);
                    float[] actual = recorder.matrix;
                    require(actual != null, "Missing first-person draw " + definition.name);
                    // renderItem's final .5 geometry scale is applied after this capture.
                    for (int axis = 0; axis < 3; axis++) {
                        double length = 0;
                        for (int row = 0; row < 3; row++) length += actual[axis * 4 + row] * actual[axis * 4 + row];
                        double expected = 0;
                        for (int row = 0; row < 3; row++) expected += display[axis * 4 + row] * display[axis * 4 + row];
                        require(Math.abs(Math.sqrt(length) * .5 - Math.sqrt(expected)) < .0001,
                                "First-person scale " + definition.name + ", swing " + swing + ": " + Math.sqrt(length) * .5 + " != " + Math.sqrt(expected));
                    }
                    if (swing == 0) {
                        require(Math.abs(actual[12] - (.56F + display[12])) < .0001
                                && Math.abs(actual[13] - (-.52F + display[13])) < .0001
                                && Math.abs(actual[14] - (-.72F + display[14])) < .0001,
                                "First-person position " + definition.name);
                        for (int column = 0; column < 3; column++) for (int row = 0; row < 3; row++) {
                            require(Math.abs(actual[column * 4 + row] * .5F - display[column * 4 + row]) < .0001,
                                    "First-person orientation " + definition.name);
                        }
                    }
                }
            }
            player.swingProgress = player.prevSwingProgress = 0;
            GlStateManager.loadIdentity(); render(hand, recorder, player, new ItemStack(Blocks.stone));
            double stoneScale = Math.sqrt(recorder.matrix[0] * recorder.matrix[0] + recorder.matrix[1] * recorder.matrix[1]
                    + recorder.matrix[2] * recorder.matrix[2]) * .5;
            require(Math.abs(stoneScale - .4) < .0001, "Native 1.8 stone hand size unchanged");
            verifyBonemeal(mc, world, player);
            // Restore the idle pose after the interaction test for the review image.
            player.isSwingInProgress = false; player.swingProgress = player.prevSwingProgress = 0;
            if (profile == BlockVersionProfile.V1_12_2) capture(directory, hand, recorder, player);
        } finally {
            GlStateManager.popMatrix();
            mc.theWorld = previousWorld; mc.thePlayer = previousPlayer;
        }
    }

    private static void verifyBonemeal(Minecraft mc, WorldClient world, EntityPlayerSP player) throws Exception {
        net.minecraft.client.multiplayer.PlayerControllerMP previousController = mc.playerController;
        net.minecraft.util.MovingObjectPosition previousHit = mc.objectMouseOver;
        java.lang.reflect.Method click = Minecraft.class.getDeclaredMethod("rightClickMouse"); click.setAccessible(true);
        net.minecraft.util.BlockPos pos = new net.minecraft.util.BlockPos(4, 64, 4);
        try {
            mc.playerController = new net.minecraft.client.multiplayer.PlayerControllerMP(mc, player.sendQueue);
            mc.playerController.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
            mc.objectMouseOver = new net.minecraft.util.MovingObjectPosition(new net.minecraft.util.Vec3(4.5, 64.1, 4.5), net.minecraft.util.EnumFacing.UP, pos);
            world.setBlockState(pos.down(), Blocks.farmland.getDefaultState(), 0);
            for (int age = 0; age < 4; age++) {
                net.minecraft.block.state.IBlockState crop = net.minecraft.block.Block.BLOCK_STATE_IDS.getByValue(ClientBlocks.localState(207 << 4 | age));
                world.setBlockState(pos, crop, 0);
                ItemStack boneMeal = new ItemStack(net.minecraft.init.Items.dye, 8, 15);
                player.inventory.mainInventory[0] = boneMeal; player.inventory.currentItem = 0;
                player.isSwingInProgress = false; player.swingProgressInt = 0;
                click.invoke(mc);
                require(player.isSwingInProgress == (age < 3), "Bonemeal hand swing at beetroot age " + age);
                require(world.getBlockState(pos).equals(crop) && boneMeal.stackSize == 8, "Bonemeal growth/consumption remains server-owned");
            }
        } finally {
            world.setBlockToAir(pos); world.setBlockToAir(pos.down());
            mc.playerController = previousController; mc.objectMouseOver = previousHit;
        }
    }

    private static void capture(Path directory, ItemRenderer hand, RecordingRenderer recorder, EntityPlayerSP player) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        int width = 1440, height = 840, cellWidth = 360, cellHeight = 260;
        Framebuffer target = new Framebuffer(width, height, true);
        target.setFramebufferColor(.07F, .08F, .1F, 1); target.framebufferClear(); target.bindFramebuffer(true);
        GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.pushMatrix();
        recorder.draw = true;
        try {
            int[] ids = {1, 201, 203, 205, 198, 202, 206, 213, 229, 235, 251, 435};
            for (int index = 0; index < ids.length; index++) {
                int x = index % 4 * cellWidth, y = 45 + index / 4 * cellHeight;
                GlStateManager.viewport(x, height - y - cellHeight, cellWidth, cellHeight);
                GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.loadIdentity();
                GLU.gluPerspective(70, cellWidth / (float) cellHeight, .05F, 100F);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.loadIdentity();
                GlStateManager.enableDepth(); GlStateManager.enableAlpha(); GlStateManager.enableTexture2D();
                ItemStack item = ids[index] == 1 ? new ItemStack(Blocks.stone) : stack(ids[index], 0);
                render(hand, recorder, player, item);
                GlStateManager.viewport(0, 0, width, height);
                GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.loadIdentity();
                GlStateManager.ortho(0, width, height, 0, -1000, 1000);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.loadIdentity();
                GlStateManager.disableLighting(); GlStateManager.disableDepth(); GlStateManager.color(1, 1, 1, 1);
                Gui.drawRect(x + 4, y + cellHeight - 1, x + cellWidth - 4, y + cellHeight, 0xff49515e);
                mc.fontRendererObj.drawString(item.getDisplayName(), x + 12, y + 8, 0xffffff);
            }
            mc.fontRendererObj.drawString("Forge 1.8.9 | 1.12.2 resources | actual first-person renderer | identical 70-degree FOV", 15, 15, 0xffffff);
            Files.createDirectories(directory);
            ScreenShotHelper.saveScreenshot(directory.toFile(), "block-hand-preview-1.12.2.png", width, height, target);
            require(Files.isRegularFile(directory.resolve("screenshots/block-hand-preview-1.12.2.png")), "Hand preview saved");
        } finally {
            recorder.draw = false;
            GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.popMatrix(); GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            target.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true);
        }
    }

    private static void render(ItemRenderer hand, RecordingRenderer recorder, EntityPlayerSP player, ItemStack stack) throws Exception {
        setField(ItemRenderer.class, hand, "itemToRender", stack);
        player.inventory.mainInventory[0] = stack; player.inventory.currentItem = 0;
        recorder.matrix = null;
        hand.renderItemInFirstPerson(1F);
    }
    private static ItemStack stack(int id, int data) { return new ItemStack(Item.getItemById(ClientBlocks.localItem(id, data))); }
    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }

    private static final class RecordingRenderer extends RenderItem {
        private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        private float[] matrix;
        private boolean draw;
        RecordingRenderer(Minecraft mc) throws Exception {
            super(mc.getTextureManager(), mc.getRenderItem().getItemModelMesher().getModelManager());
            setField(RenderItem.class, this, "itemModelMesher", mc.getRenderItem().getItemModelMesher());
        }
        @Override public void renderItem(ItemStack stack, IBakedModel model) {
            buffer.clear(); GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
            matrix = new float[16]; buffer.get(matrix);
            if (draw) super.renderItem(stack, model);
        }
    }
}
