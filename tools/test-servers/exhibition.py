"""Backed-up, repeatable maintenance of managed exhibits; never rebuilds the world."""
import argparse
import concurrent.futures
import copy
import json
import re
import shutil
import time
import uuid

import arena
import lab
import world_audit
import world_refine
import terrain

REVISION = 4


def free_play(a):
    """Remove our old automatic mode guards. Players retain their chosen game mode."""
    checks = []
    for x in range(1, 5):
        a.block(x, 60, 1, "air")
        checks.append(f"testforblock {x} 60 1 air" if a.legacy else
                      "execute " + " ".join(f"unless block {x} 60 1 {block}" for block in
                                            ["command_block", "repeating_command_block", "chain_command_block"])
                      + " run say VFLAB_CONTROL_OK")
    a.add("defaultgamemode " + ("1" if a.legacy else "creative"))
    a.sign(-19, 64, -104, ["AUSSTELLUNG", "Spielmodus frei", "Creative bleibt", "aktiv"])
    a.sign(-19, 64, -102, ["VERSEHENTLICH WEG?", "Webseite > Server", "Ausstellung", "wiederherstellen"])
    return checks


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def command_checks(row, commands):
    checks = {}
    for command in commands:
        if not re.match(r"setblock -?\d+ -?\d+ -?\d+ command_block", command):
            continue
        _, x, y, z, _ = command.split(" ", 4)
        match = re.search(r'Command:("(?:[^"\\]|\\.)*")', command)
        data = "{Command:" + match[1] + "}"
        checks[(x, y, z)] = (f"testforblock {x} {y} {z} command_block -1 {data}" if row["protocol"] < 393 else
                             f"execute if block {x} {y} {z} command_block{data} run say VFLAB_CONTROL_OK")
    return list(checks.values())


def prepare_frogspawn_station(row):
    """Prepare only this ephemeral exhibit. Caller must back up the stopped world first."""
    import grim
    grim.assert_stopped(row)
    folder = lab.ROOT / row['version']
    index = read(folder / 'arena-index.json')
    samples = [s for s in index['blocks'] if s['name'] == 'minecraft:frogspawn']
    if not samples: return []
    changes = arena.Arena(row)
    for sample in samples: world_refine.repair(changes, sample)
    signs = {tuple(s['position']): s for s in read(folder / 'sign-manifest.json')}
    signs.update(changes.signs)
    checks = read(folder / 'control-checks.json')
    checks = list(dict.fromkeys(checks + command_checks(row, changes.button_commands)))
    lab.save(folder / 'arena-index.json', index)
    lab.save(folder / 'sign-manifest.json', list(signs.values()))
    lab.save(folder / 'control-checks.json', checks)
    (folder / 'frogspawn-station-commands.txt').write_text('\n'.join(changes.commands) + '\n', encoding='utf-8')
    return changes.commands


