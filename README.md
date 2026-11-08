# Velocity ⚡

Un teclado web y Android nativo, personalizable y en español. Creado desde cero sobre este repositorio con React y Vite, sin backend propio, cuentas, anuncios ni claves de API.

## 1.0.2: selección de tus páginas de GIFs y stickers

**[Descargar APK 1.0.2 de verificación y ver las pruebas](docs/verification-1.0.2.md)** · CI correcto en Android 10 y 12.

En **☺ → GIFs / Stickers**, pulsa **Cargar selección en línea** para ver seis referencias de [GIPHY/famosos](https://giphy.com/explore/famosos) y tres del pack [G0VY8R de Sticker.ly](https://sticker.ly/s/G0VY8R), con fuente y autor identificado. No son únicamente accesos al navegador: se muestran en el panel y pueden enviarse como archivos a apps compatibles. Se conservan los GIFs/stickers propios y la importación sin conexión.

La carga es explícita, requiere Internet y comunica al proveedor las peticiones de imagen/IP, no tus palabras. No es un buscador completo, no usa una clave de API ajena ni descarga HTML durante el uso. Los archivos de terceros no van dentro de la APK ni se ofrecen como libres de derechos. Su disponibilidad y condiciones dependen de sus fuentes. La web también depende de los permisos CORS del proveedor al descargar; ofrece la fuente original si falla.

La caché nativa es de hasta 16 MB, descargas de hasta 4 MB y reutilización durante siete días; puede borrarse desde ajustes. La red tiene cola separada de escritura/sugerencias, límites y cancelación al cerrar el panel. Se desactiva en privado y campos protegidos. Se mantienen las correcciones de estabilidad de la 1.0.1 revisada. [Notas y límites de la 1.0.2](docs/releases/1.0.2.md).

## 1.0.1: letras centradas y barra de palabras

Corrige la alineación observada en un teléfono real: letras, números, espacio y acciones quedan centrados horizontal y verticalmente, sin depender del tema del fabricante. La franja superior del IME ahora propone palabras en español **sin añadir altura**; las herramientas y el modo de una mano siguen en ⋯.

Incluye 12.000 palabras por frecuencia para sugerencias locales, y corrección automática conservadora de errores comunes al pulsar espacio. Toca una sugerencia para completar; ⌫ inmediatamente después de una corrección automática restaura lo que habías escrito. Ajustes nativos permiten desactivar sugerencias y autocorrección por separado. No aprende ni guarda tus palabras ni usa servicios de Gboard. No es un corrector gramatical ni promete la precisión de Gboard. Se desactiva en campos protegidos y cuando el editor impide sugerencias; tampoco autocorrige búsquedas de filtro. La PWA comparte sugerencias por prefijo; el deshacer de autocorrección corresponde al IME Android.

El vocabulario derivado de Hermit Dave/FrequencyWords/OpenSubtitles2018 conserva su **CC BY-SA 4.0**, independiente de la Unlicense del código. [Fuente, modificaciones y licencia](public/licenses/spanish-words.txt). Las dos distribuciones están versionadas; las compilaciones no descargan diccionarios ni datos de usuarios.

**Revisión de la misma 1.0.1:** corrige el cierre al escribir sin accesibilidad activa y mejora el color/contraste del panel de emojis. Incluye pestañas GIFs/Stickers, 6 stickers propios y 4 GIF animados offline, envío a apps compatibles o compartir manteniendo pulsado, e importación privada de hasta 12 PNG/WebP/GIF desde ajustes nativos (4 MB por archivo). Los stickers se envían como imágenes, no como packs instalables de WhatsApp/Telegram. No hay buscador remoto ni envío de tus pulsaciones. La web descarga/comparte el pack y también lo precarga sin conexión.

**[Descargar la APK 1.0.1 revisada y ver sus pruebas](docs/verification-1.0.1-revision.md).** CI correcto: 14 pruebas Node, 17 de navegador, unidades/lint y pruebas nativas en Android 10 más la regresión sin accesibilidad en Android 12. La [verificación inicial](docs/verification-1.0.1.md) se conserva como historial: su APK precede al cierre reportado y queda reemplazada por esta revisión.

La APK de verificación es debug. La publicación de producción 1.0.1 sigue protegida por firma persistente y confirmación de prueba física; ver [el procedimiento](docs/stable-release.md) y [las notas 1.0.1](docs/releases/1.0.1.md).

## 1.0.0: comodidad y personalización

La base de esta actualización fue la implementación **1.0.0**; no confundir el número del código con una versión ya publicada. Añade teclas ajustables, perfiles de comodidad, fila numérica, foto privada, colores, cursor por espacio, borrado continuo y **3.979 entradas Unicode 17.0 en diez categorías**. El selector nativo usa celdas recicladas y carga fuera del hilo de UI; escribir letras no reconstruye el teclado.

En la app abre **Personalizar**; en Android puedes usar también **Activar mi teclado → ajustes nativos**. El IME no guarda conversaciones ni lee automáticamente el portapapeles. Recientes/favoritos guardan únicamente IDs de emojis. Los recientes se suspenden en privado. La compatibilidad visual de emojis depende de la fuente del teléfono.

**Publicación:** la APK de verificación es debug. La release estable requiere firma persistente protegida y revisión física; [procedimiento y lista de validación](docs/stable-release.md), [notas 1.0.0](docs/releases/1.0.0.md). Los enlaces de descarga conservan las alpha publicadas hasta completar la publicación real. No se ha sustituido un enlace por una APK inexistente.

**Historial de verificación 1.0.0:** [CI correcto y APK 1.0.0 de prueba](docs/verification-1.0.0.md). Incluye 11 pruebas Node, 15 de interfaz/offline y la instrumentación del IME en otra aplicación. La APK disponible sigue siendo de verificación, no una publicación estable.

### Compilar y probar la versión actual

```sh
npm ci
npm test
npm run android:sync
npx playwright install chromium
TEST_OFFLINE=1 CI=1 npm run test:e2e
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest # requiere dispositivo/emulador
```

El catálogo se regenera antes de cada build desde `emojibase-data@17.0.0` (MIT, licencia incluida); no descarga datos durante el uso. `npm run emoji:build` permite regenerarlo separadamente. En esta versión se usa una sola variante Android; las tres variantes de la siguiente sección pertenecen a las etiquetas históricas.

## Qué incluye

- Teclado interactivo español QWERTY con `ñ`, mayúsculas, números, símbolos, vocales acentuadas, borrado, espacio y salto de línea.
- Área de prueba editable, sugerencias locales sencillas y copia al portapapeles. En celulares, `inputmode="none"` evita que el teclado nativo tape la vista al usar las teclas virtuales.
- Modo de una mano, alineado a izquierda o derecha.
- Seis temas originales: Blue Rush, Midnight, Mint Flow, Peach Club, Lilac Dream y Graphite. Búsqueda, categorías y favoritos.
- Paquetes ZIP reales: descarga todos los temas, uno o una selección. Cada tema incluye `fondo.svg` (1080 × 1920) y `theme.json`. Los ZIP incluyen instrucciones.
- Importación y exportación de configuraciones JSON validadas.
- Traducción local de 30 frases básicas ES ⇄ EN. No simula traducción de textos desconocidos: ofrece enlaces a Google Translate y DeepL, informando de que el texto se envía al servicio al abrir el enlace.
- Notas persistentes, emojis y dictado mediante Web Speech API si el navegador lo admite.
- Preferencias de sonido, vibración, tamaño de texto y sugerencias.
- Aplicación web instalable (PWA) y modo sin conexión **en la compilación de producción**, tras cargar y almacenar los recursos en una primera visita.
- Diseño adaptable, navegación móvil, ventanas con control de foco y Escape, respeto de movimiento reducido e iconos/fuente alojados localmente.

## Web, app y teclado del sistema: tres cosas distintas

**La web y Alpha 1 no sustituyen el teclado del sistema. Alpha 2 añade una vista previa nativa. Alpha 3 sí incluye un servicio de teclado Android (`InputMethodService`).**

Para escribir en otras apps de Android instala **Alpha 3**, abre Velocity → «Activar mi teclado» → activa Velocity en Ajustes de Android → selecciónalo como método de entrada. No se activa sin una decisión explícita del usuario. Funciona en campos compatibles; algunas apps y pantallas del sistema restringen teclados externos. No es un teclado para iPhone.

La PWA sigue siendo solo esta web. Los ZIP son fondos y configuraciones, no APK ni extensiones. El traductor, las notas y el dictado siguen siendo herramientas de la app; no están integrados en el IME. Las alpha no incluían predicción, aprendizaje ni escritura por deslizamiento; las sugerencias locales se añadieron en 1.0.1. Mantén instalado tu teclado anterior durante las pruebas.

## Ejecutar

Requiere Node.js 20.19+ o 22.12+ y npm.

```sh
npm ci
npm run dev
```

Vite escucha en `0.0.0.0:5173`. Las rutas de la app son relativas al origen, sin servicios de navegador que apunten a localhost. La configuración admite el host de vista previa de Arena.

### Producción e instalación

```sh
npm run build
npm run preview -- --port 5173
```

Publica el contenido de `dist/` en la raíz de un dominio con HTTPS. El manifest y el service worker usan rutas desde `/`; para un despliegue bajo una subruta se deben adaptar ambos y la configuración `base` de Vite.

- Android / Chrome: «Instalar aplicación» o «Añadir a pantalla de inicio» en el menú del navegador.
- iPhone / Safari: Compartir → «Añadir a pantalla de inicio».
- La instalación puede no ofrecerse dentro de una vista previa embebida. Abre la página en una pestaña propia y usa el menú del navegador.
- El service worker precarga HTML, JavaScript (incluido el generador ZIP), CSS, fuente e iconos. Los enlaces y servicios externos no están disponibles sin internet.
- En desarrollo no se registra un service worker, para evitar servir versiones antiguas del código.

## Historial: tres APK preliminares para Android

| Versión                                                                                          | Variante Gradle   | Alcance                                                            |
| ------------------------------------------------------------------------------------------------ | ----------------- | ------------------------------------------------------------------ |
| [Alpha 1](https://github.com/ivanezequiel545-creator/Sonic-keyboard/releases/tag/v1.1.0-alpha.1) | `webPreview`      | App con las herramientas web, sin teclado activable.               |
| [Alpha 2](https://github.com/ivanezequiel545-creator/Sonic-keyboard/releases/tag/v1.1.0-alpha.2) | `personalPreview` | Vista nativa, temas, una mano y prueba interna. IME deshabilitado. |
| [Alpha 3](https://github.com/ivanezequiel545-creator/Sonic-keyboard/releases/tag/v1.1.0-alpha.3) | `imePreview`      | IME real, activable y seleccionable para usar en otras apps.       |

Los enlaces de la web apuntan a estos prelanzamientos. **El repositorio es privado**: inicia sesión en GitHub con una cuenta que tenga acceso; no se incluyen tokens en el sitio. Las versiones aparecen solo después de que su publicación termine correctamente. La versión histórica `android-preview` sigue siendo el antiguo contenedor web, no el IME.

### Instalar y activar Alpha 3

1. Abre su versión en GitHub desde Android y descarga `velocity-android.apk` desde **Assets**.
2. Android 6.0+; para las herramientas web mantén Android System WebView actualizado.
3. Si Android lo solicita, autoriza temporalmente instalar desde tu navegador y revoca el permiso después.
4. Abre Velocity → **Activar mi teclado** → **1 · Activar en Ajustes de Android**. Lee la advertencia estándar de Android antes de aceptar.
5. Regresa → **2 · Elegir Velocity como teclado**. Abre un campo de texto de otra aplicación.
6. Para revertir, usa ◎ o selecciona tu teclado anterior en Ajustes. Desactiva Velocity antes de desinstalarlo si ya no lo quieres.

Todos los APK comparten `applicationId`, con códigos de versión ascendentes y la misma firma **de depuración dentro de una compilación**. No son tres apps instalables a la vez. Puedes ir directamente a Alpha 3. Si la firma difiere de una compilación antigua, exporta tus ajustes antes de desinstalarla: desinstalar borra sus datos. La clave de prueba se conserva solo mientras la caché de Actions lo permita; no se promete una firma de producción persistente.

Cada versión adjunta un SHA-256: `sha256sum -c velocity-android.apk.sha256`. Son APK de prueba, no de Play Store, sin garantías de compatibilidad universal. Lee las [notas de cada alpha](docs/releases/).

### El teclado nativo de las alpha históricas

`VelocityInputMethodService` usa `InputConnection`, sin WebView. Se registra con `BIND_INPUT_METHOD`; solo Alpha 3 habilita el componente. Incluye ñ, vocales acentuadas con pulsación larga, ü/símbolos en ?123, mayúsculas/bloqueo, números, emojis, seis temas, una mano a ambos lados y acciones de editor (buscar/siguiente/enviar/listo/nueva línea). Adapta la distribución a números y contraseñas, y desactiva emojis en campos privados. El borrado respeta puntos de código Unicode, no necesariamente grafemas compuestos completos.

El servicio **no guarda, registra ni transmite pulsaciones**. Solo guarda preferencias. En aquellas alpha la app mantenía permiso de Internet para sus herramientas web y el IME no usaba la red. **Desde 1.0.2, el IME sí usa Internet cuando cargas explícitamente la selección externa de GIFs/stickers**, como se explica al inicio; no envía pulsaciones. No se solicita acceso a contactos, micrófono o almacenamiento general. No hay autocorrección ni dictado nativo en estas versiones. La configuración nativa se puede editar por separado; el botón «Aplicar estos ajustes al teclado» copia expresamente tema, mano, sonido y vibración desde la interfaz web. No hay sincronización con una cuenta.

### Compilar y publicar las alpha históricas

Esta subsección describe las etiquetas `v1.1.0-alpha.*`, no el flujo actual 1.0.2. Para la versión actual utiliza los comandos del inicio y [el procedimiento estable](docs/stable-release.md).

El workflow `.github/workflows/android-apk.yml` usa Node 22, Java 21, SDK 35 y un emulador Android 10. En aquellas etiquetas compilaba tres variantes, comprueba pruebas web/offline, pruebas unitarias nativas, lint y una prueba de escritura del IME en **una aplicación de prueba separada** (incluida solo en el APK de tests). Las pruebas automatizadas no sustituyen probar en teléfonos reales.

Tras verificar la ejecución, `workflow_dispatch` con `publish=true` publica los tres APK y checksums como prelanzamientos de GitHub del commit comprobado. No reescribe tags de versiones pertenecientes a otro commit. Si Actions está bloqueado por permisos, cuota o facturación, hay que resolverlo en GitHub antes de que aparezcan archivos descargables.

Para compilar localmente, instala Android Studio, SDK 35 y Java 21, y configura `ANDROID_HOME`/`JAVA_HOME`:

```sh
npm ci
npm run android:sync
npm run android:open
npm run android:apk       # solo en las etiquetas alpha: genera tres variantes debug
# APKs: android/app/build/outputs/apk/{webPreview,personalPreview,imePreview}/debug/
cd android
./gradlew testImePreviewDebugUnitTest lintImePreviewDebug
./gradlew connectedImePreviewDebugAndroidTest # requiere emulador/dispositivo de pruebas
```

Los APK, keystores y recursos web copiados están excluidos de Git. Se adjuntan a Releases y artefactos de Actions, no al código del repositorio. Para producción configura una clave propia con secretos, nunca en Git.

## Comprobar

```sh
npm test                  # Pruebas de datos, traducción, exportación y ZIP
npm run build             # Compilación de producción
npm audit                 # Auditoría de dependencias

# En una terminal: npm run dev
npx playwright install chromium
npm run test:e2e           # Flujos de interfaz, 3 versiones y prueba offline opcional
```

Para probar la versión compilada, sirve primero `dist/` con `npm run preview -- --port 5173`:

```sh
TEST_OFFLINE=1 npm run test:e2e
```

`TEST_URL` permite usar otra URL. `CHROMIUM_EXECUTABLE_PATH` permite un ejecutable local compatible de Chromium. Los artefactos de pruebas y la compilación están ignorados por Git.

## Privacidad

Los ajustes, notas, favoritos e historial se guardan en `localStorage`, sin cifrar, únicamente en el navegador actual. No guardes contraseñas ni datos sensibles. El contenido del área de prueba y del traductor está en memoria y desaparece al recargar. Puedes borrar los datos guardados desde Ajustes. El service worker solo conserva recursos estáticos de la aplicación.

No hay servidor de traducción, telemetría ni llamadas automáticas a servicios externos. Al elegir un enlace de traducción, su URL incluye el texto introducido. Web Speech API puede procesar audio mediante los servicios del navegador, tras solicitar permiso.

## Arte, marcas y licencia

El nombre del repositorio es histórico. **No se usan imágenes, personajes, logotipos, música ni recursos de Sonic o SEGA.** El fondo azul y las ilustraciones abstractas de rayos y órbitas son diseños propios; no se afirma que material de Sonic carezca de copyright.

El código y los fondos propios se distribuyen con la [Unlicense](LICENSE), ya presente en el repositorio: dedicación al dominio público en la medida permitida por la legislación aplicable. Los datos de frecuencia españoles conservan CC BY-SA 4.0, con atribución en `public/licenses/spanish-words.txt`. Las dependencias mantienen sus licencias: React, Capacitor y Lucide (MIT/ISC según paquete), JSZip (MIT o GPL-3.0) e Inter (SIL OFL-1.1). Consulta los archivos de licencia de los paquetes instalados.
