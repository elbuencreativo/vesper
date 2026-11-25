#!/usr/bin/env bash
# Publica una versión nueva en GitHub Releases (elbuencreativo/vesper): compila el release, calcula el
# checksum y sube el APK y update.json. La TV consulta .../releases/latest/download/update.json.
#
# Antes de publicar, sube versionCode (y versionName) en app/build.gradle.kts y haz commit.
# Uso:  tools/publish_update.sh [--notes "texto"]
set -euo pipefail

REPO="elbuencreativo/vesper"
NOTES=""
while [ $# -gt 0 ]; do
  case "$1" in
    --notes) NOTES="${2:?falta el texto}"; shift 2 ;;
    -h|--help) sed -n '2,6p' "$0"; exit 0 ;;
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
TAG="v$NAME"

mkdir -p updates
cp "$APK" updates/vesper.apk
cat > updates/update.json <<JSON
{
  "versionCode": $CODE,
  "versionName": "$NAME",
  "apkUrl": "https://github.com/$REPO/releases/download/$TAG/vesper.apk",
  "sha256": "$SHA",
  "notes": "$NOTES"
}
JSON
gh release create "$TAG" updates/vesper.apk updates/update.json --repo "$REPO" --title "Vesper $NAME" --notes "${NOTES:-Vesper $NAME}"
echo "Publicado $TAG: https://github.com/$REPO/releases/tag/$TAG"
