#!/bin/sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || exit 1

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java was not found. Install JDK 17 or a newer JDK."
  exit 1
fi
if [ ! -f SweetCherry.jar ]; then
  echo "ERROR: SweetCherry.jar was not found next to this script."
  exit 1
fi

echo "Starting SweetCherry..."
echo "Browser address: http://localhost:8080"
echo "Press Ctrl+C in this terminal to stop the application."
exec java -jar SweetCherry.jar \
  --server.port=8443 \
  --server.http.port=8080 \
  --myapp.openWebBrowserOnStartup=true \
  "$@"
