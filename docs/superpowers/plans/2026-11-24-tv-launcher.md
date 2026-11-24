# Launcher minimalista para Google TV — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Un launcher nativo para el Onn 4K (Google TV, Android 14) con inicio de dos filas, "Seguir viendo", gestión de apps y reposo con arte de la National Gallery, manejable solo con el control remoto.

**Architecture:** Una sola app Android (módulo `app`) en Kotlin con Compose for TV. Capa de datos pura y testeable (`AppCatalog`, `Prefs`, `WatchNextRepository`, `RecentsStore`) separada de la UI de Compose. El reposo es un estado de la `HomeActivity` controlado por `IdleController` (sin DreamService). El arte se prepara con un script offline y viaja dentro de `assets/`.

**Tech Stack:** Kotlin, Jetpack Compose + `androidx.tv:tv-material`, DataStore (Preferences), `androidx.tvprovider` (TvContractCompat), JUnit4 para lógica pura, Macrobenchmark/Baseline Profile, ADB para verificar en el dispositivo.

**Spec:** `docs/superpowers/specs/2026-11-24-tv-launcher-design.md`

## Global Constraints

- Dispositivo objetivo: Onn 4K Streaming Box, Google TV, Android 14 (API 34), salida 1920x1080, densidad 320. `minSdk 28`, `targetSdk 34`.
- Kotlin nativo + Jetpack Compose for TV (`androidx.tv`). Sin WebView, sin Leanback clásico.
- Imágenes de reposo pre-redimensionadas a 1920x1080 (WebP), sin desenfoque en tiempo real.
- Debe declarar categorías `HOME` + `LEANBACK_LAUNCHER` + `DEFAULT` para ser launcher por defecto. **Nunca** deshabilitar `com.google.android.apps.tv.launcherx`.
- Todo manejable con D-pad, OK, atrás y mantener OK. El control del Onn no tiene botón de menú.
- Inicio (disposición A): arriba-izquierda Wi-Fi, Bluetooth, ajustes Android, ajustes del launcher; arriba-derecha hora pequeña; centro "Seguir viendo" (tarjetas 16:9); abajo fila de apps con iconos monocromos y acceso "＋".
- Mantener OK en un app de la fila de inicio lo **cierra** directo (`killBackgroundProcesses`). Ocultar, mover y desinstalar solo en el modo de edición de "＋".
- Reposo: tras inactividad entra la obra a pantalla completa con zoom lento; tras **20 minutos** seguidos de arte la pantalla se atenúa a negro; cualquier botón despierta y la primera pulsación se descarta.
- ~20 obras de dominio público (solo con marca open access) de `NationalGalleryOfArt/opendata`, dentro del app. Ficha: título, artista, año.
- Meta de rendimiento: arranque en frío < 1 s y sin tirones visibles en el Onn; baseline profile desde el inicio.
- Anti-objetivos: sin opciones extra, sin filas "por si acaso", sin cortes bruscos en el reposo, nada que dificulte volver al home original.
- Paquete: `com.minimal.tvlauncher`. Proyecto en `/Users/leo.gomez/Desktop/6. Apps/TVLauncher` (no es repo git: Task 1 ejecuta `git init`).
- TV conectada por ADB inalámbrico (`192.168.1.32:<puerto>`; el puerto cambia). Reiniciar el servidor adb desde la Terminal del usuario si falla con "No route to host".

## Review Focus

1. Un app guardado en el orden/ocultos que se desinstala fuera del launcher: no debe romper ni dejar huecos.
2. Una tarjeta de Watch Next cuyo intent es nulo o cuyo app ya no existe: no debe crashear al pulsarla.
3. Más de ~100 apps instalados: la cuadrícula de "＋" debe seguir fluida y enfocable.
4. Pulsar HOME estando ya en el launcher o volviendo de un app: debe reiniciar el foco al inicio y el temporizador del reposo.
5. Faltan o están corruptos assets de arte, o `killBackgroundProcesses` no detiene un app: el launcher debe seguir funcionando y mostrar un reposo negro/fallback, nunca un crash.

