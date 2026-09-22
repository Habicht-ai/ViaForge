"""Actual Creative GUI clicks, hotbar keys and right clicks on an isolated Grim server."""
import argparse
import os

def exercise(row,folder,report,name,action,console):
    action('Creative login','idle',3)
    console(['gamemode creative '+name,'clear '+name,'tp '+name+' 0.5 64 -2.5 0 28.369'])
    action('Load placement fixture chunks','idle',2)
    # Pre-1.13 has no /forceload. Initial /fill may fail before a player loads
    # the area, leaving the flat world's floor one block below the target.
    # Construct after arrival, then restore the eye position before picking.
    if row['protocol']<393:
        console(['fill -8 63 -8 8 63 8 stone','fill -8 64 -8 8 69 8 air',
                 'tp '+name+' 0.5 64 -2.5 0 28.369'])
    action('Ready','idle',2)
    checks=[(-8,63,-8,'stone'),(8,63,8,'stone'),(0,63,-3,'stone'),(0,63,0,'stone'),(0,64,0,'air')]
    fixture=console([f'testforblock {x} {y} {z} {block}' if row['protocol']<393 else
                     f'execute if block {x} {y} {z} {block} run say HOTBAR_FIXTURE_OK'
                     for x,y,z,block in checks])
    marker='Successfully found the block' if row['protocol']<393 else '[Server] HOTBAR_FIXTURE_OK'
    if fixture.count(marker)!=len(checks):raise RuntimeError('Placement fixture not loaded/confirmed: '+fixture)
    report['placement_fixture_confirmed']=True
    if os.environ.get('VIAFORGE_HOTBAR_PICK')=='1':
        report['pick_block']=True
        return exercise_pick(row,report,name,action,console)
    action('Creative stone to first slot','hotbar:creative:0:minecraft:stone',2)
    report['inventory_after_creative']=console(['data get entity '+name+' Inventory','data get entity '+name+' SelectedItemSlot'])
    for slot,expected,label in [(0,'stone','First slot'),(1,'air','Empty second slot'),(0,'stone','Return first slot')]:
        console(['setblock 0 64 0 air'])
        action('Reset '+label,'idle',1)
        action('Select '+label,'hotbar:select:'+str(slot),1)
        action(label,'hotbar:click:'+str(slot),2)
        result=console(['data get entity '+name+' SelectedItemSlot','data get entity '+name+' Inventory',
            f'execute if entity @a[name={name},nbt={{SelectedItemSlot:{slot}}}] run say HOTBAR_SLOT_OK',
            'execute if block 0 64 0 '+expected+' run say HOTBAR_EXPECTED'])
        report['cases'][-1]['server_expected']=('[Server] HOTBAR_EXPECTED' in result)
        report['cases'][-1]['expected']=expected
        report['cases'][-1].update(expected_slot=slot,server_slot_expected='[Server] HOTBAR_SLOT_OK' in result)
    console(['setblock 0 64 0 air'])
    action('Settled','idle',1)
    for mode in os.environ.get('VIAFORGE_HOTBAR_MODES','creative-mouse,creative-drag,creative-key,creative-paced').split(','):
        for filled,block in ((0,'dirt'),(1,'quartz_block'),(8,'dirt'),(0,'quartz_block')):
            console(['clear '+name]);action('Clear '+mode+' '+str(filled),'idle',1)
            action(mode+' '+block+' to slot '+str(filled),f'hotbar:{mode}:{filled}:minecraft:{block}',2.5)
            for slot in (filled,(filled+1)%9):
                expected=block if slot==filled else 'air'
                console(['setblock 0 64 0 air']);action('Reset '+mode+' '+str(slot),'idle',.5)
                action(mode+' quick select '+str(slot),'hotbar:select-click:'+str(slot),2)
                result=console(['data get entity '+name+' SelectedItemSlot','data get entity '+name+' Inventory',
                    f'execute if entity @a[name={name},nbt={{SelectedItemSlot:{slot}}}] run say HOTBAR_SLOT_OK',
                    'execute if block 0 64 0 '+expected+' run say HOTBAR_EXPECTED'])
                report['cases'][-1].update(server_expected='[Server] HOTBAR_EXPECTED' in result,expected=expected,
                    expected_slot=slot,server_slot_expected='[Server] HOTBAR_SLOT_OK' in result)
    console(['clear '+name,'setblock 0 64 0 air']);action('Before reconnect inventory','idle',1)
    action('Quarz second slot before reconnect','hotbar:creative-paced:1:minecraft:quartz_block',2.5)
    action('Empty first selected before reconnect','hotbar:select:0',1)
    action('Reconnect with existing inventory','reconnect',8)
    console(['tp '+name+' 0.5 64 -2.5 0 28.369']);action('After reconnect ready','idle',2)
    action('Dirt first slot beside saved quartz','hotbar:creative-paced:0:minecraft:dirt',2.5)
    for slot,expected in ((0,'dirt'),(1,'quartz_block'),(2,'air')):
        console(['setblock 0 64 0 air']);action('Reset reconnect '+str(slot),'idle',.5)
        action('Reconnect slot '+str(slot),'hotbar:select-click:'+str(slot),2)
        result=console([f'execute if entity @a[name={name},nbt={{SelectedItemSlot:{slot}}}] run say HOTBAR_SLOT_OK',
            'execute if block 0 64 0 '+expected+' run say HOTBAR_EXPECTED'])
        report['cases'][-1].update(server_expected='[Server] HOTBAR_EXPECTED' in result,expected=expected,
            expected_slot=slot,server_slot_expected='[Server] HOTBAR_SLOT_OK' in result)

