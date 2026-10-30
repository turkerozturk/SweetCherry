#!/bin/sh
# Linux desktop launcher. Use run.sh directly for foreground/SSH/server operation.
set -u
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || exit 1
fail() {
  echo "ERROR: $1" >&2
  if command -v zenity >/dev/null 2>&1; then zenity --error --text="$1" 2>/dev/null || :; fi
  exit 1
}
command -v java >/dev/null 2>&1 || fail "Java 17+ is required. Install a desktop JRE/JDK (not headless-only)."
[ -f SweetCherry.jar ] || fail "SweetCherry.jar is missing from $SCRIPT_DIR"
if [ -z "${DISPLAY:-}" ] && [ -z "${WAYLAND_DISPLAY:-}" ]; then
  echo "No desktop session detected. Starting in the terminal; Ctrl+C stops SweetCherry."
  exec sh ./run.sh --myapp.openWebBrowserOnStartup=false "$@"
fi
# A detached process survives closing the launching terminal. Swing provides Stop and close confirmation.
umask 077
nohup sh ./run.sh --myapp.desktop.enabled=true "$@" > launcher.log 2>&1 < /dev/null &
echo "SweetCherry launcher PID: $! — startup output: $SCRIPT_DIR/launcher.log"
