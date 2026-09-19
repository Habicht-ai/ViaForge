"""Local, isolated Vanilla test servers. Python standard library only."""
from __future__ import annotations

import argparse
import concurrent.futures
import ctypes
import hashlib
import html
import json
import os
from pathlib import Path
import re
import secrets
import socket
import struct
import subprocess
import sys
import time
import urllib.request
import uuid
import zipfile

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
ROOT = REPO / "run" / "test-servers"
VERSIONS = json.loads((HERE / "versions.json").read_text())
NO_WINDOW = getattr(subprocess, "CREATE_NO_WINDOW", 0)


def save(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + "." + uuid.uuid4().hex + ".tmp")
    tmp.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    tmp.replace(path)


def download(url, path, digest, algorithm="sha1"):
    def matches(p):
        if not p.exists():
            return False
        with p.open("rb") as source:
            return hashlib.file_digest(source, algorithm).hexdigest() == digest
    if matches(path):
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".download")
    for attempt in range(3):
        try:
            with urllib.request.urlopen(url, timeout=90) as source, tmp.open("wb") as target:
                while data := source.read(1024 * 1024):
                    target.write(data)
            if not matches(tmp):
                raise RuntimeError("Pruefsumme stimmt nicht: " + url)
            tmp.replace(path)
            return
        except Exception:
            if attempt == 2:
                raise
            time.sleep(1 + attempt)


def java(row):
    minimum = row["java"]
    config = ROOT / "java.json"
    configured = json.loads(config.read_text()) if config.exists() else {}
    preferred = str(8 if minimum == 8 else 21 if minimum <= 21 else 25)
    if preferred in configured and Path(configured[preferred]).is_file():
        return configured[preferred]
    candidates = list((Path.home() / ".jdks").glob("*/bin/java.exe"))
    candidates += list((ROOT / "runtime").glob("*/bin/java.exe"))
    for exe in candidates:
        result = subprocess.run([str(exe), "-version"], capture_output=True, text=True, creationflags=NO_WINDOW)
        match = re.search(r'version "(?:1\.)?(\d+)', result.stderr + result.stdout)
        if match and int(match[1]) == int(preferred):
            configured[preferred] = str(exe)
            save(config, configured)
            return str(exe)
    raise RuntimeError(f"Java {preferred} fehlt. Pfad in {config} eintragen (siehe Dokumentation).")


def install_runtime(major):
    package_type = "jdk" if major == 8 else "jre"
    url = ("https://api.azul.com/metadata/v1/zulu/packages/?"
           f"java_version={major}&os=windows&arch=x86&hw_bitness=64&archive_type=zip&"
           f"java_package_type={package_type}&release_status=ga&availability_types=CA&latest=true")
    packages = json.load(urllib.request.urlopen(url, timeout=60))
    package = next(p for p in packages if "-fx-" not in p["name"])
    metadata = json.load(urllib.request.urlopen(
        "https://api.azul.com/metadata/v1/zulu/packages/" + package["package_uuid"], timeout=60))
    directory = ROOT / "runtime"
    archive = directory / package["name"]
    download(package["download_url"], archive, metadata["sha256_hash"], "sha256")
    with zipfile.ZipFile(archive) as z:
        for name in z.namelist():
            if not (directory / name).resolve().is_relative_to(directory.resolve()):
                raise RuntimeError("Ungueltiger ZIP-Pfad")
        z.extractall(directory)
    save(directory / f"java-{major}-source.json", metadata)


def creative_property(row):
    # 1.13 has named /gamemode commands but still parses this property as an integer.
    return "1" if row["protocol"] < 477 else "creative"


