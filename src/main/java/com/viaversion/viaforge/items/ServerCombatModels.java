package com.viaversion.viaforge.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.*;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;

/** Target sword/tool display transforms while retaining their original native item IDs. */
public final class ServerCombatModels {
    private static final Map<Item, ModelResourceLocation> MODELS = new HashMap<>();
    public static void register() {
        for (String material : new String[]{"wooden", "stone", "iron", "diamond", "golden"}) {
            for (String tool : new String[]{"sword", "axe", "pickaxe", "shovel", "hoe"}) {
                String name = material + "_" + tool; Item item = Item.itemRegistry.getObject(new ResourceLocation(name));
                ModelResourceLocation model = new ModelResourceLocation("viaforge:combat/" + name, "inventory");
                ModelBakery.registerItemVariants(item, new ModelResourceLocation(name, "inventory"), model);
                MODELS.put(item, model);
            }
        }
    }
    public static boolean active(ItemStack stack) { return stack != null && ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.COMBAT) && MODELS.containsKey(stack.getItem()); }
    public static boolean imported(ItemStack stack) { return stack != null && (com.viaversion.viaforge.hands.HandModels.current==stack || ClientItems.serverItem(Item.getIdFromItem(stack.getItem())) != null || active(stack)); }
    public static IBakedModel model(ItemStack stack) { return Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getModelManager().getModel(MODELS.get(stack.getItem())); }
    private ServerCombatModels() { }
}
