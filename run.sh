#!/bin/sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || exit 1

if ! command -v java >/dev/null 2>&1; then
  echo "HATA: Java bulunamadı. JDK 17 veya daha yeni bir JDK kurun."
  exit 1
fi
if [ ! -f target/SweetCherry.jar ]; then
  echo "HATA: target/SweetCherry.jar bulunamadı. Önce build.sh dosyasını çalıştırın."
  exit 1
fi

echo "SweetCherry başlatılıyor..."
echo "Tarayıcı adresi: http://localhost:8080"
echo "Durdurmak için bu terminalde Ctrl+C tuşlarına basın."
exec java -jar target/SweetCherry.jar \
  --server.port=8443 \
  --server.http.port=8080 \
  --myapp.openWebBrowserOnStartup=true \
  "$@"
