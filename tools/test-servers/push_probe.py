"""Proximity collision cases using real client ticks in the isolated Grim harness."""
import argparse
import math
import os
import subprocess
import time
import uuid


def exercise(row,folder,report,name,action,console):
    legacy=row['protocol']<393
    action('Creative login','idle',3)
    console(['tp '+name+' 0.5 66 0.5 0 0'])
    action('Load contact fixture','idle',3)
    console(['fill -8 63 -8 8 63 8 stone','fill -8 64 -8 8 69 8 air',
             'gamemode '+('0' if legacy else 'survival')+' '+name])
    action('Survival confirmed settle','idle',2.5)
    team='scoreboard teams ' if legacy else 'team '
    console([team+'add push_a',team+'add push_b'])

    def team_rule(group,rule):
        return team+('option ' if legacy else 'modify ')+group+' collisionRule '+rule

    def reset():
        console(['kill @e[tag=push_probe]',team+'leave '+name,
                 team_rule('push_a','always'),team_rule('push_b','always'),
                 'tp '+name+' 8.5 64 0.5 0 0'])
        action('Reset and settle','idle',1.5)

    def pig(x,y,z):
        kind='Pig' if row['protocol']<315 else 'pig'
        identity=uuid.uuid4()
        signed=lambda n,bits:n-(1<<bits) if n&(1<<(bits-1)) else n
        if row['protocol']<735:
            data=f'UUIDMost:{signed(identity.int>>64,64)}L,UUIDLeast:{signed(identity.int&((1<<64)-1),64)}L'
        else:
            data='UUID:[I;'+','.join(str(signed((identity.int>>shift)&0xffffffff,32)) for shift in (96,64,32,0))+']'
        console(['summon '+kind+f' {x} {y} {z} '+
                 '{'+data+',NoAI:1b,Invulnerable:1b,Silent:1b,Tags:["push_probe"]'+(',NoGravity:1b' if y>64 else '')+'}'])
        return str(identity)

    def join_mob(group,identity):
        output=console([team+'join '+group+' '+identity])
        if not (identity in output if legacy else 'Added ' in output and '['+group+']' in output):
            raise RuntimeError('Mob team membership not confirmed: '+output)

    def observe(label,seconds):
        console(['tp '+name+' 0.5 64 0.5 0 0'])
        action(label,'idle',seconds)

    reset();observe('Empty ground',3)
    reset();pig(1.36,64,.5);observe('Mob outside contact',3)
    if row['protocol']>=210:
        reset();pig(.9,66.1,.5);observe('Mob above player',1)
    else:report.setdefault('skipped',[]).append('Mob above player: original NoGravity entity flag not available before 1.10')
    reset();pig(.9,64,.5);observe('Mob contact right',4)
    reset();pig(.1,64,.3);observe('Mob contact diagonal left',4)
    reset();console([team+'join push_a '+name,team_rule('push_a','never')]);pig(.9,64,.5)
    observe('Local team never',3)
    reset();identity=pig(.9,64,.5);join_mob('push_a',identity);console([team_rule('push_a','never')])
    observe('Mob team never',3)
    for rule in ('pushOwnTeam','pushOtherTeams'):
        for same in (True,False):
            reset();console([team+'join push_a '+name,team_rule('push_a',rule)])
            identity=pig(.9,64,.5);join_mob('push_a' if same else 'push_b',identity)
            observe(rule+(' same team' if same else ' other team'),3)
    reset();console(['gamemode '+('1' if legacy else 'creative')+' '+name]);action('Creative mode settle','idle',2.5);pig(.9,64,.5)
    observe('Creative grounded contact',3)
    reset();console(['gamemode '+('3' if legacy else 'spectator')+' '+name]);action('Spectator mode settle','idle',2.5);pig(.9,64,.5)
    observe('Spectator contact',3)
    console(['kill @e[tag=push_probe]'])
    if os.environ.get('VIAFORGE_PUSH_PEER')=='1':
        peer_cases(row,folder,report,name,action,console,reset,observe,team,team_rule)


