"""Native 1.8.9 cache path and original C19 statuses, using an actual legacy server."""
import hashlib,http.server,json,threading,time
import lab,boat_probe
from session_visual_probe import Client,wait
from session_visual_cases import OldServer
from session_pack_cases import pack

def run():
    boat_probe.assert_no_client();folder=lab.ROOT/('session-pack-legacy-'+str(time.time_ns()));folder.mkdir();print('RUN',folder,flush=True)
    server=client=httpd=None;data=pack((220,30,180),'legacy',native_model=True);requests=[];report=dict(cases=[])
    class Handler(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            requests.append(self.path);self.send_response(200);self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
        def log_message(self,*args):pass
    try:
        httpd=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);threading.Thread(target=httpd.serve_forever,daemon=True).start();server=OldServer(folder/'backend')
        with (server.folder/'server.properties').open('a') as p:p.write(f'resource-pack=http://127.0.0.1:{httpd.server_port}/pack.zip\nresource-pack-hash={hashlib.sha1(data).hexdigest()}\n')
        server.start();client=Client(folder/'client',lab.REPO/'build/development/ViaForge-1.8.9-4.4.0-client.1-dev-java8.jar');client.start()
        def rows():return [json.loads(l) for l in (client.folder/'observations.jsonl').read_text().splitlines()]
        for index,decline in enumerate([True,False,False]):
            client.send('connect',name='Vl'+str(time.time_ns())[-8:],port=server.port,protocol=47)
            wait(lambda:client.state().get('screen')=='GuiYesNo',60,'legacy prompt');before=len(requests)
            if index==1:assert not (client.folder/'server-resource-packs').exists()
            client.send('button',button=1 if decline else 0);wait(client.ready,40,'legacy ready');time.sleep(3)
            statuses=[r for r in rows() if r.get('packet')=='C19PacketResourcePackStatus']
            if decline:assert len(requests)==before
            else:
                client.send('resource',path='minecraft:textures/blocks/stone.png');time.sleep(.3)
                pixel=[r['argb'] for r in rows() if r.get('resource')][-1]&0xffffffff;assert pixel==0xffdc1eb4
                client.send('model');time.sleep(.3);model=[r for r in rows() if r.get('model')=='stone'][-1]
                assert model['max_y']==.5 and model['min_y']==0 and model['quads']==6,model
                if index==2:assert len(requests)==before
                server.command(f'tp {client.state()["player"]} 0.5 4 0.5 0 80');client.send('photo',name=f'legacy-pack-{index}')
            report['cases'].append(dict(decline=decline,requests=len(requests)-before,statuses=statuses,state=client.state()));print('CASE',index,'observed',flush=True)
            client.send('disconnect');wait(lambda:client.state().get('screen')=='GuiMainMenu',20,'disconnect');time.sleep(2)
        actions=[str(r.get('status',r.get('field_179719_b'))) for r in rows() if r.get('packet')=='C19PacketResourcePackStatus']
        assert 'DECLINED' in actions and actions.count('ACCEPTED')==2 and actions.count('SUCCESSFULLY_LOADED')==2,actions
        report['result']='PASS'
    except Exception as error:report['result']='FAIL';report['error']=repr(error);raise
    finally:
        if client:client.stop()
        if server:server.stop()
        if httpd:httpd.shutdown();httpd.server_close()
        lab.save(folder/'report.json',report);print('REPORT',folder/'report.json',report.get('result'),flush=True)

if __name__=='__main__':run()
