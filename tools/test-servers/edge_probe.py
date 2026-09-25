"""Actual client inputs for low glides, swimming bubble columns and repeated lid use."""
import argparse
import os
import surface_probe


def fixture(console):
    output=console(['forceload add -32 -16 32 144',
        'fill 12 63 -10 22 63 140 stone','fill 12 64 -10 22 74 140 air',
        'fill -18 57 -5 -10 76 41 stone','fill -17 58 -4 -11 75 40 water',
        'fill -9 57 -5 -1 76 41 stone','fill -8 58 -4 -2 75 40 water',
        'fill -18 76 -5 -1 76 41 air',
        'fill -17 57 6 -11 57 28 soul_sand','fill -8 57 6 -2 57 28 magma_block',
        'fill -17 58 6 -11 75 28 bubble_column[drag=false]',
        'fill -8 58 6 -2 75 28 bubble_column[drag=true]'])
    if any(s in output for s in ['Incorrect','Cannot place','Unknown']):raise RuntimeError(output)


def exercise(row,folder,report,name,action,console):
    def equip(slot,item):return f'item replace entity {name} {slot} with {item}'
    action('Creative login','idle',3)
    console([f'tp {name} 0.5 64 0.5 0 0'])
    action('Creative load','idle',25)
    console([f'gamemode survival {name}',equip('armor.chest','elytra')])
    action('Survival settle','idle',3)
    if os.environ.get('VIAFORGE_METADATA_RELAY_MS','0')!='0':(folder/'relay-arm').write_text('Survival inputs only\n')
    # Before1.18.2 the coarser movement-packet threshold leaves fewer sampled
    # points in a stationary descent. Start higher instead of accepting fewer.
    glide_y=72 if row['protocol']<758 else 69
    flights=[
        ('ground sprint jump',64,0,'sequence:'+'|'.join(['1:forward-sprint-jump','1:forward-sprint','1:forward-sprint-jump','17:forward-sprint']*5),5),
        ('ground jump',64,0,'sequence:'+'|'.join(['1:jump','1:idle','1:jump','17:idle']*5),5),
        ('low glide',glide_y,0,'sequence:1:jump|119:idle',6),
        ('downward landing',72,35,'sequence:1:jump|119:forward-sprint',6)]
    rounds=int(os.environ.get('VIAFORGE_EDGE_FLIGHT_ROUNDS','1'))
    report['edge_flight_rounds']=rounds
    moving_only=os.environ.get('VIAFORGE_EDGE_MOVING_ONLY')=='1'
    report['edge_moving_only']=moving_only
    if moving_only:flights=[f for f in flights if f[0] in ('ground sprint jump','downward landing')]
    for repetition,(label,y,pitch,keys,seconds) in [(r,f) for r in range(rounds) for f in flights]:
        action('Reset keys','idle',.4)
        # Repeated legitimate landings consume health in Survival. Heal between
        # cases (both clients), without changing movement attributes or damage.
        console([f'effect give {name} instant_health 1 4 true'])
        console([f'tp {name} 16.5 {y} -5.5 0 {pitch}'])
        action('Reset flight position','idle',.4 if y>64 else 2)
        action('Edge flight '+label+(f' repeat {repetition+1}' if rounds>1 else ''),keys,seconds)
        action('Reset flight coast','idle',4)
    console([equip('armor.chest','air')])
    for label,open_ticks,wait_ticks in [('full',24,24),('fast',4,4),('very fast',2,2)]:
        action('Reset close','close',.3)
        console(['setblock 1 64 0 air','setblock 1 64 0 purple_shulker_box[facing=up]',equip('weapon.mainhand','air'),f'tp {name} 1.5 65 .5 0 90'])
        action('Reset shulker','idle',3)
        sequence='sequence:'+'|'.join([f'1:use',f'{open_ticks-1}:idle','1:close',f'{wait_ticks-1}:idle']*8)
        action('Edge shulker '+label,sequence,(open_ticks+wait_ticks)*8/20+.5)
        action('Reset close','close',2)
    for direction,x,y in [('lift',-13.5,64),('sink',-4.5,70)]:
        for keys in ['forward-sprint','forward-sprint-sneak','forward-sprint-jump']:
            action('Reset keys','idle',.3)
            console([f'tp {name} {x} {y} 1.5 0 0'])
            action('Reset swim position','idle',.4)
            action('Reset establish swim','forward-sprint',.8)
            action('Edge bubble '+direction+' '+keys,keys,3)
            action('Reset bubble coast','idle',1)
    console([f'tp {name} 16.5 64 -5.5 0 0'])
    action('Reset final ground','idle',3)
    action('Edge final coast','idle',3)


