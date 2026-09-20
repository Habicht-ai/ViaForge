"""Targeted repairs and wayfinding improvements for the reserved exhibition area."""
import concurrent.futures
import json
import shutil
import time

import arena
import lab
import world_audit

REVISION = 2
TRANSIENT = {"minecraft:flowing_water", "minecraft:flowing_lava", "minecraft:frosted_ice"}


def properties(sample):
    state = sample.get("state", "")
    if "[" not in state:
        return {}
    return dict(part.split("=", 1) for part in state.split("[", 1)[1].rstrip("]").split(","))


def wet(sample):
    name = sample["name"].removeprefix("minecraft:")
    return name in {"water", "lava", "bubble_column", "kelp", "kelp_plant", "seagrass", "tall_seagrass"} or "coral" in name or properties(sample).get("waterlogged") == "true"


def tank(a, sample):
    x, y, z = sample["position"]
    # Closed sides and bottom, including the block above a waterlogged specimen.
    a.fill(x - 1, y - 1, z - 1, x + 1, y - 1, z + 1, "glass")
    for dx, dz in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
        a.fill(x + dx, y, z + dz, x + dx, y + 2, z + dz, "glass")


def repair(a, sample):
    name = sample["name"].removeprefix("minecraft:")
    x, y, z = sample["position"]
    meta = sample.get("metadata", 0)
    props = properties(sample)
    if sample["name"] in TRANSIENT:
        return
    if name.endswith("concrete_powder") or name in {"sand", "red_sand", "gravel", "anvil", "chipped_anvil", "damaged_anvil", "dragon_egg", "suspicious_sand", "suspicious_gravel"}:
        a.block(x, y - 1, z, "stone")
    if wet(sample):
        tank(a, sample)
        if "coral" in name or name in {"kelp", "kelp_plant", "seagrass", "tall_seagrass", "bubble_column"}:
            a.fill(x, y, z, x, y + 2, z, "water")
    if any(word in name for word in ("sapling", "flower", "tulip", "grass", "fern", "bush", "roots", "mushroom", "fungus")) or name in {"bamboo", "sugar_cane", "dandelion", "poppy", "allium", "lilac", "peony", "sunflower", "oxeye_daisy", "pitcher_plant", "pink_petals", "leaf_litter"}:
        a.block(x, y - 1, z, "dirt")
    if name in {"cactus", "dead_bush", "deadbush"}:
        a.block(x, y - 2, z, "stone")
        a.block(x, y - 1, z, "sand")
    if name in {"chorus_plant", "chorus_flower"}:
        a.block(x, y - 1, z, "end_stone")
        if name == "chorus_plant":
            a.block(x, y + 1, z, "chorus_flower")
    if name == "bamboo":
        a.block(x, y - 1, z, "dirt")
    if name == "small_dripleaf":
        a.block(x, y - 1, z, "clay")
    if name == "big_dripleaf_stem":
        a.block(x, y - 1, z, "clay")
        a.block(x, y + 1, z, "big_dripleaf[facing=" + props.get("facing", "north") + "]")
    if name in {"weeping_vines", "weeping_vines_plant", "cave_vines", "cave_vines_plant", "pale_hanging_moss"}:
        a.block(x, y + 1, z, "netherrack" if name.startswith("weeping") else "oak_log")
        if name.endswith("_plant"):
            a.block(x, y - 1, z, name.removesuffix("_plant"))
    if name == "twisting_vines_plant":
        a.block(x, y - 1, z, "netherrack")
        a.block(x, y + 1, z, "twisting_vines")
    if name == "bubble_column":
        a.block(x, y - 1, z, "soul_sand")
    if name == "fire":
        a.block(x, y - 1, z, "netherrack")
    if name == "soul_fire":
        a.block(x, y - 1, z, "soul_sand")
    if name == "sugar_cane":
        a.block(x, y - 1, z, "dirt")
        a.block(x + 1, y - 2, z, "glass")
        a.block(x + 1, y - 1, z, "water")
    if name in {"grass_path", "dirt_path", "farmland"}:
        a.block(x, y + 1, z, "air")
    if a.legacy and name in {"torch", "redstone_torch", "unlit_redstone_torch"} and meta in {1, 2, 3, 4}:
        dx, dz = {1: (-1, 0), 2: (1, 0), 3: (0, -1), 4: (0, 1)}[meta]
        a.block(x + dx, y, z + dz, "stone")
    if a.legacy and name in {"rail", "detector_rail", "golden_rail", "activator_rail"} and meta & 7 in {2, 3, 4, 5}:
        dx, dz = {2: (1, 0), 3: (-1, 0), 4: (0, -1), 5: (0, 1)}[meta & 7]
        a.block(x + dx, y, z + dz, "stone")
    if not a.legacy and ("wall" in name or name in {"ladder", "tripwire_hook"}) and props.get("facing") in {"north", "south", "east", "west"}:
        dx, dz = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}[props["facing"]]
        a.block(x + dx, y, z + dz, "stone")
    if name in {"wheat", "carrots", "potatoes", "beetroots", "torchflower_crop", "pitcher_crop"}:
        a.block(x, y - 1, z, "farmland")
    a.block(x, y, z, sample["state"], meta)
    if name == "kelp_plant":
        a.block(x, y + 1, z, "kelp[age=25]")
    # Restore the paired half after the base, avoiding ghost door/bed exhibits after updates.
    if not a.legacy and props.get("half") == "lower" and (name != 'pitcher_crop' or int(props.get('age',0)) >= 3):
        top = dict(props, half="upper")
        a.block(x, y + 1, z, name + "[" + ",".join(k + "=" + v for k, v in top.items()) + "]")
    if not a.legacy and name.endswith("_bed"):
        dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[props["facing"]]
        a.block(x + dx, y, z + dz, name + "[" + ",".join(k + "=" + v for k, v in dict(props, part="head").items()) + "]")


