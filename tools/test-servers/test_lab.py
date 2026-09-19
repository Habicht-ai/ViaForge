import importlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import arena
import lab
import server_list


class TestLab(unittest.TestCase):
    def test_manifest_exactly_matches_registered_resource_versions(self):
        resources = json.loads((lab.REPO / "src/main/resources/assets/viaforge/block-versions.json").read_text())
        self.assertEqual(set(resources), {r["version"] for r in lab.VERSIONS})
        self.assertEqual(48, len(lab.VERSIONS))
        self.assertEqual(48, len({r["protocol"] for r in lab.VERSIONS}))
        self.assertEqual(96, len({r[k] for r in lab.VERSIONS for k in ["port", "rcon_port"]}))
        for row in lab.VERSIONS:
            self.assertTrue(row["server"]["url"].startswith("https://piston-data.mojang.com/"))
            self.assertEqual(40, len(row["server"]["sha1"]))

    def test_setup_does_not_accept_terms_or_overwrite_world_configuration(self):
        row = lab.select("26.2")[0]
        with tempfile.TemporaryDirectory() as tmp, patch.object(lab, "ROOT", Path(tmp)):
            lab.properties(row)
            directory = Path(tmp) / row["version"]
            self.assertIn("eula=false", (directory / "eula.txt").read_text())
            self.assertIn("view-distance=16", (directory / "server.properties").read_text())
            lab.assert_local(row)
            before = (directory / "control.json").read_text()
            (directory / "server.properties").write_text("custom=true\n")
            lab.properties(row)
            self.assertEqual("custom=true\n", (directory / "server.properties").read_text())
            self.assertEqual(before, (directory / "control.json").read_text())
            with self.assertRaises(RuntimeError):
                lab.assert_local(row)

    def test_partial_download_never_replaces_verified_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / "server.jar"
            target.write_bytes(b"existing")
            with patch("urllib.request.urlopen", side_effect=OSError("offline")), patch("time.sleep"):
                with self.assertRaises(OSError):
                    lab.download("https://example.invalid", target, "0" * 40)
            self.assertEqual(b"existing", target.read_bytes())

    def test_bad_groups_rejected(self):
        self.assertEqual(4, len(lab.select("regression")))
        self.assertEqual(2, len(lab.select("1.12.2,26.2")))
        with self.assertRaises(ValueError):
            lab.select("../../world")
        with self.assertRaises(ValueError):
            lab.select("1.99")

    def test_varints(self):
        self.assertEqual(b"\x80\x06", lab.varint(768))
        self.assertEqual(b"\xff\xff\xff\xff\x0f", lab.varint(-1))

    def test_all48_fail_memory_check_without_starting_processes(self):
        with patch.object(lab, "status", return_value=None), patch.object(lab, "assert_local"), patch.object(lab, "recover_stale_worker"), \
                patch.object(Path, "read_text", return_value="eula=true"), \
                patch.object(lab, "free_memory_mb", return_value=16384), patch("subprocess.Popen") as launch:
            with self.assertRaisesRegex(RuntimeError, "Kleinere Gruppe"):
                lab.start(lab.VERSIONS)
            launch.assert_not_called()

    def test_dead_worker_recovery_archives_commands_but_keeps_world(self):
        row = lab.select("26.2")[0]
        with tempfile.TemporaryDirectory() as tmp, patch.object(lab, "ROOT", Path(tmp)):
            lab.properties(row)
            folder = Path(tmp) / row["version"]
            (folder / "worker.lock").touch()
            lab.save(folder / "process.json", {"worker_pid": 101, "server_pid": 102})
            lab.save(folder / "queue/pending.json", {"commands": ["say old"]})
            (folder / "world").mkdir()
            (folder / "world/level.dat").write_bytes(b"preserved")
            with patch.object(lab, "process_alive", return_value=True), patch.object(lab, "status", return_value=None):
                with self.assertRaises(RuntimeError):
                    lab.recover_stale_worker(row)
                self.assertTrue((folder / "worker.lock").exists())
            with patch.object(lab, "process_alive", return_value=False), patch.object(lab, "status", return_value=None):
                lab.recover_stale_worker(row)
            self.assertFalse((folder / "worker.lock").exists())
            self.assertTrue((folder / "queue/pending.interrupted").exists())
            self.assertEqual(b"preserved", (folder / "world/level.dat").read_bytes())

    def test_generated_chests_and_samples_stay_in_loaded_footprint(self):
        # These use actual generated server registries when the lab has been set up.
        for version in ["1.9", "1.12.2", "1.13", "1.20.6", "26.2"]:
            row = lab.select(version)[0]
            if not (lab.ROOT / version / "catalog.json").exists():
                self.skipTest("Originalserver-Kataloge fehlen; zuerst setup/catalog")
            instance = arena.Arena(row)
            commands = instance.generate()
            samples = instance.index["blocks"]
            self.assertEqual(len(samples), len({tuple(s["position"]) for s in samples}))
            self.assertEqual({b["name"] for b in instance.catalog["blocks"]}, {s["name"] for s in samples})
            for s in samples:
                x, y, z = s["position"]
                self.assertTrue(-80 <= x <= 95 and -112 <= z <= 111 and 0 < y < 256)
            self.assertEqual({m for m in instance.catalog["mobs"]}, {m["name"] for m in instance.index["mobs"]})
            self.assertEqual(2, sum(m["on_demand"] for m in instance.index["mobs"]))
            chests = [c for c in commands if c.startswith("setblock") and "{Items:[" in c]
            self.assertTrue(chests)
            self.assertIn("count:1" if row["protocol"] >= 766 else "Count:1b", chests[0])
            for c in commands:
                self.assertNotIn("\n", c)

    def test_sign_format_boundaries(self):
        for version, expected in [("1.12.2", "Text1:"), ("1.20.1", "front_text:"), ("26.2", '{text:"Test"}')]:
            row = lab.select(version)[0]
            with patch.object(lab, "catalog", return_value={"blocks": [], "items": [], "mobs": []}):
                instance = arena.Arena(row)
                instance.sign(1, 64, 1, ["Test"])
                self.assertIn(expected, instance.commands[-1])

    def test_original_feature_flags_exclude_future_content(self):
        for version, excluded, included in [("1.19.4", "minecraft:cherry_planks", "minecraft:mangrove_planks"),
                                             ("1.20.4", "minecraft:crafter", "minecraft:cherry_planks"),
                                             ("1.21.3", "minecraft:pale_oak_planks", "minecraft:crafter")]:
            path = lab.ROOT / version / "catalog.json"
            if not path.exists():
                self.skipTest("Originale Feature-Exporte fehlen")
            catalog = json.loads(path.read_text(encoding="utf-8"))
            names = {block["name"] for block in catalog["blocks"]}
            self.assertNotIn(excluded, names)
            self.assertIn(included, names)
            self.assertIn(excluded, catalog["excluded_experimental"]["blocks"])

    def test_chest_rebuild_replaces_block_before_nbt(self):
        row = lab.select("1.14.1")[0]
        with patch.object(lab, "catalog", return_value={"blocks": [], "items": [{"name": "minecraft:stone"}], "mobs": []}):
            instance = arena.Arena(row)
            instance.items()
            self.assertEqual("setblock -72 88 -65 air", instance.commands[0])
            self.assertIn('chest{Items:[{Slot:0b,id:"minecraft:stone",Count:1b}]}', instance.commands[1])

    def test_new_player_arrival_column_is_clear_above_platform(self):
        row = lab.select("26.2")[0]
        with patch.object(lab, "catalog", return_value={"blocks": [], "items": [], "mobs": []}):
            instance = arena.Arena(row)
            instance.mobs()
            self.assertIn("fill -24 207 56 24 207 104 bedrock", instance.commands)
            self.assertNotIn("fill -24 207 -24 24 207 24 bedrock", instance.commands)
            instance.stations()
            self.assertIn("fill 45 299 -5 55 299 5 sea_lantern", instance.commands)
            self.assertIn("fill 45 -60 -5 55 -55 5 air", instance.commands)
            self.assertNotIn("fill -5 299 -5 5 299 5 sea_lantern", instance.commands)

    def test_server_list_preserves_unknown_tags_and_existing_servers(self):
        old_entry = server_list.string_tag("name", "Mein Server") + server_list.string_tag("ip", "example.org:25565") + server_list.string_tag("icon", "preserved") + b"\0"
        extra = server_list.string_tag("custom", "unveraendert")
        original = b"\x0a\0\0" + extra + b"\x09" + server_list.utf("servers") + b"\x0a\0\0\0\x01" + old_entry + b"\0"
        merged, added = server_list.merge(original, lab.VERSIONS)
        self.assertEqual(48, added)
        self.assertIn(extra, merged)
        self.assertIn(old_entry, merged)
        self.assertIn(server_list.string_tag("viaForge$version", "26.2"), merged)
        repeated, added = server_list.merge(merged, lab.VERSIONS)
        self.assertEqual(0, added)
        self.assertEqual(merged, repeated)
        with self.assertRaises(ValueError):
            server_list.merge(b"broken", lab.VERSIONS)


if __name__ == "__main__":
    unittest.main()
