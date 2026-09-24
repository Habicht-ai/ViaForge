"""Real input reproduction for collision shapes, item use and packet order."""
import argparse
import math
from contextlib import contextmanager
from swim_probe import total as movement_total


def attack_interleavings(packets):
    pending=False;issues=[]
    for p in packets:
        kind=p['packet']
        if kind=='ATTACK' or kind=='INTERACT_ENTITY' and p.get('action')=='ATTACK':pending=True
        elif kind=='ANIMATION' and p.get('hand','MAIN_HAND')=='MAIN_HAND':pending=False
        elif pending and kind in ('PLAYER_INPUT','ENTITY_ACTION','PLAYER_POSITION','PLAYER_POSITION_AND_ROTATION',
                                  'PLAYER_ROTATION','PLAYER_FLYING','CLIENT_TICK_END','USE_ITEM','PLAYER_DIGGING','HELD_ITEM_CHANGE'):
            issues.append(p)
    if pending:issues.append(dict(packet='MISSING_SWING'))
    return issues


def total(report, events):
    result=movement_total(report,events)
    expected=1;issues=[];sequenced=[]
    for e in events:
        if e['type']!='packet' or 'sequence' not in e:continue
        if report.get('backend_protocol',776)<759:continue
        increment=e['packet'] in ('USE_ITEM','PLAYER_BLOCK_PLACEMENT') or e.get('action') in ('START_DIGGING','FINISHED_DIGGING')
        wanted=expected if increment else 0
        if e['sequence']!=wanted:issues.append(dict(time=e['time'],packet=e['packet'],action=e.get('action'),expected=wanted,actual=e['sequence']))
        if increment:expected=e['sequence']+1
        sequenced.append(e)
    result.update(sequence_issues=issues,sequenced_packets=len(sequenced))
    if issues:result['result']='FAIL'
    return result


def fixture(console, protocol):
    legacy=protocol<393
    console(([] if legacy else ['forceload add -24 -16 24 64'])+[
             'fill -24 63 -16 24 63 64 stone',
             'fill -24 64 -16 24 69 64 air', 'fill -24 70 -16 24 75 64 air', 'setworldspawn 0 64 0',
             'fill 9 64 10 11 74 10 stone',
             'fill 10 64 9 10 74 9 ladder 2' if legacy else 'fill 10 64 9 10 74 9 ladder[facing=north]',
             'fill -12 62 20 -8 62 48 stone', 'fill -12 63 20 -8 63 48 water',
             'fill -12 64 20 -8 64 48 waterlily' if legacy else 'fill -12 64 20 -8 64 48 lily_pad',
             'setblock 0 65 3 stone'])


def exercise(row, folder, report, name, action, console):
    # Start both renderers while still Creative; frame stalls during another
    # client's initial resource load must not contaminate Survival transitions.
    if report.get('interaction_rightclick'):
        return rightclick_cases(row,folder,report,name,action,console)
    if report.get('interaction_player'):
        with player_target(row,folder) as target:
            report['real_player_target']=target
            exercise_cases(row,folder,report,name,action,console)
    else:exercise_cases(row,folder,report,name,action,console)


