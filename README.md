# Velocity ⚡

Un teclado web móvil, personalizable y en español. Creado desde cero sobre este repositorio con React y Vite, sin backend, cuentas, anuncios ni claves de API.

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

## Lo que no es

**La web y el APK no son un teclado del sistema (IME) de Android o iOS.** Una web no puede convertirse en el teclado del sistema para escribir en WhatsApp, Telegram u otras apps. El área de prueba y las herramientas funcionan dentro de Velocity; el texto se puede copiar a otra aplicación.

Instalar la PWA solo añade esta web a la pantalla de inicio. El APK incluye esta interfaz en una aplicación Android con Capacitor, pero tampoco sustituye el teclado del sistema. Para un teclado nativo, se incluye un enlace al proyecto independiente [HeliBoard](https://github.com/Helium314/HeliBoard), para Android. No hay integración ni afiliación con ese proyecto.

Los ZIP son fondos y configuraciones, no módulos ejecutables ni traductores completos. Las sugerencias usan reglas locales, no un modelo de autocorrección. El dictado, la vibración, la instalación y el portapapeles dependen de las capacidades y permisos del navegador. El dictado puede requerir conexión y procesamiento por el proveedor del navegador.

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

## APK Android

La sección Inicio, la sección Descargas y la ayuda de instalación incluyen **Descargar APK** y un enlace a la versión `android-preview` de GitHub. El repositorio es privado: el visitante debe iniciar sesión en GitHub y tener acceso. No se incluyen tokens ni credenciales en el sitio.

El workflow `.github/workflows/android-apk.yml` compila la rama de esta sesión en GitHub Actions con Node 22, Java 21 y Android SDK 35. Publica un APK **de depuración firmado para pruebas**, un checksum SHA-256 y un artefacto de Actions. No es un paquete de Play Store ni una compilación release de producción. El botón de descarga necesita que ese workflow termine correctamente; si Actions está bloqueado por permisos, cuotas o facturación, se debe resolver en la configuración del repositorio.

- Requisitos del teléfono: Android 6.0+ y Android System WebView actualizado.
- El APK contiene los recursos web y funciona sin conexión desde el primer inicio. No registra el service worker dentro de la app nativa.
- No ofrece un servicio InputMethodService ni pide que lo actives como teclado del sistema.
- Para instalarlo, abre la versión en GitHub desde el teléfono, descarga `velocity-android.apk` y autoriza temporalmente la instalación desde tu navegador si Android lo solicita. Revoca ese permiso después.
- Para guardar ZIP/JSON desde Android se usa `Filesystem` en la caché privada y el menú nativo `Share`, en vez de enlaces `blob:` que WebView no descarga. Los archivos temporales pueden eliminarse al limpiar la caché. No se solicitan permisos generales de almacenamiento.
- Las notas y ajustes permanecen en el almacenamiento local de la app; desinstalarla los borra. La copia de seguridad automática de Android está desactivada. La web y el APK no sincronizan datos entre sí.
- El dictado depende de Web Speech API y puede no existir en WebView; la app lo indica sin fingir soporte.
- La firma de prueba se conserva en caché de Actions cuando está disponible. Si cambia, puede ser necesario desinstalar antes de actualizar. Para producción se debe configurar una clave de firma propia mediante secretos, nunca en Git.

### Compilar localmente

Instala Android Studio, Android SDK 35 y Java 21, y configura `ANDROID_HOME`/`JAVA_HOME` (o `android/local.properties` para el SDK). Luego:

```sh
npm ci
npm run android:sync       # Compila la web y sincroniza Capacitor
npm run android:open       # Abre el proyecto en Android Studio
npm run android:apk        # Genera android/app/build/outputs/apk/debug/app-debug.apk
```

Los APK, keystores, recursos web copiados y archivos locales del SDK están excluidos de Git. `android/` contiene el proyecto nativo y el wrapper de Gradle necesarios para reproducir la compilación.

## Comprobar

```sh
npm test                  # Pruebas de datos, traducción, exportación y ZIP
npm run build             # Compilación de producción
npm audit                 # Auditoría de dependencias

# En una terminal: npm run dev
npx playwright install chromium
npm run test:e2e           # 8 flujos de interfaz y 1 prueba offline opcional
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

El código y los fondos propios se distribuyen con la [Unlicense](LICENSE), ya presente en el repositorio: dedicación al dominio público en la medida permitida por la legislación aplicable. Las dependencias mantienen sus licencias: React, Capacitor y Lucide (MIT/ISC según paquete), JSZip (MIT o GPL-3.0) e Inter (SIL OFL-1.1). Consulta los archivos de licencia de los paquetes instalados.
