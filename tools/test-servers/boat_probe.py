"""Real ViaForge boat input against pinned Grim in an isolated, retained test world.

No active profile or existing lab world is opened or changed. All files, including
failed runs, remain available. The server owns mounting and all vehicle physics
are performed by the actual client. No synthetic vehicle movement is injected.
"""
import argparse
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import time
import zipfile
import lab
import grim
from boat_evidence import summarize
from protocol_selection_probe import available_port


def read_json(path):
    try:
        return json.loads(path.read_text(encoding='utf-8'))
    except (FileNotFoundError, json.JSONDecodeError):
        return {}


def write_action(directory, value):
    # Java may briefly hold the polled file without Windows delete sharing.
    # Keep the old complete command visible until replacement succeeds.
    temporary = directory / 'action.tmp'
    temporary.write_text(value, encoding='utf-8')
    deadline = time.monotonic() + 5
    while True:
        try:
            temporary.replace(directory / 'action.txt')
            return
        except PermissionError:
            if time.monotonic() >= deadline:
                raise
            time.sleep(.02)


def assert_no_client():
    command = "Get-CimInstance Win32_Process | Where-Object { $_.Name -match '^java(w)?\\.exe$' -and $_.CommandLine -match 'net.minecraft.client.main.Main|GradleStart|launchwrapper.Launch' } | Select-Object ProcessId,CommandLine | ConvertTo-Json -Compress"
    found = subprocess.check_output(['powershell', '-NoProfile', '-Command', command], creationflags=lab.NO_WINDOW).decode().strip()
    if found:
        raise RuntimeError('Existing Minecraft client: ' + found)