def exercise_cases(row, folder, report, name, action, console):
    backend=report.get('backend_protocol',row['protocol']);legacy=backend<393;old_server=backend<=47
    if legacy:fixture(console,47)
    action('Creative login','idle',3)
    console([f'tp {name} 0.5 64 0.5 0 0'])
    action('Creative load','idle',25)
    console([f'gamemode survival {name}'])
    action('Survival settle','idle',3)
    def reset(title,x=.5,y=64,z=.5,yaw=0,pitch=0,item='air'):
        if legacy:
            equipment=[f'replaceitem entity {name} slot.hotbar.0 {item} 1']+([] if old_server else [f'replaceitem entity {name} slot.weapon.offhand air 1'])
        elif backend<755:
            equipment=[f'replaceitem entity {name} weapon.mainhand {item}',f'replaceitem entity {name} weapon.offhand air']
        else:
            equipment=[f'item replace entity {name} weapon.mainhand with {item}',f'item replace entity {name} weapon.offhand with air']
        console([*equipment,
                 f'tp {name} {x} {y} {z} {yaw} {pitch}'])
        action('Reset '+title,'idle',3)
    reset('walk',x=5.5)
    action('Walk control','forward',3);action('Walk coast','idle',2)
    reset('ladder',x=10.5,z=7.5)
    action('Ladder forward','forward',4)
    action('Ladder hold sneak','sneak',2)
    action('Ladder release','idle',2)
    reset('ladder jump',x=10.5,z=9.5)
    action('Ladder jump','jump',3)
    reset('lily pads',x=-10.5,z=18.5)
    action('Lily walk','forward',4);action('Lily coast','idle',2)
    reset('lily sprint',x=-10.5,z=18.5)
    action('Lily sprint','forward-sprint',3)
    if not old_server:
        reset('shield air',x=5.5,item='shield')
        action('Shield walk','forward-use',3);action('Shield release','forward',2)
        reset('shield block',item='shield')
        action('Shield on block','use',2);action('Reset release','idle',1)
    reset('plain sword',x=5.5,item='diamond_sword')
    action('Legacy sword block' if old_server else 'Plain sword control','forward-use',3)
    action('Legacy sword release' if old_server else 'Reset sword release','forward',2)
    if row['protocol']>=770 and not old_server:
        reset('blocking sword',x=5.5,item='diamond_sword[blocks_attacks={block_delay_seconds:0.0,disable_cooldown_scale:0.0,item_damage:{threshold:0.0,base:0.0,factor:0.0}}]')
        action('Blocking sword component','forward-use',3);action('Sword release','forward',2)
    reset('attack mob',x=16.5,pitch=15,item='diamond_sword')
    if report.get('interaction_player'):
        attack_player(row,folder,report,name,action,console)
    else:
        console(['summon '+('Cow' if backend<315 else 'cow')+' 16.5 64 3.0 {NoAI:1b,Invulnerable:1b,Silent:1b}'])
        action('Reset mob visible','idle',2)
        action('Attack entity','sequence:'+'|'.join(['1:attack','9:idle']*8),4.2)
        action('Attack sneak transition','sequence:'+'|'.join(['1:attack-sneak','9:idle']*8),4.2)
    reset('dig',item='diamond_pickaxe')
    action('Mine stone','attack',3)
    action('Final coast','idle',3)


def rightclick_cases(row, folder, report, name, action, console):
    action('Creative login','idle',3)
    console([f'tp {name} 0.5 64 0.5 37 60'])
    action('Creative load','idle',25)
    console([f'gamemode survival {name}'])
    action('Survival settle','idle',3)
    blocking='diamond_sword[blocks_attacks={block_delay_seconds:0.0,disable_cooldown_scale:0.0,item_damage:{threshold:0.0,base:0.0,factor:0.0}}]'
    cases=[('Plain sword ground','diamond_sword',60,'use'),
           ('Stick ground turn','stick',60,'use-turn'),
           ('Stick air turn','stick',-30,'use-turn'),
           ('Shield ground turn','shield',60,'sequence:'+'|'.join(['2:use-turn','6:turn']*8)),
           ('Shield air turn','shield',-30,'sequence:'+'|'.join(['2:use-turn','6:turn']*8)),
           ('Blocks ground turn','stone',60,'use-turn')]
    if row['protocol']>=770:cases.extend([
        ('Blocking sword pose',blocking,0,'use-photo'),
        ('Blocking sword ground turn',blocking,60,'sequence:'+'|'.join(['2:use-turn','6:turn']*8))])
    for title,item,pitch,keys in cases:
        output=console([f'item replace entity {name} weapon.mainhand with {item} {64 if item=="stone" else 1}',f'item replace entity {name} weapon.offhand with air',f'tp {name} 0.5 64 0.5 37 {pitch}',f'execute if items entity {name} weapon.mainhand {item.split("[")[0]} run say RIGHTCLICK_EQUIPMENT_OK'])
        if '[Server] RIGHTCLICK_EQUIPMENT_OK' not in output:raise RuntimeError('Actual right-click equipment not confirmed: '+output)
        action('Reset '+title,'idle',3)
        action('Rightclick '+title,keys,3.4)
        action('Reset release','idle',1)
    action('Final coast','idle',3)



