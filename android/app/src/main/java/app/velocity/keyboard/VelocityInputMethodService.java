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
  private final android.os.Handler suggestionHandler=new android.os.Handler(android.os.Looper.getMainLooper());
  private int suggestionGeneration,cursor=-1,undoCursor=-1;
  private String candidateWord="",undoOriginal,undoReplacement;
  private java.util.List<String> lastCandidates=java.util.Collections.emptyList();
  private void clearUndo(){undoOriginal=null;undoReplacement=null;undoCursor=-1;}
  public static boolean acceptsSuggestions(int type,int options,boolean incognito){
    if(incognito||(options&0x1000000)!=0||(type&InputType.TYPE_MASK_CLASS)!=InputType.TYPE_CLASS_TEXT||(type&(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS|InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE))!=0)return false;
    int variation=type&InputType.TYPE_MASK_VARIATION;
    return variation!=InputType.TYPE_TEXT_VARIATION_PASSWORD&&variation!=InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD&&variation!=InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD&&variation!=InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS&&variation!=InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS&&variation!=InputType.TYPE_TEXT_VARIATION_URI&&variation!=InputType.TYPE_TEXT_VARIATION_PERSON_NAME;
  }
  private boolean canSuggest(){return editor!=null&&!sensitive&&KeyboardPreferences.flag(this,"suggestions")&&acceptsSuggestions(editor.inputType,editor.imeOptions,KeyboardPreferences.flag(this,"incognito"));}
  private boolean canAutoCorrect(){if(!canSuggest()||!KeyboardPreferences.flag(this,"autocorrect"))return false;int variation=editor.inputType&InputType.TYPE_MASK_VARIATION;return variation==InputType.TYPE_TEXT_VARIATION_NORMAL||variation==InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE||variation==InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE||variation==InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT;}
  private void stopSuggestions(){suggestionGeneration++;suggestionHandler.removeCallbacksAndMessages(null);candidateWord="";lastCandidates=java.util.Collections.emptyList();if(keyboard!=null)keyboard.updateCandidates(lastCandidates);}
  private void requestSuggestions(){
    final int generation=++suggestionGeneration;suggestionHandler.removeCallbacksAndMessages(null);
    if(!canSuggest()){candidateWord="";lastCandidates=java.util.Collections.emptyList();if(keyboard!=null)keyboard.updateCandidates(lastCandidates);return;}
    suggestionHandler.postDelayed(()->{
      InputConnection connection=getCurrentInputConnection();if(connection==null||!canSuggest())return;
      CharSequence selected=connection.getSelectedText(0);CharSequence after=connection.getTextAfterCursor(1,0);
      if((selected!=null&&selected.length()>0)||SpanishSuggestions.wordContinues(after)){candidateWord="";lastCandidates=java.util.Collections.emptyList();keyboard.updateCandidates(lastCandidates);return;}
      final String word=SpanishSuggestions.currentWord(connection.getTextBeforeCursor(80,0));
      // Clear stale clickable candidates before the background index query.
      candidateWord="";lastCandidates=java.util.Collections.emptyList();keyboard.updateCandidates(lastCandidates);
      EmojiRepository.IO.execute(()->{try{java.util.List<String> result=SpanishDictionary.get(this).suggest(word);suggestionHandler.post(()->{if(generation!=suggestionGeneration||!canSuggest()||keyboard==null)return;candidateWord=word;lastCandidates=result;keyboard.updateCandidates(result);});}catch(java.io.IOException ignored){/* Typing still works if the local data cannot be opened. */}});
    },60);
  }
  @Override public void onUpdateSelection(int oldStart,int oldEnd,int newStart,int newEnd,int candidatesStart,int candidatesEnd){super.onUpdateSelection(oldStart,oldEnd,newStart,newEnd,candidatesStart,candidatesEnd);cursor=newEnd;if(undoOriginal!=null&&(newStart!=newEnd||newEnd!=undoCursor))clearUndo();requestSuggestions();}
  @Override public void onSuggestion(String word){
    if(!canSuggest()||!lastCandidates.contains(word))return;InputConnection connection=getCurrentInputConnection();if(connection==null)return;
    CharSequence selected=connection.getSelectedText(0);String current=SpanishSuggestions.currentWord(connection.getTextBeforeCursor(80,0));
    if(!current.equals(candidateWord)||(selected!=null&&selected.length()>0)||SpanishSuggestions.wordContinues(connection.getTextAfterCursor(1,0)))return;
    clearUndo();connection.beginBatchEdit();try{if(current.length()>0&&!connection.deleteSurroundingText(current.length(),0))return;CharSequence after=connection.getTextAfterCursor(1,0);boolean hasSpace=after!=null&&after.length()>0&&after.charAt(0)==' ';connection.commitText(word+(hasSpace?"":" "),hasSpace?2:1);}finally{connection.endBatchEdit();}requestSuggestions();
  }


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
    stopSuggestions();clearUndo();cursor=editor==null?-1:editor.initialSelEnd;
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
    keyboard.setPredictionsAllowed(canSuggest());
    keyboard.startEditor(numeric, sensitive || noLearning, shifted, label);
    requestSuggestions();
  }

  @Override
  public boolean onEvaluateFullscreenMode() {
    return false;
  }

  @Override
  public void onText(String text) {
    InputConnection connection = getCurrentInputConnection();
    if(connection==null)return;clearUndo();
    if(" ".equals(text)&&canAutoCorrect()){
      CharSequence selected=connection.getSelectedText(0);
      if((selected==null||selected.length()==0)&&!SpanishSuggestions.wordContinues(connection.getTextAfterCursor(1,0))){
        String original=SpanishSuggestions.currentWord(connection.getTextBeforeCursor(80,0));String corrected=SpanishSuggestions.automaticCorrection(original);
        if(!original.equals(corrected)){
          int expected=cursor<0?-1:cursor-original.length()+corrected.length()+1;
          connection.beginBatchEdit();try{if(connection.deleteSurroundingText(original.length(),0)&&connection.commitText(corrected+" ",1)){undoOriginal=original;undoReplacement=corrected;undoCursor=expected;}}finally{connection.endBatchEdit();}requestSuggestions();return;
        }
      }
    }
    connection.commitText(text,1);requestSuggestions();
  }

  @Override
  public void onDelete() {try{deleteFromEditor();}finally{requestSuggestions();}}
  private void deleteFromEditor() {
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
    if(undoOriginal!=null&&canSuggest()&&(selected==null||selected.length()==0)&&undoCursor>=0&&cursor==undoCursor){String expected=undoReplacement+" ";CharSequence before=connection.getTextBeforeCursor(expected.length(),0);if(before!=null&&expected.contentEquals(before)){String original=undoOriginal;clearUndo();connection.beginBatchEdit();try{connection.deleteSurroundingText(expected.length(),0);connection.commitText(original,1);}finally{connection.endBatchEdit();}return;}}
    clearUndo();
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
    clearUndo();stopSuggestions();
    InputConnection connection = getCurrentInputConnection();
    if (connection == null) return;
    int code =
      delta < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
    connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
    connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
  }

  @Override
  public void onEdit(String action) {
    clearUndo();stopSuggestions();
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
    clearUndo();
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
    clearUndo();stopSuggestions();
    if (keyboard != null) keyboard.finishEditor();
    super.onFinishInputView(finishingInput);
  }

  @Override
  public void onEnter() {
    clearUndo();stopSuggestions();
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

  @Override public void onDestroy(){clearUndo();stopSuggestions();super.onDestroy();}

  @Override
  public void onFinishInput() {
    clearUndo();stopSuggestions();
    if (keyboard != null) keyboard.finishEditor();
    editor = null;
    sensitive = false;
    super.onFinishInput();
  }
}
