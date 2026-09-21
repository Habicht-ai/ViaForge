"""Opt-in real web actions, offline logins and persisted player-data regression."""
import gzip
import hashlib
import json
import re
import sys
import time
import urllib.request
import uuid
import lab
import login_probe
import world_audit

URL = 'http://127.0.0.1:8765'


def state():
    with urllib.request.urlopen(URL + '/api/state', timeout=15) as response:
        return json.load(response)


def action(row, verb, backup_id=''):
    request = urllib.request.Request(URL + '/api/actions',
        json.dumps(dict(action=verb, versions=[row.get('profile_version', row['version'])], backup_id=backup_id)).encode(),
        {'Content-Type': 'application/json', 'X-Lab-Token': state()['token'], 'Origin': URL})
    with urllib.request.urlopen(request, timeout=15) as response:
        job_id = json.load(response)['id']
    deadline = time.monotonic() + 900
    while time.monotonic() < deadline:
        job = next(j for j in state()['jobs'] if j['id'] == job_id)
        if job['state'] in ('success', 'failed'):
            if job['state'] != 'success': raise RuntimeError(job['output'])
            print('WEB PASS', row['version'], verb, flush=True)
            return dict(action=verb, job=job_id, success=True)
        time.sleep(1)
    raise RuntimeError('Web action timed out: ' + verb)


def probe(row):
    if lab.status(row): raise RuntimeError('Probe requires a stopped server')
    name = 'VFweb' + str(time.time_ns())[-10:]
    folder = lab.ROOT / row['version']
    player_id = uuid.UUID(bytes=hashlib.md5(('OfflinePlayer:' + name).encode()).digest(), version=3)
    player_file = folder / ('world/players/data' if row['protocol'] >= 775 else 'world/playerdata') / (str(player_id) + '.dat')
    checks = []

    def joined(player, initial):
        with lab.Rcon(row) as remote:
            response = remote.command('data get entity ' + player + ' playerGameType')
        expected = 1 if initial else 0
        if not re.search(r'entity data:\s*' + str(expected) + r'\s*$', response):
            raise RuntimeError('Creative default / saved survival mode: ' + response)
        if initial: lab.batch(row, ['gamemode survival ' + player, 'give ' + player + ' obsidian 13'])

    try:
        checks.append(action(row, 'start'))
        login_probe.probe(row, lambda player: joined(player, True), 16, name)
        checks.append(action(row, 'verify'))
        checks.append(action(row, 'stop'))
        player_before = player_file.read_bytes()
        saved = world_audit.nbt(gzip.decompress(player_before))
        if saved.get('playerGameType') != 0 or not saved.get('Inventory'):
            raise RuntimeError('Expected saved survival mode and inventory')
        checks.append(action(row, 'restore'))
        if player_file.read_bytes() != player_before:
            raise RuntimeError('Restoration changed offline player data')
        checks.append(action(row, 'inspect'))
        checks.append(action(row, 'start'))
        login_probe.probe(row, lambda player: joined(player, False), 16, name, expected_spawn=None)
        checks.append(action(row, 'verify'))
        checks.append(action(row, 'stop'))
        after = world_audit.nbt(gzip.decompress(player_file.read_bytes()))
        if saved['Inventory'] != after['Inventory'] or after['playerGameType'] != 0:
            raise RuntimeError('Restart changed saved mode/inventory')
        lab.save(folder / 'web-lifecycle-probe.json', dict(success=True, time=time.time(), checks=checks,
                 new_player_creative=True, saved_mode_preserved=True, inventory_preserved=True,
                 restore_player_bytes_preserved=True, view_distance=16, restarted=True))
        print('WEB LIFECYCLE PASS', row['version'], flush=True)
    finally:
        if lab.status(row): action(row, 'stop')


if __name__ == '__main__':
    import profiles
    for row in profiles.select(sys.argv[1] if len(sys.argv) > 1 else '26.3'): probe(row)