def properties(row):
    folder = ROOT / row["version"]
    folder.mkdir(parents=True, exist_ok=True)
    secret_file = folder / "control.json"
    if not secret_file.exists():
        save(secret_file, {"password": secrets.token_hex(24)})
    password = json.loads(secret_file.read_text())["password"]
    modern = row["protocol"] >= 393
    props = {
        "server-ip": "127.0.0.1", "server-port": row["port"], "online-mode": "false",
        "enforce-secure-profile": "false", "enable-rcon": "true", "rcon.port": row["rcon_port"],
        "rcon.password": password, "broadcast-rcon-to-ops": "false", "enable-query": "false",
        "enable-jmx-monitoring": "false", "management-server-enabled": "false",
        "motd": f"ViaForge Testlabor | {row['version']} | Protokoll {row['protocol']}",
        "level-name": "world", "level-seed": "764189", "level-type": "minecraft:flat" if row["protocol"] >= 735 else "FLAT",
        "generate-structures": "false", "gamemode": creative_property(row),
        "difficulty": "normal" if modern else "2", "force-gamemode": "false", "hardcore": "false",
        "max-players": "4", "view-distance": "16", "simulation-distance": "3", "spawn-protection": "0",
        "allow-flight": "true", "allow-nether": "true", "spawn-monsters": "true", "spawn-animals": "true",
        "spawn-npcs": "true", "enable-command-block": "true", "max-tick-time": "180000",
        "network-compression-threshold": "256", "pause-when-empty-seconds": "0",
        "sync-chunk-writes": "true", "initial-enabled-packs": "vanilla",
    }
    # Ground at y=63, shared coordinates even across the 1.18 height change.
    if row["protocol"] < 393:
        props["generator-settings"] = "3;minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass;1;"
    elif row["protocol"] < 735:
        props["generator-settings"] = json.dumps({"biome": "minecraft:plains", "layers": [
            {"block": "minecraft:bedrock", "height": 1}, {"block": "minecraft:stone", "height": 59},
            {"block": "minecraft:dirt", "height": 3}, {"block": "minecraft:grass_block", "height": 1}], "structures": {}})
    else:
        props["generator-settings"] = json.dumps({"biome": "minecraft:plains", "layers": [
            {"block": "minecraft:bedrock", "height": 1},
            {"block": "minecraft:stone", "height": 123 if row["protocol"] >= 757 else 59},
            {"block": "minecraft:dirt", "height": 3}, {"block": "minecraft:grass_block", "height": 1}],
            "structure_overrides": [], "structures": {"structures": {}}})
    path = folder / "server.properties"
    # Setup can be repeated, but never silently overwrite user configuration.
    if not path.exists():
        path.write_text("\n".join(f"{k}={v}" for k, v in props.items()) + "\n", encoding="ascii")
    if not (folder / "eula.txt").exists():
        (folder / "eula.txt").write_text("# https://www.minecraft.net/eula\neula=false\n", encoding="ascii")
    (folder / "queue").mkdir(exist_ok=True)


def setup(rows):
    ROOT.mkdir(parents=True, exist_ok=True)
    for row in rows:
        properties(row)
    def fetch(row):
        descriptor = row["server"]
        download(descriptor["url"], ROOT / row["version"] / "server.jar", descriptor["sha1"])
        return row["version"]
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        for result in pool.map(fetch, rows):
            print("Server geprueft:", result, flush=True)
    for minimum in sorted({r["java"] for r in rows}):
        row = next(r for r in rows if r["java"] == minimum)
        try:
            java(row)
        except RuntimeError:
            install_runtime(8 if minimum == 8 else 21 if minimum <= 21 else 25)
            java(row)
    dashboard()


def catalog(row):
    folder = ROOT / row["version"]
    result = folder / "catalog.json"
    if result.exists():
        cached = json.loads(result.read_text(encoding="utf-8"))
        if cached.get("schema") == 3:
            return cached
    exe = java(row)
    if row["protocol"] < 393:
        with zipfile.ZipFile(folder / "server.jar") as z:
            classes = {n: z.read(n) for n in z.namelist() if n.endswith(".class") and "/" not in n}
        signatures = [[b"STDOUT", b"STDERR"], [b"flowing_water", b"bedrock", b"iron_ore"],
                      [b"iron_shovel", b"carrot_on_a_stick"], [b"Painting", b"Pig", b"Creeper"],
                      [b"AbsorptionAmount", b"HurtTime", b"DeathTime"]]
        names = [max((n for n, b in classes.items() if all(s in b for s in sig)),
                     key=lambda n: len(classes[n]))[:-6] for sig in signatures]
        helper = ROOT / "helper"
        helper.mkdir(exist_ok=True)
        javac = Path(exe).with_name("javac.exe")
        if not javac.exists():
            raise RuntimeError("Zum Export der Legacy-Registries wird ein Java-8-JDK benoetigt.")
        subprocess.run([str(javac), "-cp", str(folder / "server.jar"), "-d", str(helper),
                        str(HERE / "LegacyRegistryDump.java")], check=True, creationflags=NO_WINDOW)
        with (folder / "datagen.log").open("w") as log:
            subprocess.run([exe, "-Xmx768m", "-cp", str(helper) + os.pathsep + str(folder / "server.jar"),
                            "LegacyRegistryDump", *names, str(result)], cwd=folder, stdout=log,
                           stderr=subprocess.STDOUT, check=True, creationflags=NO_WINDOW)
        data = json.loads(result.read_text())
    else:
        reports = folder / "data" / "reports"
        if not (reports / "blocks.json").exists():
            with zipfile.ZipFile(folder / "server.jar") as z:
                bundled = "META-INF/versions.list" in z.namelist()
            command = [exe, "-Xmx1536m"]
            command += (["-DbundlerMainClass=net.minecraft.data.Main", "-jar", "server.jar"] if bundled else
                        ["-cp", "server.jar", "net.minecraft.data.Main"])
            with (folder / "datagen.log").open("w") as log:
                subprocess.run(command + ["--reports", "--output", "data"], cwd=folder, stdout=log,
                               stderr=subprocess.STDOUT, check=True, creationflags=NO_WINDOW)
        blocks = json.loads((reports / "blocks.json").read_text())
        registries = json.loads((reports / "registries.json").read_text()) if (reports / "registries.json").exists() else {}
        items = (registries["minecraft:item"]["entries"] if registries else
                 json.loads((reports / "items.json").read_text()))
        entities = registries.get("minecraft:entity_type", {}).get("entries", {})
        mobs = {name.removesuffix("_spawn_egg") for name in items if name.endswith("_spawn_egg")}
        mobs.update("minecraft:" + name for name in ["iron_golem", "snow_golem", "giant", "illusioner", "wither", "ender_dragon", "armor_stand"])
        if "minecraft:mannequin" in entities:
            mobs.add("minecraft:mannequin")
        if entities:
            assert mobs <= entities.keys(), mobs - entities.keys()
        data = {"blocks": [{"name": name, "properties": next(s.get("properties", {}) for s in b["states"] if s.get("default")),
                            "state_count": len(b["states"])} for name, b in blocks.items()],
                "items": [{"name": name, "metadata": [0]} for name in items], "mobs": sorted(mobs)}
    if row["protocol"] >= 761:
        excluded = experimental_features(row)
        data["excluded_experimental"] = excluded
        for kind in ("blocks", "items", "mobs"):
            data[kind] = [entry for entry in data[kind] if (entry if kind == "mobs" else entry["name"]) not in excluded[kind]]
    data["schema"] = 3
    data["source"] = {"server_sha1": row["server"]["sha1"], "version": row["version"],
                      "kind": "original-server-registries", "protocol": row["protocol"]}
    save(result, data)
    print(f"Katalog {row['version']}: {len(data['blocks'])} Blocktypen, {len(data['items'])} Items, {len(data['mobs'])} Lebewesen", flush=True)
    return data