@contextmanager
def player_target(row,folder):
    import os,subprocess,time
    import lab,native_push_probe,boat_probe
    if row['protocol']!=776:raise ValueError('Companion mapped to exact original 26.2')
    target=folder/'target-original';target.mkdir()
    target_name='VFtarget'+str(time.time_ns())[-7:]
    launch=native_push_probe.command(target,row['port'],name=target_name)
    child=None
    try:
        with (target/'console.log').open('w') as output:
            child=subprocess.Popen(launch,cwd=lab.REPO,env=dict(os.environ,VIAFORGE_SWIM_PROBE='1'),stdout=output,stderr=subprocess.STDOUT,creationflags=lab.NO_WINDOW)
        deadline=time.monotonic()+120
        while time.monotonic()<deadline:
            state=boat_probe.read_json(target/'client-state.json')
            if state.get('state')=='ready':break
            if state.get('state')=='FAIL' or child.poll() is not None:raise RuntimeError('Companion failed: '+str(state))
            time.sleep(.2)
        else:raise TimeoutError('Companion login')
        yield dict(name=target_name,client='original 26.2',source=boat_probe.read_json(target/'native-source.json'))
    finally:
        if child is not None and child.poll() is None:
            boat_probe.write_action(target,'stop');child.wait(timeout=60)


def attack_player(row,folder,report,name,action,console):
    target_name=report['real_player_target']['name']
    console([f'gamemode creative {target_name}',f'tp {target_name} 16.5 64 3.0 180 0',f'tp {name} 16.5 64 0.5 0 10'])
    action('Reset real target visible','idle',3)
    action('Attack entity','sequence:'+'|'.join(['1:attack','9:idle']*8),4.2)
    action('Attack sneak transition','sequence:'+'|'.join(['1:attack-sneak','9:idle']*8),4.2)


