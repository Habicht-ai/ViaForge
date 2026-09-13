package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Kind;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.*;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Client shapes and placement prediction. Ticks, growth and redstone remain server-owned. */
final class LegacyClientBlockTypes {
    static Block create(Definition d) {
        Block block;
        switch (d.kind) {
            case BED: block = new Bed(); break;
            case STAIRS: block = new Stairs(new Cube(Material.rock).setHardness(d.hardness).setResistance(d.resistance * 5F / 3F).getDefaultState()); break;
            case PILLAR: block = new Pillar(); break;
            case SLAB: block = new Slab(); break;
            case DOUBLE_SLAB: block = new DoubleSlab(); break;
            case ROD: block = new Rod(); break;
            case CHORUS: block = new Chorus(); break;
            case FLOWER: block = new Flower(); break;
            case CROP: block = new Crop(); break;
            case PATH: block = new Path(); break;
            case ICE: block = new FrostedIce(); break;
            case COMMAND: block = new Command(); break;
            case OBSERVER: block = new Observer(); break;
            case SHULKER: block = new Shulker(); break;
            case VOID: block = new StructureVoid(); break;
            case GATEWAY: block = new Gateway(); break;
            case STRUCTURE: block = new Structure(); break;
            case GLAZED: block = new Glazed(); break;
            case POWDER: block = new Cube(Material.sand); break;
            default: block = new Cube(d.id == 214 ? Material.grass : Material.rock);
        }
        block.setHardness(d.hardness).setResistance(d.resistance * 5F / 3F);
        block.setStepSound(Block.soundTypeStone);
        if (d.kind == Kind.POWDER) block.setStepSound(Block.soundTypeSand);
        else if (d.kind == Kind.PATH || d.kind == Kind.CROP) block.setStepSound(Block.soundTypeGrass);
        else if (d.kind == Kind.BED || d.kind == Kind.ROD || d.kind == Kind.CHORUS || d.kind == Kind.FLOWER || d.id == 214) block.setStepSound(Block.soundTypeWood);
        else if (d.kind == Kind.ICE) block.setStepSound(Block.soundTypeGlass);
        if (block.getMaterial() == Material.rock) block.setHarvestLevel("pickaxe", 0);
        if (d.kind == Kind.PATH || d.kind == Kind.POWDER) block.setHarvestLevel("shovel", 0);
        if (d.kind == Kind.ROD) block.setLightLevel(.9375F);
        if (d.id == 213) block.setLightLevel(.2F);
        if (d.kind == Kind.GATEWAY) block.setLightLevel(1);
        return block;
    }

