"""Real GuiYesNo, HTTP, reload, UUID/status, pop, switch and reconnect cases."""
import hashlib,http.server,io,json,os,threading,time,uuid,zipfile,argparse
from pathlib import Path
import lab,boat_probe,grim
from session_visual_probe import Server,Proxy,Client,plugin,wait,read,png

def pack(rgb,label,modern=False,native_model=False):
    from PIL import Image
    texture=io.BytesIO();Image.new('RGBA',(16,16),(*rgb,255)).save(texture,format='PNG')
    data=io.BytesIO()
    with zipfile.ZipFile(data,'w',zipfile.ZIP_DEFLATED) as z:
        z.writestr('pack.mcmeta',json.dumps({'pack':dict(description=label,**({'min_format':88,'max_format':88} if modern else {'pack_format':1}))}))
        for path in ['textures/blocks/stone.png','textures/block/stone.png']:
            z.writestr('assets/minecraft/'+path,texture.getvalue())
        if native_model:
            model={'textures':{'all':'blocks/stone','particle':'blocks/stone'},'elements':[{'from':[0,0,0],'to':[16,8,16],'faces':{side:{'texture':'#all'} for side in ['up','down','east','west','north','south']}}]}
            z.writestr('assets/minecraft/models/block/stone.json',json.dumps(model))
    return data.getvalue()

