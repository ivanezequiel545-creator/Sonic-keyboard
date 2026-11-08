## Alpha 3 · Tu teclado en otras apps ⚡

Tercer prelanzamiento: **incluye un teclado Android real (`InputMethodService`)**. Descarga **velocity-android.apk** desde Assets. No es solamente una web envuelta en un APK.

### Activar (decisión explícita del usuario)
1. Instala el APK en Android 6+ (mantén WebView actualizado para las herramientas de la app).
2. Abre Velocity → **Activar mi teclado**.
3. Toca **1 · Activar en Ajustes de Android** y habilita Velocity. Lee la advertencia estándar de Android para todos los teclados.
4. Regresa y toca **2 · Elegir Velocity como teclado**.
5. Abre un campo de texto en Notas, el navegador, WhatsApp u otra aplicación compatible. Algunas apps y pantallas del sistema restringen teclados externos. No es una extensión de teclado para iPhone.

### Qué funciona en el teclado del sistema
- Escritura QWERTY española con ñ; mantén una vocal para á/é/í/ó/ú. Ü está en ?123.
- Mayúscula de una pulsación; mantén ⇧ para bloquear mayúsculas.
- Espacio, borrado, números, símbolos y emojis. Borrado por punto de código Unicode (no segmentación completa de grafemas compuestos).
- Seis temas originales con fondo azul/órbitas y modo de una mano en ambos lados.
- Diseño adaptable en horizontal/vertical; sonido/vibración opcionales según dispositivo.
- Acciones del campo: siguiente, buscar, enviar, listo o nueva línea según lo que solicite la aplicación.
- Adaptación a campos numéricos y contraseñas. En contraseñas no se consultan contenidos del campo ni se ofrece el panel de emojis.
- ◎ permite elegir otro teclado; ⚙ abre ajustes; ⌄ oculta Velocity.

### Límites de esta alpha
El traductor, las notas y el dictado son herramientas de la aplicación, **no están integrados en el teclado nativo**. No incluye autocorrección predictiva, escritura por deslizamiento ni diccionario aprendido. La compatibilidad con todos los campos y fabricantes no está garantizada. No debe usarse como único teclado sin probarlo antes. Mantén instalado tu teclado anterior.

### Privacidad y seguridad
El IME no guarda, registra ni transmite pulsaciones. Solo persiste preferencias visuales, de sonido y vibración. La app contenedora conserva permiso de internet para las herramientas web/enlaces; el código del servicio IME no utiliza la red. Ningún teclado debe activarse sin confiar en su origen. No se solicita permiso para leer contraseñas, micrófono, contactos ni almacenamiento general. Las notas web no están cifradas.

### Instalación, comprobación y reversión
Repositorio privado: inicia sesión en GitHub con acceso. APK de depuración firmado para pruebas, no Play Store. Los tres APK de esta compilación se actualizan en orden y comparten firma. Una firma distinta de un APK anterior puede exigir desinstalarlo primero (exporta tus ajustes; desinstalar elimina sus datos).

Comprueba `sha256sum -c velocity-android.apk.sha256`. Si autorizas instalar desde tu navegador, revoca ese permiso después.

Para volver al teclado anterior, usa ◎ o Ajustes de Android → Idiomas y entrada → Teclado en pantalla. Desactiva Velocity allí antes de desinstalarlo si no deseas seguir usándolo.

La publicación se realiza solo después de compilar las tres variantes, pruebas unitarias/lint y una prueba instrumentada con un campo de texto de una app de prueba separada en emulador Android 10. Esto no sustituye las pruebas manuales en un teléfono real ni implica una certificación de WhatsApp u otras apps.

Diseño original bajo Unlicense. Sin personajes, imágenes o marcas de Sonic/SEGA ni afiliación con ellos.
