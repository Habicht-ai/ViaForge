"""Export compact, hashed evidence without changing historical probe reports."""
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path


def export():
    repo = Path(__file__).resolve().parents[2]
    runs = []
    root = repo / 'run/test-servers'
    folders = sorted({*root.glob('sneak-*'), *root.glob('swim-*-sneak-regression-final-*')})
    for folder in folders:
        path = folder / 'report.json'
        if not path.is_file():
            continue
        raw = path.read_bytes()
        report = json.loads(raw)
        summary = report.get('sneaking_summary', report.get('swimming_summary', {}))
        issues = summary.get('survival_issues', [])
        cases = []
        for case in report['cases']:
            entry = {k: case[k] for k in ('case', 'action', 'start', 'end', 'result', 'reasons',
                     'ticks', 'predictions', 'max_offset', 'heights', 'sprint_ticks') if k in case}
            entry['flags'] = len(case.get('flags', []))
            entry['setbacks'] = len(case.get('setbacks', []))
            for side in ('before', 'after'):
                state = case.get(side, {})
                player = next((p for p in state.get('players', []) if p['name'] == report.get('name')), {})
                entry[side] = dict(labac=state.get('state'), **{k: player[k] for k in (
                    'client', 'protocol', 'brand', 'gamemode', 'op', 'disabled', 'verbose',
                    'exempt_permission', 'nosetback_permission', 'nomodifypacket_permission') if k in player})
            cases.append(entry)
        runs.append(dict(folder=folder.name, report_sha256=hashlib.sha256(raw).hexdigest(),
                         completed=report.get('completed'), error=report.get('error'),
                         client=report['client'], version=report['server']['minecraft_version'],
                         protocol=report['row']['protocol'], baseline=report.get('baseline'),
                         artifact=report.get('client_artifact'),
                         result=summary.get('result'), measured=summary.get('measured_cases'),
                         passed=summary.get('passed_cases'),
                         survival_flags=sum(e['type'] == 'flag' for e in issues),
                         survival_setbacks=sum(e['type'] == 'setback' for e in issues),
                         login_issues=summary.get('login_issues', []),
                         creative_issues=summary.get('creative_issues', []),
                         sprint_actions=summary.get('sprint_actions', []),
                         duplicate_sprint_actions=summary.get('duplicate_sprint_actions', []), cases=cases))
    sources = repo / 'build/inspection/sneak/source-verification.json'
    evidence = dict(generated_utc=datetime.now(timezone.utc).isoformat(),
                    original_sources=json.loads(sources.read_text()) if sources.is_file() else [], runs=runs)
    target = repo / 'docs/SNEAK-EVIDENCE.json'
    target.write_text(json.dumps(evidence, ensure_ascii=True, indent=2) + '\n', encoding='utf-8')
    print(f'{target}: {len(runs)} retained reports')


if __name__ == '__main__':
    export()
