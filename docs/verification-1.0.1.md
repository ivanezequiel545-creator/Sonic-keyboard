# Verificación de Velocity 1.0.1

Fecha: **8 de noviembre de 2026 (UTC)**.

> **Historial de la revisión inicial.** Esta APK precede al fallo de cierre reportado en teléfono. Las pruebas de UiAutomator mantenían accesibilidad activada y no cubrían el escenario sin ese servicio. No usar este artefacto como descarga de la revisión corregida. [Descargar y verificar la revisión corregida](verification-1.0.1-revision.md).

## Resultado comprobado

El [run 42475469866](https://github.com/ivanezequiel545-creator/Sonic-keyboard/actions/runs/42475469866) terminó con **SUCCESS**, sobre `6f63b73fa64e6592ebc857470d1022785b64748a` (9 min 5 s).

- **13 pruebas Node**: preferencias/migración, datos y sugerencias españolas, traducción, exportación, ZIP y borrado de secuencias emoji completas.
- **16 pruebas Chromium**: interfaz, personalización, foto, categorías/favoritos/privacidad, recursos offline y sugerencias con el cursor en medio de texto. La selección se aplica al confirmar el render, sin callbacks de cursor atrasados que modifiquen la siguiente edición.
- Compilación de APK, **8 pruebas unitarias Java** y lint Android correctos. El vocabulario nativo contiene 12.000 palabras.
- **5 pruebas instrumentadas Android 10**: servicio protegido; escritura, emojis, cursor y borrado en otra aplicación; catálogo/privacidad; reutilización del árbol de teclas, margen y centrado real del glifo; sugerencias, autocorrección y deshacer en otra app.
- La nueva prueba escribe `holaa` pulsando las teclas del IME, toca `hola`, corrige `grasias` al pulsar espacio, deshace con ⌫, verifica escritura literal al desactivar autocorrección y comprueba ausencia de sugerencias en número/contraseña.
- El centrado se mide con el ancho del glifo y del botón, no solo con la propiedad de gravedad: el modo de una línea ya no crea un layout horizontal de ancho enorme.
- Se comprueba que cambian los tres botones de sugerencias sin recrear las teclas. Un evento de accesibilidad con `CONTENT_CHANGE_TYPE_SUBTREE` invalida los nodos hermanos antiguos; las consultas de contexto se ejecutan fuera del hilo de UI del IME.

No se eliminó ni desactivó ninguna prueba de Velocity para conseguir el resultado. Instrumentación limitada a `:app:connectedDebugAndroidTest`, no a las aplicaciones de prueba de dependencias. Los diagnósticos internos debug contienen únicamente conteos, no palabras; no se escriben en logs ni preferencias.

## Descargar e instalar

[Descargar **velocity-1.0.1-apk**: ZIP con APK y SHA-256](https://github.com/ivanezequiel545-creator/Sonic-keyboard/actions/runs/42475469866/artifacts/13615387514).

Repositorio privado: inicia sesión en GitHub con acceso al repositorio. Artefacto de aproximadamente 4,7 MB, retención de 30 días. Descomprime e instala **`velocity-1.0.1-verification.apk`**. Abre Velocity y usa **Activar mi teclado**. Conserva tu teclado anterior. Los ajustes nativos permiten activar/desactivar sugerencias y autocorrección por separado.

Es una APK **debug de verificación**, no una release estable de producción. Un cambio de firma puede impedir actualizar una alpha instalada; exporta tus datos antes de considerar desinstalarla. Si expira el artefacto, vuelve a ejecutar el workflow sobre el commit validado.

[Informes de Android](https://github.com/ivanezequiel545-creator/Sonic-keyboard/actions/runs/42475469866/artifacts/13615377502).

## Pendiente para publicación estable

Faltan firma protegida de producción y revisión física del teléfono donde se reportó el centrado. El emulador no demuestra compatibilidad con todos los fabricantes ni comodidad real. Ver [lista de validación y publicación](stable-release.md). No hay release `v1.0.1` publicada, no se han sobrescrito las alpha y el repositorio continúa privado.

Los cambios documentales posteriores al commit indicado no cambian el código ni la APK comprobados en este informe.