---

### Task 1: Proyecto base como launcher instalable

**Files:**
- Create: proyecto Gradle (`settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`), `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/minimal/tvlauncher/HomeActivity.kt`, `.gitignore`
- Test: `app/src/test/java/com/minimal/tvlauncher/SmokeTest.kt`

**Interfaces:**
- Produces: `HomeActivity : ComponentActivity` que renderiza `@Composable fun HomeScreen()`; manifest con intent-filter `MAIN` + categorías `HOME`, `LEANBACK_LAUNCHER`, `DEFAULT`.

- [ ] **Step 1: `git init`, crear `.gitignore` (incluye `.superpowers/`, `build/`, `local.properties`) y el proyecto Gradle con Compose y `androidx.tv:tv-material`**
- [ ] **Step 2: Test `SmokeTest.appId_isLauncherPackage` que afirma `BuildConfig.APPLICATION_ID == "com.minimal.tvlauncher"`; ejecutarlo y verlo fallar antes de configurar `applicationId`**
- [ ] **Step 3: Implementar `HomeActivity` con `HomeScreen()` mostrando solo la hora; declarar el manifest como home**
- [ ] **Step 4: Ejecutar `./gradlew :app:testDebugUnitTest` — Expected: PASS**
- [ ] **Step 5: Instalar en la TV: `adb -s <tv> install -r app/build/outputs/apk/debug/app-debug.apk` y `adb -s <tv> shell cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME` — Expected: incluye `com.minimal.tvlauncher`**
- [ ] **Step 6: Commit** `feat: base launcher project`

### Task 2: Spike de Watch Next (puerta de decisión)

**Files:**
- Create: `app/src/main/java/com/minimal/tvlauncher/data/WatchNextRepository.kt`, `app/src/main/java/com/minimal/tvlauncher/data/WatchNextItem.kt`
- Modify: `app/src/main/AndroidManifest.xml` (permiso `com.android.providers.tv.permission.READ_EPG_DATA`)
- Test: `app/src/test/java/com/minimal/tvlauncher/data/WatchNextMappingTest.kt`

**Interfaces:**
- Produces:
  - `data class WatchNextItem(val packageName: String, val title: String, val posterUri: Uri?, val progress: Float?, val intent: Intent?)`
  - `class WatchNextRepository(context: Context) { suspend fun load(limit: Int = 10): List<WatchNextItem> }`
  - `fun mapRow(packageName: String, title: String?, posterUri: String?, positionMs: Long, durationMs: Long, intentUri: String?): WatchNextItem?` (devuelve `null` si falta título)

- [ ] **Step 1: Test `mapRow_computesProgress_andDropsRowsWithoutTitle` con `positionMs=300000, durationMs=1200000` → `progress == 0.25f`, y título nulo → `null`**
- [ ] **Step 2: Ejecutar — Expected: FAIL (`mapRow` no definida)**
- [ ] **Step 3: Implementar `mapRow` y `WatchNextRepository.load` leyendo `TvContractCompat.WatchNextPrograms.CONTENT_URI` con `contentResolver.query`; `progress` nulo si `durationMs <= 0`; `intentUri` parseado con `Intent.parseUri(..., URI_INTENT_SCHEME)` dentro de `runCatching`**
- [ ] **Step 4: Ejecutar tests — Expected: PASS**
- [ ] **Step 5: Pantalla temporal de depuración que lista `title` + `packageName` de `load()`; instalar en la TV, abrir Netflix y YouTube, reproducir unos segundos, volver al launcher**
- [ ] **Step 6: Registrar el resultado en `docs/superpowers/specs/2026-11-24-tv-launcher-design.md` (sección "Seguir viendo"): ¿devolvió filas reales? Si NO → el plan sigue solo con `RecentsStore` (Task 4) y se elimina `WatchNextRepository`; si SÍ → se conserva y se muestra mezclado. Avisar al usuario del resultado antes de seguir.**
- [ ] **Step 7: Commit** `feat: watch next spike and repository`

