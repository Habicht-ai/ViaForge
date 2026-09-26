"""Real rapid proxy joins and cross-dimension respawns, with saved fixture worlds."""
import shutil,time
import lab,boat_probe
from session_visual_probe import Server,Proxy,Client,plugin,wait

def run():
    boat_probe.assert_no_client();folder=lab.ROOT/('session-switch-edges-'+str(time.time_ns()));folder.mkdir();print('RUN',folder,flush=True)
    servers=[];proxy=client=None;report=dict(cases=[])
    try:
        fixture=plugin()
        for label in ['lobby','gungame']:
            server=Server(folder/label,fixture);servers.append(server)
            p=server.folder/'server.properties';p.write_text(p.read_text().replace('allow-nether=false','allow-nether=true'))
            server.start()
            nether=server.folder/'world_nether'
            if nether.exists():
                server.command('save-off');server.command('save-all flush');time.sleep(2)
                shutil.copytree(nether,server.folder/'nether-before-fixture',ignore=shutil.ignore_patterns('session.lock'));server.command('save-on')
        proxy=Proxy(folder/'proxy',*servers);proxy.start();client=Client(folder/'client',lab.REPO/'build/development/ViaForge-1.8.9-4.4.0-client.1-dev-java8.jar');client.start()
        name='Ve'+str(time.time_ns())[-8:];client.send('connect',name=name,port=proxy.port,protocol=776);wait(client.ready,120,'join')
        for i in range(12):
            target='gungame' if i%2==0 else 'lobby';previous=client.state()['world'];start=time.time();client.send('chat',text='/server '+target)
            wait(lambda:client.state().get('world')!=previous,30,'new join world');wait(client.ready,30,'rapid switch ready')
            report['cases'].append(dict(case='rapid_switch',target=target,seconds=time.time()-start,state=client.state()));print('SWITCH',i,target,flush=True)
        for cycle in range(2):
            for dimension,wire,y in [('the_nether',-1,90),('overworld',0,64)]:
                servers[0].command(f'execute in minecraft:{dimension} run tp {name} 0.5 {y} 0.5')
                wait(lambda:client.state().get('dimension')==wire,30,'actual dimension respawn');wait(client.ready,30,'dimension terrain ready')
                client.send('photo',name=f'dimension-{cycle}-{dimension}');report['cases'].append(dict(case='dimension',target=dimension,state=client.state()));print('DIMENSION',cycle,dimension,flush=True)
        client.send('disconnect');wait(lambda:client.state().get('screen')=='GuiMainMenu',20,'disconnect');time.sleep(3.1)
        client.send('connect',name=name,port=proxy.port,protocol=776);wait(client.ready,120,'reconnect');report['cases'].append(dict(case='reconnect',state=client.state()));report['result']='PASS'
    except Exception as error:report['result']='FAIL';report['error']=repr(error);raise
    finally:
        if client:client.stop()
        if proxy:proxy.stop()
        for server in reversed(servers):server.stop()
        lab.save(folder/'report.json',report);print('REPORT',folder/'report.json',report.get('result'),flush=True)

if __name__=='__main__':run()