def experimental_features(row):
    folder = ROOT / row["version"]
    output = folder / "experimental-features.tsv"
    if not output.exists():
        names, members = {}, {}
        if row["protocol"] < 775:
            mapping = folder / "server-mappings.txt"
            if not mapping.exists():
                metadata = json.load(urllib.request.urlopen(row["metadata_url"], timeout=60))
                descriptor = metadata["downloads"]["server_mappings"]
                download(descriptor["url"], mapping, descriptor["sha1"])
            current = None
            for line in mapping.read_text(encoding="utf-8").splitlines():
                if line.startswith("#") or not line.strip():
                    continue
                if not line.startswith(" "):
                    current, obfuscated = line.removesuffix(":").split(" -> ")
                    names[current] = obfuscated
                else:
                    signature, obfuscated = line.strip().split(" -> ")
                    member = signature.split("(", 1)[0].split(" ")[-1]
                    members[(current, member)] = obfuscated
        def cls(name):
            return names.get(name, name)
        def member(name, field):
            return members.get((name, field), field)
        shared = "net.minecraft.SharedConstants"
        bootstrap = "net.minecraft.server.Bootstrap"
        element = "net.minecraft.world.flag.FeatureElement"
        flagset = "net.minecraft.world.flag.FeatureFlagSet"
        flags = "net.minecraft.world.flag.FeatureFlags"
        registry = "net.minecraft.core.Registry"
        builtin = "net.minecraft.core.registries.BuiltInRegistries"
        arguments = [cls(shared), member(shared, "tryDetectVersion"), cls(bootstrap), member(bootstrap, "bootStrap"),
                     cls(element), member(element, "requiredFeatures"), cls(flagset), member(flagset, "isSubsetOf"),
                     cls(flags), member(flags, "DEFAULT_FLAGS"), cls(registry), member(registry, "getKey"), cls(builtin)]
        arguments += [member(builtin, name) for name in ["BLOCK", "ITEM", "ENTITY_TYPE"]]
        helper = ROOT / "helper"
        helper.mkdir(exist_ok=True)
        compiler = Path(java(VERSIONS[0])).with_name("javac.exe")
        subprocess.run([str(compiler), "-d", str(helper), str(HERE / "FeatureFilter.java")], check=True, creationflags=NO_WINDOW)
        jars = sorted((folder / "versions").rglob("*.jar")) + sorted((folder / "libraries").rglob("*.jar"))
        if not jars:
            raise RuntimeError("Datengenerator muss zuerst die Serverbibliotheken entpacken")
        with (folder / "feature-filter.log").open("w") as log:
            subprocess.run([java(row), "-Xmx1024m", "-cp", os.pathsep.join(map(str, [helper] + jars)),
                            "vflabinspect.FeatureFilter", *arguments, str(output)], cwd=folder, stdout=log,
                           stderr=subprocess.STDOUT, check=True, creationflags=NO_WINDOW)
    excluded = {"blocks": [], "items": [], "mobs": []}
    for line in output.read_text().splitlines():
        kind, name = line.split("\t")
        excluded[kind].append(name)
    return excluded


def release_chunks(row):
    if row["protocol"] < 401:
        return []
    return ["forceload remove -80 -112 95 111"]


def read_exact(sock, count):
    data = b""
    while len(data) < count:
        part = sock.recv(count - len(data))
        if not part:
            raise ConnectionError("Verbindung geschlossen")
        data += part
    return data


