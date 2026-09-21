"""Explicit maintainer action: resolve official downloads to a reviewable, pinned lockfile."""
import concurrent.futures
import hashlib
import json
import socket
import time
import urllib.request

import lab


def metadata(url):
    request = urllib.request.Request(url, headers={"User-Agent": "ViaForgeLab/1.0 (https://github.com/GrimAnticheat/Grim)"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def resolve():
    releases = metadata("https://api.modrinth.com/v2/project/grimac/version")
    release = next(r for r in releases if "bukkit" in r["loaders"])
    artifact = next(f for f in release["files"] if f["primary"])
    project = metadata("https://fill.papermc.io/v3/projects/paper")
    available = {v for group in project["versions"].values() for v in group}
    matrix = []
    def paper(row):
        version = row["version"]
        entry = dict(minecraft=version, protocol=row["protocol"], grim=release["version_number"])
        if version not in release["game_versions"]:
            return dict(entry, available=False, reason="Not declared compatible by the pinned official Grim release")
        if version not in available:
            return dict(entry, available=False, reason="No exact Paper release in official download service; no substituted Minecraft version")
        builds = metadata(f"https://fill.papermc.io/v3/projects/paper/versions/{version}/builds")
        build = next((b for b in builds if b["channel"] == "STABLE"), builds[0])
        return dict(entry, available=True, platform="Paper", build=build)
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        matrix = list(pool.map(paper, lab.VANILLA_VERSIONS))
    lock = dict(schema=1, resolved_at=time.time(), grim=release, matrix=matrix,
                sources=["https://github.com/GrimAnticheat/Grim", "https://github.com/GrimAnticheat/Grim/wiki/Permissions",
                         "https://api.modrinth.com/v2/project/grimac/version", "https://fill.papermc.io/v3/projects/paper"])
    lab.save(lab.HERE / "grim-sources.json", lock)
    occupied = {r[k] for r in lab.VERSIONS for k in ("port", "rcon_port")}
    previous = {r["reference_version"]: r for r in lab.GRIM_VERSIONS}
    def port():
        for candidate in range(27000, 30000):
            if candidate in occupied:
                continue
            try:
                with socket.socket() as sock:
                    if hasattr(socket,'SO_EXCLUSIVEADDRUSE'):
                        sock.setsockopt(socket.SOL_SOCKET,socket.SO_EXCLUSIVEADDRUSE,1)
                    sock.bind(("127.0.0.1", candidate))
            except OSError:
                continue
            occupied.add(candidate)
            return candidate
        raise RuntimeError("No free local port")
    rows = []
    for entry in matrix:
        if not entry["available"]:
            continue
        base = next(r for r in lab.VANILLA_VERSIONS if r["version"] == entry["minecraft"])
        old = previous.get(base["version"], {})
        d = entry["build"]["downloads"]["server:default"]
        rows.append(dict(base, version=base["version"] + "-grim", minecraft_version=base["version"],
                         reference_version=base["version"], platform="Paper", platform_build=entry["build"]["id"],
                         grim_version=release["version_number"], port=old.get("port") or port(),
                         rcon_port=old.get("rcon_port") or port(), runtime_java=17 if base["protocol"] < 766 else base["java"],
                         launch_jar="paper.jar", platform_download=d,
                         jvm_args=["-DPaper.IgnoreJavaVersion=true"] if base["protocol"] < 757 else []))
    lab.save(lab.GRIM_MANIFEST, rows)
    print(f"Pinned {len(rows)} exact Paper variants; {len(matrix)-len(rows)} unavailable entries")


if __name__ == "__main__":
    resolve()
