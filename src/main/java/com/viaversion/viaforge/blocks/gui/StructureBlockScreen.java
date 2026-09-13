package com.viaversion.viaforge.blocks.gui;

import com.viaversion.viaforge.blocks.EditorBlockEntity;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.network.play.client.C17PacketCustomPayload;

public final class StructureBlockScreen extends BlockEditorScreen {
    private static final String[] MODES = {"SAVE", "LOAD", "CORNER", "DATA"};
    private static final String[] MIRRORS = {"NONE", "LEFT_RIGHT", "FRONT_BACK"};
    private static final String[] ROTATIONS = {"NONE", "CLOCKWISE_90", "CLOCKWISE_180", "COUNTERCLOCKWISE_90"};
    public StructureBlockScreen(EditorBlockEntity tile) { super(tile); }
    @Override protected String title() { return tr("structure.title"); }
    @Override protected void build() {
        if (!draft.hasKey("mode")) draft.setString("mode", MODES[tile.getBlockMetadata() & 3]);
        if (!draft.hasKey("mirror")) draft.setString("mirror", "NONE");
        if (!draft.hasKey("rotation")) draft.setString("rotation", "NONE");
        if (!draft.hasKey("integrity")) draft.setFloat("integrity", 1);
        if (!draft.hasKey("posY")) draft.setInteger("posY", 1);
        if (!draft.hasKey("ignoreEntities")) draft.setBoolean("ignoreEntities", true);
        if (!draft.hasKey("showboundingbox")) draft.setBoolean("showboundingbox", true);
        int size = (span - 8) / 3;
        button(2, 0, 18, size, "");
        button(20, size + 4, 18, size, tr("structure.save"));
        button(21, size + 4, 18, size, tr("structure.load"));
        button(22, 2 * (size + 4), 18, size, tr("structure.detect"));
        field("name", tr("structure.name"), 0, 54, span, 64, draft.getString("name"));
        field("metadata", tr("structure.data"), 0, 54, span, 128, draft.getString("metadata"));
        for (int i = 0; i < 3; i++) {
            String axis = new String[]{"X", "Y", "Z"}[i];
            field("pos" + axis, tr("structure.offset", axis), i * (size + 4), 88, size, 11, Integer.toString(draft.getInteger("pos" + axis)));
            field("size" + axis, tr("structure.size", axis), i * (size + 4), 122, size, 11, Integer.toString(draft.getInteger("size" + axis)));
        }
        field("integrity", tr("structure.integrity"), 0, 122, size, 16, Float.toString(draft.getFloat("integrity")));
        field("seed", tr("structure.seed"), size + 4, 122, span - size - 4, 20, Long.toString(draft.getLong("seed")));
        button(3, 0, 146, span / 2 - 2, ""); button(4, span / 2 + 2, 146, span / 2 - 2, "");
        button(5, 0, 171, span / 2 - 2, ""); button(6, span / 2 + 2, 171, span / 2 - 2, "");
        button(7, span / 2 + 2, 171, span / 2 - 2, "");
    }
    @Override protected void refresh() {
        super.refresh();
        String mode = draft.getString("mode"); boolean save = mode.equals("SAVE"), load = mode.equals("LOAD");
        for (java.util.Map.Entry<String, GuiTextField> field : fields.entrySet()) {
            String name = field.getKey();
            field.getValue().setVisible(name.equals("name") ? !mode.equals("DATA") : name.equals("metadata") ? mode.equals("DATA")
                    : name.startsWith("pos") ? save || load : name.startsWith("size") ? save : load);
        }
        for (GuiButton button : buttonList) {
            if (button.id == 2) button.displayString = tr("structure." + mode);
            if (button.id == 3) { button.visible = load; button.displayString = tr("structure.mirror." + draft.getString("mirror")); }
            if (button.id == 4) { button.visible = load; button.displayString = tr("structure.rotation", index(ROTATIONS, draft.getString("rotation")) * 90); }
            if (button.id == 5) { button.visible = save || load; button.displayString = tr("structure.entities", on(!draft.getBoolean("ignoreEntities"))); }
            if (button.id == 6) { button.visible = save; button.displayString = tr("structure.air", on(draft.getBoolean("showair"))); }
            if (button.id == 7) { button.visible = load; button.displayString = tr("structure.bounds", on(draft.getBoolean("showboundingbox"))); }
            if (button.id == 20 || button.id == 22) button.visible = save;
            if (button.id == 21) button.visible = load;
        }
    }
    @Override protected void change(int id) {
        if (id == 2) cycle("mode", MODES);
        if (id == 3) cycle("mirror", MIRRORS);
        if (id == 4) cycle("rotation", ROTATIONS);
        if (id == 5) draft.setBoolean("ignoreEntities", !draft.getBoolean("ignoreEntities"));
        if (id == 6) draft.setBoolean("showair", !draft.getBoolean("showair"));
        if (id == 7) draft.setBoolean("showboundingbox", !draft.getBoolean("showboundingbox"));
    }
    private void cycle(String key, String[] choices) { draft.setString(key, choices[(index(choices, draft.getString(key)) + 1) % choices.length]); }
    private static int index(String[] choices, String value) { for (int i = 0; i < choices.length; i++) if (choices[i].equals(value)) return i; return 0; }
    @Override protected C17PacketCustomPayload packet(int action) {
        draft.setString("name", text("name")); draft.setString("metadata", text("metadata"));
        try {
            for (String key : new String[]{"posX", "posY", "posZ", "sizeX", "sizeY", "sizeZ"}) draft.setInteger(key, Integer.parseInt(text(key)));
            draft.setFloat("integrity", Float.parseFloat(text("integrity")));
            draft.setLong("seed", Long.parseLong(text("seed")));
        } catch (NumberFormatException invalid) { throw new IllegalArgumentException("numbers"); }
        return BlockEditorPackets.structure(tile.getPos(), action, draft);
    }
}
