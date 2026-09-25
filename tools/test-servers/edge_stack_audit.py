"""Summarize passive stack samples from a completed edge_stall_probe --stacks run."""
import argparse
import collections
import hashlib
import json
from pathlib import Path


def audit(run, diagnostic):
    report = run / 'report.json'
    data = json.loads(report.read_text())
    assert data['completed'], 'Only completed reports may be audited'
    start = next(c['start'] * 1000 for c in data['cases'] if c['case'] == 'Survival settle')
    end = max(c['end'] * 1000 for c in data['cases'])
    source = diagnostic / 'stacks.tsv'
    samples = collections.defaultdict(list)
    costs = []
    for line in source.read_text().splitlines():
        fields = line.split('\t')
        timestamp = int(fields[0])
        if not start <= timestamp <= end: continue
        if fields[2] == 'SAMPLER_COST_NS':
            costs.append(int(fields[3]) / 1e6)
        else:
            samples[fields[2]].append(dict(time_ms=timestamp, state=fields[3], stack=fields[4:]))
    repeated = []
    for thread, rows in samples.items():
        first = previous = None
        for row in [*rows, None]:
            if previous is not None and (row is None or row['stack'] != previous['stack'] or row['state'] != previous['state']):
                if previous['time_ms'] - first['time_ms'] >= 150:
                    repeated.append(dict(thread=thread, start_ms=first['time_ms'], end_ms=previous['time_ms'],
                        sampled_span_ms=previous['time_ms']-first['time_ms'], state=first['state'], stack=first['stack']))
                first = None
            if first is None: first = row
            previous = row
    issues = data['interaction_summary']['survival_issues']
    flags = [e for e in issues if e['type'] == 'flag']
    result = dict(run=run.name, diagnostic=str(diagnostic), delay_injected=False,
        sample_count={k:len(v) for k,v in samples.items()},
        sampler_cost_max_ms=max(costs, default=0),
        sampler_cost_p99_ms=sorted(costs)[int(len(costs)*.99)] if costs else None,
        repeated_stacks=repeated,
        flags_and_nearby_stacks=[dict(flag=e, repeated_stacks=[r for r in repeated
            if r['end_ms'] >= e['time']*1000-2500 and r['start_ms'] <= e['time']*1000+100]) for e in flags],
        sources={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (report, source)},
        limit='Repeated samples show where a thread was observed; they do not prove continuous blocking between samples.')
    (diagnostic / 'stack-analysis.json').write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
    print('Survival samples:', result['sample_count'], 'sampler max/p99 ms:', result['sampler_cost_max_ms'], result['sampler_cost_p99_ms'])
    for row in sorted(repeated, key=lambda r:r['sampled_span_ms'], reverse=True)[:12]:
        print(row['thread'], row['start_ms'], row['sampled_span_ms'], row['state'], '\n'+'\n'.join(row['stack'][:8]))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('run', type=Path)
    parser.add_argument('diagnostic', type=Path)
    args = parser.parse_args()
    audit(args.run, args.diagnostic)