class Rcon:
    def __init__(self, row):
        self.sock = socket.create_connection(("127.0.0.1", row["rcon_port"]), timeout=10)
        password = json.loads((ROOT / row["version"] / "control.json").read_text())["password"]
        self.send(3, password)
        ident, _, _ = self.receive()
        if ident == -1:
            self.close()
            raise RuntimeError("RCON-Authentifizierung fehlgeschlagen")

    def send(self, kind, value):
        payload = struct.pack("<ii", 17, kind) + value.encode("utf-8") + b"\0\0"
        self.sock.sendall(struct.pack("<i", len(payload)) + payload)

    def receive(self):
        size = struct.unpack("<i", read_exact(self.sock, 4))[0]
        if not 10 <= size <= 4 * 1024 * 1024:
            raise RuntimeError("Ungueltiges RCON-Paket")
        payload = read_exact(self.sock, size)
        ident, kind = struct.unpack("<ii", payload[:8])
        return ident, kind, payload[8:-2].decode("utf-8", errors="replace")

    def command(self, text):
        if len(text.encode("utf-8")) > 1400:
            raise ValueError("Einzelner RCON-Befehl zu lang; Batch verwenden")
        self.send(2, text)
        return self.receive()[2]

    def close(self):
        self.sock.close()

    def __enter__(self):
        return self

    def __exit__(self, *_):
        self.close()


def varint(number):
    data = bytearray()
    number &= 0xFFFFFFFF
    while number > 127:
        data.append((number & 127) | 128)
        number >>= 7
    return bytes(data + bytes([number]))


def read_varint(sock):
    value = 0
    for i in range(5):
        b = read_exact(sock, 1)[0]
        value |= (b & 127) << (7 * i)
        if not b & 128:
            return value
    raise ValueError("Ungueltiger VarInt")


def status(row):
    try:
        with socket.create_connection(("127.0.0.1", row["port"]), timeout=2) as sock:
            handshake = b"\0" + varint(row["protocol"]) + b"\x09localhost" + struct.pack(">H", row["port"]) + b"\x01"
            sock.sendall(varint(len(handshake)) + handshake + b"\x01\x00")
            read_varint(sock)
            if read_varint(sock) != 0:
                return None
            return json.loads(read_exact(sock, read_varint(sock)))
    except (OSError, ValueError):
        return None


def free_memory_mb():
    class MEMORYSTATUSEX(ctypes.Structure):
        _fields_ = [("length", ctypes.c_ulong), ("load", ctypes.c_ulong)] + [
            (n, ctypes.c_ulonglong) for n in ("total", "available", "page_total", "page_available", "virtual", "virtual_available", "extended")]
    if os.name != "nt":
        raise RuntimeError("Die Speicherpruefung benoetigt Windows.")
    state = MEMORYSTATUSEX()
    state.length = ctypes.sizeof(state)
    if not ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(state)):
        raise RuntimeError("Freier Speicher konnte nicht ermittelt werden")
    return state.available // (1024 * 1024)


def heap(row):
    return 768 if row["protocol"] <= 340 else 1024 if row["protocol"] <= 754 else 1536


def process_alive(pid):
    if os.name != "nt":
        raise RuntimeError("Prozesspruefung benoetigt Windows")
    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    kernel.OpenProcess.restype = ctypes.c_void_p
    kernel.OpenProcess.argtypes = [ctypes.c_ulong, ctypes.c_int, ctypes.c_ulong]
    kernel.GetExitCodeProcess.argtypes = [ctypes.c_void_p, ctypes.POINTER(ctypes.c_ulong)]
    kernel.CloseHandle.argtypes = [ctypes.c_void_p]
    handle = kernel.OpenProcess(0x1000, 0, pid)
    if not handle:
        # ERROR_INVALID_PARAMETER means no process. Access denied is NOT evidence of death.
        return ctypes.get_last_error() != 87
    try:
        code = ctypes.c_ulong()
        if not kernel.GetExitCodeProcess(handle, ctypes.byref(code)):
            return True
        return code.value == 259
    finally:
        kernel.CloseHandle(handle)


def recover_stale_worker(row):
    folder = ROOT / row["version"]
    lock = folder / "worker.lock"
    if not lock.exists():
        return
    record = folder / "process.json"
    if not record.exists():
        raise RuntimeError("Worker-Sperre ohne Prozessnachweis; bitte worker.log pruefen: " + row["version"])
    process = json.loads(record.read_text())
    if any(process_alive(process[k]) for k in ["worker_pid", "server_pid"]):
        raise RuntimeError("Der vorhandene Worker/Server laeuft noch: " + row["version"])
    if status(row):
        raise RuntimeError("Serverport weiterhin belegt: " + row["version"])
    # Confirmed dead processes: archive their unacknowledged commands, never replay them on restart.
    for request in (folder / "queue").glob("*.json"):
        request.rename(request.with_suffix(".interrupted"))
    lock.unlink()
    print("Unterbrochenen eigenen Worker freigegeben:", row["version"], flush=True)