### Task 3: Catálogo de apps y preferencias (orden, ocultos)

**Files:**
- Create: `data/AppEntry.kt`, `data/AppCatalog.kt`, `data/Prefs.kt`, `data/OrderLogic.kt`
- Test: `app/src/test/java/com/minimal/tvlauncher/data/OrderLogicTest.kt`

**Interfaces:**
- Produces:
  - `data class AppEntry(val packageName: String, val label: String, val launchIntent: Intent)`
  - `object OrderLogic { fun arrange(installed: List<AppEntry>, savedOrder: List<String>, hidden: Set<String>, defaultFirst: List<String>): ArrangedApps }`
  - `data class ArrangedApps(val row: List<AppEntry>, val all: List<AppEntry>, val hiddenEntries: List<AppEntry>)`
  - `class Prefs(context: Context) { val order: Flow<List<String>>; val hidden: Flow<Set<String>>; suspend fun setOrder(l: List<String>); suspend fun setHidden(s: Set<String>) }`
  - `class AppCatalog(context: Context) { fun installed(): List<AppEntry> }` (usa `LEANBACK_LAUNCHER` y excluye el propio launcher)

- [ ] **Step 1: Tests de `OrderLogic.arrange`: (a) sin orden guardado, `defaultFirst = [netflix, youtube, ...]` va primero y el resto alfabético; (b) apps nuevos se añaden al final; (c) un paquete guardado pero ya desinstalado se ignora; (d) ocultos salen de `row` pero aparecen en `all` marcados y en `hiddenEntries`**
- [ ] **Step 2: Ejecutar — Expected: FAIL**
- [ ] **Step 3: Implementar `OrderLogic.arrange` como función pura; `row` = los N primeros visibles (N = 8)**
- [ ] **Step 4: Ejecutar — Expected: PASS**
- [ ] **Step 5: Implementar `Prefs` con DataStore y `AppCatalog` con `PackageManager.queryIntentActivities`**
- [ ] **Step 6: Commit** `feat: app catalog, ordering and prefs`

### Task 4: "Vistos recientemente" (respaldo)

**Files:**
- Create: `data/RecentsStore.kt`
- Test: `app/src/test/java/com/minimal/tvlauncher/data/RecentsStoreTest.kt`

**Interfaces:**
- Produces: `class RecentsStore(prefs: Prefs) { suspend fun record(packageName: String); fun recent(limit: Int = 10): Flow<List<String>> }` — lista de paquetes más reciente primero, sin duplicados.

- [ ] **Step 1: Test `record_movesToFront_noDuplicates_capsAtLimit`**
- [ ] **Step 2: Ejecutar — Expected: FAIL**
- [ ] **Step 3: Implementar con una lista persistida en `Prefs` (extender `Prefs` con `recents`)**
- [ ] **Step 4: Ejecutar — Expected: PASS**
- [ ] **Step 5: Commit** `feat: recents store`

### Task 5: Pantalla de inicio (disposición A)

**Files:**
- Create: `ui/HomeScreen.kt`, `ui/TopBar.kt`, `ui/ContinueRow.kt`, `ui/AppRow.kt`, `ui/Theme.kt`, `ui/Icons.kt`
- Modify: `HomeActivity.kt`
- Test: `app/src/test/java/com/minimal/tvlauncher/ui/ContinueRowModelTest.kt`

**Interfaces:**
- Consumes: `ArrangedApps`, `WatchNextItem`, `RecentsStore`.
- Produces:
  - `data class ContinueCard(val title: String, val subtitle: String?, val posterUri: Uri?, val progress: Float?, val intent: Intent?, val packageName: String)`
  - `fun buildContinueCards(watchNext: List<WatchNextItem>, recents: List<AppEntry>, cap: Int = 10): List<ContinueCard>` (Watch Next primero; si no hay nada, las tarjetas de respaldo con icono+nombre; si ambas vacías → lista vacía y la fila se oculta)

