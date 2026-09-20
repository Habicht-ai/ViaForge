package com.viaversion.viaforge.mixin.impl.hands;
import com.viaversion.viaforge.hands.Offhand;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiInventory.class)
public abstract class MixinHandInventory extends net.minecraft.client.gui.inventory.GuiContainer {
    protected MixinHandInventory(net.minecraft.inventory.Container container){super(container);}
    @Inject(method="drawGuiContainerBackgroundLayer",at=@At("TAIL"))
    private void empty(float partial,int mouseX,int mouseY,CallbackInfo ci){if(Offhand.active()&&Offhand.get()==null)com.viaversion.viaforge.hands.HandGui.emptySlot(guiLeft+77,guiTop+62);}
    @Inject(method="initGui",at=@At("HEAD")) private void slot(CallbackInfo ci){Offhand.ensure();}
    @Inject(method="drawGuiContainerForegroundLayer",at=@At("HEAD"),cancellable=true)
    private void label(int mouseX,int mouseY,CallbackInfo ci){if(Offhand.active()){fontRendererObj.drawString(net.minecraft.client.resources.I18n.format("container.crafting"),97,8,0x404040);ci.cancel();}}
    @Redirect(method="drawGuiContainerBackgroundLayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private void background(net.minecraft.client.renderer.texture.TextureManager textures,net.minecraft.util.ResourceLocation original){textures.bindTexture(Offhand.active()?new net.minecraft.util.ResourceLocation("viaforge:textures/gui/container/inventory.png"):original);}
    @Redirect(method="drawGuiContainerBackgroundLayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/inventory/GuiInventory;drawEntityOnScreen(IIIFFLnet/minecraft/entity/EntityLivingBase;)V"))
    private void player(int x,int y,int size,float mouseX,float mouseY,net.minecraft.entity.EntityLivingBase entity){
        if(com.viaversion.viaforge.items.InventoryEntityPreview.enabled())com.viaversion.viaforge.items.InventoryEntityPreview.draw(guiLeft+26,guiTop+8,guiLeft+75,guiTop+78,size,x-mouseX,y-50-mouseY,entity);
        else GuiInventory.drawEntityOnScreen(x,y,size,mouseX,mouseY,entity);
    }
}