def assert_local(row):
    config = {}
    for line in (ROOT / row["version"] / "server.properties").read_text().splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            config[k] = v
    expected = {"server-ip": "127.0.0.1", "online-mode": "false", "enable-query": "false",
                "server-port": str(row["port"]), "rcon.port": str(row["rcon_port"])}
    for key, value in expected.items():
        if config.get(key) != value:
            raise RuntimeError(f"Lokale Konfiguration geaendert: {row['version']} {key}; erwartet {value}")


def start(rows):
    pending = []
    for row in rows:
        existing = status(row)
        if existing:
            if existing.get("version", {}).get("protocol") != row["protocol"]:
                raise RuntimeError("Am vorgesehenen Port laeuft eine andere Version: " + row["version"])
            print(row["version"], "bereits erreichbar; wird nicht neu gestartet")
            continue
        folder = ROOT / row["version"]
        recover_stale_worker(row)
        assert_local(row)
        if not re.search(r"(?m)^\s*eula\s*=\s*true\s*$", (folder / "eula.txt").read_text()):
            raise RuntimeError("Minecraft-EULA noch nicht bestaetigt. Lesen: https://www.minecraft.net/eula; danach 'accept-eula all'.")
        pending.append(row)
    need = sum(heap(r) + 450 for r in pending)
    free = free_memory_mb()
    if need > free - 3072:
        raise RuntimeError(f"Auswahl braucht ca. {need} MiB plus 3072 MiB Reserve; frei sind {free} MiB. Kleinere Gruppe starten.")
    for row in pending:
        # Port reservation is checked before launching; never attach to an unrelated server.
        for port in (row["port"], row["rcon_port"]):
            with socket.socket() as sock:
                sock.bind(("127.0.0.1", port))
        folder = ROOT / row["version"]
        with (folder / "worker.log").open("a") as log:
            subprocess.Popen([sys.executable, str(Path(__file__).resolve()), "_worker", row["version"]],
                             cwd=REPO, stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT,
                             creationflags=NO_WINDOW | getattr(subprocess, "DETACHED_PROCESS", 0))
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            found = status(row)
            if found and "version" in found:
                if found["version"]["protocol"] != row["protocol"]:
                    raise RuntimeError("Falsches Serverprotokoll: " + str(found["version"]))
                try:
                    with Rcon(row) as rcon:
                        rcon.command("list")
                    break
                except OSError:
                    pass
            time.sleep(1)
        else:
            raise RuntimeError(f"Start fehlgeschlagen: {folder / 'console.log'}")
        print(f"Bereit: {row['version']}  127.0.0.1:{row['port']}", flush=True)


def worker(row):
    folder = ROOT / row["version"]
    lock = folder / "worker.lock"
    # A stale lock is retained rather than risking a second writer to the same world.
    try:
        fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
    except FileExistsError:
        raise RuntimeError(f"Worker-Sperre vorhanden: {lock}. Status und worker.log pruefen.")
    os.close(fd)
    child = None
    try:
        assert_local(row)
        with (folder / "console.log").open("ab", buffering=0) as log:
            child = subprocess.Popen([java(row), "-Xms256m", f"-Xmx{heap(row)}m", "-XX:ActiveProcessorCount=2",
                                      "-Dfile.encoding=UTF-8", "-Dlog4j2.formatMsgNoLookups=true",
                                      "-jar", "server.jar", "nogui"], cwd=folder, stdin=subprocess.PIPE,
                                     stdout=log, stderr=subprocess.STDOUT, creationflags=NO_WINDOW)
            save(folder / "process.json", {"worker_pid": os.getpid(), "server_pid": child.pid, "started": time.time()})
            while child.poll() is None:
                for path in sorted((folder / "queue").glob("*.json")):
                    job = json.loads(path.read_text(encoding="utf-8"))
                    marker = "VIAFORGE_JOB_" + path.stem
                    position = (folder / "console.log").stat().st_size
                    for command in job["commands"]:
                        if "\n" in command or "\r" in command:
                            raise ValueError("Mehrzeiliger Konsolenbefehl")
                        child.stdin.write((command + "\n").encode("utf-8"))
                    child.stdin.write(("say " + marker + "\n").encode("utf-8"))
                    child.stdin.flush()
                    until = time.monotonic() + 180
                    completed = False
                    while child.poll() is None and time.monotonic() < until:
                        with (folder / "console.log").open("rb") as source:
                            source.seek(position)
                            output = source.read().decode("utf-8", errors="replace")
                        if marker in output:
                            completed = True
                            break
                        time.sleep(.1)
                    save(path.with_suffix(".result"), {"completed": completed, "output": output if 'output' in locals() else ""})
                    path.unlink()
                    if not completed:
                        raise RuntimeError("Befehlspaket nicht abgeschlossen: " + marker)
                time.sleep(.1)
            save(folder / "exit.json", {"code": child.returncode, "time": time.time()})
    finally:
        if child is not None and child.poll() is None:
            # This handle was created by this worker; never kill arbitrary Java/Minecraft processes.
            try:
                child.stdin.write(b"stop\n")
                child.stdin.flush()
                child.wait(timeout=90)
            except (OSError, subprocess.TimeoutExpired):
                # Retain the sentinel so another worker cannot touch the still-open world.
                raise RuntimeError("Eigener Server beendet sich noch nicht. console.log pruefen; nicht automatisch abwuergen.")
        # Only this worker's own sentinel; no world or process deletion.
        lock.unlink(missing_ok=True)


