package com.viaversion.viaforge.mixin.impl.hands;
import com.viaversion.viaforge.hands.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.*;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiIngame.class)
public abstract class MixinHandHud extends Gui {
    @Inject(method="renderTooltip",at=@At("RETURN"))
    private void offhand(ScaledResolution resolution,float partial,CallbackInfo ci){
        ItemStack stack=Offhand.get();if(stack==null||Minecraft.getMinecraft().thePlayer.isSpectator())return;
        Minecraft mc=Minecraft.getMinecraft();boolean right=Offhand.mainLeft();int x=resolution.getScaledWidth()/2+(right?91:-120),y=resolution.getScaledHeight()-23;
        mc.getTextureManager().bindTexture(new net.minecraft.util.ResourceLocation("viaforge:textures/gui/widgets.png"));
        GlStateManager.color(1,1,1,1);GlStateManager.enableBlend();drawTexturedModalRect(x,y,right?53:24,22,29,24);
        RenderHelper.enableGUIStandardItemLighting();int itemX=x+(right?10:3);mc.getRenderItem().renderItemAndEffectIntoGUI(stack,itemX,y+4);mc.getRenderItem().renderItemOverlays(mc.fontRendererObj,stack,itemX,y+4);
        RenderHelper.disableStandardItemLighting();GlStateManager.disableBlend();
    }
}
