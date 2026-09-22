"""Evidence gates for live boat cases; absence of flags alone never passes."""
import math
from collections import Counter


DRIVING = {'Rest in water', 'Accelerate', 'Full coast', 'Left curve', 'After turn',
           'Right curve', 'Reverse', 'Combined directions', 'Final rest', 'Reboard rest',
           'Water into solid shore', 'Rest at shore', 'Stone to water', 'Coast after land to water',
           'Ice accelerate and enter water', 'Ice/water coast', 'Drive before server teleport',
           'Drive after teleport', 'Coast after teleport', 'Drive after reconnect', 'Coast after reconnect',
           'Two players driver', 'Driver after seat transfer', 'Rest after seat transfer',
           'Flowing water drive', 'Flowing water rest', 'Submerge while driving',
           'Drive after immersion', 'Rest after immersion',
           'Driver with animal passenger', 'Rest with animal passenger'}
PASSENGER = {'Second player passenger', 'Passenger during native driving', 'Passenger coast'}


def summarize(case, poses, events, name, protocol):
    start, end = case['start'], case['end']
    selected = [e for e in events if start <= e['time'] <= end and e.get('player',e.get('name')) == name]
    ticks = [p for p in poses if p.get('phase') == 'END' and start*1000 <= p['time_ms'] <= end*1000]
    boats = [p for p in ticks if 'boat' in p]
    predictions = [e for e in selected if e['type'] == 'prediction']
    vehicle_predictions = [e for e in predictions if e.get('vehicle')]
    result = dict(predictions=len(predictions), vehicle_predictions=len(vehicle_predictions),
                  packet_counts=dict(Counter(e.get('packet') for e in selected if e['type']=='packet')),
                  flags=[e for e in selected if e['type']=='flag'], setbacks=[e for e in selected if e['type']=='setback'],
                  max_offset=max((e['offset'] for e in predictions),default=None),
                  ticks=len(ticks), driver_ticks=sum(p.get('driver',False) for p in boats),
                  passenger_ticks=sum(not p.get('driver',True) for p in boats),
                  states=sorted(set(p.get('status','unknown') for p in boats)),
                  collisions=sum(p.get('collision_h',False) for p in boats),
                  ground_ticks=sum(p.get('ground',False) for p in boats),
                  wood=sorted(set(p['wood'] for p in boats if 'wood' in p)))
    if boats:
        result['start_position']=[boats[0][k] for k in ('x','y','z')]
        result['end_position']=[boats[-1][k] for k in ('x','y','z')]
        result['horizontal_path']=sum(math.hypot(b['x']-a['x'],b['z']-a['z']) for a,b in zip(boats,boats[1:]))
        result['end_speed']=math.hypot(boats[-1]['vx'],boats[-1]['vz'])
        result['max_speed']=max(math.hypot(p['vx'],p['vz']) for p in boats)
        stationary=0
        for p in reversed(boats):
            if (p['x'],p['z'])!=(boats[-1]['x'],boats[-1]['z']):break
            stationary+=1
        result['stationary_tail_ticks']=stationary
    observations=[[p for p in case.get(s,{}).get('players',[]) if p.get('name')==name] for s in ('before','after')]
    users=[p for group in observations for p in group]
    confirmed=all(observations) and all(p.get('protocol')==protocol and p.get('gamemode')=='SURVIVAL' and p.get('op') is False
                  and p.get('disabled') is False and p.get('verbose') is True
                  and all(p.get(k) is False for k in ('exempt_permission','nosetback_permission','nomodifypacket_permission')) for p in users)
    result['active_survival_confirmed']=confirmed
    errors=[]
    if case['case']=='Reconnect same account':
        if not confirmed:errors.append('No confirmed active Survival account')
        if not any(e.get('number')==1 for e in vehicle_predictions):errors.append('No first vehicle prediction after reconnect')
        if len(vehicle_predictions)<20 or result['driver_ticks']<20:errors.append('Insufficient mounted reconnect observation')
    elif case['case'] in DRIVING:
        if not confirmed:errors.append('No confirmed non-OP Survival/ON/protocol/permissions state')
        minimum=max(5,int((end-start)*20*.9))
        if result['driver_ticks']<minimum:errors.append('Insufficient actual driver ticks')
        if len(vehicle_predictions)<minimum:errors.append('Insufficient vehicle predictions')
        if 'forward' in case['action'] or case['case']=='Reverse':
            if result.get('horizontal_path',0)<.1:errors.append('Missing actual travel')
        if case['case']=='Water into solid shore' and result['collisions']==0:errors.append('No shore collision observed')
        if case['case'] in ('Stone to water','Ice accelerate and enter water'):
            if not {'LAND','WATER'}.issubset(set(result['states'])):errors.append('Land/water transition absent')
        if case['case']=='Submerge while driving' and 'UNDER_WATER' not in result['states']:
            errors.append('No submerged driver state observed')
        if case['case']=='Flowing water drive' and 'FLOWING_WATER' not in result['states']:
            errors.append('No flowing-water driver state observed')
    elif case['case'] in PASSENGER:
        if not confirmed:errors.append('No confirmed active Survival account')
        if result['passenger_ticks']<max(5,int((end-start)*20*.7)):errors.append('Missing second-seat ticks')
        moves=[e for e in selected if e['type']=='packet' and e.get('packet')=='VEHICLE_MOVE']
        if moves:errors.append('Second passenger sent vehicle movement')
    else:
        errors.append('Setup/transition observation, not a driving compatibility assertion')
    result['coverage_limits']=errors
    result['result']='FAIL' if result['flags'] or result['setbacks'] else 'INCOMPLETE' if errors else 'PASS'
    return result


