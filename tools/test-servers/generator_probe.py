"""Create isolated disposable verification worlds; never opens an existing test world."""
import json
import gzip
import shutil
import socket
import subprocess
import time
import uuid
import sys
import lab
import terrain
import world_audit


def probe(row, ordinal=0):
    original_root=lab.ROOT
    root=original_root / 'generator-probes' / (row['version'] + '-' + uuid.uuid4().hex[:8])
    folder=root / row['version']
    test=dict(row, port=25770+ordinal, rcon_port=26770+ordinal)
    exe=lab.java(row)
    for port in (test['port'],test['rcon_port']):
        with socket.socket() as check: check.bind(('127.0.0.1',port))
    lab.ROOT=root
    try:
        lab.properties(test)
        (folder/'eula.txt').write_text('eula=true\n')
        shutil.copy2(original_root/row['version']/'server.jar',folder/'server.jar')
        terrain.prepare_new_world(test)
        with (folder/'probe.log').open('w',encoding='utf-8') as log:
            process=subprocess.Popen([exe,'-Xms128m','-Xmx1024m','-XX:ActiveProcessorCount=2','-jar','server.jar','nogui'],
                cwd=folder,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,creationflags=lab.NO_WINDOW)
            try:
                deadline=time.monotonic()+180
                while time.monotonic()<deadline:
                    if process.poll() is not None: raise RuntimeError('Generator probe exited: '+str(folder))
                    if 'Done (' in (folder/'probe.log').read_text(encoding='utf-8',errors='replace'): break
                    time.sleep(.5)
                else: raise RuntimeError('Generator probe timed out: '+str(folder))
            finally:
                if process.poll() is None:
                    process.stdin.write(b'save-all flush\nstop\n');process.stdin.flush();process.wait(timeout=90)
        _,_,saved=terrain.generator(row)
        settings=saved.get('settings',{})
        world=world_audit.World(folder)
        minimum=-64 if row['protocol']>=757 else 0
        expected={minimum: ('bedrock',7), minimum+1: ('stone',1), 59: ('stone',1), 60: ('dirt',3), 63: ('grass_block',2), 64: ('air',0)}
        level=world_audit.nbt(gzip.decompress((folder/'world/level.dat').read_bytes()))['Data']
        spawn=level.get('spawn',{}).get('pos',[level.get('SpawnX',0),0,level.get('SpawnZ',0)])
        samples={str(y): world.block(spawn[0],y,spawn[2]) for y in expected}
        success=saved['type'] in ('flat','minecraft:flat') and all(
            samples[str(y)].get('Name')=='minecraft:'+name or samples[str(y)].get('legacy_id')==legacy
            for y,(name,legacy) in expected.items())
        result=dict(version=row['version'],success=success,generator=saved,blocks=samples,folder=str(folder),time=time.time())
        lab.save(folder/'result.json',result)
        if not success: raise RuntimeError(str(result))
        print('GENERATOR PASS',row['version'],flush=True)
        return result
    finally: lab.ROOT=original_root


if __name__=='__main__':
    results=[probe(row,i) for i,row in enumerate(lab.select(sys.argv[1] if len(sys.argv)>1 else '1.9,1.13.2,1.15.2,1.16.1,1.18.2,26.3'))]
    lab.save(lab.ROOT/'generator-probe-results.json',results)
