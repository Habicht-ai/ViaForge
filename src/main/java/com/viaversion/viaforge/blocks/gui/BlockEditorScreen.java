package com.viaversion.viaforge.blocks.gui;

import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.blocks.EditorBlockEntity;
import com.viaversion.viaforge.compatibility.ServerSession;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import org.lwjgl.input.Keyboard;

public abstract class BlockEditorScreen extends GuiScreen {
    protected final EditorBlockEntity tile;
    protected final Map<String, GuiTextField> fields = new LinkedHashMap<>();
    private final Map<String, String> labels = new LinkedHashMap<>();
    protected NBTTagCompound draft;
    protected int left, top, span;
    protected boolean ready, dirty;
    private String error = "";

    protected BlockEditorScreen(EditorBlockEntity tile) {
        this.tile = tile; ready = tile.received(); draft = tile.snapshot();
    }
    protected abstract void build();
    protected abstract String title();
    protected abstract C17PacketCustomPayload packet(int action);
    protected abstract void change(int id);

    @Override public final void initGui() {
        Map<String, String> previous = new LinkedHashMap<>();
        for (Map.Entry<String, GuiTextField> field : fields.entrySet()) previous.put(field.getKey(), field.getValue().getText());
        buttonList.clear(); fields.clear(); labels.clear();
        span = Math.min(360, width - 24); left = (width - span) / 2; top = Math.max(6, (height - 226) / 2);
        Keyboard.enableRepeatEvents(true);
        build();
        for (Map.Entry<String, String> field : previous.entrySet()) if (fields.containsKey(field.getKey())) fields.get(field.getKey()).setText(field.getValue());
        button(0, 0, 204, span / 2 - 4, I18n.format("gui.done"));
        button(1, span / 2 + 4, 204, span / 2 - 4, I18n.format("gui.cancel"));
        refresh();
    }
    protected GuiTextField field(String name, String label, int x, int y, int width, int max, String text) {
        GuiTextField field = new GuiTextField(fields.size(), fontRendererObj, left + x, top + y, width, 18);
        field.setMaxStringLength(max); field.setText(text);
        fields.put(name, field); labels.put(name, label);
        return field;
    }
    protected GuiButton button(int id, int x, int y, int width, String text) {
        GuiButton button = new GuiButton(id, left + x, top + y, width, 20, text); buttonList.add(button); return button;
    }
    protected String text(String name) { return fields.get(name).getText(); }
    protected static String tr(String key, Object... args) { return I18n.format("viaforge.editor." + key, args); }
    protected static String on(boolean value) { return I18n.format(value ? "options.on" : "options.off"); }
    protected void refresh() { for (GuiButton button : buttonList) if (button.id != 1) button.enabled = ready; }

    public final void serverUpdated(EditorBlockEntity updated) {
        if (updated != tile || dirty) return;
        draft = tile.snapshot(); ready = true; fields.clear(); initGui();
    }
    @Override protected final void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled || !button.visible) return;
        if (button.id == 1) { mc.displayGuiScreen(null); return; }
        if (button.id == 0 || button.id >= 20 && button.id <= 22) {
            try {
                C17PacketCustomPayload packet = packet(button.id == 0 ? 1 : button.id - 18);
                mc.thePlayer.sendQueue.addToSendQueue(packet);
                mc.displayGuiScreen(null);
            } catch (IllegalArgumentException invalid) { error = tr("invalid." + invalid.getMessage()); }
            return;
        }
        dirty = true; change(button.id); refresh();
    }
    @Override protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(null); return; }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            for (GuiButton button : buttonList) if (button.id == 0) actionPerformed(button);
            return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            java.util.List<GuiTextField> visible = new java.util.ArrayList<>();
            for (GuiTextField field : fields.values()) if (field.getVisible()) visible.add(field);
            int focus = -1;
            for (int i = 0; i < visible.size(); i++) if (visible.get(i).isFocused()) focus = i;
            for (GuiTextField field : visible) field.setFocused(false);
            if (!visible.isEmpty()) visible.get((focus + 1) % visible.size()).setFocused(true);
            return;
        }
        if (ready) for (GuiTextField field : fields.values()) if (field.getVisible() && field.textboxKeyTyped(typedChar, keyCode)) dirty = true;
        error = "";
    }
    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        for (GuiTextField field : fields.values()) if (field.getVisible()) field.mouseClicked(x, y, button);
    }
    @Override public void updateScreen() {
        if (mc.theWorld != tile.getWorld() || tile.isInvalid() || mc.thePlayer == null || !mc.thePlayer.capabilities.isCreativeMode
                || !ServerSession.supportsItem(ClientBlocks.definition(tile.getBlockType()))) { mc.displayGuiScreen(null); return; }
        for (GuiTextField field : fields.values()) field.updateCursorCounter();
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, title(), width / 2, top, 0xffffff);
        for (Map.Entry<String, GuiTextField> field : fields.entrySet()) if (field.getValue().getVisible()) {
            GuiTextField input = field.getValue();
            drawString(fontRendererObj, labels.get(field.getKey()), input.xPosition, input.yPosition - 10, 0xa0a0a0);
            input.drawTextBox();
        }
        String status = !ready ? tr("waiting") : error;
        if (!status.isEmpty()) drawCenteredString(fontRendererObj, status, width / 2, top + 193, 0xffb060);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
