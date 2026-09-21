"""Validate each active world, without rebuilding it. Back up before owned starts."""
import argparse
import concurrent.futures
import json
import time
import lab
import profiles
import world_audit


def saved(row):
    result = world_audit.audit(row)
    ground = json.loads((lab.ROOT / row['version'] / 'terrain-audit.json').read_text())
    success = not (result['unexpected'] or result.get('sign_issues') or ground['intrusions']
                   or ground['missing_chunks'] or not ground['flat'])
    return dict(success=success, blocks=result['samples'], signs=result.get('signs_checked', 0),
                block_issues=result['unexpected'], sign_issues=len(result.get('sign_issues', [])),
                terrain_intrusions=ground['intrusions'], missing_chunks=ground['missing_chunks'], flat=ground['flat'])


def one(row, saved_only=False, install_frogspawn=False):
    import arena
    import grim
    import snapshots
    folder = lab.ROOT / row['version']
    result = dict(version=row.get('profile_version', row['version']), instance=row['version'],
                  time=time.time(), success=False)
    owned = False
    try:
        grim.assert_stopped(row)
        if not saved_only:
            result['backup'] = str(snapshots.backup(row))
            commands = []
            if install_frogspawn:
                import exhibition
                commands = exhibition.prepare_frogspawn_station(row)
            owned = True
            lab.start([row])
            if commands:
                lab.batch(row, ['forceload add -80 -112 95 111'])
                time.sleep(3)
                errors = arena.error_lines(lab.batch(row, commands))
                if errors:
                    lab.batch(row, lab.release_chunks(row))
                    raise RuntimeError(str(errors[:5]))
                result['frogspawn_station_installed'] = True
            arena.verify(row)
            check = json.loads((folder / 'verification.json').read_text(encoding='utf-8'))
            result['live'] = {k: check[k] for k in ('success', 'time', 'checks', 'passed')}
            if row.get('grim_version'):
                actual = grim.state(row, True)
                result['grim'] = dict(state=actual['state'], version=row['grim_version'])
                if actual['state'] not in ('enabled', 'disabled'):
                    raise RuntimeError('Grim-Status nicht bestätigt: ' + str(actual))
            lab.stop([row]); owned = False
        result['saved'] = saved(row)
        result['success'] = result['saved']['success']
        if not result['success']:
            result['error'] = 'Gespeicherte Welt hat Abweichungen: ' + str(result['saved'])
    except Exception as error:
        result['error'] = str(error)
    finally:
        if owned and lab.status(row):
            try: lab.stop([row])
            except Exception as error: result.update(success=False, error=str(error))
    result['time'] = time.time()
    # Preserve every attempt; dashboard reads the current attempt, never a copied
    # PASS from a different world's original / reference directory.
    lab.save(folder / ('world-validation-' + str(time.time_ns()) + '.json'), result)
    lab.save(folder / 'world-validation.json', result)
    print('WORLD VALIDATION', result['version'], 'PASS' if result['success'] else 'FAIL', result.get('error', ''), flush=True)
    return result


def run(rows, saved_only=False, workers=1, install_frogspawn=False):
    report = lab.ROOT / ('active-world-survey-' + str(time.time_ns()) + '.json')
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as pool:
        tasks = [pool.submit(one, row, saved_only, install_frogspawn) for row in rows]
        for task in concurrent.futures.as_completed(tasks):
            results.append(task.result())
            lab.save(report, dict(time=time.time(), complete=len(results) == len(rows), results=results))
    print('REPORT', report, flush=True)
    return all(r['success'] for r in results)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('instances', nargs='?', default='active')
    parser.add_argument('--saved-only', action='store_true')
    parser.add_argument('--workers', type=int, choices=(1, 2), default=1)
    parser.add_argument('--install-frogspawn-stations', action='store_true',
                        help='After backup, add a water tank and manual replacement button for frogspawn')
    args = parser.parse_args()
    if args.saved_only and args.install_frogspawn_stations:
        parser.error('Frogspawn installation requires a backed-up server start')
    rows = profiles.select() if args.instances == 'active' else profiles.select(args.instances)
    if not run(rows, args.saved_only, args.workers, args.install_frogspawn_stations): raise SystemExit(1)