def evaluate(folder):
    """Re-evaluate retained evidence without overwriting its original report."""
    import json
    import hashlib
    from collections import Counter
    from pathlib import Path
    folder=Path(folder)
    report_path=folder/'report.json'
    report=json.loads(report_path.read_text(encoding='utf-8'))
    poses_file=folder/report.get('client','viaforge')/'client.jsonl'
    poses=[json.loads(s) for s in poses_file.read_text(encoding='utf-8').splitlines()] if poses_file.exists() else []
    events=report.get('events',[])
    name=report.get('name');protocol=report['row']['protocol']
    selected=[e for e in events if e.get('player',e.get('name'))==name]
    cases=[dict(case=c['case'],action=c['action'],start=c['start'],end=c['end'],
                **summarize(c,poses,selected,name,protocol)) for c in report['cases']]
    clients={tuple(p.get(k) for k in ('client','protocol','brand','gamemode','op','disabled','verbose'))
             for c in report['cases'] for phase in ('before','after')
             for p in c.get(phase,{}).get('players',[]) if p.get('name')==name and 'protocol' in p}
    return dict(folder=str(folder),report_sha256=hashlib.sha256(report_path.read_bytes()).hexdigest(),
                server=report['server'],client=report.get('client'),baseline=report.get('baseline'),
                recognized_clients=[dict(zip(('client','protocol','brand','gamemode','op','disabled','verbose'),row)) for row in sorted(clients,key=str)],
                name=name,completed=report.get('completed'),error=report.get('error'),
                flags=dict(Counter(e.get('check','unknown') for e in selected if e['type']=='flag')),
                setbacks=sum(e['type']=='setback' for e in selected),
                vehicle_predictions=sum(e['type']=='prediction' and e.get('vehicle',False) for e in selected),
                cases=cases)


if __name__=='__main__':
    import argparse
    import json
    from pathlib import Path
    parser=argparse.ArgumentParser(description='Re-evaluate real boat evidence; never edits historical reports.')
    parser.add_argument('folders',nargs='+');parser.add_argument('--out',type=Path,required=True)
    args=parser.parse_args()
    args.out.parent.mkdir(parents=True,exist_ok=True)
    args.out.write_text(json.dumps([evaluate(p) for p in args.folders],indent=2,ensure_ascii=False),encoding='utf-8')
