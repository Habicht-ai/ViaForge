"""Exercise canonical-version web routes on an already validated, vacant world."""
import json
import time
import urllib.request
import lab
import profiles
import grim
from web_lifecycle_probe import action, state, URL


def run(version='1.12.2'):
    row = profiles.select(version)[0]
    grim.assert_stopped(row)
    checks = []
    original = grim.state(row, False).get('requested_enabled', True)
    try:
        before = state()
        assert len(before['servers']) == len(lab.VANILLA_VERSIONS)
        assert all('-grim' not in r['version'] for r in before['servers'])
        checks.append(action(row, 'start'))
        assert lab.status(row)['players']['online'] == 0
        for verb, expected in [('ac-off', 'disabled'), ('ac-status', 'disabled'), ('ac-on', 'enabled')]:
            checks.append(action(row, verb))
            actual = grim.state(row, True)
            assert actual['state'] == expected, actual
        if not original: checks.append(action(row, 'ac-off'))
        checks.append(action(row, 'stop'))
        checks.append(action(row, 'backup'))
        with urllib.request.urlopen(URL + '/api/servers/' + version + '/backups') as response:
            backup = json.load(response)[0]
        assert (lab.ROOT / row['version'] / 'backups' / backup['id']).is_dir()
        checks.append(action(row, 'restore-backup', backup['id']))
        restored = json.loads((lab.ROOT / row['version'] / 'snapshot-restore.json').read_text())
        assert restored['source'] == backup['id'] and restored['success']
        assert grim.state(row, False)['requested_enabled'] == original
        checks.append(action(row, 'start'))
        checks.append(action(row, 'verify'))
        checks.append(action(row, 'stop'))
        with urllib.request.urlopen(URL + '/api/servers/' + version + '/catalog') as response:
            assert json.load(response)['blocks']
        checks.append(action(row, 'inspect'))
        lab.save(lab.ROOT / row['version'] / 'profile-web-probe.json', dict(success=True, time=time.time(),
            public_version=version, instance=row['version'], checks=checks, profiles=len(before['servers']),
            backup=backup['id'], stored_ac_state_restored=True))
        print('PROFILE WEB PASS', version, flush=True)
    finally:
        if lab.status(row):
            grim.control(row, 'on' if original else 'off')
            action(row, 'stop')


if __name__ == '__main__': run()
