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
    @PluginMethod public void getStatus(PluginCall call) {
        JSObject result = new JSObject();
        result.put("stage", BuildConfig.PREVIEW_STAGE);
        result.put("version", BuildConfig.VERSION_NAME);
        result.put("imeAvailable", BuildConfig.PREVIEW_STAGE >= 3);
        call.resolve(result);
    }
    @PluginMethod public void openSetup(PluginCall call) {
        if(BuildConfig.PREVIEW_STAGE < 2) { call.reject("La vista nativa requiere Alpha 2 o Alpha 3."); return; }
        getActivity().runOnUiThread(() -> {
            getActivity().startActivity(new Intent(getActivity(),KeyboardSetupActivity.class));
            call.resolve();
        });
    }
    @PluginMethod public void configure(PluginCall call) {
        if(BuildConfig.PREVIEW_STAGE < 2) { call.resolve(); return; }
        SharedPreferences.Editor edit=KeyboardPreferences.get(getContext()).edit();
        String theme=call.getString("theme"), hand=call.getString("hand");
        if(KeyboardPreferences.validTheme(theme)) edit.putString("theme",theme);
        if(KeyboardPreferences.validHand(hand)) edit.putString("hand",hand);
        Boolean sound=call.getBoolean("sound"), haptics=call.getBoolean("haptics");
        if(sound != null) edit.putBoolean("sound",sound);
        if(haptics != null) edit.putBoolean("haptics",haptics);
        edit.apply(); call.resolve();
    }
}
