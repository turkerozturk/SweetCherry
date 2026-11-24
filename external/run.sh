#!/bin/sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || exit 1

SWEETCHERRY_JAVA=java
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then SWEETCHERRY_JAVA="$JAVA_HOME/bin/java"; fi
if [ -x "$SCRIPT_DIR/runtime/bin/java" ]; then SWEETCHERRY_JAVA="$SCRIPT_DIR/runtime/bin/java"; fi
if ! command -v "$SWEETCHERRY_JAVA" >/dev/null 2>&1; then
  echo "ERROR: Java was not found. Install JDK 17 or a newer JDK."
  exit 1
fi
if [ ! -f SweetCherry.jar ]; then
  echo "ERROR: SweetCherry.jar was not found next to this script."
  exit 1
fi

echo "Starting SweetCherry..."
echo "Browser address and ports are read from application.yml."
echo "Press Ctrl+C in this terminal to stop the application."
exec "$SWEETCHERRY_JAVA" -jar SweetCherry.jar \
  --myapp.openWebBrowserOnStartup=true \
  "$@"
