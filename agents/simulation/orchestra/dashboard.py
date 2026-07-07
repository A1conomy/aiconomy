"""HTTP dashboard for the staged orchestration demo."""

from __future__ import annotations

import json
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Callable

from simulation.orchestra.models import UserTaskRequest
from simulation.orchestra.runner import run_orchestration
from simulation.orchestra.state import OrchestrationWatchState

_ORCHESTRA_HTML = (Path(__file__).parent.parent / "static" / "orchestra.html").read_text(encoding="utf-8")


def start_orchestra_server(
    watch_state: OrchestrationWatchState,
    *,
    host: str = "127.0.0.1",
    port: int = 8766,
    step_delay_seconds: float = 2.0,
    bid_delay_seconds: float = 1.2,
    message_delay_seconds: float = 0.8,
) -> ThreadingHTTPServer:
    """Start dashboard; POST /api/submit kicks off a background orchestration run."""

    def start_run(request: UserTaskRequest) -> None:
        if watch_state.is_running:
            return

        def worker() -> None:
            try:
                run_orchestration(
                    request,
                    watch_state=watch_state,
                    step_delay_seconds=step_delay_seconds,
                    bid_delay_seconds=bid_delay_seconds,
                    message_delay_seconds=message_delay_seconds,
                )
            except Exception as exc:
                watch_state.fail(str(exc))

        thread = threading.Thread(target=worker, name="orchestration-run", daemon=True)
        thread.start()

    handler = _make_handler(lambda: watch_state.snapshot(), start_run)
    server = ThreadingHTTPServer((host, port), handler)
    thread = threading.Thread(target=server.serve_forever, name="orchestra-dashboard", daemon=True)
    thread.start()
    return server


def _make_handler(
    snapshot_provider: Callable[[], dict[str, object]],
    start_run: Callable[[UserTaskRequest], None],
):
    class OrchestraHandler(BaseHTTPRequestHandler):
        def log_message(self, format: str, *args: object) -> None:
            return

        def do_GET(self) -> None:
            if self.path in ("/", "/index.html"):
                body = _ORCHESTRA_HTML.encode("utf-8")
                self._respond(200, "text/html; charset=utf-8", body)
                return

            if self.path == "/api/state":
                payload = json.dumps(snapshot_provider()).encode("utf-8")
                self._respond(200, "application/json", payload)
                return

            self._respond(404, "text/plain", b"Not found")

        def do_POST(self) -> None:
            if self.path != "/api/submit":
                self._respond(404, "text/plain", b"Not found")
                return

            length = int(self.headers.get("Content-Length", "0"))
            raw = self.rfile.read(length)
            try:
                data = json.loads(raw.decode("utf-8"))
                title = str(data.get("title", "")).strip()
                description = str(data.get("description", "")).strip()
                if not title or not description:
                    self._respond(400, "application/json", b'{"error":"title and description required"}')
                    return
                request = UserTaskRequest(title=title, description=description)
                start_run(request)
                self._respond(202, "application/json", b'{"status":"started"}')
            except json.JSONDecodeError:
                self._respond(400, "application/json", b'{"error":"invalid json"}')

        def _respond(self, status: int, content_type: str, body: bytes) -> None:
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(body)

    return OrchestraHandler
