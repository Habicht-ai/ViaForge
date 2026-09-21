"""Real local 1.12.2 connections: negative controls, actual chat, reconnect and persisted switch.

This protocol harness is deliberately NOT a native-client or ViaForge movement comparison.
It acknowledges server transactions and sends a known-invalid duplicate sprint sequence.
"""
import io
import json
import socket
import struct
import threading
import time
import zlib

import lab
import grim
from login_probe import read_vi


class Client:
    def __init__(self, row, name):
        self.row, self.name = row, name
        self.sock = socket.create_connection(("127.0.0.1", row["port"]), timeout=20)
        self.compressed = False
        self.play = False
        self.closed = False
        self.entity = 0
        self.position = None
        self.messages = []
        self.teleports = []
        self.errors = []
        self.lock = threading.Lock()
        self.send(0, lab.varint(340) + b"\x09localhost" + struct.pack(">H", row["port"]) + b"\x02")
        self.send(0, lab.varint(len(name)) + name.encode("ascii"))
        self.reader = threading.Thread(target=self.read, daemon=True)
        self.reader.start()

    def send(self, packet, payload=b""):
        body = lab.varint(packet) + payload
        if self.compressed:
            body = b"\0" + body
        with self.lock:
            self.sock.sendall(lab.varint(len(body)) + body)

    def read(self):
        try:
            while not self.closed:
                length = lab.read_varint(self.sock)
                source = io.BytesIO(lab.read_exact(self.sock, length))
                if self.compressed:
                    expanded = read_vi(source)
                    payload = source.read()
                    source = io.BytesIO(zlib.decompress(payload) if expanded else payload)
                packet = read_vi(source)
                data = source.read()
                if not self.play:
                    if packet == 3: self.compressed = True
                    elif packet == 2:
                        self.play = True
                        self.send(4, b"\x05en_US\x10\x00\x01\x7f\x01")
                    elif packet == 0: raise RuntimeError("Login disconnect: " + repr(data))
                elif packet == 0x23:
                    self.entity = struct.unpack(">i", data[:4])[0]
                elif packet == 0x1f:
                    self.send(0x0b, data)
                elif packet == 0x11:
                    self.send(0x05, data[:3] + b"\x01")
                elif packet == 0x2f:
                    x, y, z, yaw, pitch, flags = struct.unpack(">dddffB", data[:33])
                    old = self.position or (0,0,0,0,0)
                    p = [x,y,z,yaw,pitch]
                    for i in range(5):
                        if flags & (1 << i): p[i] += old[i]
                    self.position = p
                    self.teleports.append(dict(time=time.time(), position=p))
                    self.send(0, lab.varint(read_vi(io.BytesIO(data[33:]))))
                    self.move()
                elif packet == 0x0f:
                    stream = io.BytesIO(data)
                    self.messages.append(dict(time=time.time(), json=stream.read(read_vi(stream)).decode("utf-8")))
                elif packet == 0x1a:
                    raise RuntimeError("Play disconnect: " + repr(data))
        except Exception as error:
            if not self.closed: self.errors.append(str(error))

    def move(self):
        if self.position:
            self.send(0x0d, struct.pack(">dddB", *self.position[:3], 1))

    def invalid_sprint(self):
        # BadPacketsF skips its initial ambiguous state, then catches the third duplicate.
        for _ in range(4):
            self.send(0x15, lab.varint(self.entity) + b"\x03\x00")
            time.sleep(.06)

    def close(self):
        self.closed = True
        try: self.sock.shutdown(socket.SHUT_RDWR)
        except OSError: pass
        self.sock.close()
        self.reader.join(timeout=3)


def wait_for(predicate, message, seconds=20):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        value = predicate()
        if value: return value
        time.sleep(.15)
    raise AssertionError(message)


def actual(row):
    return grim.state(row, running=True)


