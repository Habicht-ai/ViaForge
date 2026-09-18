import http.client
import io
import json
from pathlib import Path
import tempfile
import threading
import unittest
from unittest.mock import patch, Mock

import lab
import webapp


class WebAppTests(unittest.TestCase):
    def setUp(self):
        self.app = webapp.App(background=False)
        self.server = webapp.Server(("127.0.0.1", 0), self.app)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()

    def request(self, method, path, data=None, headers=None):
        connection = http.client.HTTPConnection("127.0.0.1", self.server.server_port, timeout=3)
        combined = {"Content-Type": "application/json", "X-Lab-Token": self.app.token,
                    "Origin": self.server.origin}
        combined.update(headers or {})
        connection.request(method, path, json.dumps(data) if data is not None else None, combined)
        response = connection.getresponse()
        result = response.status, response.read(), dict(response.getheaders())
        connection.close()
        return result

    def test_local_page_and_state_expose_no_private_config(self):
        code, body, headers = self.request("GET", "/")
        self.assertEqual(200, code)
        self.assertIn(b"Deine Testserver", body)
        self.assertIn("frame-ancestors 'none'", headers["Content-Security-Policy"])
        code, body, _ = self.request("GET", "/api/state")
        self.assertEqual(200, code)
        self.assertEqual(48, json.loads(body)["profiles"])
        self.assertNotIn(b"password", body)

    def test_mutations_require_token_and_same_origin(self):
        action = {"action": "start", "versions": ["26.2"]}
        for headers in ({"X-Lab-Token": "wrong"}, {"Origin": "https://outside.invalid"},
                        {"Host": "outside.invalid"}, {"Content-Type": "text/plain"}):
            self.assertEqual(403, self.request("POST", "/api/actions", action, headers)[0])
        self.assertEqual([], self.app.jobs)
        self.assertEqual(403, self.request("GET", "/api/state", headers={"Host": "outside.invalid"})[0])

    def test_queued_actions_validate_input_and_prevent_overlap(self):
        for action in ({"action": "command", "versions": ["26.2"]},
                       {"action": "start", "versions": ["../../"]},
                       {"action": "start", "versions": ["26.2", "26.2"]},
                       {"action": "op", "versions": ["26.2"], "player": "x\nstop"}):
            self.assertEqual(400, self.request("POST", "/api/actions", action)[0])
        valid = {"action": "start", "versions": ["26.2"]}
        code, body, _ = self.request("POST", "/api/actions", valid)
        self.assertEqual(202, code)
        self.assertEqual("queued", json.loads(body)["state"])
        self.assertEqual(400, self.request("POST", "/api/actions", valid)[0])
        self.assertEqual(1, self.app.pending.qsize())

    def test_only_named_catalog_and_log_routes_are_readable(self):
        with tempfile.TemporaryDirectory() as tmp, patch.object(lab, "ROOT", Path(tmp)):
            folder = Path(tmp) / "26.2"
            folder.mkdir()
            (folder / "control.json").write_text('{"password":"secret"}')
            (folder / "arena-index.json").write_text('{"blocks":[]}')
            (folder / "console.log").write_bytes(b"ready\n")
            self.assertEqual(200, self.request("GET", "/api/servers/26.2/catalog")[0])
            self.assertEqual("ready\n", json.loads(self.request("GET", "/api/servers/26.2/log")[1])["output"])
            for path in ("/api/servers/26.2/control.json", "/../26.2/control.json", "/run/test-servers/26.2/control.json", "/api/servers/999/catalog", "/api/servers/%2e%2e/log"):
                code, body, _ = self.request("GET", path)
                self.assertEqual(404, code)
                self.assertNotIn(b"secret", body)

    def test_snapshot_distinguishes_foreign_port_from_managed_server_and_latest_failure(self):
        row = lab.select("26.2")[0]
        with tempfile.TemporaryDirectory() as tmp, patch.object(lab, "ROOT", Path(tmp)), patch.object(lab, "status") as ping:
            folder = Path(tmp) / "26.2"
            folder.mkdir()
            lab.save(folder / "persistence-check.json", {"success": True, "time": 1})
            lab.save(folder / "verification.json", {"success": False, "time": 2})
            ping.return_value = {"version": {"protocol": 340}}
            state = webapp.inspect_server(row)
            self.assertEqual("conflict", state["state"])
            self.assertFalse(state["managed"])
            self.assertFalse(state["verified"])
            ping.return_value = {"version": {"protocol": 776}, "players": {"online": 2}}
            state = webapp.inspect_server(row)
            self.assertEqual("running", state["state"])
            self.assertFalse(state["managed"])
            self.assertEqual(2, state["players"])

    def test_log_tail_is_bounded(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "console.log"
            path.write_bytes(b"a" * 50000 + b"\nlast line\n")
            self.assertEqual("last line\n", webapp.tail(path, 100))
            self.assertEqual("", webapp.tail(Path(tmp) / "missing"))

    def test_shutdown_queues_after_jobs_and_rejects_new_mutations(self):
        self.app.submit({"action": "start", "versions": ["26.2"]})
        self.assertEqual(403, self.request("POST", "/api/shutdown", {"mode": "all"}, {"X-Lab-Token": "wrong"})[0])
        self.assertEqual(400, self.request("POST", "/api/shutdown", {"mode": "unknown"})[0])
        code, body, _ = self.request("POST", "/api/shutdown", {"mode": "all"})
        self.assertEqual(202, code)
        self.assertEqual("queued", json.loads(body)["state"])
        self.assertEqual("start", self.app.pending.get_nowait()["action"])
        self.assertEqual("shutdown", self.app.pending.get_nowait()["action"])
        self.assertEqual(400, self.request("POST", "/api/actions", {"action": "start", "versions": ["1.12.2"]})[0])
        self.assertEqual(202, self.request("POST", "/api/shutdown", {"mode": "all"})[0])
        self.assertTrue(self.app.pending.empty())

    def test_web_only_shutdown_does_not_touch_minecraft(self):
        with patch.object(lab, "stop") as stop, patch.object(lab, "process_alive") as alive:
            self.app.finish_shutdown({"mode": "web"}, io.BytesIO())
            stop.assert_not_called()
            alive.assert_not_called()

    def test_shutdown_only_stops_owned_servers_and_refuses_unclear_processes(self):
        with tempfile.TemporaryDirectory() as tmp, patch.object(lab, "ROOT", Path(tmp)), \
                patch.object(lab, "process_alive", return_value=True), patch.object(lab, "status", return_value={"version": {"protocol": 776}}), \
                patch.object(lab, "stop") as stop:
            folder = Path(tmp) / "26.2"
            folder.mkdir()
            (folder / "worker.lock").touch()
            lab.save(folder / "process.json", {"worker_pid": 101, "server_pid": 102})
            stop.side_effect = lambda rows: (folder / "worker.lock").unlink()
            job = {"mode": "all"}
            self.app.finish_shutdown(job, io.BytesIO())
            self.assertEqual(["26.2"], job["versions"])
            self.assertEqual(["26.2"], [r["version"] for r in stop.call_args[0][0]])
            (folder / "worker.lock").touch()
            with patch.object(lab, "process_alive", side_effect=lambda pid: pid == 102):
                with self.assertRaisesRegex(RuntimeError, "Unklarer Worker"):
                    self.app.finish_shutdown({"mode": "all"}, io.BytesIO())

    def test_successful_shutdown_stops_background_loop_but_failure_keeps_ui(self):
        with tempfile.TemporaryDirectory() as tmp, patch.object(lab, "ROOT", Path(tmp)):
            self.app.shutdown_callback = Mock()
            self.app.request_shutdown({"mode": "web"})
            with patch.object(self.app, "finish_shutdown", side_effect=RuntimeError("save failed")):
                worker = threading.Thread(target=self.app.work, daemon=True)
                worker.start()
                self.app.pending.join()
            self.assertEqual("failed", self.app.closing["state"])
            self.assertFalse(self.app.closed.is_set())
            self.app.shutdown_callback.assert_not_called()
            self.app.request_shutdown({"mode": "web"})
            self.app.pending.join()
            worker.join(3)
            self.assertTrue(self.app.closed.is_set())
            self.app.shutdown_callback.assert_called_once()


if __name__ == "__main__":
    unittest.main()