def wayfinding(a):
    checks = []
    for y, title, color, meta in [(64, "BLOECKE A", "lime", 5), (88, "ITEMS / MODELLE", "orange", 1),
                                  (112, "MOBS", "light_blue", 3), (144, "BLOECKE B", "purple", 10)]:
        wool = "wool" if a.legacy else color + "_wool"
        for x in range(-80, -64):
            a.block(x, y - 1, -78, wool, meta)
        a.block(-75, y, -78, "quartz_block")
        a.block(-75, y + 1, -78, "sea_lantern")
        a.sign(-75, y + 2, -78, [title, a.row["version"], "RUECKWEG LINKS", "Navigation"])
        a.button(-78, y, 108, ["Zur Navigation", "Rueckweg"], "tp @p 0 65 -100")
        checks.append(((-75, y + 1, -78), "sea_lantern"))
    # Arrival pad: colored perimeter and corner lights, outside the arrival column.
    for z in [-12, 12]:
        for x in range(-12, 13):
            a.block(x, 175, z, "wool" if a.legacy else "lime_wool", 5)
    for x in [-12, 12]:
        a.fill(x, 175, -11, x, 175, 11, "sea_lantern")
    a.sign(-4, 176, -3, ["WILLKOMMEN", "1. Knopf", "2. Bereich", "3. Testen"])
    a.sign(4, 176, -3, ["TESTLABOR", "Start: Creative", "Survival-Knopf", "bei Navigation"])
    return checks


def labels(a, index):
    if a.legacy:
        return
    for i, sample in enumerate(index["blocks"]):
        x, y, z = sample["position"]
        a.labelled(x, y, z - 2, sample["name"], "#" + str(i + 1))
    chests = list(dict.fromkeys(tuple(item["chest"]) for item in index["items"]))
    for i, (x, y, z) in enumerate(chests):
        a.sign(x, y, z - 1, ["ITEMKATALOG", "Kiste " + str(i + 1), f"{i*27+1} - {min(i*27+27,len(index['items']))}", "alle Items"])
    for mob in index["mobs"]:
        if not mob["on_demand"]:
            x, y, z = mob["position"]
            a.labelled(x, y, z - 4, mob["name"], "Mob-Test")


