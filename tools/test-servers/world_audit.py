"""Read-only audit of saved exhibition chunks; no world data is written."""
import argparse
from collections import Counter
import gzip
import json
import struct
import time
import zlib

import lab
from server_list import Reader


def unexpected(sample):
    if sample["name"] in {"minecraft:flowing_water", "minecraft:flowing_lava", "minecraft:frosted_ice"}:
        return False
    return not (sample["name"].endswith("air") and sample.get("actual", {}).get("Name", "").endswith("air"))


class NBT(Reader):
    def payload(self, kind, depth=0):
        if depth > 64:
            raise ValueError("NBT nesting limit")
        formats = {1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}
        if kind in formats:
            fmt = ">" + formats[kind]
            return struct.unpack(fmt, self.read(struct.calcsize(fmt)))[0]
        if kind == 7:
            return self.read(self.number())
        if kind in (11, 12):
            count = self.number()
            return struct.unpack(">" + str(count) + ("i" if kind == 11 else "q"), self.read(count * (4 if kind == 11 else 8)))
        if kind == 8:
            return self.string()
        if kind == 9:
            subtype = self.read(1)[0]
            return [self.payload(subtype, depth + 1) for _ in range(self.number())]
        if kind == 10:
            value = {}
            while (subtype := self.read(1)[0]) != 0:
                name = self.string()
                value[name] = self.payload(subtype, depth + 1)
            return value
        raise ValueError("Unknown NBT tag " + str(kind))


def nbt(data):
    reader = NBT(data)
    kind = reader.read(1)[0]
    reader.string()
    return reader.payload(kind)


def block_state(value):
    # 26.3 uses bare identifiers or {id,properties}, inside heterogeneous-list
    # wrappers. Older chunks retain {Name,Properties}. Normalize without defaults.
    if isinstance(value, dict) and set(value) == {''}: value = value['']
    if isinstance(value, str): return {'Name': value}
    if 'id' in value:
        return {'Name': value['id'], **({'Properties': value['properties']} if 'properties' in value else {})}
    return value


