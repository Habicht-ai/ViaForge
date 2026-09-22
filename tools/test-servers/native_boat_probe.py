"""Verified official 1.12.2 client preparation for the boat comparison."""
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import subprocess
import urllib.request
import zipfile
import lab


def command(folder, port, name=None):
    base=lab.ROOT/'native-clients/1.12.2'
    if not (base/'prepared.json').exists(): prepare()
    prepared=json.loads((base/'prepared.json').read_text())
    client=Path(prepared['classpath'][0])
    assert hashlib.sha1(client.read_bytes()).hexdigest()==prepared['client']['sha1']
    asm=next((Path.home()/'.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.9.1').rglob('asm-9.9.1.jar'))
    gson=next(Path(p) for p in prepared['classpath'] if 'gson' in p)
    classes=lab.REPO/'build/native-boat/classes';classes.mkdir(parents=True,exist_ok=True)
    javac=Path(lab.java({'java':21})).with_name('javac.exe')
    subprocess.run([str(javac),'--release','8','-cp',os.pathsep.join(map(str,[asm,gson])), '-d',str(classes),
        str(lab.HERE/'native-probe/NativeBoatProbe.java')],check=True,creationflags=lab.NO_WINDOW)
    agent=lab.REPO/'build/libs/ViaForgeLab-NativeBoatProbe-1.0.0.jar'
    with zipfile.ZipFile(agent,'w',zipfile.ZIP_DEFLATED) as jar:
        jar.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nPremain-Class: viaforge.lab.NativeBoatProbe\n\n')
        with zipfile.ZipFile(asm) as dep:
            for n in dep.namelist():
                if n.startswith('org/') and n.endswith('.class'):jar.writestr(n,dep.read(n))
        for p in classes.rglob('*.class'):jar.write(p,p.relative_to(classes).as_posix())
    import time
    name=name or 'NativeB'+str(time.time_ns())[-8:]
    lab.save(folder/'native-source.json',dict(version='1.12.2',client=prepared['client'],
        client_sha256=hashlib.sha256(client.read_bytes()).hexdigest(),agent_sha256=hashlib.sha256(agent.read_bytes()).hexdigest()))
    (folder/'options.txt').write_text('pauseOnLostFocus:false\nrenderDistance:16\nmaxFps:60\nautoJump:false\n')
    return [lab.java({'java':8}),'-Xmx1G','-XX:ActiveProcessorCount=2','-Dboat.name='+name,
        '-Djava.library.path='+prepared['natives'],f'-javaagent:{agent}={folder}','-cp',os.pathsep.join(prepared['classpath']),
        'net.minecraft.client.main.Main','--username',name,'--version','1.12.2','--gameDir',str(folder),
        '--assetsDir',prepared['assets'],'--assetIndex','1.12','--accessToken','0','--userType','legacy',
        '--server','127.0.0.1','--port',str(port)]


def prepare(version='1.12.2'):
    base=lab.ROOT/'native-clients'/version
    base.mkdir(parents=True,exist_ok=True)
    manifest=json.load(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
    descriptor=next(v for v in manifest['versions'] if v['id']==version)
    lab.download(descriptor['url'],base/'version.json',descriptor['sha1'])
    meta=json.loads((base/'version.json').read_text())
    client=meta['downloads']['client'];lab.download(client['url'],base/'client.jar',client['sha1'])
    libs=[];natives=base/'natives';natives.mkdir(exist_ok=True)
    for lib in meta['libraries']:
        allowed=not lib.get('rules')
        for rule in lib.get('rules',[]):
            if rule.get('os',{}).get('name','windows')=='windows': allowed=rule['action']=='allow'
        if not allowed:continue
        artifact=lib.get('downloads',{}).get('artifact')
        if artifact:
            path=base/'libraries'/artifact['path'];lab.download(artifact['url'],path,artifact['sha1']);libs.append(path)
        classifier=lib.get('natives',{}).get('windows')
        if classifier:
            artifact=lib['downloads']['classifiers'][classifier.replace('${arch}','64')]
            path=base/'libraries'/artifact['path'];lab.download(artifact['url'],path,artifact['sha1'])
            with zipfile.ZipFile(path) as archive:
                for entry in archive.namelist():
                    if entry.lower().endswith('.dll'):
                        (natives/Path(entry).name).write_bytes(archive.read(entry))
    assets=Path(os.environ['APPDATA'])/'.minecraft/assets'
    index=meta['assetIndex'];index_path=assets/'indexes'/(index['id']+'.json');lab.download(index['url'],index_path,index['sha1'])
    def asset(row):
        h=row['hash'];lab.download('https://resources.download.minecraft.net/'+h[:2]+'/'+h,assets/'objects'/h[:2]/h,h)
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        list(pool.map(asset,json.loads(index_path.read_text())['objects'].values()))
    lab.save(base/'prepared.json',dict(version=version,metadata=descriptor,client=client,
        classpath=[str(base/'client.jar'),*map(str,libs)],natives=str(natives),assets=str(assets)))
    print('NATIVE PREPARED',base,flush=True)
    return base


if __name__=='__main__':prepare()
