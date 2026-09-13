package com.viaversion.viaforge.development;

import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaforge.items.ClientItems;
import java.util.*;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.*;

final class CreativeCategorySmokeTest {
    private static Map<String, List<String>> nativeOrder;
    static void disconnected() {
        Map<String, List<String>> actual = new HashMap<>();
        for (CreativeTabs tab : CreativeTabs.creativeTabArray) {
            if (tab == null || tab == CreativeTabs.tabInventory || tab == CreativeTabs.tabAllSearch) continue;
            List<ItemStack> stacks = new ArrayList<>(); tab.displayAllReleventItems(stacks);
            actual.put(tab.getTabLabel(), identities(stacks));
        }
        if (nativeOrder == null) nativeOrder = actual;
        else ServerEntitySmokeTest.require(nativeOrder.equals(actual), "Original creative ordering restored on disconnect");
    }
    static void verify(BlockVersionProfile profile) {
        Map<CreativeTabs, List<ItemStack>> contents = new HashMap<>();
        for (CreativeTabs tab : CreativeTabs.creativeTabArray) {
            if (tab == null || tab == CreativeTabs.tabInventory || tab == CreativeTabs.tabAllSearch) continue;
            List<ItemStack> entries = new ArrayList<>(); tab.displayAllReleventItems(entries); contents.put(tab, entries);
        }
        CreativeTabs materials = profile.protocol() >= 335 ? CreativeTabs.tabMisc : CreativeTabs.tabMaterials;
        for (LegacyItemCatalog.Definition definition : LegacyItemCatalog.ITEMS) {
            if (definition.id == 403) continue;
            CreativeTabs expected;
            switch (definition.id) {
                case 397: case 426: expected = CreativeTabs.tabDecorations; break;
                case 373: case 437: case 438: case 441: expected = CreativeTabs.tabBrewing; break;
                case 439: case 440: case 442: case 449: expected = CreativeTabs.tabCombat; break;
                case 432: case 434: case 436: expected = CreativeTabs.tabFood; break;
                case 433: case 450: case 452: expected = materials; break;
                case 453: expected = null; break;
                case 383: expected = CreativeTabs.tabMisc; break;
                default: expected = CreativeTabs.tabTransport;
            }
            if (profile.protocol() < definition.protocol) expected = null;
            membership(contents, ClientItems.item(definition), expected, definition.name);
        }
        for (LegacyBlockCatalog.Definition definition : LegacyBlockCatalog.BLOCKS) {
            if (definition.itemId() < 0) continue;
            CreativeTabs expected;
            switch (definition.id) {
                case 198: case 199: case 200: case 26: expected = CreativeTabs.tabDecorations; break;
                case 207: expected = materials; break;
                case 218: expected = CreativeTabs.tabRedstone; break;
                case 137: case 208: case 210: case 211: case 217: case 255: expected = null; break;
                default: expected = definition.id >= 219 && definition.id <= 250 ? CreativeTabs.tabDecorations : CreativeTabs.tabBlock;
            }
            if (profile.protocol() < definition.itemProtocol()) expected = null;
            membership(contents, Item.getItemById(ClientItems.localItem(definition.itemId(), definition.itemData())), expected, definition.name);
        }
        membership(contents, Items.iron_ingot, materials, "existing iron ingot");
        verifyOrder(profile, contents);
        for (CreativeTabs tab : contents.keySet()) {
            Map<Integer, Integer> books = new HashMap<>();
            for (ItemStack stack : contents.get(tab)) {
                LegacyItemDefinition definition = ClientItems.serverItem(Item.getIdFromItem(stack.getItem()));
                if (definition == null || definition.itemId() != 403) continue;
                net.minecraft.nbt.NBTTagList stored = stack.getTagCompound().getTagList("StoredEnchantments", 10);
                for (int i = 0; i < stored.tagCount(); i++) {
                    net.minecraft.nbt.NBTTagCompound enchantment = stored.getCompoundTagAt(i);
                    ServerEntitySmokeTest.require(books.put((int)enchantment.getShort("id"), (int)enchantment.getShort("lvl")) == null, "No duplicate enchantment levels in a category");
                }
            }
            Map<Integer, Integer> expected = new HashMap<>();
            if (tab == CreativeTabs.tabCombat) { expected.put(9, 2); if (profile.protocol() >= 315) { expected.put(70, 1); expected.put(10, 1); expected.put(71, 1); } }
            if (tab == CreativeTabs.tabTools) { expected.put(70, 1); if (profile.protocol() >= 315) expected.put(71, 1); }
            if (tab == CreativeTabs.tabCombat && profile.protocol() >= 316) expected.put(22, 3);
            ServerEntitySmokeTest.require(books.equals(expected), "Target enchantment category " + tab.getTabLabel() + ": " + books + " expected " + expected);
        }
    }
    private static void verifyOrder(BlockVersionProfile profile, Map<CreativeTabs, List<ItemStack>> contents) {
        List<ItemStack> decorations = contents.get(CreativeTabs.tabDecorations);
        int start = -1;
        for (int i = 0; i < decorations.size(); i++) if (identity(decorations.get(i)).equals("198:0")) { start = i; break; }
        ServerEntitySmokeTest.require(start >= 0, "End rods occur before the standalone decoration items");
        List<String> expected = new ArrayList<>(Arrays.asList("198:0", "199:0", "200:0"));
        if (profile.protocol() >= 315) for (int id = 219; id <= 234; id++) expected.add(id + ":0");
        if (profile.protocol() >= 335) for (int id = 235; id <= 250; id++) expected.add(id + ":0");
        expected.add("321:0"); expected.add("323:0"); // painting, sign, then beds
        if (profile.protocol() >= 335) for (int color = 0; color < 16; color++) expected.add("355:" + color);
        else expected.add("355:0");
        expected.add("389:0"); expected.add("390:0"); // item frame and flower pot
        for (int skull = 0; skull < 6; skull++) expected.add("397:" + skull);
        expected.add("416:0");
        for (int color = 15; color >= 0; color--) expected.add("425:" + color);
        expected.add("426:0");
        List<String> actual = identities(decorations.subList(start, decorations.size()));
        ServerEntitySmokeTest.require(actual.equals(expected), "Vanilla decoration sequence " + profile + ": " + actual + " expected " + expected);
        for (CreativeTabs tab : new CreativeTabs[]{CreativeTabs.tabCombat, CreativeTabs.tabTools}) {
            int lastBook = -1, lastBookIndex = -1;
            List<ItemStack> entries = contents.get(tab);
            for (int i = 0; i < entries.size(); i++) {
                ItemStack stack = entries.get(i);
                if (!identity(stack).startsWith("403:")) continue;
                int id = stack.getTagCompound().getTagList("StoredEnchantments", 10).getCompoundTagAt(0).getShort("id");
                ServerEntitySmokeTest.require(id > lastBook, "Old and new enchantment books share native registry order");
                if (lastBookIndex >= 0) ServerEntitySmokeTest.require(i == lastBookIndex + 1, "Enchanted books form a contiguous group");
                lastBook = id; lastBookIndex = i;
            }
            if (tab == CreativeTabs.tabCombat) {
                int shield = identities(entries).indexOf("442:0");
                ServerEntitySmokeTest.require(profile.protocol() >= 335 ? lastBookIndex < shield : lastBookIndex > shield,
                        "1.12 book-order transition relative to new combat items");
            }
        }
    }
    static String identity(ItemStack stack) {
        int id = Item.getIdFromItem(stack.getItem()), data = stack.getMetadata();
        LegacyItemDefinition definition = ClientItems.serverItem(id);
        if (definition != null) { id = definition.itemId(); data = definition.preservesDamage() ? data : definition.itemData(); }
        return id + ":" + data;
    }
    static List<String> identities(List<ItemStack> stacks) {
        List<String> result = new ArrayList<>();
        for (ItemStack stack : stacks) result.add(identity(stack));
        return result;
    }
    private static void membership(Map<CreativeTabs, List<ItemStack>> contents, Item item, CreativeTabs expected, String label) {
        for (Map.Entry<CreativeTabs, List<ItemStack>> entry : contents.entrySet()) {
            boolean found = false; for (ItemStack stack : entry.getValue()) if (stack.getItem() == item) found = true;
            ServerEntitySmokeTest.require(found == (entry.getKey() == expected), "Creative category " + label + " in " + entry.getKey().getTabLabel());
        }
        if (expected == null) {
            LegacyItemDefinition definition = ClientItems.serverItem(Item.getIdFromItem(item));
            if (definition != null && com.viaversion.viaforge.blocks.ServerBlockSession.supportsItem(definition)) ServerEntitySmokeTest.require(item.getCreativeTab() == null, "Unlisted item hidden in creative search: " + label);
        }
    }
}
