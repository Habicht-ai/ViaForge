package com.viaversion.viaforge.mixin.impl.hands;
import java.util.Map;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ItemModelMesher.class)
public interface ItemShapes {
    @Accessor("simpleShapes") Map<Integer,ModelResourceLocation> viaForge$shapes();
    @Accessor("shapers") Map<Item,ItemMeshDefinition> viaForge$meshes();
}
