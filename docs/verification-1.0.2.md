# Velocity 1.0.2 — revisada: imágenes dentro de GIFs y Stickers

Esta revisión sustituye la APK anterior `2000063`, cuyos accesos a páginas confundían el flujo de selección.

Fecha: **8 de noviembre de 2026 (UTC)**. `versionName=1.0.2`, `versionCode=2000066`; la APK debug lleva sufijo `-verification`.

## Descarga

[**Descargar ZIP con APK 1.0.2 y SHA-256**](https://github.com/ivanezequiel545-creator/Sonic-keyboard/actions/runs/42511062874/artifacts/13628988613).

Artefacto **`velocity-1.0.2-apk`**, aproximadamente 5,1 MB, con retención de 30 días. El repositorio ya es público por decisión de su propietario; Actions todavía puede pedir iniciar sesión para descargar. descomprime e instala **`velocity-1.0.2-verification.apk`**. Su checksum está en el mismo ZIP. Conserva tu teclado anterior durante la comprobación.

El código de versión permite actualizar la anterior **si coincide la firma**. La clave debug depende de la caché de Actions; no se promete que coincida entre todas las compilaciones. Si Android rechaza la actualización por firma, **no desinstales sin exportar tus ajustes**: desinstalar borra los datos privados, incluidos archivos importados/foto que no hayas guardado por separado.

## Evidencia comprobada

[CI **42511062874: SUCCESS**](https://github.com/ivanezequiel545-creator/Sonic-keyboard/actions/runs/42511062874), código **`ecfa3cd92f18dff93f2e0e37d65a44cfc6ae9203`**. Los cambios posteriores son exclusivamente documentación.

- **17 pruebas Node**, incluida igualdad del catálogo web/nativo, fuentes/atribución, allowlist de URLs, formato y límites de descarga con respuestas controladas.
- **18 pruebas Chromium**, incluidas personalización, privacidad, sugerencias, emojis, originales offline y selección externa: ninguna petición de imagen previa al consentimiento, seis referencias GIF atribuidas, descarga de un archivo real de prueba, créditos plegados, vuelta al panel sin repetir consentimiento, ausencia de nuevas pestañas del navegador y ocultación en privado.
- APK principal y de instrumentación compiladas; **8 unidades Java** y lint de la app correctos.
- **Android 10 / API 29:** suite nativa completa correcta. Incluye escritura/centrado, catálogo y colores, sugerencias/completar/corregir/deshacer, URI de originales/importados y la selección externa. Las pruebas nuevas comprueban catálogo/URLs/caché/cancelación/límites, cierre explícito al finalizar input sin depender de una notificación de detach, y recepción de **GIF y WebP en otra aplicación**, con lectura de cabecera y permiso temporal de URI. La app receptora rechaza cualquier `linkUri`: esta APK entrega el archivo sin URL de página adjunta. También se comprueba que los créditos se abren/cerran dentro del panel y que Stickers carga automáticamente después de autorizar GIFs.
- **Android 12 / API 31:** pasa separadamente la regresión de escribir `hola`, ocultar, reabrir sin entrar a Velocity y escribir otra vez, verificando `holahola`. Se desactiva realmente la accesibilidad y se afirma que está apagada antes de inyectar pulsaciones. La configuración de servicios se guarda/restaura; no se ha eliminado ni saltado esta comprobación para pasar CI. En API 29 ese caso específico se marca no aplicable porque su API de automatización requiere Android 12.
- `npm audit --omit=dev` local: **0 vulnerabilidades**; sin cambios de dependencias posteriores.

[**Informes Android 10 y 12**](https://github.com/ivanezequiel545-creator/Sonic-keyboard/actions/runs/42511062874/artifacts/13629626718), retención de 14 días.

## Cómo usarlo

1. En un campo normal de una app compatible, abre **☺ → GIFs / Stickers**.
2. La primera vez pulsa **Cargar selección en línea**; se recuerda para ambos paneles hasta desactivarla. Después las miniaturas aparecen directamente al abrir GIFs o Stickers: seis referencias de GIPHY/famosos o tres del pack G0VY8R de Sticker.ly, con fuente y autor identificado.
3. Toca la miniatura para enviar la imagen, sin abrir el navegador ni enviar un enlace. Ya no hay botones «Abrir página»/«Ver fuente» en las tarjetas nativas. Los créditos están aparte dentro del teclado; mantener pulsado ofrece compartir explícitamente. Los stickers son imágenes, no packs instalables de WhatsApp/Telegram. Las miniaturas GIF nativas son estáticas, pero no se convierte el archivo enviado en PNG.
4. Puedes ocultar la selección y usar el pack propio sin conexión. **Ajustes nativos → Borrar caché de selección en línea** limpia sus archivos y copias de envío; los importados se gestionan por separado.

La carga externa se bloquea en privado/campos protegidos. El proveedor recibe peticiones de imagen/IP, no el texto del editor. No se incluyen imágenes de terceros en la APK ni se declaran libres de derechos. [Fuentes, derechos y límites](releases/1.0.2.md).

## Correcciones detectadas al ampliar la verificación

- Android puede ocultar el IME sin desprender su vista: el panel ahora se cierra y cancela trabajo también desde `finishEditor`, no únicamente desde `onDetachedFromWindow`.
- Mientras se recalculaban sugerencias podían quedar etiquetas antiguas que parecían activas aunque el servicio ya las había invalidado. Ahora se conservan visualmente, pero están desactivadas y se anuncian como actualizándose hasta recibir el resultado válido; no se reconstruyen las letras.
- Deshacer autocorrección no depende ya de offsets almacenados en notificaciones de selección que pueden llegar tarde. Comprueba la selección actual del editor y el contenido esperado antes de sustituir texto. Se conserva la prueba real de completar, corregir y deshacer, sin relajar sus resultados esperados.

## Pendiente antes de producción

**Comprobación real de las fuentes:** en esta ejecución el runner descargó correctamente los **seis GIF de GIPHY y los tres WebP de Sticker.ly** usando el descargador con validación de MIME/cabecera y límite de 4 MB. Resultado registrado como avisos «Media source availability» y en `media-sources.json` dentro de los informes. Los archivos externos no se guardan como parte de la APK.

La entrega instrumentada sigue usando archivos controlados, separadamente de ese chequeo real. La descarga correcta desde el runner no garantiza disponibilidad futura, acceso desde la red del teléfono, CORS del navegador ni compatibilidad de cada chat. Falta comprobar en el teléfono la carga/animación/recepción y las condiciones de uso. Si una fuente falla se informa y se conserva el pack propio, sin abrir una página automáticamente.

Sigue siendo una APK de **verificación**, no una publicación estable de producción. Falta aceptación física de este código y firma persistente protegida; [procedimiento](stable-release.md). No se han sobrescrito releases anteriores ni fusionado el PR. La visibilidad pública fue elegida y cambiada por el propietario.