- [ ] **Step 1: Tests de `buildContinueCards`: mezcla con tope 10; respaldo cuando Watch Next vacío; lista vacía cuando no hay nada; descarta tarjetas con `intent == null` solo si no hay respaldo posible**
- [ ] **Step 2: Ejecutar — Expected: FAIL**
- [ ] **Step 3: Implementar `buildContinueCards` y la UI Compose: barra superior (4 iconos circulares + hora `HH:mm` pequeña), fila "Seguir viendo", fila de apps con "＋" al final; foco D-pad con escala 1.08 y contorno blanco; fondo oscuro `#0B0B0D`; iconos monocromos de las 4 acciones en `Icons.kt`**
- [ ] **Step 4: Ejecutar tests — Expected: PASS**
- [ ] **Step 5: Instalar en la TV y verificar con `adb exec-out screencap -p > home.png` que se ve la disposición A y que D-pad navega entre filas**
- [ ] **Step 6: Commit** `feat: home screen layout A`

### Task 6: Lanzar y cerrar apps

**Files:**
- Create: `actions/AppActions.kt`
- Modify: `ui/AppRow.kt`, `ui/ContinueRow.kt`, `AndroidManifest.xml` (permiso `KILL_BACKGROUND_PROCESSES`)
- Test: `app/src/test/java/com/minimal/tvlauncher/actions/AppActionsTest.kt`

**Interfaces:**
- Produces: `class AppActions(context: Context, recents: RecentsStore) { suspend fun launch(entry: AppEntry): Boolean; fun close(packageName: String): Boolean; fun uninstall(packageName: String) }`. `launch` devuelve `false` si `startActivity` lanza `ActivityNotFoundException` y no registra el reciente en ese caso.

- [ ] **Step 1: Test `launch_returnsFalse_andSkipsRecents_whenActivityMissing` con un `Context` falso**
- [ ] **Step 2: Ejecutar — Expected: FAIL**
- [ ] **Step 3: Implementar `AppActions`; `close` usa `ActivityManager.killBackgroundProcesses`; `uninstall` usa `Intent.ACTION_DELETE` con `package:` URI**
- [ ] **Step 4: Conectar: OK lanza, mantener OK (≥ 600 ms) en la fila de apps llama `close` y muestra un aviso breve "Cerrado"; en tarjetas de "Seguir viendo" solo lanza**
- [ ] **Step 5: Verificar en la TV: abrir YouTube, volver al launcher, mantener OK sobre YouTube y confirmar con `adb shell pidof com.google.android.youtube.tv` — Expected: sin salida**
- [ ] **Step 6: Commit** `feat: launch and close apps`

### Task 7: Pantalla "＋" con modo de edición

**Files:**
- Create: `ui/AllAppsScreen.kt`, `ui/EditMode.kt`
- Modify: `ui/HomeScreen.kt` (navegación), `data/Prefs.kt`
- Test: `app/src/test/java/com/minimal/tvlauncher/data/ReorderTest.kt`

**Interfaces:**
- Produces: `fun moveItem(order: List<String>, from: Int, to: Int): List<String>` (indices acotados, no pierde elementos); `fun toggleHidden(hidden: Set<String>, pkg: String): Set<String>`.

- [ ] **Step 1: Tests de `moveItem` (mover al inicio, al final, índices fuera de rango no cambian la lista) y `toggleHidden`**
- [ ] **Step 2: Ejecutar — Expected: FAIL**
- [ ] **Step 3: Implementar ambas funciones puras**
- [ ] **Step 4: Cuadrícula con todos los apps (ocultos atenuados), OK abre el app; botón "Editar" entra al modo de edición donde OK sobre un app abre acciones (Mover, Ocultar/Mostrar, Desinstalar); Mover usa D-pad y OK para soltar; persistir con `Prefs.setOrder/setHidden`**
- [ ] **Step 5: Verificar en la TV con ≥ 100 apps simulados (lista de prueba en debug) que la cuadrícula navega sin tirones**
- [ ] **Step 6: Commit** `feat: all apps screen with edit mode`