def batch(row, commands):
    folder = ROOT / row["version"]
    if not (folder / "worker.lock").exists():
        raise RuntimeError("Server muss vom Testlabor gestartet sein")
    key = str(time.time_ns()) + "-" + uuid.uuid4().hex
    path = folder / "queue" / (key + ".json")
    save(path, {"commands": commands})
    result = path.with_suffix(".result")
    deadline = time.monotonic() + 200
    while time.monotonic() < deadline:
        if result.exists():
            data = json.loads(result.read_text(encoding="utf-8"))
            if not data["completed"]:
                raise RuntimeError("Befehlspaket unvollstaendig: " + str(result))
            return data["output"]
        time.sleep(.2)
    raise RuntimeError("Befehlspaket ohne Antwort: " + str(path))


def stop(rows):
    for row in rows:
        if not status(row):
            continue
        folder = ROOT / row["version"]
        if not (folder / "process.json").exists() or not (folder / "worker.lock").exists():
            raise RuntimeError("Kein eigener Worker: " + row["version"])
        process = json.loads((folder / "process.json").read_text())
        if not all(process_alive(process[k]) for k in ("worker_pid", "server_pid")):
            raise RuntimeError("Die aufgezeichneten eigenen Prozesse laufen nicht; stop verweigert: " + row["version"])
        with Rcon(row) as rcon:
            rcon.command("stop")
        deadline = time.monotonic() + 90
        while (folder / "worker.lock").exists() and time.monotonic() < deadline:
            time.sleep(.5)
        if (folder / "worker.lock").exists():
            raise RuntimeError("Speichern/Beenden dauert an: " + row["version"])
        print("Gespeichert und beendet:", row["version"], flush=True)


def dashboard():
    lines = []
    verified = 0
    for r in VERSIONS:
        folder = ROOT / r["version"]
        ready = (folder / "arena-report.json").exists()
        check = folder / "verification.json"
        valid = check.exists() and json.loads(check.read_text()).get("success", False)
        verified += valid
        details = folder / "arena-index.json"
        if details.exists():
            index = json.loads(details.read_text(encoding="utf-8"))
            records = []
            for kind in ("blocks", "items", "mobs", "variants"):
                for item in index.get(kind, []):
                    pos = item.get("position", item.get("chest"))
                    records.append(dict(kind=kind, name=item["name"], variant=item.get("variant", ""),
                                        position=pos, detail=("Slot " + str(item["slot"])) if "slot" in item else
                                        ("auf Knopfdruck" if item.get("on_demand") else "Meta " + str(item.get("metadata", 0)))))
            data = json.dumps(records, ensure_ascii=True).replace("<", "\\u003c")
            (folder / "catalog.html").write_text("<!doctype html><html lang='de'><meta charset='utf-8'><title>Testkatalog " + r["version"] + "</title>"
                "<style>body{font:16px system-ui;margin:32px;background:#151c26;color:#e0ecf5}a{color:#8ee3ba}"
                "input,select{padding:10px;background:#243446;color:white;border:1px solid #579;margin:8px}"
                "td,th{padding:6px 14px;border-bottom:1px solid #344;text-align:left}code{color:#8ee3ba}</style>"
                "<a href='../index.html'>Alle Versionen</a><h1>Testkatalog " + r["version"] + "</h1>"
                "<p>Suchbegriff eingeben. Koordinaten zeigen das Exponat bzw. die Itemkiste. Zum Teleportieren OP-Rechte verwenden.</p>"
                "<input id='query' placeholder='z.B. shulker, bed, piglin' size='36'><select id='kind'><option value=''>Alle Bereiche</option>"
                "<option>blocks</option><option>items</option><option>mobs</option><option>variants</option></select><span id='count'></span>"
                "<table><thead><tr><th>Bereich</th><th>Name</th><th>Details</th><th>Position</th><th>Teleport in der Naehe</th></tr></thead><tbody id='rows'></tbody></table>"
                "<script>const data=" + data + ";const q=document.querySelector('#query'),k=document.querySelector('#kind'),rows=document.querySelector('#rows');"
                "function render(){const found=data.filter(x=>(!k.value||x.kind===k.value)&&(x.name+' '+x.variant).toLowerCase().includes(q.value.toLowerCase().replaceAll(' ','_')));"
                "rows.replaceChildren();document.querySelector('#count').textContent=found.length+' Treffer';for(const x of found.slice(0,300)){const tr=document.createElement('tr');"
                "for(const value of [x.kind,x.name+' '+x.variant,x.detail,x.position.join(', '),'/tp @p '+x.position[0]+' '+(x.position[1]+1)+' '+(x.position[2]-2)]){const td=document.createElement('td');td.textContent=value;tr.append(td)}rows.append(tr)}}"
                "q.addEventListener('input',render);k.addEventListener('change',render);render();</script></html>", encoding="utf-8")
        state = "Geprueft" if valid else "Testwelt gebaut" if ready else "Testwelt noch zu bauen"
        lines.append(f"<tr><td>{r['version']}</td><td>{r['protocol']}</td><td><code>127.0.0.1:{r['port']}</code></td>"
                     f"<td>{state}</td><td>" + (f"<a href='{r['version']}/catalog.html'>Katalog durchsuchen</a>" if details.exists() else "") + "</td></tr>")
    (ROOT / "index.html").write_text("<!doctype html><html lang='de'><meta charset='utf-8'><title>ViaForge Testlabor</title>"
        "<style>body{font:16px system-ui;max-width:1000px;margin:40px auto;background:#151c26;color:#e0ecf5}"
        "td,th{padding:9px 22px;text-align:left;border-bottom:1px solid #344}code{color:#8ee3ba}a{color:#8bd5ff}</style>"
        f"<h1>ViaForge Testlabor</h1><p>48 originale Vanilla-Versionen. Zugriff nur auf diesem PC. {verified}/48 Welten geprueft.</p>"
        "<p>Starten: <code>Testserver.bat start 26.2</code> &middot; Gruppe: <code>start regression</code><br>"
        "Beenden mit Speichern: <code>Testserver.bat stop all</code></p>"
        "<p>Im Spiel: Multiplayer → Direkt verbinden → Adresse kopieren. In ViaForge die passende Serverversion waehlen.</p>"
        "<p>OP: <code>Testserver.bat op 26.2 Spielername</code>. Spawn: <code>/tp @p 0 177 0</code>.<br>"
        "Teststationen: <code>/tp @p 0 65 -100</code>.<br>"
        "Blockkatalog und Koordinaten stehen pro Version in <code>arena-index.json</code>.</p>"
        "<table><tr><th>Version</th><th>Protokoll</th><th>Adresse</th><th>Welt</th><th>Inhalte</th></tr>" + "".join(lines) + "</table></html>", encoding="utf-8")


