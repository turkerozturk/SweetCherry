#!/bin/sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || exit 1

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java was not found. Install JDK 17 or a newer JDK."
  echo "Details: documentation/tr/setup-and-run.md"
  exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ { print $2; exit }')
JAVA_MAJOR=$(printf '%s\n' "$JAVA_VERSION" | awk -F. '{ if ($1 == 1) print $2; else print $1 }')
case "$JAVA_MAJOR" in
  ''|*[!0-9]*)
    echo "ERROR: The Java version could not be read. Check the 'java -version' command."
    exit 1
    ;;
esac
if [ "$JAVA_MAJOR" -lt 17 ]; then
  echo "ERROR: Java $JAVA_VERSION was found; SweetCherry requires JDK 17 or newer."
  exit 1
fi

echo "Java found: $JAVA_VERSION"
echo "Building SweetCherry. The first build may take a few minutes depending on your internet connection..."
if ! sh ./mvnw -DskipTests package; then
  echo "ERROR: The build could not be completed. Review the Maven error above."
  exit 1
fi

echo "BUILD COMPLETED: target/SweetCherry.jar"
echo "RUNNABLE DIRECTORY: release/SweetCherry/"