### Task 8: Accesos superiores (Wi-Fi, Bluetooth, ajustes)

**Files:**
- Create: `actions/SystemShortcuts.kt`
- Test: `app/src/test/java/com/minimal/tvlauncher/actions/SystemShortcutsTest.kt`

**Interfaces:**
- Produces: `class SystemShortcuts(context: Context) { fun openWifi(); fun openBluetooth(); fun openAndroidSettings() }`. Cada una prueba su pantalla específica y, si `resolveActivity` es nulo, abre `Settings.ACTION_SETTINGS`.

- [ ] **Step 1: Verificar por ADB en la TV cuáles resuelven: `adb shell cmd package resolve-activity -a android.settings.WIFI_SETTINGS`, `-a android.settings.BLUETOOTH_SETTINGS`, `-a android.settings.SETTINGS`; anotar en el commit**
- [ ] **Step 2: Test `openWifi_fallsBackToGeneralSettings_whenSpecificMissing`**
- [ ] **Step 3: Ejecutar — Expected: FAIL**
- [ ] **Step 4: Implementar y conectar a la barra superior**
- [ ] **Step 5: Ejecutar tests — Expected: PASS; probar los 3 accesos en la TV**
- [ ] **Step 6: Commit** `feat: system shortcuts`

### Task 9: Pipeline de arte (20 obras)

**Files:**
- Create: `tools/art/prepare_art.py`, `tools/art/README.md`, `app/src/main/assets/art/*.webp`, `app/src/main/assets/art/art.json`
- Test: `tools/art/test_prepare_art.py`

**Interfaces:**
- Produces: `art.json` = lista de `{ "file": "xxx.webp", "title": str, "artist": str, "year": str }`. Función `select_open_access(rows, n=20) -> list[dict]`.

- [ ] **Step 1: Test `test_select_open_access_skips_rows_without_flag_and_without_image` con filas de ejemplo**
- [ ] **Step 2: Ejecutar `python3 -I -m pytest tools/art` — Expected: FAIL**
- [ ] **Step 3: Implementar el script: descarga solo `objects.csv` y `published_images.csv` del repo (no el repo completo), filtra `openaccess == 1` y con `iiifurl`, elige 20 obras de pinturas con variedad de época y paleta, descarga cada imagen a 1920 px de ancho por IIIF, convierte a WebP calidad 82, escribe `art.json`. Los archivos descargados van a un directorio nuevo y vacío**
- [ ] **Step 4: Ejecutar tests — Expected: PASS**
- [ ] **Step 5: Revisar visualmente las 20 imágenes (que no sean oscuras de más ni con marcas) y que `du -sh app/src/main/assets/art` sea razonable (< 25 MB)**
- [ ] **Step 6: Commit** `feat: art assets pipeline`

### Task 10: Modo reposo

**Files:**
- Create: `idle/IdleController.kt`, `ui/IdleScreen.kt`, `data/ArtRepository.kt`
- Modify: `HomeActivity.kt`
- Test: `app/src/test/java/com/minimal/tvlauncher/idle/IdleControllerTest.kt`

**Interfaces:**
- Produces:
  - `enum class IdleState { Active, Art, Dark }`
  - `class IdleController(scope: CoroutineScope, clock: Clock, idleAfterMs: Long = 300_000, darkAfterMs: Long = 1_200_000) { val state: StateFlow<IdleState>; fun onUserInput(): Boolean; fun onLauncherPaused(); fun onLauncherResumed() }` — `onUserInput` devuelve `true` si la pulsación se debe **descartar** (solo cuando estaba en `Art` o `Dark`).
  - `class ArtRepository(context: Context) { fun all(): List<ArtWork>; fun next(): ArtWork? }`; `data class ArtWork(val assetPath: String, val title: String, val artist: String, val year: String)`; `next()` devuelve `null` si faltan assets.

