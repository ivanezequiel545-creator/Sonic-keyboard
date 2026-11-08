package app.velocity.keyboard;

import android.content.Context;
import android.graphics.*;
import java.io.*;

/** Private bounded image, never a persisted external URI or permission to a gallery. */
public final class KeyboardBackground {

  private static Bitmap cached;
  private static long modified = -1;

  private KeyboardBackground() {}

  public static File file(Context c) {
    return new File(c.getFilesDir(), "keyboard-background.jpg");
  }

  public static synchronized Bitmap bitmap(Context c) {
    File file = file(c);
    long stamp = file.exists() ? file.lastModified() : 0;
    if (stamp != modified) {
      cached = file.exists() ? BitmapFactory.decodeFile(file.getPath()) : null;
      modified = stamp;
    }
    return cached;
  }

  public static void save(Context c, InputStream stream) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int n;
    while ((n = stream.read(buffer)) != -1) {
      bytes.write(buffer, 0, n);
      if (bytes.size() > 8 * 1024 * 1024) throw new IOException(
        "Imagen demasiado grande (máx. 8 MB)."
      );
    }
    byte[] data = bytes.toByteArray();
    BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
    if (
      bounds.outWidth <= 0 ||
      bounds.outHeight <= 0 ||
      bounds.outWidth > 30000 ||
      bounds.outHeight > 30000
    ) throw new IOException("Imagen no válida.");
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = 1;
    while (
        bounds.outWidth / options.inSampleSize > 1024 ||
        bounds.outHeight / options.inSampleSize > 1024
      )
      options.inSampleSize *= 2;
    Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length, options);
    if (image == null) throw new IOException("Formato no compatible.");
    File temporary = new File(c.getFilesDir(), "keyboard-background.tmp");
    try (FileOutputStream out = new FileOutputStream(temporary)) {
      if (
        !image.compress(Bitmap.CompressFormat.JPEG, 85, out)
      ) throw new IOException("No se pudo guardar.");
    } finally {
      image.recycle();
    }
    synchronized (KeyboardBackground.class) {
      File dest = file(c);
      if (!temporary.renameTo(dest)) {
        temporary.delete();
        throw new IOException("No se pudo sustituir el fondo.");
      }
      modified = -1;
      cached = null;
    }
  }

  public static synchronized void clear(Context c) {
    file(c).delete();
    cached = null;
    modified = -1;
  }
}
