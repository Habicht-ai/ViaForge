"""Boot each vacant installed variant and require actual Grim + bridge state, not a listening port."""
import concurrent.futures
import json
import shutil
import time
import zipfile
import lab
import grim


def one(row):
    folder = lab.ROOT / row['version']
    existing = lab.status(row)
    if existing: return dict(version=row['version'], skipped='Already running; use dedicated live probes')
    result = dict(version=row['version'], platform=row['platform'], build=row['platform_build'],
                  minecraft=row['minecraft_version'], grim=row['grim_version'], java=lab.java(row), started=time.time())
    try:
        grim.assert_stopped(row)
        grim.update_bridge(row)
        lab.start([row])
        until = time.monotonic() + 15
        while time.monotonic() < until:
            actual = grim.state(row, running=True)
            if actual.get('state') in ('enabled','disabled'): break
            time.sleep(.5)
        result['actual'] = actual
        with lab.Rcon(row) as rcon:
            result['rcon_status'] = rcon.command('labac status')
            result['version_output'] = rcon.command('version')
        config = folder / 'plugins/GrimAC/config.yml'
        result['elytra_default'] = next((s.strip() for s in config.read_text(encoding='utf-8').splitlines()
                    if 'allow-sprint-jumping-when-using-elytra:' in s), None) if config.exists() else None
        result['success'] = actual.get('state') in ('enabled','disabled') and result['elytra_default'] == 'allow-sprint-jumping-when-using-elytra: true'
    except Exception as ex:
        result.update(success=False, error=str(ex))
    finally:
        if lab.status(row): lab.stop([row])
    result['finished'] = time.time()
    lab.save(folder / 'grim-startup-probe.json', result)
    print(row['version'], 'PASS' if result['success'] else 'FAIL', result.get('error',''), flush=True)
    return result


if __name__ == '__main__':
    import argparse
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('versions', nargs='?', default='grim')
    rows=lab.select(parser.parse_args().versions)
    report=lab.ROOT / ('grim-platform-survey-' + str(time.time_ns()) + '.json')
    results=[]
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        futures=[pool.submit(one,row) for row in rows]
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())
            lab.save(report,dict(complete=len(results)==len(rows),results=results))
    print(report,flush=True)
