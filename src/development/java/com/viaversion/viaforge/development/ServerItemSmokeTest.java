package com.viaversion.viaforge.development;

import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ScreenShotHelper;

final class ServerItemSmokeTest {
    static List<ItemStack> variants(LegacyItemCatalog.Definition definition) {
        List<ItemStack> stacks = new ArrayList<>();
        net.minecraft.item.Item item = ClientItems.item(definition); item.getSubItems(item, item.getCreativeTab(), stacks);
        return stacks;
    }
    static void disconnected() {
        require(net.minecraft.init.Items.iron_ingot.getCreativeTab() == net.minecraft.creativetab.CreativeTabs.tabMaterials, "Native material tab restored outside newer servers");
        for (LegacyItemCatalog.Definition definition : LegacyItemCatalog.ITEMS) require(variants(definition).isEmpty(), "Items hidden outside newer servers: " + definition.name);
    }
    static void pipeline(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server) throws Exception {
        int count = 0;
        ByteBuf skullPacket = BlockPipelineSmokeTest.packet(BlockItemPipelineSmokeTest.clientbound(profile, "BLOCK_ENTITY_DATA"));
        Types.BLOCK_POSITION1_8.write(skullPacket, new com.viaversion.viaversion.api.minecraft.BlockPosition(4, 64, 4)); skullPacket.writeByte(4);
        com.viaversion.nbt.tag.CompoundTag skullTag = new com.viaversion.nbt.tag.CompoundTag(); skullTag.putByte("SkullType", (byte)5); skullTag.putByte("Rot", (byte)7);
        Types.NAMED_COMPOUND_TAG.write(skullPacket, skullTag); BlockPipelineSmokeTest.receiveCompressed(client, server, skullPacket);
        ByteBuf skullResult = BlockPipelineSmokeTest.take(client, 0x35);
        try { Types.BLOCK_POSITION1_8.read(skullResult); require(skullResult.readUnsignedByte() == 4 && Types.NAMED_COMPOUND_TAG.read(skullResult).getByte("SkullType") == 5, "Dragon head tile type preserved"); }
        finally { skullResult.release(); }
        for (LegacyItemCatalog.Definition definition : LegacyItemCatalog.ITEMS) {
            List<ItemStack> variants = variants(definition);
            require(variants.isEmpty() == (profile.protocol() < definition.protocol), "Item release boundary " + definition.name + " " + profile);
            for (ItemStack stack : variants) {
                if (definition.durability > 0) stack.setItemDamage(117);
                if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
                stack.getTagCompound().setInteger("ViaForgeTestValue", 12345);
                NBTTagCompound display = new NBTTagCompound(); display.setString("Name", "Named " + definition.name);
                NBTTagList lore = new NBTTagList(); lore.appendTag(new NBTTagString("Original lore")); display.setTag("Lore", lore);
                stack.getTagCompound().setTag("display", display);
                if (definition.kind == LegacyItemCatalog.Kind.SHIELD) {
                    NBTTagCompound banner = new NBTTagCompound(); banner.setInteger("Base", 4);
                    NBTTagList patterns = new NBTTagList(); NBTTagCompound stripe = new NBTTagCompound(); stripe.setString("Pattern", "cs"); stripe.setInteger("Color", 1); patterns.appendTag(stripe); banner.setTag("Patterns", patterns); stack.getTagCompound().setTag("BlockEntityTag", banner);
                }
                if (definition.kind == LegacyItemCatalog.Kind.BOOK) { NBTTagList recipes = new NBTTagList(); recipes.appendTag(new NBTTagString("minecraft:stone_button")); stack.getTagCompound().setTag("Recipes", recipes); }
                Item original = wire(stack); original.setIdentifier(definition.id); if (!definition.preservesDamage()) original.setData((short)definition.data);
                ByteBuf packet = BlockPipelineSmokeTest.packet(BlockItemPipelineSmokeTest.clientbound(profile, "CONTAINER_SET_SLOT"));
                packet.writeByte(0).writeShort(36); Types.ITEM1_8.write(packet, original); BlockPipelineSmokeTest.receiveCompressed(client, server, packet);
                ByteBuf received = BlockPipelineSmokeTest.take(client, 0x2f); Item local;
                try { received.skipBytes(3); local = Types.ITEM1_8.read(received); } finally { received.release(); }
                String label = definition.name + " " + stack.getTagCompound() + " " + profile;
                require(local.identifier() == net.minecraft.item.Item.getIdFromItem(stack.getItem()), "Native item identity " + label + ": " + local);
                require(local.data() == (definition.preservesDamage() ? 117 : 0), "Item durability " + label);
                require(original.tag().equals(local.tag()), "Original item NBT restored " + label + ": " + local.tag());
                for (boolean fresh : new boolean[]{false, true}) for (boolean creative : new boolean[]{false, true}) {
                    Item outgoing = fresh ? wire(stack) : local;
                    ByteBuf click = BlockPipelineSmokeTest.packet(creative ? 0x10 : 0x0e);
                    if (creative) click.writeShort(36); else click.writeByte(0).writeShort(36).writeByte(0).writeShort(1).writeByte(0);
                    Types.ITEM1_8.write(click, outgoing);
                    Item result = BlockItemPipelineSmokeTest.sendItem(client, server, click, BlockItemPipelineSmokeTest.serverbound(profile, creative ? "SET_CREATIVE_MODE_SLOT" : "CONTAINER_CLICK"), creative);
                    require(result.identifier() == original.identifier() && result.data() == original.data(), "Server item identity " + label + ": " + result);
                    require(original.tag().equals(result.tag()), "Server item NBT " + label + ": " + result.tag());
                }
                count++;
            }
        }
        System.out.println("[ViaForge item smoke] " + profile + ": " + count + " item variants, server/client/creative/click/NBT/durability round trips passed");
    }
    static Item wire(ItemStack stack) throws Exception {
        ByteBuf bytes = Unpooled.buffer();
        try { new PacketBuffer(bytes).writeItemStackToBuffer(stack); return Types.ITEM1_8.read(bytes); } finally { bytes.release(); }
    }
    static void models(BlockVersionProfile profile, WorldClient world, Path directory) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        IBakedModel missing = mc.getRenderItem().getItemModelMesher().getModelManager().getMissingModel();
        for (LegacyItemCatalog.Definition definition : LegacyItemCatalog.ITEMS) for (ItemStack stack : variants(definition)) {
            IBakedModel model = mc.getRenderItem().getItemModelMesher().getItemModel(stack);
            require(model != missing, "Item model " + definition.name);
            require(model.isBuiltInRenderer() || model.getParticleTexture().getIconName().startsWith("viaforge:"), "Target item texture " + definition.name);
            require(stack.getMaxStackSize() == definition.stackSize && stack.getMaxDamage() == definition.durability, "Item properties " + definition.name);
            if (definition.kind == LegacyItemCatalog.Kind.ELYTRA) {
                stack.setItemDamage(431);
                require(mc.getRenderItem().getItemModelMesher().getItemModel(stack) != model, "Broken elytra model");
                require(((ItemArmor)stack.getItem()).damageReduceAmount == 0, "Elytra must not grant armor points");
            }
        }
        if (profile == BlockVersionProfile.V1_12_2) preview(directory);
    }
    static void interactions(BlockVersionProfile profile, WorldClient world, net.minecraft.client.entity.EntityPlayerSP player) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        player.clearItemInUse();
        for (LegacyItemCatalog.Definition definition : LegacyItemCatalog.ITEMS) {
            if (definition.protocol > profile.protocol()) continue;
            ItemStack stack = new ItemStack(ClientItems.item(definition));
            player.inventory.mainInventory[0] = stack; player.inventory.currentItem = 0;
            player.getFoodStats().setFoodLevel(10);
            stack.getItem().onItemRightClick(stack, world, player);
            if (definition.kind == LegacyItemCatalog.Kind.FOOD || definition.kind == LegacyItemCatalog.Kind.SOUP || definition.kind == LegacyItemCatalog.Kind.POTION) {
                require(player.getItemInUse() == stack && player.getItemInUseCount() == 32, "Native eating/drinking " + definition.name);
                player.clearItemInUse();
                if (stack.getItem() instanceof ItemFood) {
                    player.getFoodStats().setFoodLevel(20); stack.getItem().onItemRightClick(stack, world, player);
                    require(player.isUsingItem() == (definition.id == 432), "Chorus always edible; ordinary food needs hunger");
                    player.clearItemInUse();
                    require(((ItemFood)stack.getItem()).getHealAmount(stack) == (definition.id == 432 ? 4 : definition.id == 434 ? 1 : 6), "Food value " + definition.name);
                }
            }
            if (definition.kind == LegacyItemCatalog.Kind.SHIELD) {
                require(player.getItemInUse() == stack && player.getItemInUseCount() == 72000, "Shield main-hand use");
                IBakedModel blocking = mc.getRenderItem().getItemModelMesher().getItemModel(stack);
                player.clearItemInUse();
                require(mc.getRenderItem().getItemModelMesher().getItemModel(stack) != blocking, "Shield blocking model");
            }
            if (definition.kind == LegacyItemCatalog.Kind.ELYTRA) {
                require(player.getCurrentArmor(2) != null && player.getCurrentArmor(2).getItem() == stack.getItem(), "Elytra right-click equips chest slot");
                require(player.inventoryContainer.getSlot(6).isItemValid(new ItemStack(stack.getItem())), "Elytra accepted by native chest slot");
                player.setCurrentItemOrArmor(3, null);
            }
            if (definition.kind == LegacyItemCatalog.Kind.HEAD) require(player.inventoryContainer.getSlot(5).isItemValid(stack), "Dragon head accepted by native helmet slot");
            if (definition.kind == LegacyItemCatalog.Kind.ARROW || definition.kind == LegacyItemCatalog.Kind.TIPPED_ARROW) {
                java.util.Arrays.fill(player.inventory.mainInventory, null); player.inventory.mainInventory[1] = stack;
                ItemStack bow = new ItemStack(net.minecraft.init.Items.bow);
                net.minecraft.init.Items.bow.onItemRightClick(bow, world, player);
                require(player.getItemInUse() == bow, "Bow charges with new ammunition " + definition.name); player.clearItemInUse();
            }
        }
        java.util.Arrays.fill(player.inventory.mainInventory, null); player.clearItemInUse();
        player.getFoodStats().setFoodLevel(20);
        net.minecraft.util.BlockPos pos = new net.minecraft.util.BlockPos(4, 64, 4);
        world.setBlockState(pos, net.minecraft.init.Blocks.skull.getDefaultState(), 0);
        net.minecraft.tileentity.TileEntitySkull skull = (net.minecraft.tileentity.TileEntitySkull)world.getTileEntity(pos); skull.setType(5);
        ItemStack picked = net.minecraft.init.Blocks.skull.getPickBlock(new net.minecraft.util.MovingObjectPosition(new net.minecraft.util.Vec3(4.5, 64.5, 4.5), net.minecraft.util.EnumFacing.UP, pos), world, pos, player);
        require(ClientItems.is(picked, LegacyItemCatalog.Kind.HEAD), "Middle-click picks imported dragon head"); world.setBlockToAir(pos);
    }
    private static void preview(Path directory) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        List<ItemStack> stacks = new ArrayList<>();
        for (LegacyItemCatalog.Definition definition : LegacyItemCatalog.ITEMS) {
            List<ItemStack> entries = variants(definition);
            if (!entries.isEmpty()) stacks.add(entries.get(Math.min(entries.size() - 1, 5)));
        }
        ItemStack broken = new ItemStack(net.minecraft.item.Item.getItemById(ClientItems.localItem(443, 0))); broken.setItemDamage(431); stacks.add(broken);
        ItemStack patterned = new ItemStack(net.minecraft.item.Item.getItemById(ClientItems.localItem(442, 0)));
        NBTTagCompound nbt = new NBTTagCompound(), banner = new NBTTagCompound(); banner.setInteger("Base", 4);
        NBTTagList patterns = new NBTTagList(); NBTTagCompound stripe = new NBTTagCompound(); stripe.setString("Pattern", "cs"); stripe.setInteger("Color", 1); patterns.appendTag(stripe); banner.setTag("Patterns", patterns); nbt.setTag("BlockEntityTag", banner); patterned.setTagCompound(nbt); stacks.add(patterned);
        int width = 1440, height = 850;
        Framebuffer target = new Framebuffer(width, height, true); target.setFramebufferColor(.08F, .09F, .12F, 1); target.framebufferClear(); target.bindFramebuffer(true);
        GlStateManager.matrixMode(5889); GlStateManager.pushMatrix(); GlStateManager.loadIdentity(); GlStateManager.ortho(0, width, height, 0, -1000, 1000);
        GlStateManager.matrixMode(5888); GlStateManager.pushMatrix(); GlStateManager.loadIdentity();
        try {
            RenderHelper.disableStandardItemLighting(); mc.fontRendererObj.drawString("Forge 1.8.9 | Minecraft 1.12.2 items | native inventory renderer", 20, 14, 0xffffff);
            for (int i = 0; i < stacks.size(); i++) {
                int x = (i % 6) * 240, y = 40 + (i / 6) * 160;
                GlStateManager.disableLighting(); GlStateManager.disableDepth(); Gui.drawRect(x + 8, y, x + 232, y + 152, 0xff252b35);
                mc.fontRendererObj.drawString(stacks.get(i).getDisplayName(), x + 15, y + 9, 0xffffff);
                GlStateManager.enableDepth(); GlStateManager.enableAlpha(); RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.pushMatrix(); GlStateManager.translate(x + 80, y + 46, 0); GlStateManager.scale(4, 4, 4);
                mc.getRenderItem().renderItemAndEffectIntoGUI(stacks.get(i), 0, 0); GlStateManager.popMatrix();
            }
            RenderHelper.disableStandardItemLighting(); Files.createDirectories(directory);
            ScreenShotHelper.saveScreenshot(directory.toFile(), "items-preview-1.12.2.png", width, height, target);
        } finally {
            GlStateManager.matrixMode(5888); GlStateManager.popMatrix(); GlStateManager.matrixMode(5889); GlStateManager.popMatrix(); GlStateManager.matrixMode(5888);
            target.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true); GlStateManager.color(1,1,1,1);
        }
    }
    private static void require(boolean condition, String label) { if (!condition) throw new AssertionError(label); }
}
