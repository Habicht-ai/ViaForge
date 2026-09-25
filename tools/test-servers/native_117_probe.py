"""Original 1.17.1 input observer; official mappings name reflection/hooks only."""
import hashlib, json, os, re, subprocess, time, uuid, zipfile
from pathlib import Path
import lab


def mapping_data(text):
    classes={}
    for line in text.splitlines():
        if line and not line.startswith((' ', '#')):
            original,obfuscated=line.rstrip(':').split(' -> ');classes[original]=obfuscated
    def descriptor(name):
        if name.endswith('[]'):return '['+descriptor(name[:-2])
        primitives=dict(void='V',boolean='Z',byte='B',char='C',short='S',int='I',long='J',float='F',double='D')
        return primitives.get(name) or 'L'+classes.get(name,name).replace('.','/')+';'
    fields={};methods={};hooks={};owner=None
    for line in text.splitlines():
        if not line or line.startswith('#'):continue
        if not line.startswith(' '):owner=classes[line.split(' -> ')[0]];continue
        definition,runtime=line.strip().split(' -> ')
        definition=re.sub(r'^\d+:\d+:','',definition)
        if '(' not in definition:
            fields[owner+'|'+definition.split()[-1]]=runtime
        else:
            match=re.match(r'(\S+) (\S+)\(([^)]*)\)',definition)
            if not match:continue
            result,name,args=match.groups();args=args.split(',') if args else []
            methods.setdefault(owner+'|'+name+'|'+str(len(args)),[])
            if runtime not in methods[owner+'|'+name+'|'+str(len(args))]:methods[owner+'|'+name+'|'+str(len(args))].append(runtime)
            desc='('+''.join(map(descriptor,args))+')'+descriptor(result)
            if owner in (classes['net.minecraft.client.Minecraft'],classes['net.minecraft.client.multiplayer.ClientPacketListener']):
                if name=='tick' and not args or name in ('handlePing','handleSetEntityData','send','onDisconnect') and len(args)==1:
                    hooks[owner.replace('.','/')+'|'+runtime+desc]=name
    return dict(classes=classes,fields=fields,methods=methods,hooks=hooks)


def command(folder,port,name=None):
    base=lab.ROOT/'native-clients/1.17.1'
    if not (base/'prepared.json').exists():
        from native_boat_probe import prepare
        prepare('1.17.1')
    prepared=json.loads((base/'prepared.json').read_text());meta=json.loads((base/'version.json').read_text())
    client=Path(prepared['classpath'][0]);assert hashlib.sha1(client.read_bytes()).hexdigest()==prepared['client']['sha1']
    mappings=meta['downloads']['client_mappings'];mapping_file=base/'client-mappings.txt'
    lab.download(mappings['url'],mapping_file,mappings['sha1'])
    lab.save(folder/'native-map.json',mapping_data(mapping_file.read_text()))
    asm=next((Path.home()/'.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.9.1').rglob('asm-9.9.1.jar'))
    commons=next((Path.home()/'.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm-commons/9.9.1').rglob('asm-commons-9.9.1.jar'))
    gson=next(Path(p) for p in prepared['classpath'] if 'gson' in p)
    classes=lab.REPO/'build/native117/classes';classes.mkdir(parents=True,exist_ok=True)
    javac=Path(lab.java({'java':21})).with_name('javac.exe')
    subprocess.run([str(javac),'--release','17','-cp',os.pathsep.join(map(str,[asm,commons,gson])),'-d',str(classes),str(lab.HERE/'native-probe/Native117Probe.java')],check=True,creationflags=lab.NO_WINDOW)
    agent=folder/'ViaForgeLab-Native117Probe.jar'
    with zipfile.ZipFile(agent,'w',zipfile.ZIP_DEFLATED) as jar:
        jar.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nPremain-Class: viaforge.lab.Native117Probe\n\n')
        for dependency in (asm,commons):
            with zipfile.ZipFile(dependency) as dep:
                for n in dep.namelist():
                    if n.startswith('org/') and n.endswith('.class'):jar.writestr(n,dep.read(n))
        for p in classes.rglob('*.class'):jar.write(p,p.relative_to(classes).as_posix())
    name=name or 'Native17'+str(time.time_ns())[-7:]
    (folder/'options.txt').write_text('pauseOnLostFocus:false\nrenderDistance:16\nmaxFps:60\nautoJump:false\n')
    lab.save(folder/'native-source.json',dict(version='1.17.1',client=prepared['client'],mappings=mappings,
        client_sha256=hashlib.sha256(client.read_bytes()).hexdigest(),agent_sha256=hashlib.sha256(agent.read_bytes()).hexdigest(),mapping_sha256=hashlib.sha256(mapping_file.read_bytes()).hexdigest()))
    return [lab.java({'java':17}),'-Xmx2G','-XX:ActiveProcessorCount=2','-Dpush.name='+name,'-Dprobe.port='+str(port),'-Djava.library.path='+prepared['natives'],
        f'-javaagent:{agent}={folder}','-cp',os.pathsep.join(prepared['classpath']),meta['mainClass'],
        '--username',name,'--version','1.17.1','--gameDir',str(folder),'--assetsDir',prepared['assets'],
        '--assetIndex',meta['assetIndex']['id'],'--accessToken','0','--uuid',str(uuid.uuid3(uuid.NAMESPACE_DNS,name)),
        '--userType','legacy']
