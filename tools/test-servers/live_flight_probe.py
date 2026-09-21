"""Opt-in real Forge flight test. Uses a fresh offline test player, never an existing player's data."""
import json
import os
import subprocess
import sys
import time
import lab


def probe(row, reuse_running=False):
    owned = not lab.status(row)
    if not owned and not reuse_running: raise RuntimeError('Server is running; use --reuse-running to attach without stopping it')
    folder = lab.REPO / 'build/logs'
    state = folder / ('live-flight-' + row['version'] + '-' + str(time.time_ns()) + '.json')
    environment = dict(os.environ, VIAFORGE_NO_PAUSE='1', VIAFORGE_LIVE_FLIGHT=str(state),
                       VIAFORGE_LIVE_PROTOCOL=str(row['protocol']), VIAFORGE_LIVE_PORT=str(row['port']))
    environment.pop('VIAFORGE_BLOCK_SMOKE_TEST', None)
    environment.pop('VIAFORGE_SMOKE_PROTOCOL', None)
    if owned: lab.start([row])
    launched = None
    try:
        with (folder / ('live-flight-' + row['version'] + '.log')).open('w') as log:
            launched = subprocess.Popen(['cmd.exe', '/d', '/c', r'.\build.bat', 'runClient', '-x', 'preRunClient'],
                cwd=lab.REPO, env=environment, stdout=log, stderr=subprocess.STDOUT, creationflags=lab.NO_WINDOW)
            handled = set()
            deadline = time.monotonic() + 360
            result = {}
            timeline = []
            observations = []
            last_observation = 0
            while time.monotonic() < deadline:
                if state.exists():
                    result = json.loads(state.read_text())
                    stage = result['state']
                    if row.get('grim_version') and time.monotonic() - last_observation > .9:
                        import grim
                        observations.append(dict(stage=stage, actual=grim.state(row, running=True)))
                        last_observation = time.monotonic()
                    if stage in {'FAIL', 'PASS'}: break
                    if stage not in handled:
                        timeline.append(dict(result))
                        name = result['name']
                        if stage == 'ready':
                            legacy = row['protocol'] < 393
                            slot = 'slot.armor.chest' if legacy else 'armor.chest'
                            equip = ('item replace entity ' + name + ' armor.chest with elytra') if row['protocol'] >= 755 else f'replaceitem entity {name} {slot} elytra'
                            commands = [f'gamemode {"0" if legacy else "survival"} {name}', equip]
                            if row['protocol'] >= 316:
                                commands.append(f'give {name} {"fireworks" if legacy else "firework_rocket"} 32')
                            commands.append(f'tp {name} -40 220 -53 -90 0')
                            lab.batch(row, commands)
                        elif stage == 'flying':
                            # 1.9 does not persist the fall-flight bit in entity NBT.
                            # The actual server metadata flag is asserted every tick in the Forge probe.
                            lab.batch(row, [f'tp {name} 64 159 -52'])
                        elif stage == 'water_landed':
                            # Existing clear strip on the navigation floor, no world edits.
                            lab.batch(row, [f'tp {name} -18 65 -83 -90 0'])
                        handled.add(stage)
                if launched.poll() is not None: break
                time.sleep(.25)
            if row.get('grim_version'):
                events = lab.ROOT / row['version'] / 'plugins/ViaForgeLabAC/events.jsonl'
                records = [json.loads(line) for line in events.read_text(encoding='utf-8').splitlines()]
                records = [e for e in records if e.get('player', e.get('name')) == result.get('name')]
                report = dict(server=row, client='ViaForge development client', result=result, stages=timeline,
                              observations=observations, events=records,
                              compatibility_pass=result.get('state') == 'PASS' and not any(e['type'] in ('flag','setback') for e in records),
                              time=time.time())
                lab.save(state.with_name(state.stem + '-grim.json'), report)
            if result.get('state') != 'PASS': raise RuntimeError('Live flight failed: ' + str(result))
            launched.wait(timeout=60)
            if launched.returncode != 0: raise RuntimeError('Forge process failed')
            report = lab.ROOT / row['version'] / 'live-flight-probe.json'
            if report.exists():
                import shutil
                shutil.copy2(report, report.with_name(report.name + '.' + str(time.time_ns()) + '.bak'))
            lab.save(report, dict(result, success=True, report=str(state)))
            print('LIVE FLIGHT SEQUENCE PASS', row['version'], '(Grim findings are separate)' if row.get('grim_version') else '', flush=True)
    finally:
        # The development client owns its normal shutdown. Never terminate another client.
        if owned: lab.stop([row])


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('versions', nargs='?', default='1.9,1.12.2,26.3')
    parser.add_argument('--reuse-running', action='store_true', help='Attach to an already running server and leave it running')
    args = parser.parse_args()
    for row in lab.select(args.versions): probe(row, args.reuse_running)
