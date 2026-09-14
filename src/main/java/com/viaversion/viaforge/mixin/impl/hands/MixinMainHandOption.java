package com.viaversion.viaforge.mixin.impl.hands;

import com.viaversion.viaforge.common.ViaForgeCommon;
import com.viaversion.viaforge.common.platform.ViaForgeConfig;
import com.viaversion.viaforge.hands.Offhand;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiCustomizeSkin.class)
public abstract class MixinMainHandOption extends GuiScreen {
    private static String label(){return I18n.format("options.viaforge.mainHand")+": "+I18n.format(ViaForgeCommon.getManager().getConfig().isLeftMainHand()?"options.viaforge.left":"options.viaforge.right");}
    @Inject(method="initGui",at=@At("TAIL"))
    private void option(CallbackInfo ci){if(Offhand.active())buttonList.add(new GuiButton(4190,width/2+5,height/6+72,150,20,label()));}
    @Inject(method="actionPerformed",at=@At("HEAD"),cancellable=true)
    private void toggle(GuiButton button,CallbackInfo ci){
        if(button.id!=4190||!button.enabled)return;
        ViaForgeConfig config=ViaForgeCommon.getManager().getConfig();config.set(ViaForgeConfig.LEFT_MAIN_HAND,!config.isLeftMainHand());
        button.displayString=label();mc.gameSettings.sendSettingsToServer();ci.cancel();
    }
}
