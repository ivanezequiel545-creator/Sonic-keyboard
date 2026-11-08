import { Capacitor } from "@capacitor/core";

export const isNativeApp = Capacitor.isNativePlatform();
export const androidReleaseUrl =
  "https://github.com/ivanezequiel545-creator/Sonic-keyboard/releases/tag/android-preview";
export const androidApkUrl =
  "https://github.com/ivanezequiel545-creator/Sonic-keyboard/releases/download/android-preview/velocity-android.apk";

// Android WebView cannot download blob: URLs. Use native cache + the Android
// share sheet instead, so users can save or send the same ZIP/JSON as on web.
export async function saveDownload(blob, name) {
  if (isNativeApp) {
    const [{ Filesystem, Directory }, { Share }] = await Promise.all([
      import("@capacitor/filesystem"),
      import("@capacitor/share"),
    ]);
    const data = await new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result).split(",")[1]);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
    const file = await Filesystem.writeFile({
      path: `exports/${name}`,
      directory: Directory.Cache,
      recursive: true,
      data,
    });
    await Share.share({
      title: name,
      files: [file.uri],
      dialogTitle: "Guardar o compartir con…",
    });
    return;
  }
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = name;
  anchor.click();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}
