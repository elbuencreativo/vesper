#!/usr/bin/env bash
# Devuelve el control del inicio al launcher original y (opcional) desinstala TV Launcher.
#
# Uso:  tools/revert_home.sh <ip:puerto> [--home PAQUETE] [--keep] [--dry-run]
#   <ip:puerto>   dirección de depuración inalámbrica de la TV (cambia en cada sesión)
#   --home        launcher al que volver (por defecto el de Google TV: com.google.android.apps.tv.launcherx;
#                 usa p. ej. com.spocky.projengmenu para volver a Projectivy)
#   --keep        no desinstalar TV Launcher
#   --dry-run     solo muestra los comandos, no toca nada
set -euo pipefail

TARGET=""
HOME_PKG="com.google.android.apps.tv.launcherx"
OUR_PKG="com.minimal.tvlauncher"
KEEP=0
DRY=0

while [ $# -gt 0 ]; do
  case "$1" in
    --home) HOME_PKG="${2:?falta el paquete}"; shift 2 ;;
    --keep) KEEP=1; shift ;;
    --dry-run) DRY=1; shift ;;
    -h|--help) sed -n '2,10p' "$0"; exit 0 ;;
    *) [ -z "$TARGET" ] && TARGET="$1" || { echo "argumento inesperado: $1" >&2; exit 2; }; shift ;;
  esac
done
[ -n "$TARGET" ] || { echo "uso: $0 <ip:puerto> [--home PAQUETE] [--keep] [--dry-run]" >&2; exit 2; }

run() {
  if [ "$DRY" -eq 1 ]; then echo "+ $*"; else "$@"; fi
}

run adb connect "$TARGET"
# TV Launcher necesita que el home de Google TV esté deshabilitado; hay que reactivarlo antes de ceder el rol.
run adb -s "$TARGET" shell pm enable --user 0 com.google.android.apps.tv.launcherx
run adb -s "$TARGET" shell cmd role add-role-holder --user 0 android.app.role.HOME "$HOME_PKG"
run adb -s "$TARGET" shell input keyevent KEYCODE_HOME
if [ "$KEEP" -eq 0 ]; then
  run adb -s "$TARGET" uninstall "$OUR_PKG"
fi
echo "Listo: el inicio vuelve a $HOME_PKG."
