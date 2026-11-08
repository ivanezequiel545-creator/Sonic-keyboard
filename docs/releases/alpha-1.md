## Alpha 1 · La aplicación

Primero de tres prelanzamientos. Descarga **velocity-android.apk** desde Assets. Android 6+ con WebView actualizado. Es una compilación de depuración firmada para pruebas, no una publicación de Play Store.

Incluye las herramientas web: seis temas originales, teclado de prueba, emojis, notas, una mano, traducción básica y exportación ZIP/JSON mediante el menú nativo de compartir. Los recursos web están incluidos para uso sin conexión.

**Esta alpha NO es un teclado del sistema.** No se puede activar para escribir en otras apps. Alpha 2 añade una prueba de interfaz nativa; **Alpha 3** incorpora el servicio IME real.

### Instalar y actualizar
- Repositorio privado: inicia sesión en GitHub con una cuenta que tenga acceso.
- Abre Assets y descarga el APK en Android. Autoriza temporalmente instalar desde tu navegador si Android lo solicita y revoca ese permiso después.
- Los tres APK tienen el mismo identificador y una firma de prueba compartida dentro de esta compilación: son etapas sucesivas, no tres apps independientes. Instala en orden ascendente o ve directamente a Alpha 3.
- Si Android rechaza una actualización por firma distinta del APK antiguo, exporta primero tus ajustes, desinstala la versión antigua e instala la nueva. Desinstalar borra los datos locales.
- Comprueba la integridad con `sha256sum -c velocity-android.apk.sha256` en la misma carpeta que el APK. La firma de prueba no equivale a una auditoría de seguridad.

Arte original bajo Unlicense; sin personajes o recursos de Sonic/SEGA. El dictado depende del navegador; enlaces externos requieren internet. No hay cuentas ni analítica propia.
