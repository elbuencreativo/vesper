# Launcher minimalista para Google TV — Diseño

Fecha: 2026-11-24

## Intención y por qué

El usuario quiere reemplazar el home de su TV por un launcher propio. Ya usa Projectivy Launcher y lo descarta por dos razones: **no se siente premium** (sobre todo cuando queda en reposo antes de ver algo) y **se siente pesado/lento y con demasiadas opciones**. Lo que quiere: algo **simple, rápido, minimalista y premium**, manejable solo con el control remoto.

## Dispositivo objetivo (verificado por ADB el 2026-11-24)

- Onn 4K Streaming Box (Google TV), Android 14 (API 34), `tv,nosdcard`.
- Pantalla física 3840x2160 con salida configurada a 1920x1080, densidad 320.
- Launcher actual: `com.google.android.apps.tv.launcherx`. Projectivy 4.73 ya instalado.
- Apps del usuario: Netflix, YouTube, Prime Video, Apple TV, HBO Max (`com.wbd.stream`), Crunchyroll, Spotify, Stremio, Jellyfin, Wholphin, SmartTube, OnTV, Downloader.
- Hardware modesto: el rendimiento es una restricción de diseño, no un detalle.

## Objetivos

1. Sentirse más fluido y premium que Projectivy: respuesta inmediata al control, animaciones suaves, arranque al instante.
2. Inicio con la disposición **A** elegida en las maquetas:
   - Arriba a la izquierda, cuatro accesos pequeños: Wi-Fi, Bluetooth (audífonos), ajustes de Android, ajustes del launcher. Arriba a la derecha, hora pequeña.
   - Al centro, fila **"Seguir viendo"** (tarjetas 16:9).
   - Abajo, fila de **apps** con iconos minimalistas monocromos y un acceso "＋" al resto de las apps.
3. **Modo reposo:** tras unos minutos sin tocar el control, la interfaz se desvanece y entra una obra de arte a pantalla completa con zoom muy lento; solo hora pequeña y ficha de la obra (título, artista, año, National Gallery of Art). Cualquier botón vuelve al inicio al instante.
4. **~20 obras de dominio público** de la National Gallery of Art (repo `NationalGalleryOfArt/opendata`), empaquetadas dentro del app, que rotan. No se descarga el repo completo.
5. Gestión de apps: **ocultar**, **desinstalar**, **reordenar** (mover con el control) y **cerrar un app** manteniendo pulsado el botón del control.
6. Todo funciona con el control remoto (D-pad, OK, atrás, mantener OK).
7. Dejar **abierta la puerta a un menú de ajustes** del launcher (inicialmente mínimo; el usuario lo hará crecer después).

## No objetivos

- No es un reemplazo completo de Projectivy: sin fondos configurables, sin plugins, sin editor de filas, sin widgets.
- No descarga ni gestiona catálogos de arte en línea; el reposo funciona sin internet.
- No gestiona cuentas, perfiles ni contenido propio.
- No es una app para teléfono ni para otros formatos de pantalla.

## Anti-objetivos (fracasos aunque "funcione")

- Que se sienta lento o con tirones en el Onn.
- Que acumule opciones, menús o filas "por si acaso".
- Que el reposo se vea barato (cortes bruscos, texto grande, ruido visual).
- Que quite al usuario el control de su home de forma difícil de revertir.

## Restricciones

- Kotlin nativo con Jetpack Compose for TV (`androidx.tv`).
- Perfil de arranque (baseline profile) y build release para medir fluidez.
- Imágenes de reposo pre-redimensionadas a 1920x1080 (WebP/JPEG), sin desenfoque en tiempo real.
- Debe declarar categorías de home/launcher de Leanback/Android TV para poder ser launcher por defecto.
- Instalación y activación como home por ADB (la TV ya está conectada por depuración inalámbrica).
- Cerrar apps: Android no permite que un launcher normal fuerce el cierre de otro app. Opciones: permiso concedido por ADB una vez, o abrir la pantalla de "Forzar detención". Decisión de implementación abajo.

## Enfoques considerados

| | Enfoque | Compromiso |
|---|---|---|
| **A (elegido)** | Kotlin + Compose for TV | Máximo control visual y animaciones suaves; riesgo de rendimiento en hardware modesto, mitigado con imágenes pre-redimensionadas, sin blur en vivo y baseline profile |
| B | Kotlin + vistas clásicas (Leanback/RecyclerView) | Más liviano, pero rígido para animar y Leanback está en desuso; resultado más "estándar" |
| C | Web en WebView | Descartado: el más pesado |

