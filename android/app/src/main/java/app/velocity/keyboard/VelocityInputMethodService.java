package app.velocity.keyboard;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

/** Android binds this service only after the user explicitly enables/selects it. */
public final class VelocityInputMethodService
  extends InputMethodService
  implements VelocityKeyboardView.Listener
{

  private VelocityKeyboardView keyboard;
  private EditorInfo editor;
  private boolean sensitive;

  @Override
  public View onCreateInputView() {
    keyboard = new VelocityKeyboardView(this, this);
    configureEditor();
    return keyboard;
  }

  @Override
  public void onStartInput(EditorInfo info, boolean restarting) {
    super.onStartInput(info, restarting);
    editor = info;
    configureEditor();
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting) {
    super.onStartInputView(info, restarting);
    editor = info;
    configureEditor();
  }

  private void configureEditor() {
    if (keyboard == null) return;
    int type = editor == null ? InputType.TYPE_CLASS_TEXT : editor.inputType;
    int inputClass = type & InputType.TYPE_MASK_CLASS;
    int variation = type & InputType.TYPE_MASK_VARIATION;
    sensitive =
      (inputClass == InputType.TYPE_CLASS_TEXT &&
        (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
          variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
          variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
      (inputClass == InputType.TYPE_CLASS_NUMBER &&
        variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    boolean numeric =
      inputClass == InputType.TYPE_CLASS_NUMBER ||
      inputClass == InputType.TYPE_CLASS_PHONE ||
      inputClass == InputType.TYPE_CLASS_DATETIME;
    boolean shifted =
      !sensitive &&
      !numeric &&
      ((type & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0 ||
        (type & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0);
    String label = "↵";
    int action =
      editor == null
        ? EditorInfo.IME_ACTION_NONE
        : editor.imeOptions & EditorInfo.IME_MASK_ACTION;
    if (
      editor != null &&
      (editor.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0
    ) {
      switch (action) {
        case EditorInfo.IME_ACTION_DONE:
          label = "Listo";
          break;
        case EditorInfo.IME_ACTION_NEXT:
          label = "Sig.";
          break;
        case EditorInfo.IME_ACTION_PREVIOUS:
          label = "Ant.";
          break;
        case EditorInfo.IME_ACTION_SEARCH:
          label = "Buscar";
          break;
        case EditorInfo.IME_ACTION_SEND:
          label = "Enviar";
          break;
        case EditorInfo.IME_ACTION_GO:
          label = "Ir";
          break;
        default:
          break;
      }
    }
    boolean noLearning = editor != null && (editor.imeOptions & 0x1000000) != 0;
    keyboard.startEditor(numeric, sensitive || noLearning, shifted, label);
  }

  @Override
  public boolean onEvaluateFullscreenMode() {
    return false;
  }

  @Override
  public void onText(String text) {
    InputConnection connection = getCurrentInputConnection();
    if (connection != null) connection.commitText(text, 1);
  }

  @Override
  public void onDelete() {
    InputConnection connection = getCurrentInputConnection();
    if (connection == null) return;
    // Do not inspect a password's contents. Let its editor process deletion.
    if (sensitive) {
      connection.sendKeyEvent(
        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
      );
      connection.sendKeyEvent(
        new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL)
      );
      return;
    }
    CharSequence selected = connection.getSelectedText(0);
    if (selected != null && selected.length() > 0) {
      connection.commitText("", 1);
      return;
    }
    int length = KeyboardLogic.previousGraphemeLength(
      connection.getTextBeforeCursor(128, 0)
    );
    if (length > 0) connection.deleteSurroundingText(length, 0);
  }

  @Override
  public void onMoveCursor(int delta) {
    InputConnection connection = getCurrentInputConnection();
    if (connection == null) return;
    int code =
      delta < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
    connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
    connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
  }

  @Override
  public void onEdit(String action) {
    InputConnection connection = getCurrentInputConnection();
    if (connection == null) return;
    if ("home".equals(action) || "end".equals(action)) {
      int code = "home".equals(action)
        ? KeyEvent.KEYCODE_MOVE_HOME
        : KeyEvent.KEYCODE_MOVE_END;
      connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
      connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
      return;
    }
    if (
      sensitive ||
      KeyboardPreferences.flag(this, "incognito") ||
      (editor != null && (editor.imeOptions & 0x1000000) != 0)
    ) return;
    int id = "copy".equals(action)
      ? android.R.id.copy
      : "cut".equals(action)
        ? android.R.id.cut
        : "paste".equals(action)
          ? android.R.id.paste
          : android.R.id.selectAll;
    connection.performContextMenuAction(id);
  }

  @Override
  public void onDoubleSpace() {
    InputConnection connection = getCurrentInputConnection();
    if (connection == null) return;
    if (!sensitive) {
      CharSequence before = connection.getTextBeforeCursor(2, 0);
      if (
        before != null &&
        before.length() == 2 &&
        before.charAt(1) == ' ' &&
        !Character.isWhitespace(before.charAt(0))
      ) {
        connection.beginBatchEdit();
        connection.deleteSurroundingText(1, 0);
        connection.commitText(". ", 1);
        connection.endBatchEdit();
        return;
      }
    }
    connection.commitText(" ", 1);
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    if (keyboard != null) keyboard.finishEditor();
    super.onFinishInputView(finishingInput);
  }

  @Override
  public void onEnter() {
    InputConnection connection = getCurrentInputConnection();
    if (connection == null) return;
    int options =
      editor == null ? EditorInfo.IME_ACTION_NONE : editor.imeOptions;
    int action = options & EditorInfo.IME_MASK_ACTION;
    if (
      (options & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0 &&
      action != EditorInfo.IME_ACTION_NONE &&
      action != EditorInfo.IME_ACTION_UNSPECIFIED
    ) {
      if (connection.performEditorAction(action)) return;
    }
    if (
      editor != null &&
      (editor.inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
    ) connection.commitText("\n", 1);
    else {
      connection.sendKeyEvent(
        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
      );
      connection.sendKeyEvent(
        new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
      );
    }
  }

  @Override
  public void onSwitchKeyboard() {
    InputMethodManager manager = (InputMethodManager) getSystemService(
      INPUT_METHOD_SERVICE
    );
    if (manager != null) manager.showInputMethodPicker();
  }

  @Override
  public void onSettings() {
    startActivity(
      new Intent(this, KeyboardSetupActivity.class).addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK
      )
    );
  }

  @Override
  public void onHide() {
    requestHideSelf(0);
  }

  @Override
  public void onFinishInput() {
    if (keyboard != null) keyboard.finishEditor();
    editor = null;
    sensitive = false;
    super.onFinishInput();
  }
}
