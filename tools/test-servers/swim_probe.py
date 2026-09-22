"""Actual player swimming in a backed-up isolated pool with active Grim."""
import argparse
import math

def exercise(row,folder,report,name,action,console):
    action('Creative login','idle',3)
    console(['gamemode survival '+name,'effect give '+name+' water_breathing 600 0 true'])
    action('Survival settle','idle',3)
    output=console(['execute if block 0 63 0 water[level=0] run say SWIM_POOL_OK'])
    if '[Server] SWIM_POOL_OK' not in output:raise RuntimeError('Water fixture not confirmed: '+output)
    for label,keys,seconds,pitch in [
        ('Underwater idle','idle',3,0),('Forward without sprint','forward',3,0),
        ('Sprint swim level','forward-sprint',5,0),('Swim up','forward-sprint',3,-30),
        ('Swim down','forward-sprint',3,30),('Ascend key','jump',3,0),
        ('Descend key','sneak',3,0),('Sprint diagonal','forward-right-sprint',4,0)]:
        console(['tp '+name+' 0.5 63 0.5 0 '+str(pitch)])
        action('Reset '+label,'idle',2)
        action(label,keys,seconds)
        if 'sprint' in keys:action('Coast after '+label,'idle',4)
    for label,keys in [('Dive while swimming','forward-sprint-sneak'),('Rise while swimming','forward-sprint-jump'),('Reverse underwater','back')]:
        console(['tp '+name+' 0.5 63 0.5 0 0']);action('Reset '+label,'idle',2);action(label,keys,3)
    # Start the pose before pressing shift too: 1.13's simultaneous start remains upright.
    console(['tp '+name+' 0.5 63 0.5 0 0']);action('Reset established dive','idle',2)
    action('Establish swimming','forward-sprint',1.5);action('Dive during established swim','forward-sprint-sneak',3)
    boots='minecraft:diamond_boots'+('[enchantments={"minecraft:depth_strider":3}]' if row['protocol']>=770 else
            '[enchantments={levels:{"minecraft:depth_strider":3}}]' if row['protocol']>=766 else
            '{Enchantments:[{id:"minecraft:depth_strider",lvl:3s}]}')
    equip=(f'item replace entity {name} armor.feet with ' if row['protocol']>=755 else f'replaceitem entity {name} armor.feet ')
    console([equip+boots,'tp '+name+' 0.5 63 -12.5 0 0']);action('Reset depth strider','idle',2)
    action('Depth strider swimming','forward-sprint',3);console([equip+'air'])
    console(['effect give '+name+' dolphins_grace 30 0 true','tp '+name+' 0.5 63 -12.5 0 0'])
    action('Reset dolphins grace','idle',2);action('Dolphins grace','forward-sprint',3)
    console(['effect clear '+name+' dolphins_grace'])
    console(['effect clear '+name+' water_breathing','effect give '+name+' conduit_power 30 0 true','tp '+name+' 0.5 63 0.5 0 0'])
    action('Reset conduit','idle',2);action('Conduit breathing','idle',3)
    console(['effect clear '+name+' conduit_power','effect give '+name+' water_breathing 600 0 true'])
    for x,block,label in [(-12,'soul_sand','Bubble lift'),(12,'magma_block','Bubble sink')]:
        console([f'fill {x-1} 40 -13 {x+1} 40 -11 {block}',f'fill {x-1} 41 -13 {x+1} 66 -11 water'])
        action('Reset '+label,'idle',3)
        console([f'tp {name} {x}.5 '+('45' if x<0 else '63')+' -12.5 0 0'])
        action(label,'idle',3)
    console([f'tp {name} 12.5 55 -12.8 0 0'])
    action('Bubble sink edge','idle',3)
    if report.get('swim_transitions'):
        if row['protocol']<477:raise ValueError('Confined pose transition requires original 1.14+ pose rules')
        # Prepare both fixtures while the player is still at the bubble column.
        # Keep the shore beside the passage: neither world edits around the player
        # nor a teleport into a just-replaced ceiling belong to this movement test.
        console(['fill -2 62 0 2 64 12 stone','fill 8 61 20 12 66 60 stone','tp '+name+' 0.5 61 -6.5 0 0'])
        action('Reset low passage','idle',2);action('Swim into low passage','forward-sprint',3)
        action('Rest inside low passage','idle',3);action('Swim out of low passage','forward-sprint',5)
        # Include enough submerged approach and ground for the full sprint/jump coast.
        # Ending the shore at z=40 let the test run off its own fixture into a cave.
        console(['tp '+name+' 10.5 63 10.5 0 0'])
        action('Reset shore','idle',2);action('Swim against shore','forward-sprint-jump',5)
        action('Rest on shore','idle',3)

