"""Migrate existing local worlds to free game modes and a 16-chunk view distance."""
import argparse
import concurrent.futures
import json
import re
import shutil
import time
import uuid

import arena
import exhibition
import lab
import world_audit


def properties(text, row):
    for key, value in {"view-distance": "16", "gamemode": lab.creative_property(row)}.items():
        pattern = r"(?m)^" + re.escape(key) + r"=.*$"
        text = re.sub(pattern, key + "=" + value, text) if re.search(pattern, text) else text.rstrip() + "\n" + key + "=" + value + "\n"
    return text


def apply(row):
    folder = lab.ROOT / row["version"]
    if lab.status(row):
        raise RuntimeError("Bitte vor der Einstellungsumstellung speichern und stoppen: " + row["version"])
    before = world_audit.audit(row)  # Also checks managed process ownership before copying the world.
    backup = folder / "backups" / ("free-play-" + time.strftime("%Y%m%d-%H%M%S") + "-" + uuid.uuid4().hex[:6])
    shutil.copytree(folder / "world", backup / "world")
    for name in ["server.properties", "sign-manifest.json", "control-checks.json", "arena-report.json", "exhibition-maintenance.json"]:
        if (folder / name).exists():
            shutil.copy2(folder / name, backup / name)
    config = folder / "server.properties"
    config.write_text(properties(config.read_text(encoding="utf-8"), row), encoding="utf-8")
    a = arena.Arena(row)
    checks = exhibition.free_play(a)
    old_checks = exhibition.read(folder / "control-checks.json")
    old_checks = [c for c in old_checks if not re.search(r"\b(?:testforblock|(?:if|unless) block) [1-4] 60 1 ", c)]
    lab.save(folder / "control-checks.json", old_checks + checks)
    signs = {tuple(s["position"]): s for s in exhibition.read(folder / "sign-manifest.json")}
    signs.update(a.signs)
    lab.save(folder / "sign-manifest.json", list(signs.values()))
    result = dict(version=row["version"], success=False, view_distance=16, automatic_adventure=False,
                  backup=str(backup), time=time.time())
    lab.save(folder / "server-settings-check.json", result)
    lab.start([row])
    try:
        if row["protocol"] >= 401:
            lab.batch(row, ["forceload add -80 -112 95 111"])
            time.sleep(2)
        errors = arena.error_lines(lab.batch(row, a.commands))
        if errors:
            raise RuntimeError(str(errors))
        arena.verify(row)
        errors = arena.error_lines(lab.batch(row, lab.release_chunks(row) + ["save-all flush"]))
        if errors:
            raise RuntimeError(str(errors))
    finally:
        lab.stop([row])
    world = world_audit.World(folder)
    for x in range(1, 5):
        block = world.block(x, 60, 1)
        if block.get("Name") in {"minecraft:command_block", "minecraft:repeating_command_block", "minecraft:chain_command_block"} or block.get("legacy_id") in {137, 210, 211}:
            raise RuntimeError("Alter Spielmodus-Befehlsblock noch vorhanden")
    sign_issues = world_audit.audit_signs(world, list(a.signs.values()))
    if sign_issues or not re.search(r"(?m)^view-distance=16$", config.read_text(encoding="utf-8")):
        raise RuntimeError("Sichtweite oder Hinweisschilder stimmen nicht")
    if not re.search(r"(?m)^gamemode=" + lab.creative_property(row) + "$", config.read_text(encoding="utf-8")):
        raise RuntimeError("Server hat den Creative-Standard nicht uebernommen")
    report = exhibition.read(folder / "arena-report.json")
    report.update(gallery_protection=False, exhibition_revision=exhibition.REVISION, view_distance=16)
    lab.save(folder / "arena-report.json", report)
    maintenance = exhibition.read(folder / "exhibition-maintenance.json")
    maintenance.update(revision=exhibition.REVISION, settings_updated=time.time())
    lab.save(folder / "exhibition-maintenance.json", maintenance)
    result.update(success=True, time=time.time())
    lab.save(folder / "server-settings-check.json", result)
    print("SETTINGS PASS", row["version"], "Creative frei, Sichtweite 16", flush=True)
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("versions", nargs="?", default="all")
    args = parser.parse_args()
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        futures = {pool.submit(apply, row): row for row in lab.select(args.versions)}
        for future in concurrent.futures.as_completed(futures):
            try:
                results.append(future.result())
            except Exception as error:
                result = dict(version=futures[future]["version"], success=False, error=str(error))
                results.append(result)
                print("FAILED", result, flush=True)
    lab.save(lab.ROOT / "server-settings-last-run.json", dict(time=time.time(), results=results))
    if not all(r["success"] for r in results):
        raise SystemExit(1)