def summarize(case, poses, events, name, protocol):
    ticks=[p for p in poses if p.get('phase')=='END' and case['start']*1000<=p['time_ms']<=case['end']*1000]
    selected=[e for e in events if case['start']<=e['time']<=case['end']]
    predictions=[e for e in selected if e['type']=='prediction']
    flags=[e for e in selected if e['type']=='flag'];setbacks=[e for e in selected if e['type']=='setback']
    packets=[e for e in selected if e['type']=='packet']
    result=dict(ticks=len(ticks),predictions=len(predictions),flags=flags,setbacks=setbacks,
                max_offset=max((e['offset'] for e in predictions),default=None),
                using_ticks=sum(p.get('using_item',False) for p in ticks),
                ladder_ticks=sum(p.get('ladder',False) for p in ticks),
                y_range=[min((p['player_y'] for p in ticks),default=None),max((p['player_y'] for p in ticks),default=None)],
                packets=packets)
    if case['case'].startswith(('Reset ','Creative ','Survival ')):return result
    errors=[]
    for side in ('before','after'):
        state=case[side];p=next((p for p in state.get('players',[]) if p['name']==name),{})
        if state.get('state')!='enabled' or p.get('protocol')!=protocol or p.get('gamemode')!='SURVIVAL' or p.get('op',True) or p.get('disabled',True) or not p.get('verbose') or any(p.get(k,True) for k in ('exempt_permission','nosetback_permission','nomodifypacket_permission')):
            errors.append(side+': Survival/Grim conditions not confirmed')
    if len(ticks)<20 or any(not p.get('loaded',False) or p.get('paused',True) or not p.get('player_alive',False) for p in ticks):errors.append('Active loaded real client not confirmed')
    moving=any(k in case['action'] for k in ('forward','jump'))
    if moving and len(predictions)<15:errors.append('Insufficient predictions')
    if sum(p.get('action')==case['action'] for p in ticks)<20:errors.append('Actual requested input not observed')
    if moving and ticks and case['case'] not in ('Ladder jump',) and math.dist(
            [ticks[0][k] for k in ('player_x','player_y','player_z')],
            [ticks[-1][k] for k in ('player_x','player_y','player_z')])<1:errors.append('Insufficient real movement')
    if case['case'].startswith('Ladder') and result['ladder_ticks']<10:errors.append('Ladder not reached')
    if (case['case']=='Ladder forward' or case['case']=='Ladder jump' and protocol>=477) and ticks and result['y_range'][1]-ticks[0]['player_y']<2:errors.append('Did not climb')
    if case['case'] in ('Lily walk','Lily sprint') and not any(20<=p['player_z']<=48 and abs(p['player_y']-64.09375)<1e-7 for p in ticks):errors.append('Original lily collision surface not observed')
    if case['case'] in ('Shield walk','Shield on block','Blocking sword component','Legacy sword block') and result['using_ticks']<20:errors.append('Actual sustained item use not observed')
    if case['case']=='Plain sword control' and result['using_ticks']:errors.append('Plain modern sword unexpectedly usable')
    if case['case'] in ('Attack entity','Mine stone') and not any(p.get('packet') in ('INTERACT_ENTITY','ATTACK','PLAYER_DIGGING') for p in packets):errors.append('Actual interaction packets not observed')
    if case['case'].startswith('Attack '):
        order=['attack' if p['packet']=='ATTACK' or p['packet']=='INTERACT_ENTITY' and p.get('action')=='ATTACK' else 'swing'
               for p in packets if p['packet'] in ('ATTACK','ANIMATION') or p['packet']=='INTERACT_ENTITY' and p.get('action')=='ATTACK']
        result['attack_order']=order
        result['attack_interleavings']=attack_interleavings(packets)
        if order.count('attack')<5:errors.append('Insufficient real attacks')
        if protocol>=107 and order!=['attack','swing']*(len(order)//2):errors.append('Attack/swing order differs from original')
        if protocol>=107 and result['attack_interleavings']:errors.append('State/movement packet between attack and swing')
    if case['case'].startswith('Rightclick '):
        uses=[p for p in packets if p['packet']=='USE_ITEM']
        places=[p for p in packets if p['packet']=='PLAYER_BLOCK_PLACEMENT']
        if not uses and not places:errors.append('No actual right-click packets')
        if 'ground' in case['case'] and not places:errors.append('Ground block was not targeted')
        if 'turn' in case['action'] and len({p['yaw'] for p in ticks})<20:errors.append('Camera turn not observed')
        if 'Blocking sword' in case['case'] and result['using_ticks']<10:errors.append('Component blocking not observed')
        if 'Plain sword' in case['case'] and result['using_ticks']:errors.append('Plain modern sword unexpectedly blocks')
        if 'Blocks ground' in case['case'] and protocol>=393:
            if uses or any(p.get('hand')!='MAIN_HAND' for p in places):errors.append('Rejected placement incorrectly falls through to air use or offhand')
    if flags or setbacks:errors.append('Grim flags or setbacks')
    result.update(result='FAIL' if errors else 'PASS',reasons=errors)
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version',default='26.2');parser.add_argument('--label',default='baseline')
    parser.add_argument('--client',choices=['viaforge','native'],default='viaforge')
    parser.add_argument('--backend',choices=['1.8.8'])
    parser.add_argument('--baseline',action='store_true')
    parser.add_argument('--rightclick',action='store_true')
    parser.add_argument('--player-target',action='store_true',help='Attack a second actual original client on the isolated 26.2 endpoint')
    parser.add_argument('--netty-runtime',nargs='?',const='4.1.68.Final',choices=['4.1.68.Final','4.1.9.Final'],help='Explicit isolated 1.8.8 reference runtime; server JAR unchanged')
    args=parser.parse_args()
    import boat_probe
    report=boat_probe.run(args.version,args.label,args.client,scenario='interaction',baseline=args.baseline,interaction_backend=args.backend,interaction_netty=args.netty_runtime,interaction_player=args.player_target,interaction_rightclick=args.rightclick,baseline_stage='rightclick' if args.rightclick else None)
    result=report['interaction_summary']
    print('INTERACTION',result['result'],str(result['passed_cases'])+'/'+str(result['measured_cases']),flush=True)
    raise SystemExit(0 if result['result']=='PASS' else 1)
