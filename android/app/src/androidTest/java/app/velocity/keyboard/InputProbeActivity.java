package app.velocity.keyboard;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

/** Only included in the test APK, not in any distributed release. */
public class InputProbeActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,20,20,20);
        EditText text = new EditText(this); text.setContentDescription("external-probe-text"); text.setHint("Texto"); text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); root.addView(text,new LinearLayout.LayoutParams(-1,120));
        EditText number = new EditText(this); number.setContentDescription("external-probe-number"); number.setHint("Número"); number.setInputType(InputType.TYPE_CLASS_NUMBER); root.addView(number,new LinearLayout.LayoutParams(-1,120));
        EditText password = new EditText(this); password.setContentDescription("external-probe-password"); password.setHint("Privado"); password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); root.addView(password,new LinearLayout.LayoutParams(-1,120));
        // This test host explicitly requests its IME, as a messaging app does.
        // Do not depend on Android's remembered soft-input visibility between
        // password, numeric and text editors or on accessibility click timing.
        for (EditText editor : new EditText[]{text, number, password}) {
            editor.setOnClickListener(view -> {
                view.requestFocus();
                view.post(() -> ((android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(view,
                    android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT));
            });
        }
        setContentView(root);
    }
}
