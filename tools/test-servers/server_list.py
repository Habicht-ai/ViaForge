"""Lossless append to Minecraft's uncompressed servers.dat; unknown tags stay byte-for-byte."""
import json
import shutil
import struct
import subprocess
import time
from pathlib import Path
import lab


def utf(text):
    data = text.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def string_tag(key, value):
    return b"\x08" + utf(key) + utf(value)


def entries(rows):
    return [string_tag("name", "ViaForge Labor " + row["version"]) +
            string_tag("ip", "127.0.0.1:" + str(row["port"])) +
            string_tag("viaForge$version", row["version"]) + b"\0" for row in rows]


class Reader:
    def __init__(self, data):
        if len(data) > 16 * 1024 * 1024:
            raise ValueError("Serverliste groesser als 16 MiB")
        self.data, self.pos = data, 0

    def read(self, count):
        if count < 0 or self.pos + count > len(self.data):
            raise ValueError("Beschaedigte NBT-Serverliste")
        result = self.data[self.pos:self.pos + count]
        self.pos += count
        return result

    def string(self):
        size = struct.unpack(">H", self.read(2))[0]
        return self.read(size).decode("utf-8", errors="replace")

    def number(self):
        size = struct.unpack(">i", self.read(4))[0]
        if not 0 <= size <= 1_000_000:
            raise ValueError("Ungueltige NBT-Laenge")
        return size

    def payload(self, kind, depth=0):
        if depth > 32:
            raise ValueError("NBT zu tief verschachtelt")
        sizes = {1: 1, 2: 2, 3: 4, 4: 8, 5: 4, 6: 8}
        if kind in sizes:
            self.read(sizes[kind])
        elif kind in (7, 11, 12):
            self.read(self.number() * {7: 1, 11: 4, 12: 8}[kind])
        elif kind == 8:
            self.string()
        elif kind == 9:
            subtype = self.read(1)[0]
            for _ in range(self.number()):
                self.payload(subtype, depth + 1)
        elif kind == 10:
            while (subtype := self.read(1)[0]) != 0:
                self.string()
                self.payload(subtype, depth + 1)
        else:
            raise ValueError("Unbekannter NBT-Typ: " + str(kind))


def address(entry):
    reader = Reader(entry)
    found = None
    while (kind := reader.read(1)[0]) != 0:
        key = reader.string()
        if key == "ip" and kind == 8:
            found = reader.string()
        else:
            reader.payload(kind)
    return found


def merge(data, rows):
    reader = Reader(data)
    if reader.read(1) != b"\x0a":
        raise ValueError("Unkomprimierte NBT-Serverliste erwartet")
    reader.string()
    prefix = data[:reader.pos]
    parts, have_list, added = [], False, 0
    while True:
        start = reader.pos
        kind = reader.read(1)[0]
        if kind == 0:
            break
        key = reader.string()
        if key == "servers" and kind == 9:
            if have_list or reader.read(1) != b"\x0a":
                raise ValueError("Ungueltige servers-Liste")
            have_list = True
            old = []
            for _ in range(reader.number()):
                begin = reader.pos
                reader.payload(10)
                old.append(data[begin:reader.pos])
            addresses = {address(item) for item in old}
            new = [item for item in entries(rows) if address(item) not in addresses]
            added += len(new)
            parts.append(b"\x09" + utf("servers") + b"\x0a" + struct.pack(">i", len(old) + len(new)) + b"".join(old + new))
        else:
            reader.payload(kind)
            parts.append(data[start:reader.pos])
    if reader.pos != len(data):
        raise ValueError("Unerwartete Daten hinter der NBT-Wurzel")
    if not have_list:
        new = entries(rows)
        added = len(new)
        parts.append(b"\x09" + utf("servers") + b"\x0a" + struct.pack(">i", len(new)) + b"".join(new))
    return prefix + b"".join(parts) + b"\0", added


def export(rows):
    data, _ = merge(b"\x0a\x00\x00\x00", rows)
    target = lab.ROOT / "servers-labor.dat"
    target.write_bytes(data)
    print("Separate Serverliste:", target)


def import_dev(rows):
    # Refuse to race a client which caches and later rewrites servers.dat.
    result = subprocess.run(["powershell", "-NoProfile", "-Command",
        "@(Get-CimInstance Win32_Process | Where-Object { $_.Name -match '^javaw?\\.exe$' -and $_.CommandLine -match 'GradleStart|net.minecraft.client.main.Main|net.minecraft.launchwrapper.Launch' }).Count"],
        capture_output=True, text=True, check=True, creationflags=lab.NO_WINDOW)
    if int(result.stdout.strip()) != 0:
        raise RuntimeError("Minecraft-Client laeuft noch. Vor 'import-list' selbst schliessen, damit servers.dat nicht ueberschrieben wird.")
    target = lab.REPO / "run/servers.dat"
    original = target.read_bytes() if target.exists() else b"\x0a\x00\x00\x00"
    combined, added = merge(original, rows)
    if not added:
        print("Alle lokalen Adressen sind bereits in der Entwicklungsliste.")
        return
    if target.exists():
        backup = target.with_name("servers.dat.viaforge-lab-" + str(time.time_ns()) + ".bak")
        shutil.copy2(target, backup)
        print("Sicherung:", backup)
    temporary = target.with_name("servers.dat.viaforge-lab.tmp")
    temporary.write_bytes(combined)
    temporary.replace(target)
    print(f"{added} Testserver in run/servers.dat ergaenzt; vorhandene Eintraege unveraendert.")
