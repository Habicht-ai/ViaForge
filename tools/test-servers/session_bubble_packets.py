"""Actual server particle commands: original sprites versus preserved ViaForge events."""
import json,threading,time
import lab,boat_probe
from session_visual_probe import Server,Client,plugin,wait
from session_visual_cases import NativeClient

def run():
    boat_probe.assert_no_client();folder=lab.ROOT/('session-bubble-packets-'+str(time.time_ns()));folder.mkdir();print('RUN',folder,flush=True)
    server=client=None;report=dict(cases=[])
    try:
        server=Server(folder/'backend',plugin());server.start()
        for implementation in ['original','fixed']:
            name=('Np' if implementation=='original' else 'Vp')+str(time.time_ns())[-8:]
            if implementation=='original':client=NativeClient(folder/implementation,server.port,name)
            else:
                client=Client(folder/implementation,lab.REPO/'build/development/ViaForge-1.8.9-4.4.0-client.1-dev-java8.jar')
            with (client.folder/'options.txt').open('a') as options:options.write('gamma:0.5\n')
            client.start()
            if implementation=='fixed':client.send('connect',name=name,port=server.port,protocol=776);wait(client.ready,120,'join')
            server.command('time set noon')
            server.command(f'gamemode spectator {name}');server.command(f'tp {name} 0.5 64 0.5 0 0')
            if implementation=='fixed':client.send('resource',path='minecraft:textures/particle/particles.png',snapshot=True)
            if implementation=='original':
                server.command('fill -2 64 2 2 68 6 water');time.sleep(2)
            for particle in ['bubble_pop','current_down','bubble_column_up']:
                def emit():
                    for i in range(60):
                        server.command(f'particle minecraft:{particle} 0.5 65.5 3.5 0.7 0.7 0.3 0.02 15 force {name}');time.sleep(.05)
                begin=time.time();worker=threading.Thread(target=emit);worker.start()
                if implementation=='original':client.photo('idle',particle)
                else:time.sleep(1.5);client.send('photo',name=particle)
                worker.join();time.sleep(.4)
                rows=[json.loads(l) for l in (client.folder/('packet-order.jsonl' if implementation=='original' else 'observations.jsonl')).read_text().splitlines() if l.strip()]
                rows=[r for r in rows if r.get('time_ms',0)>=begin*1000]
                if implementation=='original':
                    expected={'bubble_pop':'BubblePopParticle','current_down':'WaterCurrentDownParticle','bubble_column_up':'BubbleColumnUpParticle'}[particle]
                    count=sum(r.get('visual')==expected and r.get('phase')=='<init>' for r in rows)
                else:count=max([r.get('pop_count' if particle=='bubble_pop' else 'particle_count',0) for r in rows] or [0])
                assert count>0,(implementation,particle,count)
                report['cases'].append(dict(implementation=implementation,particle=particle,observed=count));print('PARTICLE',implementation,particle,count,flush=True)
            client.stop();client=None
        report['result']='CAPTURED_REQUIRES_VISUAL_REVIEW'
    except Exception as error:report['result']='FAIL';report['error']=repr(error);raise
    finally:
        if client:client.stop()
        if server:server.stop()
        lab.save(folder/'report.json',report);print('REPORT',folder/'report.json',report.get('result'),flush=True)

if __name__=='__main__':run()
