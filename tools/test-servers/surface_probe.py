"""Real-client reproductions for vertical elytra flight, slime and shulker lids."""
import argparse
import math


def fixture(console, protocol):
    commands=([] if protocol<393 else ['forceload add -32 -32 32 64'])+[
        'fill -24 63 -16 24 63 64 stone','fill -24 64 -16 24 69 64 air',
        'fill -24 70 -16 24 75 64 air','fill -10 63 4 10 63 40 slime',
        'fill -4 64 -14 4 67 -9 stone',
        'fill -3 67 -13 3 67 -10 air',
        'fill -3 64 -13 3 66 -10 water',
        'setworldspawn 0 64 0']
    # Old command spelling is slime too; modern block ID is slime_block.
    if protocol>=393:commands=[c.replace(' slime',' slime_block') for c in commands]
    output=console(commands)
    if any(error in output for error in ['Unknown','Incorrect','Cannot place','not a valid']):raise RuntimeError(output)


def exercise(row,folder,report,name,action,console):
    modern=row['protocol']>=755
    def equip(slot,item,count=1):
        return (f'item replace entity {name} {slot} with {item} {count}' if modern else
                f'replaceitem entity {name} '+('slot.' if row['protocol']<393 else '')+f'{slot} {item} {count}')
    action('Creative login','idle',3)
    console([f'tp {name} 0.5 64 0.5 0 0'])
    action('Creative load','idle',25)
    if row['protocol']<393:
        # 1.12 lacks forceload; the player must first load the fixture columns.
        fixture(console,row['protocol'])
        action('Creative fixture load','idle',3)
    console([f'gamemode survival {name}'])
    action('Survival settle','idle',3)
    for label,keys,x,y,z,seconds in [
            ('Stone walk','forward',16.5,64,4.5,3),
            ('Slime walk','forward',0.5,64,6.5,4),
            ('Slime sprint','forward-sprint',0.5,64,6.5,4),
            ('Slime sneak','forward-sneak',0.5,64,6.5,4),
            ('Slime jumping','forward-jump',0.5,64,6.5,5),
            ('Slime bounce','idle',0.5,71,6.5,5),
            ('Slime sneak landing','sneak',0.5,71,6.5,4)]:
        action('Reset keys','idle',.3)
        console([f'tp {name} {x} {y} {z} 0 0'])
        action('Reset '+label,'idle' if y==64 else keys,.3 if y>64 else 2)
        action('Surface '+label,keys,seconds)
        action('Reset coast','idle',3)
    for facing,x,y,z,yaw,pitch in [('up',1.5,65,.5,0,90),('east',2.3,64,.5,90,65),('north',1.5,64,-.3,0,65)]:
        action('Reset close','close',.5)
        block=f'purple_shulker_box[facing={facing}]' if row['protocol']>=393 else 'purple_shulker_box '+str({'up':1,'east':5,'north':2}[facing])
        console(['setblock 1 64 0 air',f'setblock 1 64 0 {block}',equip('weapon.mainhand','air'),f'tp {name} {x} {y} {z} {yaw} {pitch}'])
        action('Reset shulker '+facing,'idle',3)
        action('Surface Shulker '+facing,'sequence:1:use|99:idle',4)
        action('Reset close','close',2)
    console(['setblock 1 64 0 air',equip('armor.chest','elytra'),equip('weapon.mainhand','firework_rocket' if row['protocol']>=393 else 'fireworks',32)])
    for label,boost in [('unpowered',False),('rocket',True),('high rocket',True)]:
        action('Reset keys','idle',.3)
        console([f'tp {name} 0.5 64 -6.5 0 0'])
        action('Reset flight grounded','idle',3)
        # Explicitly cover flight both below and above the legacy build height.
        console([f'tp {name} 0.5 {280 if label=="high rocket" else 180 if boost else 240} -6.5 0 0'])
        action('Reset falling','idle',.7)
        action('Reset launch','sequence:1:jump|19:idle',1)
        action(('Reset' if label=='high rocket' else 'Flight')+' horizontal '+label,'idle',1)
        action('Flight vertical '+label,'sequence:1:lookup-use|19:lookup|1:lookup-use|19:lookup' if boost else 'lookup',2)
        if label!='high rocket':action('Flight near vertical '+label,'lookup-near',1.5)
        if boost:action('Reset boost expiration','idle',3)
        # A water landing clears accumulated fall distance in the real client.
        # Teleporting a descending glider straight onto stone can kill even vanilla.
        console([f'tp {name} 0.5 65 -11.5 0 0'])
        action('Reset flight water landing','idle',3)
    console([f'tp {name} 0.5 64 -6.5 0 0',equip('armor.chest','air')])
    action('Reset grounded finish','idle',3)
    action('Surface final coast','idle',3)