def run(version, label, client_type='viaforge', extended=False, baseline=False, reconnect=False, controls=False, scenario='boat', baseline_stage=None):
    assert_no_client()
    source_row = next(r for r in lab.GRIM_VERSIONS if r['minecraft_version'] == version)
    if (client_type=='native' or extended or controls) and version!='1.12.2' and not (scenario=='push' and version=='26.2' and not extended and not controls):
        raise ValueError('Native/extended script is currently mapped to exact 1.12.2')
    folder = lab.ROOT / (scenario + '-' + version + '-' + label + '-' + str(time.time_ns()))
    folder.mkdir()
    source = lab.ROOT / source_row['version']
    original = source / source_row['launch_jar']
    assert grim.digest(original) == source_row['platform_download']['checksums']['sha256']
    boot_args=[]
    with zipfile.ZipFile(original) as archive:
        if 'patch.properties' in archive.namelist():
            patch = dict(s.split('=', 1) for s in archive.read('patch.properties').decode().splitlines() if '=' in s and not s.startswith('#'))
            patched = source / ('cache/patched_' + version + '.jar')
            assert grim.digest(patched) == patch['patchedHash'].lower()
            shutil.copy2(patched, folder / 'server.jar')
            import paper_boot
            with zipfile.ZipFile(patched) as jar: guard=paper_boot.guard(jar.read('org/bukkit/craftbukkit/Main.class'))
            if guard:
                agent=lab.REPO/'build/libs/ViaForgeLab-LegacyPaperBoot-1.0.0.jar'
                boot_args=[f"-javaagent:{agent}={guard['main_sha256']}:{guard['offset']}:{guard['old_maximum']}"]
                lab.save(folder/'paper-boot-adapter.json',guard)
        else:
            patched=original
            shutil.copy2(original,folder/'server.jar')
            for directory in ('libraries','versions','cache'):
                if (source/directory).exists():shutil.copytree(source/directory,folder/directory)
    plugins = folder / 'plugins'
    plugins.mkdir()
    artifact = next(f for f in grim.lock()['grim']['files'] if f['primary'])
    grim_jar = source / 'plugins' / artifact['filename']
    assert grim.digest(grim_jar) == '91c06e7ae7da53636bc5e500d5af3d36a6180247e155fa5b4340da5a72f9eeb7'
    shutil.copy2(grim_jar, plugins / grim_jar.name)
    helper = lab.REPO / 'build/libs/ViaForgeLabAC-1.0.0.jar'
    shutil.copy2(helper, plugins / helper.name)
    # Copy configuration, never live datastore/player state. No check is weakened.
    shutil.copytree(source / 'plugins/GrimAC', plugins / 'GrimAC', ignore=shutil.ignore_patterns('*.db*', 'logs', 'debug'))
    (plugins / 'ViaForgeLabAC').mkdir()
    (plugins / 'ViaForgeLabAC/config.yml').write_text('enabled: true\ntrace-packets: true\ntrace-boats: true\n')
    game, rcon = available_port(), available_port()
    row = dict(source_row, version=folder.name, port=game.getsockname()[1], rcon_port=rcon.getsockname()[1])
    password = secrets.token_hex(24)
    lab.save(folder / 'control.json', dict(password=password))
    (folder / 'eula.txt').write_text('eula=true\n')
    (folder / 'bukkit.yml').write_text('settings:\n  connection-throttle: 0\n  allow-end: false\n')
    props = {'server-ip': '127.0.0.1', 'server-port': row['port'], 'online-mode': 'false',
             'enable-rcon': 'true', 'rcon.port': row['rcon_port'], 'rcon.password': password,
             'broadcast-rcon-to-ops': 'false', 'enable-query': 'false', 'gamemode': 1, 'force-gamemode': 'false',
             'view-distance': 16, 'simulation-distance': 3, 'level-type': 'FLAT', 'generate-structures': 'false',
             'generator-settings': '3;minecraft:bedrock,62*minecraft:stone;1;' if row['protocol']<393 else '',
             'allow-flight': 'true', 'allow-nether': 'false', 'spawn-protection': 0, 'max-players': 8,
             'motd': 'Isolated ViaForge boat diagnosis'}
    (folder / 'server.properties').write_text(''.join(f'{k}={v}\n' for k, v in props.items()))
    lab.save(folder / 'sources.json', dict(server=source_row, row=row, java=lab.java(source_row),
             paper_sha256=grim.digest(patched), grim_sha256=grim.digest(grim_jar), helper_sha256=grim.digest(helper)))
    config_files = [lab.REPO / 'run/options.txt', lab.REPO / 'run/ViaForge/viaforge.yml']
    for p in config_files:
        if p.exists(): shutil.copy2(p, folder / (p.name + '.before'))
    development=lab.REPO/'run/mods/ViaForge-development.jar'
    if baseline and (scenario=='push' or baseline_stage=='push'):
        old=lab.REPO/'build/backups/entity-push-20260922-143245/ViaForge-baseline-with-probe.jar'
        if not old.exists():raise RuntimeError('Explicit pre-push baseline archive missing')
        shutil.copy2(development,folder/'development-fixed.jar')
        shutil.copy2(old,development)
        lab.save(folder/'baseline-classes.json',dict(source=str(old),source_sha256=grim.digest(old),description='Pre-push production with observation probe only'))
    elif baseline:
        old=lab.REPO/'build/backups/boats-20260921-195746/ViaForge-development.jar'
        if not old.exists():raise RuntimeError('Explicit a24ca9c baseline archive missing')
        shutil.copy2(development,folder/'development-fixed.jar')
        with zipfile.ZipFile(old) as original_mod,zipfile.ZipFile(development) as current,zipfile.ZipFile(folder/'development-baseline.jar','w',zipfile.ZIP_DEFLATED) as out:
            replaced={}
            for item in current.infolist():
                target=item.filename.startswith('com/viaversion/viaforge/boats/ServerBoat') or item.filename=='com/viaversion/viaforge/mixin/impl/boats/MixinBoatPlayer.class'
                data=original_mod.read(item.filename) if target else current.read(item.filename)
                out.writestr(item,data)
                if target:
                    import hashlib
                    replaced[item.filename]=hashlib.sha256(data).hexdigest()
        lab.save(folder/'baseline-classes.json',dict(source=str(old),source_sha256=grim.digest(old),classes=replaced))
        shutil.copy2(folder/'development-baseline.jar',development)
    server = client = companion = None
    companion_dir = None
    cases = []
    report = dict(server=source_row, row=row, label=label, client=client_type, baseline=baseline, started=time.time(), cases=cases, completed=False, scenario=scenario)
    client_dir = folder / client_type
    client_dir.mkdir()
    server_log = folder / 'server-console.log'

    def start_server():
        nonlocal server
        with server_log.open('a') as log:
            process = subprocess.Popen([lab.java(source_row), '-Xms256M', '-Xmx1024M', '-XX:ActiveProcessorCount=2',
                '-DPaper.IgnoreJavaVersion=true', *boot_args, '-jar', 'server.jar', 'nogui'], cwd=folder, stdin=subprocess.PIPE,
                stdout=log, stderr=subprocess.STDOUT, text=True, creationflags=lab.NO_WINDOW)
        server=process # Retain ownership even if readiness fails.
        deadline = time.monotonic() + 120
        while time.monotonic() < deadline:
            if process.poll() is not None: raise RuntimeError('Paper exited: ' + str(server_log))
            if lab.status(row):
                try:
                    with lab.Rcon(row) as remote: remote.command('list')
                    return process
                except OSError: pass
            time.sleep(.3)
        raise TimeoutError('Server startup')

    def console(commands):
        marker = 'BOAT_DONE_' + str(time.time_ns())
        offset = server_log.stat().st_size
        server.stdin.write('\n'.join([*commands, 'say ' + marker]) + '\n')
        server.stdin.flush()
        deadline = time.monotonic() + 40
        while time.monotonic() < deadline:
            with server_log.open('rb') as log:
                log.seek(offset); output = log.read(1024*1024).decode('utf-8',errors='replace')
            if server_log.stat().st_size-offset>1024*1024:
                raise RuntimeError('Unbounded server output during command; inspect '+str(server_log))
            if '[Server] ' + marker in output:
                report.setdefault('commands', []).append(dict(time=time.time(), commands=commands, output=output))
                return output
            time.sleep(.1)
        raise TimeoutError('Console: ' + repr(commands))

    def action(label, value, seconds):
        write_action(client_dir, value)
        before = grim.state(row, True)
        start = time.time()
        time.sleep(seconds)
        case = dict(case=label, action=value, start=start, end=time.time(), before=before, after=grim.state(row, True))
        cases.append(case)
        lab.save(folder / 'report.json', report)
        print('CASE', label, flush=True)
        state = read_json(client_dir / 'client-state.json')
        if state.get('state') == 'FAIL' or client.poll() is not None: raise RuntimeError('Client stopped: ' + repr(state))

    print('BOAT PROBE', folder, flush=True)
    try:
        game.close(); rcon.close(); server = start_server()
        console(['save-all'])
        server.stdin.write('stop\n'); server.stdin.flush(); server.wait(timeout=60)
        backup = folder / 'backups/before-fixture'
        shutil.copytree(folder / 'world', backup / 'world')
        lab.save(backup / 'manifest.json', {str(p.relative_to(backup)): grim.digest(p) for p in (backup / 'world').rglob('*') if p.is_file()})
        server = start_server()
        console(['gamerule doDaylightCycle false', 'gamerule doMobSpawning false', 'gamerule doWeatherCycle false', 'time set 6000',
                 'setworldspawn -32 66 0'])
        if row['protocol']>=393:
            # Send finished chunks on join, not thousands of individual block
            # changes that can stall resource/chunk conversion during setup.
            console(['forceload add -48 -48 48 48'])
            time.sleep(2)
            if scenario=='boat':
                console(['fill -48 62 -48 48 62 48 stone','fill -48 63 -48 48 64 48 water',
                    'fill 32 63 -48 48 64 48 stone','fill -48 65 -48 48 67 48 air'])
            else:
                console(['fill -8 63 -8 8 63 8 stone','fill -8 64 -8 8 69 8 air'])
        if scenario=='push':
            console(['fill -8 63 -8 8 63 8 stone','fill -8 64 -8 8 69 8 air',
                     'gamerule spawnRadius 0','setworldspawn 0 64 0'])
        grim.control(row, 'on')
        env = dict(os.environ, VIAFORGE_NO_PAUSE='1', JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=4',
                   VIAFORGE_PUSH_PROBE='1' if scenario=='push' else '0', VIAFORGE_BOAT_PROBE=str(client_dir), VIAFORGE_BOAT_PROTOCOL=str(row['protocol']), VIAFORGE_BOAT_PORT=str(row['port']))
        for key in ('VIAFORGE_BLOCK_SMOKE_TEST', 'VIAFORGE_SMOKE_PROTOCOL', 'VIAFORGE_PROTOCOL_PROBE', 'VIAFORGE_LIVE_FLIGHT'):
            env.pop(key, None)
        with (client_dir / 'console.log').open('w') as log:
            if client_type=='native':
                if scenario=='push' and version=='26.2':
                    import native_push_probe
                    launch=native_push_probe.command(client_dir,row['port'])
                else:
                    import native_boat_probe
                    launch=native_boat_probe.command(client_dir,row['port'])
            else:
                launch=['cmd.exe', '/d', '/c', r'.\build.bat', 'runClient', '-x', 'preRunClient']
                if baseline:launch += ['-x','installDevelopmentJar']
            client = subprocess.Popen(launch,
                cwd=lab.REPO, env=env, stdout=log, stderr=subprocess.STDOUT, creationflags=lab.NO_WINDOW)
        deadline = time.monotonic() + 240
        while time.monotonic() < deadline:
            result = read_json(client_dir / 'client-state.json')
            if result.get('state') == 'ready': break
            if result.get('state') == 'FAIL' or client.poll() is not None: raise RuntimeError('Client startup: ' + repr(result))
            time.sleep(.3)
        else: raise TimeoutError('Client startup')
        name = result['name']; report['name'] = name
        if client_type=='viaforge':
            report['client_artifact']=dict(path=str(development),sha256=grim.digest(development),
                boat_sources={str(p.relative_to(lab.REPO)):grim.digest(p) for p in [
                    lab.REPO/'src/main/java/com/viaversion/viaforge/boats/ServerBoat.java',
                    lab.REPO/'src/main/java/com/viaversion/viaforge/mixin/impl/boats/MixinBoatPlayer.java']})
            if scenario=='push':
                relative=['compatibility/ClientEntityPush.java','compatibility/ClientEntityMotion.java',
                          'common/compatibility/EntityPushRules.java','common/compatibility/EntityPositionRules.java',
                          'common/compatibility/LegacyCompatibility.java','common/blocks/LegacyEntityPackets.java',
                          'mobs/ServerMobs.java','mixin/impl/compatibility/MixinEntityPush.java',
                          'mixin/impl/compatibility/MixinRemoteEntityPush.java','mixin/impl/compatibility/MixinEntityMotionPackets.java',
                          'mixin/impl/connect/MixinMotionThreshold.java']
                report['client_artifact']['push_sources']={s:grim.digest(lab.REPO/'src/main/java/com/viaversion/viaforge'/s) for s in relative}
        if scenario=='push':
            import push_probe
            push_probe.exercise(row,folder,report,name,action,console)
        else:
            action('Creative login', 'idle', 3)
            console(['tp ' + name + ' -32 66 0 -90 20'])
            action('Load fixture chunks', 'idle', 3)
            if row['protocol']<393:
                console(['fill -48 62 -48 48 62 48 stone','fill -48 63 -48 48 64 48 water', 'fill 32 63 -48 48 64 48 stone',
                         'fill -48 65 -48 48 67 48 air'])
            action('Settle fixture water', 'idle', 2)
            legacy=row['protocol']<393
            fixture = console(['testforblock 0 64 0 water 0', 'testforblock 0 64 0 flowing_water 0', 'testforblock 32 64 0 stone'] if legacy else
                ['execute if block 0 64 0 water[level=0] run say BOAT_WATER_OK','execute if block 32 64 0 stone run say BOAT_SHORE_OK'])
            if not (fixture.count('Successfully found the block') == 2 if legacy else '[Server] BOAT_WATER_OK' in fixture and '[Server] BOAT_SHORE_OK' in fixture):
                raise RuntimeError('Fixture not loaded/confirmed: ' + fixture)
            boat_id='Boat' if row['protocol']<315 else 'boat' if row['protocol']<768 else 'oak_boat'
            console(['gamemode '+('0' if legacy else 'survival')+' ' + name, 'tp ' + name + ' -32 65 0 -90 20',
                     'summon '+boat_id+' -31 64.6 0 {Rotation:[-90f,0f]}'])
            if extended:console(['entitydata @e[type=boat] {CustomName:"BoatProbe"}'])
            if client_type=='viaforge':
                deadline=time.monotonic()+60
                while time.monotonic()<deadline:
                    setup=read_json(client_dir/'setup.json')
                    if setup.get('gamemode')=='SURVIVAL' and setup.get('fixture_water') and setup.get('boat_present') and time.time()*1000-setup.get('time_ms',0)<2000:
                        report['client_fixture_confirmed']=setup;break
                    if read_json(client_dir/'client-state.json').get('state')=='FAIL' or client.poll() is not None:
                        raise RuntimeError('Client failed before loaded Survival boat fixture')
                    time.sleep(.2)
                else:raise TimeoutError('Client did not confirm loaded Survival water/boat fixture: '+repr(setup))
            action('Survival boarding', 'board', 3)
            for label, value, seconds in [('Rest in water','idle',4), ('Accelerate','forward',4), ('Full coast','idle',20),
                ('Left curve','forward-left',3), ('After turn','idle',20), ('Right curve','forward-right',3),
                ('Reverse','back',3), ('Combined directions','forward-back-left',3), ('Final rest','idle',20),
                ('Dismount','dismount',1), ('Reboard','board',3), ('Reboard rest','idle',3)]:
                action(label, value, seconds)
            if extended:
                def reset_boat(label, x, y, z, yaw, nbt=''):
                    action(label+' dismount','dismount',.6)
                    console(['kill @e[type=boat,name=BoatProbe]',
                        'tp '+name+f' {x-1} {y+1} {z} {yaw} 20',
                        f'summon boat {x} {y} {z} {{Rotation:[{yaw}f,0f],CustomName:"BoatProbe"'+nbt+'}'])
                    action(label+' board','board',3)
                    action(label+' settle','idle',2)
                reset_boat('Shore collision',25,64.6,0,-90)
                action('Water into solid shore','forward',5)
                action('Rest at shore','idle',5)
                reset_boat('Land to water',38,65,0,90)
                action('Stone to water','forward',7)
                action('Coast after land to water','idle',6)
                action('Prepare ice dismount','dismount',.6)
                console(['fill 32 64 -16 48 64 16 ice','testforblock 38 64 0 ice'])
                reset_boat('Ice to water',38,65,0,90,',Type:"spruce"')
                action('Ice accelerate and enter water','forward',3)
                action('Ice/water coast','idle',7)
                reset_boat('Immersion',-20,64.6,12,-90,',Type:"birch"')
                console(['fill -22 65 10 -18 66 14 water'])
                action('Submerge while driving','forward',1)
                console(['fill -28 65 4 -10 67 20 air'])
                action('Drive after immersion','forward',3)
                action('Rest after immersion','idle',5)
                reset_boat('Flowing water',-20,64.6,12,-90,',Type:"jungle"')
                console(['fill -22 65 10 -18 66 14 flowing_water 1'])
                action('Flowing water drive','forward',1)
                console(['fill -28 65 4 -10 67 20 air'])
                action('Flowing water rest','idle',5)
                reset_boat('Animal passenger',-20,64.6,-12,-90,',Passengers:[{id:"pig",NoAI:1b,Invulnerable:1b}]')
                action('Driver with animal passenger','forward-left',3)
                action('Rest with animal passenger','idle',3)
                action('Leave second seat','dismount',1)
                console(['kill @e[type=pig]'])
                reset_boat('Server teleport',-20,64.6,0,-90,',Type:"dark_oak"')
                action('Drive before server teleport','forward',2)
                console(['tp '+name+' -20 65 0 -90 20'])
                action('Authoritative player teleport','idle',3)
                console(['tp '+name+' @e[type=boat,name=BoatProbe,c=1]'])
                action('Board after teleport','board',3)
                action('Drive after teleport','forward-right',2)
                action('Coast after teleport','idle',5)
                if client_type=='viaforge':
                    import native_boat_probe
                    companion_dir=folder/'native-companion';companion_dir.mkdir()
                    with (companion_dir/'console.log').open('w') as log:
                        companion=subprocess.Popen(native_boat_probe.command(companion_dir,row['port']),cwd=companion_dir,
                            stdout=log,stderr=subprocess.STDOUT,creationflags=lab.NO_WINDOW)
                    deadline=time.monotonic()+120
                    while time.monotonic()<deadline:
                        other=read_json(companion_dir/'client-state.json')
                        if other.get('state')=='ready':break
                        if other.get('state')=='FAIL' or companion.poll() is not None:raise RuntimeError('Companion failed: '+repr(other))
                        time.sleep(.2)
                    else:raise TimeoutError('Companion login')
                    report['companion']=other
                    action('Prepare two players','dismount',1)
                    console(['kill @e[type=boat,name=BoatProbe]','gamemode 0 '+other['name'],
                        'tp '+other['name']+' -21 65 -12 -90 20','tp '+name+' -19 65 -12 90 20',
                        'summon boat -20 64.6 -12 {Rotation:[-90f,0f],CustomName:"BoatProbe",Type:"acacia"}'])
                    write_action(companion_dir, 'board');action('Native driver boards','idle',3)
                    write_action(companion_dir, 'idle');action('Second player boards','board',3)
                    action('Second player passenger','forward-left',3)
                    write_action(companion_dir, 'forward-right');action('Passenger during native driving','back',3)
                    write_action(companion_dir, 'idle');action('Passenger coast','idle',5)
                    write_action(companion_dir, 'dismount');action('Native driver leaves','idle',2)
                    write_action(companion_dir, 'idle');action('Driver after seat transfer','forward',3)
                    action('Rest after seat transfer','idle',6)
                    write_action(companion_dir, 'stop');companion.wait(timeout=45)
                if client_type=='viaforge':
                    action('Reconnect same account','reconnect',8)
                    reset_boat('After reconnect',-20,64.6,0,-90)
                    action('Drive after reconnect','forward',3)
                    action('Coast after reconnect','idle',6)
            if reconnect and not extended:
                if client_type=='native':
                    before=grim.state(row,True);start=time.time()
                    write_action(client_dir, 'stop');client.wait(timeout=60)
                    write_action(client_dir, 'idle')
                    with (client_dir/'console.log').open('a') as log:
                        client=subprocess.Popen(native_boat_probe.command(client_dir,row['port'],name=name),
                            cwd=client_dir,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=lab.NO_WINDOW)
                    deadline=time.monotonic()+120
                    while time.monotonic()<deadline:
                        if read_json(client_dir/'client-state.json').get('state')=='ready':break
                        if client.poll() is not None:raise RuntimeError('Native reconnect exited')
                        time.sleep(.2)
                    else:raise TimeoutError('Native reconnect')
                    time.sleep(8)
                    cases.append(dict(case='Reconnect same account',action='reconnect',start=start,end=time.time(),before=before,after=grim.state(row,True)))
                else:action('Reconnect same account','reconnect',8)
                action('Coast after reconnect','idle',6)
        write_action(client_dir, 'stop'); client.wait(timeout=60)
        report['completed'] = read_json(client_dir / 'client-state.json').get('state') == 'DONE'
        if controls:
            import grim_probe
            grim_probe.wait_for(lambda:not grim.state(row,True).get('players'), 'Real client quit cleanup')
            console(['save-all']);server.stdin.write('stop\n');server.stdin.flush();server.wait(timeout=60)
            shutil.copytree(folder/'world',folder/'backups/before-controls/world')
            server=start_server()
            samples=[];report['controls']=samples
            names=['BoatCtlA'+str(time.time_ns())[-7:],'BoatCtlB'+str(time.time_ns())[-7:]]
            grim_probe.exercise(row,names,samples,commands=console)
            grim_probe.wait_for(lambda:not grim.state(row,True).get('players'),'Control clients quit cleanup')
            grim.control(row,'off');console(['save-all']);server.stdin.write('stop\n');server.stdin.flush();server.wait(timeout=60)
            server=start_server()
            grim_probe.wait_for(lambda:grim.state(row,True).get('state')=='disabled','OFF survives restart')
            control_client=grim_probe.Client(row,names[0])
            try:
                grim_probe.wait_for(lambda:len(grim.state(row,True).get('players',[]))==1 and grim.state(row,True)['players'][0].get('verbose'),'Reconnect verbose')
                off=grim.state(row,True);assert off['players'][0]['disabled'] and not off['players'][0]['op'],off
                control_client.invalid_sprint();time.sleep(1.3)
                assert grim.state(row,True)['players'][0]['flags']==0
                samples.append(dict(case='restart_off_reconnect',actual=grim.state(row,True)))
                command=b'/labac on';control_client.send(2,lab.varint(len(command))+command)
                grim_probe.wait_for(lambda:grim.state(row,True).get('state')=='enabled','Non-OP ON command')
                control_client.invalid_sprint()
                grim_probe.wait_for(lambda:any('BadPacketsF' in m['json'] for m in control_client.messages),'Actual reconnect verbose chat')
                samples.append(dict(case='nonop_command_on',actual=grim.state(row,True),chat=control_client.messages))
                grim.control(row,'status')
            finally:control_client.close()
            grim_probe.wait_for(lambda:not grim.state(row,True).get('players'),'Final control client quit')
            console(['save-all']);server.stdin.write('stop\n');server.stdin.flush();server.wait(timeout=60)
            server=start_server()
            grim_probe.wait_for(lambda:grim.state(row,True).get('state')=='enabled','ON survives restart')
            samples.append(dict(case='restart_on',actual=grim.state(row,True)))
    except BaseException as error:
        report['error'] = repr(error)
        raise
    finally:
        if companion and companion.poll() is None:
            write_action(companion_dir, 'stop')
            companion.wait(timeout=45)
        if client and client.poll() is None:
            write_action(client_dir, 'stop')
            try: client.wait(timeout=60)
            except subprocess.TimeoutExpired: report['client_still_running'] = client.pid
        if server and server.poll() is None:
            server.stdin.write('save-all\nstop\n'); server.stdin.flush(); server.wait(timeout=60)
        if client is None or client.poll() is not None:
            if baseline:shutil.copy2(folder/'development-fixed.jar',development)
            for p in config_files:
                if (folder / (p.name + '.before')).exists(): shutil.copy2(folder / (p.name + '.before'), p)
        events_file = plugins / 'ViaForgeLabAC/events.jsonl'
        events = [json.loads(s) for s in events_file.read_text(encoding='utf-8').splitlines()] if events_file.exists() else []
        if report.get('companion'):
            report['companion_events']=[e for e in events if e.get('player',e.get('name'))==report['companion']['name']]
        events = [e for e in events if e.get('player',e.get('name')) == report.get('name')]
        poses_file = client_dir / 'client.jsonl'
        poses = [json.loads(s) for s in poses_file.read_text().splitlines()] if poses_file.exists() else []
        for case in cases:
            if scenario=='push':
                import push_probe
                case.update(push_probe.summarize(case,poses,events,report.get('name'),row['protocol']))
            else:case.update(summarize(case,poses,events,report.get('name'),row['protocol']))
        report['ended'] = time.time(); report['events'] = events
        lab.save(folder / 'report.json', report)
        print('REPORT', folder / 'report.json', flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version', default='1.12.2'); parser.add_argument('--label', default='baseline')
    parser.add_argument('--client', choices=['viaforge','native'],default='viaforge')
    parser.add_argument('--extended',action='store_true')
    parser.add_argument('--baseline',action='store_true',help='Use preserved a24ca9c boat classes with the same development input probe')
    parser.add_argument('--baseline-stage',choices=['boats','push'],help='Select the preserved pre-boat or pre-push production baseline (requires --baseline)')
    parser.add_argument('--reconnect',action='store_true',help='Reconnect while mounted after the core sequence, including the original-client comparison')
    parser.add_argument('--controls',action='store_true',help='After driving: genuine verbose negative controls and persistent ON/OFF on this isolated 1.12.2 server')
    args = parser.parse_args()
    if args.baseline_stage and not args.baseline:parser.error('--baseline-stage requires --baseline')
    run(args.version, args.label,args.client,args.extended,args.baseline,args.reconnect,args.controls,baseline_stage=args.baseline_stage)
