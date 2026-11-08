# Verificación de Velocity 1.0.0

Fecha: **8 de noviembre de 2026 (UTC)**.

## Resultado comprobado

El [run 42445502285](https://github.com/ivanezequiel545-creator/Sonic-keyboard/actions/runs/42445502285) terminó con **SUCCESS** sobre el código `510b349fc8a8f0e4d4acae00c34c8240a67c64ec`.

- 11 pruebas Node: preferencias/migración, datos, traducción, exportación y ZIP; las 3.979 secuencias emoji se borran completas, también con el fallback sin `Intl.Segmenter`.
- 15 pruebas Chromium: interfaz, personalización/persistencia, foto, contraste, categorías/favoritos/privacidad, pantallas de 360/390 px y recursos offline, incluido el catálogo antes de abrirlo por primera vez.
- APK Android compilada, pruebas unitarias y lint de la aplicación pasados.
- 4 pruebas instrumentadas en emulador Android 10: permiso del servicio; escritura/borrado y cambio de campos en otra aplicación, búsqueda emoji que no se escribe en el editor anfitrión, cursor por espacio y borrado repetido; catálogo completo/privacidad; reutilización del árbol de teclas y margen inferior.
- `npm audit`: 0 vulnerabilidades encontradas en la verificación local.

El comando de instrumentación está limitado a `:app:connectedDebugAndroidTest`: ejecuta todas las pruebas propias de Velocity, no las aplicaciones de tests internas de dependencias de Capacitor. No se eliminó ni desactivó ninguna prueba de Velocity para conseguir el resultado.

## Descargar e instalar la APK de prueba

[Descargar el artefacto **velocity-1.0.0-apk**](https://github.com/ivanezequiel545-creator/Sonic-keyboard/actions/runs/42445502285/artifacts/13602804295).

Es un ZIP de Actions (retención de 30 días) con `velocity-1.0.0-verification.apk` y su `.sha256`. Inicia sesión en GitHub con acceso al repositorio privado, descomprime e instala la APK; abre Velocity y toca **Activar mi teclado**. Conserva tu teclado anterior. El APK usa firma **debug** y muestra el sufijo `-verification`; **no es la release estable firmada**. Un cambio de firma puede impedir actualizar una alpha instalada: exporta tus datos antes de considerar desinstalarla.

Si el artefacto expira, vuelve a ejecutar el workflow sobre esta rama/commit. No hay una APK 1.0.0 estable publicada ni se han sobrescrito las alpha.

## Pendiente para producción

1. Configurar los cuatro secretos de firma persistente descritos en [stable-release.md](stable-release.md), directamente en GitHub, nunca en chat ni en Git.
2. Validar en teléfono físico esta versión, sus gestos/ergonomía y la actualización. El emulador no demuestra compatibilidad universal ni comodidad real en tu teléfono.
3. Solo entonces ejecutar la publicación con `publish=true` y `device_verified=true`. El flujo comprueba los requisitos, repite las pruebas y publica `v1.0.0` con APK release y SHA-256.

Las actualizaciones documentales posteriores a `510b349` no cambian el código ni la APK comprobados en este informe.
