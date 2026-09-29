#!/usr/bin/env bash
# Pull a pre-built image and restart the service on the Droplet.
# Usage: IMAGE=ghcr.io/org/repo:tag ./deploy/remote-deploy.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f .env ]]; then
  echo "Missing .env — copy .env.example and set DB credentials first." >&2
  exit 1
fi

IMAGE="${IMAGE:?IMAGE env var is required (e.g. ghcr.io/owner/repo:sha)}"

echo "==> Pulling ${IMAGE}"
docker pull "${IMAGE}"

# Persist image tag for compose (without clobbering secrets)
if grep -q '^IMAGE=' .env; then
  sed -i.bak "s|^IMAGE=.*|IMAGE=${IMAGE}|" .env && rm -f .env.bak
else
  printf '\nIMAGE=%s\n' "${IMAGE}" >> .env
fi

echo "==> Restarting stack"
docker compose pull url-shortener || true
docker compose up -d --no-build --force-recreate url-shortener
docker image prune -f >/dev/null 2>&1 || true

echo "==> Status"
docker compose ps
docker logs --tail 30 url-shortener
