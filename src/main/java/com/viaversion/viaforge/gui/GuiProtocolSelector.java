/*
 * This file is part of ViaForge - https://github.com/ViaVersion/ViaForge
 * Copyright (C) 2021-2026 Florian Reuth <git@florianreuth.de> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.viaversion.viaforge.gui;

import com.mojang.realmsclient.gui.ChatFormatting;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.util.DumpUtil;
import com.viaversion.viaforge.common.ViaForgeCommon;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiProtocolSelector extends GuiScreen {

    private static final String ORIGINAL_MOD_CREDIT = "Original ViaForge by Florian Reuth (EnZaXD) and contributors";

    private final GuiScreen parent;
    private final boolean simple;
    private final FinishedCallback finishedCallback;
    private final boolean saveOnClose;
    private final boolean serverSpecific;
    private ProtocolVersion selection;

    private SlotList list;
    private List<String> creditLines;

    private String status;
    private long time;

    public GuiProtocolSelector(final GuiScreen parent) {
        this(parent, false, (version, unused) -> ViaForgeCommon.getManager().setTargetVersion(version), true);
    }

    public GuiProtocolSelector(final GuiScreen parent, final boolean simple, final FinishedCallback finishedCallback) {
        this(parent, simple, finishedCallback, false);
    }

    private GuiProtocolSelector(GuiScreen parent, boolean simple, FinishedCallback finishedCallback, boolean saveOnClose) {
        this(parent, simple, finishedCallback, saveOnClose, false, ViaForgeCommon.getManager().getTargetVersion());
    }

    /** A null override means this server follows the global selection. */
    public GuiProtocolSelector(GuiScreen parent, ProtocolVersion serverOverride, FinishedCallback finishedCallback) {
        this(parent, true, finishedCallback, false, true, serverOverride);
    }

    private GuiProtocolSelector(GuiScreen parent, boolean simple, FinishedCallback finishedCallback,
                                boolean saveOnClose, boolean serverSpecific, ProtocolVersion initialSelection) {
        this.parent = parent;
        this.simple = simple;
        this.finishedCallback = finishedCallback;
        this.saveOnClose = saveOnClose;
        this.serverSpecific = serverSpecific;
        this.selection = initialSelection;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new GuiButton(1, 5, height - 25, 20, 20, "<-"));
        if (serverSpecific) {
            buttonList.add(new GuiButton(4, 30, height - 25, Math.min(300, width - 35), 20,
                    "Use global: " + ViaForgeCommon.getManager().getTargetVersion().getName()));
        }
        if (!this.simple) {
            buttonList.add(new GuiButton(2, width - 105, 5, 100, 20, "Create dump"));
            buttonList.add(new GuiButton(3, width - 105, height - 25, 100, 20, "Reload configs"));
        }

        final int lineHeight = fontRendererObj.FONT_HEIGHT + 2;
        creditLines = fontRendererObj.listFormattedStringToWidth(ORIGINAL_MOD_CREDIT, Math.max(1, width - 10));
        final int listTop = 6 + lineHeight * (4 + creditLines.size());
        boolean firstOpen = list == null;
        int scroll = firstOpen ? 0 : list.getAmountScrolled();
        list = new SlotList(mc, width, height, listTop, height - 30, lineHeight);
        if (firstOpen) {
            ProtocolVersion effective = selection != null ? selection : ViaForgeCommon.getManager().getTargetVersion();
            int index = list.versions.indexOf(effective);
            scroll = Math.max(0, index * lineHeight - (height - 30 - listTop) / 2);
        }
        list.scrollBy(scroll);
    }

    @Override
    public void onGuiClosed() {
        // Config.set writes YAML synchronously. Persist once when leaving, not
        // on each row click (GuiSlot even reports a single press twice).
        if (saveOnClose && selection != ViaForgeCommon.getManager().getTargetVersion()) {
            finishedCallback.finished(selection, parent);
        }
        super.onGuiClosed();
    }

    public void setStatus(final String status) {
        this.status = status;
        this.time = System.currentTimeMillis();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        list.actionPerformed(button);

        if (button.id == 1) {
            mc.displayGuiScreen(parent);
        } else if (button.id == 2) {
            try {
                GuiScreen.setClipboardString(DumpUtil.postDump(UUID.randomUUID()).get());
                setStatus(ChatFormatting.GREEN + "Dump created and copied to clipboard");
            } catch (InterruptedException | ExecutionException e) {
                setStatus(ChatFormatting.RED + "Failed to create dump: " + e.getMessage());
            }
        } else if (button.id == 3) {
            Via.getManager().getConfigurationProvider().reloadConfigs();
        } else if (button.id == 4 && serverSpecific) {
            selection = null;
            finishedCallback.finished(null, parent);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        list.handleMouseInput();
        super.handleMouseInput();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (System.currentTimeMillis() - this.time >= 10_000) {
            this.status = null;
        }

        list.drawScreen(mouseX, mouseY, partialTicks);

        GL11.glPushMatrix();
        GL11.glScalef(2.0F, 2.0F, 2.0F);
        drawCenteredString(fontRendererObj, ChatFormatting.GOLD + "ViaForge", width / 4, 3, 16777215);
        GL11.glPopMatrix();

        drawCenteredString(fontRendererObj, "https://github.com/ViaVersion/ViaForge", width / 2, (fontRendererObj.FONT_HEIGHT + 2) * 2 + 3, -1);
        for (int i = 0; i < creditLines.size(); i++) {
            drawCenteredString(fontRendererObj, creditLines.get(i), width / 2, (fontRendererObj.FONT_HEIGHT + 2) * (3 + i) + 3, 0xAAAAAA);
        }
        String scope = serverSpecific ? "This server: " + (selection == null ? "Global default" : selection.getName())
                : "Global default (server overrides take priority)";
        drawCenteredString(fontRendererObj, scope, width / 2,
                (fontRendererObj.FONT_HEIGHT + 2) * (3 + creditLines.size()) + 3, 0xFFFF88);
        drawString(fontRendererObj, status != null ? status : "Discord: http://discord.gg/viaversion", 3, 3, -1);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    class SlotList extends GuiSlot {
        private final List<ProtocolVersion> versions = com.viaversion.viaforge.common.ProtocolSelection.versions();
        private long handledClick = Long.MIN_VALUE;

        public SlotList(Minecraft client, int width, int height, int top, int bottom, int slotHeight) {
            super(client, width, height, top, bottom, slotHeight);
        }

        @Override
        protected int getSize() {
            return versions.size();
        }

        @Override
        protected void elementClicked(int index, boolean b, int i1, int i2) {
            long event = Mouse.getEventNanoseconds();
            if (event == handledClick || index < 0 || index >= versions.size()) return;
            handledClick = event;
            selection = versions.get(index);
            if (!saveOnClose) finishedCallback.finished(selection, parent);
        }
        @Override
        public void handleMouseInput() {
            // GuiSlot normally uses coordinates saved by the previous frame.
            // Queued mouse events can otherwise choose a different row.
            mouseX = Mouse.getEventX() * GuiProtocolSelector.this.width / mc.displayWidth;
            mouseY = GuiProtocolSelector.this.height - Mouse.getEventY() * GuiProtocolSelector.this.height / mc.displayHeight - 1;
            super.handleMouseInput();
        }

        @Override
        protected boolean isSelected(int index) {
            return versions.get(index) == selection;
        }

        @Override
        protected void drawBackground() {
            drawDefaultBackground();
        }

        @Override
        protected void drawSlot(int index, int x, int y, int slotHeight, int mouseX, int mouseY) {
            // Native GuiSlot invokes drawSlot even for off-screen entries.
            if (y + slotHeight < top || y >= bottom) return;
            final ProtocolVersion targetVersion = selection;
            final ProtocolVersion version = versions.get(index);

            String color;
            if (targetVersion == version) {
                color = GuiProtocolSelector.this.simple ? ChatFormatting.GOLD.toString() : ChatFormatting.GREEN.toString();
            } else {
                color = GuiProtocolSelector.this.simple ? ChatFormatting.WHITE.toString() : ChatFormatting.DARK_RED.toString();
            }

            drawCenteredString(mc.fontRendererObj, (color) + version.getName(), width / 2, y, -1);
        }
    }

    public interface FinishedCallback {

        void finished(final ProtocolVersion version, final GuiScreen parent);

    }

}
