"""Backed-up destructive fixture ONLY inside the independently copied, vacant Grim test world."""
import json
import time
import lab
import snapshots
import terrain
import exhibition
import world_audit


def probe():
    row=lab.select('1.12.2-grim')[0]
    state=lab.status(row)
    if state and state['players']['online']:raise RuntimeError('Vacant isolated variant required')
    if state:lab.stop([row])
    before=snapshots.backup(row)
    folder=lab.ROOT/row['version']
    index=json.loads((folder/'arena-index.json').read_text())
    specimen=next(s for s in index['blocks'] if s['name']=='minecraft:dirt')
    p=' '.join(map(str,specimen['position']))
    report=dict(time=time.time(),server=row['version'],backup=str(before),success=False)
    try:
        lab.start([row])
        # Two intrusions above the former 222 ceiling, one damaged managed exhibit,
        # and an outside sentinel that the maintenance action must retain.
        lab.batch(row,['setblock -70 250 -100 log','setblock -69 251 -100 leaves 4',
                       'setblock 80 250 110 stone',f'setblock {p} air','save-all flush'])
        lab.stop([row])
        survey=terrain.inspect(row)
        assert [-70,250,-100] in [list(p) for p in survey['positions']],survey['intrusions']
        report['before_repair']=survey
        result=exhibition.maintain(row,restore=True)
        world=world_audit.World(folder)
        assert world.block(-70,250,-100).get('legacy_id')==0
        assert world.block(-69,251,-100).get('legacy_id')==0
        assert world.block(80,250,110).get('legacy_id')==1
        assert world.block(*specimen['position']).get('legacy_id')==3
        assert all(world.block(x,60,1).get('legacy_id')==0 for x in range(1,5))
        report.update(success=True,maintenance=result,outside_preserved=True,restored_specimen=specimen)
    finally:
        if lab.status(row):lab.stop([row])
        snapshots.restore(row,before.name)
        report['fixture_cleanup']='Original copied world restored from verified snapshot; fixture and repair remain backed up'
        lab.save(folder/('terrain-repair-probe-'+str(time.time_ns())+'.json'),report)
    print('TERRAIN REPAIR PASS',flush=True)


if __name__=='__main__':probe()