def provision(rows):
    """Two servers at a time: initialize, validate, save, stop. Never stop existing sessions."""
    import arena
    results = []
    def one(row):
        running = status(row) is not None
        try:
            if not running:
                start([row])
            arena.build(row)
            arena.upgrade(row)
            arena.verify(row)
            return {"version": row["version"], "success": True}
        except Exception as error:
            return {"version": row["version"], "success": False, "error": str(error)}
        finally:
            if not running and status(row):
                stop([row])
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        tasks = {pool.submit(one, row): row for row in rows}
        for task in concurrent.futures.as_completed(tasks):
            try:
                result = task.result()
            except Exception as error:
                result = {"version": tasks[task]["version"], "success": False, "error": str(error)}
            results.append(result)
            save(ROOT / result["version"] / "provision-result.json", result)
            save(ROOT / "provision-report.json", [json.loads(p.read_text(encoding="utf-8"))
                 for p in sorted(ROOT.glob("*/provision-result.json"))])
            print("FERTIG", result, flush=True)
    dashboard()
    failed = [r for r in results if not r["success"]]
    if failed:
        raise RuntimeError(f"{len(failed)} Versionen noch fehlerhaft. Siehe provision-report.json")


def audit(rows):
    """Read persisted worlds on a fresh server process, with no world rebuild."""
    import arena
    failures = []
    for row in rows:
        if status(row):
            raise RuntimeError("Fuer die Neustartpruefung den Testserver vorher speichern/beenden: " + row["version"])
        try:
            start([row])
            report = json.loads((ROOT / row["version"] / "arena-report.json").read_text())
            needs_upgrade = report.get("revision", 0) < arena.REVISION or report.get("control_revision") != 1 or (row["protocol"] >= 757 and report.get("height_station_revision") != 1)
            arena.upgrade(row)
            # Upgrade saves; a second fresh start proves those latest additions survived too.
            if needs_upgrade:
                stop([row])
                start([row])
            arena.verify(row)
            result = json.loads((ROOT / row["version"] / "verification.json").read_text(encoding="utf-8"))
            result.pop("output", None)
            result["fresh_process"] = True
            result["arena_revision"] = arena.REVISION
            result["control_revision"] = 1
            if row["protocol"] >= 757:
                result["height_station_revision"] = 1
            save(ROOT / row["version"] / "persistence-check.json", result)
        except Exception as error:
            result = {"version": row["version"], "success": False, "error": str(error)}
            failures.append(result)
            save(ROOT / row["version"] / "persistence-check.json", result)
            print("AUDIT FEHLER", result, flush=True)
        finally:
            if status(row):
                stop([row])
    dashboard()
    if failures:
        raise RuntimeError(f"{len(failures)} Neustartpruefungen fehlgeschlagen")


