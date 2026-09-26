"""Observe actual original pack push/pop behavior across the local Velocity route."""
import hashlib,http.server,json,threading,time,uuid
import lab,boat_probe
from session_visual_probe import Server,Proxy,plugin,wait,read
from session_visual_cases import NativeClient
from session_pack_cases import pack

def run():
    boat_probe.assert_no_client();folder=lab.ROOT/('session-proxy-original-'+str(time.time_ns()));folder.mkdir();print('RUN',folder,flush=True)
    servers=[];proxy=client=httpd=None;report=dict(checks=[]);release=threading.Event();started=threading.Event()
    payloads={'/first':pack((220,30,180),'magenta',True),'/slow':pack((30,220,80),'slow green',True)}
    class Handler(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            body=payloads[self.path];self.send_response(200);self.send_header('Content-Length',str(len(body)));self.end_headers()
            if self.path=='/slow':started.set();release.wait(45)
            try:self.wfile.write(body)
            except (BrokenPipeError,ConnectionResetError,ConnectionAbortedError):pass
        def log_message(self,*args):pass
    try:
        fixture=plugin()
        for name in ['lobby','gungame']:
            server=Server(folder/name,fixture);servers.append(server);server.start()
        proxy=Proxy(folder/'proxy',*servers);proxy.start();httpd=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);threading.Thread(target=httpd.serve_forever,daemon=True).start()
        name='Np'+str(time.time_ns())[-8:];client=NativeClient(folder/'client',proxy.port,name);client.start();source=0
        def push(path):
            id=str(uuid.uuid4());servers[source].command(f'sessionlab push {name} {id} http://127.0.0.1:{httpd.server_port}{path} {hashlib.sha1(payloads[path]).hexdigest()}');return id
        def status(id,value):return any(f'{id} {value}' in (s.folder/'console.log').read_text(errors='replace') for s in servers)
        def check(label,value):
            report['checks'].append(dict(label=label,pass_=bool(value)));print('CHECK',label,value,flush=True)
            if not value:raise AssertionError(label)
        def pixel(label):
            client.send('resource',path='minecraft:textures/block/stone.png');time.sleep(.3);client.photo('idle',label)
            rows=[json.loads(l) for l in (client.folder/'packet-order.jsonl').read_text().splitlines()]
            return [r['argb'] for r in rows if r.get('resource')][-1]&0xffffffff
        initial=push('/first');wait(lambda:read(client.folder/'ui-state.json').get('screen')in ('ConfirmScreen','PackConfirmScreen'),30,'prompt');client.send('button',button=0)
        wait(lambda:status(initial,'SUCCESSFULLY_LOADED'),60,'first pack')
        slow=push('/slow');wait(started.is_set,20,'download start');client.send('chat',text='/server gungame');source=1
        wait(lambda:f'{name} joined the game' in (servers[1].folder/'console.log').read_text(errors='replace'),30,'backend switch');wait(client.ready,30,'terrain after switch')
        release.set();time.sleep(5);report['switch_texture']=pixel('after-switch');check('switch keeps world usable',client.ready())
        rows=[json.loads(l) for l in (client.folder/'packet-order.jsonl').read_text().splitlines()]
        report['pop_packets_before_explicit']=[r for r in rows if r.get('packet')=='ClientboundResourcePackPopPacket']
        release.clear();started.clear();payloads['/slow']=pack((220,30,180),'popped magenta',True);popped=push('/slow');wait(started.is_set,20,'pending pop')
        servers[1].command(f'sessionlab pop {name} {popped}');time.sleep(1);release.set();time.sleep(4)
        check('explicit pop prevents replacement',pixel('explicit-pop')==report['switch_texture']);report['result']='PASS'
    except Exception as error:report['result']='FAIL';report['error']=repr(error);raise
    finally:
        release.set()
        if client:client.stop()
        if proxy:proxy.stop()
        for server in reversed(servers):server.stop()
        if httpd:httpd.shutdown();httpd.server_close()
        lab.save(folder/'report.json',report);print('REPORT',folder/'report.json',report.get('result'),flush=True)

if __name__=='__main__':run()
