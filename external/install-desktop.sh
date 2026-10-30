#!/bin/sh
# Per-user Linux menu/desktop shortcut; no sudo and no system-service installation.
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CR=$(printf '\r')
case "$SCRIPT_DIR" in
  *'
'*|*"$CR"*) echo "Unsupported newline in application path." >&2; exit 1 ;;
esac
[ -f "$SCRIPT_DIR/SweetCherry.jar" ] || { echo "Run this script next to SweetCherry.jar." >&2; exit 1; }
chmod u+x "$SCRIPT_DIR/run.sh" "$SCRIPT_DIR/launcher.sh" "$SCRIPT_DIR/install-desktop.sh"
APP_DIR=${XDG_DATA_HOME:-"$HOME/.local/share"}/applications
mkdir -p "$APP_DIR"
# Desktop Entry Exec quoting differs from shell quoting. Escape literal percent field codes as well.
LAUNCHER=$(printf '%s' "$SCRIPT_DIR/launcher.sh" | sed 's/\\/\\\\/g; s/"/\\"/g; s/`/\\`/g; s/\$/\\$/g; s/%/%%/g')
ENTRY="$APP_DIR/sweetcherry.desktop"
cat > "$ENTRY" <<EOF
[Desktop Entry]
Type=Application
Version=1.0
Name=SweetCherry
Comment=CherryTree web companion
Exec="$LAUNCHER"
Icon=applications-office
Terminal=false
Categories=Office;
StartupNotify=false
EOF
chmod u+x "$ENTRY"
echo "Application menu shortcut: $ENTRY"
DESKTOP_DIR="${HOME}/Desktop"
if command -v xdg-user-dir >/dev/null 2>&1; then DESKTOP_DIR=$(xdg-user-dir DESKTOP); fi
if [ -d "$DESKTOP_DIR" ] && [ "$DESKTOP_DIR" != "$HOME" ]; then
  cp "$ENTRY" "$DESKTOP_DIR/sweetcherry.desktop"
  chmod u+x "$DESKTOP_DIR/sweetcherry.desktop"
  echo "Desktop shortcut: $DESKTOP_DIR/sweetcherry.desktop"
  echo "Your desktop may ask you to mark the shortcut as trusted / Allow Launching."
fi
echo "Moving the application folder? Run this script again to update the shortcuts."
