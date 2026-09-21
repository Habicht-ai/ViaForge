"""Saved-generator audit and bounded terrain repair. Call mutations only after backup."""
import copy
import gzip
import json
import re
import struct
import zipfile
import time

import lab
import world_audit
from server_list import Reader, utf


def flat_settings(row):
    return {"biome": "minecraft:plains", "layers": [
        {"block": "minecraft:bedrock", "height": 1},
        {"block": "minecraft:stone", "height": 123 if row["protocol"] >= 757 else 59},
        {"block": "minecraft:dirt", "height": 3}, {"block": "minecraft:grass_block", "height": 1}],
        "features": False, "lakes": False,
        **({"structure_overrides": []} if row["protocol"] >= 758 else {"structures": {"structures": {}}})}


def prepare_new_world(row):
    """Work around vanilla's ignored flat options / 1.18.2 RegistryOps bug before any chunks exist."""
    if not (393 <= row['protocol'] < 735 or row['protocol'] == 758): return False
    folder = lab.ROOT / row['version']
    world = folder / 'world'
    if world.exists(): return False  # Never replace or bootstrap an existing world, even an incomplete one.
    with zipfile.ZipFile(folder / 'server.jar') as jar:
        # The three 1.13 releases predate version.json; values verified in their original saved levels.
        data_version = json.loads(jar.read('version.json'))['world_version'] if 'version.json' in jar.namelist() else {393:1519,401:1628,404:1631}[row['protocol']]
    settings = flat_settings(row)
    if row['protocol'] < 735:
        settings['structures'] = {}
        settings.pop('features'); settings.pop('lakes')
    data = dict(DataVersion=data_version, generatorName='flat', generatorVersion=0,
                generatorOptions=settings, GameType=1, MapFeatures=False, initialized=False,
                hardcore=False, allowCommands=True, version=19133, LevelName='world')
    if row['protocol'] == 758:
        for key in ('generatorName', 'generatorVersion', 'generatorOptions'): data.pop(key)
        data['WorldGenSettings'] = dict(seed=764189, generate_features=False, bonus_chest=False, dimensions={
            'minecraft:overworld': dict(type='minecraft:overworld', generator=dict(type='minecraft:flat', settings=settings)),
            'minecraft:the_nether': dict(type='minecraft:the_nether', generator=dict(type='minecraft:noise', seed=764189,
                settings='minecraft:nether', biome_source=dict(type='minecraft:multi_noise', preset='minecraft:nether'))),
            'minecraft:the_end': dict(type='minecraft:the_end', generator=dict(type='minecraft:noise', seed=764189,
                settings='minecraft:end', biome_source=dict(type='minecraft:the_end', seed=764189)))})
    _, payload = encode(data)
    # The seed is a LONG, not an INT; vanilla reads the NBT numeric type.
    seed = b'\x04' + utf('RandomSeed') + struct.pack('>q', 764189)
    world.mkdir()
    (world / 'level.dat').write_bytes(gzip.compress(b'\x0a\0\0\x0a' + utf('Data') + payload[:-1] + seed + b'\0\0'))
    return True


def generator(row):
    folder = lab.ROOT / row["version"] / "world"
    path = folder / ("data/minecraft/world_gen_settings.dat" if row["protocol"] >= 775 else "level.dat")
    if row['protocol'] >= 775 and not path.exists():
        # Paper 26.x moves per-dimension saved data together with the chunk regions.
        path = folder / 'dimensions/minecraft/overworld/data/minecraft/world_gen_settings.dat'
    root = world_audit.nbt(gzip.decompress(path.read_bytes()))
    if row["protocol"] >= 775:
        data = root.get("data", root.get("Data", root))
        return path, ["data", "dimensions", "minecraft:overworld", "generator"], data["dimensions"]["minecraft:overworld"]["generator"]
    data = root["Data"]
    if row["protocol"] >= 735:
        return path, ["Data", "WorldGenSettings", "dimensions", "minecraft:overworld", "generator"], data["WorldGenSettings"]["dimensions"]["minecraft:overworld"]["generator"]
    return path, ["Data", "generatorOptions"], {"type": data.get("generatorName"), "settings": data.get("generatorOptions")}


def encode(value):
    if isinstance(value, bool): return 1, bytes([value])
    if isinstance(value, int): return 3, struct.pack(">i", value)
    if isinstance(value, str): return 8, utf(value)
    if isinstance(value, dict):
        return 10, b"".join(bytes([kind]) + utf(key) + payload for key, v in value.items() for kind, payload in [encode(v)]) + b"\0"
    if isinstance(value, list):
        entries = [encode(v) for v in value]
        kind = entries[0][0] if entries else 8
        assert all(k == kind for k, _ in entries)
        return 9, bytes([kind]) + struct.pack(">i", len(entries)) + b"".join(p for _, p in entries)
    raise TypeError(value)