def exercise(row, names, report):
    clients = [Client(row, name) for name in names]
    try:
        wait_for(lambda: all(c.position for c in clients), "Both accounts must log in")
        wait_for(lambda: len(actual(row).get("players", [])) == 2 and all(p.get("verbose") for p in actual(row)["players"]),
                 "Both non-OP accounts need actual Grim verbose")
        for client in clients:
            lab.batch(row, ["gamemode 0 " + client.name, "tp " + client.name + " -18 64 -83"])
        time.sleep(1)
        for _ in range(45):
            for c in clients: c.move()
            time.sleep(.05)
        on = actual(row)
        assert all(not p["op"] and p["protocol"] == 340 and not p["disabled"] for p in on["players"]), on
        wait_for(lambda: all(p.get("predictions", 0) > 0 for p in actual(row)["players"]), "Survival movement predictions required")
        before = [len(c.messages) for c in clients]
        clients[0].invalid_sprint()
        wait_for(lambda: all(any("BadPacketsF" in m["json"] for m in c.messages[start:]) for c,start in zip(clients,before)),
                 "Both clients must RECEIVE genuine verbose below the ordinary alert threshold")
        report.append(dict(case="on_negative_control", actual=actual(row), chat=[c.messages[start:] for c,start in zip(clients,before)]))
        grim.control(row, "off")
        time.sleep(1.2)
        off = actual(row)
        before_flags = sum(p["flags"] for p in off["players"])
        before_predictions = sum(p["predictions"] for p in off["players"])
        for c in clients: c.invalid_sprint()
        for _ in range(25):
            for c in clients: c.move()
            time.sleep(.05)
        time.sleep(1.2)
        off_after = actual(row)
        assert sum(p["flags"] for p in off_after["players"]) == before_flags, off_after
        assert sum(p["predictions"] for p in off_after["players"]) == before_predictions, off_after
        report.append(dict(case="off_same_packets", before=off, after=off_after))
        # Same connected players, no reload/reconnect.
        grim.control(row, "on")
        before = [len(c.messages) for c in clients]
        clients[1].invalid_sprint()
        wait_for(lambda: all(any("BadPacketsF" in m["json"] for m in c.messages[start:]) for c,start in zip(clients,before)),
                 "ON must resume checks and verbose for already connected accounts")
        report.append(dict(case="on_again", actual=actual(row), chat=[c.messages[start:] for c,start in zip(clients,before)]))
        assert not any(c.errors for c in clients), [c.errors for c in clients]
    finally:
        for c in clients: c.close()


def probe(row):
    if row["protocol"] != 340 or not row.get("grim_version"):
        raise ValueError("This negative control requires the exact 1.12.2 Grim variant")
    status = lab.status(row)
    if status and status["players"]["online"]:
        raise RuntimeError("Isolated probe requires an empty variant")
    if not status: lab.start([row])
    names = ["GLabA" + str(time.time_ns())[-8:], "GLabB" + str(time.time_ns())[-8:]]
    results = []
    path = lab.ROOT / row["version"] / ("grim-controls-" + str(time.time_ns()) + ".json")
    try:
        grim.control(row, "on")
        exercise(row, names, results)
        wait_for(lambda: not actual(row).get("players"), "Quit cleanup")
        grim.control(row, "off")
        lab.stop([row]); lab.start([row])
        wait_for(lambda: actual(row).get("state") == "disabled", "OFF must persist across restart")
        client = Client(row, names[0])
        try:
            wait_for(lambda: len(actual(row).get("players", [])) == 1 and actual(row)["players"][0].get("verbose"), "Reconnect verbose")
            p = actual(row)["players"][0]
            assert p["disabled"] and not p["op"], p
            results.append(dict(case="restart_off_reconnect", actual=actual(row)))
            client.invalid_sprint();time.sleep(1.3)
            assert actual(row)["players"][0]["flags"] == 0
            # Non-OP in-game command, independent of RCON.
            text = b"/labac on";client.send(2, lab.varint(len(text)) + text)
            wait_for(lambda: actual(row).get("state") == "enabled", "Non-OP command ON")
            client.invalid_sprint()
            wait_for(lambda: any("BadPacketsF" in m["json"] for m in client.messages), "Reconnect checks active")
            results.append(dict(case="nonop_command_on", actual=actual(row), chat=client.messages))
        finally: client.close()
        wait_for(lambda: not actual(row).get("players"), "Quit cleanup")
        lab.stop([row]);lab.start([row])
        wait_for(lambda: actual(row).get("state") == "enabled", "ON must persist")
        results.append(dict(case="restart_on", actual=actual(row)))
        lab.save(path, dict(success=True, time=time.time(), server=row, accounts=names, client="synthetic 1.12.2 protocol harness",
                            cases=results, limits="Negative/control test, not native or ViaForge movement compatibility"))
        print("GRIM CONTROL PASS", path, flush=True)
    except Exception as error:
        lab.save(path, dict(success=False, time=time.time(), accounts=names, cases=results, error=str(error)))
        raise


if __name__ == "__main__":
    probe(lab.select("1.12.2-grim")[0])
