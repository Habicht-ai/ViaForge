"""Inventory stacks, deliberately independent of the placed block-state catalog."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile

import lab

REVISION = 2
# These are command/operator tools, not ordinary catalog inventory entries.
TECHNICAL = {"air", "barrier", "light", "structure_void", "structure_block", "command_block",
             "chain_command_block", "repeating_command_block", "command_block_minecart", "jigsaw",
             "debug_stick", "knowledge_book", "spawner", "trial_spawner", "vault", "test_block",
             "test_instance_block"}


def stacks(row, catalog):
    if row.get("reference_version"):
        return stacks(next(r for r in lab.VANILLA_VERSIONS if r["version"] == row["reference_version"]), catalog)
    if row["protocol"] >= 393:
        return [dict(name=e["name"], metadata=0) for e in catalog["items"]
                if e["name"].removeprefix("minecraft:") not in TECHNICAL]
    path = lab.ROOT / row["version"] / "inventory-catalog.json"
    source = json.loads((lab.REPO / "src/main/resources/assets/viaforge/block-versions.json").read_text())[row["version"]]
    if path.exists():
        cached = json.loads(path.read_text(encoding="utf-8"))
        if (cached.get("revision") == REVISION and cached.get("server_sha1") == row["server"]["sha1"]
                and cached.get("client_sha1") == source["sha1"] and (path.parent / "inventory-block-ids.json").exists()):
            return cached["items"]
    client = lab.REPO / "run/ViaForge/block-assets" / (row["version"] + "-client.jar")
    if not client.exists():
        lab.download(source["url"], client, source["sha1"])
    client_sha1 = hashlib.sha1(client.read_bytes()).hexdigest()
    if client_sha1 != source["sha1"]:
        raise RuntimeError("Original-Client-Pruefsumme stimmt nicht: " + str(client))
    folder = lab.ROOT / row["version"]
    with zipfile.ZipFile(folder / "server.jar") as z:
        classes = {n: z.read(n) for n in z.namelist() if n.endswith(".class") and "/" not in n}
    signatures = [[b"STDOUT", b"STDERR"], [b"iron_shovel", b"carrot_on_a_stick"], [b"flowing_water", b"bedrock", b"iron_ore"]]
    names = [max((n for n, b in classes.items() if all(s in b for s in sig)),
                 key=lambda n: len(classes[n]))[:-6] for sig in signatures]
    # A per-version helper directory also allows concurrent migrations without javac races.
    helper = folder / "inventory-helper"
    helper.mkdir(exist_ok=True)
    exe = lab.java(row)
    subprocess.run([str(Path(exe).with_name("javac.exe")), "-cp", str(folder / "server.jar"),
                    "-d", str(helper), str(lab.HERE / "CreativeInventoryDump.java")],
                   check=True, creationflags=lab.NO_WINDOW)
    raw = folder / "creative-items-raw.json"
    with (folder / "inventory-export.log").open("w") as log:
        subprocess.run([exe, "-Xmx768m", "-cp", os.pathsep.join(map(str, [helper, client, folder / "server.jar"])),
                        "vflab.CreativeInventoryDump", *names[:2], str(raw), names[2], str(folder / "inventory-block-ids.json")], cwd=folder,
                       stdout=log, stderr=subprocess.STDOUT, check=True, creationflags=lab.NO_WINDOW)
    available = {e["name"] for e in catalog["items"]}
    items = json.loads(raw.read_text(encoding="utf-8"))
    if not items or any(e["name"] not in available for e in items):
        raise RuntimeError("Creative-Export passt nicht zur Server-Itemregistry")
    items = [e for e in items if e["name"].removeprefix("minecraft:") not in TECHNICAL]
    lab.save(path, dict(revision=REVISION, version=row["version"], server_sha1=row["server"]["sha1"],
                       client_sha1=client_sha1,
                       source="Original client creative item enumerators, including original stack NBT", items=items))
    return items
