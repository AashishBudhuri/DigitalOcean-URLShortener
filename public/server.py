#!/usr/bin/env python3
"""Local static server + mock /api/shorten for frontend testing."""

from __future__ import annotations

import json
import secrets
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parent
PORT = 8080
HOST = "0.0.0.0"

# In-memory store for this mock only
ALIASES: dict[str, str] = {}


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)

    def end_headers(self):
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def do_POST(self):
        if urlparse(self.path).path != "/api/shorten":
            self.send_error(404, "Not Found")
            return

        length = int(self.headers.get("Content-Length", 0))
        raw = self.rfile.read(length) if length else b"{}"
        try:
            body = json.loads(raw.decode("utf-8") or "{}")
        except json.JSONDecodeError:
            self._json(400, {"error": "Invalid JSON body."})
            return

        long_url = (body.get("longUrl") or body.get("long_url") or "").strip()
        alias = (body.get("alias") or "").strip()

        if not long_url:
            self._json(400, {"error": "Long URL is required."})
            return

        parsed = urlparse(long_url)
        if parsed.scheme not in ("http", "https") or not parsed.netloc:
            self._json(400, {"error": "Enter a valid http(s) URL."})
            return

        if alias:
            if alias in ALIASES and ALIASES[alias] != long_url:
                self._json(409, {"error": "Alias already taken."})
                return
        else:
            alias = secrets.token_urlsafe(6)

        ALIASES[alias] = long_url
        short_url = f"http://localhost:{PORT}/{alias}"
        self._json(201, {"shortUrl": short_url, "alias": alias, "longUrl": long_url})

    def _json(self, status: int, payload: dict):
        data = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, fmt, *args):
        print(f"[{self.log_date_time_string()}] {fmt % args}")


if __name__ == "__main__":
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print(f"Serving {ROOT} at http://localhost:{PORT}/")
    print("Mock API: POST /api/shorten")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")
