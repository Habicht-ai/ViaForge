"""Loopback-only browser UI for the existing Vanilla test-server manager."""
from __future__ import annotations

import argparse
import concurrent.futures
import json
import os
from pathlib import Path
import queue
import re
import secrets
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit

import lab

PORT = 8765
APP_ID = "viaforge-testlabor-web-v1"
ASSETS = Path(__file__).with_name("web")
ROWS = {r["version"]: r for r in lab.VERSIONS}
ACTIONS = {"start", "stop", "verify", "inspect", "kit", "op"}
TITLES = {"start": "Starten", "stop": "Speichern und stoppen", "verify": "Welt prüfen",
          "kit": "Testpaket geben", "op": "Operator vergeben", "inspect": "Gespeicherte Exponate prüfen"}


def read_json(path, default=None):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return default


def tail(path, limit=24000):
    try:
        with path.open("rb") as source:
            size = source.seek(0, 2)
            source.seek(max(0, size - limit))
            data = source.read().decode("utf-8", errors="replace")
            return data.split("\n", 1)[-1] if size > limit else data
    except OSError:
        return ""


def inspect_server(row):
    folder = lab.ROOT / row["version"]
    found = lab.status(row)
    record = read_json(folder / "process.json", {})
    managed = False
    if (folder / "worker.lock").exists() and record:
        managed = all(lab.process_alive(record.get(key, 0)) for key in ("worker_pid", "server_pid"))
    protocol_ok = bool(found and found.get("version", {}).get("protocol") == row["protocol"])
    state = "running" if protocol_ok else "conflict" if found else "starting" if managed else "offline"
    report = read_json(folder / "arena-report.json", {})
    # A later failed verification must not be hidden by an older successful restart test.
    checks = [read_json(folder / name, {}) for name in ("persistence-check.json", "verification.json")]
    check = max(checks, key=lambda value: value.get("time", 0))
    audit = read_json(folder / "world-audit.json", {})
    transient = {"minecraft:flowing_water", "minecraft:flowing_lava", "minecraft:frosted_ice"}
    issues = [s for s in audit.get("missing", []) + audit.get("changed_type", []) if s["name"] not in transient
              and not (s["name"].endswith("air") and s.get("actual", {}).get("Name", "").endswith("air"))]
    return dict(version=row["version"], protocol=row["protocol"], address=f"127.0.0.1:{row['port']}",
                state=state, managed=managed, players=(found or {}).get("players", {}).get("online", 0),
                heap_mb=lab.heap(row), budget_mb=lab.heap(row) + 450,
                prepared=(folder / "server.jar").exists() and bool(report),
                catalog=(folder / "arena-index.json").exists(),
                blocks=report.get("block_types", 0), items=report.get("item_stacks", 0),
                mobs=report.get("living_types", 0), variants=report.get("legacy_mob_variants", 0),
                verified=check.get("success"), checks=check.get("passed", 0), checked_at=check.get("time"),
                exhibit_issues=len(issues) if audit else None, exhibit_checked_at=audit.get("time"),
                started=record.get("started") if managed else None)


