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
echo "Default browser address: http://localhost:8080 (external YAML/arguments may change the port)"
echo "Press Ctrl+C in this terminal to stop the application."
exec java -jar SweetCherry.jar \
  --myapp.openWebBrowserOnStartup=true \
  "$@"
