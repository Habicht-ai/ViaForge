"""Version-aware Vanilla commands for a persistent, labelled regression world."""
import json
import re
import time
from pathlib import Path
import lab

REVISION = 4


def quoted(value):
    return json.dumps(str(value), ensure_ascii=True)


class Arena:
    def __init__(self, row):
        self.row = row
        self.p = row["protocol"]
        self.legacy = self.p < 393
        self.catalog = lab.catalog(row)
        self.commands = []
        self.index = {"version": row["version"], "revision": REVISION, "blocks": [], "items": [], "mobs": [], "stations": []}
        self.names = {b["name"].removeprefix("minecraft:") for b in self.catalog["blocks"]}

    def add(self, command):
        self.commands.append(command)

    def block(self, x, y, z, name, meta=0, nbt=""):
        if nbt:
            self.add(f"setblock {x} {y} {z} air")
        self.add(f"setblock {x} {y} {z} {name}" + (f" {meta} replace {nbt}" if self.legacy else nbt))

    def fill(self, x1, y1, z1, x2, y2, z2, block):
        self.add(f"fill {x1} {y1} {z1} {x2} {y2} {z2} {block}")

    def sign(self, x, y, z, lines):
        lines = (list(lines) + [""] * 4)[:4]
        if self.p >= 763:
            if self.p >= 770:
                messages = ",".join("{text:" + quoted(line) + "}" for line in lines)
            else:
                messages = ",".join(quoted(json.dumps({"text": line})) for line in lines)
            nbt = '{front_text:{messages:[' + messages + ']},is_waxed:1b}'
        else:
            nbt = "{" + ",".join(f"Text{i+1}:" + quoted(json.dumps({"text": line})) for i, line in enumerate(lines)) + "}"
        name = "standing_sign" if self.legacy else "sign" if self.p <= 404 else "oak_sign"
        if not self.legacy:
            name += "[rotation=8]"
        self.block(x, y, z, name, 8, nbt)

    def labelled(self, x, y, z, name, subtitle=""):
        words = name.removeprefix("minecraft:").split("_")
        lines = [""]
        for word in words:
            if len(lines[-1]) + len(word) + 1 > 17 and len(lines) < 3:
                lines.append("")
            lines[-1] += (" " if lines[-1] else "") + word
        self.sign(x, y, z, lines[:3] + [subtitle])

    def button(self, x, y, z, title, command):
        self.block(x, y, z, "command_block", nbt="{Command:" + quoted(command) + ",TrackOutput:0b}")
        self.block(x, y + 1, z, "stone_button" if self.legacy else "stone_button[face=floor]", 5)
        self.sign(x, y + 1, z - 1, title)

    def rules(self):
        wanted = [("doDaylightCycle", "advance_time", "false"), ("doWeatherCycle", "advance_weather", "false"),
                  ("doMobSpawning", "spawn_mobs", "false"), ("mobGriefing", "mob_griefing", "false"),
                  ("doTileDrops", "block_drops", "false"), ("doEntityDrops", "entity_drops", "false"),
                  ("keepInventory", "keep_inventory", "true"), ("randomTickSpeed", "random_tick_speed", "0"),
                  ("commandBlockOutput", "command_block_output", "false"), ("logAdminCommands", "log_admin_commands", "false"),
                  ("spawnRadius", "respawn_radius", "0"), ("doFireTick", "fire_spread_radius_around_player", "false")]
        report = lab.ROOT / self.row["version"] / "data/reports/commands.json"
        available = json.loads(report.read_text())["children"]["gamerule"]["children"] if report.exists() else None
        for old, new, value in wanted:
            key = old
            if available is not None and old not in available:
                if new not in available:
                    continue
                key = new
                if new == "fire_spread_radius_around_player":
                    value = "0"
            # doWeatherCycle was introduced in 1.11; do not create a fake legacy rule.
            if self.p < 315 and old == "doWeatherCycle":
                continue
            self.add(f"gamerule {key} {value}")
        if available:
            for rule in ["doTraderSpawning", "doPatrolSpawning", "doInsomnia", "spawn_wandering_traders", "spawn_patrols", "spawn_phantoms"]:
                if rule in available:
                    self.add(f"gamerule {rule} false")
        self.add("time set 6000")
        self.add("weather clear 1000000")
        self.add("setworldspawn 0 177 0")

    def floors(self):
        # Modest footprint and several floors keep the full catalog inside legacy spawn chunks.
        for y in [63, 87, 111, 143]:
            for x in range(-80, 80, 32):
                for z in range(-80, 112, 32):
                    self.fill(x, y, z, x + 31, y, z + 31, "stonebrick" if self.legacy else "stone_bricks")
        for y in [64, 88, 112, 144]:
            for x in range(-80, 80, 16):
                for z in range(-80, 112, 16):
                    self.block(x, y - 1, z, "sea_lantern")
        self.fill(-20, 63, -112, 40, 63, -82, "quartz_block")
        self.fill(-12, 175, -12, 12, 175, 12, "quartz_block")
        self.sign(0, 176, -3, ["VIAFORGE LAB", self.row["version"], "START", "Knopf druecken"])
        self.button(0, 176, 0, ["Teststationen", "und Navigation"], "tp @p 0 65 -100")
        self.sign(0, 64, -104, ["VIAFORGE LAB", self.row["version"], "48 echte Server", "nur localhost"])
        destinations = [("Bloecke A", -78, 65, -78), ("Alle Items", -78, 89, -78),
                        ("Mobs", -78, 113, -78), ("Bloecke B", -78, 145, -78),
                        ("Boot / Wasser", 15, 65, -95), ("Boss-Arena", 0, 209, 80)]
        for i, (title, x, y, z) in enumerate(destinations):
            self.button(-15 + i * 5, 64, -98, [title, "Knopf druecken"], f"tp @p {x} {y} {z}")
        for y in [64, 88, 112, 144, 208]:
            self.button(-78 if y < 208 else -5, y, -76 if y < 208 else 75,
                        ["Zurueck", "zum Start"], "tp @p 0 65 -100")

    def blocks(self):
        samples = []
        for block in self.catalog["blocks"]:
            for meta in (block["metadata"] if self.legacy else [0]):
                samples.append((block, meta))
        if len(samples) > 2240:
            raise ValueError("Blockkatalog passt nicht in die zwei Ausstellungsstockwerke")
        for i, (block, meta) in enumerate(samples):
            floor, local = divmod(i, 1120)
            x, z, y = -76 + local % 32 * 5, -68 + local // 32 * 5, 64 + floor * 80
            name = block["name"].removeprefix("minecraft:")
            self.labelled(x, y, z - 1, name, f"Meta {meta}" if self.legacy else f"#{i + 1}")
            if name in {"water", "flowing_water", "lava", "flowing_lava", "bubble_column"}:
                self.fill(x - 1, y, z, x + 1, y, z + 2, "glass")
            # Soil, water and wall support for fragile blocks. The index retains every registry entry,
            # including technical/invisible states; those cannot all be permanent standalone exhibits.
            if name in {"wheat", "carrots", "potatoes", "beetroots", "melon_stem", "pumpkin_stem", "torchflower_crop", "pitcher_crop"}:
                self.block(x, y - 1, z + 1, "farmland")
            elif name in {"cactus", "dead_bush", "deadbush"}:
                self.block(x, y - 1, z + 1, "sand")
            elif name == "nether_wart":
                self.block(x, y - 1, z + 1, "soul_sand")
            elif any(t in name for t in ["sapling", "flower", "tulip", "grass", "fern", "bush", "mushroom", "roots"]) or name in {"dandelion", "poppy", "allium", "lilac", "peony", "sunflower"}:
                self.block(x, y - 1, z + 1, "dirt")
            state = "minecraft:" + name
            if not self.legacy:
                props = dict(block.get("properties", {}))
                if "persistent" in props:
                    props["persistent"] = "true"
                if "distance" in props and name.endswith("leaves"):
                    props["distance"] = "1"
                if props.get("half") == "upper":
                    props["half"] = "lower"
                if props.get("face") == "wall":
                    props["face"] = "floor"
                if props:
                    state += "[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]"
            # Apply the same support/container rules as existing-world repairs.
            from world_refine import repair, TRANSIENT
            sample = {"name": block["name"], "metadata": meta, "position": [x, y, z + 1], "state": state}
            repair(self, sample) if sample["name"] not in TRANSIENT else self.block(x, y, z + 1, state, meta)
            if not self.legacy and name.endswith("_bed"):
                props = dict(block.get("properties", {}))
                props["part"] = "head"
                dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[props["facing"]]
                self.block(x + dx, y, z + 1 + dz, name + "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]")
            self.index["blocks"].append({"name": block["name"], "metadata": meta, "position": [x, y, z + 1], "state": state})

    def items(self):
        items = []
        block_variants = {b["name"]: b.get("metadata", [0]) for b in self.catalog["blocks"]}
        for entry in self.catalog["items"]:
            if entry["name"] == "minecraft:air":
                continue
            for meta in block_variants.get(entry["name"], [0]) if self.legacy else [0]:
                items.append((entry["name"], meta, ""))
        if self.legacy:
            items += [("minecraft:spawn_egg", 0, "{EntityTag:{id:" + quoted(mob) + "}}")
                      for mob in self.catalog["mobs"] if "dragon" not in mob.lower() and "wither" != mob.lower()]
        for i in range(0, len(items), 27):
            number = i // 27
            x, y, z = -72 + number % 24 * 6, 88, -65 + number // 24 * 7
            stacks = []
            for slot, (name, meta, tag) in enumerate(items[i:i + 27]):
                stack = "{Slot:" + str(slot) + "b,id:" + quoted(name)
                stack += ",count:1" if self.p >= 766 else ",Count:1b"
                if self.legacy:
                    stack += ",Damage:" + str(meta) + "s"
                if tag:
                    stack += ",tag:" + tag
                stacks.append(stack + "}")
                self.index["items"].append({"name": name, "metadata": meta, "chest": [x, y, z], "slot": slot})
            # /setblock on an existing identical chest clears its inventory before rejecting the
            # unchanged state in some releases. Replace the block explicitly before restoring NBT.
            self.block(x, y, z, "chest", 2, "{Items:[" + ",".join(stacks) + "]}")
            self.sign(x, y, z - 1, ["ITEMKATALOG", f"Kiste {number + 1}", f"{i + 1} - {min(i + 27, len(items))}", "alle Items"])
        self.sign(-70, 88, -73, ["Inventartests", "Q / Strg+Q", "Shift / Ziehen", "Offhand / Stapel"])

    def mobs(self):
        self.fill(-24, 207, 56, 24, 207, 104, "bedrock")
        for i, entity in enumerate(self.catalog["mobs"]):
            name = entity.removeprefix("minecraft:")
            boss = name.lower().replace("_", "") in {"enderdragon", "wither", "witherboss"}
            tag = "vflab_boss" if boss else f"vflab_mob_{i}"
            x, y, z = -70 + (i % 12) * 12, 112, -62 + (i // 12) * 16
            self.fill(x - 3, y - 1, z - 3, x + 3, y - 1, z + 3, "quartz_block")
            self.fill(x - 3, y + 7, z - 3, x + 3, y + 7, z + 3, "sea_lantern")
            if name.lower().replace("_", "") in {"squid", "glowsquid", "guardian", "elderguardian", "cod", "salmon", "pufferfish", "tropicalfish", "dolphin", "axolotl", "tadpole", "nautilus", "zombienautilus"}:
                self.fill(x - 2, y, z - 2, x + 2, y + 2, z + 2, "glass")
                self.fill(x - 1, y, z - 1, x + 1, y + 2, z + 1, "water")
            nbt = '{NoAI:1b,NoGravity:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,IsImmuneToZombification:1b,Tags:[' + quoted(tag) + ',"vflab_mob"],CustomNameVisible:1b'
            nbt += ",CustomName:" + (quoted(name) if self.legacy else "{text:" + quoted(name) + "}" if self.p >= 770 else quoted(json.dumps({"text": name})))
            if name in {"vex", "evocation_illager", "wandering_trader", "trader_llama"}:
                nbt += ",LifeTicks:2147483647,DespawnDelay:2147483647"
            if name in {"slime", "magma_cube", "LavaSlime", "Slime"}:
                nbt += ",Size:1"
            nbt += "}"
            if boss:
                self.button(x, y, z, [name, "Boss-Arena", "Spawn auf Knopf"], f"summon {entity} 0 210 80 {nbt}")
            else:
                self.add(f"summon {entity} {x + .5} {y + .1} {z + .5} {nbt}")
                self.labelled(x, y, z - 4, name, "Mob-Test")
            self.index["mobs"].append({"name": entity, "position": [x, y, z], "tag": tag, "on_demand": boss})
        self.button(5, 208, 75, ["Bosse entfernen"], "kill @e[tag=vflab_boss]")

    def stations(self):
        self.fill(15, 63, -110, 38, 63, -85, "glass")
        self.fill(16, 63, -109, 37, 63, -86, "water")
        self.sign(20, 64, -111, ["WASSER / BOOTE", "Farbe / Alpha", "2 Passagiere", "Ein-/Aussteigen"])
        boats = [name["name"] for name in self.catalog["items"] if name["name"].endswith("boat") or name["name"] in {"minecraft:boat", "minecraft:bamboo_raft"}]
        self.index["stations"].append({"name": "water_boats", "position": [20, 64, -95], "items": boats})
        self.button(-15, 64, -87, ["Survival", "Schild-Q-Test"], "gamemode survival @p" if not self.legacy else "gamemode 0 @p")
        self.button(-10, 64, -87, ["Creative"], "gamemode creative @p" if not self.legacy else "gamemode 1 @p")
        self.button(-5, 64, -87, ["Schild geben"], "give @p minecraft:shield 1")
        self.button(0, 64, -87, ["Boot geben"], "give @p " + (boats[0] if boats else "minecraft:boat") + " 1")
        for i, block in enumerate(["glass", "stained_glass" if self.legacy else "red_stained_glass", "leaves" if self.legacy else "oak_leaves", "grass" if self.legacy else "grass_block", "ice", "packed_ice"]):
            self.block(-15 + i * 4, 64, -108, block)
        self.sign(-17, 64, -110, ["RENDERTEST", "Glas / Laub", "Gras / Eis", "Farbe / Alpha"])
        if self.p >= 757:
            for y in [-60, 300]:
                self.fill(45, y, -5, 55, y + 5, 5, "air")
                self.fill(45, y - 1, -5, 55, y - 1, 5, "sea_lantern")
                self.button(50, y, 0, ["Zurueck"], "tp @p 0 65 -100")
                self.index["stations"].append({"name": "world_height", "position": [50, y, 0]})
            self.button(5, 64, -87, ["Hoehe -60", "ab 1.18"], "tp @p 50 -59 0")
            self.button(10, 64, -87, ["Hoehe 300", "ab 1.18"], "tp @p 50 301 0")

    def generate(self):
        self.rules()
        self.floors()
        self.blocks()
        self.items()
        self.mobs()
        self.stations()
        from world_refine import wayfinding
        wayfinding(self)
        return self.commands

    def fixtures(self, index):
        """Small, repeatable refinements to the original reserved exhibition cells."""
        for sample in index["blocks"]:
            name = sample["name"].removeprefix("minecraft:")
            if name not in {"water", "flowing_water", "lava", "flowing_lava", "bubble_column"}:
                continue
            x, y, z = sample["position"]
            self.fill(x - 1, y, z - 1, x + 1, y, z + 1, "glass")
            self.block(x, y, z, sample["state"], sample["metadata"])
        aquatic = {"squid", "glowsquid", "guardian", "elderguardian", "cod", "salmon", "pufferfish", "tropicalfish", "dolphin", "axolotl", "tadpole", "nautilus", "zombienautilus"}
        for mob in index["mobs"]:
            name = mob["name"].removeprefix("minecraft:").lower().replace("_", "")
            if name in aquatic:
                x, y, z = mob["position"]
                self.fill(x - 2, y, z - 2, x + 2, y + 2, z + 2, "glass")
                self.fill(x - 1, y, z - 1, x + 1, y + 2, z + 1, "water")
            if mob["name"] == "minecraft:trader_llama":
                self.add("kill @e[tag=" + mob["tag"] + "]")
                x, y, z = mob["position"]
                self.add(f"summon minecraft:trader_llama {x+.5} {y+.1} {z+.5} " +
                         '{NoAI:1b,NoGravity:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,DespawnDelay:2147483647,Tags:[' +
                         quoted(mob["tag"]) + ',"vflab_mob"]}')
        self.fill(-12, 175, -12, 12, 175, 12, "quartz_block")
        self.sign(0, 176, -3, ["VIAFORGE LAB", self.row["version"], "START", "Knopf druecken"])
        self.button(0, 176, 0, ["Teststationen", "und Navigation"], "tp @p 0 65 -100")
        self.add("setworldspawn 0 177 0")
        # Vanilla chooses the highest safe block when a new player joins. Keep the boss floor
        # clear of the spawn column, otherwise first login lands in the boss arena.
        self.fill(-24, 207, -24, 24, 209, 24, "air")
        self.fill(-24, 207, 56, 24, 207, 104, "bedrock")
        self.button(-5, 208, 75, ["Zurueck", "zum Start"], "tp @p 0 65 -100")
        self.button(5, 208, 75, ["Bosse entfernen"], "kill @e[tag=vflab_boss]")
        self.button(10, 64, -98, ["Boss-Arena", "Knopf druecken"], "tp @p 0 209 80")
        for mob in index["mobs"]:
            if mob["on_demand"]:
                x, y, z = mob["position"]
                nbt = '{NoAI:1b,NoGravity:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,Tags:["vflab_boss","vflab_mob"]}'
                self.button(x, y, z, [mob["name"].removeprefix("minecraft:"), "Boss-Arena", "Spawn auf Knopf"], f"summon {mob['name']} 0 210 80 {nbt}")
        colors = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
        for i, color in enumerate(colors if self.p >= 335 else ["red"]):
            x, y, z = -70 + i * 5, 88, 8
            if self.legacy:
                nbt = "{color:" + str(i if self.p >= 335 else 14) + "}" if self.p >= 335 else ""
                self.block(x, y, z, "bed", 0, nbt)
                self.block(x, y, z + 1, "bed", 8, nbt)
            else:
                for part, dz in [("foot", 0), ("head", 1)]:
                    self.block(x, y, z + dz, color + "_bed[facing=south,part=" + part + "]")
            self.labelled(x, y, z - 1, color + "_bed", "BETTEN-TEST")
        for i, color in enumerate(colors):
            name = color + "_shulker_box"
            if self.legacy and color == "light_gray":
                name = "silver_shulker_box"
            if name in self.names:
                self.block(-70 + i * 5, 88, 20, name, 1)
                self.labelled(-70 + i * 5, 88, 19, name, "OEFFNEN / ABBAU")
        self.sign(-72, 88, 3, ["MODELLTEST", "Betten / Shulker", "Hand / GUI / F5", "Platzieren / Q"])
        variants = []
        if self.p < 315:
            specifications = [("Skeleton", "Wither Skeleton", "SkeletonType:1b"),
                              ("Guardian", "Elder Guardian", "Elder:1b"),
                              ("Zombie", "Zombie Villager", "IsVillager:1b"),
                              ("EntityHorse", "Donkey", "Type:1"), ("EntityHorse", "Mule", "Type:2"),
                              ("EntityHorse", "Zombie Horse", "Type:3"), ("EntityHorse", "Skeleton Horse", "Type:4")]
            if self.p >= 210:
                specifications += [("Skeleton", "Stray", "SkeletonType:2b"), ("Zombie", "Husk", "ZombieType:6")]
            for i, (entity, label, tag_nbt) in enumerate(specifications):
                slot = len(index["mobs"]) + i
                x, y, z = -70 + slot % 12 * 12, 112, -62 + slot // 12 * 16
                tag = f"vflab_variant_{i}"
                self.add(f"kill @e[tag={tag}]")
                self.fill(x - 3, y - 1, z - 3, x + 3, y - 1, z + 3, "quartz_block")
                self.fill(x - 3, y + 7, z - 3, x + 3, y + 7, z + 3, "sea_lantern")
                self.labelled(x, y, z - 4, label, "NBT-VARIANTE")
                self.add(f"summon {entity} {x+.5} {y+.1} {z+.5} " + "{" + tag_nbt + ',NoAI:1b,NoGravity:1b,Invulnerable:1b,PersistenceRequired:1b,Silent:1b,Tags:[' + quoted(tag) + ',"vflab_mob"],CustomName:' + quoted(label) + '}')
                variants.append({"name": entity, "variant": label, "position": [x, y, z], "tag": tag, "on_demand": False})
        index["variants"] = variants
        index["revision"] = REVISION
        from world_refine import wayfinding
        wayfinding(self)
        return self.commands


def error_lines(output):
    patterns = ("Unknown or incomplete", "Incorrect argument", "Expected ", "Invalid ", "Unknown block",
                "Unknown entity", "Unable to summon", "Cannot summon", "Unable to locate", "outside of the world",
                "position is not loaded", "not a valid", "Data tag parsing failed", "An unexpected error", "No key ",
                "/ERROR]", "Exception", "Couldn't save chunk", "Failed to save")
    return [line for line in output.splitlines() if any(p.lower() in line.lower() for p in patterns)]


def build(row):
    folder = lab.ROOT / row["version"]
    report = folder / "arena-report.json"
    if report.exists():
        print(row["version"], "Testwelt bereits gebaut; keine Spielerbauten ueberschrieben", flush=True)
        return
    if not lab.status(row):
        raise RuntimeError("Server zuerst starten: " + row["version"])
    arena = Arena(row)
    commands = arena.generate()
    lab.save(folder / "arena-index.json", arena.index)
    (folder / "arena-commands.txt").write_text("\n".join(commands) + "\n", encoding="utf-8")
    if row["protocol"] >= 401:
        lab.batch(row, ["forceload add -80 -112 95 111"])
        time.sleep(3)
    else:
        lab.batch(row, ["setworldspawn 0 64 0"])
    if row["protocol"] < 401:
        # Pre-1.13.1 has no forceload: restarting OUR worker prepares the vanilla spawn area.
        lab.stop([row])
        lab.start([row])
    errors = []
    # Retry a partially built fresh arena without multiplying its mobs.
    lab.batch(row, ["kill @e[tag=vflab_mob]"])
    for offset in range(0, len(commands), 40):
        output = lab.batch(row, commands[offset:offset + 40])
        errors.extend(error_lines(output))
        if offset % 800 == 0:
            print(row["version"], f"Testgelände {offset}/{len(commands)}", flush=True)
    save_output = lab.batch(row, (["forceload remove all"] if row["protocol"] >= 401 else []) + ["save-all flush"])
    errors.extend(error_lines(save_output))
    lab.save(folder / "arena-build-results.json", {"version": row["version"], "commands": len(commands), "errors": errors})
    if errors:
        raise RuntimeError(f"{row['version']}: {len(errors)} Baufehler; siehe arena-build-results.json")
    lab.save(report, {"version": row["version"], "revision": 1, "commands": len(commands),
                      "block_types": len(arena.catalog["blocks"]), "block_samples": len(arena.index["blocks"]),
                      "item_stacks": len(arena.index["items"]), "living_types": len(arena.index["mobs"]),
                      "bosses_on_demand": sum(m["on_demand"] for m in arena.index["mobs"]), "built": time.time()})
    print("GEBAUT", row["version"], flush=True)


def upgrade(row):
    folder = lab.ROOT / row["version"]
    report = json.loads((folder / "arena-report.json").read_text())
    if report.get("revision", 0) >= REVISION:
        upgrade_heights(row)
        upgrade_controls(row)
        return
    index = json.loads((folder / "arena-index.json").read_text(encoding="utf-8"))
    instance = Arena(row)
    commands = instance.fixtures(index)
    if row["protocol"] >= 401:
        lab.batch(row, ["forceload add -80 -112 95 111"])
        time.sleep(3)
    errors = []
    for offset in range(0, len(commands), 100):
        errors += error_lines(lab.batch(row, commands[offset:offset + 100]))
    if errors:
        lab.save(folder / "fixtures-errors.json", errors)
        raise RuntimeError("Fehler bei Teststationen: " + row["version"])
    save_output = lab.batch(row, (["forceload remove all"] if row["protocol"] >= 401 else []) + ["save-all flush"])
    if error_lines(save_output):
        raise RuntimeError("Speichern der Teststationen fehlgeschlagen: " + row["version"])
    lab.save(folder / "arena-index.json", index)
    report["revision"] = REVISION
    report["legacy_mob_variants"] = len(index["variants"])
    lab.save(folder / "arena-report.json", report)
    print("Teststationen aktualisiert:", row["version"], flush=True)
    upgrade_heights(row)
    upgrade_controls(row)


def upgrade_controls(row):
    folder = lab.ROOT / row["version"]
    report = json.loads((folder / "arena-report.json").read_text())
    if report.get("control_revision") == 1:
        return
    instance = Arena(row)
    instance.generate()
    index = json.loads((folder / "arena-index.json").read_text(encoding="utf-8"))
    instance.fixtures(index)
    controls = {}
    checks = []
    for command in instance.commands:
        fields = command.split(" ", 4)
        if fields[0] != "setblock":
            continue
        block = fields[4].split("[", 1)[0].split("{", 1)[0].split(" ", 1)[0]
        if block in {"command_block", "standing_sign", "sign", "oak_sign", "stone_button"}:
            controls[tuple(fields[1:4])] = (command, block)
    commands, buttons = [], []
    for (x, y, z), (command, block) in controls.items():
        if block == "stone_button":
            buttons.append(command)
            continue
        commands.extend([f"setblock {x} {y} {z} air", command])
        if block == "command_block":
            match = re.search(r'Command:("(?:[^"\\]|\\.)*")', command)
            if not match:
                raise ValueError("Command-NBT fehlt: " + command)
            nbt = "{Command:" + match[1] + "}"
            checks.append(f"testforblock {x} {y} {z} command_block -1 {nbt}" if row["protocol"] < 393 else
                          f"execute if block {x} {y} {z} command_block{nbt} run say VFLAB_CONTROL_OK")
    commands += buttons
    if row["protocol"] >= 401:
        lab.batch(row, ["forceload add -80 -112 95 111"])
        time.sleep(2)
    errors = []
    for offset in range(0, len(commands), 80):
        errors.extend(error_lines(lab.batch(row, commands[offset:offset+80])))
    errors.extend(error_lines(lab.batch(row, ["save-all flush"] + (["forceload remove all"] if row["protocol"] >= 401 else []))))
    if errors:
        raise RuntimeError("Steuerknoepfe/Schilder: " + str(errors[:5]))
    lab.save(folder / "control-checks.json", checks)
    report["control_revision"] = 1
    lab.save(folder / "arena-report.json", report)
    print("Navigation und Schilder aktualisiert:", row["version"], flush=True)


def upgrade_heights(row):
    if row["protocol"] < 757:
        return
    folder = lab.ROOT / row["version"]
    report = json.loads((folder / "arena-report.json").read_text())
    if report.get("height_station_revision") == 1:
        return
    instance = Arena(row)
    instance.fill(-5, 299, -5, 5, 302, 5, "air")
    for y in [-60, 300]:
        instance.fill(45, y, -5, 55, y + 5, 5, "air")
        instance.fill(45, y - 1, -5, 55, y - 1, 5, "sea_lantern")
        instance.button(50, y, 0, ["Zurueck"], "tp @p 0 65 -100")
    instance.button(5, 64, -87, ["Hoehe -60", "ab 1.18"], "tp @p 50 -59 0")
    instance.button(10, 64, -87, ["Hoehe 300", "ab 1.18"], "tp @p 50 301 0")
    lab.batch(row, ["forceload add -80 -112 95 111"])
    time.sleep(2)
    errors = error_lines(lab.batch(row, instance.commands + ["save-all flush", "forceload remove all"]))
    if errors:
        raise RuntimeError("Hoehenstationen: " + str(errors))
    report["height_station_revision"] = 1
    lab.save(folder / "arena-report.json", report)
    index = json.loads((folder / "arena-index.json").read_text(encoding="utf-8"))
    for station in index["stations"]:
        if station["name"] == "world_height":
            station["position"][0] = 50
    lab.save(folder / "arena-index.json", index)
    print("Hoehenstationen ausserhalb der Ankunftssaeule:", row["version"], flush=True)


def verify(row):
    folder = lab.ROOT / row["version"]
    index = json.loads((folder / "arena-index.json").read_text(encoding="utf-8"))
    found = lab.status(row)
    if not found or found["version"]["protocol"] != row["protocol"]:
        raise RuntimeError("Serverstatus/Protokoll stimmt nicht: " + row["version"])
    commands = []
    control_checks = folder / "control-checks.json"
    if control_checks.exists():
        commands += json.loads(control_checks.read_text())
    refinement_checks = folder / "refinement-checks.json"
    if refinement_checks.exists():
        commands += json.loads(refinement_checks.read_text())
    # A first-time player must land on the arrival platform, with no higher floor above it.
    if row["protocol"] < 393:
        commands += ["testforblock 0 175 0 quartz_block", "testforblock 0 207 0 air"]
    else:
        commands += ["execute if block 0 175 0 quartz_block run say VFLAB_BLOCK_OK",
                     "execute if block 0 207 0 air run say VFLAB_BLOCK_OK"]
    if row["protocol"] >= 757:
        commands += ["execute if block 0 299 0 air run say VFLAB_BLOCK_OK",
                     "execute if block 50 299 0 sea_lantern run say VFLAB_BLOCK_OK",
                     "execute if block 50 -58 0 air run say VFLAB_BLOCK_OK"]
    stable = {"minecraft:stone", "minecraft:purpur_block", "minecraft:purpur_stairs", "minecraft:white_shulker_box",
              "minecraft:orange_shulker_box", "minecraft:glass", "minecraft:grass_block", "minecraft:diamond_block"}
    for sample in index["blocks"]:
        if sample["name"] not in stable:
            continue
        x, y, z = sample["position"]
        if row["protocol"] < 393:
            commands.append(f"testforblock {x} {y} {z} {sample['name']} {sample['metadata']}")
        else:
            commands.append(f"execute if block {x} {y} {z} {sample['state']} run say VFLAB_BLOCK_OK")
    for mob in index["mobs"] + index.get("variants", []):
        if mob["on_demand"]:
            continue
        selector = "@e[tag=" + mob["tag"] + "]"
        commands.append(f"testfor {selector}" if row["protocol"] < 393 else f"execute if entity {selector} run say VFLAB_MOB_OK")
    # Validate every catalog chest, including original item IDs, stack counts and legacy damage/NBT.
    for command in (folder / "arena-commands.txt").read_text(encoding="utf-8").splitlines():
        if not re.match(r"setblock -?\d+ 88 -?\d+ chest", command):
            continue
        _, x, y, z, *rest = command.split(" ", 4)
        nbt = command[command.index("{Items:"):]
        if row["protocol"] < 393:
            commands.append(f"testforblock {x} {y} {z} chest -1 {nbt}")
        else:
            commands.append(f"execute if block {x} {y} {z} chest{nbt} run say VFLAB_CHEST_OK")
    if row["protocol"] >= 401:
        lab.batch(row, ["forceload add -80 -112 95 111"])
        time.sleep(3)
    if row["protocol"] >= 393:
        commands = [re.sub(r"VFLAB_(BLOCK|MOB|CHEST|CONTROL)_OK", lambda m: m[0] + "_" + str(i) + "_END", command)
                    for i, command in enumerate(commands)]
    output = lab.batch(row, commands)
    matches = len(re.findall(r"Successfully found the block|Found .+|VFLAB_BLOCK_OK|VFLAB_MOB_OK|VFLAB_CHEST_OK|VFLAB_CONTROL_OK", output))
    if row["protocol"] >= 401:
        lab.batch(row, ["forceload remove all"])
    result = {"version": row["version"], "protocol": found["version"]["protocol"], "checks": len(commands),
              "passed": matches, "success": matches == len(commands), "output": output, "time": time.time(), "schema": 2}
    if row["protocol"] >= 393:
        result["failed_commands"] = [c for c in commands if c.rsplit("run say ", 1)[1] not in output]
    lab.save(folder / "verification.json", result)
    if not result["success"]:
        raise RuntimeError(f"{row['version']}: {matches}/{len(commands)} Weltpruefungen bestanden (verification.json)")
    print("PASS", row["version"], len(commands), "Welt-/Mob-Pruefungen", flush=True)
