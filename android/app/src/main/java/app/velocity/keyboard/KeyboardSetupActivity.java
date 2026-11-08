package app.velocity.keyboard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Arrays;

/** Explicit onboarding; never silently enables or selects an input method. */
public final class KeyboardSetupActivity extends Activity {
    private TextView status;
    private VelocityKeyboardView preview;
    private LinearLayout content;
    private boolean ready;
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(0xfff7f8fa);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20),dp(28),dp(20),dp(28)); scroll.addView(content);
        content.setOnApplyWindowInsetsListener((view,insets) -> {
            content.setPadding(dp(20),dp(20)+insets.getSystemWindowInsetTop(),dp(20),dp(24)+insets.getSystemWindowInsetBottom()); return insets;
        });
        setContentView(scroll);
        addText("VELOCITY · " + BuildConfig.VERSION_NAME, 12, 0xff6f8553);
        addText(BuildConfig.PREVIEW_STAGE >= 3 ? "Tu teclado, en tus apps." : "Conoce tu teclado nativo.", 28, 0xff20252d);
        addText("Diseño original. Sin anuncios. Tus pulsaciones no se guardan ni se envían por este teclado.",14,0xff737f93);
        status = addText("",14,0xff2359e8);
        if (BuildConfig.PREVIEW_STAGE >= 3) {
            addText("1. Activa Velocity en la lista de teclados de Android.\n2. Selecciónalo como método de entrada.\n3. Abre un campo de texto en otra aplicación.",14,0xff61708a);
            addButton("1 · Activar en Ajustes de Android", () -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
            addButton("2 · Elegir Velocity como teclado", () -> {
                InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (manager != null) manager.showInputMethodPicker();
            });
            addText("Android muestra una advertencia estándar al activar cualquier teclado. Lee los avisos antes de aceptar. No actives teclados de fuentes en las que no confíes.",12,0xff8b7691);
        } else {
            addText("Alpha 2 permite probar esta vista nativa. No se puede activar como teclado del sistema. Instala Alpha 3 para escribir en otras aplicaciones.",14,0xff8b7691);
        }
        addText("Tu estilo",19,0xff20252d);
        select("Tema", KeyboardPreferences.NAMES, KeyboardPreferences.themeIndex(this), position -> {
            KeyboardPreferences.get(this).edit().putString("theme",KeyboardPreferences.THEMES[position]).apply();
        });
        String[] handValues = {"full","left","right"};
        int handIndex = Arrays.asList(handValues).indexOf(KeyboardPreferences.get(this).getString("hand","full"));
        select("Modo de una mano", new String[]{"Completo","Izquierda","Derecha"}, Math.max(0,handIndex), position -> {
            KeyboardPreferences.get(this).edit().putString("hand",handValues[position]).apply();
        });
        toggle("Sonido al escribir","sound",false);
        toggle("Vibración (según dispositivo)","haptics",true);
        addText("Prueba nativa",19,0xff20252d);
        addText("Mantén pulsada una vocal para acentuarla; usa ?123 para ü y símbolos. Mantén ⇧ para bloquear mayúsculas. Puedes girar el celular y cambiar de lado con ⇆.",12,0xff737f93);
        EditText sample = new EditText(this); sample.setHint("Escribe aquí con la vista previa…"); sample.setTextColor(0xff344462); sample.setMinLines(2); sample.setGravity(Gravity.TOP); sample.setShowSoftInputOnFocus(false); content.addView(sample,new LinearLayout.LayoutParams(-1,dp(110)));
        preview = new VelocityKeyboardView(this, new VelocityKeyboardView.Listener() {
            @Override public void onText(String text) {
                int start = Math.max(0,sample.getSelectionStart()), end = Math.max(0,sample.getSelectionEnd());
                sample.getText().replace(Math.min(start,end),Math.max(start,end),text);
            }
            @Override public void onDelete() {
                Editable text = sample.getText(); int start = Math.max(0,sample.getSelectionStart()), end = Math.max(0,sample.getSelectionEnd());
                if(start != end) text.delete(Math.min(start,end),Math.max(start,end));
                else if(start > 0) text.delete(start-KeyboardLogic.codePointLengthAtEnd(text.subSequence(0,start)),start);
            }
            @Override public void onEnter() { onText("\n"); }
            @Override public void onSwitchKeyboard() { ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker(); }
            @Override public void onSettings() { scroll.smoothScrollTo(0,0); }
            @Override public void onHide() { preview.setVisibility(View.GONE); }
        });
        content.addView(preview,new LinearLayout.LayoutParams(-1,-2));
        addButton("Mostrar vista previa", () -> preview.setVisibility(View.VISIBLE));
        if(BuildConfig.PREVIEW_STAGE >= 3) {
            addText("Prueba del teclado del sistema",19,0xff20252d);
            addText("Después de activar y elegir Velocity, toca estos campos. Aquí se abre el IME real, no la vista previa anterior.",12,0xff737f93);
            EditText field = new EditText(this); field.setHint("Campo de texto con teclado del sistema"); field.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); content.addView(field,new LinearLayout.LayoutParams(-1,dp(90)));
            EditText number = new EditText(this); number.setHint("Prueba numérica"); number.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); content.addView(number,new LinearLayout.LayoutParams(-1,dp(60)));
            EditText password = new EditText(this); password.setHint("Prueba privada (no uses una contraseña real)"); password.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD); content.addView(password,new LinearLayout.LayoutParams(-1,dp(60)));
        }
        addText("Límites de esta alpha",19,0xff20252d);
        addText("El IME incluye escritura, números, símbolos, emojis, seis temas y una mano. Traductor, notas y dictado siguen en la app web; no están integrados en la barra del teclado nativo. Sin autocorrección predictiva ni gestos de deslizamiento. Algunas apps pueden impedir teclados externos o usar su propio campo. No funciona en iPhone ni en pantallas de bloqueo del sistema.",12,0xff737f93);
        addText("Para volver atrás: pulsa ◎ en el teclado o selecciona tu teclado anterior en Ajustes. Para desactivar Velocity: Ajustes de Android → Idiomas y entrada → Teclado en pantalla. Antes de desinstalar, exporta las preferencias de la web si quieres conservarlas.",12,0xff737f93);
        addButton("Volver a Velocity", this::finish);
        ready = true;
    }
    @Override protected void onResume() {
        super.onResume();
        if(status == null) return;
        boolean enabled = false;
        InputMethodManager manager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if(manager != null) for(InputMethodInfo info : manager.getEnabledInputMethodList()) if(info.getPackageName().equals(getPackageName())) enabled = true;
        String selected = Settings.Secure.getString(getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);
        boolean active = selected != null && new ComponentName(this,VelocityInputMethodService.class).equals(ComponentName.unflattenFromString(selected));
        status.setText(BuildConfig.PREVIEW_STAGE < 3 ? "Alpha 2 · solo vista previa" : active ? "✓ Velocity está seleccionado" : enabled ? "✓ Activado · falta seleccionarlo" : "Velocity aún no está activado");
        if(preview != null) preview.refresh();
    }
    private TextView addText(String value,int size,int color) {
        TextView text=new TextView(this);text.setText(value);text.setTextSize(size);text.setTextColor(color);text.setPadding(0,dp(10),0,dp(10));text.setLineSpacing(dp(3),1);content.addView(text);return text;
    }
    private void addButton(String label,Runnable action) {
        Button button=new Button(this);button.setAllCaps(false);button.setText(label);button.setTextColor(0xff2359e8);button.setOnClickListener(v->action.run());content.addView(button,new LinearLayout.LayoutParams(-1,dp(54)));
    }
    private interface Selection { void changed(int index); }
    private void select(String label,String[] values,int index,Selection change) {
        addText(label,13,0xff61708a);
        Spinner spinner=new Spinner(this); ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);spinner.setSelection(index);
        spinner.setContentDescription(label);content.addView(spinner,new LinearLayout.LayoutParams(-1,dp(50)));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            private boolean first = true;
            @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id){ if(first){first=false;return;} change.changed(position); if(ready && preview!=null)preview.refresh(); }
            @Override public void onNothingSelected(AdapterView<?> parent){}
        });
    }
    private void toggle(String label,String key,boolean initial) {
        Switch toggle=new Switch(this);toggle.setText(label);toggle.setTextSize(13);toggle.setChecked(KeyboardPreferences.get(this).getBoolean(key,initial));content.addView(toggle,new LinearLayout.LayoutParams(-1,dp(50)));
        toggle.setOnCheckedChangeListener((view,checked)->KeyboardPreferences.get(this).edit().putBoolean(key,checked).apply());
    }
}