class App:
    def __init__(self, background=True):
        self.token = secrets.token_urlsafe(32)
        self.lock = threading.RLock()
        self.wake = threading.Event()
        self.closed = threading.Event()
        self.shutdown_callback = None
        self.closing = None
        self.jobs = []
        self.pending = queue.Queue(maxsize=8)
        self.snapshot = {"servers": [], "updated_at": None, "free_mb": None, "error": None}
        if background:
            threading.Thread(target=self.poll, daemon=True).start()
            threading.Thread(target=self.work, daemon=True).start()

    def poll(self):
        with concurrent.futures.ThreadPoolExecutor(max_workers=16) as pool:
            while not self.closed.is_set():
                try:
                    servers = list(pool.map(inspect_server, lab.VERSIONS))
                    snapshot = dict(servers=servers, updated_at=time.time(), free_mb=lab.free_memory_mb(), error=None)
                    with self.lock:
                        self.snapshot = snapshot
                except Exception as error:
                    with self.lock:
                        self.snapshot["error"] = str(error)
                self.wake.wait(5)
                self.wake.clear()

    def state(self):
        with self.lock:
            jobs = [dict(j) for j in self.jobs[-16:]]
            state = dict(self.snapshot)
        for job in jobs:
            job["output"] = tail(lab.ROOT / "web-jobs" / (job["id"] + ".log"), 5000)
        state.update(app=APP_ID, token=self.token, jobs=jobs, reserve_mb=3072, profiles=len(ROWS),
                     closing=dict(self.closing) if self.closing else None)
        return state

    def request_shutdown(self, data):
        if not isinstance(data, dict) or data.get("mode") not in {"all", "web"}:
            raise ValueError("Beenden-Modus fehlt: all oder web.")
        with self.lock:
            if self.closing and self.closing["state"] != "failed":
                return dict(self.closing)
            if self.pending.full():
                raise ValueError("Die Warteschlange ist voll. Bitte kurz warten.")
            job = dict(id=secrets.token_hex(12), action="shutdown", title="Anwendung beenden",
                       versions=[], player="", mode=data["mode"], state="queued", created=time.time())
            self.closing = job
            self.jobs.append(job)
            self.pending.put_nowait(job)
            return dict(job)

    def finish_shutdown(self, job, output):
        # Executed after earlier jobs. Re-scan process ownership, never terminate arbitrary Java processes.
        if job["mode"] == "all":
            owned = []
            for row in lab.VERSIONS:
                folder = lab.ROOT / row["version"]
                record = read_json(folder / "process.json", {})
                if not (folder / "worker.lock").exists():
                    continue
                live = {key: lab.process_alive(record[key]) for key in ("worker_pid", "server_pid") if record.get(key)}
                if not any(live.values()):
                    continue
                if not all(live.get(key) for key in ("worker_pid", "server_pid")):
                    raise RuntimeError("Unklarer Worker-Zustand bei " + row["version"] + "; Verwaltung bleibt geöffnet.")
                owned.append(row)
            job["versions"] = [row["version"] for row in owned]
            for row in owned:
                # A CLI-started server may still be booting. Wait for its normal save/stop interface.
                until = time.monotonic() + 180
                while not lab.status(row):
                    record = read_json(lab.ROOT / row["version"] / "process.json", {})
                    if not any(lab.process_alive(record[k]) for k in ("worker_pid", "server_pid") if record.get(k)):
                        break
                    if time.monotonic() > until:
                        raise RuntimeError(row["version"] + " noch nicht erreichbar; nicht gewaltsam beendet.")
                    time.sleep(1)
                lab.stop([row])
                record = read_json(lab.ROOT / row["version"] / "process.json", {})
                if lab.ROOT.joinpath(row["version"], "worker.lock").exists() and any(
                        lab.process_alive(record[k]) for k in ("worker_pid", "server_pid") if record.get(k)):
                    raise RuntimeError("Server läuft noch: " + row["version"])
                output.write((row["version"] + " gespeichert und beendet.\n").encode())
                output.flush()
        output.write("Webverwaltung wird beendet. Erneut öffnen mit Testserver.vbs.\n".encode())

    def submit(self, data):
        if not isinstance(data, dict) or data.get("action") not in ACTIONS:
            raise ValueError("Unbekannte Aktion.")
        versions = data.get("versions")
        if not isinstance(versions, list) or not versions or len(versions) > 48 or any(
                not isinstance(v, str) or v not in ROWS for v in versions) or len(set(versions)) != len(versions):
            raise ValueError("Bitte gültige Server auswählen.")
        player = data.get("player", "")
        if data["action"] in {"kit", "op"} and (not isinstance(player, str) or not re.fullmatch(r"[A-Za-z0-9_]{1,16}", player)):
            raise ValueError("Ein gültiger Minecraft-Spielername ist erforderlich.")
        with self.lock:
            if self.closing and self.closing["state"] != "failed":
                raise ValueError("Die Anwendung wird beendet; neue Aktionen sind gesperrt.")
            if any(set(versions).intersection(j["versions"]) for j in self.jobs if j["state"] in {"queued", "running"}):
                raise ValueError("Für einen dieser Server läuft bereits eine Aktion.")
            if self.pending.full():
                raise ValueError("Die Warteschlange ist voll. Bitte kurz warten.")
            job = dict(id=secrets.token_hex(12), action=data["action"], title=TITLES[data["action"]],
                       versions=versions, player=player if data["action"] in {"kit", "op"} else "",
                       state="queued", created=time.time())
            self.jobs.append(job)
            self.jobs = self.jobs[-100:]
            self.pending.put_nowait(job)
            return dict(job)

    def work(self):
        while True:
            job = self.pending.get()
            path = lab.ROOT / "web-jobs" / (job["id"] + ".log")
            try:
                with self.lock:
                    job.update(state="running", started=time.time())
                path.parent.mkdir(parents=True, exist_ok=True)
                argv = [sys.executable, "-u", str(lab.HERE / "lab.py"), job["action"], ",".join(job["versions"])]
                if job["action"] == "inspect":
                    argv = [sys.executable, "-u", str(lab.HERE / "world_audit.py"), ",".join(job["versions"])]
                if job["player"]:
                    argv.append(job["player"])
                env = dict(os.environ, PYTHONIOENCODING="utf-8", PYTHONUTF8="1")
                with path.open("wb") as output:
                    if job["action"] == "shutdown":
                        self.finish_shutdown(job, output)
                        code = 0
                    else:
                        child = subprocess.Popen(argv, cwd=lab.REPO, stdin=subprocess.DEVNULL,
                                                 stdout=output, stderr=subprocess.STDOUT, env=env,
                                                 creationflags=lab.NO_WINDOW)
                        code = child.wait()
                with self.lock:
                    job.update(state="success" if code == 0 else "failed", exit_code=code, finished=time.time())
                if job["action"] == "shutdown" and code == 0:
                    lab.save(lab.ROOT / "web-shutdown.json", job)
                    # Allow the browser to receive the successful completion before closing the listener.
                    time.sleep(2)
                    self.closed.set()
                    self.wake.set()
                    if self.shutdown_callback:
                        self.shutdown_callback()
                    return
            except Exception as error:
                with path.open("a", encoding="utf-8") as output:
                    output.write("\n" + str(error))
                with self.lock:
                    job.update(state="failed", finished=time.time())
            finally:
                self.wake.set()
                self.pending.task_done()


