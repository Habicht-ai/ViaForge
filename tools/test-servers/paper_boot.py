"""Exact-byte legacy Paper Java guard adapter. Original downloaded JARs remain intact."""
import hashlib
import json
from pathlib import Path
import shutil
import struct
import subprocess
import zipfile
import lab


def guard(data):
    if b'Unsupported Java detected' not in data or b'Paper.IgnoreJavaVersion' in data:
        return None
    count = int.from_bytes(data[8:10], 'big')
    i, pos, found = 1, 10, []
    while i < count:
        tag = data[pos]; pos += 1
        if tag == 1:
            length = int.from_bytes(data[pos:pos+2], 'big'); pos += 2 + length
        elif tag in (5, 6):
            if tag == 6:
                value = struct.unpack('>d', data[pos:pos+8])[0]
                pattern = b'\x8d\x14' + i.to_bytes(2, 'big') + b'\x97\x9e'
                if 52 <= value < 61 and data.count(pattern) == 1: found.append((pos, value))
            pos += 8; i += 1
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): pos += 4
        elif tag in (7, 8, 16, 19, 20): pos += 2
        elif tag == 15: pos += 3
        else: raise ValueError('Unknown constant pool entry: ' + str(tag))
        i += 1
    if len(found) != 1: raise ValueError('Unreviewed legacy Java guard shape')
    return dict(main_sha256=hashlib.sha256(data).hexdigest(), offset=found[0][0], old_maximum=found[0][1], new_maximum=61.0)


def build():
    source = lab.HERE / 'labac/bootguard/LegacyPaperBoot.java'
    classes = lab.REPO / 'build/labac/bootguard'
    classes.mkdir(parents=True, exist_ok=True)
    javac = Path(lab.java({'java': 21})).with_name('javac.exe')
    subprocess.run([str(javac), '--release', '17', '-d', str(classes), str(source)], check=True, creationflags=lab.NO_WINDOW)
    target = lab.REPO / 'build/libs/ViaForgeLab-LegacyPaperBoot-1.0.0.jar'
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as jar:
        jar.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\nPremain-Class: viaforge.lab.LegacyPaperBoot\n\n')
        for file in classes.rglob('*.class'): jar.write(file, file.relative_to(classes).as_posix())
    return target


def prepare(row):
    if not row.get('grim_version'): return []
    folder = lab.ROOT / row['version']
    if row['protocol'] < 393:
        with zipfile.ZipFile(folder / row['launch_jar']) as jar:
            if 'patch.json' in jar.namelist():
                patch = json.loads(jar.read('patch.json'))
                # Old S3 URLs are gone. Supply the SAME Mojang bytes from its content-addressed CDN.
                destination = folder / ('cache/mojang_' + row['minecraft_version'] + '.jar')
                if not destination.exists():
                    lab.download(row['server']['url'], destination, patch['originalHash'].lower(), 'sha256')
                if b'SystemClassLoader not URLClassLoader' in jar.read('com/destroystokyo/paperclip/Paperclip.class'):
                    patched = folder / ('cache/patched_' + row['minecraft_version'] + '.jar')
                    if not patched.exists():
                        subprocess.run([lab.java(row), '-Dpaperclip.patchonly=true', '-jar', row['launch_jar']],
                                       cwd=folder, creationflags=lab.NO_WINDOW, timeout=90)
                    if hashlib.sha256(patched.read_bytes()).hexdigest() != patch['patchedHash'].lower():
                        raise ValueError('Paperclip output checksum mismatch')
                    row['_effective_launch_jar'] = patched.relative_to(folder).as_posix()
                    lab.save(folder / 'paper-boot-adapter.json',dict(original_paper=row['launch_jar'],
                        launch_jar=row['_effective_launch_jar'],sha256=patch['patchedHash'].lower(),runtime_java=17,
                        reason='Legacy Paperclip assumes URLClassLoader; launch its verified unmodified output directly'))
        return []
    if not row['protocol'] < 735: return []
    # Paperclip verifies its Mojang base and applies its own embedded patch.
    patched = folder / ('cache/patched_' + row['minecraft_version'] + '.jar')
    if not patched.exists():
        subprocess.run([lab.java(row), '-Dpaperclip.patchonly=true', '-jar', row['launch_jar']],
                       cwd=folder, check=True, creationflags=lab.NO_WINDOW)
    with zipfile.ZipFile(patched) as jar: reviewed = guard(jar.read('org/bukkit/craftbukkit/Main.class'))
    if reviewed is None: return []
    agent = lab.REPO / 'build/libs/ViaForgeLab-LegacyPaperBoot-1.0.0.jar'
    if not agent.exists(): raise RuntimeError('Build the reviewed Paper boot adapter first')
    shutil.copy2(agent, folder / agent.name)
    reviewed.update(paper_build=row['platform_build'], minecraft=row['minecraft_version'], runtime_java=17,
                    paper_sha256=hashlib.sha256((folder / row['launch_jar']).read_bytes()).hexdigest(),
                    agent_sha256=hashlib.sha256(agent.read_bytes()).hexdigest())
    lab.save(folder / 'paper-boot-adapter.json', reviewed)
    return [f"-javaagent:{agent.name}={reviewed['main_sha256']}:{reviewed['offset']}:{reviewed['old_maximum']}"]


if __name__ == '__main__': print(build())
