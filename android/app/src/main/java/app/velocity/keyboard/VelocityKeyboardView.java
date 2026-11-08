package app.velocity.keyboard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Comfortable native keyboard. Text/case updates reuse keys; no per-letter rebuild. */
public final class VelocityKeyboardView extends LinearLayout {

  public interface Listener {
    void onText(String text);
    void onDelete();
    void onEnter();
    void onSwitchKeyboard();
    void onSettings();
    void onHide();

    default void onSuggestion(String word) {}

    default void onMoveCursor(int delta) {}

    default void onEdit(String action) {}

    default void onDoubleSpace() {
      onText(" ");
    }
  }

  private final Listener listener;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final List<Button> letters = new ArrayList<>();
  private final List<ComfortKey> keys = new ArrayList<>();
  private boolean shifted, capsLock, symbols, emojis, numeric, sensitive, tools, searching;
  private int symbolPage, theme, keyColor, ink, accent, background, navigationBottom;
  private String actionLabel = "↵",
    emojiQuery = "";
  private long lastSpace;
  private LinearLayout panel;
  private TextView searchLabel;
  private boolean predictionsAllowed;
  private final List<Button> candidateButtons=new ArrayList<>();
  private List<String> candidateWords=Collections.emptyList();
  public void setPredictionsAllowed(boolean allowed){predictionsAllowed=allowed;}
  public void updateCandidates(List<String> words){candidateWords=new ArrayList<>(words);paintCandidates();}
  String candidateDiagnostics(){StringBuilder result=new StringBuilder("buttons="+candidateButtons.size()+"; lengths=");for(Button button:candidateButtons)result.append(button.getText().length()).append(',');return result.toString();}
  private void paintCandidates(){
    List<String> snapshot=new ArrayList<>(candidateWords);
    for(int i=0;i<candidateButtons.size();i++){
      Button button=candidateButtons.get(i);String word=i<snapshot.size()?snapshot.get(i):"";
      button.setText(word);button.setEnabled(!word.isEmpty());
      button.setContentDescription(word.isEmpty()?"Sugerencia vacía":"Usar sugerencia: "+word);
      button.setTypeface(null,i==1?Typeface.BOLD:Typeface.NORMAL);
    }
    // One subtree event after all three slots have changed. Accessibility
    // clients must not retain a partially updated sibling-button cache.
    if(isAttachedToWindow()){
      android.view.accessibility.AccessibilityEvent event=android.view.accessibility.AccessibilityEvent.obtain(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
      event.setContentChangeTypes(android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE);
      sendAccessibilityEventUnchecked(event);
    }
  }


  public VelocityKeyboardView(Context context, Listener listener) {
    super(context);
    this.listener = listener;
    setOrientation(VERTICAL);
    if (android.os.Build.VERSION.SDK_INT >= 26) setImportantForAutofill(
      IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    );
    setOnApplyWindowInsetsListener((v, insets) -> {
      navigationBottom = insets.getSystemWindowInsetBottom();
      applyBottomPadding();
      return insets;
    });
    refresh();
  }

  public void startEditor(
    boolean numeric,
    boolean sensitive,
    boolean shifted,
    String label
  ) {
    this.numeric = numeric;
    this.sensitive = sensitive;
    this.shifted = shifted;
    actionLabel = label;
    capsLock = symbols = emojis = tools = searching = false;
    emojiQuery = "";
    lastSpace = 0;
    refresh();
  }

  public void finishEditor() {
    for (ComfortKey key : keys) key.cancel();
    handler.removeCallbacksAndMessages(null);
    shifted = capsLock = symbols = emojis = tools = searching = false;
    emojiQuery = "";
    candidateWords=Collections.emptyList();paintCandidates();
    lastSpace = 0;
  }

  private int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private boolean landscape() {
    return (
      getResources().getConfiguration().orientation ==
      Configuration.ORIENTATION_LANDSCAPE
    );
  }

  private boolean privateMode() {
    return sensitive || KeyboardPreferences.flag(getContext(), "incognito");
  }

  private void applyBottomPadding() {
    setPadding(dp(4), dp(4), dp(4),
      dp(KeyboardPreferences.number(getContext(), "bottomPadding")) + navigationBottom);
  }

  public void refresh() {
    applyBottomPadding();
    for (ComfortKey key : keys) key.cancel();
    keys.clear();
    letters.clear();
    removeAllViews();
    theme = KeyboardPreferences.themeIndex(getContext());
    background = KeyboardPreferences.color(
      getContext(),
      "backgroundColor",
      KeyboardPreferences.COLORS[theme]
    );
    keyColor = KeyboardPreferences.color(getContext(), "keyColor", 0xfff4f7ff);
    ink = KeyboardPreferences.readable(keyColor);
    accent = KeyboardPreferences.color(
      getContext(),
      "accentColor",
      KeyboardPreferences.ACCENTS[theme]
    );
    setBackground(
      new Backdrop(background, KeyboardBackground.bitmap(getContext()))
    );
    LinearLayout holder = new LinearLayout(getContext());
    holder.setGravity(Gravity.CENTER_VERTICAL);
    String hand = KeyboardPreferences.get(getContext()).getString(
      "hand",
      "full"
    );
    float width = "full".equals(hand)
      ? 1
      : KeyboardPreferences.number(getContext(), "handWidth") / 100f;
    if ("right".equals(hand)) holder.addView(
      sideButton(),
      new LayoutParams(0, -1, 1 - width)
    );
    panel = new LinearLayout(getContext());
    panel.setOrientation(VERTICAL);
    holder.addView(panel, new LayoutParams(0, -2, width));
    if ("left".equals(hand)) holder.addView(
      sideButton(),
      new LayoutParams(0, -1, 1 - width)
    );
    addView(holder, new LayoutParams(-1, -2));
    toolbar();
    if (tools) {
      editorTools();
      return;
    }
    if (emojis && !sensitive && !numeric && !searching) {
      panel.addView(
        new EmojiPanel(
          getContext(),
          new EmojiPanel.Listener() {
            public void selected(String emoji) {
              listener.onText(emoji);
              lastSpace = 0;
            }

            public void search() {
              searching = true;
              refresh();
            }
          },
          emojiQuery
        )
      );
      LinearLayout bottom = row(false);
      key(
        bottom,
        "ABC",
        () -> {
          emojis = false;
          refresh();
        },
        1,
        true
      ).setContentDescription("Emojis o letras");
      key(
        bottom,
        "⌕",
        () -> {
          searching = true;
          refresh();
        },
        1,
        true
      );
      delete(bottom, 1);
      return;
    }
    if (searching) {
      LinearLayout search = row(true);
      key(
        search,
        "← Emojis",
        () -> {
          searching = false;
          refresh();
        },
        1,
        true
      );
      TextView text = new TextView(getContext());
      searchLabel = text;
      text.setText("⌕ " + emojiQuery);
      text.setTextColor(KeyboardPreferences.readable(background));
      text.setTextSize(15);
      search.addView(text, new LayoutParams(0, -1, 3));
      key(
        search,
        "×",
        () -> {
          emojiQuery = "";
          refresh();
        },
        .7f,
        true
      );
    }
    if (numeric) {
      addRow(new String[] { "1", "2", "3", "4", "5" }, false);
      addRow(new String[] { "6", "7", "8", "9", "0" }, false);
      LinearLayout row = row(false);
      for (String value : new String[] { "+", "-", ".", ",", "#", "*" })
        key(row, value, () -> emit(value), 1, false);
      delete(row, 1.3f);
    } else {
      if (
        !symbols && KeyboardPreferences.flag(getContext(), "numberRow")
      ) addRow(
        new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" },
        true
      );
      String[][] rows = symbols
        ? symbolPage == 0
          ? new String[][] {
              { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" },
              { "@", "#", "$", "%", "&", "-", "+", "(", ")", "/" },
              { "_", "\"", "'", ":", ";", "!", "?" },
            }
          : new String[][] {
              { "[", "]", "{", "}", "<", ">", "€", "£", "¥", "¢" },
              { "=", "*", "\\", "|", "~", "^", "°", "•", "©", "®" },
              { "á", "é", "í", "ó", "ú", "ü", "ñ" },
            }
        : new String[][] {
            { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p" },
            { "a", "s", "d", "f", "g", "h", "j", "k", "l", "ñ" },
            { "z", "x", "c", "v", "b", "n", "m" },
          };
      addRow(rows[0], false);
      addRow(rows[1], false);
      LinearLayout last = row(false);
      if (symbols) key(
        last,
        symbolPage == 0 ? "2/2" : "1/2",
        () -> {
          symbolPage = 1 - symbolPage;
          refresh();
        },
        1.3f,
        true
      ).setContentDescription("Cambiar página de símbolos");
      else {
        Button shift = key(
          last,
          capsLock ? "⇪" : "⇧",
          () -> {
            shifted = !shifted;
            capsLock = false;
            updateCase();
          },
          1.3f,
          true
        );
        shift.setContentDescription(
          "Mayúsculas; mantener pulsado para bloquear"
        );
        shift.setOnLongClickListener(v -> {
          capsLock = !capsLock;
          shifted = capsLock;
          updateCase();
          return true;
        });
      }
      for (String value : rows[2]) letter(last, value);
      delete(last, 1.3f);
    }
    LinearLayout bottom = row(false);
    if (!numeric) key(
      bottom,
      symbols ? "ABC" : "?123",
      () -> {
        symbols = !symbols;
        symbolPage = 0;
        refresh();
      },
      1.3f,
      true
    );
    if (!sensitive && !numeric && !searching) key(
      bottom,
      "☺",
      () -> {
        emojis = true;
        refresh();
      },
      1,
      true
    ).setContentDescription("Emojis o letras");
    key(bottom, ",", () -> emit(","), .7f, true);
    ComfortKey space = key(
      bottom,
      searching ? "espacio · buscar" : "español · Velocity",
      () -> {
        long now = android.os.SystemClock.uptimeMillis();
        if (
          !searching &&
          !privateMode() &&
          KeyboardPreferences.flag(getContext(), "doubleSpacePeriod") &&
          lastSpace != 0 &&
          now - lastSpace < 350
        ) {
          listener.onDoubleSpace();
          lastSpace = 0;
        } else {
          emit(" ");
          lastSpace = now;
        }
      },
      4.4f,
      false
    );
    space.setContentDescription("Espacio");
    space.cursor = !searching;
    key(bottom, ".", () -> emit("."), .7f, true);
    Button enter = key(
      bottom,
      searching ? "Buscar" : actionLabel,
      () -> {
        if (searching) {
          searching = false;
          refresh();
        } else listener.onEnter();
      },
      1.6f,
      true
    );
    enter.setContentDescription(
      searching
        ? "Ver resultados de emojis"
        : "Acción del campo: " + actionLabel
    );
    enter.setTextColor(KeyboardPreferences.readable(accent));
    enter.setBackground(ripple(accent));
  }

  private void updateCase() {
    for (Button button : letters) {
      String raw = (String) button.getTag();
      button.setText(
        shifted ? raw.toUpperCase(Locale.forLanguageTag("es")) : raw
      );
    }
    for (ComfortKey button : keys)
      if (
        button.getContentDescription() != null &&
        button.getContentDescription().toString().startsWith("Mayúsculas")
      ) button.setText(capsLock ? "⇪" : shifted ? "⬆" : "⇧");
  }

  private void emit(String text) {
    lastSpace = 0;
    if (searching) {
      emojiQuery += text;
      if (searchLabel != null) searchLabel.setText("⌕ " + emojiQuery);
    } else listener.onText(text);
    if (shifted && !capsLock) {
      shifted = false;
      updateCase();
    }
  }

  private void deleteAction() {
    lastSpace = 0;
    if (searching) {
      int length = KeyboardLogic.previousGraphemeLength(emojiQuery);
      if (length > 0) emojiQuery = emojiQuery.substring(
        0,
        emojiQuery.length() - length
      );
      refresh();
    } else listener.onDelete();
  }

  private void delete(LinearLayout row, float weight) {
    ComfortKey button = key(row, "⌫", this::deleteAction, weight, true);
    button.repeat = true;
    button.setContentDescription("Borrar carácter");
  }

  private void addRow(String[] values, boolean small) {
    LinearLayout row = row(small);
    for (String value : values) letter(row, value);
  }

  private void letter(LinearLayout row, String value) {
    Button button = key(
      row,
      shifted && !symbols
        ? value.toUpperCase(Locale.forLanguageTag("es"))
        : value,
      () ->
        emit(
          shifted && !symbols
            ? value.toUpperCase(Locale.forLanguageTag("es"))
            : value
        ),
      1,
      false
    );
    if (!symbols && Character.isLetter(value.codePointAt(0))) {
      button.setTag(value);
      letters.add(button);
      String accent = KeyboardLogic.accent(value);
      if (accent != null) button.setOnLongClickListener(v -> {
        PopupMenu popup = new PopupMenu(getContext(), button);
        String[] variants = "u".equals(value)
          ? new String[] { "ú", "ü" }
          : new String[] { accent };
        for (String variant : variants)
          popup
            .getMenu()
            .add(
              shifted
                ? variant.toUpperCase(Locale.forLanguageTag("es"))
                : variant
            )
            .setOnMenuItemClickListener(item -> {
              emit(item.getTitle().toString());
              return true;
            });
        popup.show();
        return true;
      });
    }
  }

  private LinearLayout row(boolean small) {
    LinearLayout row = new LinearLayout(getContext());
    row.setGravity(Gravity.CENTER);
    int height = KeyboardPreferences.number(getContext(), "keyHeight");
    if (landscape()) height = Math.min(44, height);
    if (small) height = Math.max(35, height - 12);
    panel.addView(row, new LayoutParams(-1, dp(height)));
    return row;
  }

  private ComfortKey key(
    LinearLayout row,
    String label,
    Runnable action,
    float weight,
    boolean special
  ) {
    ComfortKey button = new ComfortKey(getContext());
    keys.add(button);
    button.setAllCaps(false);
    button.setText(label);
    button.setFocusable(false);
    button.setTextSize(
      label.length() > 5
        ? Math.max(
            10,
            KeyboardPreferences.number(getContext(), "keyTextSize") - 6
          )
        : KeyboardPreferences.number(getContext(), "keyTextSize")
    );
    button.setMinWidth(0);
    button.setMinimumWidth(0);
    button.setMinHeight(0);
    button.setMinimumHeight(0);
    button.setPadding(0, 0, 0, 0);
    button.setIncludeFontPadding(false);
    // OEM/service themes can default to TOP|START. Never inherit key alignment.
    button.setSingleLine(true);
    button.setHorizontallyScrolling(false);
    button.setGravity(Gravity.CENTER);
    button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    int alpha =
      (KeyboardPreferences.number(getContext(), "keyOpacity") * 255) / 100;
    int color = special ? 0xff20375d : (keyColor & 0x00ffffff) | (alpha << 24);
    button.setTextColor(special ? 0xfff7faff : ink);
    button.setBackground(ripple(color));
    button.setStateListAnimator(null);
    LayoutParams params = new LayoutParams(0, -1, weight);
    int gap = dp(KeyboardPreferences.number(getContext(), "keyGap") / 2f);
    params.setMargins(gap, dp(3), gap, dp(3));
    row.addView(button, params);
    button.setOnClickListener(v -> {
      feedback(button);
      action.run();
    });
    return button;
  }

  private Drawable ripple(int color) {
    GradientDrawable shape = new GradientDrawable();
    shape.setColor(color);
    shape.setCornerRadius(
      dp(KeyboardPreferences.number(getContext(), "keyRadius"))
    );
    return new RippleDrawable(ColorStateList.valueOf(0x443c7aff), shape, null);
  }

  private void feedback(View key) {
    if (
      KeyboardPreferences.flag(getContext(), "haptics")
    ) key.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    if (KeyboardPreferences.flag(getContext(), "sound")) {
      AudioManager manager = (AudioManager) getContext().getSystemService(
        Context.AUDIO_SERVICE
      );
      if (manager != null) manager.playSoundEffect(
        AudioManager.FX_KEY_CLICK,
        .15f
      );
    }
  }

  private Button sideButton() {
    Button button = new Button(getContext());
    button.setText("⇆");
    button.setGravity(Gravity.CENTER);
    button.setTextColor(KeyboardPreferences.readable(background));
    button.setBackgroundColor(Color.TRANSPARENT);
    button.setContentDescription("Cambiar el lado del teclado");
    button.setOnClickListener(v -> {
      String hand = KeyboardPreferences.get(getContext()).getString(
        "hand",
        "right"
      );
      KeyboardPreferences.get(getContext())
        .edit()
        .putString("hand", "right".equals(hand) ? "left" : "right")
        .apply();
      refresh();
    });
    return button;
  }

  private void toolbar() {
    LinearLayout bar = new LinearLayout(getContext());
    bar.setGravity(Gravity.CENTER_VERTICAL);
    panel.addView(bar, new LayoutParams(-1, dp(40)));
    candidateButtons.clear();
    boolean showCandidates=predictionsAllowed&&!privateMode()&&!numeric&&!symbols&&!emojis&&!tools&&!searching&&KeyboardPreferences.flag(getContext(),"suggestions");
    if(showCandidates){
      tool(bar,"⋯","Herramientas de edición",()->{tools=true;refresh();});
      LinearLayout candidates=new LinearLayout(getContext());candidates.setContentDescription("Sugerencias en español");bar.addView(candidates,new LayoutParams(0,-1,1));
      for(int i=0;i<3;i++){final int index=i;Button button=new Button(getContext());button.setAllCaps(false);button.setTextSize(14);button.setTextColor(KeyboardPreferences.readable(background));button.setBackgroundColor(Color.TRANSPARENT);button.setPadding(dp(2),0,dp(2),0);button.setMinWidth(0);button.setMinimumWidth(0);button.setMinHeight(0);button.setMinimumHeight(0);button.setGravity(Gravity.CENTER);button.setSingleLine(true);button.setHorizontallyScrolling(false);button.setEllipsize(android.text.TextUtils.TruncateAt.END);button.setFocusable(false);button.setOnClickListener(v->{if(index<candidateWords.size())listener.onSuggestion(candidateWords.get(index));});candidateButtons.add(button);candidates.addView(button,new LayoutParams(0,-1,1));}paintCandidates();
    }else{
      TextView title=new TextView(getContext());title.setText(sensitive?"Velocity · campo privado":privateMode()?"Velocity · privado":"⚡ Velocity");title.setTextColor(KeyboardPreferences.readable(background));title.setTextSize(11);title.setPadding(dp(5),0,0,0);bar.addView(title,new LayoutParams(0,-2,1));
      tool(bar,"⋯","Herramientas de edición",()->{tools=!tools;emojis=searching=false;refresh();});
      tool(bar,"1 mano","Cambiar modo de una mano",this::toggleHand);
    }
    tool(bar, "◎", "Elegir otro teclado", listener::onSwitchKeyboard);
    tool(bar, "⚙", "Ajustes de Velocity", listener::onSettings);
    tool(bar, "⌄", "Ocultar teclado", listener::onHide);
  }

  private void tool(
    LinearLayout row,
    String text,
    String label,
    Runnable action
  ) {
    Button button = new Button(getContext());
    button.setAllCaps(false);
    button.setText(text);
    button.setTextSize(11);
    button.setGravity(Gravity.CENTER);
    button.setTextColor(KeyboardPreferences.readable(background));
    button.setBackgroundColor(Color.TRANSPARENT);
    button.setPadding(0, 0, 0, 0);
    button.setMinWidth(0);
    button.setMinimumWidth(0);
    button.setContentDescription(label);
    button.setFocusable(false);
    button.setOnClickListener(v -> action.run());
    row.addView(button, new LayoutParams(dp(text.length() > 2 ? 44 : 32), -1));
  }

  private void toggleHand(){String hand=KeyboardPreferences.get(getContext()).getString("hand","full");KeyboardPreferences.get(getContext()).edit().putString("hand","full".equals(hand)?"right":"full").apply();refresh();}
  private void editorTools() {
    LinearLayout hand=row(true);key(hand,"1 mano",this::toggleHand,1,true).setContentDescription("Cambiar modo de una mano");

    LinearLayout move = row(false);
    key(
      move,
      "←",
      () -> listener.onMoveCursor(-1),
      1,
      true
    ).setContentDescription("Mover cursor a la izquierda");
    key(
      move,
      "→",
      () -> listener.onMoveCursor(1),
      1,
      true
    ).setContentDescription("Mover cursor a la derecha");
    key(move, "Inicio", () -> listener.onEdit("home"), 1, true);
    key(move, "Fin", () -> listener.onEdit("end"), 1, true);
    LinearLayout edit = row(false);
    for (String[] item : new String[][] {
      { "selectAll", "Seleccionar" },
      { "copy", "Copiar" },
      { "cut", "Cortar" },
      { "paste", "Pegar" },
    }) {
      Button b = key(edit, item[1], () -> listener.onEdit(item[0]), 1, true);
      if (privateMode()) b.setEnabled(false);
    }
    LinearLayout privacy = row(false);
    key(
      privacy,
      privateMode() ? "Privado ✓" : "Modo privado",
      () -> {
        KeyboardPreferences.get(getContext())
          .edit()
          .putBoolean(
            "incognito",
            !KeyboardPreferences.flag(getContext(), "incognito")
          )
          .apply();
        refresh();
      },
      2,
      true
    ).setEnabled(!sensitive);
    key(
      privacy,
      "Borrar recientes",
      () -> {
        EmojiRepository.saveIds(getContext(), "emojiRecent", new ArrayList<>());
        Toast.makeText(
          getContext(),
          "Recientes eliminados",
          Toast.LENGTH_SHORT
        ).show();
      },
      2,
      true
    );
    LinearLayout back = row(false);
    key(
      back,
      "ABC · Volver",
      () -> {
        tools = false;
        refresh();
      },
      1,
      false
    );
    TextView note = new TextView(getContext());
    note.setText(
      "Desliza el espacio para mover el cursor. El portapapeles solo se usa al tocar Copiar/Cortar/Pegar."
    );
    note.setTextSize(11);
    note.setTextColor(KeyboardPreferences.readable(background));
    note.setPadding(dp(6), dp(6), dp(6), dp(6));
    panel.addView(note);
  }

  @Override
  protected void onDetachedFromWindow() {
    for (ComfortKey key : keys) key.cancel();
    handler.removeCallbacksAndMessages(null);
    super.onDetachedFromWindow();
  }

  private final class ComfortKey extends androidx.appcompat.widget.AppCompatButton {

    boolean repeat, cursor, repeating, dragging;
    float downX, lastX;
    final Runnable tick = new Runnable() {
      public void run() {
        if (!isPressed()) return;
        repeating = true;
        deleteAction();
        handler.postDelayed(this, 55);
      }
    };

    ComfortKey(Context c) {
      super(c);
    }

    void cancel() {
      handler.removeCallbacks(tick);
      setPressed(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      int action = event.getActionMasked();
      if (action == MotionEvent.ACTION_DOWN) {
        repeating = dragging = false;
        downX = lastX = event.getX();
        if (repeat) handler.postDelayed(tick, 400);
      } else if (action == MotionEvent.ACTION_MOVE && cursor) {
        if (Math.abs(event.getX() - downX) > dp(14)) dragging = true;
        if (dragging && Math.abs(event.getX() - lastX) > dp(12)) {
          int delta = event.getX() > lastX ? 1 : -1;
          listener.onMoveCursor(delta);
          lastX = event.getX();
          lastSpace = 0;
          return true;
        }
      } else if (
        action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
      ) {
        handler.removeCallbacks(tick);
        if (repeating || dragging) {
          setPressed(false);
          return true;
        }
      }
      return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
      return super.performClick();
    }
  }

  private static final class Backdrop extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int color;
    private final Bitmap image;

    Backdrop(int color, Bitmap image) {
      this.color = color;
      this.image = image;
    }

    @Override
    public void draw(Canvas c) {
      paint.setShader(
        new LinearGradient(
          0,
          0,
          getBounds().width(),
          getBounds().height(),
          color,
          0xff112341,
          Shader.TileMode.CLAMP
        )
      );
      c.drawRect(getBounds(), paint);
      paint.setShader(null);
      if (image != null) {
        float scale = Math.max(
          getBounds().width() / (float) image.getWidth(),
          getBounds().height() / (float) image.getHeight()
        );
        float width = image.getWidth() * scale,
          height = image.getHeight() * scale;
        c.drawBitmap(
          image,
          null,
          new RectF(
            (getBounds().width() - width) / 2,
            (getBounds().height() - height) / 2,
            (getBounds().width() + width) / 2,
            (getBounds().height() + height) / 2
          ),
          paint
        );
        paint.setColor(0x66000000);
        c.drawRect(getBounds(), paint);
      }
      paint.setStyle(Paint.Style.STROKE);
      paint.setColor(0x18ffffff);
      paint.setStrokeWidth(2);
      c.save();
      c.rotate(-25, getBounds().centerX(), getBounds().centerY());
      for (int i = 0; i < 3; i++) c.drawOval(
        -50 - i * 50,
        10 - i * 30,
        getBounds().width() + i * 50,
        getBounds().height() + i * 30,
        paint
      );
      c.restore();
      paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void setAlpha(int alpha) {
      paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
      paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.OPAQUE;
    }
  }
}