def replace_tag(raw, path, value):
    """Splice one named NBT tag, preserving every other byte and numeric tag type."""
    reader = Reader(raw)
    assert reader.read(1) == b"\x0a"
    reader.string()
    def locate(depth):
        while True:
            start = reader.pos
            kind = reader.read(1)[0]
            if not kind: raise ValueError("Missing NBT path: " + "/".join(path))
            name = reader.string()
            if name == path[depth]:
                if depth == len(path) - 1:
                    reader.payload(kind)
                    return start, reader.pos
                if kind != 10: raise ValueError("Expected compound")
                return locate(depth + 1)
            reader.payload(kind)
    start, end = locate(0)
    kind, payload = encode(value)
    return raw[:start] + bytes([kind]) + utf(path[-1]) + payload + raw[end:]


def fix_generator(row):
    path, keys, old = generator(row)
    if row["protocol"] >= 735:
        wanted = {"type": "minecraft:flat", "settings": flat_settings(row)}
        if old == wanted: return False
        path.write_bytes(gzip.compress(replace_tag(gzip.decompress(path.read_bytes()), keys, wanted)))
    else:
        if row["protocol"] < 393:
            wanted = "3;minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass;1;"
        else:
            wanted = flat_settings(row)
            wanted["structures"] = {}
            wanted.pop("features", None); wanted.pop("lakes", None)
        if old["settings"] == wanted and old['type'] == 'flat': return False
        raw = replace_tag(gzip.decompress(path.read_bytes()), keys, wanted)
        raw = replace_tag(raw, ['Data', 'generatorName'], 'flat')
        path.write_bytes(gzip.compress(raw))
    return True


def properties(text, row):
    values = {"level-type": "minecraft:flat" if row["protocol"] >= 759 else "flat"}
    if row["protocol"] >= 735:
        values["generator-settings"] = json.dumps(flat_settings(row), separators=(",", ":"))
    else:
        values["generator-settings"] = "3;minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass;1;" if row["protocol"] < 393 else "minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass_block;minecraft:plains;"
    for key, value in values.items():
        pattern = r"(?m)^" + key + r"=.*$"
        text = re.sub(pattern, lambda _: key + "=" + value, text) if re.search(pattern, text) else text + "\n" + key + "=" + value + "\n"
    return text


def protected_positions(a, index):
    import world_refine
    repair = type(a)(a.row)
    for sample in index["blocks"]: world_refine.repair(repair, sample)
    commands = a.commands + repair.commands
    protected = set()
    for command in commands:
        fields = command.split()
        if fields[0] == "setblock":
            position = tuple(map(int, fields[1:4]))
            if fields[4].split("[", 1)[0].removeprefix("minecraft:") != "air": protected.add(position)
            else: protected.discard(position)
        elif fields[0] == "fill":
            x1, y1, z1, x2, y2, z2 = map(int, fields[1:7])
            for y in range(max(64, min(y1,y2)), min(319, max(y1,y2)) + 1):
                for z in range(z1, z2 + 1):
                    for x in range(x1, x2 + 1):
                        if fields[7].removeprefix("minecraft:") != "air": protected.add((x, y, z))
                        else: protected.discard((x, y, z))
    # Retain actual chests, specimen cells, paired plant parts, and all mob pens.
    for sample in index["blocks"]:
        x, y, z = sample["position"]
        protected.add((x, y, z))
    for sign in a.signs.values():
        x, y, z = sign["position"]; protected.add((x, y - 1, z)); protected.add((x, y, z))
    return protected


def natural(block):
    block = world_audit.block_state(block)
    if "legacy_id" in block:
        return block["legacy_id"] in {1, 2, 3, 8, 9, 10, 11, 12, 13, 17, 18, 31, 32, 37, 38, 39, 40, 78, 79, 80, 81, 82, 83, 106, 110, 161, 162, 175}
    name = block.get("Name", "").removeprefix("minecraft:")
    return name in {"water", "lava", "stone", "dirt", "grass_block", "coarse_dirt", "podzol", "rooted_dirt", "sand", "gravel", "granite", "diorite", "andesite", "clay", "snow", "snow_block", "ice", "packed_ice", "grass", "short_grass", "tall_grass", "fern", "large_fern", "vine", "dandelion", "poppy", "brown_mushroom", "red_mushroom", "sugar_cane", "cactus", "dead_bush", "seagrass", "tall_seagrass", "deepslate", "tuff", "calcite", "dripstone_block", "pointed_dripstone", "moss_block", "moss_carpet", "mud", "muddy_mangrove_roots", "mangrove_roots", "azalea", "flowering_azalea", "red_sand", "sandstone", "red_sandstone", "mycelium", "blue_ice", "powder_snow", "bamboo", "bamboo_sapling", "brown_mushroom_block", "red_mushroom_block", "mushroom_stem"} or name.endswith(("_leaves", "_log", "_ore"))