- [ ] **Step 1: Tests de `IdleController` con reloj falso: pasa a `Art` a los 5 min, a `Dark` a los 20 min **de arte**, `onUserInput` en `Art` devuelve `true` y vuelve a `Active`, `onLauncherResumed` reinicia el temporizador, `onLauncherPaused` lo detiene**
- [ ] **Step 2: Ejecutar — Expected: FAIL**
- [ ] **Step 3: Implementar `IdleController` y `ArtRepository` (lee `art.json`, rota cada 45 s en orden aleatorio sin repetir seguidas)**
- [ ] **Step 4: `IdleScreen`: fundido de ~1.2 s, zoom lento (escala 1.0→1.06 en 45 s), hora pequeña, ficha título/artista/año/"National Gallery of Art"; `Dark` pinta negro; mientras el estado ≠ `Active` poner `FLAG_KEEP_SCREEN_ON`, y quitarla en `onPause`**
- [ ] **Step 5: Interceptar `dispatchKeyEvent` en `HomeActivity`: si `onUserInput()` devuelve `true`, consumir el evento**
- [ ] **Step 6: Ejecutar tests — Expected: PASS; en la TV bajar temporalmente los tiempos por flag de debug y ver el ciclo Active→Art→Dark→despertar**
- [ ] **Step 7: Commit** `feat: idle art mode`

### Task 11: Ajustes del launcher, reversión y salvavidas

**Files:**
- Create: `ui/LauncherSettingsScreen.kt`, `tools/revert_home.sh`, `util/CrashGuard.kt`
- Test: `app/src/test/java/com/minimal/tvlauncher/util/CrashGuardTest.kt`

**Interfaces:**
- Produces: `class CrashGuard(prefs: Prefs, clock: Clock) { fun onStart(); fun shouldYieldToStock(): Boolean }` — `true` tras 3 arranques fallidos dentro de 2 minutos.

- [ ] **Step 1: Test `shouldYieldToStock_afterThreeCrashesInTwoMinutes` y `false` si pasan más de 2 minutos**
- [ ] **Step 2: Ejecutar — Expected: FAIL**
- [ ] **Step 3: Implementar `CrashGuard`; si `shouldYieldToStock()`, `HomeActivity` lanza el home original (`com.google.android.apps.tv.launcherx`) y termina**
- [ ] **Step 4: Pantalla mínima de ajustes: "Mostrar apps ocultos", "Volver al launcher original" (abre selector de home del sistema)**
- [ ] **Step 5: `tools/revert_home.sh <ip:puerto>`: reactiva el home original como predeterminado y desinstala `com.minimal.tvlauncher`; probar en seco con `--dry-run`**
- [ ] **Step 6: Ejecutar tests — Expected: PASS**
- [ ] **Step 7: Commit** `feat: launcher settings, revert script and crash guard`

### Task 12: Rendimiento y entrega

**Files:**
- Create: módulo `baselineprofile`, `app/src/main/baseline-prof.txt`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Generar baseline profile con Macrobenchmark en el Onn (arranque + navegar filas + abrir "＋")**
- [ ] **Step 2: Build release; medir arranque en frío: `adb shell am start -W -n com.minimal.tvlauncher/.HomeActivity` repetido 10 veces — Expected: mediana `TotalTime` < 1000 ms**
- [ ] **Step 3: Revisar memoria con `adb shell dumpsys meminfo com.minimal.tvlauncher` en inicio y en reposo y anotar los valores en el README**
- [ ] **Step 4: Fijar como home predeterminado y probar el flujo completo con el control: inicio, abrir/cerrar apps, "＋", editar, reposo, despertar**
- [ ] **Step 5: Commit** `chore: baseline profile and release build`
