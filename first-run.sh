#!/bin/sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || exit 1

sh ./build.sh || exit 1
chmod u+x ./release/SweetCherry/run.sh ./release/SweetCherry/launcher.sh ./release/SweetCherry/install-desktop.sh || exit 1
echo "Optional Linux shortcuts: sh ./release/SweetCherry/install-desktop.sh"
case "$(uname -s)" in
  Linux) exec sh ./release/SweetCherry/launcher.sh "$@" ;;
  *) exec sh ./release/SweetCherry/run.sh "$@" ;;
esac