    static class Cube extends Block {
        Cube(Material material) { super(material); }
        @Override public ItemStack getPickBlock(MovingObjectPosition hit, World world, BlockPos pos, EntityPlayer player) {
            return ClientBlocks.pick(this);
        }
        @Override public int getMetaFromState(IBlockState state) { return 0; }
        @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState(); }
        @Override public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos, IBlockState state) {
            if (world.getBlockState(pos).getBlock() == this) setBlockBoundsBasedOnState(world, pos);
            return super.getCollisionBoundingBox(world, pos, state);
        }
    }
    static final class Bed extends net.minecraft.block.BlockBed {
        @Override public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighbor) {
            // The two halves/color updates arrive separately; the server removes
            // unsupported beds. Do not delete a temporarily mismatched client half.
        }
        @Override public ItemStack getPickBlock(MovingObjectPosition hit, World world, BlockPos pos, EntityPlayer player) { return ClientBlocks.pick(this); }
        @Override public net.minecraft.item.Item getItem(World world, BlockPos pos) { return net.minecraft.item.Item.getItemById(ClientBlocks.localItem(355, ClientBlocks.definition(this).color)); }
        @Override public boolean isBed(IBlockAccess world, BlockPos pos, Entity entity) { return true; }
        @Override public EnumFacing getBedDirection(IBlockAccess world, BlockPos pos) { return world.getBlockState(pos).getValue(FACING); }
        @Override public boolean isBedFoot(IBlockAccess world, BlockPos pos) { return world.getBlockState(pos).getValue(PART) == EnumPartType.FOOT; }
        @Override public void onFallenUpon(World world, BlockPos pos, Entity entity, float distance) { super.onFallenUpon(world, pos, entity, distance * .5F); }
        @Override public void onLanded(World world, Entity entity) {
            if (entity.isSneaking()) super.onLanded(world, entity);
            else if (entity.motionY < 0) entity.motionY = -entity.motionY * .66 * (entity instanceof EntityLivingBase ? 1 : .8);
        }
    }
    static final class Stairs extends BlockStairs {
        Stairs(IBlockState model) { super(model); }
        @Override public ItemStack getPickBlock(MovingObjectPosition hit, World world, BlockPos pos, EntityPlayer player) { return ClientBlocks.pick(this); }
    }
    static final class Pillar extends Cube {
        static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class);
        Pillar() { super(Material.rock); setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y)); }
        @Override protected BlockState createBlockState() { return new BlockState(this, AXIS); }
        @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(AXIS, meta == 4 ? EnumFacing.Axis.X : meta == 8 ? EnumFacing.Axis.Z : EnumFacing.Axis.Y); }
        @Override public int getMetaFromState(IBlockState state) { return state.getValue(AXIS) == EnumFacing.Axis.X ? 4 : state.getValue(AXIS) == EnumFacing.Axis.Z ? 8 : 0; }
        @Override public IBlockState onBlockPlaced(World w, BlockPos p, EnumFacing face, float x, float y, float z, int meta, EntityLivingBase placer) { return getDefaultState().withProperty(AXIS, face.getAxis()); }
    }
    enum Variant implements IStringSerializable { DEFAULT; @Override public String getName() { return "default"; } }
    static final class Slab extends BlockSlab {
        static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);
        Slab() { super(Material.rock); setDefaultState(blockState.getBaseState().withProperty(HALF, EnumBlockHalf.BOTTOM)); }
        @Override public boolean isDouble() { return false; }
        @Override protected BlockState createBlockState() { return new BlockState(this, HALF, VARIANT); }
        @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(HALF, (meta & 8) == 0 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP); }
        @Override public int getMetaFromState(IBlockState state) { return state.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0; }
        @Override public String getUnlocalizedName(int meta) { return getUnlocalizedName(); }
        @Override public IProperty<?> getVariantProperty() { return VARIANT; }
        @Override public Object getVariant(ItemStack stack) { return Variant.DEFAULT; }
        @Override public ItemStack getPickBlock(MovingObjectPosition hit, World world, BlockPos pos, EntityPlayer player) { return ClientBlocks.pick(this); }
    }
    static final class DoubleSlab extends Cube {
        DoubleSlab() { super(Material.rock); }
        @Override protected BlockState createBlockState() { return new BlockState(this, Slab.VARIANT); }
    }
    static class Facing extends Cube {
        static final PropertyDirection FACING = PropertyDirection.create("facing");
        Facing(Material material) { super(material); setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP)); }
        @Override protected BlockState createBlockState() { return new BlockState(this, FACING); }
        @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.getFront(meta & 7)); }
        @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getIndex(); }
        @Override public IBlockState onBlockPlaced(World w, BlockPos p, EnumFacing face, float x, float y, float z, int meta, EntityLivingBase placer) { return getDefaultState().withProperty(FACING, face); }
    }
    static final class Rod extends Facing {
        Rod() { super(Material.circuits); setLightOpacity(0); }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public boolean isFullCube() { return false; }
        @Override public EnumWorldBlockLayer getBlockLayer() { return EnumWorldBlockLayer.CUTOUT; }
        @Override public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) {
            setRodBounds(world.getBlockState(pos));
        }
        @Override public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos, IBlockState state) {
            // Placement asks about a future state while the world still contains air.
            setRodBounds(state);
            return new AxisAlignedBB(pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ,
                    pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ);
        }
        private void setRodBounds(IBlockState state) {
            EnumFacing.Axis axis = state.getValue(FACING).getAxis();
            setBlockBounds(axis == EnumFacing.Axis.X ? 0 : .375F, axis == EnumFacing.Axis.Y ? 0 : .375F, axis == EnumFacing.Axis.Z ? 0 : .375F,
                    axis == EnumFacing.Axis.X ? 1 : .625F, axis == EnumFacing.Axis.Y ? 1 : .625F, axis == EnumFacing.Axis.Z ? 1 : .625F);
        }
        @Override public IBlockState onBlockPlaced(World w, BlockPos p, EnumFacing face, float x, float y, float z, int meta, EntityLivingBase placer) {
            IBlockState support = w.getBlockState(p.offset(face.getOpposite()));
            return getDefaultState().withProperty(FACING, support.getBlock() == this && support.getValue(FACING) == face ? face.getOpposite() : face);
        }
    }
    static final class Chorus extends Cube {
        static final PropertyBool[] CONNECTIONS = {PropertyBool.create("down"), PropertyBool.create("up"), PropertyBool.create("north"),
                PropertyBool.create("south"), PropertyBool.create("west"), PropertyBool.create("east")};
        Chorus() { super(Material.plants); setLightOpacity(0); }
        // BlockState sorts its input array in place. Keep our direction-indexed array intact.
        @Override protected BlockState createBlockState() { return new BlockState(this, CONNECTIONS.clone()); }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public boolean isFullCube() { return false; }
        @Override public EnumWorldBlockLayer getBlockLayer() { return EnumWorldBlockLayer.CUTOUT; }
        @Override public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
            for (EnumFacing face : EnumFacing.values()) {
                Block neighbor = world.getBlockState(pos.offset(face)).getBlock();
                state = state.withProperty(CONNECTIONS[face.getIndex()], neighbor == this || neighbor instanceof Flower || face == EnumFacing.DOWN && neighbor == Blocks.end_stone);
            }
            return state;
        }
        @Override public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) {
            IBlockState s = getActualState(world.getBlockState(pos), world, pos);
            setBlockBounds(s.getValue(CONNECTIONS[4]) ? 0 : .1875F, s.getValue(CONNECTIONS[0]) ? 0 : .1875F, s.getValue(CONNECTIONS[2]) ? 0 : .1875F,
                    s.getValue(CONNECTIONS[5]) ? 1 : .8125F, s.getValue(CONNECTIONS[1]) ? 1 : .8125F, s.getValue(CONNECTIONS[3]) ? 1 : .8125F);
        }
        @Override public void addCollisionBoxesToList(World world, BlockPos pos, IBlockState state, AxisAlignedBB mask, List<AxisAlignedBB> result, Entity entity) {
            state = getActualState(state, world, pos);
            addBox(pos, mask, result, .1875, .1875, .1875, .8125, .8125, .8125);
            for (EnumFacing face : EnumFacing.values()) {
                if (!state.getValue(CONNECTIONS[face.getIndex()])) continue;
                addBox(pos, mask, result, face == EnumFacing.WEST ? 0 : face == EnumFacing.EAST ? .8125 : .1875,
                        face == EnumFacing.DOWN ? 0 : face == EnumFacing.UP ? .8125 : .1875,
                        face == EnumFacing.NORTH ? 0 : face == EnumFacing.SOUTH ? .8125 : .1875,
                        face == EnumFacing.EAST ? 1 : face == EnumFacing.WEST ? .1875 : .8125,
                        face == EnumFacing.UP ? 1 : face == EnumFacing.DOWN ? .1875 : .8125,
                        face == EnumFacing.SOUTH ? 1 : face == EnumFacing.NORTH ? .1875 : .8125);
            }
        }
        private void addBox(BlockPos pos, AxisAlignedBB mask, List<AxisAlignedBB> result, double x, double y, double z, double xx, double yy, double zz) {
            AxisAlignedBB box = new AxisAlignedBB(x, y, z, xx, yy, zz).offset(pos.getX(), pos.getY(), pos.getZ());
            if (box.intersectsWith(mask)) result.add(box);
        }
    }
    static final class Flower extends Cube {
        static final PropertyInteger AGE = PropertyInteger.create("age", 0, 5);
        Flower() { super(Material.plants); }
        @Override protected BlockState createBlockState() { return new BlockState(this, AGE); }
        @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(AGE, Math.min(meta, 5)); }
        @Override public int getMetaFromState(IBlockState state) { return state.getValue(AGE); }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public boolean isFullCube() { return false; }
        @Override public EnumWorldBlockLayer getBlockLayer() { return EnumWorldBlockLayer.CUTOUT; }
    }
    static class AgeFour extends Cube {
        static final PropertyInteger AGE = PropertyInteger.create("age", 0, 3);
        AgeFour(Material material) { super(material); }
        @Override protected BlockState createBlockState() { return new BlockState(this, AGE); }
        @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(AGE, meta & 3); }
        @Override public int getMetaFromState(IBlockState state) { return state.getValue(AGE); }
    }
    static final class Crop extends AgeFour implements net.minecraft.block.IGrowable {
        Crop() { super(Material.plants); setLightOpacity(0); }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public boolean isFullCube() { return false; }
        @Override public EnumWorldBlockLayer getBlockLayer() { return EnumWorldBlockLayer.CUTOUT; }
        @Override public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos, IBlockState state) { return null; }
        @Override public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) { setBlockBounds(0, 0, 0, 1, (world.getBlockState(pos).getValue(AGE) + 1) * .125F, 1); }
        @Override public boolean canPlaceBlockAt(World world, BlockPos pos) { return world.getBlockState(pos.down()).getBlock() == Blocks.farmland; }
        @Override public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean client) {
            return ServerBlockSession.supports(ClientBlocks.definition(this)) && state.getValue(AGE) < 3;
        }
        @Override public boolean canUseBonemeal(World world, java.util.Random random, BlockPos pos, IBlockState state) { return state.getValue(AGE) < 3; }
        @Override public void grow(World world, java.util.Random random, BlockPos pos, IBlockState state) {
            // The server supplies growth/consumption; IGrowable acknowledges the local
            // successful use so Minecraft plays its normal hand swing.
        }
    }
    static final class FrostedIce extends AgeFour {
        FrostedIce() { super(Material.ice); slipperiness = .98F; setLightOpacity(3); }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public EnumWorldBlockLayer getBlockLayer() { return EnumWorldBlockLayer.TRANSLUCENT; }
        @Override public boolean shouldSideBeRendered(IBlockAccess world, BlockPos pos, EnumFacing side) { return world.getBlockState(pos).getBlock() != this && super.shouldSideBeRendered(world, pos, side); }
    }
    static final class Path extends Cube {
        Path() { super(Material.ground); setBlockBounds(0, 0, 0, 1, .9375F, 1); setLightOpacity(255); }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public boolean isFullCube() { return false; }
    }
    static final class Command extends Facing {
        static final PropertyBool CONDITIONAL = PropertyBool.create("conditional");
        Command() { super(Material.rock); }
        @Override protected BlockState createBlockState() { return new BlockState(this, FACING, CONDITIONAL); }
        @Override public IBlockState getStateFromMeta(int meta) { return super.getStateFromMeta(meta).withProperty(CONDITIONAL, (meta & 8) != 0); }
        @Override public int getMetaFromState(IBlockState state) { return super.getMetaFromState(state) | (state.getValue(CONDITIONAL) ? 8 : 0); }
        @Override public boolean hasTileEntity(IBlockState state) { return true; }
        @Override public net.minecraft.tileentity.TileEntity createTileEntity(World world, IBlockState state) { return new EditorBlockEntity(); }
        @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float x, float y, float z) { return EditorBlockEntity.open(world, pos, player); }
        @Override public IBlockState onBlockPlaced(World world, BlockPos pos, EnumFacing face, float x, float y, float z, int meta, EntityLivingBase placer) {
            return getDefaultState().withProperty(FACING, net.minecraft.block.BlockPistonBase.getFacingFromEntity(world, pos, placer));
        }
    }
    static final class Observer extends Facing {
        static final PropertyBool POWERED = PropertyBool.create("powered");
        Observer() { super(Material.rock); }
        @Override protected BlockState createBlockState() { return new BlockState(this, FACING, POWERED); }
        @Override public IBlockState getStateFromMeta(int meta) { return super.getStateFromMeta(meta).withProperty(POWERED, (meta & 8) != 0); }
        @Override public int getMetaFromState(IBlockState state) { return super.getMetaFromState(state) | (state.getValue(POWERED) ? 8 : 0); }
        @Override public IBlockState onBlockPlaced(World w, BlockPos p, EnumFacing face, float x, float y, float z, int meta, EntityLivingBase placer) { return getDefaultState().withProperty(FACING, net.minecraft.block.BlockPistonBase.getFacingFromEntity(w, p, placer).getOpposite()); }
    }
    static final class Shulker extends Facing {
        Shulker() { super(Material.rock); setLightOpacity(0); }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public boolean isFullCube() { return false; }
        @Override public int getRenderType() { return 2; }
        @Override public boolean hasTileEntity(IBlockState state) { return true; }
        @Override public net.minecraft.tileentity.TileEntity createTileEntity(World world, IBlockState state) { return new ShulkerBlockEntity(); }
        @Override public boolean onBlockEventReceived(World world, BlockPos pos, IBlockState state, int event, int value) {
            net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
            return tile != null && tile.receiveClientEvent(event, value);
        }
        @Override public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) {
            net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
            float opening = tile instanceof ShulkerBlockEntity ? ((ShulkerBlockEntity) tile).progress(1) * .5F : 0;
            EnumFacing facing = world.getBlockState(pos).getValue(FACING);
            setBlockBounds(facing == EnumFacing.WEST ? -opening : 0, facing == EnumFacing.DOWN ? -opening : 0, facing == EnumFacing.NORTH ? -opening : 0,
                    facing == EnumFacing.EAST ? 1 + opening : 1, facing == EnumFacing.UP ? 1 + opening : 1, facing == EnumFacing.SOUTH ? 1 + opening : 1);
        }
        @Override public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos, IBlockState state) {
            setBlockBounds(0, 0, 0, 1, 1, 1);
            return super.getCollisionBoundingBox(world, pos, state);
        }
        @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float x, float y, float z) { return true; }
    }
    static final class StructureVoid extends Cube {
        StructureVoid() { super(Material.circuits); setBlockBounds(.3F, .3F, .3F, .7F, .7F, .7F); }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public boolean isFullCube() { return false; }
        @Override public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos, IBlockState state) { return null; }
    }
    static final class Gateway extends Cube {
        Gateway() { super(Material.portal); setLightOpacity(0); }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public boolean isFullCube() { return false; }
        @Override public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos, IBlockState state) { return null; }
    }
    enum Mode implements IStringSerializable { SAVE, LOAD, CORNER, DATA; @Override public String getName() { return name().toLowerCase(java.util.Locale.ROOT); } }
    static final class Structure extends Cube {
        static final PropertyEnum<Mode> MODE = PropertyEnum.create("mode", Mode.class);
        Structure() { super(Material.iron); }
        @Override protected BlockState createBlockState() { return new BlockState(this, MODE); }
        @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(MODE, Mode.values()[meta & 3]); }
        @Override public int getMetaFromState(IBlockState state) { return state.getValue(MODE).ordinal(); }
        @Override public boolean hasTileEntity(IBlockState state) { return true; }
        @Override public net.minecraft.tileentity.TileEntity createTileEntity(World world, IBlockState state) { return new EditorBlockEntity(); }
        @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float x, float y, float z) { return EditorBlockEntity.open(world, pos, player); }
    }
    static final class Glazed extends Cube {
        static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
        Glazed() { super(Material.rock); }
        @Override protected BlockState createBlockState() { return new BlockState(this, FACING); }
        @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3)); }
        @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getHorizontalIndex(); }
        @Override public IBlockState onBlockPlaced(World w, BlockPos p, EnumFacing face, float x, float y, float z, int meta, EntityLivingBase placer) { return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()); }
    }
    private LegacyClientBlockTypes() { }
}
