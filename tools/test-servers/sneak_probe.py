"""Real keyboard-driven land sneak/sprint sequences; retained worlds and active Grim."""
import argparse
import math
from swim_probe import total as swimming_total


def total(report, events):
    creative=next((c['start'] for c in report['cases'] if c['case'].startswith('Creative separate')),float('inf'))
    result=swimming_total(report,[e for e in events if e['time']<creative])
    result['creative_issues']=[e for e in events if e['time']>=creative and e['type'] in ('flag','setback')]
    if result['creative_issues']:result['result']='FAIL'
    sprint=False; duplicates=[]; actions=[]
    for event in events:
        if event.get('action') not in ('START_SPRINTING','STOP_SPRINTING'):continue
        next_sprint=event['action']=='START_SPRINTING'
        actions.append(dict(time=event['time'],action=event['action']))
        if next_sprint==sprint:duplicates.append(event)
        sprint=next_sprint
    result['sprint_actions']=actions
    result['duplicate_sprint_actions']=duplicates
    if duplicates:result['result']='FAIL'
    return result


def fixture(console, protocol, extended=False):
    console(['forceload add -32 -32 32 112',
             'fill -32 63 -32 32 63 112 stone',
             'fill -32 64 -32 32 66 112 air', 'setworldspawn 0 64 0'])
    if extended:
        console(['fill 19 65 6 21 65 16 stone_slab[type=top]',
                 'fill 9 64 12 11 66 12 stone',
                 'fill 27 64 6 29 64 6 stone_slab[type=bottom]',
                 'fill 27 64 7 29 64 10 stone',
                 'fill -14 61 8 -10 61 20 stone','fill -14 62 8 -10 63 20 air',
                 'fill -23 64 -3 -23 66 25 stone','fill -17 64 -3 -17 66 25 stone',
                 'fill -22 64 -3 -18 66 -3 stone',
                 'fill -22 64 -2 -18 66 12 water',
                 'fill -22 65 8 -18 66 24 stone','fill -22 64 13 -18 64 13 oak_sign[rotation=0]',
                 'fill -7 64 66 -5 64 66 stone_stairs[facing=south]',
                 'fill -7 64 67 -5 64 74 stone','fill -7 65 67 -5 65 67 stone_stairs[facing=south]',
                 'fill -7 65 68 -5 65 74 stone','fill -7 66 68 -5 66 68 stone_stairs[facing=south]',
                 'fill -7 66 69 -5 66 74 stone',
                 'fill -32 60 67 -26 60 94 stone','fill -32 61 67 -32 65 95 stone',
                 'fill -26 61 67 -26 65 95 stone','fill -31 61 95 -27 65 95 stone',
                 'fill -31 61 67 -27 65 67 oak_sign[rotation=0]',
                 'fill -31 61 68 -27 65 94 water'])


def sequence(steps):
    return 'sequence:'+ '|'.join(str(ticks)+':'+keys for ticks,keys in steps)


