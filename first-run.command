#!/bin/sh
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || exit 1
sh ./first-run.sh
STATUS=$?
printf '\nÇıkmak için Enter tuşuna basın...'
read -r _unused
exit "$STATUS"

