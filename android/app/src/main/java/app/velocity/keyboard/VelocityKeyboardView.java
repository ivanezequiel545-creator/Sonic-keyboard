package app.velocity.keyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/** Native keyboard, with no WebView, network access or typed-text persistence. */
public final class VelocityKeyboardView extends LinearLayout {
    public interface Listener {
        void onText(String text);
        void onDelete();
        void onEnter();
        void onSwitchKeyboard();
        void onSettings();
        void onHide();
    }
    private final Listener listener;
    private boolean shifted;
    private boolean capsLock;
    private boolean symbols;
    private boolean emojis;
    private boolean numeric;
    private boolean sensitive;
    private String actionLabel = "↵";
    private int theme;
    private LinearLayout panel;

    public VelocityKeyboardView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setPadding(dp(5), dp(4), dp(5), dp(5));
        if (android.os.Build.VERSION.SDK_INT >= 26) setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        refresh();
    }
    // Never retain a previous field's layout/caps state when changing editors.
    public void startEditor(boolean numeric, boolean sensitive, boolean shifted, String actionLabel) {
        this.numeric = numeric;
        this.sensitive = sensitive;
        this.shifted = shifted;
        this.actionLabel = actionLabel;
        capsLock = false;
        symbols = false;
        emojis = false;
        refresh();
    }
    public void finishEditor() {
        shifted = capsLock = symbols = emojis = false;
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private boolean compact() { return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE; }
    public void refresh() {
        removeAllViews();
        theme = KeyboardPreferences.themeIndex(getContext());
        setBackground(new OrbitBackground(KeyboardPreferences.COLORS[theme], KeyboardPreferences.DARKS[theme]));
        LinearLayout holder = new LinearLayout(getContext());
        holder.setGravity(Gravity.CENTER_VERTICAL);
        holder.setOrientation(HORIZONTAL);
        String hand = KeyboardPreferences.get(getContext()).getString("hand", "full");
        boolean oneHand = !"full".equals(hand);
        if (oneHand && "right".equals(hand)) holder.addView(sideButton(), new LayoutParams(0, LayoutParams.MATCH_PARENT, 0.16f));
        panel = new LinearLayout(getContext());
        panel.setOrientation(VERTICAL);
        holder.addView(panel, new LayoutParams(0, LayoutParams.WRAP_CONTENT, oneHand ? 0.84f : 1f));
        if (oneHand && "left".equals(hand)) holder.addView(sideButton(), new LayoutParams(0, LayoutParams.MATCH_PARENT, 0.16f));
        addView(holder, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        toolbar();
        if (emojis && !sensitive && !numeric) {
            for (String[] row : new String[][] {{"😊","💙","⚡","✨","🔥","🚀"},{"😂","🥰","👍","🎉","👋","❤️"},{"🤔","😎","🙌","💯","🌟","🍀"}}) addRow(row);
        } else if (numeric) {
            addRow(new String[] {"1","2","3","4","5"});
            addRow(new String[] {"6","7","8","9","0"});
            LinearLayout row = row();
            for (String key : new String[] {"+","-",".",",","#","*"}) addKey(row, key, () -> emit(key), 1, false);
            addKey(row, "⌫", listener::onDelete, 1.5f, true);
        } else {
            String[][] rows = symbols ? new String[][] {
                {"1","2","3","4","5","6","7","8","9","0"},
                {"@","#","€","_","&","-","+","(",")","/"},
                {"á","é","í","ó","ú","ü","!","?"}
            } : new String[][] {
                {"q","w","e","r","t","y","u","i","o","p"},
                {"a","s","d","f","g","h","j","k","l","ñ"},
                {"z","x","c","v","b","n","m"}
            };
            addRow(rows[0]);
            addRow(rows[1]);
            LinearLayout last = row();
            Button shift = addKey(last, capsLock ? "⇪" : "⇧", () -> { shifted = !shifted; capsLock = false; refresh(); }, 1.3f, true);
            shift.setContentDescription("Mayúsculas; mantener pulsado para bloquear");
            shift.setOnLongClickListener(v -> { capsLock = !capsLock; shifted = capsLock; refresh(); return true; });
            for (String key : rows[2]) addLetter(last, key);
            Button delete = addKey(last, "⌫", listener::onDelete, 1.3f, true);
            delete.setContentDescription("Borrar carácter");
        }
        LinearLayout bottom = row();
        if (!numeric) addKey(bottom, symbols ? "ABC" : "?123", () -> { symbols = !symbols; emojis = false; refresh(); }, 1.3f, true);
        if (!sensitive && !numeric) addKey(bottom, emojis ? "ABC" : "☺", () -> { emojis = !emojis; refresh(); }, 1, true).setContentDescription("Emojis o letras");
        addKey(bottom, ",", () -> emit(","), .7f, true);
        addKey(bottom, "español · Velocity", () -> emit(" "), 4.5f, false).setContentDescription("Espacio");
        addKey(bottom, ".", () -> emit("."), .7f, true);
        Button enter = addKey(bottom, actionLabel, listener::onEnter, 1.7f, true);
        enter.setContentDescription("Acción del campo: " + actionLabel);
        enter.setTextColor(KeyboardPreferences.DARKS[theme]);
        enter.setBackground(rounded(KeyboardPreferences.ACCENTS[theme]));
        // Space below the keys respects gesture navigation and display cutouts.
        setOnApplyWindowInsetsListener((v, insets) -> {
            setPadding(dp(5), dp(4), dp(5), dp(5) + insets.getSystemWindowInsetBottom());
            return insets;
        });
    }
    private Button sideButton() {
        Button side = new Button(getContext());
        side.setText("⇆"); side.setTextColor(0xffeef3ff); side.setTextSize(20); side.setBackgroundColor(0x00000000);
        side.setContentDescription("Cambiar el lado del teclado"); side.setAllCaps(false);
        side.setOnClickListener(v -> {
            String hand = KeyboardPreferences.get(getContext()).getString("hand", "right");
            KeyboardPreferences.get(getContext()).edit().putString("hand", "right".equals(hand) ? "left" : "right").apply();
            refresh();
        });
        return side;
    }
    private void toolbar() {
        LinearLayout bar = new LinearLayout(getContext()); bar.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(bar, new LayoutParams(LayoutParams.MATCH_PARENT, dp(compact() ? 30 : 38)));
        TextView name = new TextView(getContext()); name.setText(sensitive ? "Velocity · campo privado" : "⚡ Velocity"); name.setTextColor(0xfff2f5ff); name.setTextSize(11); name.setPadding(dp(8),0,0,0);
        bar.addView(name, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1)); name.setGravity(Gravity.CENTER_VERTICAL);
        toolbarButton(bar, "1 mano", "Cambiar modo de una mano", () -> {
            String hand = KeyboardPreferences.get(getContext()).getString("hand", "full");
            KeyboardPreferences.get(getContext()).edit().putString("hand", "full".equals(hand) ? "right" : "full").apply(); refresh();
        });
        toolbarButton(bar, "◎", "Elegir otro teclado", listener::onSwitchKeyboard);
        toolbarButton(bar, "⚙", "Ajustes de Velocity", listener::onSettings);
        toolbarButton(bar, "⌄", "Ocultar teclado", listener::onHide);
    }
    private void toolbarButton(LinearLayout bar, String text, String label, Runnable action) {
        Button button = new Button(getContext()); button.setText(text); button.setContentDescription(label); button.setTextSize(11); button.setTextColor(0xfff0f4ff); button.setPadding(0,0,0,0); button.setMinWidth(0); button.setMinimumWidth(0); button.setMinHeight(0); button.setMinimumHeight(0); button.setAllCaps(false); button.setBackgroundColor(0x00000000); button.setOnClickListener(v -> action.run());
        bar.addView(button, new LayoutParams(dp("1 mano".equals(text) ? 50 : 33), LayoutParams.MATCH_PARENT));
    }
    private LinearLayout row() {
        LinearLayout row = new LinearLayout(getContext()); row.setOrientation(HORIZONTAL); row.setGravity(Gravity.CENTER);
        panel.addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, dp(compact() ? 39 : 49)));
        return row;
    }
    private void addRow(String[] keys) { LinearLayout row = row(); for (String key : keys) addLetter(row, key); }
    private void addLetter(LinearLayout row, String key) {
        Button button = addKey(row, shifted && !symbols && !emojis ? key.toUpperCase(Locale.forLanguageTag("es")) : key,
            () -> emit(shifted && !symbols && !emojis ? key.toUpperCase(Locale.forLanguageTag("es")) : key), 1, false);
        String accent = KeyboardLogic.accent(key);
        if (accent != null && !emojis && !symbols) button.setOnLongClickListener(v -> { emit(shifted ? accent.toUpperCase(Locale.forLanguageTag("es")) : accent); return true; });
    }
    private Button addKey(LinearLayout row, String label, Runnable action, float weight, boolean special) {
        Button button = new Button(getContext());
        button.setAllCaps(false); button.setText(label); button.setTextSize(label.length() > 5 ? 10 : 17);
        button.setMinWidth(0); button.setMinimumWidth(0); button.setMinHeight(0); button.setMinimumHeight(0); button.setPadding(0,0,0,0);
        button.setTextColor(special ? 0xfff6f8ff : KeyboardPreferences.DARKS[theme]);
        button.setBackground(rounded(special ? 0x497fb2ff : 0xf5ffffff));
        button.setStateListAnimator(null);
        LayoutParams params = new LayoutParams(0, LayoutParams.MATCH_PARENT, weight); params.setMargins(dp(2),dp(3),dp(2),dp(3)); row.addView(button, params);
        button.setOnClickListener(v -> { feedback(button); action.run(); });
        return button;
    }
    private Drawable rounded(int color) { GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(5)); return bg; }
    private void emit(String text) { listener.onText(text); if (shifted && !capsLock) { shifted = false; refresh(); } }
    private void feedback(Button key) {
        if (KeyboardPreferences.get(getContext()).getBoolean("haptics", true)) key.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (KeyboardPreferences.get(getContext()).getBoolean("sound", false)) {
            AudioManager audio = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) audio.playSoundEffect(AudioManager.FX_KEY_CLICK, .15f);
        }
    }
    private static final class OrbitBackground extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int primary, dark;
        OrbitBackground(int primary, int dark) { this.primary = primary; this.dark = dark; }
        @Override public void draw(Canvas canvas) {
            paint.setShader(new LinearGradient(0,0,getBounds().width(),getBounds().height(),primary,dark,Shader.TileMode.CLAMP));
            canvas.drawRect(getBounds(),paint); paint.setShader(null); paint.setColor(0x19ffffff); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2);
            canvas.save(); canvas.rotate(-28,getBounds().centerX(),getBounds().centerY());
            for(int i=0;i<3;i++) canvas.drawOval(-70-i*55,20-i*35,getBounds().width()+20+i*55,getBounds().height()+20+i*35,paint);
            canvas.restore(); paint.setStyle(Paint.Style.FILL);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }
}
