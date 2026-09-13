package com.viaversion.viaforge.development;

import com.viaversion.viaforge.blocks.ClientBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

final class LegacyShapeSmokeTest {
    static void verify(WorldClient world) {
        BlockPos pos = new BlockPos(8, 70, 8);
        AxisAlignedBB query = new AxisAlignedBB(7, 69, 7, 10, 72, 10);
        for (int meta = 0; meta < 6; meta++) {
            IBlockState rod = local(198 << 4 | meta); world.setBlockState(pos, rod, 0);
            AxisAlignedBB bounds = rod.getBlock().getCollisionBoundingBox(world, pos, rod);
            require(Math.abs(volume(bounds) - .0625) < .00001, "End rod collision in direction " + meta);
            require(rod.getBlock().collisionRayTrace(world, pos, new Vec3(8.5, 73, 8.5), new Vec3(8.5, 69, 8.5)) != null, "End rod selection");
        }
        for (int meta : new int[]{0, 8}) {
            IBlockState slab = local(205 << 4 | meta); world.setBlockState(pos, slab, 0);
            List<AxisAlignedBB> boxes = new ArrayList<>(); slab.getBlock().addCollisionBoxesToList(world, pos, slab, query, boxes, null);
            require(boxes.size() == 1 && Math.abs(volume(boxes.get(0)) - .5) < .00001, "Purpur half slab volume");
            require(Math.abs(boxes.get(0).minY - (70 + (meta == 8 ? .5 : 0))) < .00001, "Upper/lower slab collision");
        }
        IBlockState path = local(208 << 4); world.setBlockState(pos, path, 0);
        require(Math.abs(path.getBlock().getCollisionBoundingBox(world, pos, path).maxY - 70.9375) < .00001, "Grass path height");
        for (int age = 0; age < 4; age++) {
            IBlockState crop = local(207 << 4 | age); world.setBlockState(pos, crop, 0);
            require(crop.getBlock().getCollisionBoundingBox(world, pos, crop) == null, "Beetroot has no collision");
            crop.getBlock().setBlockBoundsBasedOnState(world, pos);
            require(Math.abs(crop.getBlock().getSelectedBoundingBox(world, pos).maxY - (70 + (age + 1) * .125)) < .00001, "Beetroot selection by age");
        }
        IBlockState chorus = local(199 << 4); world.setBlockState(pos, chorus, 0);
        for (int mask = 0; mask < 64; mask++) {
            for (EnumFacing facing : EnumFacing.values()) {
                if ((mask & 1 << facing.getIndex()) != 0) world.setBlockState(pos.offset(facing), chorus, 0);
                else world.setBlockToAir(pos.offset(facing));
            }
            IBlockState actual = chorus.getBlock().getActualState(chorus, world, pos);
            for (IProperty<?> property : actual.getPropertyNames()) {
                EnumFacing face = EnumFacing.byName(property.getName());
                require(actual.getValue(property).equals((mask & 1 << face.getIndex()) != 0), "Chorus connection " + property.getName()
                        + " mask=" + mask + " actual=" + actual + " neighbor=" + world.getBlockState(pos.offset(face)));
            }
            require(!Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(actual).getGeneralQuads().isEmpty(), "Connected chorus geometry " + mask);
            List<AxisAlignedBB> boxes = new ArrayList<>(); chorus.getBlock().addCollisionBoxesToList(world, pos, chorus, query, boxes, null);
            double total = 0; for (AxisAlignedBB box : boxes) total += volume(box);
            require(Math.abs(total - (.625 * .625 * .625 + Integer.bitCount(mask) * .1875 * .625 * .625)) < .00001, "Chorus collision connections " + mask);
        }
        for (EnumFacing face : EnumFacing.values()) world.setBlockToAir(pos.offset(face));
        if (com.viaversion.viaforge.blocks.ServerBlockSession.supports(ClientBlocks.definition(local(219 << 4).getBlock()))) {
            for (int facing = 0; facing < 6; facing++) {
                IBlockState shulker = local(219 << 4 | facing); world.setBlockState(pos, shulker, 0);
                com.viaversion.viaforge.blocks.ShulkerBlockEntity tile = (com.viaversion.viaforge.blocks.ShulkerBlockEntity) world.getTileEntity(pos);
                require(tile != null, "Shulker block entity");
                world.addBlockEvent(pos, shulker.getBlock(), 1, 1);
                for (int tick = 0; tick < 10; tick++) tile.update();
                require(Math.abs(volume(shulker.getBlock().getCollisionBoundingBox(world, pos, shulker)) - 1.5) < .00001, "Open shulker collision " + facing);
                world.addBlockEvent(pos, shulker.getBlock(), 1, 0);
                for (int tick = 0; tick < 10; tick++) tile.update();
                require(Math.abs(volume(shulker.getBlock().getCollisionBoundingBox(world, pos, shulker)) - 1) < .00001, "Closed shulker collision");
            }
        }
        world.setBlockToAir(pos);
        verifyPlacement(world, pos);
    }
    private static void verifyPlacement(WorldClient world, BlockPos pos) {
        net.minecraft.entity.player.EntityPlayer player = new net.minecraft.entity.player.EntityPlayer(world,
                new com.mojang.authlib.GameProfile(new java.util.UUID(0, 2), "BlockPlacement")) {
            @Override public boolean isSpectator() { return false; }
        };
        player.setPosition(4, 70, 4);
        net.minecraft.item.Item slabItem = net.minecraft.item.Item.getItemById(ClientBlocks.localItem(205, 0));
        for (boolean top : new boolean[]{false, true}) {
            world.setBlockState(pos.down(), net.minecraft.init.Blocks.stone.getDefaultState(), 0);
            world.setBlockToAir(pos);
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(slabItem, 2);
            BlockPos click = top ? pos.offset(EnumFacing.WEST) : pos.down();
            if (top) world.setBlockState(click, net.minecraft.init.Blocks.stone.getDefaultState(), 0);
            require(slabItem.onItemUse(stack, player, world, click, top ? EnumFacing.EAST : EnumFacing.UP, .5F, top ? .75F : 1F, .5F), "Place purpur slab");
            IBlockState placed = world.getBlockState(pos);
            require(placed.equals(local(205 << 4 | (top ? 8 : 0))), "Slab placement half");
            require(slabItem.onItemUse(stack, player, world, pos, top ? EnumFacing.DOWN : EnumFacing.UP, .5F, .5F, .5F), "Merge purpur slabs");
            require(world.getBlockState(pos).equals(local(204 << 4)) && stack.stackSize == 0, "Double slab and consumed stack");
            world.setBlockToAir(pos.west());
        }
        net.minecraft.item.Item rodItem = net.minecraft.item.Item.getItemById(ClientBlocks.localItem(198, 0));
        for (EnumFacing face : EnumFacing.values()) {
            world.setBlockToAir(pos);
            BlockPos support = pos.offset(face.getOpposite());
            world.setBlockState(support, net.minecraft.init.Blocks.stone.getDefaultState(), 0);
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(rodItem);
            require(rodItem.onItemUse(stack, player, world, support, face, .5F, .5F, .5F), "Place end rod");
            require(world.getBlockState(pos).equals(local(198 << 4 | face.getIndex())), "End rod placement direction");
            world.setBlockToAir(support);
        }
        world.setBlockToAir(pos);
        world.setBlockState(pos.down(), net.minecraft.init.Blocks.farmland.getDefaultState(), 0);
        net.minecraft.item.Item seeds = net.minecraft.item.Item.getItemById(ClientBlocks.localItem(435, 0));
        require(seeds.onItemUse(new net.minecraft.item.ItemStack(seeds), player, world, pos.down(), EnumFacing.UP, .5F, 1F, .5F), "Plant beetroot seeds");
        require(world.getBlockState(pos).equals(local(207 << 4)), "Beetroot seed block prediction");
        world.setBlockToAir(pos);
        world.setBlockToAir(pos.down());
    }
    private static double volume(AxisAlignedBB box) { return (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ); }
    private static IBlockState local(int raw) { return Block.BLOCK_STATE_IDS.getByValue(ClientBlocks.localState(raw)); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
