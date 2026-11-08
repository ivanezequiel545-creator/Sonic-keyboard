package app.velocity.keyboard;

import android.content.Intent;
import android.content.SharedPreferences;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "VelocityKeyboard")
public final class KeyboardBridgePlugin extends Plugin {

  @PluginMethod
  public void getStatus(PluginCall call) {
    JSObject result = new JSObject();
    result.put("stage", BuildConfig.PREVIEW_STAGE);
    result.put("version", BuildConfig.VERSION_NAME);
    result.put("imeAvailable", BuildConfig.PREVIEW_STAGE >= 3);
    call.resolve(result);
  }

  @PluginMethod
  public void openSetup(PluginCall call) {
    if (BuildConfig.PREVIEW_STAGE < 2) {
      call.reject("La vista nativa requiere Alpha 2 o Alpha 3.");
      return;
    }
    getActivity().runOnUiThread(() -> {
      getActivity().startActivity(
        new Intent(getActivity(), KeyboardSetupActivity.class)
      );
      call.resolve();
    });
  }

  @PluginMethod
  public void configure(PluginCall call) {
    if (BuildConfig.PREVIEW_STAGE < 2) {
      call.resolve();
      return;
    }
    SharedPreferences.Editor edit = KeyboardPreferences.get(
      getContext()
    ).edit();
    String theme = call.getString("theme"),
      hand = call.getString("hand");
    if (KeyboardPreferences.validTheme(theme)) edit.putString("theme", theme);
    if (KeyboardPreferences.validHand(hand)) edit.putString("hand", hand);
    for (String key : KeyboardPreferences.BOOLEAN_KEYS) {
      Boolean value = call.getBoolean(key);
      if (value != null) edit.putBoolean(key, value);
    }
    for (String key : KeyboardPreferences.INTEGER_KEYS) {
      Integer value = call.getInt(key);
      if (value != null) edit.putInt(
        key,
        KeyboardPreferences.clamp(key, value)
      );
    }
    for (String key : new String[] {
      "backgroundColor",
      "keyColor",
      "accentColor",
    }) {
      String value = call.getString(key);
      if (KeyboardPreferences.validColor(value)) edit.putString(key, value);
    }

    edit.apply();
    call.resolve();
  }

  @PluginMethod
  public void setBackground(PluginCall call) {
    String data = call.getString("data");
    if (data == null) {
      KeyboardBackground.clear(getContext());
      call.resolve();
      return;
    }
    if (data.length() > 3 * 1024 * 1024) {
      call.reject("Imagen demasiado grande");
      return;
    }
    EmojiRepository.IO.execute(() -> {
      try {
        byte[] bytes = android.util.Base64.decode(
          data,
          android.util.Base64.DEFAULT
        );
        try (
          java.io.ByteArrayInputStream stream =
            new java.io.ByteArrayInputStream(bytes)
        ) {
          KeyboardBackground.save(getContext(), stream);
        }
        call.resolve();
      } catch (Exception e) {
        call.reject("No se pudo guardar el fondo");
      }
    });
  }
}
