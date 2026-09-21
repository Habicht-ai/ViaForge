"""Stopped-server snapshots with verified files and a preserved pre-restore snapshot."""
import argparse
import json
import re
import shutil
import time
import uuid

import lab
from grim import assert_stopped, digest


def members(folder):
    # Bukkit may move only its own copied Nether/End into sibling world directories.
    return [p for p in folder.iterdir() if p.name in {"world", "world_nether", "world_the_end", "plugins"}
            or p.name in {"server.properties", "bukkit.yml", "spigot.yml", "paper.yml", "arena-index.json",
                          "arena-report.json", "sign-manifest.json", "inventory-catalog.json", "inventory-block-ids.json",
                          "inventory-commands.txt", "control-checks.json", "ops.json", "whitelist.json", "config"}]


def backup(row):
    assert_stopped(row)
    folder = lab.ROOT / row["version"]
    target = folder / "backups" / ("snapshot-" + time.strftime("%Y%m%d-%H%M%S") + "-" + uuid.uuid4().hex[:6])
    target.mkdir(parents=True)
    names = []
    for path in members(folder):
        names.append(path.name)
        if path.is_dir(): shutil.copytree(path, target / path.name)
        else: shutil.copy2(path, target / path.name)
    assert_stopped(row)
    hashes = {p.relative_to(target).as_posix(): digest(p) for p in target.rglob("*") if p.is_file()}
    lab.save(target / "snapshot.json", dict(version=row["version"], time=time.time(), members=names, sha256=hashes))
    print("BACKUP", target, flush=True)
    return target


def list_backups(row):
    folder = lab.ROOT / row["version"] / "backups"
    result = []
    for p in sorted(folder.glob("snapshot-*/snapshot.json"), reverse=True):
        try:
            data = json.loads(p.read_text(encoding="utf-8"))
            result.append(dict(id=p.parent.name, time=data["time"], files=len(data["sha256"])))
        except (OSError, ValueError, KeyError): continue
    return result


def restore(row, identifier):
    assert_stopped(row)
    if not re.fullmatch(r"snapshot-[0-9-]+-[a-f0-9]{6}", identifier):
        raise ValueError("Invalid snapshot ID")
    folder = (lab.ROOT / row["version"]).resolve()
    source = (folder / "backups" / identifier).resolve()
    if not source.is_relative_to(folder / "backups"):
        raise ValueError("Snapshot outside instance")
    metadata = json.loads((source / "snapshot.json").read_text(encoding="utf-8"))
    if metadata["version"] != row["version"]:
        raise ValueError("Snapshot belongs to another server")
    for name, expected in metadata["sha256"].items():
        path = (source / name).resolve()
        if not path.is_relative_to(source) or digest(path) != expected:
            raise ValueError("Snapshot checksum mismatch: " + name)
    allowed = {p.name for p in members(source)}
    if set(metadata["members"]) != allowed:
        raise ValueError("Invalid snapshot members")
    actual_files = {p.relative_to(source).as_posix()
        for name in metadata['members'] for p in
        ((source / name).rglob('*') if (source / name).is_dir() else [source / name]) if p.is_file()}
    if actual_files != set(metadata['sha256']):
        raise ValueError('Snapshot contains unverified or missing files')
    staging = folder / ("restore-staging-" + uuid.uuid4().hex)
    staging.mkdir()
    for name in metadata["members"]:
        path = source / name
        if path.is_dir(): shutil.copytree(path, staging / name)
        else: shutil.copy2(path, staging / name)
    assert_stopped(row)
    safety = backup(row)
    displaced = safety / "displaced"
    displaced.mkdir()
    installed = []
    moved = []
    try:
        for path in members(folder):
            path.rename(displaced / path.name)
            moved.append(path.name)
        for name in metadata["members"]:
            (staging / name).rename(folder / name)
            installed.append(name)
    except Exception:
        for name in installed: (folder / name).rename(staging / name)
        for name in moved: (displaced / name).rename(folder / name)
        raise
    lab.save(folder / "snapshot-restore.json", dict(time=time.time(), source=identifier, before=str(safety),
        success=True, remains_stopped=True))
    print("RESTORED", identifier, "previous state retained at", safety, flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["backup", "restore-backup"])
    parser.add_argument("versions")
    parser.add_argument("backup_id", nargs="?")
    args = parser.parse_args()
    import profiles
    for row in profiles.select(args.versions):
        if args.action == "backup": backup(row)
        else: restore(row, args.backup_id or "")
