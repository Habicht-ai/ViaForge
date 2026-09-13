package com.viaversion.viaforge.items;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.common.blocks.LegacyItemDefinition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

/** Vanilla iterates its numeric registry; Forge assigns our representations different IDs. */
public final class ServerCreativeOrder {
    public static void sortSearch(net.minecraft.inventory.Container inventory) {
        com.viaversion.viaforge.mixin.impl.items.CreativeContainerAccess container =
                (com.viaversion.viaforge.mixin.impl.items.CreativeContainerAccess)inventory;
        sort(container.viaForge$items());
        container.viaForge$scrollTo(0);
    }
    public static void sort(List<ItemStack> entries) {
        if (!ServerBlockSession.supportsProtocol(107) || entries.size() < 2) return;
        List<ItemStack> vanilla = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            ItemStack stack = entries.get(i);
            if (serverId(stack) < 0) continue;
            vanilla.add(stack); positions.add(i);
        }
        boolean booksAtEnd = !ServerBlockSession.supportsProtocol(335);
        vanilla.sort((a, b) -> {
            int first = serverId(a), second = serverId(b);
            // Through 1.11.2, tabs/search append books after the item registry.
            int order = Integer.compare(booksAtEnd && first == 403 ? Integer.MAX_VALUE : first,
                    booksAtEnd && second == 403 ? Integer.MAX_VALUE : second);
            if (order != 0) return order;
            if (first == 403) return Integer.compare(enchantment(a), enchantment(b));
            if (first == 355 || first == 251 || first == 252) return Integer.compare(serverData(a), serverData(b));
            // Preserve each native item's sub-item order, e.g. banner dye metadata
            // runs in the opposite direction to beds. Stable sorting also retains NBT variants.
            return 0;
        });
        // Items contributed by other mods keep their positions and relative order.
        for (int i = 0; i < positions.size(); i++) entries.set(positions.get(i), vanilla.get(i));
    }
    private static int serverId(ItemStack stack) {
        if (stack == null) return -1;
        Item item = stack.getItem();
        LegacyItemDefinition definition = ClientItems.serverItem(Item.getIdFromItem(item));
        if (definition != null) return ServerBlockSession.supportsItem(definition) ? definition.itemId() : -1;
        ResourceLocation name = Item.itemRegistry.getNameForObject(item);
        return name != null && name.getResourceDomain().equals("minecraft") ? Item.getIdFromItem(item) : -1;
    }
    private static int serverData(ItemStack stack) {
        LegacyItemDefinition definition = ClientItems.serverItem(Item.getIdFromItem(stack.getItem()));
        return definition == null ? stack.getMetadata() : definition.itemData();
    }
    private static int enchantment(ItemStack stack) {
        if (!stack.hasTagCompound()) return Integer.MAX_VALUE;
        NBTTagList stored = stack.getTagCompound().getTagList("StoredEnchantments", 10);
        if (stored.tagCount() != 1) return Integer.MAX_VALUE;
        return (stored.getCompoundTagAt(0).getShort("id") & 65535) << 16
                | (stored.getCompoundTagAt(0).getShort("lvl") & 65535);
    }
    private ServerCreativeOrder() { }
}