## Decisiones tomadas

Del usuario:
- Enfoque A (Compose for TV).
- Disposición A del inicio: dos filas centradas, accesos y hora arriba.
- Reposo con obras de la National Gallery.
- Incluir ordenar apps; dejar abierta la opción de un menú de ajustes.

Del asistente (marcadas como supuestos, corregibles):
- El reposo ocurre **dentro del launcher** (no como protector de pantalla del sistema).
- Las 20 obras las elige el asistente y viajan dentro del app.
- Pocas opciones: solo las pedidas, sin extras.

## "Seguir viendo": estado y plan

- Intención: mostrar la fila real de **Watch Next** del sistema (título, imagen, progreso) y abrir el contenido directo en el app.
- Verificado: `READ_EPG_DATA` es de nivel `normal`; Projectivy solo tiene ese permiso y aun así muestra la fila. Una consulta directa vía ADB devolvió vacío, pero ADB no es un launcher, así que no concluye nada.
- **Pendiente:** confirmar con una prueba mínima en la TV que un launcher normal puede leer Watch Next. Esa prueba es el primer paso del plan.
- Respaldo si no funciona: "vistos recientemente" propio (últimos apps abiertos, sin título ni progreso).

## Decisiones del asistente tras la revisión (supuestos, corregibles)

- **Respaldo de "Seguir viendo":** el propio launcher registra los apps que abre (sin permisos especiales). Las tarjetas de respaldo muestran icono + nombre del app. Si hay contenido real de Watch Next, se mezclan tarjetas de varios apps con un tope (~10). Si no hay nada, la fila se oculta en vez de mostrar un vacío.
- **Recuperación:** nunca se deshabilita el home original de Google TV; el launcher se activa como predeterminado y se entrega un script ADB de reversión. Ajustes del launcher incluye "Volver al launcher original". Si el launcher falla repetidamente al arrancar, se rinde y deja pasar al original.
- **Cerrar apps:** se usa `killBackgroundProcesses` (permiso normal, sin ADB). Como el launcher está al frente, los demás apps están en segundo plano y quedan detenidos. No es un "Forzar detención" completo; es un cierre práctico que sirve para liberar memoria.
- **Apps ocultos:** el acceso "＋" muestra todos los apps, con los ocultos atenuados y una opción para volver a mostrarlos. Esa pantalla es una cuadrícula simple con modo de edición (mover, ocultar, desinstalar).
- **Primer arranque:** el orden inicial pone primero los apps de streaming que usas (Netflix, YouTube, Prime Video, Apple TV, HBO Max…), el resto va a "＋", y los apps nuevos se añaden al final.
- **Accesos superiores:** se verifica por ADB cada pantalla de sistema (Wi-Fi, Bluetooth) en tu Onn; si alguna no existe, abre los ajustes generales. "Ajustes del launcher" es una pantalla mínima real desde el día uno.
- **Reposo:** mantiene la pantalla encendida solo mientras el launcher está al frente. El temporizador se reinicia al volver de un app. La primera pulsación solo despierta (no actúa). Tras **20 minutos** seguidos de arte (decisión del usuario), se atenúa a negro para no dejar una imagen fija ni gastar de más; cualquier botón lo despierta.
- **Acciones sobre apps (decisión del usuario):** mantener OK en un app de la fila de inicio lo **cierra** directo. Ocultar, mover y desinstalar se hacen en el modo de edición de la pantalla "＋"; la fila de inicio queda limpia.
- **Arte:** solo obras con la marca "open access" del dataset; ficha con título, artista y año.
- **Rendimiento:** meta de arranque en frío menor a 1 s y sin tirones visibles; baseline profile desde el inicio.

## Lo que se deja al constructor

- Estructura de módulos y gestión de estado.
- Diseño visual fino (tipografía, colores, curvas de animación, tiempos del zoom y del fundido).
- Tiempo exacto de inactividad para entrar al reposo.
- Elección concreta de las 20 obras y su pipeline de preparación (descarga IIIF, redimensionado, atribución).
- Mecanismo exacto para cerrar apps (ADB vs. pantalla de sistema).
- Detalles del modo de reordenar (gesto, feedback visual) y de la persistencia del orden y los apps ocultos.
- Iconos minimalistas monocromos: estilo y fuente.
