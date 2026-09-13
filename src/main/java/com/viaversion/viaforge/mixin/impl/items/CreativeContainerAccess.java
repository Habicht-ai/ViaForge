package com.viaversion.viaforge.mixin.impl.items;

import java.util.List;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.gui.inventory.GuiContainerCreative$ContainerCreative")
public interface CreativeContainerAccess {
    @Accessor("itemList") List<ItemStack> viaForge$items();
    @Invoker("scrollTo") void viaForge$scrollTo(float scroll);
}
