"""Fresh read-only saved-world survey, including extra terrain and missing chunks."""
import argparse
import time
import lab
import terrain


def audit(rows):
    report = lab.ROOT / ('terrain-survey-' + str(time.time_ns()) + '.json')
    results = []
    for row in rows:
        paused = False
        try:
            status = lab.status(row)
            if status:
                if status.get('players', {}).get('online', 0):
                    raise RuntimeError('Connected players; repeat this saved-world survey when vacant')
                # Flush once and suspend disk saves while reading the persisted snapshot.
                # The server keeps running. Always restore saving, even on an audit error.
                with lab.Rcon(row) as remote:
                    remote.command('save-off')
                    paused = True
                    remote.command('save-all flush')
            result = terrain.inspect(row)
            result['success'] = result['flat'] and not result['intrusions'] and not result['missing_chunks']
            result['saved_snapshot'] = True
            lab.save(lab.ROOT / row['version'] / 'terrain-audit.json',result)
            results.append(result)
        except Exception as ex:
            results.append(dict(version=row['version'], success=False, error=str(ex)))
        finally:
            if paused:
                with lab.Rcon(row) as remote: remote.command('save-on')
        lab.save(report, dict(time=time.time(), results=results, complete=len(results)==len(rows)))
        print(row['version'], {k:v for k,v in results[-1].items() if k in ('flat','intrusions','missing_chunks','error','success')}, flush=True)
    print(report, flush=True)
    return results


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('versions', nargs='?', default='all')
    import profiles
    audit(profiles.select(parser.parse_args().versions))
