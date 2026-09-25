"""Correlate a completed edge run with passive JVM-pause and host-clock logs."""
import argparse
import collections
import csv
import datetime
import hashlib
import json
from pathlib import Path
import re


def audit(run, diagnostic):
    report = run / 'report.json'
    data = json.loads(report.read_text())
    assert data['completed'], 'Only completed reports may be audited'
    start = next(c['start'] * 1000 for c in data['cases'] if c['case'] == 'Survival settle')
    end = max(c['end'] * 1000 for c in data['cases'])
    ticks_file = run / 'viaforge/client.jsonl'
    ticks = [json.loads(line) for line in ticks_file.read_text().splitlines()]
    ticks = [t for t in ticks if t.get('phase') == 'END' and start <= t['time_ms'] <= end]
    gaps = [dict(milliseconds=b['time_ms'] - a['time_ms'], start_ms=a['time_ms'],
                 end_ms=b['time_ms'], action=b['action'], tick=b['player_tick'])
            for a, b in zip(ticks, ticks[1:])]
    pattern = (r'(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}\+\d{4}): [0-9.]+: '
               r'Total time for which application threads were stopped: ([0-9.]+) seconds')
    gc_file = diagnostic / 'client-gc.log'
    pauses = []
    for match in re.finditer(pattern, gc_file.read_text()):
        timestamp = datetime.datetime.fromisoformat(match[1]).timestamp() * 1000
        if start <= timestamp <= end:
            pauses.append(dict(end_ms=timestamp, milliseconds=float(match[2]) * 1000))
    clock_file = diagnostic / 'clock.csv'
    with clock_file.open() as stream:
        clock = [(int(r['utc_ns']), int(r['monotonic_ns'])) for r in csv.DictReader(stream)]
    intervals = [dict(utc_ms=(b[0]-a[0])/1e6, monotonic_ms=(b[1]-a[1])/1e6, end_ms=b[0]/1e6)
                 for a, b in zip(clock, clock[1:]) if start <= b[0]/1e6 <= end]
    issues = data['interaction_summary']['survival_issues']
    flags = [e for e in issues if e['type'] == 'flag']
    clusters = [e for i, e in enumerate(flags) if i == 0 or e['time'] - flags[i-1]['time'] > 2]
    result = dict(run=run.name, diagnostic=str(diagnostic), survival_start_ms=start,
        survival_end_ms=end, summary=data['interaction_summary'], delay_injected=False,
        max_tick_gap_ms=max(g['milliseconds'] for g in gaps),
        max_jvm_stopped_ms=max(p['milliseconds'] for p in pauses),
        max_host_monotonic_interval_ms=max(p['monotonic_ms'] for p in intervals),
        max_host_clock_discrepancy_ms=max(abs(p['utc_ms']-p['monotonic_ms']) for p in intervals),
        longest_tick_gaps=sorted(gaps, key=lambda g:g['milliseconds'], reverse=True)[:15],
        flag_clusters=[dict(first_flag=e,
            preceding_tick_gaps=[g for g in gaps if g['milliseconds'] > 150 and e['time']*1000-2500 < g['end_ms'] <= e['time']*1000],
            nearby_jvm_stops=[p for p in pauses if abs(p['end_ms']-e['time']*1000) < 1500]) for e in clusters],
        sources={str(p): hashlib.sha256(p.read_bytes()).hexdigest()
                 for p in (report, ticks_file, gc_file, clock_file)})
    (diagnostic / 'analysis.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k not in ('summary','sources','longest_tick_gaps','flag_clusters')}, indent=2))
    print('Survival issues:', dict(collections.Counter(e.get('check',e['type']) for e in issues)))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('run', type=Path)
    parser.add_argument('diagnostic', type=Path)
    args = parser.parse_args()
    audit(args.run, args.diagnostic)
