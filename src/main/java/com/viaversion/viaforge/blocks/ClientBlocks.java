package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Kind;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSeeds;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Client registry representations; inventory entries are enabled only on matching servers. */
public final class ClientBlocks {
    public static final String[] COLORS = LegacyBlockCatalog.COLORS;
    private static final Map<Integer, IBlockState> STATES = new HashMap<>();
    private static final Map<Block, Definition> DEFINITIONS = new HashMap<>();
    private static final Map<Integer, Item> ITEMS = new HashMap<>();
    private static final Map<Integer, Definition> SERVER_ITEMS = new HashMap<>();

    private ClientBlocks() { }

    public static void register() {
        net.minecraftforge.fml.client.registry.ClientRegistry.registerTileEntity(EditorBlockEntity.class, "viaforge:block_editor", new StructureBlockRenderer());
        net.minecraftforge.fml.client.registry.ClientRegistry.registerTileEntity(ShulkerBlockEntity.class, "viaforge:shulker_box", new ShulkerBlockRenderer());
        for (Definition definition : LegacyBlockCatalog.BLOCKS) {
            Block block = LegacyClientBlockTypes.create(definition);
            block.setUnlocalizedName("viaforge." + definition.name);
            block.setCreativeTab(CreativeTabs.tabBlock);
            DEFINITIONS.put(block, definition);
            GameRegistry.registerBlock(block, definition.itemId() >= 0 && definition.kind != Kind.CROP && definition.kind != Kind.BED ? ServerItemBlock.class : null, definition.name);
            for (int meta = 0; meta < 16; meta++) {
                if (definition.acceptsMetadata(meta)) {
                    IBlockState state = block.getStateFromMeta(definition.color < 0 || definition.kind == Kind.BED ? meta : 0);
                    STATES.put(definition.stateId(meta), state);
                    if (definition.kind == Kind.BED && definition.color == 14) STATES.put(26 << 4 | meta, state);
                }
            }
            if (definition.itemId() < 0) continue;
            Item item;
            if (definition.kind == Kind.CROP) {
                item = new ServerSeeds(block);
                GameRegistry.registerItem(item, "beetroot_seeds");
            } else if (definition.kind == Kind.BED) {
                item = new ServerBedItem(block);
                GameRegistry.registerItem(item, definition.name);
            } else item = Item.getItemFromBlock(block);
            ITEMS.put(definition.itemId() << 16 | definition.itemData(), item);
            SERVER_ITEMS.put(Item.getIdFromItem(item), definition);
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation("viaforge:" + (definition.kind == Kind.CROP ? "beetroot_seeds" : definition.name), "inventory"));
        }
    }

    public static Definition definition(Block block) { return DEFINITIONS.get(block); }
    public static int localState(int serverState) {
        IBlockState state = STATES.get(serverState);
        return state == null ? -1 : Block.BLOCK_STATE_IDS.get(state);
    }
    public static int localItem(int serverId, int data) {
        Item item = ITEMS.get(serverId << 16 | (data & 65535));
        return item == null ? -1 : Item.getIdFromItem(item);
    }
    public static Definition serverItem(int localId) { return SERVER_ITEMS.get(localId); }
    public static ItemStack pick(Block block) {
        Definition definition = DEFINITIONS.get(block);
        if (definition == null || !ServerBlockSession.supportsItem(definition)) return null;
        int itemId = definition.kind == Kind.DOUBLE_SLAB ? 205 : definition.itemId();
        Item item = ITEMS.get(itemId << 16 | definition.itemData());
        return item == null ? null : new ItemStack(item);
    }

    private static String displayName(Definition definition) {
        if (definition.kind == Kind.CROP) return "Beetroot Seeds";
        if (definition.id == 206) return "End Stone Bricks";
        StringBuilder name = new StringBuilder();
        for (String word : definition.name.split("_")) {
            if (name.length() > 0) name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }

    public static final class ServerItemBlock extends ItemBlock {
        public ServerItemBlock(Block block) { super(block); if (definition(block).kind == Kind.SHULKER) setMaxStackSize(1); }
        @Override public String getItemStackDisplayName(ItemStack stack) { return displayName(definition(block)); }
        @Override public void getSubItems(Item item, CreativeTabs tab, java.util.List<ItemStack> items) {
            if (ServerBlockSession.supportsItem(definition(block))) items.add(new ItemStack(item));
        }
        @Override public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
                EnumFacing side, float hitX, float hitY, float hitZ) {
            if (!ServerBlockSession.supportsItem(definition(block)) || stack.stackSize <= 0) return false;
            if (definition(block).kind == Kind.SLAB) {
                IBlockState state = world.getBlockState(pos);
                boolean upper = state.getBlock() == block && block.getMetaFromState(state) == 8;
                if (state.getBlock() == block && (side == EnumFacing.UP && !upper || side == EnumFacing.DOWN && upper)) {
                    return player.canPlayerEdit(pos, side, stack) && mergeSlab(stack, world, pos);
                }
                BlockPos adjacent = pos.offset(side);
                if (world.getBlockState(adjacent).getBlock() == block) return player.canPlayerEdit(adjacent, side, stack) && mergeSlab(stack, world, adjacent);
            }
            return super.onItemUse(stack, player, world, pos, side, hitX, hitY, hitZ);
        }
        private boolean mergeSlab(ItemStack stack, World world, BlockPos pos) {
            IBlockState doubled = STATES.get(204 << 4);
            if (!world.checkNoEntityCollision(doubled.getBlock().getCollisionBoundingBox(world, pos, doubled))) return false;
            if (world.setBlockState(pos, doubled, 3)) {
                world.playSoundEffect(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                        block.stepSound.getPlaceSound(), (block.stepSound.getVolume() + 1) / 2, block.stepSound.getFrequency() * .8F);
                stack.stackSize--;
            }
            return true;
        }
    }

    private static final class ServerSeeds extends ItemSeeds {
        private final Block crop;
        ServerSeeds(Block crop) { super(crop, Blocks.farmland); this.crop = crop; setUnlocalizedName("viaforge.beetroot_seeds"); }
        @Override public String getItemStackDisplayName(ItemStack stack) { return "Beetroot Seeds"; }
        @Override public void getSubItems(Item item, CreativeTabs tab, java.util.List<ItemStack> items) {
            if (ServerBlockSession.supports(definition(crop))) items.add(new ItemStack(item));
        }
        @Override public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
                EnumFacing side, float x, float y, float z) {
            return ServerBlockSession.supports(definition(crop)) && super.onItemUse(stack, player, world, pos, side, x, y, z);
        }
    }

    private static final class ServerBedItem extends net.minecraft.item.ItemBed {
        private final Block bed;
        ServerBedItem(Block bed) { this.bed = bed; setMaxStackSize(1); setUnlocalizedName("viaforge." + definition(bed).name); }
        @Override public String getItemStackDisplayName(ItemStack stack) { return displayName(definition(bed)); }
        @Override public void getSubItems(Item item, CreativeTabs tab, java.util.List<ItemStack> items) {
            if (ServerBlockSession.supportsItem(definition(bed))) items.add(new ItemStack(item));
        }
        @Override public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
                EnumFacing side, float x, float y, float z) {
            // Like vanilla ItemBed, acknowledge client use and let the server place
            // both halves. Never execute 1.8's server code, which places Blocks.bed.
            return world.isRemote && stack.stackSize > 0 && ServerBlockSession.supportsItem(definition(bed));
        }
    }
}
