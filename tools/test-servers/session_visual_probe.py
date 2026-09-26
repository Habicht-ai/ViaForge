"""Isolated real-client resource/proxy/visual regression. Preserves every run and world."""
import argparse, hashlib, http.server, io, json, os, secrets, shutil, socket, struct
import subprocess, threading, time, zipfile, zlib
from pathlib import Path
import lab, grim, boat_probe

def read(path):
    try:return json.loads(path.read_text(encoding='utf-8-sig'))
    except (FileNotFoundError,json.JSONDecodeError,PermissionError):return {}

def wait(predicate, seconds=120, detail='condition'):
    until=time.monotonic()+seconds
    while time.monotonic()<until:
        result=predicate()
        if result:return result
        time.sleep(.2)
    raise TimeoutError(detail)

def port():
    with socket.socket() as s:s.bind(('127.0.0.1',0));return s.getsockname()[1]

def plugin():
    source=lab.HERE/'session-lab/SessionLab.java';classes=lab.REPO/'build/session-lab/classes';classes.mkdir(parents=True,exist_ok=True)
    bukkit=lab.ROOT/'downloads/cache/patched_1.12.2.jar'
    subprocess.run([str(Path(lab.java({'java':21})).with_name('javac.exe')),'--release','8','-cp',str(bukkit),'-d',str(classes),str(source)],check=True,creationflags=lab.NO_WINDOW)
    jar=classes.parent/'SessionLab.jar'
    with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
        z.writestr('plugin.yml','name: SessionLab\nversion: 1.0\nmain: viaforge.lab.SessionLab\ncommands:\n  sessionlab:\n    description: Isolated resource fixture\n')
        for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes).as_posix())
    return jar

class Server:
    def __init__(self,folder,fixture_plugin):
        row=next(r for r in lab.GRIM_VERSIONS if r['minecraft_version']=='26.2')
        original=lab.ROOT/row['version'];folder.mkdir()
        assert grim.digest(original/row['launch_jar'])==row['platform_download']['checksums']['sha256']
        shutil.copy2(original/row['launch_jar'],folder/'server.jar')
        for name in ('libraries','versions','cache'):
            if (original/name).exists():shutil.copytree(original/name,folder/name)
        (folder/'plugins').mkdir();shutil.copy2(fixture_plugin,folder/'plugins/SessionLab.jar')
        self.folder=folder;self.port=port();self.rcon=port();self.password=secrets.token_hex(24)
        props={'server-ip':'127.0.0.1','server-port':self.port,'online-mode':'false','enable-rcon':'true','rcon.port':self.rcon,'rcon.password':self.password,
               'enable-query':'false','gamemode':'creative','force-gamemode':'false','view-distance':4,'simulation-distance':3,'level-type':'minecraft:flat',
               'generator-settings':json.dumps({'layers':[{'block':'minecraft:bedrock','height':1},{'block':'minecraft:stone','height':127}],'biome':'minecraft:plains'}),
               'generate-structures':'false','allow-nether':'false','spawn-protection':0,'allow-flight':'true','max-players':6,'pause-when-empty-seconds':-1}
        (folder/'server.properties').write_text(''.join(f'{k}={v}\n' for k,v in props.items()));(folder/'eula.txt').write_text('eula=true\n')
        (folder/'bukkit.yml').write_text('settings:\n  connection-throttle: 0\n  allow-end: false\n')
        self.process=None;lab.save(folder/'source.json',dict(server=row,java=lab.java(row),plugin_sha256=grim.digest(fixture_plugin),port=self.port))
    def start(self):
        with (self.folder/'console.log').open('w') as log:
            self.process=subprocess.Popen([lab.java({'java':25}),'-Xmx1G','-XX:ActiveProcessorCount=2','-jar','server.jar','nogui'],cwd=self.folder,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True,creationflags=lab.NO_WINDOW)
        wait(lambda:'Done (' in (self.folder/'console.log').read_text(errors='replace'),180,'server startup')
        self.command('save-off');self.command('save-all flush')
        wait(lambda:'Saved the game' in (self.folder/'console.log').read_text(errors='replace'),30,'saved fixture world')
        shutil.copytree(self.folder/'world',self.folder/'world-before-fixture',ignore=shutil.ignore_patterns('session.lock'))
        self.command('save-on')
        self.command('setworldspawn 0 64 0');self.command('gamerule spawn_mobs false');self.command('time set day')
    def command(self,text):
        self.process.stdin.write(text+'\n');self.process.stdin.flush()
    def stop(self):
        if self.process and self.process.poll() is None:self.command('stop');self.process.wait(timeout=90)