def exercise(row, folder, report, name, action, console):
    action('Creative login', 'idle', 3)
    console(['tp '+name+' 0.5 64 0.5 0 0'])
    action('Creative settle', 'idle', 3)
    check=console(['execute if block 0 63 0 stone if block 0 64 0 air if block 0 65 0 air run say SNEAK_FLOOR_OK'])
    if '[Server] SNEAK_FLOOR_OK' not in check:
        raise RuntimeError('Dry fixture not confirmed')
    console(['gamemode survival '+name])
    action('Survival settle', 'idle', 3)
    if report.get('sneak_timing'):
        for title,steps in [
            ('Double tap then sneak',[(2,'forward'),(2,'idle'),(40,'forward'),(30,'forward-sneak'),(40,'forward'),(60,'idle')]),
            ('Expired double tap',[(2,'forward'),(8,'idle'),(60,'forward'),(60,'idle')]),
            ('Sneak interrupts double tap',[(2,'forward'),(2,'sneak'),(60,'forward'),(60,'idle')])]:
            console(['tp '+name+' 0.5 64 0.5 0 0']);action('Reset '+title,'idle',2)
            action(title,sequence(steps),sum(t for t,k in steps)/20+.3)
        return
    if report.get('sneak_swift'):
        wet=report['sneak_swift']=='water'
        if wet:
            console(['tp '+name+' -29.5 61 80.5 0 0'])
            action('Reset water position','idle',3)
        console([f'item replace entity {name} armor.legs with minecraft:diamond_leggings{{Enchantments:[{{id:"minecraft:swift_sneak",lvl:3s}}]}}'])
        action('Reset swift sneak','idle',3);action('Swift sneak water' if wet else 'Swift sneak','forward-sneak',3)
        console([f'item replace entity {name} armor.legs with air']);action('Reset equipment restoration','idle',3)
        return
    if report.get('sneak_attributes'):
        for title,speed in [('Zero sneak speed',0),('Full sneak speed',1),('Restored sneak speed',.3)]:
            console([f'attribute {name} minecraft:sneaking_speed base set {speed}'])
            action('Reset '+title,'sneak',2)
            action(title,'forward-right-sneak',3)
            action('Reset coast '+title,'idle',3)
        return
    sequences = [
        ('Walk control', [('forward',3), ('idle',3)]),
        ('Sprint control', [('forward-sprint',3), ('idle',3)]),
        ('Sneak stand', [('sneak',2), ('idle',2)]),
        ('Sneak walk', [('forward-sneak',3), ('forward',2), ('idle',3)]),
        ('Sprint then sneak', [('forward-sprint',2), ('forward-sprint-sneak',3), ('forward-sprint',2), ('idle',3)]),
        ('Sneak then sprint', [('forward-sneak',2), ('forward-sneak-sprint',3), ('forward-sprint',2), ('idle',3)]),
        ('Simultaneous', [('forward-sprint-sneak',3), ('idle',3)]),
        ('Diagonal', [('forward-right-sprint',2), ('forward-right-sprint-sneak',3), ('forward-right',2), ('idle',3)]),
        ('Reverse', [('back-sneak-sprint',3), ('back',2), ('idle',3)]),
        ('Jump landing', [('forward-sprint-jump',2), ('forward-sprint-sneak-jump',2), ('forward-sneak',2), ('idle',3)])]
    for title, steps in sequences:
        console(['tp '+name+' 0.5 64 0.5 0 0'])
        action('Reset '+title, 'idle', 2)
        for index,(keys,seconds) in enumerate(steps):
            action(title+' / '+str(index+1),keys,seconds)
    if report.get('sneak_extended'):
        for title,base in [('Walk pulses','forward'),('Sprint pulses','forward-sprint'),('Jump pulses','forward-jump-sprint')]:
            console(['tp '+name+' 0.5 64 0.5 0 0']);action('Reset '+title,'idle',2)
            steps=[(20,base)]+[(ticks,keys) for _ in range(8) for ticks,keys in [(1,base+'-sneak'),(3,base)]]+[(60,'idle')]
            action(title,sequence(steps),sum(t for t,k in steps)/20+.3)
        for title,x,z,steps in [
            ('Wall contact',10.5,6.5,[('forward-sprint',2),('forward-sprint-sneak',2),('back-sneak',2),('idle',3)]),
            ('Slab and step',28.5,2.5,[('forward-sprint',3),('forward-sprint-sneak',2),('idle',3)]),
            ('Stairs',-5.5,61.5,[('forward',1.5),('forward-sprint-sneak',2),('back-sneak',3),('idle',3)]),
            ('Edge',-12.5,5.5,[('forward-sneak',4),('forward-sprint-sneak',2),('forward',1),('idle',3)]),
            ('Low ceiling',20.5,4.5,[('forward-sneak',3),('forward-sprint',3),('forward',5),('idle',3)]),
            ('Crawl water exit',-20.5,1.5,[('forward-sprint',5),('idle',3),('forward',7),('forward-sneak',3),('idle',3)]),
            ('Water entry',-29.5,63.5,[('forward-sprint',2),('forward-sprint-sneak',2),('idle',3)])]:
            console(['tp '+name+f' {x} 64 {z} 0 0']);action('Reset '+title,'idle',2)
            for index,(keys,seconds) in enumerate(steps):action(title+' / '+str(index+1),keys,seconds)
        if row['protocol']>=767:
            console(['tp '+name+' 0.5 64 0.5 0 0',f'attribute {name} minecraft:sneaking_speed base set 0.65'])
            action('Reset sneak attribute','idle',2)
            action('Sneak attribute','forward-sneak',3)
            console([f'attribute {name} minecraft:sneaking_speed base set 0.3'])
            action('Reset attribute restoration','idle',3)
        if row['protocol']>=759:
            console(['tp '+name+' 0.5 64 0.5 0 0'])
            leggings='minecraft:diamond_leggings'+('[enchantments={"minecraft:swift_sneak":3}]' if row['protocol']>=770 else
                    '[enchantments={levels:{"minecraft:swift_sneak":3}}]' if row['protocol']>=766 else '{Enchantments:[{id:"minecraft:swift_sneak",lvl:3s}]}')
            console([f'item replace entity {name} armor.legs with '+leggings])
            action('Reset swift sneak','idle',3);action('Swift sneak','forward-sneak',3)
            console([f'item replace entity {name} armor.legs with air']);action('Reset equipment restoration','idle',3)
        console(['gamemode creative '+name,'tp '+name+' 0.5 64 0.5 0 0'])
        action('Creative separate sneak','forward-sneak',3)
        action('Creative separate sprint sneak','forward-sprint-sneak',3)
        action('Creative separate rest','idle',3)


