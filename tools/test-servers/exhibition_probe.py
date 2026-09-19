"""Opt-in integration test: freely chosen player modes and backed-up damage recovery."""
import json
import re
import time
import sys

import arena
import exhibition
import lab
import login_probe
import world_audit


def probe(row, resume=False, modes_only=False):
    if lab.status(row):
        raise RuntimeError("Probe nur bei beendetem Testserver starten")
    folder = lab.ROOT / row["version"]
    index = exhibition.read(folder / "arena-index.json")
    sample = next(s for s in index["blocks"] if s["name"] == "minecraft:purpur_block")
    x, y, z = sample["position"]
    before = world_audit.World(folder)
    if not resume and not modes_only and before.block(100, 70, 100) not in ({"Name": "minecraft:air"}, {"legacy_id": 0, "metadata": 0}):
        raise RuntimeError("Probe-Sentinelplatz ist belegt")
    checks = []

    def joined(name):
        legacy = row["protocol"] < 393
        for label, pos, requested, expected in [
                ("gallery_creative", "-78 89 -78", 1, 1),
                ("gallery_survival", "-78 65 -78", 0, 0),
                ("arrival_creative", "0 177 0", 1, 1),
                ("arrival_survival", "0 177 0", 0, 0),
                ("test_stations_survival", "0 65 -100", 0, 0),
                ("test_stations_creative", "0 65 -100", 1, 1)]:
            mode = str(requested) if legacy else ["survival", "creative"][requested]
            lab.batch(row, ["tp " + name + " " + pos, "gamemode " + mode + " " + name])
            time.sleep(.6)
            selector = f"@a[name={name}," + (f"m={expected}]" if legacy else
                          "gamemode=" + ["survival", "creative", "adventure"][expected] + "]")
            command = "testfor " + selector if legacy else "data get entity " + name + " playerGameType"
            with lab.Rcon(row) as remote:
                output = remote.command(command)
            passed = ("Found " in output if legacy else bool(re.search(r"entity data:\s*" + str(expected) + r"\s*$", output)))
            checks.append(dict(name=label, success=passed, output=output))
            if not passed:
                raise RuntimeError("Spielmodus wurde unerwartet veraendert: " + str(checks[-1]))

    chest = index["items"][0]["chest"]
    if not resume:
        lab.start([row])
        try:
            login_probe.probe(row, joined, expected_view_distance=16)
            time.sleep(1)  # Let the intentionally disconnected socket finish logging out.
            if modes_only:
                lab.save(folder / "free-play-probe.json", dict(success=True, version=row["version"], mode_checks=checks, time=time.time()))
                print("FREE PLAY PROBE PASS", row["version"], flush=True)
                return
            if row["protocol"] >= 401:
                lab.batch(row, ["forceload add -80 -112 95 111", "forceload add 100 100"])
                time.sleep(2)
            commands = [f"setblock {x} {y} {z} stone", "setblock -15 65 -99 air",
                        "setblock " + " ".join(map(str, chest)) + " air", "setblock 100 70 100 diamond_block", "save-all flush"]
            errors = [line for line in arena.error_lines(lab.batch(row, commands)) if "lost connection:" not in line]
            if errors:
                raise RuntimeError(str(errors))
        finally:
            lab.stop([row])
    damaged = world_audit.audit(row)
    if not any(s["position"] == [x, y, z] for s in damaged["changed_type"]) or not damaged["sign_issues"]:
        raise RuntimeError("Absichtlich beschaedigte Exponate wurden nicht erkannt")
    exhibition.maintain(row, restore=True)
    after = world_audit.World(folder)
    chest_entity = after.block_entity(*chest)
    sentinel = after.block(100, 70, 100)
    if len(chest_entity.get("Items", [])) != 27 or sentinel.get("Name", "") != "minecraft:diamond_block" and sentinel.get("legacy_id") != 57:
        raise RuntimeError("Kistenwiederherstellung oder Erhalt fremder Bauten fehlgeschlagen")
    # Restart verification proves free game modes, inventories and signs survive a normal server restart.
    lab.start([row])
    try:
        login_probe.probe(row, joined, expected_view_distance=16)
        arena.verify(row)
        if row["protocol"] >= 401:
            lab.batch(row, ["forceload add 100 100"])
            time.sleep(1)
        lab.batch(row, ["setblock 100 70 100 air"] +
                  (["forceload remove 100 100"] if row["protocol"] >= 401 else []) + ["save-all flush"])
    finally:
        lab.stop([row])
    result = dict(version=row["version"], success=True, mode_checks=checks, damage_detected=True,
                  restored_block_sign_chest=True, external_build_preserved=True, restart_checked=True, time=time.time())
    lab.save(folder / "exhibition-probe.json", result)
    print("EXHIBITION PROBE PASS", row["version"], flush=True)


if __name__ == "__main__":
    for row in lab.select(sys.argv[1] if len(sys.argv) > 1 else "1.12.2,26.2"):
        probe(row, "--resume" in sys.argv, "--modes-only" in sys.argv)