def summarize(case,poses,events,name,protocol):
    ticks=[p for p in poses if p.get('phase')=='END' and case['start']*1000<=p['time_ms']<=case['end']*1000]
    selected=[e for e in events if e.get('player',e.get('name'))==name and case['start']<=e['time']<=case['end']]
    flags=[e for e in selected if e['type']=='flag'];setbacks=[e for e in selected if e['type']=='setback']
    predictions=[e for e in selected if e['type']=='prediction']
    result=dict(ticks=len(ticks),predictions=len(predictions),flags=flags,setbacks=setbacks,
                max_offset=max((e['offset'] for e in predictions),default=None),
                swimming_ticks=sum(p.get('height') in (.6,0.6000000238418579) or p.get('swimming',False) for p in ticks),
                heights=sorted({p['height'] for p in ticks if 'height' in p}),
                start_position=[ticks[0][k] for k in ('player_x','player_y','player_z')] if ticks else None,
                end_position=[ticks[-1][k] for k in ('player_x','player_y','player_z')] if ticks else None)
    if case['case'].startswith(('Reset ','Creative ','Survival ')):return result
    errors=[]
    for side in ['before','after']:
        state=case[side];p=next((p for p in state.get('players',[]) if p['name']==name),{})
        if state.get('state')!='enabled' or p.get('protocol')!=protocol or p.get('gamemode')!='SURVIVAL' or p.get('op',True) or p.get('disabled',True) or not p.get('verbose') or any(p.get(k,True) for k in ('exempt_permission','nosetback_permission','nomodifypacket_permission')):errors.append(side+': active Survival/ON/protocol not confirmed')
    minimum=2 if case['action'] in ('idle','sneak') else 10
    if len(ticks)<20 or len(predictions)<minimum:errors.append('Insufficient observed ticks/predictions')
    dry_case=case['case']=='Rest on shore'
    if not dry_case and sum(p.get('water',False) for p in ticks)<20:errors.append('No sustained observed water contact')
    if sum(p.get('action')==case['action'] for p in ticks)<20:errors.append('Requested input not observed')
    if flags or setbacks:errors.append('Grim flag or setback')
    if any(p.get('player_alive') is False for p in ticks):errors.append('Dead receiver')
    early_dive=protocol<477 and case['case']=='Dive while swimming'
    if 'sprint' in case['action'] and not early_dive and result['swimming_ticks']<20:errors.append('No sustained actual swimming pose')
    if early_dive and result['swimming_ticks']!=0:errors.append('1.13 must not start swimming with slowed shift input')
    if case['case']=='Dolphins grace' and sum(p.get('dolphins_grace',False) for p in ticks)<20:errors.append('Dolphins grace not present in actual client')
    if case['case']=='Depth strider swimming' and sum(p.get('depth_strider',0)>=3 or p.get('water_efficiency',0)>=1 for p in ticks)<20:errors.append('Depth strider not observed in actual client')
    if case['case']=='Conduit breathing' and (sum(p.get('conduit_power',False) for p in ticks)<20 or any(p.get('air',0)<299 for p in ticks)):errors.append('Conduit did not preserve underwater breath')
    if case['case']=='Rest inside low passage' and result['swimming_ticks']<20:errors.append('Confined swimming pose was lost')
    if case['case'] in ('Swim into low passage','Rest inside low passage') and (not ticks or not 0<=ticks[-1]['player_z']<=12 or ticks[-1]['player_y']>=62):errors.append('Low passage was not reached')
    if case['case']=='Swim out of low passage' and (not ticks or ticks[-1]['player_z']<=13):errors.append('Low passage was not exited')
    if dry_case and (not ticks or ticks[-1]['player_y']<66.99 or ticks[-1].get('water',True)):errors.append('Shore transition did not reach dry ground')
    if ticks and case['case'].startswith('Bubble '):
        dy=ticks[-1]['player_y']-ticks[0]['player_y']
        if (case['case']=='Bubble lift' and dy<4) or (case['case'].startswith('Bubble sink') and dy>-4):errors.append('Bubble effect did not move player in expected direction')
    if ticks and 'forward' in case['action'] and math.hypot(ticks[-1]['player_x']-ticks[0]['player_x'],ticks[-1]['player_z']-ticks[0]['player_z'])<1:errors.append('No substantial swimming movement')
    result.update(result='FAIL' if errors else 'PASS',reasons=errors)
    return result

def total(report, events):
    """Include the intervals between actions, equipment changes and teleport resets."""
    survival=next((c['start'] for c in report['cases'] if c['case']=='Survival settle'),float('inf'))
    issues=[e for e in events if e['type'] in ('flag','setback')]
    active=[e for e in issues if e['time']>=survival]
    cases=[c for c in report['cases'] if 'result' in c]
    return dict(result='PASS' if report['completed'] and cases and not active and all(c['result']=='PASS' for c in cases) else 'FAIL',
                survival_issues=active,login_issues=[e for e in issues if e['time']<survival],
                measured_cases=len(cases),passed_cases=sum(c['result']=='PASS' for c in cases))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--version',default='1.13.2')
    parser.add_argument('--label',default='swim');parser.add_argument('--client',choices=['viaforge','native'],default='viaforge')
    parser.add_argument('--baseline',action='store_true')
    parser.add_argument('--world-template',help='Copy a stopped reference world into the new isolated instance')
    parser.add_argument('--transitions',action='store_true',help='Also exercise a confined passage and the shore')
    args=parser.parse_args()
    import boat_probe
    boat_probe.run(args.version,args.label,args.client,baseline=args.baseline,scenario='swim',world_template=args.world_template,swim_transitions=args.transitions)
