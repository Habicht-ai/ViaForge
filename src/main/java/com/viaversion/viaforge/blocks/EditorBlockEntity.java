package com.viaversion.viaforge.blocks;
import com.viaversion.viaforge.compatibility.ServerSession;

import com.viaversion.viaforge.blocks.gui.BlockEditorScreen;
import com.viaversion.viaforge.blocks.gui.CommandBlockScreen;
import com.viaversion.viaforge.blocks.gui.StructureBlockScreen;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Kind;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/** Server snapshots only. Editing never runs commands or changes structures on the client. */
public final class EditorBlockEntity extends TileEntity {
    private NBTTagCompound snapshot = new NBTTagCompound();
    private boolean received;

    public NBTTagCompound snapshot() { return (NBTTagCompound) snapshot.copy(); }
    public boolean received() { return received; }
    public boolean structure() { Definition d = ClientBlocks.definition(getBlockType()); return d != null && d.kind == Kind.STRUCTURE; }
    public int serverBlockId() { return ClientBlocks.definition(getBlockType()).id; }
    @Override public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        return structure() ? StructureBlockRenderer.bounds(snapshot).offset(pos.getX(), pos.getY(), pos.getZ())
                : new net.minecraft.util.AxisAlignedBB(pos, pos.add(1, 1, 1));
    }

    public void accept(NBTTagCompound tag) {
        // Some Via paths synthesize chunk tiles containing only id/x/y/z. Those
        // placeholders must neither erase a real snapshot nor enable an empty editor.
        if (tag == null || !tag.hasKey(structure() ? "mode" : "Command", 8)) return;
        snapshot = (NBTTagCompound) tag.copy(); received = true;
        if (structure() && worldObj != null) {
            try {
                LegacyClientBlockTypes.Mode mode = LegacyClientBlockTypes.Mode.valueOf(tag.getString("mode"));
                worldObj.setBlockState(pos, worldObj.getBlockState(pos).withProperty(LegacyClientBlockTypes.Structure.MODE, mode), 2);
            } catch (IllegalArgumentException ignored) { }
        }
        if (Minecraft.getMinecraft().currentScreen instanceof BlockEditorScreen) {
            ((BlockEditorScreen) Minecraft.getMinecraft().currentScreen).serverUpdated(this);
        }
    }
    @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); snapshot = (NBTTagCompound) tag.copy(); }
    @Override public void writeToNBT(NBTTagCompound tag) { tag.merge(snapshot); super.writeToNBT(tag); }
    @Override public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        Definition before = ClientBlocks.definition(oldState.getBlock()), after = ClientBlocks.definition(newState.getBlock());
        // Changing command mode swaps registry blocks while retaining the same server tile.
        return before == null || after == null || before.kind != after.kind;
    }

    public static boolean open(World world, BlockPos pos, EntityPlayer player) {
        Definition definition = ClientBlocks.definition(world.getBlockState(pos).getBlock());
        if (!world.isRemote || !ServerSession.supportsItem(definition) || !ServerEditorPermissions.canEdit(player)) return false;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof EditorBlockEntity)) return false;
        EditorBlockEntity editor = (EditorBlockEntity) tile;
        // Command blocks send a fresh, permission-checked snapshot in response to use.
        // Never let a fast reopen submit the cached command from before the last edit.
        if (!editor.structure()) editor.received = false;
        Minecraft.getMinecraft().displayGuiScreen(editor.structure() ? new StructureBlockScreen(editor) : new CommandBlockScreen(editor));
        return true;
    }
}
