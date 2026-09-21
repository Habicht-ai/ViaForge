"""Launch installed ORIGINAL 26.2 client with a tick/key observation agent in a separate game directory."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
import uuid
import zipfile
import lab
import grim


def build():
    asm=next((Path.home()/'.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.9.1').rglob('asm-9.9.1.jar'))
    target=lab.REPO/'build/libs/ViaForgeLab-NativeInputProbe-1.0.0.jar'
    classes=lab.REPO/'build/native-probe/classes';classes.mkdir(parents=True,exist_ok=True)
    javac=Path(lab.java({'java':21})).with_name('javac.exe')
    subprocess.run([str(javac),'--release','17','-cp',str(asm),'-d',str(classes),str(lab.HERE/'native-probe/NativeInputProbe.java')],check=True,creationflags=lab.NO_WINDOW)
    with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as jar:
        jar.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nPremain-Class: viaforge.lab.NativeInputProbe\n\n')
        with zipfile.ZipFile(asm) as dep:
            for name in dep.namelist():
                if name.startswith('org/') and name.endswith('.class'):jar.writestr(name,dep.read(name))
        for file in classes.rglob('*.class'):jar.write(file,file.relative_to(classes).as_posix())
    return target


def probe():
    row=lab.select('26.2-grim')[0]
    status=lab.status(row)
    if status and status['players']['online']:raise RuntimeError('Vacant Grim variant required')
    if not status:lab.start([row])
    grim.control(row,'on')
    base=Path(os.environ['APPDATA'])/'.minecraft'
    metadata=json.loads((base/'versions/26.2/26.2.json').read_text())
    client=base/'versions/26.2/26.2.jar'
    assert hashlib.sha1(client.read_bytes()).hexdigest()==metadata['downloads']['client']['sha1']
    libraries=[]
    for lib in metadata['libraries']:
        allowed=not lib.get('rules')
        for rule in lib.get('rules',[]):
            if rule.get('os',{}).get('name','windows')=='windows':allowed=rule['action']=='allow'
        if not allowed:continue
        artifact=lib.get('downloads',{}).get('artifact')
        if artifact:
            path=base/'libraries'/artifact['path']
            lab.download(artifact['url'],path,artifact['sha1'])
            libraries.append(path)
    folder=lab.REPO/'build/native-probe'/str(time.time_ns());folder.mkdir(parents=True)
    (folder/'options.txt').write_text('pauseOnLostFocus:false\nrenderDistance:8\nsimulationDistance:3\nmaxFps:60\nautoJump:false\n')
    name='Native'+str(time.time_ns())[-8:]
    args=[lab.java(row),'-Xmx2G',f'-javaagent:{build()}={folder}','-cp',os.pathsep.join(map(str,[client,*libraries])),
          metadata['mainClass'],'--username',name,'--version','26.2','--gameDir',str(folder),'--assetsDir',str(base/'assets'),
          '--assetIndex',metadata['assetIndex']['id'],'--accessToken','0','--uuid',str(uuid.uuid3(uuid.NAMESPACE_DNS,name)),
          '--userType','legacy','--versionType','release','--quickPlayMultiplayer',f"127.0.0.1:{row['port']}"]
    stages=[]
    def action(value,seconds):
        (folder/'action.txt').write_text(value)
        stages.append(dict(action=value,time=time.time(),duration=seconds,grim=grim.state(row,True)))
        time.sleep(seconds)
    def command(text):
        with lab.Rcon(row) as remote:return remote.command(text.replace('@s',name))
    process=None
    try:
        with (folder/'client.log').open('w') as log:process=subprocess.Popen(args,cwd=folder,stdout=log,stderr=subprocess.STDOUT,creationflags=lab.NO_WINDOW)
        until=time.monotonic()+120
        while not (folder/'positions.csv').exists():
            if process.poll() is not None:raise RuntimeError('Original client exited: '+str(folder/'client.log'))
            if time.monotonic()>until:raise RuntimeError('Original login timed out')
            time.sleep(.3)
        action('idle',3)
        command('gamemode survival @s');command('tp @s -18 65 -83 -90 0')
        action('idle',2)
        for mode,duration in [('walk',1),('sprint',1),('jump',1.5),('sneak',1),('idle',1)]:action(mode,duration)
        command('item replace entity @s armor.chest with elytra')
        command('item replace entity @s weapon.mainhand with firework_rocket 32')
        command('tp @s -40 220 -53 -90 0')
        action('idle',.5);action('glide',1.5);action('rocket',3.5)
        command('tp @s 64 159 -52');action('idle',3)
        command('item replace entity @s armor.chest with air');action('idle',1)
        action('stop',1);process.wait(timeout=45)
        records=[json.loads(s) for s in (lab.ROOT/row['version']/'plugins/ViaForgeLabAC/events.jsonl').read_text(encoding='utf-8').splitlines()]
        records=[e for e in records if e.get('player',e.get('name'))==name]
        poses=(folder/'positions.csv').read_text().splitlines()
        report=dict(server=row,client='Original Mojang 26.2 + tick/key-only observation agent',client_sha256=grim.digest(client),
                    name=name,stages=stages,events=records,poses=poses,process_exit=process.returncode,
                    gliding=any(';true;false;' in p for p in poses),error=(folder/'error.txt').read_text() if (folder/'error.txt').exists() else None)
        lab.save(folder/'report.json',report)
        print('NATIVE PROBE RECORDED',folder/'report.json',flush=True)
    finally:
        if process and process.poll() is None:(folder/'action.txt').write_text('stop')


if __name__=='__main__':probe()