def select(value):
    groups = {"regression": ["1.12.2", "1.13.2", "1.21.11", "26.2"],
              "legacy": ["1.9", "1.10.2", "1.11.2", "1.12.2"],
              "modern": ["1.16.5", "1.18.2", "1.20.6", "26.2"]}
    if value == "all":
        return VERSIONS
    names = groups.get(value, value.split(","))
    selected = [r for r in VERSIONS if r["version"] in names]
    if len(selected) != len(names):
        raise ValueError("Unbekannte Version/Gruppe: " + value)
    return selected


def main():
    parser = argparse.ArgumentParser(description="ViaForge Vanilla-Testlabor (nur localhost)")
    parser.add_argument("action", choices=["setup", "catalog", "start", "stop", "status", "build", "verify", "provision", "audit", "find", "kit", "export-list", "import-list", "command", "op", "accept-eula", "_worker"])
    parser.add_argument("versions", nargs="?", default="all", help="Versionen mit Komma, all, regression, legacy oder modern")
    parser.add_argument("arguments", nargs="*")
    args = parser.parse_args()
    rows = select(args.versions)
    if args.action == "setup":
        setup(rows)
    elif args.action == "catalog":
        for row in rows:
            catalog(row)
    elif args.action in ("export-list", "import-list"):
        import server_list
        (server_list.export if args.action == "export-list" else server_list.import_dev)(rows)
    elif args.action == "accept-eula":
        # This command is an explicit action by the operator, never part of setup/start.
        for row in rows:
            (ROOT / row["version"] / "eula.txt").write_text("# Accepted by operator: https://www.minecraft.net/eula\neula=true\n", encoding="ascii")
        save(ROOT / "eula-acceptance.json", {"time": time.time(), "versions": [r["version"] for r in rows]})
        print("Minecraft-EULA fuer die ausgewaehlten Testserver bestaetigt.")
    elif args.action == "start":
        start(rows)
    elif args.action == "provision":
        provision(rows)
    elif args.action == "audit":
        audit(rows)
    elif args.action == "stop":
        stop(rows)
    elif args.action == "status":
        for r in rows:
            found = status(r)
            print(f"{r['version']:9} 127.0.0.1:{r['port']}  " + ("LAEUFT " + str(found.get('version', 'startet')) if found else "aus"))
    elif args.action == "_worker":
        if len(rows) != 1:
            raise ValueError("Worker benoetigt genau eine Version")
        worker(rows[0])
    elif args.action == "find":
        needle = " ".join(args.arguments).lower().replace(" ", "_")
        if not needle:
            raise ValueError("Suchbegriff fehlt, z.B. find 26.2 shulker")
        for row in rows:
            index = json.loads((ROOT / row["version"] / "arena-index.json").read_text(encoding="utf-8"))
            matches = 0
            for category in ("blocks", "items", "mobs", "variants"):
                for item in index.get(category, []):
                    if needle not in (item["name"] + " " + item.get("variant", "")).lower():
                        continue
                    position = item.get("position", item.get("chest"))
                    print(row["version"], category, item["name"], item.get("variant", ""),
                          "bei", ",".join(map(str, position)),
                          "Slot " + str(item["slot"]) if "slot" in item else "Meta " + str(item.get("metadata", 0)))
                    matches += 1
            if not matches:
                print(row["version"], "kein Treffer")
    elif args.action == "kit":
        name = " ".join(args.arguments)
        if not re.fullmatch(r"[A-Za-z0-9_]{1,16}", name):
            raise ValueError("Spielername fehlt/ungueltig: kit 26.2 DeinName")
        for row in rows:
            available = {item["name"] for item in catalog(row)["items"]}
            desired = ["shield", "purpur_block", "purpur_stairs", "stone", "elytra", "diamond_sword",
                       "totem_of_undying", "totem", "red_bed", "bed", "orange_shulker_box", "boat", "oak_boat",
                       "piglin_spawn_egg", "shulker_spawn_egg", "spawn_egg"]
            commands = []
            for item in desired:
                identifier = "minecraft:" + item
                if identifier in available:
                    commands.append(f"give {name} {identifier} " + ("16" if item in {"stone", "purpur_block"} else "1"))
            print(row["version"], batch(row, commands))
    elif args.action in ("build", "verify"):
        import arena
        for r in rows:
            getattr(arena, args.action)(r)
            if args.action == "build":
                arena.upgrade(r)
        dashboard()
    else:
        command = " ".join(args.arguments)
        if args.action == "op":
            if not re.fullmatch(r"[A-Za-z0-9_]{1,16}", command):
                raise ValueError("Ungueltiger Spielername")
            command = "op " + command
        if not command:
            raise ValueError("Befehl fehlt")
        for r in rows:
            print(r["version"], batch(r, [command]))


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, ValueError, OSError, subprocess.CalledProcessError) as error:
        print("FEHLER:", error, file=sys.stderr)
        sys.exit(1)
