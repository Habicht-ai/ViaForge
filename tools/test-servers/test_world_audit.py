import unittest
from unittest.mock import patch
import world_audit
import world_refine
import arena


class WorldAuditTests(unittest.TestCase):
    def test_palette_packing_before_and_after_116(self):
        palette = [{"Name": "block_" + str(i)} for i in range(19)]
        for padded in [False, True]:
            data = [0] * (342 if padded else 320)
            for pos in range(4096):
                value = pos % len(palette)
                word, shift = (pos // 12, pos % 12 * 5) if padded else divmod(pos * 5, 64)
                data[word] |= value << shift & ((1 << 64) - 1)
                if not padded and shift + 5 > 64:
                    data[word + 1] |= value >> (64 - shift)
            world = world_audit.World.__new__(world_audit.World)
            world.chunk = lambda *_: {"_data_version": 2566 if padded else 2230,
                                     "Sections": [{"Y": 4, "Palette": palette, "BlockStates": data}]}
            for pos in [0, 11, 12, 13, 63, 1023, 4095]:
                self.assertEqual(palette[pos % 19], world.block(pos % 16, 64 + pos // 256, pos // 16 % 16))

    def test_legacy_metadata_nibbles(self):
        world = world_audit.World.__new__(world_audit.World)
        world.chunk = lambda *_: {"Sections": [{"Y": 4, "Blocks": b"\x32" * 4096, "Data": b"\x45" * 2048}]}
        self.assertEqual({"legacy_id": 50, "metadata": 5}, world.block(0, 64, 0))
        self.assertEqual({"legacy_id": 50, "metadata": 4}, world.block(1, 64, 0))

    def test_waterlogged_exhibit_gets_closed_tank_before_repair(self):
        with patch.object(arena.lab, "catalog", return_value={"blocks": [], "items": [], "mobs": []}):
            a = arena.Arena({"version": "26.2", "protocol": 776})
        sample = dict(name="minecraft:brain_coral_wall_fan", state="minecraft:brain_coral_wall_fan[facing=north,waterlogged=true]", position=[0, 64, 0], metadata=0)
        world_refine.repair(a, sample)
        self.assertIn("fill -1 63 -1 1 63 1 glass", a.commands)
        self.assertIn("fill -1 64 0 -1 66 0 glass", a.commands)
        self.assertIn("setblock 0 64 1 stone", a.commands)
        self.assertEqual("setblock 0 64 0 " + sample["state"], a.commands[-1])

    def test_modern_sign_faces_arriving_player(self):
        with patch.object(arena.lab, "catalog", return_value={"blocks": [], "items": [], "mobs": []}):
            a = arena.Arena({"version": "26.2", "protocol": 776})
        a.sign(0, 64, 0, ["Test"])
        self.assertIn("oak_sign[rotation=8]", a.commands[-1])


if __name__ == "__main__":
    unittest.main()
