package app.velocity.keyboard;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.*;

/** Native preferences, bounded gallery import and explicit IME onboarding. */
public final class KeyboardSetupActivity extends Activity {

  private LinearLayout content;
  private TextView status;
  private VelocityKeyboardView preview;
  private boolean initializing = true;

  private int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    build();
  }

  private void build() {
    initializing = true;
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(0xfff7f8fa);
    content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(18), dp(28), dp(18), dp(28));
    scroll.addView(content);
    setContentView(scroll);
    content.setOnApplyWindowInsetsListener((view, insets) -> {
      content.setPadding(
        dp(18),
        dp(20) + insets.getSystemWindowInsetTop(),
        dp(18),
        dp(24) + insets.getSystemWindowInsetBottom()
      );
      return insets;
    });
    text("VELOCITY · 1.0.0", 12, 0xff6f8553);
    text("Cómodo. A tu manera.", 28, 0xff20252d);
    text(
      "Ajustes locales del teclado nativo. No guarda ni envía tus pulsaciones.",
      13,
      0xff697b94
    );
    status = text("", 14, 0xff2359e8);
    button("1 · Activar en Ajustes de Android", () ->
      startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    );
    button("2 · Elegir Velocity como teclado", () ->
      (
        (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)
      ).showInputMethodPicker()
    );
    text(
      "Lee la advertencia de Android antes de activar cualquier teclado. Conserva tu teclado anterior mientras pruebas esta versión.",
      12,
      0xff81778d
    );
    text("Prueba y ajusta", 20, 0xff20252d);
    EditText sample = new EditText(this);
    sample.setHint("Escribe con la vista previa…");
    sample.setMinLines(2);
    sample.setGravity(Gravity.TOP);
    sample.setShowSoftInputOnFocus(false);
    content.addView(sample, new LinearLayout.LayoutParams(-1, dp(96)));
    preview = new VelocityKeyboardView(
      this,
      new VelocityKeyboardView.Listener() {
        public void onText(String value) {
          int a = Math.max(0, sample.getSelectionStart()),
            b = Math.max(0, sample.getSelectionEnd());
          sample.getText().replace(Math.min(a, b), Math.max(a, b), value);
        }

        public void onDelete() {
          Editable value = sample.getText();
          int a = Math.max(0, sample.getSelectionStart()),
            b = Math.max(0, sample.getSelectionEnd());
          if (a != b) value.delete(Math.min(a, b), Math.max(a, b));
          else if (a > 0) value.delete(
            a - KeyboardLogic.previousGraphemeLength(value.subSequence(0, a)),
            a
          );
        }

        public void onEnter() {
          onText("\n");
        }

        public void onSwitchKeyboard() {
          (
            (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)
          ).showInputMethodPicker();
        }

        public void onSettings() {
          scroll.smoothScrollTo(0, 0);
        }

        public void onHide() {
          preview.setVisibility(View.GONE);
        }

        public void onMoveCursor(int delta) {
          sample.setSelection(
            Math.max(
              0,
              Math.min(
                sample.length(),
                Math.max(0, sample.getSelectionStart()) + delta
              )
            )
          );
        }
      }
    );
    content.addView(preview, new LinearLayout.LayoutParams(-1, -2));
    button("Mostrar vista previa", () -> preview.setVisibility(View.VISIBLE));
    text("Comodidad", 20, 0xff20252d);
    LinearLayout presets = new LinearLayout(this);
    for (String name : new String[] { "Cómodo", "Compacto", "Accesible" }) {
      Button b = new Button(this);
      b.setText(name);
      b.setAllCaps(false);
      b.setTextSize(11);
      b.setOnClickListener(v -> {
        preset(name);
        build();
      });
      presets.addView(b, new LinearLayout.LayoutParams(0, dp(50), 1));
    }
    content.addView(presets);
    for (int i = 0; i < KeyboardPreferences.INTEGER_KEYS.length; i++) {
      String[] labels = {
        "Altura de teclas",
        "Separación horizontal",
        "Bordes redondeados",
        "Tamaño de letra",
        "Margen inferior",
        "Ancho para una mano (%)",
        "Opacidad de teclas (%)",
      };
      slider(
        labels[i],
        KeyboardPreferences.INTEGER_KEYS[i],
        KeyboardPreferences.MIN[i],
        KeyboardPreferences.MAX[i]
      );
    }
    String[] hands = { "full", "left", "right" };
    select(
      "Modo de una mano",
      new String[] { "Completo", "Izquierda", "Derecha" },
      Math.max(
        0,
        Arrays.asList(hands).indexOf(
          KeyboardPreferences.get(this).getString("hand", "full")
        )
      ),
      index ->
        KeyboardPreferences.get(this)
          .edit()
          .putString("hand", hands[index])
          .apply()
    );
    toggle("Fila numérica permanente", "numberRow");
    toggle("Sonido al escribir", "sound");
    toggle("Vibración del sistema", "haptics");
    toggle("Punto con doble espacio (opcional)", "doubleSpacePeriod");
    text("Tu diseño", 20, 0xff20252d);
    select(
      "Tema",
      KeyboardPreferences.NAMES,
      KeyboardPreferences.themeIndex(this),
      index ->
        KeyboardPreferences.get(this)
          .edit()
          .putString("theme", KeyboardPreferences.THEMES[index])
          .apply()
    );
    toggle("Usar mis colores", "customColors");
    color("Color de fondo", "backgroundColor", "#163db8");
    color("Color de teclas", "keyColor", "#f4f7ff");
    color("Color de acento", "accentColor", "#d7fb83");
    button("Elegir fondo de mi galería", () -> {
      Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      pick.setType("image/*");
      pick.addCategory(Intent.CATEGORY_OPENABLE);
      startActivityForResult(pick, 41);
    });
    button("Quitar mi foto de fondo", () -> {
      KeyboardBackground.clear(this);
      preview.refresh();
    });
    text(
      "Se copia una imagen reducida al almacenamiento privado de Velocity (máx. 8 MB de entrada). No se sube ni se pide acceso general a tu galería.",
      12,
      0xff697b94
    );
    text("Emojis y privacidad", 20, 0xff20252d);
    text(
      "Catálogo local de Unicode 17.0, con 3.979 entradas, variantes y componentes. Categorías, tonos de piel, búsqueda en español, recientes y favoritos. Mantén pulsado un emoji para variantes/favoritos. Los más nuevos requieren una fuente compatible.",
      13,
      0xff697b94
    );
    toggle("Guardar emojis recientes", "rememberEmojis");
    toggle(
      "Modo privado (sin recientes ni edición de portapapeles)",
      "incognito"
    );
    button("Borrar recientes de emojis", () -> {
      EmojiRepository.saveIds(this, "emojiRecent", new ArrayList<>());
      Toast.makeText(this, "Recientes eliminados", Toast.LENGTH_SHORT).show();
    });
    button("Borrar favoritos de emojis", () ->
      new android.app.AlertDialog.Builder(this)
        .setTitle("¿Eliminar favoritos?")
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Eliminar", (d, w) ->
          EmojiRepository.saveIds(this, "emojiFavorites", new ArrayList<>())
        )
        .show()
    );
    text("Ayudas para escribir", 20, 0xff20252d);
    text(
      "Mantén ⌫ para borrar continuamente. Desliza el espacio para mover el cursor. Mantén una letra para acentos, ⇧ para bloquear mayúsculas y usa 2/2 para más símbolos. ⋯ abre edición y modo privado. El portapapeles solo actúa al tocar Copiar/Cortar/Pegar, nunca se guarda su historial. Estas funciones y los emojis se bloquean en campos privados del editor.",
      13,
      0xff697b94
    );
    text("Prueba del teclado del sistema", 20, 0xff20252d);
    EditText field = new EditText(this);
    field.setHint("Toca aquí tras elegir Velocity");
    field.setInputType(
      android.text.InputType.TYPE_CLASS_TEXT |
        android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
    );
    content.addView(field, new LinearLayout.LayoutParams(-1, dp(85)));
    text("Información de la versión", 20, 0xff20252d);
    text(
      "Versión instalada: " +
        BuildConfig.VERSION_NAME +
        ". La compatibilidad depende de cada aplicación y dispositivo. Sin predicción aprendida, dictado nativo ni gestos para formar palabras. El traductor y las notas siguen en la app. No funciona como teclado de iOS ni en pantallas del sistema que impidan teclados externos.",
      12,
      0xff697b94
    );
    button("Restablecer diseño y comodidad", () ->
      new android.app.AlertDialog.Builder(this)
        .setTitle("¿Restablecer el diseño?")
        .setMessage(
          "No borra notas web ni tus emojis favoritos. Se eliminará la foto del teclado."
        )
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Restablecer", (d, w) -> {
          android.content.SharedPreferences.Editor edit =
            KeyboardPreferences.get(this).edit();
          for (String key : KeyboardPreferences.INTEGER_KEYS) edit.remove(key);
          edit
            .remove("customColors")
            .remove("theme")
            .remove("hand")
            .remove("numberRow")
            .apply();
          KeyboardBackground.clear(this);
          build();
        })
        .show()
    );
    button("Volver a Velocity", this::finish);
    initializing = false;
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (status == null) return;
    boolean enabled = false;
    InputMethodManager manager = (InputMethodManager) getSystemService(
      INPUT_METHOD_SERVICE
    );
    for (InputMethodInfo info : manager.getEnabledInputMethodList())
      if (info.getPackageName().equals(getPackageName())) enabled = true;
    String selected = Settings.Secure.getString(
      getContentResolver(),
      Settings.Secure.DEFAULT_INPUT_METHOD
    );
    boolean active =
      selected != null &&
      new ComponentName(this, VelocityInputMethodService.class).equals(
        ComponentName.unflattenFromString(selected)
      );
    status.setText(
      active
        ? "✓ Velocity está seleccionado"
        : enabled
          ? "✓ Activado · falta seleccionarlo"
          : "Velocity aún no está activado"
    );
    if (preview != null) preview.refresh();
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (
      request != 41 ||
      result != RESULT_OK ||
      data == null ||
      data.getData() == null
    ) return;
    android.net.Uri uri = data.getData();
    EmojiRepository.IO.execute(() -> {
      try (
        java.io.InputStream input = getContentResolver().openInputStream(uri)
      ) {
        if (input == null) throw new java.io.IOException();
        KeyboardBackground.save(this, input);
        runOnUiThread(() -> {
          if (!isFinishing()) {
            preview.refresh();
            Toast.makeText(
              this,
              "Fondo guardado solo en este dispositivo",
              Toast.LENGTH_SHORT
            ).show();
          }
        });
      } catch (Exception e) {
        runOnUiThread(() ->
          Toast.makeText(
            this,
            "No se pudo cargar. Usa una imagen de menos de 8 MB.",
            Toast.LENGTH_LONG
          ).show()
        );
      }
    });
  }

  private void preset(String name) {
    int[] values = "Compacto".equals(name)
      ? new int[] { 44, 2, 5, 16, 4, 88, 96 }
      : "Accesible".equals(name)
        ? new int[] { 64, 4, 10, 22, 16, 90, 100 }
        : KeyboardPreferences.DEFAULT;
    android.content.SharedPreferences.Editor edit = KeyboardPreferences.get(
      this
    ).edit();
    for (int i = 0; i < values.length; i++) edit.putInt(
      KeyboardPreferences.INTEGER_KEYS[i],
      values[i]
    );
    edit.putBoolean("numberRow", !"Compacto".equals(name)).apply();
  }

  private TextView text(String value, int size, int color) {
    TextView text = new TextView(this);
    text.setText(value);
    text.setTextSize(size);
    text.setTextColor(color);
    text.setPadding(0, dp(10), 0, dp(10));
    text.setLineSpacing(dp(3), 1);
    content.addView(text);
    return text;
  }

  private void button(String label, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setAllCaps(false);
    b.setTextColor(0xff2359e8);
    b.setOnClickListener(v -> action.run());
    content.addView(b, new LinearLayout.LayoutParams(-1, dp(54)));
  }

  private interface Selection {
    void changed(int index);
  }

  private void select(
    String label,
    String[] values,
    int index,
    Selection change
  ) {
    text(label, 13, 0xff61738d);
    Spinner spinner = new Spinner(this);
    spinner.setContentDescription(label);
    spinner.setAdapter(
      new ArrayAdapter<>(
        this,
        android.R.layout.simple_spinner_dropdown_item,
        values
      )
    );
    spinner.setSelection(index);
    content.addView(spinner, new LinearLayout.LayoutParams(-1, dp(48)));
    spinner.setOnItemSelectedListener(
      new AdapterView.OnItemSelectedListener() {
        boolean first = true;

        public void onItemSelected(AdapterView<?> a, View v, int p, long id) {
          if (first) {
            first = false;
            return;
          }
          change.changed(p);
          if (preview != null) preview.refresh();
        }

        public void onNothingSelected(AdapterView<?> a) {}
      }
    );
  }

  private void toggle(String label, String key) {
    Switch view = new Switch(this);
    view.setText(label);
    view.setTextSize(13);
    view.setChecked(KeyboardPreferences.flag(this, key));
    content.addView(view, new LinearLayout.LayoutParams(-1, dp(58)));
    view.setOnCheckedChangeListener((v, checked) -> {
      KeyboardPreferences.get(this).edit().putBoolean(key, checked).apply();
      if (preview != null) preview.refresh();
    });
  }

  private void slider(String label, String key, int min, int max) {
    TextView caption = text(
      label + ": " + KeyboardPreferences.number(this, key),
      13,
      0xff61738d
    );
    SeekBar bar = new SeekBar(this);
    bar.setContentDescription(label);
    bar.setMax(max - min);
    bar.setProgress(KeyboardPreferences.number(this, key) - min);
    content.addView(bar, new LinearLayout.LayoutParams(-1, dp(44)));
    bar.setOnSeekBarChangeListener(
      new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar b, int value, boolean user) {
          caption.setText(label + ": " + (value + min));
        }

        public void onStartTrackingTouch(SeekBar b) {}

        public void onStopTrackingTouch(SeekBar b) {
          KeyboardPreferences.get(KeyboardSetupActivity.this)
            .edit()
            .putInt(key, b.getProgress() + min)
            .apply();
          preview.refresh();
        }
      }
    );
  }

  private void color(String label, String key, String initial) {
    text(label + " (#RRGGBB)", 13, 0xff61738d);
    LinearLayout row = new LinearLayout(this);
    EditText input = new EditText(this);
    input.setText(KeyboardPreferences.get(this).getString(key, initial));
    input.setSingleLine(true);
    input.setContentDescription(label);
    row.addView(input, new LinearLayout.LayoutParams(0, dp(50), 1));
    Button apply = new Button(this);
    apply.setText("Aplicar");
    apply.setOnClickListener(v -> {
      String value = input.getText().toString().trim();
      if (!KeyboardPreferences.validColor(value)) {
        input.setError("Usa #RRGGBB");
        return;
      }
      KeyboardPreferences.get(this)
        .edit()
        .putString(key, value)
        .putBoolean("customColors", true)
        .apply();
      preview.refresh();
    });
    row.addView(apply);
    content.addView(row);
  }
}