def refine(row, remaining_only=False):
    folder = lab.ROOT / row["version"]
    report_path = folder / "refinement.json"
    old = json.loads(report_path.read_text()) if report_path.exists() else {}
    if old.get("revision") == REVISION and old.get("success"):
        return old
    if lab.status(row):
        raise RuntimeError("Bitte Testserver für gesichertes Upgrade vorher stoppen: " + row["version"])
    before = world_audit.audit(row)
    backup = folder / "backups" / "before-refinement-1"
    if not backup.exists():
        shutil.copytree(folder / "world", backup / "world")
        shutil.copy2(folder / "arena-index.json", backup / "arena-index.json")
        lab.save(backup / "audit.json", before)
    index = json.loads((folder / "arena-index.json").read_text(encoding="utf-8"))
    a = arena.Arena(row)
    wet_samples = [] if remaining_only else [s for s in index["blocks"] if wet(s)]
    for s in wet_samples:
        tank(a, s)
    if row["protocol"] >= 393 and not remaining_only:
        # Drain only the reserved block exhibition floors; item and mob floors are untouched.
        for y in [64, 144]:
            for x in range(-80, 80, 32):
                for z in range(-80, 112, 32):
                    a.add(f"fill {x} {y} {z} {x+31} {y+2} {z+31} air replace water")
    targets = {tuple(s["position"]): s for s in before["missing"] + before["changed_type"] + wet_samples}
    for sample in targets.values():
        repair(a, sample)
    if not remaining_only:
        labels(a, index)
    checks = wayfinding(a)
    extra_checks = []
    for y in (64, 88, 112, 144):
        for x, by, z, name, nbt in [(-75, y+1, -78, "sea_lantern", ""),
                                    (-78, y, 108, "command_block", '{Command:"tp @p 0 65 -100"}')]:
            extra_checks.append(f"testforblock {x} {by} {z} {name} -1 {nbt}" if a.legacy else
                                f"execute if block {x} {by} {z} {name}{nbt} run say VFLAB_CONTROL_OK")
    lab.start([row])
    try:
        if row["protocol"] >= 401:
            lab.batch(row, ["forceload add -80 -112 95 111"])
            time.sleep(2)
        errors = []
        for offset in range(0, len(a.commands), 60):
            errors += arena.error_lines(lab.batch(row, a.commands[offset:offset+60]))
        if errors:
            raise RuntimeError(str(errors[:10]))
        time.sleep(3)
        lab.batch(row, ["save-all flush"] + lab.release_chunks(row))
        lab.save(folder / "refinement-checks.json", extra_checks)
        arena.verify(row)
    finally:
        lab.stop([row])
    after = world_audit.audit(row)
    world = world_audit.World(folder)
    signs = []
    for position, expected in checks:
        actual = world.block(*position)
        signs.append(actual.get("Name") == "minecraft:" + expected if row["protocol"] >= 393 else actual.get("legacy_id") == 169)
    remaining = [s for s in after["missing"] + after["changed_type"] if s["name"] not in TRANSIENT
                 and not (s["name"].endswith("air") and s.get("actual", {}).get("Name", "").endswith("air"))]
    result = dict(version=row["version"], revision=REVISION, success=all(signs) and not remaining, time=time.time(),
                  commands=len(a.commands), before_missing=len(before["missing"]), after_missing=len(after["missing"]),
                  before_changed=len(before["changed_type"]), after_changed=len(after["changed_type"]),
                  wayfinding_checks=signs, remaining=remaining, backup=str(backup))
    lab.save(report_path, result)
    print("REFINED", result, flush=True)
    return result


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("versions", nargs="?", default="all")
    parser.add_argument("--remaining-only", action="store_true")
    args = parser.parse_args()
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        futures = {pool.submit(refine, row, args.remaining_only): row for row in lab.select(args.versions)}
        failures = []
        for future in concurrent.futures.as_completed(futures):
            try:
                result = future.result()
                if not result["success"]:
                    failures.append(dict(version=result["version"], error="Remaining block exhibit issues", remaining=result["remaining"]))
            except Exception as error:
                result = dict(version=futures[future]["version"], error=str(error))
                failures.append(result)
                print("FAILED", result, flush=True)
        if failures:
            lab.save(lab.ROOT / "refinement-failures.json", failures)
            raise SystemExit(1)
