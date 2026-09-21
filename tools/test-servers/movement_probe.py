"""Record actual ViaForge input sequences under unmodified Grim; setbacks are findings, never hidden PASSes."""
import argparse
import json
import os
import subprocess
import time
import lab
import grim
import snapshots
import arena


def probe(row):
    status=lab.status(row)
    if status and status['players']['online']:raise RuntimeError('Vacant isolated Grim variant required')
    if status:lab.stop([row])
    safety=snapshots.backup(row)
    folder=lab.REPO/'build/logs';path=folder/('movement-'+row['version']+'-'+str(time.time_ns())+'.json')
    cases=[];process=None;result={}
    try:
        lab.start([row]);grim.control(row,'on')
        legacy=row['protocol']<393
        if row['protocol']>=401:
            lab.batch(row,['forceload add -32 -96 0 -80']);time.sleep(2)
        # Original copied world is preserved in safety. Only a small reserved station strip is used.
        commands=['fill -19 64 -92 -2 67 -85 air','fill -19 63 -92 -2 63 -85 quartz_block',
                  'fill -6 64 -92 -6 66 -89 glass',
                  'fill -11 65 -87 -5 65 -85 '+('stone_slab 8' if legacy else 'smooth_stone_slab[type=top]')]
        setup_output=lab.batch(row,commands)
        if arena.error_lines(setup_output) or 'not loaded' in setup_output:
            raise RuntimeError('Movement fixture was not installed: '+setup_output)
        fixture=lab.batch(row,['testforblock -6 65 -90 glass' if legacy else 'execute if block -6 65 -90 glass run say LAB_WALL_PRESENT'])
        if not ('Successfully found' in fixture or 'LAB_WALL_PRESENT' in fixture):raise RuntimeError('Missing actual collision fixture: '+fixture)
        env=dict(os.environ,VIAFORGE_NO_PAUSE='1',VIAFORGE_LIVE_FLIGHT=str(path),VIAFORGE_LIVE_PROTOCOL=str(row['protocol']),
                 VIAFORGE_LIVE_PORT=str(row['port']),VIAFORGE_LIVE_SEQUENCE='1')
        env.pop('VIAFORGE_BLOCK_SMOKE_TEST',None);env.pop('VIAFORGE_SMOKE_PROTOCOL',None)
        with path.with_suffix('.log').open('w') as log:
            process=subprocess.Popen(['cmd.exe','/d','/c',r'.\build.bat','runClient','-x','preRunClient'],cwd=lab.REPO,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=lab.NO_WINDOW)
        until=time.monotonic()+180
        while time.monotonic()<until:
            if path.exists():
                result=json.loads(path.read_text())
                if result['state'] in ('ready','FAIL'):break
            if process.poll() is not None:raise RuntimeError('Probe client exited during startup')
            time.sleep(.2)
        if result.get('state')!='ready':raise RuntimeError('Probe not ready: '+str(result))
        name=result['name']
        def command(text):
            with lab.Rcon(row) as remote:return remote.command(text.replace('@s',name))
        def action(label,value,seconds):
            start=time.time();path.with_name(path.name+'.action').write_text(value)
            before=grim.state(row,True);time.sleep(seconds)
            cases.append(dict(case=label,action=value,start=start,end=time.time(),before=before,after=grim.state(row,True)))
        action('Creative login','idle',2)
        command('gamemode '+('0' if legacy else 'survival')+' @s')
        command('tp @s -18 64 -90 -90 0');action('Survival settle','idle',2)
        action('Walk toward wall','walk',2);action('Sprint collision','sprint',2);action('Jump at wall','jump',1)
        action('Break glass wall','break',1)
        broken=lab.batch(row,['testforblock -6 65 -90 air' if legacy else 'execute if block -6 65 -90 air run say LAB_WALL_BROKEN'])
        cases[-1]['block_result']=broken
        command('tp @s -12 64 -86 -90 0');action('Corridor settle','idle',.7);action('Sneak into 1.5-high passage','sneak',3)
        equip=('replaceitem entity @s slot.armor.chest elytra' if legacy else 'item replace entity @s armor.chest with elytra')
        command(equip);command('give @s '+('fireworks' if legacy else 'firework_rocket')+' 32')
        command('tp @s -40 220 -53 -90 0');action('Falling takeoff setup','idle',.5);action('Falling glide','glide',2)
        action('Rocket use','use',2);action('Inventory during flight','inventory',1)
        command('tp @s 64 159 -52');action('Water landing','idle',2)
        command('tp @s -18 64 -83 -90 0');action('Landing settle','idle',.5);action('Sprint/jump/glide cycles','cycles',4)
        command('replaceitem entity @s slot.armor.chest air' if legacy else 'item replace entity @s armor.chest with air')
        action('Unequip elytra','idle',1)
        command('replaceitem entity @s slot.weapon.offhand shield' if legacy else 'item replace entity @s weapon.offhand with shield')
        command('replaceitem entity @s slot.weapon.mainhand air' if legacy else 'item replace entity @s weapon.mainhand with air')
        action('Offhand shield use','use',1);action('Inventory after landing','inventory',1)
        action('Finish','stop',.3);process.wait(timeout=60)
        events=[json.loads(line) for line in (lab.ROOT/row['version']/'plugins/ViaForgeLabAC/events.jsonl').read_text(encoding='utf-8').splitlines()]
        events=[e for e in events if e.get('player',e.get('name'))==name]
        poses=path.with_name(path.name+'.poses').read_text().splitlines()
        for case in cases:
            case['events']=[e for e in events if case['start']<=e['time']<=case['end']]
            case['poses']=[p for p in poses if case['start']*1000<=float(p.split(';')[0])<=case['end']*1000]
            case['findings']={kind:sum(e['type']==kind for e in case['events']) for kind in ['flag','setback']}
        report=dict(server=row,client='ViaForge development 1.8.9',name=name,cases=cases,events=events,fixture=fixture,setup_output=setup_output,
                    result=json.loads(path.read_text()),backup=str(safety),time=time.time(),
                    compatibility_pass=not any(e['type'] in ('flag','setback') for e in events),
                    limits='Input/position record; success requires case-specific motion evidence plus check/setback review')
        lab.save(path.with_name(path.stem+'-grim.json'),report)
        print('MOVEMENT RECORDED',path.with_name(path.stem+'-grim.json'),flush=True)
    finally:
        if process and process.poll() is None:
            path.with_name(path.name+'.action').write_text('stop');process.wait(timeout=60)
        if lab.status(row):lab.stop([row])
        snapshots.restore(row,safety.name)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('versions',nargs='?',default='1.12.2-grim,26.2-grim')
    for row in lab.select(parser.parse_args().versions):probe(row)
