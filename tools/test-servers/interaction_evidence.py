"""Re-evaluate retained real-client reports without overwriting their raw evidence."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import interaction_probe


def evaluate(path):
    path=Path(path)
    report=json.loads(path.read_text(encoding='utf-8'))
    poses_path=path.parent/report['client']/'client.jsonl'
    poses=[json.loads(line) for line in poses_path.read_text().splitlines()] if poses_path.exists() else []
    events_path=path.parent/'plugins/ViaForgeLabAC/events.jsonl'
    events=[json.loads(line) for line in events_path.read_text(encoding='utf-8').splitlines()] if events_path.exists() else []
    events=[e for e in events if e.get('player',e.get('name'))==report.get('name')]
    cases=[]
    for case in report['cases']:
        case=dict(case)
        case.update(interaction_probe.summarize(case,poses,events,report.get('name'),report['row']['protocol']))
        cases.append(case)
    summary=interaction_probe.total(dict(report,cases=cases),events)
    def counts(rows):return dict(Counter(e.get('check',e['type']) for e in rows))
    return dict(folder=path.parent.name,client=report['client'],target=report['row']['protocol'],
        backend=report.get('backend_protocol'),baseline=report.get('baseline'),completed=report.get('completed'),
        error=report.get('error'),started=report['started'],ended=report.get('ended'),
        client_artifact=report.get('client_artifact'),real_player_target=report.get('real_player_target'),
        result=summary['result'],passed=summary['passed_cases'],measured=summary['measured_cases'],
        survival=counts(summary['survival_issues']),login=counts(summary['login_issues']),
        sequenced_packets=summary['sequenced_packets'],sequence_errors=len(summary['sequence_issues']),
        cases=[dict(case=c['case'],result=c['result'],reasons=c['reasons'],ticks=c['ticks'],
                    predictions=c['predictions'],max_offset=c['max_offset'],using_ticks=c['using_ticks'],
                    flags=counts(c['flags']),setbacks=len(c['setbacks']),
                    attack_order=c.get('attack_order'),attack_interleavings=c.get('attack_interleavings'),
                    before=c['before'],after=c['after']) for c in cases if 'result' in c],
        evidence={str(p.relative_to(path.parent)):dict(bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest())
                  for p in [path,poses_path,events_path,path.parent/'sources.json',path.parent/'client_artifact.json',
                            path.parent/report['client']/'native-source.json'] if p.exists()})


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('reports',nargs='+',type=Path);parser.add_argument('--output',required=True,type=Path)
    args=parser.parse_args()
    results=[evaluate(p) for p in args.reports]
    args.output.write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8')
    for r in results:print(r['folder'],r['result'],str(r['passed'])+'/'+str(r['measured']),r['survival'])