class Client:
    def __init__(self,folder,jar):
        folder.mkdir();self.folder=folder;self.serial=0;self.process=None
        (folder/'mods').mkdir();shutil.copy2(jar,folder/'mods/ViaForge-development.jar')
        lab.save(folder/'artifact.json',dict(source=str(jar),sha256=grim.digest(jar),size=jar.stat().st_size))
        shutil.copytree(lab.REPO/'run/natives',folder/'natives')
        (folder/'ViaForge').mkdir()
        # Shared verified original assets only; personal packs/options/caches remain isolated.
        subprocess.run(['powershell','-NoProfile','-Command',f"New-Item -ItemType Junction -Path '{folder / 'ViaForge/block-assets'}' -Target '{lab.REPO / 'run/ViaForge/block-assets'}' | Out-Null"],check=True,creationflags=lab.NO_WINDOW)
        (folder/'options.txt').write_text('pauseOnLostFocus:false\nrenderDistance:4\nmaxFps:60\nguiScale:2\n')
    def start(self):
        env=dict(os.environ,VIAFORGE_NO_PAUSE='1',JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=4',VIAFORGE_SESSION_PROBE=str(self.folder),VIAFORGE_GAME_DIR=str(self.folder))
        for key in ('VIAFORGE_BLOCK_SMOKE_TEST','VIAFORGE_BOAT_PROBE','VIAFORGE_PROTOCOL_PROBE'):env.pop(key,None)
        with (self.folder/'console.log').open('w') as log:
            self.process=subprocess.Popen(['cmd.exe','/d','/c',str(lab.REPO/'build.bat'),'runClient','-x','preRunClient','-x','installDevelopmentJar'],cwd=lab.REPO,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=lab.NO_WINDOW)
        def started():
            if self.process.poll() is not None:raise RuntimeError('Forge exited: '+str(self.folder/'console.log'))
            return read(self.folder/'state.json')
        wait(started,180,'Forge probe startup')
        state=read(self.folder/'state.json');assert Path(state['game_dir']).resolve()==self.folder.resolve(),state
    def send(self,op,**args):
        self.serial+=1;p=self.folder/'command.next';p.write_text(json.dumps(dict(id=self.serial,op=op,**args)));p.replace(self.folder/'command.json')
        if op!='stop':wait(lambda:read(self.folder/'state.json').get('command')==self.serial,15,'client command '+op)
    def state(self):
        if (self.folder/'error.txt').exists():raise RuntimeError('Probe failed: '+(self.folder/'error.txt').read_text())
        if self.process.poll() is not None:raise RuntimeError('Client exited: '+str(self.folder/'console.log'))
        return read(self.folder/'state.json')
    def ready(self):
        state=self.state();return state.get('screen')=='none' and state.get('loaded') and not state.get('awaiting_world',True)
    def stop(self):
        if self.process and self.process.poll() is None:self.send('stop');self.process.wait(timeout=90)

class Proxy:
    def __init__(self,folder,lobby,game):
        folder.mkdir();self.folder=folder;self.port=port();self.process=None
        source=lab.REPO/'build/inspection/session-resources/platform'
        jar=source/'velocity-4.2.0-30.jar'
        assert grim.digest(jar)=='35a5596a5468a035d8a32c8de5ebb0dc6b8d8f0cc3ff5169d514aca762af8aa8'
        shutil.copy2(jar,folder/'proxy.jar')
        config=(source/'default-velocity.toml').read_text()
        config=config.replace('0.0.0.0:25565',f'127.0.0.1:{self.port}').replace('online-mode = true','online-mode = false').replace('force-key-authentication = true','force-key-authentication = false')
        config=config.replace('127.0.0.1:30066',f'127.0.0.1:{lobby.port}').replace('factions = "127.0.0.1:30067"',f'gungame = "127.0.0.1:{game.port}"').replace('minigames = "127.0.0.1:30068"','')
        start=config.index('[forced-hosts]');end=config.index('[advanced]');config=config[:start]+'[forced-hosts]\n\n'+config[end:]
        (folder/'velocity.toml').write_text(config)
        lab.save(folder/'source.json',dict(version='4.2.0 build 30',sha256=grim.digest(jar),port=self.port,backends=dict(lobby=lobby.port,gungame=game.port)))
    def start(self):
        with (self.folder/'console.log').open('w') as log:
            self.process=subprocess.Popen([lab.java({'java':25}),'-Xmx384M','-XX:ActiveProcessorCount=2','-jar','proxy.jar'],cwd=self.folder,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True,creationflags=lab.NO_WINDOW)
        def started():
            if self.process.poll() is not None:raise RuntimeError('Velocity exited: '+str(self.folder/'console.log'))
            return 'Done (' in (self.folder/'console.log').read_text(errors='replace')
        wait(started,60,'Velocity startup')
    def stop(self):
        if self.process and self.process.poll() is None:
            self.process.stdin.write('shutdown\n');self.process.stdin.flush();self.process.wait(timeout=60)

def baseline_observer(label,backup=None):
    backup=backup or lab.REPO/'build/backups/session-resources-initial'
    original=backup/'run/mods/ViaForge-development.jar';current=lab.REPO/'build/development/ViaForge-1.8.9-4.4.0-client.1-dev-java8.jar'
    target=backup/f'ViaForge-before-with-session-probe-{label}.jar'
    assert not target.exists(),target
    with zipfile.ZipFile(original) as a,zipfile.ZipFile(current) as b:
        replacements={n:b.read(n) for n in b.namelist() if n.startswith('com/viaversion/viaforge/development/SessionVisualProbe') or n=='com/viaversion/viaforge/development/DevelopmentAccounts.class'}
        with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as z:
            for info in a.infolist():
                if info.filename not in replacements:z.writestr(info,a.read(info))
            for n,data in replacements.items():z.writestr(n,data)
    with zipfile.ZipFile(original) as a,zipfile.ZipFile(target) as b:
        changed=[n for n in sorted(set(a.namelist())|set(b.namelist())) if n not in a.namelist() or n not in b.namelist() or a.read(n)!=b.read(n)]
    assert all(n in replacements for n in changed)
    lab.save(backup/f'observer-{label}-proof.json',dict(original_sha256=grim.digest(original),instrumented_sha256=grim.digest(target),changed_entries=changed))
    return target

def run_proxy(mode):
    boat_probe.assert_no_client();folder=lab.ROOT/('session-'+mode+'-'+str(time.time_ns()));folder.mkdir()
    print('RUN',folder,flush=True);servers=[];proxy=client=None
    report=dict(mode=mode,started=time.time(),switches=[])
    try:
        fixture=plugin()
        for name in ('lobby','gungame'):
            server=Server(folder/name,fixture);servers.append(server);server.start()
        proxy=Proxy(folder/'proxy',*servers);proxy.start()
        artifact=baseline_observer(str(time.time_ns())) if mode.startswith('before') else lab.REPO/'build/development/ViaForge-1.8.9-4.4.0-client.1-dev-java8.jar'
        report['artifact_sha256']=grim.digest(artifact)
        client=Client(folder/'client',artifact);client.start();name='VFsw'+str(time.time_ns())[-8:]
        client.send('connect',name=name,port=proxy.port,protocol=776);wait(client.ready,120,'first proxy join')
        report['initial']=client.state()
        for i in range(12):
            target='gungame' if i%2==0 else 'lobby';before=client.state();begin=time.time()
            client.send('chat',text='/server '+target)
            wait(lambda:client.state().get('world')!=before['world'],30,'changed world')
            ready=True
            try:wait(client.ready,20,'terrain transition')
            except TimeoutError:ready=False
            row=dict(target=target,ready=ready,seconds=time.time()-begin,state=client.state());report['switches'].append(row)
            print('SWITCH',i,target,ready,row['state'].get('screen'),flush=True)
            client.send('photo',name=f'switch-{i:02d}')
            if not ready:break
            time.sleep(.5)
        report['result']='REPRODUCED' if any(not x['ready'] for x in report['switches']) else 'PASS'
    except Exception as exc:
        report['error']=repr(exc);report['result']='ERROR';raise
    finally:
        if client:client.stop()
        if proxy:proxy.stop()
        for server in reversed(servers):server.stop()
        report['finished']=time.time();lab.save(folder/'report.json',report);print('REPORT',folder/'report.json',report['result'],flush=True)

def png():
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data))
    pixels=b''.join(b'\0'+bytes([220,30,180,255])*16 for _ in range(16))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(pixels))+chunk(b'IEND',b'')