def reserved(x, y, z):
    # Include the air column above all four galleries and the Elytra takeoff platform.
    # A saved world's actual sections determine whether its ceiling is 255 or 319.
    return -80 <= x <= 79 and -112 <= z <= 111 and 64 <= y <= 319


def intrusion_positions(world, protected):
    """Decode only sections with natural material; inspect extra blocks, not only exhibits."""
    for cx in range(-5, 5):
        for cz in range(-7, 7):
            chunk = world.chunk(cx, cz)
            for section in chunk.get("sections", chunk.get("Sections", [])):
                sy = section["Y"]
                if not 4 <= sy <= 19: continue
                palette = section.get("block_states", {}).get("palette", section.get("Palette", []))
                if palette and not any(natural(b) for b in palette): continue
                if "Blocks" in section and not any(b in {1, 2, 3, 8, 9, 10, 11, 12, 13, 17, 18, 31, 32, 37, 38, 39, 40, 78, 79, 80, 81, 82, 83, 106, 110, 161, 162, 175} for b in section["Blocks"]): continue
                for y in range(sy * 16, sy * 16 + 16):
                    for z in range(cz * 16, cz * 16 + 16):
                        for x in range(cx * 16, cx * 16 + 16):
                            if (x, y, z) not in protected and natural(world.block(x, y, z)):
                                # Unknown container/entity data is never removed by a terrain classification.
                                if not world.block_entity(x, y, z): yield (x, y, z)


def removal_commands(positions):
    rows = {}
    for x, y, z in positions:
        if not reserved(x, y, z): raise ValueError("Outside reserved exhibition")
        rows.setdefault((y, z), []).append(x)
    result = []
    for (y, z), xs in sorted(rows.items(), reverse=True):
        xs = sorted(set(xs)); start = end = xs[0]
        for x in xs[1:] + [1000000]:
            if x == end + 1: end = x; continue
            result.append(f"fill {start} {y} {z} {end} {y} {z} air")
            start = end = x
    return result


def inspect(row, generated=None, index=None):
    import arena
    folder = lab.ROOT / row["version"]
    if generated is None:
        generated = arena.Arena(row); generated.generate()
        index = json.loads((folder / "arena-index.json").read_text(encoding="utf-8"))
        generated.fixtures(copy.deepcopy(index))
    protected = protected_positions(generated, index)
    world = world_audit.World(folder)
    positions = list(intrusion_positions(world, protected))
    missing_chunks = [[x,z] for x in range(-5,5) for z in range(-7,7) if not world.chunk(x,z)]
    generator_path, generator_keys, gen = generator(row)
    configured = gen.get('type') in ('flat', 'minecraft:flat')
    if row['protocol'] >= 393:
        configured = configured and gen.get('settings', {}).get('layers') == flat_settings(row)['layers']
    return {"version": row["version"], "time": time.time(), "generator": gen, "flat": configured,
            "world_path": str((folder / 'world').resolve()), "region_path": str(world.region.resolve()),
            "generator_path": str(generator_path.resolve()), "generator_nbt_path": generator_keys,
            "chunks_checked": 140 - len(missing_chunks), "missing_chunks": missing_chunks,
            "section_heights": sorted({s['Y'] for c in world.chunks.values() for s in c.get('sections',c.get('Sections',[]))}),
            "intrusions": len(positions), "positions": positions,
            "scope": "Natural terrain in reserved x=-80..79,z=-112..111,y=64..319 (actual saved build height); managed specimens/supports, tanks, chests and mob pens excluded. Outside terrain is retained."}


def boundary_commands(world, protected=()):
    # Keep outside lakes/builds intact. Contain inflow on our side of the boundary,
    # only where terrain removal leaves air, and never inside a managed specimen.
    result = []
    edge = [(x, z) for x in (-81, 80) for z in range(-112, 112)]
    edge += [(x, z) for z in (-113, 112) for x in range(-80, 80)]
    for x, z in edge:
        inside_x, inside_z = max(-80,min(79,x)), max(-112,min(111,z))
        for y in range(64, 320):
            b = world.block(x, y, z)
            if b.get("Name") in {"minecraft:water", "minecraft:lava"} or b.get("legacy_id") in {8,9,10,11}:
                position=(inside_x,y,inside_z)
                inner=world.block(*position)
                air=inner.get('legacy_id')==0 or inner.get('Name') in {'minecraft:air','minecraft:cave_air','minecraft:void_air'}
                if position not in protected and not world.block_entity(*position) and (air or natural(inner)):
                    result.append(f"setblock {inside_x} {y} {inside_z} glass")
    return result
