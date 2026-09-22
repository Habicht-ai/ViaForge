"""Real Forge joins to an isolated multi-version Paper server, with wire-level evidence.

Uses only a new test world and fresh offline accounts. Original lab servers, their
plugins and worlds are never opened or changed. Requires installed 1.12.2 Paper.
"""
import hashlib
from collections import Counter
import io
import json
import os
from pathlib import Path
import secrets
import re
import select
import shutil
import socket
import socketserver
import struct
import subprocess
import threading
import time
import zipfile
import lab

VIA_URL = 'https://github.com/ViaVersion/ViaVersion/releases/download/5.12.0/ViaVersion-5.12.0.jar'
VIA_SHA = '72c40a6a702d67f226fc9a0d8ad82aba1483fdabe2e6159bcdddb2dc070750b0'


def available_port():
    sock = socket.socket()
    sock.bind(('127.0.0.1', 0))
    return sock


def number(stream):
    value = 0
    for shift in range(0, 35, 7):
        data = stream.read(1)
        if not data: raise EOFError()
        value |= (data[0] & 127) << shift
        if not data[0] & 128: return value
    raise ValueError('Invalid VarInt')


class RecordingProxy(socketserver.ThreadingTCPServer):
    daemon_threads = True
    allow_reuse_address = False


class Forwarder(socketserver.BaseRequestHandler):
    def handle(self):
        try:
            self.request.settimeout(20)
            size = lab.read_varint(self.request)
            if not 1 <= size <= 4096: raise ValueError('Invalid handshake size')
            packet = lab.read_exact(self.request, size)
            stream = io.BytesIO(packet)
            if number(stream) != 0: raise ValueError('Not a handshake')
            protocol = number(stream)
            host = stream.read(number(stream)).decode('utf-8')
            port = struct.unpack('>H', stream.read(2))[0]
            state = number(stream)
            with self.server.record_lock:
                self.server.records.append(dict(protocol=protocol, host=host, port=port, state=state, time=time.time()))
            with socket.create_connection(('127.0.0.1', self.server.backend), timeout=20) as backend:
                backend.sendall(lab.varint(size) + packet)
                while True:
                    ready, _, _ = select.select([self.request, backend], [], [], 30)
                    if not ready: return
                    for source in ready:
                        data = source.recv(65536)
                        if not data: return
                        (backend if source is self.request else self.request).sendall(data)
        except (OSError, EOFError):
            pass  # Client closes normally between cases; record is already kept.
        except Exception as error:
            with self.server.record_lock: self.server.errors.append(repr(error))