def maintain(row, restore=False):
    folder = lab.ROOT / row["version"]
    report_path = folder / "exhibition-maintenance.json"
    if not restore and report_path.exists():
        old = read(report_path)
        if old.get("revision") == REVISION and old.get("success"):
            print("Bereits aktualisiert:", row["version"], flush=True)
            return old
    if lab.status(row):
        raise RuntimeError("Vor Wiederherstellung bitte diesen Testserver auf der Webseite stoppen: " + row["version"])
    before = world_audit.audit(row, terrain_check=False)  # Also rejects a live worker still starting/saving.
    index = read(folder / "arena-index.json")
    generated = arena.Arena(row)
    generated.generate()
    generated.fixtures(copy.deepcopy(index))  # Only collect definitions; do not run mob/floor rebuilds.
    inventory = arena.Arena(row)
    inventory.items()
    for position, sign in inventory.signs.items():
        generated.signs[position] = sign
    changes = arena.Arena(row)
    terrain_before = terrain.inspect(row, generated, index)
    if terrain_before["missing_chunks"]:
        raise RuntimeError("Exhibition chunks missing; refusing incomplete repair: " + str(terrain_before["missing_chunks"]))
    changes.commands += terrain.removal_commands(terrain_before["positions"])
    changes.commands += terrain.boundary_commands(world_audit.World(folder), terrain.protected_positions(generated,index))
    changes.elytra()
    index["stations"] = [s for s in index["stations"] if s["name"] != "elytra_flight"] + changes.index["stations"]
    world = world_audit.World(folder)
    targets = before["missing"] + before["changed_type"]
    if terrain_before["intrusions"]: targets = index["blocks"]
    for sample in targets:
        world_refine.repair(changes, sample)
    for sample in index['blocks']:
        if sample['name'] == 'minecraft:frogspawn':
            world_refine.repair(changes, sample)
    # Restore the separate bed/shulker model gallery too, without respawning any mobs.
    if restore:
        fixture = arena.Arena(row)
        fixture.fixtures(copy.deepcopy(index))
        for command in fixture.commands:
            if re.match(r"setblock -?\d+ 88 (8|9|20) (?:minecraft:)?(?:air(?: |$)|bed |\w+_bed\[|\w+_shulker_box)", command):
                changes.add(command)
    old_chests = {tuple(e["chest"]) for e in index["items"]}
    new_chests = {tuple(e["chest"]) for e in inventory.index["items"]}
    for x, y, z in old_chests - new_chests:
        changes.block(x, y, z, "air")
        changes.block(x, y, z - 1, "air")
    changes.commands += inventory.commands
    changes.commands += generated.button_commands
    checks = command_checks(row, generated.button_commands)
    checks += free_play(changes)
    generated.signs.update(changes.signs)
    # Put back all labels, with support, even when an earlier audit missed absent signs.
    for position, sign in generated.signs.items():
        x, y, z = position
        below = world.block(x, y - 1, z)
        if below.get("legacy_id") == 0 or below.get("Name") in {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}:
            changes.block(x, y - 1, z, "quartz_block")
        changes.add(f"setblock {x} {y} {z} air")
        changes.add(sign["command"])
    # Backup before either world or expected-data mutations. Every manual restore gets its own snapshot.
    backup = folder / "backups" / ("exhibition-" + time.strftime("%Y%m%d-%H%M%S") + "-" + uuid.uuid4().hex[:6])
    shutil.copytree(folder / "world", backup / "world")
    for name in ["arena-index.json", "arena-report.json", "server.properties", "inventory-commands.txt",
                 "sign-manifest.json", "control-checks.json", "exhibition-maintenance.json"]:
        if (folder / name).exists():
            shutil.copy2(folder / name, backup / name)
    result = dict(version=row["version"], revision=REVISION, success=False, time=time.time(), backup=str(backup),
                  restore=restore, commands=len(changes.commands), item_stacks=len(inventory.index["items"]),
                  signs=len(generated.signs))
    lab.save(report_path, result)
    config = (folder / "server.properties").read_text(encoding="utf-8")
    terrain.fix_generator(row)
    config = terrain.properties(config, row)
    config = re.sub(r"(?m)^gamemode=.*$", "gamemode=" + lab.creative_property(row), config)
    (folder / "server.properties").write_text(config, encoding="utf-8")
    # Save inspectable commands and expectations before starting; a failure remains explicit/retryable.
    (folder / "exhibition-commands.txt").write_text("\n".join(changes.commands) + "\n", encoding="utf-8")
    (folder / "inventory-commands.txt").write_text("\n".join(inventory.commands) + "\n", encoding="utf-8")
    index["items"] = inventory.index["items"]
    lab.save(folder / "arena-index.json", index)
    lab.save(folder / "sign-manifest.json", list(generated.signs.values()))
    lab.save(folder / "control-checks.json", checks)
    lab.start([row])
    try:
        if row["protocol"] >= 401:
            lab.batch(row, ["forceload add -80 -112 95 111"])
            time.sleep(2)
        for offset in range(0, len(changes.commands), 60):
            errors = arena.error_lines(lab.batch(row, changes.commands[offset:offset + 60]))
            if errors:
                raise RuntimeError(str(errors[:5]))
        time.sleep(2)
        arena.verify(row)
        errors = arena.error_lines(lab.batch(row, lab.release_chunks(row) + ["save-all flush"]))
        if errors:
            raise RuntimeError(str(errors[:5]))
    finally:
        lab.stop([row])
    after = world_audit.audit(row, terrain_check=False)
    terrain_after = terrain.inspect(row)
    lab.save(folder / "terrain-audit.json", terrain_after)
    result.update(terrain_removed=terrain_before["intrusions"], terrain_remaining=terrain_after["intrusions"], generator_flat=terrain_after["flat"])
    result.update(success=after["unexpected"] == 0 and not after.get("sign_issues") and terrain_after["intrusions"] == 0 and terrain_after["flat"],
                  block_issues=after["unexpected"], sign_issues=after.get("sign_issues", []), time=time.time())
    lab.save(report_path, result)
    report = read(folder / "arena-report.json")
    report.update(item_stacks=result["item_stacks"], exhibition_revision=REVISION, gallery_protection=False)
    lab.save(folder / "arena-report.json", report)
    print("EXHIBITION", row["version"], "PASS" if result["success"] else "FAIL", result["item_stacks"],
          "Items,", result["signs"], "Schilder,", len(result["sign_issues"]), "Schildfehler", flush=True)
    if not result["success"]:
        raise RuntimeError("Ausstellungspruefung fehlgeschlagen: " + row["version"])
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("versions", nargs="?", default="all")
    parser.add_argument("--restore", action="store_true")
    args = parser.parse_args()
    import profiles
    failures = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        futures = {pool.submit(maintain, row, args.restore): row for row in profiles.select(args.versions)}
        for future in concurrent.futures.as_completed(futures):
            try:
                future.result()
            except Exception as error:
                failure = dict(version=futures[future]["version"], error=str(error))
                failures.append(failure)
                print("FAILED", failure, flush=True)
    lab.save(lab.ROOT / "exhibition-last-run.json", dict(time=time.time(), failures=failures))
    if failures:
        raise SystemExit(1)
    lab.dashboard()