def run():
    boat_probe.assert_no_client();folder=lab.ROOT/('session-pack-cases-'+str(time.time_ns()));folder.mkdir();print('RUN',folder,flush=True)
    report=dict(started=time.time(),checks=[]);servers=[];proxy=client=None;http=None
    payloads={'/magenta':pack((220,30,180),'magenta'),'/green':pack((30,220,80),'green'),'/invalid':b'not a resource zip','/slow':pack((30,220,80),'delayed-green')}
    release=threading.Event();started=threading.Event();requests=[]
    class Handler(__import__("http.server",fromlist=["BaseHTTPRequestHandler"]).BaseHTTPRequestHandler):
        def do_GET(self):
            requests.append(dict(path=self.path,time=time.time(),version=self.headers.get('X-Minecraft-Version'),pack_format=self.headers.get('X-Minecraft-Pack-Format')))
            if self.path=='/error':self.send_error(503);return
            body=payloads[self.path]
            self.send_response(200);self.send_header('Content-Length',str(len(body)));self.end_headers()
            if self.path=='/slow':started.set();release.wait(45)
            try:self.wfile.write(body)
            except (BrokenPipeError,ConnectionResetError,ConnectionAbortedError):pass
        def log_message(self,*args):pass
    # Avoid shadowing http.server in the request handler.
    import http.server as http_module
    try:
        fixture=plugin()
        for name in ['lobby','gungame']:
            server=Server(folder/name,fixture);servers.append(server);server.start()
        proxy=Proxy(folder/'proxy',*servers);proxy.start()
        http=http_module.ThreadingHTTPServer(('127.0.0.1',0),Handler);threading.Thread(target=http.serve_forever,daemon=True).start()
        client=Client(folder/'client',lab.REPO/'build/development/ViaForge-1.8.9-4.4.0-client.1-dev-java8.jar');client.start()
        account='VFpk'+str(time.time_ns())[-8:];client.send('connect',name=account,port=proxy.port,protocol=776);wait(client.ready,120,'join')
        source=0
        servers[0].command(f'tp {account} 0.5 64 0.5 0 80')
        def check(label,condition,**evidence):
            row=dict(label=label,pass_=bool(condition),**evidence);report['checks'].append(row);print('CHECK',label,row['pass_'],flush=True)
            if not condition:raise AssertionError(label)
        def logs():return '\n'.join((s.folder/'console.log').read_text(errors='replace') for s in servers)
        def status(id,value):return f'{id} {value}' in logs()
        def push(path,id=None,hash_=None):
            id=id or str(uuid.uuid4());data=payloads.get(path,b'')
            servers[source].command(f'sessionlab push {account} {id} http://127.0.0.1:{http.server_port}{path} {hash_ or hashlib.sha1(data).hexdigest()}');return id
        def pop(id=''):servers[source].command(f'sessionlab pop {account} {id}')
        def pixel(label):
            client.send('resource',path='minecraft:textures/blocks/stone.png');time.sleep(.3)
            rows=[json.loads(l) for l in (client.folder/'observations.jsonl').read_text().splitlines()]
            value=[r['argb'] for r in rows if r.get('resource')=='minecraft:textures/blocks/stone.png'][-1]
            client.send('photo',name=label);return value&0xffffffff
        # Consent is tested through actual screens on separate connections.
        rejected=push('/magenta');wait(lambda:client.state().get('screen')=='GuiYesNo',30,'decline prompt');client.send('button',button=1)
        wait(lambda:status(rejected,'DECLINED'),20,'declined status');check('decline avoids HTTP',len(requests)==0)
        client.send('disconnect');wait(lambda:client.state().get('screen')=='GuiMainMenu',20,'disconnect')
        time.sleep(3.1);client.send('connect',name=account,port=proxy.port,protocol=776);wait(client.ready,120,'reconnect')
        magenta=push('/magenta');wait(lambda:client.state().get('screen')=='GuiYesNo',30,'accept prompt')
        check('fresh cache absent',not (client.folder/'server-resource-packs').exists());client.send('button',button=0)
        wait(lambda:status(magenta,'SUCCESSFULLY_LOADED'),45,'loaded');check('fresh cache texture',pixel('magenta')==0xffdc1eb4)
        check('selected resource format header',requests[-1]['pack_format']=='88.0' and requests[-1]['version']=='26.2')
        check('downloaded before loaded',logs().index(magenta+' DOWNLOADED')<logs().index(magenta+' SUCCESSFULLY_LOADED'))
        green=push('/green');wait(lambda:status(green,'SUCCESSFULLY_LOADED'),45,'second pack');check('latest pack priority',pixel('green-priority')==0xff1edc50)
        pop(green);time.sleep(3);check('pop restores lower pack',pixel('pop-green')==0xffdc1eb4)
        before=len(requests);again=push('/magenta');wait(lambda:status(again,'SUCCESSFULLY_LOADED'),30,'cached reload');check('verified cache reuse',len(requests)==before)
        invalid=push('/invalid');wait(lambda:status(invalid,'FAILED_RELOAD'),30,'invalid zip rejected');check('invalid keeps previous texture',pixel('invalid-pack')==0xffdc1eb4)
        failed=push('/error');wait(lambda:status(failed,'FAILED_DOWNLOAD'),30,'HTTP failure');check('HTTP failure stays usable',client.ready())
        badhash=push('/green',hash_='0'*40);wait(lambda:status(badhash,'FAILED_DOWNLOAD'),30,'hash failure');check('hash failure stays usable',client.ready())
        slow=push('/slow');wait(started.is_set,20,'download started')
        previous=client.state()['world'];client.send('chat',text='/server gungame');source=1
        wait(lambda:client.state().get('world')!=previous,30,'world switched during download');wait(client.ready,30,'terrain ready during download')
        release.set();time.sleep(4);check('switch with pending download',client.ready(),state=client.state())
        # Switching alone is not a pop. Capture this behavior for original comparison.
        report['switch_texture']=pixel('after-switch')
        check('switch texture agrees with original 26.2',report['switch_texture']==0xff1edc50)
        current=push('/green');wait(lambda:status(current,'SUCCESSFULLY_LOADED'),40,'new backend pack');check('new backend textures',pixel('new-backend')==0xff1edc50)
        release.clear();started.clear();payloads['/slow']=pack((220,30,180),'popped delayed magenta');popped=push('/slow');wait(started.is_set,20,'pending pack to pop')
        pop(popped);time.sleep(1);release.set();time.sleep(3)
        check('explicit pop rejects pending callback',pixel('after-explicit-pop')==0xff1edc50)
        release.clear();started.clear();payloads['/slow']=pack((30,220,80),'second delayed green');slow2=push('/slow');wait(started.is_set,20,'second pending download')
        client.send('disconnect');wait(lambda:client.state().get('screen')=='GuiMainMenu',20,'disconnect pending');release.set();time.sleep(3.1)
        client.send('connect',name=account,port=proxy.port,protocol=776);source=0;wait(client.ready,120,'reconnect after pending')
        check('reconnect rejects stale callback',client.state()['server_pack']=='null')
        report['result']='PASS'
    except Exception as error:report['error']=repr(error);report['result']='FAIL';raise
    finally:
        release.set()
        if client:client.stop()
        if proxy:proxy.stop()
        for server in reversed(servers):server.stop()
        if http:http.shutdown();http.server_close()
        report['http_requests']=requests;report['finished']=time.time();lab.save(folder/'report.json',report);print('REPORT',folder/'report.json',report.get('result'),flush=True)

if __name__=='__main__':run()
