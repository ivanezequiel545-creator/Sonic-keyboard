# Validar y publicar 1.0.1

No se publica automáticamente una APK debug como estable. Los push de la rama de trabajo ejecutan unidades, pruebas web/offline, compilación/lint Android y pruebas de IME en otra aplicación del emulador. Si todo pasa, Actions adjunta `velocity-1.0.1-apk`, con APK de verificación y checksum. Tener estos archivos **no equivale** a haber publicado la versión estable.

## Firma de producción

El propietario debe configurar en **GitHub → repositorio → Settings → Secrets and variables → Actions**, nunca en el chat ni en Git:

- `VELOCITY_KEYSTORE_BASE64`: keystore persistente codificado en base64.
- `VELOCITY_STORE_PASSWORD`.
- `VELOCITY_KEY_ALIAS`.
- `VELOCITY_KEY_PASSWORD`.

Conserva una copia segura de la clave fuera del repositorio. Usa la clave anterior si necesitas actualización sobre una instalación existente; una clave nueva no puede sustituir su firma. No reutilices una clave debug como clave de producción. El flujo descodifica la clave en el directorio temporal del runner, no la adjunta a artefactos, y la elimina al finalizar. La conexión actual de Arena no tiene permiso para gestionar esos secretos; configúralos directamente en GitHub si aún no existen. No envíes credenciales al agente.

## Revisión física pendiente antes de publicar

Registrar modelo/Android, APK/hash y resultado. Probar en al menos un teléfono físico:

- Activar/cambiar/desactivar el IME y volver al teclado anterior.
- Actualización y firma; persistencia de ajustes y datos. Comprobar `versionCode` instalado, especialmente si procede de una alpha.
- Chat, navegador y editor; texto, selección, saltos, acciones Buscar/Enviar/Siguiente.
- Contraseña/PIN/campos sin aprendizaje: sin emojis/portapapeles sensible ni registros de recientes.
- Cómodo/Compacto/Accesible, una mano, paisaje y gestos/tres botones; margen de navegación, errores de pulsación y comodidad real.
- Centrado de letras/números/acciones en el dispositivo donde se reportó el problema y con su escala de fuente.
- Barra española: completar una palabra, corrección al espacio, deshacer con ⌫, alternar opciones, cursor a mitad de texto, evitar resultados de otro campo, privado/contraseña/email/número y filtros sin autocorrección.
- Repetición de borrado, cursor por espacio, acentos, símbolos y número; secuencias emoji completas.
- Búsqueda nativa de emojis (la consulta no llega al campo de la otra app), categorías, tonos, recientes/favoritos y modo privado.
- Foto grande/incorrecta, giro, volver de galería y restablecer; funcionamiento en avión y con WebView actualizado.

## Publicación inmutable

En Actions ejecuta **Android · Velocity 1.0.1**, seleccionando el commit/rama validado; activa `publish` y `device_verified` solo cuando hayas hecho la revisión física. El flujo falla si no se confirma la prueba o faltan secretos. Tras todas las pruebas, compila la APK release con firma protegida y crea `v1.0.1` con `velocity-1.0.1.apk` y SHA-256. No sobrescribe etiquetas alpha ni una 1.0.1 ya existente. Si cambia código tras revisar en el teléfono, repetir la validación de ese commit.

Los enlaces web conservan Alpha 3 como última APK publicada hasta completar este proceso; no apuntan a una 1.0.1 inexistente. Después de publicar, actualizar `src/platform.js` y las pruebas de enlaces para indicar la estable, manteniendo las tres alpha como historial.
