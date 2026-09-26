"""Verify cached official clients and inspect the particle classes at behavior boundaries."""
import hashlib, json, subprocess, urllib.request, zipfile
from pathlib import Path
import lab

def main():
    base=lab.REPO/'build/inspection/session-resources'
    manifest=json.load(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
    proof={}
    for version in ['1.13','1.14.4','1.15','1.16.5','1.17','1.18.1','1.18.2','1.19','1.20.1','1.21.1','1.21.3','1.21.5','1.21.6','1.21.9','1.21.11']:
        entry=next(r for r in manifest['versions'] if r['id']==version)
        meta=json.load(urllib.request.urlopen(entry['url']))
        jar=lab.REPO/'run/ViaForge/block-assets'/f'{version}-client.jar'
        if not jar.exists():
            print('Missing cached',version,flush=True);continue
        assert hashlib.sha1(jar.read_bytes()).hexdigest()==meta['downloads']['client']['sha1']
        folder=base/version;folder.mkdir(exist_ok=True)
        proof[version]={'metadata_url':entry['url'],'client':meta['downloads']['client'],'sha256':hashlib.sha256(jar.read_bytes()).hexdigest()}
        if 'client_mappings' not in meta['downloads']:continue
        mapping=meta['downloads']['client_mappings'];path=folder/'mappings.txt'
        lab.download(mapping['url'],path,mapping['sha1'])
        names={l.split(' -> ')[0]:l.split(' -> ')[1][:-1] for l in path.read_text().splitlines() if ' -> ' in l and not l.startswith(' ')}
        wanted=['net.minecraft.client.particle.'+n for n in ('BubbleColumnUpParticle','WaterCurrentDownParticle','BubblePopParticle','Particle','TextureSheetParticle','SingleQuadParticle')]
        wanted+=['net.minecraft.client.multiplayer.ClientLevel','net.minecraft.client.multiplayer.ClientWorld','net.minecraft.world.level.block.BubbleColumnBlock']
        with zipfile.ZipFile(jar) as z:
            for name in wanted:
                if name not in names:continue
                obf=names[name];target=folder/(obf+'.class');target.write_bytes(z.read(obf+'.class'))
                if not (folder/(obf+'.java')).exists():
                    with (folder/'decompile.log').open('a') as log:
                        subprocess.run([lab.java({'java':21}),'-jar',str(lab.REPO/'build/inspection/cfr-0.152.jar'),str(target),'--outputdir',str(folder),'--silent','true'],stdout=log,stderr=log,check=True,creationflags=lab.NO_WINDOW)
        proof[version]['classes']={name:names[name] for name in wanted if name in names}
        print(version,proof[version]['classes'],flush=True)
    lab.save(base/'bubble-sources.json',proof)

if __name__=='__main__':main()
