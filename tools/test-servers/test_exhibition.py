import json
import unittest
from unittest.mock import patch

import arena
import exhibition
import inventory_catalog
import lab
import world_audit


class ExhibitionTests(unittest.TestCase):
    def test_original_creative_stacks_never_use_placement_metadata(self):
        for version in ["1.9", "1.12.2"]:
            row = lab.select(version)[0]
            entries = inventory_catalog.stacks(row, lab.catalog(row))
            def damage(name):
                return {e["metadata"] for e in entries if e["name"] == "minecraft:" + name}
            self.assertEqual({0}, damage("purpur_stairs"))
            self.assertEqual({0}, damage("furnace"))
            self.assertEqual(set(range(4)), damage("log"))
            self.assertEqual(set(range(16)), damage("wool"))
            self.assertEqual(set(range(16)) if version == "1.12.2" else {0}, damage("bed"))
            eggs = [e for e in entries if e["name"] == "minecraft:spawn_egg"]
            self.assertGreater(len(eggs), 20)
            self.assertTrue(all("EntityTag" in e["snbt"] for e in eggs))
            self.assertFalse(any('"minecraft:iron_golem"' in e["snbt"] or '"VillagerGolem"' in e["snbt"] for e in eggs))
            self.assertTrue(any("Potion:" in e["snbt"] for e in entries))

    def test_modern_inventory_uses_only_item_registry_and_omits_operator_tools(self):
        row = lab.select("26.2")[0]
        entries = inventory_catalog.stacks(row, lab.catalog(row))
        names = {e["name"] for e in entries}
        self.assertIn("minecraft:purpur_stairs", names)
        self.assertIn("minecraft:shulker_spawn_egg", names)
        for name in ["water", "piston_head", "oak_wall_sign", "air", "command_block", "debug_stick"]:
            self.assertNotIn("minecraft:" + name, names)

    def test_control_labels_have_support_before_placement(self):
        with patch.object(lab, "catalog", return_value={"blocks": [], "items": []}):
            a = arena.Arena({"protocol": 340, "version": "test"})
        a.button(0, 64, 0, ["Label"], "tp @p 0 65 0")
        self.assertEqual("setblock 0 64 -1 quartz_block 0 replace ", a.commands[0])
        self.assertEqual([0, 65, -1], a.signs[(0, 65, -1)]["position"])

    def test_restoration_removes_old_guards_and_never_forces_player_modes(self):
        for version in ["1.9", "1.12.2", "1.13", "26.2"]:
            a = arena.Arena(lab.select(version)[0])
            checks = exhibition.free_play(a)
            self.assertEqual(4, len(checks))
            self.assertTrue(all(f"setblock {x} 60 1 air" in "\n".join(a.commands) for x in range(1, 5)))
            self.assertFalse(any("repeating_command_block" in c or "gamemode " in c and "@" in c for c in a.commands))
            self.assertIn("defaultgamemode " + ("1" if a.legacy else "creative"), a.commands)

    def test_saved_sign_check_catches_removed_support_text_and_block(self):
        class FakeWorld:
            block_value = {"Name": "minecraft:oak_sign"}
            below = {"Name": "minecraft:stone"}
            message = '{"text":"Test"}'
            def block(self, x, y, z):
                return self.block_value if y == 64 else self.below
            def block_entity(self, *args):
                return {"front_text": {"messages": [self.message, "", "", ""]}}
        world = FakeWorld()
        manifest = [dict(position=[0, 64, 0], lines=["Test", "", "", ""])]
        self.assertEqual([], world_audit.audit_signs(world, manifest))
        world.message = {"text": "Test"}
        self.assertEqual([], world_audit.audit_signs(world, manifest))
        world.below = {"Name": "minecraft:air"}
        self.assertEqual("unsupported_sign", world_audit.audit_signs(world, manifest)[0]["error"])
        world.message = "Wrong"
        self.assertEqual("sign_text", world_audit.audit_signs(world, manifest)[0]["error"])
        world.block_value = {"Name": "minecraft:air"}
        self.assertEqual("missing_sign", world_audit.audit_signs(world, manifest)[0]["error"])

    def test_restore_refuses_running_world(self):
        with patch.object(lab, "status", return_value={"version": {"protocol": 776}}), \
                patch.object(exhibition.shutil, "copytree") as backup:
            with self.assertRaisesRegex(RuntimeError, "stoppen"):
                exhibition.maintain(lab.select("26.2")[0], restore=True)
            backup.assert_not_called()


if __name__ == "__main__":
    unittest.main()
