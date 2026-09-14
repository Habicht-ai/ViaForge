package com.viaversion.viaforge.items;

import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog.*;
import com.viaversion.viaforge.common.blocks.LegacyItemDefinition;
import java.util.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.*;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Local registry ids are resolved only at the native edge of Via's pipeline. */
public final class ClientItems {
    private static final Map<Integer, Item> ITEMS = new HashMap<>();
    private static final Map<Integer, Definition> DEFINITIONS = new HashMap<>();
    public static void register() {
        ServerCombatModels.register();
        ServerEnchantments.register();
        ServerPotions.register();
        for (Definition definition : LegacyItemCatalog.ITEMS) {
            Item item = definition.kind == Kind.ELYTRA ? new ServerElytra(definition)
                    : definition.kind == Kind.ENCHANTED_BOOK ? new ServerEnchantedBook(definition)
                    : definition.kind == Kind.FOOD || definition.kind == Kind.SOUP ? new ServerFood(definition) : new ServerItem(definition);
            GameRegistry.registerItem(item, definition.name);
            ITEMS.put(definition.id, item); DEFINITIONS.put(Item.getIdFromItem(item), definition);
            ModelResourceLocation normal = new ModelResourceLocation("viaforge:" + definition.model, "inventory");
            if (definition.kind == Kind.ELYTRA || definition.kind == Kind.SHIELD) {
                ModelResourceLocation alternate = new ModelResourceLocation("viaforge:" + (definition.kind == Kind.ELYTRA ? "broken_elytra" : "shield_blocking"), "inventory");
                ModelResourceLocation left = new ModelResourceLocation("viaforge:shield_left", "inventory");
                ModelResourceLocation leftBlocking = new ModelResourceLocation("viaforge:shield_blocking_left", "inventory");
                if (definition.kind == Kind.SHIELD) net.minecraft.client.resources.model.ModelBakery.registerItemVariants(item, normal, alternate, left, leftBlocking);
                else net.minecraft.client.resources.model.ModelBakery.registerItemVariants(item, normal, alternate);
                ModelLoader.setCustomMeshDefinition(item, stack -> definition.kind == Kind.ELYTRA
                        ? (stack.getItemDamage() >= 431 ? alternate : normal)
                        : ServerHeldItemRenderer.left() ? (ServerHeldItemRenderer.blocking(stack) ? leftBlocking : left)
                        : (ServerHeldItemRenderer.blocking(stack) ? alternate : normal));
            } else ModelLoader.setCustomModelResourceLocation(item, 0, normal);
        }
    }
    public static int localItem(int id, int data) {
        int block = ClientBlocks.localItem(id, data);
        if (block >= 0) return block;
        Item item = ITEMS.get(id);
        if (item == null || id == 397 && data != 5) return -1;
        return Item.getIdFromItem(item);
    }
    public static LegacyItemDefinition serverItem(int id) {
        LegacyItemDefinition block = ClientBlocks.serverItem(id);
        return block != null ? block : DEFINITIONS.get(id);
    }
    public static Definition definition(ItemStack stack) { return stack == null ? null : DEFINITIONS.get(Item.getIdFromItem(stack.getItem())); }
    public static Item item(Definition definition) { return ITEMS.get(definition.id); }
    public static boolean is(ItemStack stack, Kind kind) { Definition definition = definition(stack); return definition != null && definition.kind == kind && ServerSession.supportsItem(definition); }
    public static boolean arrow(ItemStack stack) { return is(stack, Kind.ARROW) || is(stack, Kind.TIPPED_ARROW); }
    private ClientItems() { }
}