def pack_bytes():
    out=io.BytesIO()
    with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
        z.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':1,'description':'Isolated magenta stone'}}))
        z.writestr('assets/minecraft/textures/blocks/stone.png',png())
    return out.getvalue()

def run(mode):
    boat_probe.assert_no_client();folder=lab.ROOT/('session-'+mode+'-'+str(time.time_ns()));folder.mkdir()
    print('RUN',folder,flush=True);server=None;client=None;http_server=None
    report={'mode':mode,'started':time.time(),'checks':[]}
    data=pack_bytes();sha=hashlib.sha1(data).hexdigest()
    class Handler(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            self.send_response(200);self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
        def log_message(self,*args):pass
    try:
        jar=plugin();server=Server(folder/'backend',jar);server.start()
        http_server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);threading.Thread(target=http_server.serve_forever,daemon=True).start()
        artifact=lab.REPO/('build/backups/session-resources-initial/ViaForge-before-with-session-probe-v3.jar' if mode.startswith('before') else 'run/mods/ViaForge-development.jar')
        client=Client(folder/'client',artifact);client.start()
        name='VFsr'+str(time.time_ns())[-8:];client.send('connect',name=name,port=server.port,protocol=776)
        wait(client.ready,120,'first join');time.sleep(2);report['first_join']=client.state()
        server.command(f'sessionlab pack {name} http://127.0.0.1:{http_server.server_port}/pack.zip {sha}')
        wait(lambda:client.state().get('screen')=='GuiYesNo',30,'resource prompt');client.send('photo',name='pack-prompt');time.sleep(.3)
        report['cache_exists_before_accept']=(client.folder/'server-resource-packs').exists()
        try:client.send('button',button=0)
        except TimeoutError:pass
        time.sleep(5);report['state_after_accept']=read(client.folder/'state.json');report['client_exited']=client.process.poll() is not None
        report['crash_reports']=[str(p) for p in client.folder.glob('crash-reports/*')]
        console=(client.folder/'console.log').read_text(errors='replace')
        report['directory_exception']='Parameter \'directory\' is not a directory' in console and 'deleteOldServerResourcesPacks' in console
        report['result']='REPRODUCED' if report['directory_exception'] else 'OBSERVED'
    except Exception as exc:
        report['error']=repr(exc);report['result']='ERROR';raise
    finally:
        if client:client.stop()
        if server:server.stop()
        if http_server:http_server.shutdown();http_server.server_close()
        report['finished']=time.time();lab.save(folder/'report.json',report);print('REPORT',folder/'report.json',report.get('result'),flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--mode',default='before-pack');args=parser.parse_args()
    (run_proxy if 'proxy' in args.mode else run)(args.mode)
