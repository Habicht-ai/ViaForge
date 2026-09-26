"""Saved-disabled required pack, cached reconnect and real F5 item captures."""
import argparse,hashlib,http.server,json,shutil,struct,threading,time,uuid
import lab,boat_probe,server_list
from session_visual_probe import Server,Client,plugin,wait,read,baseline_observer
from session_visual_cases import NativeClient
from session_pack_cases import pack

def server_entry(folder,port,enabled=False):
    entry=server_list.string_tag('name','Isolated session regression')+server_list.string_tag('ip',f'127.0.0.1:{port}')
    if enabled is not None:entry+=b'\x01'+server_list.utf('acceptTextures')+bytes([int(enabled)])
    entry+=b'\x00'
    (folder/'servers.dat').write_bytes(b'\x0a\x00\x00\x09'+server_list.utf('servers')+b'\x0a'+struct.pack('>i',1)+entry+b'\x00')

def accepted(folder):
    # One controlled fixture entry; inspect the actual NBT byte tag, not UI memory.
    data=(folder/'servers.dat').read_bytes();tag=b'\x01'+server_list.utf('acceptTextures')
    offset=data.index(tag)+len(tag);return data[offset]==1

def run(mode):
    boat_probe.assert_no_client();folder=lab.ROOT/('rejoin-items-'+mode+'-'+str(time.time_ns()));folder.mkdir();print('RUN',folder,flush=True)
    server=client=httpd=None;report=dict(mode=mode,checks=[],images=[]);requests=[]
    payload=pack((220,30,180),'required local pack',True)
    class Handler(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            requests.append(dict(path=self.path,time=time.time()));self.send_response(200);self.send_header('Content-Length',str(len(payload)));self.end_headers();self.wfile.write(payload)
        def log_message(self,*args):pass
    try:
        httpd=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);threading.Thread(target=httpd.serve_forever,daemon=True).start()
        server=Server(folder/'backend',plugin());id=str(uuid.uuid4())
        with (server.folder/'server.properties').open('a') as p:p.write(f'resource-pack=http://127.0.0.1:{httpd.server_port}/pack\nresource-pack-sha1={hashlib.sha1(payload).hexdigest()}\nresource-pack-id={id}\nrequire-resource-pack=true\n')
        server.start();name=('Nr' if mode=='original' else 'Vr')+str(time.time_ns())[-8:]
        def check(label,value,**evidence):
            report['checks'].append(dict(label=label,pass_=bool(value),**evidence));print('CHECK',label,bool(value),flush=True)
            assert value,label
        if mode=='original':
            client=NativeClient(folder/'client',server.port,name)
            server_entry(client.folder,server.port)
            client.start(wait_join=False)
        else:
            artifact=lab.REPO/'build/development/ViaForge-1.8.9-4.4.0-client.1-dev-java8.jar'
            if mode=='before':artifact=baseline_observer('rejoin-'+str(time.time_ns()),lab.REPO/'build/backups/rejoin-items-1790434204730097100')
            client=Client(folder/'client',artifact);server_entry(client.folder,server.port);client.start();client.send('connect',name=name,port=server.port,protocol=776,pack_mode='DISABLED')
        def screen():return read(client.folder/('ui-state.json' if mode=='original' else 'state.json')).get('screen')
        wait(lambda:screen() in ('GuiDisconnected','DisconnectedScreen','GuiYesNo','PackConfirmScreen'),100,'required pack response')
        first=screen();report['first_screen']=first
        if mode=='before':
            check('saved disabled reproduces immediate disconnect',first=='GuiDisconnected' and not requests)
            client.send('photo',name='disabled-required-disconnect');time.sleep(.5)
            client.send('connect',name=name,port=server.port,protocol=776,pack_mode='ENABLED')
        else:
            check('saved disabled required asks again',first in ('GuiYesNo','PackConfirmScreen') and not requests)
            if mode=='fixed':client.send('photo',name='disabled-required-prompt')
            client.send('button',button=0)
        wait(client.ready,120,'required pack accepted and world loaded');check('required pack loads',len(requests)==1)
        if mode!='before':check('accept persisted in server list',accepted(client.folder))
        server.command(f'tp {name} 0.5 64 0.5 0 0');time.sleep(2)
        for slot,item in enumerate(['compass','clock','diamond_sword']):
            server.command(f'item replace entity {name} hotbar.0 with {item}');time.sleep(1)
            if mode=='original':state=client.photo('f5front','item-'+item)
            else:client.send('view',camera=2);client.send('photo',name='f5-'+item);state=client.state();time.sleep(.3)
            report['images'].append(dict(item=item,state=state))
        if mode=='fixed':
            client.send('disconnect');wait(lambda:screen()=='GuiMainMenu',30,'disconnect');time.sleep(3.1)
            client.send('connect',name=name,port=server.port,protocol=776,saved=True);wait(client.ready,120,'cached required reconnect')
            check('enabled cached rejoin without prompt or HTTP',len(requests)==1,state=client.state())
            client.send('disconnect');wait(lambda:screen()=='GuiMainMenu',30,'disconnect');time.sleep(3.1)
            server_entry(client.folder,server.port,enabled=None)
            client.send('connect',name=name,port=server.port,protocol=776,saved=True);wait(lambda:screen()=='GuiYesNo',60,'required prompt again')
            client.send('button',button=1);wait(lambda:screen()=='GuiDisconnected',30,'explicit required decline')
            check('explicit required decline disconnects without download',len(requests)==1);client.send('photo',name='required-decline');time.sleep(.5)
            check('required refusal retains PROMPT preference',b'acceptTextures' not in (client.folder/'servers.dat').read_bytes())
        report['result']='PASS_REQUIRES_IMAGE_REVIEW'
    except Exception as error:report['result']='FAIL';report['error']=repr(error);raise
    finally:
        if client:client.stop()
        if server:server.stop()
        if httpd:httpd.shutdown();httpd.server_close()
        report['http_requests']=requests;lab.save(folder/'report.json',report);print('REPORT',folder/'report.json',report.get('result'),flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('mode',choices=['before','original','fixed']);run(p.parse_args().mode)
