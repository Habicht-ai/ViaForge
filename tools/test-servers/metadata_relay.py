"""Diagnostic loopback TCP relay: preserve bytes/order, delay one flight-end frame.

Requires an isolated offline server with network compression disabled. This is
an explicit adverse-network original-client comparison, never a release fix or
a normal PASS run. No packet is created, edited, discarded or acknowledged.
"""
import hashlib
import json
import socket
import threading
import time


def varint(data, start=0):
    value=0
    for i in range(5):
        if start+i>=len(data):return None
        b=data[start+i];value|=(b&127)<<(7*i)
        if b<128:return value,start+i+1
    raise ValueError('Oversized VarInt')


class Relay:
    def __init__(self, folder, server_port, milliseconds):
        self.folder=folder;self.server_port=server_port;self.delay=milliseconds/1000
        self.listener=socket.socket();self.listener.bind(('127.0.0.1',0));self.listener.listen(1)
        self.listener.settimeout(.5);self.port=self.listener.getsockname()[1]
        self.closed=threading.Event();self.sockets=[];self.errors=[];self.stats={};self.delays=[]
        self.thread=threading.Thread(target=self.run,daemon=True);self.thread.start()

    def run(self):
        try:
            while not self.closed.is_set():
                try:client,_=self.listener.accept();break
                except socket.timeout:continue
            else:return
            server=socket.create_connection(('127.0.0.1',self.server_port))
            self.sockets=[client,server]
            for s in self.sockets:s.settimeout(.5);s.setsockopt(socket.IPPROTO_TCP,socket.TCP_NODELAY,1)
            upload=threading.Thread(target=self.copy,args=(client,server,'serverbound',False),daemon=True);upload.start()
            self.copy(server,client,'clientbound',True);upload.join(3)
        except OSError as e:
            if not self.closed.is_set():self.errors.append(repr(e))

    def copy(self, source, destination, direction, framed):
        received=hashlib.sha256();sent=hashlib.sha256();n_in=n_out=0;pending=bytearray();flags={}
        try:
            while not self.closed.is_set():
                try:data=source.recv(65536)
                except socket.timeout:continue
                if not data:break
                received.update(data);n_in+=len(data)
                if not framed:
                    destination.sendall(data);sent.update(data);n_out+=len(data);continue
                pending.extend(data)
                while (header:=varint(pending)) is not None:
                    length,offset=header
                    if length>8*1024*1024:raise ValueError('Unexpected frame length')
                    if len(pending)<offset+length:break
                    frame=bytes(pending[:offset+length]);payload=frame[offset:];del pending[:offset+length]
                    packet=varint(payload)
                    if packet and packet[0]==0x63 and (entity:=varint(payload,packet[1])):
                        entity_id,start=entity
                        if len(payload)>=start+3 and payload[start:start+2]==b'\0\0':
                            value=payload[start+2];before=flags.get(entity_id,0);flags[entity_id]=value
                            if before&128 and not value&128 and not self.delays and (self.folder/'relay-arm').exists():
                                record=dict(start_ms=time.time_ns()/1e6,entity=entity_id,flags=value,bytes=len(frame),sha256=hashlib.sha256(frame).hexdigest(),requested_ms=self.delay*1000)
                                time.sleep(self.delay)
                                record['sent_ms']=time.time_ns()/1e6;self.delays.append(record)
                    destination.sendall(frame);sent.update(frame);n_out+=len(frame)
        except (OSError,ValueError) as e:
            if not self.closed.is_set():self.errors.append(direction+': '+repr(e))
        finally:
            self.stats[direction]=dict(received_bytes=n_in,sent_bytes=n_out,received_sha256=received.hexdigest(),sent_sha256=sent.hexdigest(),unforwarded_bytes=len(pending))
            try:destination.shutdown(socket.SHUT_WR)
            except OSError:pass

    def close(self):
        self.closed.set();self.listener.close()
        for s in self.sockets:
            try:s.shutdown(socket.SHUT_RDWR);s.close()
            except OSError:pass
        self.thread.join(5)
        result=dict(diagnostic_only=True,bind='127.0.0.1',port=self.port,server_port=self.server_port,compression=False,delays=self.delays,streams=self.stats,errors=self.errors)
        (self.folder/'network-relay.json').write_text(json.dumps(result,indent=2)+'\n')
        return result
