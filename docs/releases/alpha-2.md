## Alpha 2 · Tu estilo nativo

Segundo prelanzamiento. Descarga **velocity-android.apk** desde Assets. Incluye Alpha 1 y añade una pantalla de teclado **nativa Android**, no dibujada por WebView.

- Abre «Probar teclado nativo» desde la app.
- Elige entre seis temas, modo completo/izquierda/derecha, sonido y vibración compatible.
- Prueba español con ñ, vocales acentuadas manteniendo pulsado, bloqueo de mayúsculas manteniendo ⇧, números, símbolos y emojis.
- «Aplicar estos ajustes al teclado» copia expresamente el tema y las preferencias de la web al módulo nativo. La configuración nativa también puede ajustarse de forma independiente.

**Esta etapa aún NO habilita un teclado del sistema.** Es una prueba nativa dentro de Velocity. Para escribir en otras aplicaciones instala **Alpha 3**.

### Instalación y precauciones
Android 6+, WebView actualizado. Repositorio privado: requiere acceso e inicio de sesión en GitHub. Descarga el APK desde Assets. Es una compilación de depuración firmada para pruebas, no Play Store.

Los tres prelanzamientos comparten identificador y firma dentro de la compilación: puedes actualizar Alpha 1 a Alpha 2 y después a Alpha 3. Si vienes de un APK antiguo con firma distinta, exporta los ajustes antes de desinstalarlo; desinstalar borra sus datos. La clave de prueba depende de la caché de Actions y no es una firma de producción persistente garantizada.

Comprueba el archivo con `sha256sum -c velocity-android.apk.sha256`. Autoriza la instalación desde el navegador solo si confías en esta compilación y revoca el permiso después.

El módulo nativo no guarda ni transmite pulsaciones. El texto de prueba no se persiste. Las notas web se guardan localmente sin cifrar. No uses contraseñas reales para probar. Arte original; sin recursos ni afiliación con Sonic o SEGA.
