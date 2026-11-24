#!/usr/bin/env bash
# Publica una versión nueva para "Buscar actualización": compila el release, escribe update.json
# (con checksum) en updates/ y sirve esa carpeta por HTTP en tu red local.
#
# Antes de publicar, sube versionCode (y versionName) en app/build.gradle.kts.
# Uso:  tools/publish_update.sh [--no-serve] [--port 8000] [--notes "texto"]
# La TV consulta la dirección fijada en gradle.properties (updateUrl). Deja este script corriendo
# mientras la TV busca/descarga y detenlo con Ctrl-C.
set -euo pipefail

PORT=8000
SERVE=1
NOTES=""
while [ $# -gt 0 ]; do
  case "$1" in
    --no-serve) SERVE=0; shift ;;
    --port) PORT="${2:?falta el puerto}"; shift 2 ;;
    --notes) NOTES="${2:?falta el texto}"; shift 2 ;;
    -h|--help) sed -n '2,9p' "$0"; exit 0 ;;
    *) echo "argumento inesperado: $1" >&2; exit 2 ;;
  esac
done

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"

./gradlew :app:assembleRelease --console=plain -q
APK="app/build/outputs/apk/release/app-release.apk"
CODE="$(grep -E '^\s*versionCode\s*=' app/build.gradle.kts | head -1 | tr -dc '0-9')"
NAME="$(grep -E '^\s*versionName\s*=' app/build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/')"
SHA="$(shasum -a 256 "$APK" | awk '{print $1}')"
IP="$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo 127.0.0.1)"

mkdir -p updates
cp "$APK" updates/tv-launcher.apk
cat > updates/update.json <<EOF
{
  "versionCode": $CODE,
  "versionName": "$NAME",
  "apkUrl": "http://$IP:$PORT/tv-launcher.apk",
  "sha256": "$SHA",
  "notes": "$NOTES"
}
EOF
echo "Publicado $NAME (código $CODE) en updates/. Dirección: http://$IP:$PORT/update.json"
grep -q "updateUrl=http://$IP:$PORT/" gradle.properties || echo "Aviso: gradle.properties apunta a otra dirección; la TV consulta la que tenga compilada."

if [ "$SERVE" -eq 1 ]; then
  echo "Sirviendo updates/ (Ctrl-C para detener)…"
  exec python3 -m http.server "$PORT" --bind 0.0.0.0 --directory updates
fi