def peer_cases(row,folder,report,name,action,console,reset,observe,team,team_rule):
    import lab
    from boat_probe import read_json,write_action
    if row['minecraft_version']=='1.12.2':
        from native_boat_probe import command
    elif row['minecraft_version']=='26.2':
        from native_push_probe import command
    else:raise ValueError('Exact original peer currently prepared only for 1.12.2 and 26.2')
    peer=folder/'peer';peer.mkdir()
    process=None
    try:
        with (peer/'console.log').open('w') as log:
            process=subprocess.Popen(command(peer,row['port']),cwd=peer,stdout=log,stderr=subprocess.STDOUT,creationflags=lab.NO_WINDOW)
        deadline=time.monotonic()+120
        while time.monotonic()<deadline:
            state=read_json(peer/'client-state.json')
            if state.get('state')=='ready':break
            if state.get('state')=='FAIL' or process.poll() is not None:raise RuntimeError('Peer startup: '+repr(state))
            time.sleep(.3)
        else:raise TimeoutError('Peer startup')
        other=state['name'];report['companion']=state
        console(['gamemode '+('0' if row['protocol']<393 else 'survival')+' '+name,
                 'gamemode '+('0' if row['protocol']<393 else 'survival')+' '+other])
        for label,rule in [('Player contact','always'),('Player team never','never')]:
            reset();console([team+'join push_b '+other,team_rule('push_b',rule),'tp '+other+' 0.9 64 0.5 0 0'])
            action('Peer settle','idle',2);observe(label,4)
        reset();console([team_rule('push_b','always'),'tp '+other+' 0.5 64 -2.5 0 0'])
        action('Peer settle','idle',2)
        console(['tp '+name+' 0.5 64 0.5 0 0']);write_action(peer,'forward')
        action('Player walks into receiver','idle',2);write_action(peer,'idle')
        action('Player contact coast','idle',3)
    finally:
        if process and process.poll() is None:
            write_action(peer,'stop');process.wait(timeout=45)


def summarize(case,poses,events,name,protocol):
    ticks=[p for p in poses if p.get('phase')=='END' and case['start']*1000<=p['time_ms']<=case['end']*1000]
    selected=[e for e in events if e.get('player',e.get('name'))==name and case['start']<=e['time']<=case['end']]
    flags=[e for e in selected if e['type']=='flag'];setbacks=[e for e in selected if e['type']=='setback']
    overlap=sum(any(n.get('overlap') for n in p.get('nearby',[])) for p in ticks)
    displacement=math.hypot(ticks[-1]['player_x']-.5,ticks[-1]['player_z']-.5) if ticks else None
    result=dict(ticks=len(ticks),overlap_ticks=overlap,displacement=displacement,
                start_position=[ticks[0][k] for k in ('player_x','player_y','player_z')] if ticks else None,
                end_position=[ticks[-1][k] for k in ('player_x','player_y','player_z')] if ticks else None,
                max_speed=max((math.hypot(p.get('player_vx',0),p.get('player_vz',0)) for p in ticks),default=0),
                predictions=sum(e['type']=='prediction' for e in selected),
                max_prediction_offset=max((e.get('offset',0) for e in selected if e['type']=='prediction'),default=None),
                nearby_types=sorted({n.get('type','unknown') for p in ticks for n in p.get('nearby',[])}),
                flags=flags,setbacks=setbacks)
    moving={'Mob contact right','Mob contact diagonal left','pushOwnTeam other team','pushOtherTeams same team','Creative grounded contact','Player contact','Player walks into receiver'}
    blocked={'Empty ground','Mob outside contact','Mob above player','Local team never','Mob team never','pushOwnTeam same team','pushOtherTeams other team','Spectator contact','Player team never'}
    if case['case'] in moving|blocked:
        reasons=[]
        for side in ('before','after'):
            state=case[side];player=next((p for p in state.get('players',[]) if p['name']==name),{})
            mode='CREATIVE' if case['case']=='Creative grounded contact' else 'SPECTATOR' if case['case']=='Spectator contact' else 'SURVIVAL'
            if state.get('state')!='enabled' or player.get('protocol')!=protocol or player.get('gamemode')!=mode or player.get('disabled',True) or player.get('op',True) or player.get('exempt_permission',True) or not player.get('verbose',False) or player.get('nosetback_permission',True) or player.get('nomodifypacket_permission',True):
                reasons.append(side+': active non-OP Grim / exact protocol / expected game mode not confirmed')
        if len(ticks)<10:reasons.append('Insufficient observed ticks')
        if not result['predictions'] and case['case']!='Spectator contact':reasons.append('No Grim predictions')
        if flags or setbacks:reasons.append('Grim flag or setback')
        if displacement is None or (displacement<=0 if case['case'] in moving else displacement>1e-8):reasons.append('Expected displacement absent or forbidden displacement present')
        if case['case'] not in {'Empty ground','Mob outside contact','Mob above player'} and overlap<1:reasons.append('No observed contact')
        if ticks and abs(ticks[-1]['player_y']-64)>1e-6:reasons.append('Receiver did not stay on fixture ground')
        if any(p.get('player_alive') is False for p in ticks):reasons.append('Dead receiver')
        result['result']='FAIL' if reasons else 'PASS';result['reasons']=reasons
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version',default='1.12.2');parser.add_argument('--label',default='contact')
    parser.add_argument('--client',choices=['viaforge','native'],default='viaforge')
    parser.add_argument('--baseline',action='store_true',help='Use the preserved pre-push production JAR for a diagnostic comparison')
    args=parser.parse_args()
    import boat_probe
    boat_probe.run(args.version,args.label,args.client,baseline=args.baseline,scenario='push')
