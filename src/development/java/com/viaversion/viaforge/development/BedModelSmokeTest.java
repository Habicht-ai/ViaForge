package com.viaversion.viaforge.development;

import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/** Compares the baked output with the entity ModelBox geometry/UVs used by vanilla 1.12.2. */
final class BedModelSmokeTest {
    static void verify() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        for (int color = 0; color < 16; color++) {
            for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
                for (boolean head : new boolean[]{false, true}) {
                    List<float[][]> expected = nativeHalf(head, facing, 0);
                    for (boolean occupied : new boolean[]{false, true}) {
                        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(ClientBlocks.localState(
                                LegacyBlockCatalog.bedState(facing.getHorizontalIndex() | (head ? 8 : 0), color)))
                                .withProperty(BlockBed.OCCUPIED, occupied);
                        check(mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state), expected,
                                LegacyBlockCatalog.COLORS[color] + " " + facing + " head=" + head + " occupied=" + occupied);
                    }
                }
            }
            List<float[][]> expected = nativeHalf(true, EnumFacing.SOUTH, 0);
            expected.addAll(nativeHalf(false, EnumFacing.SOUTH, -16));
            ItemStack stack = new ItemStack(Item.getItemById(ClientBlocks.localItem(355, color)));
            check(mc.getRenderItem().getItemModelMesher().getItemModel(stack), expected,
                    LegacyBlockCatalog.COLORS[color] + " inventory/held geometry");
        }
    }

    private static void check(IBakedModel model, List<float[][]> expected, String label) {
        List<BakedQuad> quads = new ArrayList<>(model.getGeneralQuads());
        for (EnumFacing face : EnumFacing.values()) quads.addAll(model.getFaceQuads(face));
        if (quads.size() != expected.size()) throw new AssertionError("Bed face count: " + label);
        TextureAtlasSprite sprite = model.getParticleTexture();
        List<float[][]> unmatched = new ArrayList<>(expected);
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertexData();
            float[][] actual = new float[4][5];
            for (int i = 0; i < 4; i++) {
                for (int axis = 0; axis < 3; axis++) actual[i][axis] = Float.intBitsToFloat(data[i * 7 + axis]);
                actual[i][3] = (Float.intBitsToFloat(data[i * 7 + 4]) - sprite.getMinU()) / (sprite.getMaxU() - sprite.getMinU());
                actual[i][4] = (Float.intBitsToFloat(data[i * 7 + 5]) - sprite.getMinV()) / (sprite.getMaxV() - sprite.getMinV());
            }
            int match = -1;
            for (int i = 0; i < unmatched.size(); i++) if (matches(actual, unmatched.get(i))) { match = i; break; }
            if (match < 0) throw new AssertionError("Bed geometry/UV differs from native ModelBed: " + label
                    + " face=" + quad.getFace() + " vertices=" + java.util.Arrays.deepToString(actual));
            unmatched.remove(match);
        }
    }

    private static boolean matches(float[][] actual, float[][] expected) {
        // Allow cyclic vertex reordering, but never reversed winding or reassigned UV corners.
        for (int start = 0; start < 4; start++) {
            boolean same = true;
            for (int vertex = 0; vertex < 4; vertex++) for (int field = 0; field < 5; field++) {
                if (Math.abs(actual[vertex][field] - expected[(vertex + start) % 4][field]) > .0001F) same = false;
            }
            if (same) return true;
        }
        return false;
    }

    private static List<float[][]> nativeHalf(boolean head, EnumFacing facing, int z) throws Exception {
        // 1.8 and 1.12 use the same ModelBox/TexturedQuad layout. Keep this fixture in
        // entity coordinates, independently of the production block-JSON conversion.
        ModelBase model = new ModelBase() { };
        model.textureWidth = model.textureHeight = 64;
        List<ModelRenderer> parts = new ArrayList<>();
        ModelRenderer mattress = new ModelRenderer(model, 0, head ? 0 : 22);
        mattress.addBox(0, 0, 0, 16, 16, 6); parts.add(mattress);
        for (int leg = head ? 1 : 0; leg < 4; leg += 2) {
            ModelRenderer foot = new ModelRenderer(model, 50, leg * 6);
            foot.addBox(leg < 2 ? 0 : -16, 6, (leg & 1) == 0 ? -16 : 0, 3, 3, 3);
            foot.rotateAngleX = (float) Math.PI / 2;
            foot.rotateAngleZ = new float[]{0, (float) Math.PI / 2, (float) Math.PI * 1.5F, (float) Math.PI}[leg];
            parts.add(foot);
        }
        int angle = facing == EnumFacing.NORTH ? 0 : facing == EnumFacing.SOUTH ? 180 : facing == EnumFacing.WEST ? -90 : 90;
        Matrix4f placement = new Matrix4f();
        placement.translate(new Vector3f(facing == EnumFacing.SOUTH || facing == EnumFacing.EAST ? 16 : 0, 9,
                z + (facing == EnumFacing.SOUTH || facing == EnumFacing.WEST ? 16 : 0)));
        placement.rotate((float) Math.PI / 2, new Vector3f(1, 0, 0));
        placement.rotate((float) Math.toRadians(angle), new Vector3f(0, 0, 1));
        Field faces = null;
        for (Field field : ModelBox.class.getDeclaredFields()) if (field.getType() == TexturedQuad[].class) { faces = field; break; }
        if (faces == null) throw new AssertionError("Native ModelBox quads unavailable");
        faces.setAccessible(true);
        List<float[][]> result = new ArrayList<>();
        for (ModelRenderer part : parts) {
            Matrix4f transform = new Matrix4f(placement);
            transform.rotate(part.rotateAngleZ, new Vector3f(0, 0, 1));
            transform.rotate(part.rotateAngleX, new Vector3f(1, 0, 0));
            for (ModelBox box : part.cubeList) for (TexturedQuad quad : (TexturedQuad[]) faces.get(box)) {
                float[][] vertices = new float[4][5];
                for (int i = 0; i < 4; i++) {
                    PositionTextureVertex vertex = quad.vertexPositions[i];
                    Vector4f point = Matrix4f.transform(transform, new Vector4f((float) vertex.vector3D.xCoord,
                            (float) vertex.vector3D.yCoord, (float) vertex.vector3D.zCoord, 1), null);
                    vertices[i] = new float[]{point.x / 16, point.y / 16, point.z / 16, vertex.texturePositionX, vertex.texturePositionY};
                }
                result.add(vertices);
            }
        }
        return result;
    }
}
