package com.viaversion.viaforge.mixin.impl.hands;
import com.viaversion.viaforge.hands.Offhand;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiContainerCreative.class)
public abstract class MixinCreativeHands extends GuiContainer {
    protected MixinCreativeHands(Container c){super(c);}
    @Inject(method="initGui",at=@At("HEAD"))private void ensure(CallbackInfo ci){Offhand.ensure();}
    @Inject(method="setCurrentCreativeTab",at=@At("TAIL"))
    private void slot(CreativeTabs tab,CallbackInfo ci){
        if(Offhand.active()&&tab==CreativeTabs.tabInventory&&inventorySlots.inventorySlots.size()>45){
            Slot s=inventorySlots.getSlot(45);s.xDisplayPosition=35;s.yDisplayPosition=20;
            for(int id=5;id<9;id++){Slot armor=inventorySlots.getSlot(id);armor.xDisplayPosition=id<7?54:108;armor.yDisplayPosition=(id%2==1)?6:33;}
        }
    }
    @Inject(method="drawGuiContainerBackgroundLayer",at=@At("TAIL"))
    private void empty(float partial,int mouseX,int mouseY,CallbackInfo ci){if(Offhand.active()&&Offhand.get()==null&&((GuiContainerCreative)(Object)this).getSelectedTabIndex()==CreativeTabs.tabInventory.getTabIndex())com.viaversion.viaforge.hands.HandGui.emptySlot(guiLeft+35,guiTop+20);}
    @Redirect(method="drawGuiContainerBackgroundLayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private void background(net.minecraft.client.renderer.texture.TextureManager textures,net.minecraft.util.ResourceLocation resource){
        textures.bindTexture(Offhand.active()&&resource.getResourcePath().equals("textures/gui/container/creative_inventory/tab_inventory.png")?new net.minecraft.util.ResourceLocation("viaforge",resource.getResourcePath()):resource);
    }
    @Redirect(method="drawGuiContainerBackgroundLayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/inventory/GuiInventory;drawEntityOnScreen(IIIFFLnet/minecraft/entity/EntityLivingBase;)V"))
    private void player(int x,int y,int size,float yaw,float pitch,net.minecraft.entity.EntityLivingBase entity){
        if(com.viaversion.viaforge.items.InventoryEntityPreview.enabled())com.viaversion.viaforge.items.InventoryEntityPreview.draw(guiLeft+73,guiTop+6,guiLeft+105,guiTop+49,size,x-yaw,y-30-pitch,entity);
        else GuiInventory.drawEntityOnScreen(x+(Offhand.active()?45:0),y,size,yaw+(Offhand.active()?45:0),pitch,entity);
    }
}
