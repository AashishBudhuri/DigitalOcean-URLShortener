#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

PORT="${PORT:-8080}"
SKIP_TESTS="${SKIP_TESTS:-true}"

if [[ -f "${HOME}/.sdkman/bin/sdkman-init.sh" ]]; then
  # shellcheck disable=SC1091
  source "${HOME}/.sdkman/bin/sdkman-init.sh"
elif [[ -f "/usr/local/sdkman/bin/sdkman-init.sh" ]]; then
  # shellcheck disable=SC1091
  source "/usr/local/sdkman/bin/sdkman-init.sh"
fi

command -v java >/dev/null 2>&1 || { echo "Java is required but not found." >&2; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "Maven is required but not found." >&2; exit 1; }

if command -v fuser >/dev/null 2>&1; then
  fuser -k "${PORT}/tcp" >/dev/null 2>&1 || true
fi

echo "==> Building url-shortener (Java $(java -version 2>&1 | head -n1))"
MVN_ARGS=(-DskipTests)
if [[ "${SKIP_TESTS}" != "true" ]]; then
  MVN_ARGS=()
fi
mvn -q clean package "${MVN_ARGS[@]}"

JAR="$(ls -1 target/url-shortener-*.jar | grep -v '\.jar\.original$' | head -n1)"
if [[ -z "${JAR}" ]]; then
  echo "Build succeeded but jar was not found in target/." >&2
  exit 1
fi

echo "==> Starting ${JAR} on port ${PORT}"
echo "    UI:  http://localhost:${PORT}/"
echo "    API: http://localhost:${PORT}/api/shorten"
exec java -jar "${JAR}" --server.port="${PORT}"
