#!/bin/sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || exit 1

if ! command -v java >/dev/null 2>&1; then
  echo "HATA: Java bulunamadı. JDK 17 veya daha yeni bir JDK kurun."
  echo "Ayrıntılar: documentation/tr/setup-and-run.md"
  exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ { print $2; exit }')
JAVA_MAJOR=$(printf '%s\n' "$JAVA_VERSION" | awk -F. '{ if ($1 == 1) print $2; else print $1 }')
case "$JAVA_MAJOR" in
  ''|*[!0-9]*)
    echo "HATA: Java sürümü okunamadı. 'java -version' komutunu kontrol edin."
    exit 1
    ;;
esac
if [ "$JAVA_MAJOR" -lt 17 ]; then
  echo "HATA: Java $JAVA_VERSION bulundu; SweetCherry için JDK 17 veya daha yenisi gerekir."
  exit 1
fi

echo "Java bulundu: $JAVA_VERSION"
echo "SweetCherry derleniyor. İlk derleme internet hızına göre birkaç dakika sürebilir..."
if ! sh ./mvnw -DskipTests package; then
  echo "HATA: Derleme tamamlanamadı. Yukarıdaki Maven hata mesajını inceleyin."
  exit 1
fi

echo "DERLEME TAMAMLANDI: target/SweetCherry.jar"
echo "ÇALIŞTIRILABİLİR KLASÖR: release/"
