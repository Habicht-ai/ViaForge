"""Configuration prompt and modern pack priority on a real 26.2 original/Forge client."""
import argparse, hashlib, http.server, json, threading, time, uuid
from pathlib import Path
import lab, boat_probe
from session_visual_probe import Server, Client, plugin, read, wait
from session_visual_cases import NativeClient
from session_pack_cases import pack

def run(implementation,decline):
    boat_probe.assert_no_client()
    folder=lab.ROOT/('session-config-'+implementation+'-'+str(time.time_ns()));folder.mkdir();print('RUN',folder,flush=True)
    report=dict(implementation=implementation,decline=decline,checks=[]);server=client=httpd=None
    payloads={'/magenta':pack((220,30,180),'magenta',True),'/green':pack((30,220,80),'green',True),'/invalid':b'not a zip'};requests=[]
    class Handler(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            requests.append(dict(path=self.path,time=time.time(),pack_format=self.headers.get('X-Minecraft-Pack-Format')))
            if self.path=='/error':self.send_error(503);return
            body=payloads[self.path];self.send_response(200);self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body)
        def log_message(self,*args):pass
    try:
        fixture=plugin();httpd=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);threading.Thread(target=httpd.serve_forever,daemon=True).start()
        server=Server(folder/'backend',fixture)
        initial=str(uuid.uuid4());base=f'http://127.0.0.1:{httpd.server_port}'
        with (server.folder/'server.properties').open('a') as p:p.write(f'resource-pack={base}/magenta\nresource-pack-sha1={hashlib.sha1(payloads["/magenta"]).hexdigest()}\nresource-pack-id={initial}\nrequire-resource-pack=false\n')
        server.start();name=('Np' if implementation=='original' else 'Vp')+str(time.time_ns())[-8:]
        if implementation=='original':
            client=NativeClient(folder/'client',server.port,name);client.start(wait_join=False)
        else:
            client=Client(folder/'client',lab.REPO/'build/development/ViaForge-1.8.9-4.4.0-client.1-dev-java8.jar');client.start();client.send('connect',name=name,port=server.port,protocol=776)
        def state():return read(client.folder/('ui-state.json' if implementation=='original' else 'state.json'))
        def check(label,value,**data):
            report['checks'].append(dict(label=label,pass_=bool(value),**data));print('CHECK',label,value,flush=True)
            if not value:raise AssertionError(label)
        wait(lambda:state().get('screen') in ('ConfirmScreen','PackConfirmScreen','GuiYesNo'),120,'configuration prompt')
        check('prompt before download',not requests,state=state());client.send('button',button=1 if decline else 0)
        wait(client.ready,120,'configuration completed and world loaded')
        if decline:
            check('decline no HTTP',not requests);report['result']='PASS';return
        def pixel(label):
            location='minecraft:textures/'+('block' if implementation=='original' else 'blocks')+'/stone.png'
            client.send('resource',path=location);time.sleep(.4)
            rows=[json.loads(l) for l in (client.folder/('packet-order.jsonl' if implementation=='original' else 'observations.jsonl')).read_text().splitlines()]
            value=[r['argb'] for r in rows if r.get('resource')==location][-1]&0xffffffff
            if implementation=='original':client.photo('idle',label)
            else:client.send('photo',name=label)
            return value
        def status(id,value):return f'{id} {value}' in (server.folder/'console.log').read_text(errors='replace')
        def push(path):
            id=str(uuid.uuid4());server.command(f'sessionlab push {name} {id} {base}{path} {hashlib.sha1(payloads.get(path,b"")).hexdigest()}');return id
        server.command(f'tp {name} 0.5 64 0.5 0 80');time.sleep(2)
        check('configuration texture',pixel('config-magenta')==0xffdc1eb4)
        check('format header',requests[0]['pack_format']=='88.0',request=requests[0])
        green=push('/green');wait(lambda:status(green,'SUCCESSFULLY_LOADED'),60,'second modern pack loaded')
        check('last push wins',pixel('green-priority')==0xff1edc50)
        def confirmations():return (server.folder/'console.log').read_text(errors='replace').count(initial+' SUCCESSFULLY_LOADED')
        before_pop=confirmations()
        server.command(f'sessionlab pop {name} {green}');time.sleep(4)
        check('pop restores configuration pack',pixel('pop-green')==0xffdc1eb4)
        check('reload acknowledges retained pack again',confirmations()>before_pop)
        invalid=push('/invalid');wait(lambda:status(invalid,'FAILED_RELOAD'),60,'invalid zip reload status')
        check('invalid keeps loaded pack',pixel('invalid')==0xffdc1eb4)
        before_http=confirmations()
        failed=push('/error');wait(lambda:status(failed,'FAILED_DOWNLOAD'),40,'HTTP status')
        check('failed HTTP keeps world usable',client.ready());time.sleep(1)
        check('HTTP failure does not reload unchanged pack',confirmations()==before_http);report['result']='PASS'
    except Exception as error:report['result']='FAIL';report['error']=repr(error);raise
    finally:
        if client:client.stop()
        if server:server.stop()
        if httpd:httpd.shutdown();httpd.server_close()
        report['http_requests']=requests;lab.save(folder/'report.json',report);print('REPORT',folder/'report.json',report.get('result'),flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('implementation',choices=['original','fixed']);parser.add_argument('--decline',action='store_true');args=parser.parse_args();run(args.implementation,args.decline)
