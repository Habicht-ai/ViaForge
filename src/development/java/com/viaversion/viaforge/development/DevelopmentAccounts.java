package com.viaversion.viaforge.development;

import com.viaversion.viaforge.account.AccountManager;
import com.viaversion.viaforge.gui.account.AccountManagerGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Login support packaged only in the development JAR used by runClient. */
@Mod(modid = "viaforge_dev_accounts", name = "ViaForge Development Accounts", version = "1.0",
        dependencies = "required-after:viaforge", acceptableRemoteVersions = "*", clientSideOnly = true)
public final class DevelopmentAccounts {
    private static final int ACCOUNTS_BUTTON_ID = 1_000_000_001;
    private AccountManager accounts;

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        if (ProtocolConnectionSmokeTest.installIfRequested()) return;
        if (LiveFlightSmokeTest.installIfRequested()) return;
        if (BlockClientSmokeTest.installIfRequested()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        accounts = new AccountManager(minecraft, minecraft.mcDataDir.toPath().resolve("ViaForge/accounts"));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiMainMenu) {
            event.buttonList.add(new GuiButton(ACCOUNTS_BUTTON_ID, event.gui.width - 105, 5, 100, 20, "Accounts"));
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.gui instanceof GuiMainMenu && event.button.id == ACCOUNTS_BUTTON_ID) {
            Minecraft.getMinecraft().displayGuiScreen(new AccountManagerGui(event.gui, accounts));
            event.setCanceled(true);
        }
    }
}