class World:
    def __init__(self, folder):
        self.region = folder / "world" / "region"
        if not self.region.exists():
            self.region = folder / "world/dimensions/minecraft/overworld/region"
        self.regions = {}
        self.chunks = {}

    def chunk(self, x, z):
        key = (x, z)
        if key in self.chunks:
            return self.chunks[key]
        region = (x // 32, z // 32)
        if region not in self.regions:
            self.regions[region] = (self.region / f"r.{region[0]}.{region[1]}.mca").read_bytes()
        data = self.regions[region]
        location = int.from_bytes(data[4 * (x % 32 + z % 32 * 32):][:4], "big")
        offset = (location >> 8) * 4096
        if not offset:
            return {}
        length = int.from_bytes(data[offset:offset + 4], "big")
        compression = data[offset + 4]
        payload = data[offset + 5:offset + 4 + length]
        if compression == 2:
            payload = zlib.decompress(payload)
        elif compression == 1:
            payload = gzip.decompress(payload)
        elif compression != 3:
            raise ValueError("Unsupported chunk compression " + str(compression))
        root = nbt(payload)
        value = root.get("Level", root)
        value["_data_version"] = root.get("DataVersion", 0)
        self.chunks[key] = value
        return value

    def block(self, x, y, z):
        chunk = self.chunk(x // 16, z // 16)
        section = next((s for s in chunk.get("sections", chunk.get("Sections", [])) if s["Y"] == y // 16), {})
        position = (y % 16) * 256 + (z % 16) * 16 + x % 16
        if "Blocks" in section:
            def nibble(name):
                data = section.get(name, b"")
                return ((data[position // 2] >> (4 * (position % 2))) & 15) if data else 0
            return {"legacy_id": section["Blocks"][position] + (nibble("Add") << 8), "metadata": nibble("Data")}
        states = section.get("block_states", {})
        palette = states.get("palette", section.get("Palette", []))
        if not palette:
            return {"Name": "minecraft:air"}
        packed = states.get("data", section.get("BlockStates", []))
        if len(palette) == 1 or not packed:
            return block_state(palette[0])
        bits = max(4, (len(palette) - 1).bit_length())
        # 1.16+ avoids values crossing a 64-bit word; 1.13-1.15 packs continuously.
        padded = "block_states" in section or chunk.get("_data_version", 0) >= 2529
        if padded:
            word, shift = position // (64 // bits), position % (64 // bits) * bits
            value = (packed[word] & ((1 << 64) - 1)) >> shift
        else:
            word, shift = divmod(position * bits, 64)
            value = (packed[word] & ((1 << 64) - 1)) >> shift
            if shift + bits > 64:
                value |= (packed[word + 1] & ((1 << 64) - 1)) << (64 - shift)
        block = palette[value & ((1 << bits) - 1)]
        return block_state(block)

    def block_entity(self, x, y, z):
        chunk = self.chunk(x // 16, z // 16)
        if "_entities_by_position" not in chunk:
            chunk["_entities_by_position"] = {(e["x"], e["y"], e["z"]): e
                for e in chunk.get("block_entities", chunk.get("TileEntities", []))}
        return chunk["_entities_by_position"].get((x, y, z), {})


def component_text(value):
    if isinstance(value, str):
        if value.startswith(('{', '[', '"')):
            try:
                return component_text(json.loads(value))
            except (ValueError, TypeError):
                pass
        return value
    if isinstance(value, dict):
        return value.get("text", "") + "".join(component_text(v) for v in value.get("extra", []))
    if isinstance(value, list):
        return "".join(component_text(v) for v in value)
    return ""


def audit_signs(world, manifest):
    issues = []
    for sign in manifest:
        x, y, z = sign["position"]
        block, entity, below = world.block(x, y, z), world.block_entity(x, y, z), world.block(x, y - 1, z)
        messages = entity.get("front_text", {}).get("messages", [entity.get("Text" + str(i), "") for i in range(1, 5)])
        if block.get("legacy_id") != 63 and block.get("Name") not in {"minecraft:sign", "minecraft:oak_sign"}:
            issues.append(dict(position=[x, y, z], error="missing_sign"))
        elif [component_text(m) for m in messages] != sign["lines"]:
            issues.append(dict(position=[x, y, z], error="sign_text", actual=messages, expected=sign["lines"]))
        elif below.get("legacy_id") == 0 or below.get("Name") in {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}:
            issues.append(dict(position=[x, y, z], error="unsupported_sign"))
    return issues


def audit(row, terrain_check=True):
    folder = lab.ROOT / row["version"]
    record = json.loads((folder / "process.json").read_text()) if (folder / "process.json").exists() else {}
    if (folder / "worker.lock").exists() and any(lab.process_alive(record[k]) for k in ("worker_pid", "server_pid") if k in record):
        raise RuntimeError("Gespeicherte Welt nur bei beendetem Server prüfen: " + row["version"])
    world = World(folder)
    index = json.loads((folder / "arena-index.json").read_text(encoding="utf-8"))
    legacy_ids = {}
    if row["protocol"] < 393:
        import inventory_catalog
        inventory_catalog.stacks(row, lab.catalog(row))
        legacy_ids = json.loads((folder / "inventory-block-ids.json").read_text())
    legacy_names = {value: name for name, value in legacy_ids.items()}
    # These block IDs switch during ordinary scheduled updates; this is not player damage.
    def stable_name(name):
        return {"minecraft:lit_redstone_ore": "minecraft:redstone_ore", "minecraft:lit_redstone_lamp": "minecraft:redstone_lamp",
                "minecraft:powered_repeater": "minecraft:unpowered_repeater", "minecraft:powered_comparator": "minecraft:unpowered_comparator",
                "minecraft:lit_furnace": "minecraft:furnace", "minecraft:unlit_redstone_torch": "minecraft:redstone_torch"}.get(name, name)
    missing, changed = [], []
    for sample in index["blocks"]:
        actual = world.block(*sample["position"])
        if "legacy_id" in actual:
            actual = dict(actual, Name=legacy_names.get(actual["legacy_id"], "unknown:" + str(actual["legacy_id"])))
        absent = actual.get("Name") in {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"} or actual.get("legacy_id") == 0
        if absent and sample["name"] not in {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}:
            missing.append(dict(sample, actual=actual))
        elif stable_name(actual.get("Name", sample["name"])) != stable_name(sample["name"]):
            changed.append(dict(sample, actual=actual))
    result = dict(version=row["version"], time=time.time(), samples=len(index["blocks"]),
                  missing=missing, changed_type=changed,
                  unexpected=sum(unexpected(s) for s in missing + changed),
                  missing_types=dict(Counter(s["name"] for s in missing)),
                  scope="Saved block identity (normal legacy powered/lit transitions allowed), plus managed sign texts/supports. Does not validate every property/NBT variant or rendering.")
    manifest = folder / "sign-manifest.json"
    if manifest.exists():
        signs = json.loads(manifest.read_text(encoding="utf-8"))
        result.update(signs_checked=len(signs), sign_issues=audit_signs(world, signs))
    if terrain_check:
        import terrain
        ground = terrain.inspect(row)
        lab.save(folder / 'terrain-audit.json', ground)
        result.update(terrain_intrusions=ground['intrusions'], generator_flat=ground['flat'])
        result['scope'] += ' Also scans extra natural terrain and the saved generator inside the reserved exhibition.'
    lab.save(folder / "world-audit.json", result)
    print(row["version"], "Blockabgleich:", result["unexpected"], "unerwartete Abweichungen;",
          len(missing) + len(changed) - result["unexpected"], "kurzlebige/gleichwertige Zustaende", flush=True)
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("versions", nargs="?", default="all")
    args = parser.parse_args()
    results = [audit(row) for row in lab.select(args.versions)]
    lab.save(lab.ROOT / "world-audit-summary.json", [dict(version=r["version"], samples=r["samples"],
             missing=len(r["missing"]), changed_type=len(r["changed_type"]), unexpected=r['unexpected'],
             sign_issues=len(r.get('sign_issues', [])), terrain_intrusions=r['terrain_intrusions'],
             generator_flat=r['generator_flat']) for r in results])
    if any(r['unexpected'] or r.get('sign_issues') or r['terrain_intrusions'] or not r['generator_flat'] for r in results):
        raise SystemExit(1)
