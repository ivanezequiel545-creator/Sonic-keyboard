# Velocity 1.0.1 revisada — cierre al escribir, colores y multimedia

Fecha: **8 de noviembre de 2026 (UTC)**. Se mantiene `versionName=1.0.1`; la nueva APK de verificación tiene `versionCode=2000054` y sufijo `-verification`.

## Evidencia comprobada

[CI 42479260908](https://github.com/ivanezequiel545-creator/Sonic-keyboard/actions/runs/42479260908): **SUCCESS**, 10 min 28 s de ejecución del job, sobre **`0d74bc5f0cb600565bb71ceec0d79287cfd16e08`**. Los cambios posteriores son exclusivamente documentación.

- **14 pruebas Node**: datos/preferencias, sugerencias y el nuevo pack original, idéntico en web y Android; PNG transparentes y GIF con bucle real.
- **17 pruebas Chromium**: personalización, texto/cursor, privacidad, sugerencias, emojis, offline y descarga del PNG/GIF real sin conexión incluso antes de abrir el panel por primera vez. Comprobación local adicional del panel GIF a 390 px: sin desbordamiento horizontal.
- APK principal y APK de instrumentación compiladas antes de arrancar los emuladores; **8 unidades Java** y lint de la app correctos.
- **Android 10 / API 29: 7 pruebas instrumentadas pasadas**. Incluyen las cinco pruebas anteriores, envío de PNG/GIF reales a otra aplicación con lectura de URI concedida y validación de cabecera MIME, importación acotada/rechazo de archivos inválidos o demasiado grandes, limpieza, compatibilidad MIME y contraste del panel.
- En API 29 se marca una prueba como no aplicable: la bandera pública de automatización sin accesibilidad requiere Android 12. **Esa misma prueba se ejecuta y pasa separadamente en Android 12 / API 31**; no se ha quitado para obtener un resultado verde.
- El caso API 31 desactiva realmente la accesibilidad, comprueba que está desactivada, inyecta pulsaciones táctiles para `hola`, oculta el IME, vuelve a abrirlo en la aplicación externa y escribe de nuevo, verificando `holahola` y el teclado todavía visible. No abre la actividad de Velocity para recuperarlo.
- `npm audit --omit=dev` local: **0 vulnerabilidades**.

[Informes de los dos emuladores](https://github.com/ivanezequiel545-creator/Sonic-keyboard/actions/runs/42479260908/artifacts/13616843860).

## Por qué la revisión anterior no detectó el cierre

La barra enviaba directamente un evento de accesibilidad sin comprobar si ese servicio estaba activado. Las pruebas de UiAutomator activan accesibilidad, por lo que no ejercitaban el escenario normal de un teléfono sin ese servicio. Ahora se usa el método comprobado del framework (`sendAccessibilityEvent`) y se asigna `CONTENT_CHANGE_TYPE_SUBTREE` al inicializar el evento. Se conserva la actualización de los tres candidatos sin enviar un evento inválido cuando accesibilidad está apagada. La regresión dedicada cubre tanto escritura como reapertura en ese estado.

Durante la revisión también se detectó y corrigió un redondeo de alfa en las mezclas de color de Android: las superficies del panel se fuerzan a opacas antes de calcular contraste. La prueba comprueba opacidad y contraste, además del envío real desde el panel.

Este resultado automático no demuestra compatibilidad universal ni reemplaza la prueba en el teléfono donde se reportó el fallo.

## Descargar esta APK, no la inicial

[**Descargar ZIP de la 1.0.1 revisada, con APK y SHA-256**](https://github.com/ivanezequiel545-creator/Sonic-keyboard/actions/runs/42479260908/artifacts/13616808978).

Artefacto **`velocity-1.0.1-apk`**, aproximadamente 5,1 MB, privado y con retención de 30 días. Inicia sesión en GitHub, descomprime e instala **`velocity-1.0.1-verification.apk`**. El número visible sigue siendo 1.0.1; el código Android superior permite actualizar la APK anterior si la firma coincide. No desinstales sin exportar tus ajustes si Android indica un problema de firma. Conserva tu teclado anterior mientras compruebas la revisión.

### Funciones añadidas

- Colores del panel de emojis basados en el tema elegido, categoría seleccionada diferenciada y texto de contraste calculado. No se cambian los glifos Unicode de la fuente del teléfono.
- **☺ → GIFs / Stickers**: 4 GIF animados y 6 PNG transparentes, originales y offline. Las miniaturas nativas son estáticas para ahorrar batería; el archivo enviado sigue siendo un GIF animado.
- Toca para enviar a una app que anuncie el MIME compatible. Mantén pulsado para compartir explícitamente mediante Android. Si la app no admite la imagen, se avisa en vez de insertar una ruta falsa en el texto. Los stickers son imágenes, no packs instalables de WhatsApp/Telegram.
- **Ajustes nativos → Importar mi GIF o sticker**: PNG/WebP/GIF, hasta 12 archivos, 4 MB por archivo, 4096 px por lado y 16 millones de píxeles como máximo. Copia privada, sin acceso general a la galería ni búsqueda remota. Puedes borrar los importados y sus copias en caché. Los archivos compartidos conservan sus metadatos originales.
- GIFs/stickers desactivados en campos protegidos y modo privado del IME. En la web el pack se descarga/comparte, sin fingir que su editor de texto recibe imágenes.

## Estado de publicación

Sigue siendo una APK firmada para **verificación**, no la estable de producción. Falta confirmar el resultado en el teléfono afectado y disponer de la firma de producción protegida. [Revisión física y procedimiento de publicación](stable-release.md). No se ha publicado `v1.0.1`, no se han sobrescrito las alpha ni se ha cambiado la visibilidad privada del repositorio.
