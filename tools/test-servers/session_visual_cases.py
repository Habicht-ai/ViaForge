"""Screenshots and passive state from original 26.2 and isolated ViaForge clients."""
import argparse, hashlib, json, os, shutil, subprocess, time, urllib.request
from pathlib import Path
import lab, grim, boat_probe, native_push_probe
from session_visual_probe import Server, Client, plugin, read, wait, port, baseline_observer

class OldServer(Server):
    def __init__(self,folder):
        folder.mkdir();self.folder=folder;self.port=port();self.process=None
        manifest=json.load(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
        metadata=next(v for v in manifest['versions'] if v['id']=='1.8.9')
        info=json.load(urllib.request.urlopen(metadata['url']))
        jar=Path.home()/'.gradle/caches/unimined/net/minecraft/minecraft/1.8.9/minecraft-1.8.9-server.jar'
        assert hashlib.sha1(jar.read_bytes()).hexdigest()==info['downloads']['server']['sha1']
        shutil.copy2(jar,folder/'server.jar')
        (folder/'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={self.port}\nonline-mode=false\ngamemode=1\nforce-gamemode=false\nview-distance=4\nlevel-type=FLAT\nallow-nether=false\nallow-flight=true\nspawn-protection=0\nspawn-monsters=false\nspawn-animals=false\n')
        (folder/'eula.txt').write_text('eula=true\n')
        lab.save(folder/'source.json',dict(version='1.8.9',protocol=47,metadata_url=metadata['url'],server=info['downloads']['server'],sha256=grim.digest(jar),port=self.port))
    def start(self):
        with (self.folder/'console.log').open('w') as log:
            self.process=subprocess.Popen([lab.java({'java':8}),'-Xmx768M','-XX:ActiveProcessorCount=2','-jar','server.jar','nogui'],cwd=self.folder,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True,creationflags=lab.NO_WINDOW)
        wait(lambda:'Done (' in (self.folder/'console.log').read_text(errors='replace'),90,'1.8.9 startup')
        self.command('save-off');self.command('save-all');time.sleep(2)
        shutil.copytree(self.folder/'world',self.folder/'world-before-fixture',ignore=shutil.ignore_patterns('session.lock'))
        self.command('save-on');self.command('gamerule doMobSpawning false');self.command('gamerule doDaylightCycle false');self.command('time set day')

class Translator:
    def __init__(self,folder,server):
        folder.mkdir();self.folder=folder;self.port=port();self.server=server;self.process=None
        jar=lab.REPO/'build/inspection/session-resources/platform/ViaProxy-3.4.13.jar'
        assert grim.digest(jar)=='4bbb6a6b9d3dd6a2028773ed44b97a98751e4a48e416aaad4bf6d9c860083be9'
        shutil.copy2(jar,folder/'proxy.jar')
        lab.save(folder/'source.json',dict(version='ViaProxy 3.4.13',sha256=grim.digest(jar),port=self.port,backend=server.port,target_protocol=47,backend_version='1.8.9'))
    def start(self):
        with (self.folder/'console.log').open('w') as log:
            self.process=subprocess.Popen([lab.java({'java':21}),'-Xmx512M','-XX:ActiveProcessorCount=2','-jar','proxy.jar','cli','--bind-address',f'127.0.0.1:{self.port}','--target-address',f'127.0.0.1:{self.server.port}','--target-version','1.8.9'],cwd=self.folder,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True,creationflags=lab.NO_WINDOW)
        def started():
            if self.process.poll() is not None:raise RuntimeError('ViaProxy exited: '+str(self.folder/'console.log'))
            return 'Proxy started' in (self.folder/'console.log').read_text(errors='replace')
        wait(started,60,'ViaProxy startup')
    def stop(self):
        if self.process and self.process.poll() is None:
            # ViaProxy CLI has no console shutdown command; terminate this owned helper only.
            self.process.terminate();self.process.wait(timeout=30)

class NativeClient:
    def __init__(self,folder,server_port,name):
        folder.mkdir();self.folder=folder;self.process=None;self.name=name
        self.command=native_push_probe.command(folder,server_port,name)
        (folder/'options.txt').write_text('pauseOnLostFocus:false\nrenderDistance:4\nsimulationDistance:3\nmaxFps:60\nautoJump:false\nguiScale:2\n')
    def start(self,wait_join=True):
        env=dict(os.environ,VIAFORGE_INTERACTION_PROBE='1',VIAFORGE_VISUAL_SESSION='1',VIAFORGE_PROBE_TIMEOUT_MS='1200000')
        with (self.folder/'console.log').open('w') as log:
            self.process=subprocess.Popen(self.command,cwd=self.folder,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=lab.NO_WINDOW)
        if wait_join:wait(self.ready,180,'original 26.2 join')
    def send(self,op,**args):
        serial=getattr(self,'serial',0)+1;self.serial=serial
        p=self.folder/'command.next';p.write_text(json.dumps(dict(id=serial,op=op,**args)));p.replace(self.folder/'command.json')
        if op!='stop':wait(lambda:read(self.folder/'ui-state.json').get('command')==serial,15,'original command '+op)
    def state(self):
        if self.process.poll() is not None:raise RuntimeError('Original client exited: '+str(self.folder/'console.log'))
        return read(self.folder/'visual-state.json')
    def ready(self):
        state=self.state();return state.get('screen')=='none' and state.get('loaded') and state.get('loaded_column')
    def action(self,keys):
        (self.folder/'action.txt').write_text(keys)
        wait(lambda:self.state().get('action')==keys,10,'original input')
    def photo(self,keys,label):
        self.action(keys+' photo '+label);time.sleep(2.5);return self.state()
    def stop(self):
        if self.process and self.process.poll() is None:
            self.send('stop');self.process.wait(timeout=60)

def bubble_fixture(server):
    for x,block in [(-4,'soul_sand'),(2,'magma_block')]:
        server.command(f'fill {x} 63 0 {x+3} 71 3 glass')
        server.command(f'fill {x+1} 64 1 {x+2} 70 2 water')
        server.command(f'fill {x+1} 63 1 {x+2} 63 2 {block}')
        server.command(f'fill {x} 71 0 {x+3} 71 3 air')
    server.command('setblock 8 64 1 soul_sand');server.command('setblock 10 64 1 magma_block')
    server.command('setblock 10 65 1 oak_slab[type=bottom,waterlogged=true]')

def run(kind):
    boat_probe.assert_no_client();folder=lab.ROOT/('session-visual-'+kind+'-'+str(time.time_ns()));folder.mkdir();print('RUN',folder,flush=True)
    server=proxy=client=None;report=dict(kind=kind,started=time.time(),cases=[])
    try:
        if kind=='f5':
            server=OldServer(folder/'backend');server.start();proxy=Translator(folder/'translator',server);proxy.start();endpoint=proxy.port
        else:
            server=Server(folder/'backend',plugin());server.start();endpoint=server.port
        for implementation in ('original','before','fixed'):
            name=('Ns' if implementation=='original' else 'Vs')+str(time.time_ns())[-8:]
            if implementation=='original':client=NativeClient(folder/implementation,endpoint,name);client.start()
            else:
                jar=baseline_observer('visual-'+str(time.time_ns())) if implementation=='before' else lab.REPO/'build/development/ViaForge-1.8.9-4.4.0-client.1-dev-java8.jar'
                client=Client(folder/implementation,jar);client.start();client.send('connect',name=name,port=endpoint,protocol=776);wait(client.ready,120,'ViaForge visual join')
            if kind=='f5':
                server.command(f'gamemode 0 {name}');server.command(f'tp {name} 0.5 4 0.5 0 0');server.command(f'replaceitem entity {name} slot.hotbar.0 diamond_sword 1');server.command(f'replaceitem entity {name} slot.hotbar.1 stone 1');time.sleep(2)
                steps=[('idle',''),('start','use'),('hold','use'),('release',''),('reuse','use'),('switch','use'),('after_switch','')]
                for label,keys in steps:
                    if implementation=='original':
                        state=client.photo('f5front '+keys+(' slot1' if label=='switch' else ''),label)
                    else:
                        client.send('view',camera=2);client.send('input',keys=keys)
                        if label=='switch':client.send('slot',slot=1)
                        time.sleep(2);client.send('photo',name='f5-'+label);time.sleep(.3);state=client.state()
                    report['cases'].append(dict(implementation=implementation,label=label,state=state));print(implementation,label,state.get('using'),state.get('use_action'),flush=True)
            else:
                if implementation=='original':
                    bubble_fixture(server);time.sleep(3)
                    for label,condition in [('UP','-3 64 1 bubble_column[drag=false]'),('DOWN','3 64 1 bubble_column[drag=true]'),('DRY','8 64 1 soul_sand'),('SLAB','10 65 1 oak_slab[waterlogged=true]')]:
                        server.command(f'execute if block {condition} run say BUBBLE_FIXTURE_{label}')
                        wait(lambda:('BUBBLE_FIXTURE_'+label) in (server.folder/'console.log').read_text(errors='replace'),10,'verified fixture '+label)
                server.command(f'tp {name} 0.5 64 -4.5 0 0');time.sleep(2)
                server.command(f'gamemode spectator {name}')
                for label,position,setting in [('outside',(0.5,64,-4.5),0),('underwater',(-2.5,66,1.5),0),('surface',(-2.5,70,1.5),0),('downward',(3.5,67,1.5),0),('decreased',(0.5,64,-4.5),1),('minimal',(0.5,64,-4.5),2)]:
                    server.command(f'tp {name} {position[0]} {position[1]} {position[2]} 0 0')
                    if implementation=='original':state=client.photo('idle '+(['','decreased','minimal'][setting]),label)
                    else:
                        client.send('view',camera=0,particles=setting);time.sleep(3);client.send('photo',name='bubbles-'+label);time.sleep(.3);state=client.state()
                    report['cases'].append(dict(implementation=implementation,label=label,state=state));print(implementation,label,flush=True)
                    time.sleep(8)
            client.stop();client=None;time.sleep(2)
        report['result']='CAPTURED_REQUIRES_VISUAL_REVIEW'
    except Exception as error:report['result']='ERROR';report['error']=repr(error);raise
    finally:
        if client:client.stop()
        if proxy:proxy.stop()
        if server:server.stop()
        report['finished']=time.time();lab.save(folder/'report.json',report);print('REPORT',folder/'report.json',report.get('result'),flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('kind',choices=['f5','bubble']);run(parser.parse_args().kind)
