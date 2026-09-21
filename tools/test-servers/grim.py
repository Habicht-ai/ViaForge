"""Pinned, independent Paper/Grim laboratory variants. No original world is opened or converted."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import uuid
import zipfile

import lab


def lock():
    return json.loads((lab.HERE / "grim-sources.json").read_text(encoding="utf-8"))


def digest(path):
    with Path(path).open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def assert_stopped(row):
    folder = lab.ROOT / row["version"]
    record = json.loads((folder / "process.json").read_text()) if (folder / "process.json").exists() else {}
    # Historical process.json remains after clean stop; Windows can reuse its PIDs.
    if ((folder / "worker.lock").exists() and any(lab.process_alive(record.get(k, 0))
            for k in ("server_pid", "worker_pid"))) or lab.status(row):
        raise RuntimeError("Server must be stopped before copying/restoring: " + row["version"])


def build_bridge():
    downloads = lab.ROOT / "downloads"
    release = lock()["grim"]
    artifact = next(f for f in release["files"] if f["primary"])
    grim = downloads / artifact["filename"]
    lab.download(artifact["url"], grim, artifact["hashes"]["sha512"], "sha512")
    # This exact official Paper output supplies the oldest common Bukkit API for compilation.
    row = next(r for r in lab.GRIM_VERSIONS if r["minecraft_version"] == "1.12.2")
    d = row["platform_download"]
    paper = downloads / d["name"]
    lab.download(d["url"], paper, d["checksums"]["sha256"], "sha256")
    subprocess.run([lab.java(row), "-Dpaperclip.patchonly=true", "-jar", str(paper)],
                   cwd=downloads, check=True, creationflags=lab.NO_WINDOW)
    bukkit = downloads / "cache/patched_1.12.2.jar"
    classes = lab.REPO / "build/labac/classes"
    classes.mkdir(parents=True, exist_ok=True)
    javac = Path(lab.java({"java": 21})).with_name("javac.exe")
    sources = sorted((lab.HERE / "labac/src").rglob("*.java"))
    subprocess.run([str(javac), "--release", "17", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [bukkit, grim])),
                    "-d", str(classes), *map(str, sources)], check=True, creationflags=lab.NO_WINDOW)
    jar = lab.REPO / "build/libs/ViaForgeLabAC-1.0.0.jar"
    jar.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as archive:
        for file in sorted(classes.rglob("*.class")):
            archive.write(file, file.relative_to(classes).as_posix())
        for name in ["plugin.yml", "config.yml"]:
            archive.write(lab.HERE / "labac" / name, name)
    lab.save(lab.REPO / "build/labac/build.json", dict(time=time.time(), jar=str(jar), sha256=digest(jar),
        grim_sha256=digest(grim), paper_sha256=digest(paper), bukkit_sha256=digest(bukkit),
        sources={str(p.relative_to(lab.REPO)): digest(p) for p in sources}))
    print("BUILT", jar, digest(jar), flush=True)
    return jar


def install(row):
    if not row.get("reference_version"):
        raise ValueError("Not a Grim variant")
    assert_stopped(row)
    folder = lab.ROOT / row["version"]
    reference = next(r for r in lab.VANILLA_VERSIONS if r["version"] == row["reference_version"])
    source = lab.ROOT / reference["version"]
    helper = lab.REPO / "build/libs/ViaForgeLabAC-1.0.0.jar"
    if not helper.exists():
        raise RuntimeError("Run grim.py build-bridge first")
    lab.properties(row)
    descriptor = row["platform_download"]
    cached = lab.ROOT / "downloads" / descriptor["name"]
    lab.download(descriptor["url"], cached, descriptor["checksums"]["sha256"], "sha256")
    shutil.copy2(cached, folder / row["launch_jar"])
    artifact = next(f for f in lock()["grim"]["files"] if f["primary"])
    cached_grim = lab.ROOT / "downloads" / artifact["filename"]
    lab.download(artifact["url"], cached_grim, artifact["hashes"]["sha512"], "sha512")
    plugins = folder / "plugins"
    plugins.mkdir(exist_ok=True)
    shutil.copy2(cached_grim, plugins / artifact["filename"])
    shutil.copy2(helper, plugins / helper.name)
    if not (folder / "world").exists():
        assert_stopped(reference)
        if not (source / "world/level.dat").exists():
            raise RuntimeError("Prepared reference world missing: " + reference["version"])
        stage = folder / ("world-copy-" + uuid.uuid4().hex)
        shutil.copytree(source / "world", stage)
        # A stopped source is required for the entire copy, including its inventory data.
        assert_stopped(reference)
        stage.rename(folder / "world")
        for name in ["arena-index.json", "arena-report.json", "catalog.json", "catalog.html", "sign-manifest.json",
                     "inventory-catalog.json", "inventory-block-ids.json", "inventory-commands.txt", "control-checks.json"]:
            if (source / name).exists():
                shutil.copy2(source / name, folder / name)
        if (source / "data").exists():
            shutil.copytree(source / "data", folder / "data", dirs_exist_ok=True)
        lab.save(folder / "world-origin.json", dict(time=time.time(), source=str(source / "world"),
            copied_while_stopped=True, source_level_sha256=digest(source / "world/level.dat"),
            platform_conversion="Only the independent copy may be converted by Paper; reference remains Vanilla"))
    # User explicitly accepted the EULA for this local laboratory in the task handover.
    (folder / "eula.txt").write_text("# Explicit local laboratory acceptance, 2026-09-20\neula=true\n", encoding="ascii")
    lab.save(folder / "installation.json", dict(time=time.time(), platform=row["platform"],
        minecraft=row["minecraft_version"], platform_build=row["platform_build"], grim=row["grim_version"],
        files={row["launch_jar"]: digest(folder / row["launch_jar"]),
               "plugins/" + artifact["filename"]: digest(cached_grim), "plugins/" + helper.name: digest(helper)}))
    print("INSTALLED", row["version"], flush=True)


def control(row, action):
    if not row.get("grim_version") or action not in {"on", "off", "status"}:
        raise ValueError("Grim not available or invalid action")
    if not lab.status(row):
        raise RuntimeError("Start the Grim variant before issuing labac commands")
    with lab.Rcon(row) as remote:
        print(remote.command("labac " + action), flush=True)
    if action != "status":
        until = time.monotonic() + 10
        while time.monotonic() < until:
            actual = state(row, running=True)
            if actual.get("state") == ("enabled" if action == "on" else "disabled"):
                print(json.dumps(actual, ensure_ascii=True), flush=True)
                return
            time.sleep(.2)
        raise RuntimeError("Grim did not confirm requested state: " + str(actual))


def update_bridge(row):
    """Update only our helper on a stopped variant, retaining its old binary/config."""
    assert_stopped(row)
    if not row.get('grim_version'):raise ValueError('Grim variant required')
    helper=lab.REPO/'build/libs/ViaForgeLabAC-1.0.0.jar'
    folder=lab.ROOT/row['version'];target=folder/'plugins'/helper.name
    if target.exists() and digest(target)==digest(helper):return
    backup=folder/'backups'/('labac-update-'+str(time.time_ns()))
    backup.mkdir(parents=True)
    for source in [target,folder/'plugins/ViaForgeLabAC/config.yml',folder/'installation.json']:
        if source.exists():shutil.copy2(source,backup/source.name)
    shutil.copy2(helper,target)
    record=json.loads((folder/'installation.json').read_text(encoding='utf-8'))
    record['files']['plugins/'+helper.name]=digest(helper)
    record['bridge_updated']=time.time();record['bridge_backup']=str(backup)
    lab.save(folder/'installation.json',record)
    print('UPDATED BRIDGE',row['version'],flush=True)


def state(row, running=False):
    if not row.get("grim_version"):
        matrix = next((e for e in lock()["matrix"] if e["minecraft"] == row["version"]), {})
        return dict(state="unavailable", reason=row.get('unavailable_reason') or matrix.get("reason", "No verified Grim platform for this exact Minecraft version"))
    probe = lab.ROOT / row['version'] / 'grim-startup-probe.json'
    if not running and probe.exists():
        verified = json.loads(probe.read_text(encoding='utf-8'))
        if verified.get('success') is False:
            return dict(state='unavailable',reason=verified.get('error') or 'Installed build failed its latest real Grim startup check; inspect console.log')
    file = lab.ROOT / row["version"] / "plugins/ViaForgeLabAC/status.json"
    try:
        result = json.loads(file.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return dict(state="unavailable" if running else "offline", requested_enabled=True)
    if not running:
        result["state"] = "offline"
    elif time.time() - result.get("time", 0) > 8:
        result["state"] = "unavailable"
        result["reason"] = "No current confirmation from the running bridge"
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["build-bridge", "update-bridge", "install", "on", "off", "status"])
    parser.add_argument("versions", nargs="?", default="grim")
    args = parser.parse_args()
    if args.action == "build-bridge":
        build_bridge()
    else:
        import profiles
        for row in profiles.select(args.versions):
            if args.action == "install": install(row)
            elif args.action == 'update-bridge':update_bridge(row)
            else: control(row, args.action)