class Server(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = False

    def __init__(self, address, app):
        self.app = app
        super().__init__(address, Handler)
        self.authority = f"127.0.0.1:{self.server_port}"
        self.origin = "http://" + self.authority
        self.app.shutdown_callback = self.shutdown


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass

    def send(self, code, data, content_type="application/json; charset=utf-8"):
        if not isinstance(data, bytes):
            data = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Referrer-Policy", "no-referrer")
        self.send_header("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; object-src 'none'; frame-ancestors 'none'; base-uri 'none'")
        self.end_headers()
        try:
            self.wfile.write(data)
        except (ConnectionError, OSError):
            pass

    def local(self):
        # Exact Host + Origin checks also reject DNS rebinding and requests from other websites.
        if self.headers.get("Host") != self.server.authority or self.headers.get("Origin", self.server.origin) != self.server.origin:
            self.send(403, {"error": "Zugriff nur über die lokale Testlabor-Webseite."})
            return False
        return True

    def do_GET(self):
        if not self.local():
            return
        path = urlsplit(self.path).path
        if path == "/api/health":
            return self.send(200, {"app": APP_ID})
        if path == "/api/state":
            return self.send(200, self.server.app.state())
        if path in {"/", "/app.js", "/style.css", "/favicon.svg"}:
            name, mime = {"/": ("index.html", "text/html; charset=utf-8"),
                          "/app.js": ("app.js", "text/javascript; charset=utf-8"),
                          "/style.css": ("style.css", "text/css; charset=utf-8"),
                          "/favicon.svg": ("favicon.svg", "image/svg+xml")}[path]
            return self.send(200, (ASSETS / name).read_bytes(), mime)
        match = re.fullmatch(r"/api/servers/([\d.]+)/(catalog|log)", path)
        if match and match[1] in ROWS:
            folder = lab.ROOT / match[1]
            if match[2] == "catalog":
                catalog = read_json(folder / "arena-index.json")
                return self.send(200 if catalog else 404, catalog or {"error": "Noch kein Katalog vorhanden."})
            return self.send(200, {"output": tail(folder / "console.log")})
        self.send(404, {"error": "Nicht gefunden."})

    def do_POST(self):
        if not self.local():
            return
        if self.headers.get("X-Lab-Token") != self.server.app.token or self.headers.get("Content-Type") != "application/json":
            return self.send(403, {"error": "Bitte die Testlabor-Seite neu laden."})
        if self.path not in {"/api/actions", "/api/shutdown"}:
            return self.send(404, {"error": "Nicht gefunden."})
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if not 0 < size <= 8192:
                raise ValueError("Ungültige Anfragegröße.")
            self.connection.settimeout(5)
            data = json.loads(self.rfile.read(size))
            action = self.server.app.request_shutdown if self.path == "/api/shutdown" else self.server.app.submit
            self.send(202, action(data))
        except (ValueError, OSError, TypeError) as error:
            self.send(400, {"error": str(error)})


def existing(port):
    try:
        with urllib.request.urlopen(f"http://127.0.0.1:{port}/api/health", timeout=2) as response:
            return json.load(response).get("app") == APP_ID
    except (OSError, ValueError):
        return False


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--open", action="store_true", help="Open browser, reuse an existing dashboard")
    parser.add_argument("--port", type=int, default=PORT)
    args = parser.parse_args()
    url = f"http://127.0.0.1:{args.port}"
    if existing(args.port):
        if args.open:
            webbrowser.open(url)
        return
    try:
        server = Server(("127.0.0.1", args.port), App())
    except OSError:
        # Another simultaneous launcher may have won the port race.
        if existing(args.port):
            if args.open:
                webbrowser.open(url)
            return
        message = f"Port {args.port} ist bereits belegt. Die Testlabor-Webseite konnte nicht gestartet werden."
        if os.name == "nt":
            import ctypes
            ctypes.windll.user32.MessageBoxW(None, message, "ViaForge Testlabor", 0x10)
        raise RuntimeError(message)
    record = "web-process.json" if args.port == PORT else f"web-process-{args.port}.json"
    lab.save(lab.ROOT / record, {"pid": os.getpid(), "port": args.port, "started": time.time()})
    if args.open:
        threading.Timer(.4, lambda: webbrowser.open(url)).start()
    try:
        server.serve_forever()
    finally:
        server.app.closed.set()
        server.app.wake.set()
        server.server_close()


if __name__ == "__main__":
    main()
