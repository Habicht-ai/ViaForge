package com.viaversion.viaforge.blocks.gui;

import com.viaversion.viaforge.blocks.EditorBlockEntity;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.IChatComponent;

public final class CommandBlockScreen extends BlockEditorScreen {
    private String mode;
    private boolean conditional, automatic, trackOutput;

    public CommandBlockScreen(EditorBlockEntity tile) { super(tile); }
    @Override protected String title() { return tr("command.title"); }
    @Override protected void build() {
        mode = draft.hasKey("mode") ? draft.getString("mode") : tile.serverBlockId() == 210 ? "AUTO" : tile.serverBlockId() == 211 ? "SEQUENCE" : "REDSTONE";
        conditional = draft.hasKey("conditional") ? draft.getBoolean("conditional") : (tile.getBlockMetadata() & 8) != 0;
        automatic = draft.getBoolean("auto"); trackOutput = !draft.hasKey("TrackOutput") || draft.getBoolean("TrackOutput");
        field("command", tr("command.input"), 0, 40, span, 32500, draft.getString("Command")).setFocused(true);
        String output = draft.getString("LastOutput");
        try { IChatComponent component = IChatComponent.Serializer.jsonToComponent(output); if (component != null) output = component.getUnformattedText(); } catch (Exception ignored) { }
        GuiTextField previous = field("output", tr("command.output"), 0, 110, span, 32767, output);
        previous.setEnabled(false);
        int size = (span - 8) / 3;
        button(2, 0, 153, size, ""); button(3, size + 4, 153, size, ""); button(4, 2 * (size + 4), 153, size, "");
        button(5, 0, 72, span, "");
    }
    @Override protected void refresh() {
        super.refresh();
        for (GuiButton button : buttonList) {
            if (button.id == 2) button.displayString = tr("command." + mode);
            if (button.id == 3) button.displayString = tr(conditional ? "command.conditional" : "command.unconditional");
            if (button.id == 4) button.displayString = tr(automatic ? "command.always" : "command.redstone");
            if (button.id == 5) button.displayString = tr("command.track", on(trackOutput));
        }
    }
    @Override protected void change(int id) {
        if (id == 2) mode = mode.equals("REDSTONE") ? "SEQUENCE" : mode.equals("SEQUENCE") ? "AUTO" : "REDSTONE";
        if (id == 3) conditional = !conditional;
        if (id == 4) automatic = !automatic;
        if (id == 5) trackOutput = !trackOutput;
        draft.setString("mode", mode); draft.setBoolean("conditional", conditional); draft.setBoolean("auto", automatic); draft.setBoolean("TrackOutput", trackOutput);
    }
    @Override protected C17PacketCustomPayload packet(int action) {
        draft.setString("Command", text("command")); draft.setString("mode", mode);
        draft.setBoolean("conditional", conditional); draft.setBoolean("auto", automatic); draft.setBoolean("TrackOutput", trackOutput);
        return BlockEditorPackets.command(tile.getPos(), draft);
    }
}