def summarize(case,poses,events,name,protocol):
    ticks=[p for p in poses if p.get('phase')=='END' and case['start']*1000<=p['time_ms']<=case['end']*1000]
    selected=[e for e in events if case['start']<=e['time']<=case['end']]
    predictions=[e for e in selected if e['type']=='prediction']
    flags=[e for e in selected if e['type']=='flag'];setbacks=[e for e in selected if e['type']=='setback']
    errors=[]
    for side in ['before','after']:
        state=case[side];p=next((p for p in state.get('players',[]) if p['name']==name),{})
        if state.get('state')!='enabled' or p.get('protocol')!=protocol or p.get('gamemode')!='SURVIVAL' or p.get('op',True) or p.get('disabled',True) or not p.get('verbose') or any(p.get(k,True) for k in ['exempt_permission','nosetback_permission','nomodifypacket_permission']):errors.append(side+': actual Grim/Survival conditions missing')
    # 1.8 isBlockLoaded rejects y>=256 even with the column fully present.
    # Flight can legitimately be above build height: verify the actual column.
    loaded_key='loaded_column' if case['case'].startswith('Flight ') else 'loaded'
    if len(ticks)<18 or any(not p.get(loaded_key,p.get('loaded',False)) or p.get('paused',True) or not p.get('player_alive',False) for p in ticks):errors.append('Insufficient loaded real-client ticks')
    if sum(p.get('action')==case['action'] for p in ticks)<18:errors.append('Actual requested input not observed')
    delta=[ticks[-1]['player_'+k]-ticks[0]['player_'+k] for k in ['x','y','z']] if ticks else [0,0,0]
    if case['case'].startswith('Flight '):
        if sum(p.get('elytra',False) for p in ticks)<min(25,len(ticks)-2):errors.append('Actual elytra flight not sustained')
        if len(predictions)<15:errors.append('Insufficient flight predictions')
        target=-89.9 if 'near vertical' in case['case'] else -90 if 'vertical' in case['case'] else 0
        if sum(abs(p.get('pitch',999)-target)<.001 for p in ticks)<min(25,len(ticks)-2):errors.append('Required camera pitch not observed')
        if case['case'] in ('Flight vertical rocket','Flight vertical high rocket'):
            if max((p.get('player_vy',0) for p in ticks),default=0)<.5 or sum(e.get('packet')=='USE_ITEM' for e in selected)<2:errors.append('Actual rocket acceleration/use not observed')
    if 'Slime' in case['case']:
        if sum('slime' in p.get('substrate' if 'jumping' in case['case'] else 'floor','').lower() for p in ticks)<10 and 'landing' not in case['case'] and 'bounce' not in case['case']:errors.append('Actual slime contact not observed')
        if len(predictions)<(8 if 'landing' in case['case'] else 15):errors.append('Insufficient slime predictions')
    if 'forward' in case['action']:
        if math.hypot(delta[0],delta[2])<.5:errors.append('Actual input displacement missing')
        if len(predictions)<15:errors.append('Insufficient movement predictions')
    if 'Shulker' in case['case']:
        if len(predictions)<8:errors.append('Insufficient lid movement predictions')
        if max((p.get('shulker_progress',0) for p in ticks),default=0)<.99:errors.append('Actual shulker opening not observed')
        axis=1 if 'up' in case['case'] else 0 if 'east' in case['case'] else 2
        values=[p['player_'+['x','y','z'][axis]] for p in ticks]
        if not values or max(values)-min(values)<.4:errors.append('Original lid displacement missing')
        if protocol>=755 and axis!=1:
            increments=[abs(b-a) for a,b in zip(values,values[1:]) if abs(b-a)>.001]
            if len(increments)<8 or max(increments,default=0)>.12:errors.append('Progressive original lid push missing')
    if flags or setbacks:errors.append('Grim flags or setbacks')
    return dict(result='FAIL' if errors else 'PASS',reasons=errors,ticks=len(ticks),predictions=len(predictions),flags=flags,setbacks=setbacks,
                max_offset=max((e['offset'] for e in predictions),default=None),delta=delta,elytra_ticks=sum(p.get('elytra',False) for p in ticks),
                packets=[e for e in selected if e['type']=='packet'])


if __name__=='__main__':
    import boat_probe,interaction_probe
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--version',default='26.2');p.add_argument('--label',default='surfaces-before');p.add_argument('--client',choices=['viaforge','native'],default='viaforge');p.add_argument('--baseline',action='store_true');p.add_argument('--backend',choices=['1.21.11'])
    args=p.parse_args();r=boat_probe.run(args.version,args.label,args.client,scenario='interaction',interaction_surfaces=True,baseline=args.baseline,baseline_stage='surfaces',interaction_backend=args.backend)
    s=r['interaction_summary'];print('SURFACES',s['result'],str(s['passed_cases'])+'/'+str(s['measured_cases']),flush=True)
    raise SystemExit(0 if s['result']=='PASS' else 1)
