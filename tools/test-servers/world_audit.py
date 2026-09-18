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
            return palette[0]
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
        return palette[value & ((1 << bits) - 1)]


def audit(row):
    folder = lab.ROOT / row["version"]
    record = json.loads((folder / "process.json").read_text()) if (folder / "process.json").exists() else {}
    if (folder / "worker.lock").exists() and any(lab.process_alive(record[k]) for k in ("worker_pid", "server_pid") if k in record):
        raise RuntimeError("Gespeicherte Welt nur bei beendetem Server prüfen: " + row["version"])
    world = World(folder)
    index = json.loads((folder / "arena-index.json").read_text(encoding="utf-8"))
    missing, changed = [], []
    for sample in index["blocks"]:
        actual = world.block(*sample["position"])
        absent = actual.get("Name") in {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"} or actual.get("legacy_id") == 0
        if absent and sample["name"] not in {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}:
            missing.append(dict(sample, actual=actual))
        elif actual.get("Name", sample["name"]) != sample["name"]:
            changed.append(dict(sample, actual=actual))
    result = dict(version=row["version"], time=time.time(), samples=len(index["blocks"]),
                  missing=missing, changed_type=changed,
                  unexpected=sum(unexpected(s) for s in missing + changed),
                  missing_types=dict(Counter(s["name"] for s in missing)),
                  scope="Saved block presence; modern block identity. Does not validate every property/NBT variant or rendering.")
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
             missing=len(r["missing"]), changed_type=len(r["changed_type"])) for r in results])
