package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

/** Structure selection and air markers are local previews of server-owned settings. */
public final class StructureBlockRenderer extends TileEntitySpecialRenderer<EditorBlockEntity> {
    @Override public void renderTileEntityAt(EditorBlockEntity tile, double x, double y, double z, float partialTicks, int destroyStage) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!tile.structure() || !tile.received() || mc.thePlayer == null || !mc.thePlayer.capabilities.isCreativeMode
                || !ServerBlockSession.supportsItem(ClientBlocks.definition(tile.getBlockType()))) return;
        NBTTagCompound data = tile.snapshot();
        boolean save = data.getString("mode").equals("SAVE"), load = data.getString("mode").equals("LOAD");
        if (!save && !load || load && !data.getBoolean("showboundingbox")) return;
        if (size(data, "X") == 0 || size(data, "Y") == 0 || size(data, "Z") == 0) return;
        GlStateManager.pushMatrix(); GlStateManager.translate(x, y, z);
        GlStateManager.disableTexture2D(); GlStateManager.disableLighting(); GlStateManager.disableCull();
        GlStateManager.enableBlend(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        float lineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        GL11.glLineWidth(2);
        try {
            WorldRenderer vertices = Tessellator.getInstance().getWorldRenderer();
            vertices.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            outline(vertices, bounds(data), .65F, .7F, 1F);
            if (save && data.getBoolean("showair")) {
                int ox = offset(data, "X"), oy = offset(data, "Y"), oz = offset(data, "Z");
                for (int xx = 0; xx < size(data, "X"); xx++) for (int yy = 0; yy < size(data, "Y"); yy++) for (int zz = 0; zz < size(data, "Z"); zz++) {
                    BlockPos pos = tile.getPos().add(ox + xx, oy + yy, oz + zz);
                    net.minecraft.block.Block block = tile.getWorld().getBlockState(pos).getBlock();
                    LegacyBlockCatalog.Definition definition = ClientBlocks.definition(block);
                    boolean empty = block == Blocks.air, structureVoid = definition != null && definition.kind == LegacyBlockCatalog.Kind.VOID;
                    if (!empty && !structureVoid) continue;
                    double radius = empty ? .1 : .05, cx = ox + xx + .5, cy = oy + yy + .5, cz = oz + zz + .5;
                    outline(vertices, new AxisAlignedBB(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius),
                            structureVoid ? 1 : .5F, .5F, structureVoid ? .5F : 1);
                }
            }
            Tessellator.getInstance().draw();
        } finally {
            GL11.glLineWidth(lineWidth);
            GlStateManager.disableBlend(); GlStateManager.enableCull(); GlStateManager.enableLighting(); GlStateManager.enableTexture2D();
            GlStateManager.color(1, 1, 1, 1); GlStateManager.popMatrix();
        }
    }

    public static AxisAlignedBB bounds(NBTTagCompound data) {
        int minX = 0, minZ = 0, maxX = Math.max(0, size(data, "X") - 1), maxZ = Math.max(0, size(data, "Z") - 1);
        if (data.getString("mode").equals("LOAD")) {
            minX = minZ = Integer.MAX_VALUE; maxX = maxZ = Integer.MIN_VALUE;
            for (int x : new int[]{0, Math.max(0, size(data, "X") - 1)}) for (int z : new int[]{0, Math.max(0, size(data, "Z") - 1)}) {
                int xx = data.getString("mirror").equals("FRONT_BACK") ? -x : x;
                int zz = data.getString("mirror").equals("LEFT_RIGHT") ? -z : z;
                int rx = xx, rz = zz;
                switch (data.getString("rotation")) {
                    case "CLOCKWISE_90": rx = -zz; rz = xx; break;
                    case "CLOCKWISE_180": rx = -xx; rz = -zz; break;
                    case "COUNTERCLOCKWISE_90": rx = zz; rz = -xx; break;
                    default: break;
                }
                minX = Math.min(minX, rx); maxX = Math.max(maxX, rx); minZ = Math.min(minZ, rz); maxZ = Math.max(maxZ, rz);
            }
        }
        return new AxisAlignedBB(offset(data, "X") + minX, offset(data, "Y"), offset(data, "Z") + minZ,
                offset(data, "X") + maxX + 1, offset(data, "Y") + size(data, "Y"), offset(data, "Z") + maxZ + 1);
    }
    private static int size(NBTTagCompound data, String axis) { return Math.max(0, Math.min(32, data.getInteger("size" + axis))); }
    private static int offset(NBTTagCompound data, String axis) { return Math.max(-32, Math.min(32, data.getInteger("pos" + axis))); }
    private static void outline(WorldRenderer renderer, AxisAlignedBB box, float red, float green, float blue) {
        for (int corner = 0; corner < 8; corner++) for (int axis = 0; axis < 3; axis++) {
            if ((corner & 1 << axis) != 0) continue;
            vertex(renderer, box, corner, red, green, blue);
            vertex(renderer, box, corner | 1 << axis, red, green, blue);
        }
    }
    private static void vertex(WorldRenderer renderer, AxisAlignedBB box, int corner, float r, float g, float b) {
        renderer.pos((corner & 1) == 0 ? box.minX : box.maxX, (corner & 2) == 0 ? box.minY : box.maxY, (corner & 4) == 0 ? box.minZ : box.maxZ)
                .color(r, g, b, 1F).endVertex();
    }
}