def run():
    from boat_probe import assert_no_client
    assert_no_client()
    stamp = str(time.time_ns())
    folder = lab.ROOT / ('protocol-selection-' + stamp)
    folder.mkdir()
    paper = next(r for r in lab.GRIM_VERSIONS if r['version'] == '1.12.2-grim')
    source = lab.ROOT / paper['version']
    # Verify both the downloaded Paper artifact and its embedded patched output.
    original = source / paper['launch_jar']
    assert hashlib.sha256(original.read_bytes()).hexdigest() == paper['platform_download']['checksums']['sha256']
    with zipfile.ZipFile(original) as jar:
        patch = dict(line.split('=', 1) for line in jar.read('patch.properties').decode().splitlines() if '=' in line and not line.startswith('#'))
    patched = source / 'cache/patched_1.12.2.jar'
    assert hashlib.sha256(patched.read_bytes()).hexdigest() == patch['patchedHash'].lower()
    shutil.copy2(patched, folder / 'server.jar')
    plugins = folder / 'plugins'
    plugins.mkdir()
    lab.download(VIA_URL, plugins / 'ViaVersion-5.12.0.jar', VIA_SHA, 'sha256')
    # These are new files in a new test instance; no original world is converted.
    (folder / 'eula.txt').write_text('eula=true\n')
    (folder / 'bukkit.yml').write_text('settings:\n  connection-throttle: 0\n')
    game, rcon = available_port(), available_port()
    row = dict(version=folder.name, port=game.getsockname()[1], rcon_port=rcon.getsockname()[1], protocol=340)
    password = secrets.token_hex(24)
    lab.save(folder / 'control.json', dict(password=password))
    props = {'server-ip': '127.0.0.1', 'server-port': row['port'], 'online-mode': 'false',
             'enable-rcon': 'true', 'rcon.port': row['rcon_port'], 'rcon.password': password,
             'broadcast-rcon-to-ops': 'false', 'enable-query': 'false', 'gamemode': 1, 'force-gamemode': 'false',
             'view-distance': 16, 'simulation-distance': 3, 'level-type': 'FLAT', 'generate-structures': 'false',
             'generator-settings': '3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;',
             'allow-flight': 'true', 'spawn-protection': 0, 'max-players': 8,
             'motd': 'Isolated ViaForge protocol-selection regression'}
    (folder / 'server.properties').write_text(''.join(f'{k}={v}\n' for k, v in props.items()))
    proxy = RecordingProxy(('127.0.0.1', 0), Forwarder)
    proxy.backend = row['port']; proxy.records = []; proxy.errors = []; proxy.record_lock = threading.Lock()
    thread = threading.Thread(target=proxy.serve_forever, daemon=True)
    thread.start()
    lab.save(folder / 'sources.json', dict(paper=paper['platform_download'], patched_sha256=patch['patchedHash'],
             viaversion=dict(version='5.12.0', url=VIA_URL, sha256=VIA_SHA), java=lab.java(paper),
             ports=dict(game=row['port'], rcon=row['rcon_port'], proxy=proxy.server_address[1])))
    state = folder / 'client.json'
    config = lab.REPO / 'run/ViaForge/viaforge.yml'
    shutil.copy2(config, folder / 'viaforge.yml.before')
    environment = dict(os.environ, VIAFORGE_NO_PAUSE='1', VIAFORGE_PROTOCOL_PROBE=str(state),
                       VIAFORGE_PROTOCOL_PORT=str(proxy.server_address[1]))
    for key in ('VIAFORGE_LIVE_FLIGHT', 'VIAFORGE_BLOCK_SMOKE_TEST', 'VIAFORGE_SMOKE_PROTOCOL'):
        environment.pop(key, None)
    server = client = None
    observations = []
    started = time.time()
    print('PROTOCOL PROBE', folder, flush=True)
    try:
        with (folder / 'server-console.log').open('w') as server_log, (folder / 'client-console.log').open('w') as client_log:
            game.close(); rcon.close()
            server = subprocess.Popen([lab.java(paper), '-Xms256M', '-Xmx1024M', '-DPaper.IgnoreJavaVersion=true',
                '-jar', 'server.jar', 'nogui'], cwd=folder, stdin=subprocess.PIPE, stdout=server_log,
                stderr=subprocess.STDOUT, text=True, creationflags=lab.NO_WINDOW)
            deadline = time.monotonic() + 100
            while True:
                if server.poll() is not None: raise RuntimeError('Paper failed to start')
                if time.monotonic() > deadline: raise TimeoutError('Paper startup')
                if lab.status(row):
                    try:
                        with lab.Rcon(row) as control:
                            observations.append(dict(command='viaversion list', response=control.command('viaversion list')))
                        break
                    except OSError:
                        pass  # The game port may open before RCON starts.
                time.sleep(1)
            client = subprocess.Popen(['cmd.exe', '/d', '/c', r'.\build.bat', 'runClient', '-x', 'preRunClient'],
                cwd=lab.REPO, env=environment, stdout=client_log, stderr=subprocess.STDOUT, creationflags=lab.NO_WINDOW)
            handled = set()
            deadline = time.monotonic() + 600
            result = {}
            while time.monotonic() < deadline:
                if state.exists():
                    result = json.loads(state.read_text())
                    if result['state'] in ('PASS', 'FAIL'): break
                    if result['state'] == 'joined' and result['case'] not in handled:
                        with lab.Rcon(row) as control:
                            observations.append(dict(case=result['case'], name=result['name'],
                                response=control.command('viaversion list'), players=control.command('list')))
                        handled.add(result['case'])
                        print('JOIN', result['joins'][-1], flush=True)
                if client.poll() is not None: break
                time.sleep(.2)
            client.wait(timeout=60)
            logins = [r['protocol'] for r in proxy.records if r['state'] == 2]
            expected = [340, 393, 340, 393, 763, 777, 340]
            ping_counts = Counter(r['protocol'] for r in proxy.records if r['state'] == 1)
            expected_names = ['1.12.2', '1.13', '1.12.2', '1.13', '1.20-1.20.1', '26.3', '1.12.2']
            server_matches = all(('[' + expected_names[o['case']] + ']') in re.sub('\u00a7.', '', o['response'])
                                 and o['name'] in o['response'] and o['name'] in o['players']
                                 for o in observations if 'case' in o)
            success = (result.get('state') == 'PASS' and client.returncode == 0 and logins == expected
                       and len(handled) == len(expected) and not proxy.errors and server_matches
                       and ping_counts == Counter({340: 42, 393: 42, 763: 42, 777: 42}))
            lab.save(folder / 'report.json', dict(success=success, started=started, ended=time.time(), result=result,
                     expected_login_protocols=expected, wire_login_protocols=logins, wire=proxy.records,
                     server_observations=observations, server_versions_match=server_matches, ping_protocol_counts=dict(ping_counts),
                     proxy_errors=proxy.errors, server='Paper 1.12.2 build 1620 + ViaVersion 5.12.0'))
            if not success: raise RuntimeError('Protocol selection regression failed: ' + str(folder / 'report.json'))
            print('PROTOCOL SELECTION PASS', folder / 'report.json', flush=True)
    finally:
        # The owned client closes itself. Never terminate an unrelated instance.
        if client is None or client.poll() is not None:
            shutil.copy2(folder / 'viaforge.yml.before', config)
        if server and server.poll() is None:
            server.stdin.write('save-all\nstop\n'); server.stdin.flush(); server.wait(timeout=60)
        proxy.shutdown(); proxy.server_close()
        game.close(); rcon.close()


if __name__ == '__main__': run()