def summarize(case, poses, events, name, protocol):
    ticks=[p for p in poses if p.get('phase')=='END' and case['start']*1000<=p['time_ms']<=case['end']*1000]
    selected=[e for e in events if e.get('player',e.get('name'))==name and case['start']<=e['time']<=case['end']]
    predictions=[e for e in selected if e['type']=='prediction']
    flags=[e for e in selected if e['type']=='flag']; setbacks=[e for e in selected if e['type']=='setback']
    result=dict(ticks=len(ticks),predictions=len(predictions),flags=flags,setbacks=setbacks,
                max_offset=max((e['offset'] for e in predictions),default=None),
                heights=sorted({p['height'] for p in ticks if 'height' in p}),
                sprint_ticks=sum(p.get('sprinting',p.get('sprint',False)) for p in ticks))
    if case['case'].startswith(('Reset ','Creative ','Survival ')):return result
    errors=[]
    for side in ('before','after'):
        state=case[side]; player=next((p for p in state.get('players',[]) if p['name']==name),{})
        if state.get('state')!='enabled' or player.get('protocol')!=protocol or player.get('gamemode')!='SURVIVAL' or player.get('op',True) or player.get('disabled',True) or not player.get('verbose') or any(player.get(k,True) for k in ('exempt_permission','nosetback_permission','nomodifypacket_permission')):
            errors.append(side+': active Survival/ON/protocol not confirmed')
    moving=any(key in case['action'] for key in ('forward','back','right','left'))
    zero_speed=case['case']=='Zero sneak speed'
    blocked=case['case'].startswith(('Wall contact / 2','Edge / 2')) or zero_speed
    if len(ticks)<20 or len(predictions)<(10 if moving and not blocked else 2):errors.append('Insufficient actual ticks/predictions')
    if sum(p.get('action')==case['action'] for p in ticks)<20:errors.append('Input not observed')
    if any(p.get('loaded', 'floor' in p) is not True or p.get('paused',False) for p in ticks):errors.append('Loaded active client fixture not confirmed')
    if not case['case'].startswith(('Crawl water exit','Water entry','Swift sneak water')) and any(p.get('water',True) for p in ticks):errors.append('Dry conditions not confirmed')
    if case['case']=='Swift sneak water' and sum(p.get('water',False) for p in ticks)<20:errors.append('Water conditions not confirmed')
    if any(p.get('player_alive') is not True for p in ticks):errors.append('Living receiver not confirmed')
    if moving and not blocked and ticks and math.hypot(ticks[-1]['player_x']-ticks[0]['player_x'],ticks[-1]['player_z']-ticks[0]['player_z'])<1:errors.append('Insufficient actual movement')
    if blocked and not zero_speed and ticks and not any(p.get('collision_h') or case['case'].startswith('Edge') and 7<p['player_z']<8.5 for p in ticks):errors.append('Obstacle was not reached')
    if case['case'] in ('Zero sneak speed','Full sneak speed','Restored sneak speed'):
        expected={'Zero sneak speed':0,'Full sneak speed':1,'Restored sneak speed':.3}[case['case']]
        if sum(abs(p.get('sneak_speed',-1)-expected)<1e-6 for p in ticks)<20:errors.append('Boundary sneak attribute not observed')
        if not all(math.isfinite(p[k]) for p in ticks for k in ('player_x','player_y','player_z','player_vx','player_vy','player_vz')):errors.append('Non-finite position or velocity')
        if zero_speed and ticks and math.hypot(ticks[-1]['player_x']-ticks[0]['player_x'],ticks[-1]['player_z']-ticks[0]['player_z'])>1e-7:errors.append('Zero sneak speed moved the player')
    if case['case']=='Low ceiling / 2' and sum(p.get('height')==1.5 and not p.get('sneaking',True) for p in ticks)<30:errors.append('Forced crouching without shift not observed')
    if case['case']=='Low ceiling / 4' and (not ticks or ticks[-1].get('height')!=1.8):errors.append('Low ceiling was not exited')
    if case['case']=='Crawl water exit / 2' and sum(abs(p.get('height',0)-.6)<1e-6 and not p.get('water',True) and not p.get('sneaking',True) for p in ticks)<30:errors.append('Dry forced crawl without shift not observed')
    if case['case']=='Crawl water exit / 3' and (not ticks or ticks[-1].get('height')!=1.8):errors.append('Crawl tunnel was not exited')
    if case['case']=='Water entry / 1' and (not any(not p.get('water',True) for p in ticks) or sum(p.get('water',False) for p in ticks)<20):errors.append('Dry-to-water transition not observed')
    if case['case']=='Stairs / 2' and (not ticks or max(p['player_y'] for p in ticks)<66.9):errors.append('Stair ascent not observed')
    if case['case'] in ('Sneak attribute','Swift sneak','Swift sneak water'):
        expected=.65 if case['case']=='Sneak attribute' else .75
        if sum(abs(p.get('sneak_speed',-1)-expected)<1e-6 for p in ticks)<20:errors.append('Actual sneak speed not observed')
    sprint_expected=case['case']=='Sprint control / 1' or case['case']=='Sprint then sneak / 2' and protocol>=477 or case['case']=='Simultaneous / 1' and protocol>=573
    no_sprint_expected=case['case']=='Sneak then sprint / 2' or case['case']=='Sprint then sneak / 2' and protocol<477 or case['case']=='Simultaneous / 1' and protocol<573
    if sprint_expected and result['sprint_ticks']<20:errors.append('Original sprint state not retained/started')
    if no_sprint_expected and result['sprint_ticks']>2:errors.append('Sprint started where original slowed input forbids it')
    if case['case']=='Double tap then sneak' and result['sprint_ticks']<70:errors.append('Double tap did not establish/retain sprint')
    if case['case'] in ('Expired double tap','Sneak interrupts double tap') and result['sprint_ticks']>0:errors.append('Invalid double tap started sprint')
    if case['case'].endswith('pulses') and sum(p.get('input_sneak',p.get('sneaking',False)) for p in ticks)!=8:errors.append('Eight one-tick sneak pulses not observed')
    if case['case'].startswith(('Jump landing / 1','Jump landing / 2')) and sum(not p.get('ground',p.get('player_ground',True)) for p in ticks)<10:errors.append('Jump input did not cause observed airtime')
    if case['action']=='idle' and ticks and not ticks[-1].get('water',True) and math.hypot(ticks[-1].get('player_vx',float('inf')),ticks[-1].get('player_vz',float('inf')))>=.003:errors.append('Horizontal coast did not finish')
    if flags or setbacks:errors.append('Grim flag or setback')
    result.update(result='FAIL' if errors else 'PASS',reasons=errors)
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version',default='26.2');parser.add_argument('--label',default='land')
    parser.add_argument('--client',choices=['viaforge','native'],default='viaforge')
    parser.add_argument('--extended',action='store_true')
    parser.add_argument('--attributes-only',action='store_true')
    parser.add_argument('--baseline',action='store_true',help='Preserved d1d531c production with observation probe')
    parser.add_argument('--swift-only',action='store_true',help='Focused dry Swift Sneak case for 1.19-1.20.4')
    parser.add_argument('--swift-water-only',action='store_true',help='Repeat the discovered submerged Swift Sneak case on 1.19-1.20.4')
    parser.add_argument('--timing-only',action='store_true',help='Exact double-tap and sneak-interruption ticks')
    args=parser.parse_args()
    import boat_probe
    boat_probe.run(args.version,args.label,args.client,scenario='sneak',sneak_extended=args.extended,sneak_attributes=args.attributes_only,baseline=args.baseline,sneak_swift='water' if args.swift_water_only else args.swift_only,sneak_timing=args.timing_only)
