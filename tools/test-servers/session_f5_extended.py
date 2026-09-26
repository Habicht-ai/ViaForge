"""Real two-player observation plus native-modern negative sword-block case."""
import json,time
import lab,boat_probe
from session_visual_probe import Server,Client,plugin,wait
from session_visual_cases import NativeClient,OldServer,Translator

def run(legacy_only=False):
    boat_probe.assert_no_client();folder=lab.ROOT/('session-f5-extended-'+str(time.time_ns()));folder.mkdir();print('RUN',folder,flush=True)
    report=dict(cases=[]);server=proxy=native=forge=None
    try:
        # Prepare observer compilation before either client enters its measured phase.
        server=OldServer(folder/'old-backend');server.start();proxy=Translator(folder/'translator',server);proxy.start()
        nn='Nb'+str(time.time_ns())[-8:];fn='Vb'+str(time.time_ns())[-8:]
        native=NativeClient(folder/'native',proxy.port,nn)
        forge=Client(folder/'forge',lab.REPO/'build/development/ViaForge-1.8.9-4.4.0-client.1-dev-java8.jar');forge.start();forge.send('connect',name=fn,port=proxy.port,protocol=776);wait(forge.ready,120,'Forge joined')
        native.start()
        x=native.state()['player_x'];z=native.state()['player_z']
        for name,position,yaw in [(nn,z,180),(fn,z-4,0)]:
            server.command(f'gamemode 0 {name}');server.command(f'tp {name} {x} 4 {position} {yaw} 0');server.command(f'replaceitem entity {name} slot.hotbar.0 diamond_sword 1');server.command(f'replaceitem entity {name} slot.hotbar.1 stone 1')
        time.sleep(3)
        native.send('look',yaw=180,pitch=0)
        # Keep both players in already-loaded spawn chunks for this renderer test.
        wait(lambda:any(fn in p['name'] for p in native.state().get('remote_players',[])),20,'original remote player actually present')
        time.sleep(2)
        for label,keys in [('idle',''),('hold','use'),('release',''),('swap','use slot1')]:
            native.action(keys or 'idle');time.sleep(2);forge.send('photo',name='remote-original-'+label)
            report['cases'].append(dict(case='forge_observes_native',label=label,state=native.state()));print('REMOTE native',label,flush=True)
        native.action('idle');forge.send('slot',slot=0)
        for label,keys in [('idle',''),('hold','use'),('release',''),('swap','use')]:
            forge.send('input',keys=keys)
            if label=='swap':forge.send('slot',slot=1)
            time.sleep(2);native.photo('idle','remote-forge-'+label)
            report['cases'].append(dict(case='native_observes_forge',label=label,state=forge.state()));print('REMOTE forge',label,flush=True)
        native.action('slot0 idle');forge.send('input',keys='');forge.send('slot',slot=0);forge.send('input',keys='use');time.sleep(2);forge.send('photo',name='first-person-block');native.photo('use','first-person-block')
        native.stop();native=None;forge.stop();forge=None;proxy.stop();proxy=None;server.stop();server=None
        if legacy_only:report['result']='CAPTURED_REQUIRES_VISUAL_REVIEW';return
        # No legacy translator here. A plain modern sword must retain NONE.
        server=Server(folder/'modern-backend',plugin());server.start()
        for implementation in ['original','fixed']:
            name=('Nm' if implementation=='original' else 'Vm')+str(time.time_ns())[-8:]
            if implementation=='original':native=NativeClient(folder/'modern-original',server.port,name);native.start();client=native
            else:
                forge=Client(folder/'modern-fixed',lab.REPO/'build/development/ViaForge-1.8.9-4.4.0-client.1-dev-java8.jar');forge.start();forge.send('connect',name=name,port=server.port,protocol=776);wait(forge.ready,120,'modern Forge');client=forge
            server.command(f'gamemode survival {name}');server.command(f'item replace entity {name} hotbar.0 with diamond_sword');time.sleep(2)
            if implementation=='original':state=client.photo('f5front use','plain-modern-sword')
            else:client.send('view',camera=2);client.send('input',keys='use');time.sleep(2);client.send('photo',name='plain-modern-sword');state=client.state()
            report['cases'].append(dict(case='plain_modern',implementation=implementation,state=state));assert not state['using'] and state['use_action']=='NONE',state
            client.stop();native=forge=None
        report['result']='CAPTURED_REQUIRES_VISUAL_REVIEW'
    except Exception as error:report['result']='FAIL';report['error']=repr(error);raise
    finally:
        if native:native.stop()
        if forge:forge.stop()
        if proxy:proxy.stop()
        if server:server.stop()
        lab.save(folder/'report.json',report);print('REPORT',folder/'report.json',report.get('result'),flush=True)

if __name__=='__main__':
    import argparse
    parser=argparse.ArgumentParser();parser.add_argument('--legacy-only',action='store_true');run(parser.parse_args().legacy_only)
