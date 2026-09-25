import pathlib
import socket
import tempfile
import threading
import unittest
from metadata_relay import Relay, varint


class MetadataRelayTest(unittest.TestCase):
    def test_partial_varints_wait_and_oversized_headers_fail(self):
        self.assertIsNone(varint(b'\x80'))
        self.assertEqual((128,2),varint(b'\x80\x01'))
        with self.assertRaises(ValueError):varint(b'\x80'*5)

    def test_fragmented_stream_is_byte_identical_and_only_one_clear_is_delayed(self):
        with tempfile.TemporaryDirectory() as temp, socket.socket() as server:
            folder=pathlib.Path(temp);(folder/'relay-arm').write_text('armed')
            server.bind(('127.0.0.1',0));server.listen(1);server.settimeout(3)
            # Two gliding -> standing transitions. All bytes must be forwarded.
            stream=(b'\x06\x63\x01\x00\x00\x80\xff'+b'\x06\x63\x01\x00\x00\x00\xff')*2
            upload=[];errors=[]
            def send():
                try:
                    with server.accept()[0] as s:
                        s.settimeout(3);request=bytearray()
                        while len(request)<3:request.extend(s.recv(3-len(request)))
                        upload.append(bytes(request))
                        for b in stream:s.sendall(bytes([b]))
                        s.shutdown(socket.SHUT_WR)
                        while s.recv(100):pass
                except Exception as error:errors.append(error)
            worker=threading.Thread(target=send);worker.start()
            relay=Relay(folder,server.getsockname()[1],30)
            try:
                with socket.create_connection(('127.0.0.1',relay.port),timeout=3) as client:
                    client.sendall(b'abc');received=bytearray()
                    while data:=client.recv(100):received.extend(data)
                    self.assertEqual(stream,received)
                worker.join(3);self.assertFalse(worker.is_alive());self.assertFalse(errors)
                relay.thread.join(3)
            finally:result=relay.close()
            self.assertEqual([b'abc'],upload);self.assertEqual(1,len(result['delays']))
            self.assertGreaterEqual(result['delays'][0]['sent_ms']-result['delays'][0]['start_ms'],25)
            self.assertFalse(result['errors'])
            for s in result['streams'].values():
                self.assertEqual(s['received_sha256'],s['sent_sha256']);self.assertEqual(s['received_bytes'],s['sent_bytes'])
