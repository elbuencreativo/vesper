# TV Launcher

Launcher minimalista para Google TV (probado en Onn 4K, Android 14). Inicio con accesos, "Seguir viendo" y apps; reposo con obras de la National Gallery of Art.

## Compilar

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest          # tests
./gradlew :app:assembleRelease            # app/build/outputs/apk/release/app-release.apk
python3 -I -m unittest discover -s tools/art -t tools/art   # tests del selector de arte
```

## Instalar en la TV

Activa la depuración inalámbrica en la TV y usa su `IP:puerto`:

```bash
adb connect 192.168.1.XX:PUERTO
adb install -r app/build/outputs/apk/release/app-release.apk
```

Luego elige TV Launcher en **Configuración > Apps > App de inicio predeterminada**.

### Que la tecla HOME abra TV Launcher

En Google TV el home de Google tiene prioridad sobre cualquier launcher de terceros, así que el rol de inicio no basta. TV Launcher incluye un servicio de accesibilidad que captura solo la tecla HOME y abre el launcher (no lee la pantalla ni toca el home de Google). Actívalo una vez en **Configuración > Accesibilidad > TV Launcher · tecla HOME**, o por ADB:

```bash
adb -s IP:PUERTO shell settings put secure enabled_accessibility_services com.minimal.tvlauncher/com.minimal.tvlauncher.actions.HomeKeyService
adb -s IP:PUERTO shell settings put secure accessibility_enabled 1
```

Para desactivarlo: Configuración > Accesibilidad, o `adb shell settings delete secure enabled_accessibility_services`.

## Volver al launcher anterior

Desde el app: Ajustes del launcher (icono ◐) > "Volver al launcher original". O por ADB:

```bash
tools/revert_home.sh 192.168.1.XX:PUERTO --dry-run     # ver qué haría
tools/revert_home.sh 192.168.1.XX:PUERTO               # vuelve al home de Google TV y desinstala TV Launcher
tools/revert_home.sh 192.168.1.XX:PUERTO --home com.spocky.projengmenu --keep   # vuelve a Projectivy, conserva TV Launcher
```

Si el launcher falla 3 veces seguidas al arrancar, cede el control solo al home de Google TV.

## Diseño

Paleta de 10 tonos (azul noche a ámbar) en `ui/Theme.kt`, con roles tonales al estilo Material 3. Fondo permanente: una obra de la National Gallery bajo una capa de la paleta, que cambia cada 10 minutos. El foco es ámbar con resplandor naranja. Los iconos son monocromos cálidos y recuperan su color al enfocar.

## Controles

- **OK** abre; **mantener OK** en un app de la fila abre su pantalla de información (Forzar detención).
- **＋** abre todas las apps; **Editar** permite mover, ocultar y desinstalar.
- Sin tocar el control 5 min entra el reposo; tras 20 min de arte la pantalla queda en negro. Cualquier botón vuelve al inicio.

## Cambiar las obras del reposo

Edita `PREFERRED`/`SCENIC` en `tools/art/prepare_art.py` y ejecuta:

```bash
python3 -I tools/art/prepare_art.py --out /ruta/NUEVA/vacia --assets app/src/main/assets/art
```

Solo usa obras con marca *open access* del repo [NationalGalleryOfArt/opendata](https://github.com/NationalGalleryOfArt/opendata).

## Pruebas rápidas en depuración

```bash
adb shell am start -n com.minimal.tvlauncher/.HomeActivity --el idle_ms 12000 --el dark_ms 25000
```

Acorta los tiempos del reposo (solo en builds de depuración).