def exercise_pick(row,report,name,action,console):
    legacy=row['protocol']<393
    for block in ('dirt','quartz_block'):
        console(['clear '+name,'setblock 0 64 0 air','setblock 0 63 0 '+block])
        action('Select first for '+block,'hotbar:select:0',1)
        action('Pick '+block,'hotbar:pick:'+block,2)
        report.setdefault('picked_inventories',[]).append(console([f'testfor {name} {{Inventory:[{{Slot:0b,id:"minecraft:{block}"}}]}}' if legacy else 'data get entity '+name+' Inventory']))
        action('Place picked '+block,'hotbar:click:'+block,2)
        verify_pick(report,name,console,0,block)
        console(['setblock 0 64 0 air']);action('Reset before scroll '+block,'idle',1)
        action('Scroll right '+block,'hotbar:scroll:-1',1)
        action('Empty hand after pick '+block,'hotbar:click:empty',2)
        verify_pick(report,name,console,1,'air')


def verify_pick(report,name,console,slot,expected):
    legacy=report['row']['protocol']<393
    result=console([f'testfor {name} {{SelectedItemSlot:{slot}}}','testforblock 0 64 0 '+expected] if legacy else
        ['data get entity '+name+' Inventory',
         f'execute if entity @a[name={name},nbt={{SelectedItemSlot:{slot}}}] run say HOTBAR_SLOT_OK',
         'execute if block 0 64 0 '+expected+' run say HOTBAR_EXPECTED'])
    report['cases'][-1].update(server_expected=('Successfully found the block' if legacy else '[Server] HOTBAR_EXPECTED') in result,expected=expected,
        expected_slot=slot,server_slot_expected=('Found '+name if legacy else '[Server] HOTBAR_SLOT_OK') in result)
    if os.environ.get('VIAFORGE_HOTBAR_SOUND')=='1':
        report['cases'][-1]['expected_sound']=None if expected=='air' else 'gravel' if expected=='dirt' else 'stone'
        report['cases'][-1]['require_sound_source']=os.environ.get('VIAFORGE_HOTBAR_SOURCE')=='1'


def summarize(case,poses,events,name,protocol):
    ticks=[p for p in poses if p.get('phase')=='END' and case['start']*1000<=p['time_ms']<=case['end']*1000]
    out=dict(ticks=len(ticks),slots=sorted({p['selected_slot'] for p in ticks if 'selected_slot' in p}),
             flags=[e for e in events if e['type']=='flag' and case['start']<=e['time']<=case['end']])
    if 'expected' not in case:return out
    problems=[]
    if 'expected_sound' in case:
        sounds=[p for p in poses if p.get('phase')=='sound' and case['start']*1000-150<=p['time_ms']<=case['end']*1000
                and abs(p.get('x',100)-.5)<.01 and abs(p.get('y',100)-64.5)<.01 and abs(p.get('z',100)-.5)<.01]
        out['placement_sounds']=sounds
        wanted=case['expected_sound']
        if wanted is None and sounds:problems.append('Empty hand played a placement sound')
        if wanted is not None and (len(sounds)!=1 or sounds[0].get('sound')!='viaforge:mob_sound.4.block.'+wanted+'.place'
                or not sounds[0].get('registered') or abs(sounds[0].get('volume',0)-1)>.0001 or abs(sounds[0].get('pitch',0)-.8)>.0001):
            problems.append('Missing, duplicate or incorrect placement sound')
        if wanted is not None and case.get('require_sound_source'):
            sources=[p for p in poses if p.get('phase')=='sound_source' and p.get('sound')=='viaforge:mob_sound.4.block.'+wanted+'.place'
                     and case['start']*1000-150<=p['time_ms']<=case['end']*1000]
            out['sound_sources']=sources
            if len(sources)!=1:problems.append('Expected one actual audio source')
    if out['flags']:problems.append('Grim flagged during placement')
    if any(e['type']=='setback' and case['start']<=e['time']<=case['end'] for e in events):problems.append('Grim setback during placement')
    if not case['server_expected']:problems.append('Authoritative server block differs')
    if 'expected_slot' in case:
        if not case.get('server_slot_expected'):problems.append('Authoritative selected slot differs')
        last=ticks[-1] if ticks else {}
        if last.get('selected_slot')!=case['expected_slot']:problems.append('Client selected slot differs')
        hotbar=last.get('hotbar',[])
        item=hotbar[case['expected_slot']] if len(hotbar)==9 else {}
        wanted=None if case['expected']=='air' else 'minecraft:'+case['expected']
        if (item or {}).get('item')!=wanted:problems.append('Client held item differs')
    if len(ticks)<20:problems.append('Insufficient client observations')
    for side in ('before','after'):
        state=case[side];p=next((p for p in state.get('players',[]) if p['name']==name),{})
        if (state.get('state')!='enabled' or p.get('protocol')!=protocol or p.get('op',True)
                or p.get('disabled',True) or any(p.get(key,True) for key in ('exempt_permission','nosetback_permission','nomodifypacket_permission'))):
            problems.append('Active non-OP Grim/protocol not confirmed')
    out.update(result='FAIL' if problems else 'PASS',reasons=problems)
    return out

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--version',default='26.2');parser.add_argument('--label',default='baseline')
    a=parser.parse_args()
    import boat_probe
    result=boat_probe.run(a.version,a.label,scenario='hotbar')
    measured=[case for case in result['cases'] if 'expected' in case]
    if not result['completed'] or not measured or any(case.get('result')!='PASS' for case in measured):
        raise SystemExit(1)