def summarize(case,poses,events,name,protocol):
    # Reuse strict Grim/Survival/loaded-client evidence without the high-glide
    # requirement to remain flying after landing.
    result=surface_probe.summarize(case,poses,events,name,protocol)
    ticks=[p for p in poses if p.get('phase')=='END' and case['start']*1000<=p['time_ms']<=case['end']*1000]
    errors=result['reasons']
    if result['predictions']<15 and 'final coast' not in case['case']:errors.append('Insufficient movement predictions')
    if 'flight ' in case['case']:
        if sum(t.get('elytra',False) for t in ticks)<3:errors.append('No actual low flight')
        first=next((i for i,t in enumerate(ticks) if t.get('elytra',False)),len(ticks))
        if not any(t.get('player_ground',t.get('ground',False)) for t in ticks[first+1:]):errors.append('No actual landing')
    if 'shulker ' in case['case']:
        progress=[t.get('shulker_progress',0) for t in ticks]
        if sum(b>a for a,b in zip(progress,progress[1:]))<8 or sum(b<a for a,b in zip(progress,progress[1:]))<8:errors.append('Repeated original opening/closing not observed')
    if 'bubble ' in case['case']:
        if sum(t.get('swimming',False) for t in ticks)<10:errors.append('Actual swimming not observed')
        if sum(6<t['player_z']<29 and 58<t['player_y']<76 for t in ticks)<10:errors.append('Bubble fixture not traversed')
        # Require the observed column, not coordinates alone.
        expected=17 if 'lift' in case['case'] else 18
        if sum(t.get('bubble')==expected for t in ticks)<10:errors.append('Actual column direction not observed')
    result['result']='FAIL' if errors else 'PASS'
    return result


if __name__=='__main__':
    import boat_probe
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--client',choices=['viaforge','native'],default='viaforge');p.add_argument('--label',default='edges-before')
    p.add_argument('--baseline',action='store_true')
    p.add_argument('--version',default='26.2')
    p.add_argument('--flight-rounds',type=int,default=1,help='Repeat all four landing sequences, including full resets')
    p.add_argument('--moving-only',action='store_true',help='Stress the two moving flight sequences that reproduced the intermittent flags')
    p.add_argument('--metadata-delay-ms',type=int,default=0,help='DIAGNOSTIC ONLY: loopback relay delays one original flight-end frame, without changing bytes/order')
    p.add_argument('--direct',action='store_true',help='Use the selected version server instead of the reported 1.21.11 ViaVersion endpoint')
    args=p.parse_args()
    if not 1<=args.flight_rounds<=20:p.error('--flight-rounds must be between 1 and 20')
    if not 0<=args.metadata_delay_ms<=200:p.error('--metadata-delay-ms must be between 0 and 200')
    os.environ['VIAFORGE_METADATA_RELAY_MS']=str(args.metadata_delay_ms)
    os.environ['VIAFORGE_EDGE_FLIGHT_ROUNDS']=str(args.flight_rounds)
    os.environ['VIAFORGE_EDGE_MOVING_ONLY']='1' if args.moving_only else '0'
    os.environ['VIAFORGE_PROBE_TIMEOUT_MS']=str(max(600000,120000+args.flight_rounds*60000))
    r=boat_probe.run(args.version,args.label,args.client,scenario='interaction',interaction_surfaces='edges',interaction_backend=None if args.direct else '1.21.11',baseline=args.baseline,baseline_stage='edges')
    s=r['interaction_summary'];print('EDGES',s['result'],s['passed_cases'],s['measured_cases'],flush=True)
    raise SystemExit(0 if s['result']=='PASS' else 1)
