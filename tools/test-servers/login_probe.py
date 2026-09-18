"""Small real offline-login probes for the regression endpoints, not a rendering test."""
import io
import json
import re
import socket
import struct
import sys
import time
import uuid
import zlib
import lab


def read_vi(source):
    value = 0
    for i in range(5):
        data = source.read(1)
        if not data:
            raise ValueError("Kurzes VarInt")
        value |= (data[0] & 127) << (7 * i)
        if data[0] < 128:
            return value
    raise ValueError("Zu langes VarInt")


def probe(row):
    modern = row["protocol"] == 776
    if row["protocol"] not in (340, 776):
        raise ValueError("Dieser Login-Probe ist fuer 1.12.2 und 26.2 implementiert")
    report = json.loads((lab.ROOT / row["version"] / "data/reports/packets.json").read_text()) if modern else None
    name = "VLab" + str(time.time_ns())[-10:]
    phase, compressed = "login", False
    with socket.create_connection(("127.0.0.1", row["port"]), timeout=20) as sock:
        def send(payload):
            if compressed:
                # The server accepts an uncompressed packet below its negotiated threshold (256).
                assert len(payload) < 256
                payload = b"\0" + payload
            sock.sendall(lab.varint(len(payload)) + payload)
        send(b"\0" + lab.varint(row["protocol"]) + b"\x09localhost" + struct.pack(">H", row["port"]) + b"\x02")
        send(b"\0" + lab.varint(len(name)) + name.encode("ascii") + (uuid.uuid4().bytes if modern else b""))
        for _ in range(2000):
            size = lab.read_varint(sock)
            if not 0 < size < 8 * 1024 * 1024:
                raise RuntimeError("Ungueltige Paketlaenge")
            source = io.BytesIO(lab.read_exact(sock, size))
            if compressed:
                length = read_vi(source)
                payload = source.read()
                source = io.BytesIO(zlib.decompress(payload) if length else payload)
            packet = read_vi(source)
            if phase == "login":
                if packet == 3:
                    compressed = True
                elif packet == 2:
                    phase = "configuration" if modern else "play"
                    if modern:
                        send(b"\x03")
                elif packet == 0:
                    raise RuntimeError("Login abgelehnt: " + repr(source.read()))
                continue
            if phase == "configuration":
                incoming = report[phase]["clientbound"]
                outgoing = report[phase]["serverbound"]
                def is_type(key):
                    return packet == incoming["minecraft:" + key]["protocol_id"]
                def reply(key, data=b""):
                    send(lab.varint(outgoing["minecraft:" + key]["protocol_id"]) + data)
                if is_type("select_known_packs"):
                    reply("select_known_packs", b"\0")
                elif is_type("finish_configuration"):
                    reply("finish_configuration")
                    phase = "play"
                elif is_type("keep_alive"):
                    reply("keep_alive", source.read())
                elif is_type("ping"):
                    reply("pong", source.read())
                elif is_type("disconnect"):
                    raise RuntimeError("Konfiguration abgelehnt: " + repr(source.read()))
                continue
            position_packet = report["play"]["clientbound"]["minecraft:player_position"]["protocol_id"] if modern else 0x2F
            if packet == position_packet:
                if modern:
                    with lab.Rcon(row) as remote:
                        response = remote.command("data get entity " + name + " Pos")
                    match = re.search(r"\[(-?[\d.]+)d, (-?[\d.]+)d, (-?[\d.]+)d\]", response)
                    if not match:
                        raise RuntimeError("Position nicht lesbar: " + response)
                    position = [float(n) for n in match.groups()]
                else:
                    position = list(struct.unpack(">ddd", source.read(24)))
                if position != [.5, 177., .5]:
                    raise RuntimeError("Falscher erster Ankunftspunkt: " + repr(position))
                result = {"success": True, "protocol": row["protocol"], "spawn": position,
                          "real_offline_login": True, "rendering_test": False, "time": time.time()}
                lab.save(lab.ROOT / row["version"] / "login-probe.json", result)
                print("LOGIN PASS", row["version"], position)
                return
        raise RuntimeError("Kein Spieler-Positionspaket empfangen")


if __name__ == "__main__":
    for row in lab.select(sys.argv[1] if len(sys.argv) > 1 else "1.12.2,26.2"):
        probe(row)
