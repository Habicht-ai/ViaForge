"""Pinned official 26.2 client, observed without replacing simulation or packets."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
import uuid
import zipfile
import lab


def command(folder, port, name=None):
    base=Path(os.environ['APPDATA'])/'.minecraft'
    metadata=json.loads((base/'versions/26.2/26.2.json').read_text())
    client=base/'versions/26.2/26.2.jar'
    assert hashlib.sha1(client.read_bytes()).hexdigest()==metadata['downloads']['client']['sha1']
    libraries=[]
    for lib in metadata['libraries']:
        allowed=not lib.get('rules')
        for rule in lib.get('rules',[]):
            if rule.get('os',{}).get('name','windows')=='windows':allowed=rule['action']=='allow'
        if allowed and (artifact:=lib.get('downloads',{}).get('artifact')):
            path=base/'libraries'/artifact['path'];lab.download(artifact['url'],path,artifact['sha1']);libraries.append(path)
    asm=next((Path.home()/'.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.9.1').rglob('asm-9.9.1.jar'))
    gson=next(p for p in libraries if 'gson' in str(p))
    classes=lab.REPO/'build/native-push/classes';classes.mkdir(parents=True,exist_ok=True)
    javac=Path(lab.java({'java':21})).with_name('javac.exe')
    subprocess.run([str(javac),'--release','17','-cp',os.pathsep.join(map(str,[asm,gson])),'-d',str(classes),str(lab.HERE/'native-probe/NativePushProbe.java')],check=True,creationflags=lab.NO_WINDOW)
    agent=lab.REPO/'build/libs/ViaForgeLab-NativePushProbe-1.0.0.jar'
    with zipfile.ZipFile(agent,'w',zipfile.ZIP_DEFLATED) as jar:
        jar.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nPremain-Class: viaforge.lab.NativePushProbe\n\n')
        with zipfile.ZipFile(asm) as dep:
            for n in dep.namelist():
                if n.startswith('org/') and n.endswith('.class'):jar.writestr(n,dep.read(n))
        for p in classes.rglob('*.class'):jar.write(p,p.relative_to(classes).as_posix())
    name=name or 'NativeP'+str(time.time_ns())[-8:]
    (folder/'options.txt').write_text('pauseOnLostFocus:false\nrenderDistance:16\nsimulationDistance:3\nmaxFps:60\nautoJump:false\n')
    lab.save(folder/'native-source.json',dict(version='26.2',client_sha256=hashlib.sha256(client.read_bytes()).hexdigest(),client=metadata['downloads']['client'],agent_sha256=hashlib.sha256(agent.read_bytes()).hexdigest()))
    return [lab.java({'java':25}),'-Xmx2G','-XX:ActiveProcessorCount=2','-Dpush.name='+name,
        f'-javaagent:{agent}={folder}','-cp',os.pathsep.join(map(str,[client,*libraries])),metadata['mainClass'],
        '--username',name,'--version','26.2','--gameDir',str(folder),'--assetsDir',str(base/'assets'),
        '--assetIndex',metadata['assetIndex']['id'],'--accessToken','0','--uuid',str(uuid.uuid3(uuid.NAMESPACE_DNS,name)),
        '--userType','legacy','--versionType','release','--quickPlayMultiplayer',f'127.0.0.1:{port}']
