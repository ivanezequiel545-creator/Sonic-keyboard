package app.velocity.keyboard;

import static org.junit.Assert.*;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NativeKeyboardTest {

  private String shell(String command) throws Exception {
    try (
      ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation()
        .getUiAutomation()
        .executeShellCommand(command);
      FileInputStream input = new FileInputStream(fd.getFileDescriptor());
      ByteArrayOutputStream bytes = new ByteArrayOutputStream()
    ) {
      byte[] buffer = new byte[2048];
      int count;
      while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
      return bytes.toString("UTF-8").trim();
    }
  }

  @Test(timeout = 30000)
  public void serviceUsesAndroidInputMethodPermission() throws Exception {
    Context target =
      InstrumentationRegistry.getInstrumentation().getTargetContext();
    assertEquals("app.velocity.keyboard", target.getPackageName());
    ServiceInfo service = target
      .getPackageManager()
      .getServiceInfo(
        new ComponentName(target, VelocityInputMethodService.class),
        PackageManager.GET_META_DATA | PackageManager.MATCH_DISABLED_COMPONENTS
      );
    assertEquals("android.permission.BIND_INPUT_METHOD", service.permission);
    assertTrue(service.exported);
    assertEquals(BuildConfig.PREVIEW_STAGE >= 3, service.enabled);
  }

  @Test(timeout = 90000)
  public void typesAndDeletesInAnotherAppAndAdaptsToNumericAndPasswordFields()
    throws Exception {
    assertTrue(BuildConfig.PREVIEW_STAGE >= 3);
    UiDevice device = UiDevice.getInstance(
      InstrumentationRegistry.getInstrumentation()
    );
    String component = "app.velocity.keyboard/.VelocityInputMethodService";
    String previous = shell("settings get secure default_input_method");
    String testPackage = InstrumentationRegistry.getInstrumentation()
      .getContext()
      .getPackageName();
    try {
      shell("settings put secure show_ime_with_hard_keyboard 1");
      shell("ime enable " + component);
      shell("ime set " + component);
      shell(
        "am start -W -n " +
          testPackage +
          "/app.velocity.keyboard.InputProbeActivity -f 0x10008000"
      );
      UiObject2 field = device.wait(
        Until.findObject(By.desc("external-probe-text")),
        10000
      );
      assertNotNull(field);
      field.click();
      assertTrue(device.wait(Until.hasObject(By.desc("Espacio")), 10000));
      for (String key : new String[] { "h", "o", "l", "a" }) {
        UiObject2 button = device.wait(Until.findObject(By.text(key)), 5000);
        assertNotNull(button);
        button.click();
      }
      assertTrue(
        device.wait(
          Until.hasObject(By.text("hola").desc("external-probe-text")),
          5000
        )
      );
      device.findObject(By.desc("Borrar carácter")).click();
      assertTrue(
        device.wait(
          Until.hasObject(By.text("hol").desc("external-probe-text")),
          5000
        )
      );
      device.findObject(By.desc("Emojis o letras")).click();
      device.wait(Until.findObject(By.text("😊")), 5000).click();
      device.findObject(By.desc("Emojis o letras")).click();
      device.findObject(By.desc("Borrar carácter")).click();
      assertTrue(
        device.wait(
          Until.hasObject(By.text("hol").desc("external-probe-text")),
          5000
        )
      );
      device.findObject(By.desc("Emojis o letras")).click();
      device
        .wait(Until.findObject(By.desc("Buscar emojis en español")), 5000)
        .click();
      for (String key : new String[] { "g", "a", "t", "o" })
        device.wait(Until.findObject(By.text(key)), 5000).click();
      assertTrue(device.hasObject(By.text("hol").desc("external-probe-text")));
      device.findObject(By.desc("Ver resultados de emojis")).click();
      UiObject2 cat = device.wait(Until.findObject(By.text("🐈\uFE0F")), 5000);
      assertNotNull("Spanish search must expose the fully qualified cat emoji", cat);
      cat.click();
      device.findObject(By.desc("Emojis o letras")).click();
      device.findObject(By.desc("Borrar carácter")).click();
      assertTrue(
        device.wait(
          Until.hasObject(By.text("hol").desc("external-probe-text")),
          5000
        )
      );
      device.findObject(By.desc("external-probe-number")).click();
      device.wait(Until.findObject(By.text("1")), 5000).click();
      device.findObject(By.text("2")).click();
      assertTrue(
        device.wait(
          Until.hasObject(By.text("12").desc("external-probe-number")),
          5000
        )
      );
      assertFalse(device.hasObject(By.text("q")));
      device.findObject(By.desc("external-probe-password")).click();
      assertTrue(
        device.wait(Until.hasObject(By.text("Velocity · campo privado")), 5000)
      );
      assertFalse(device.hasObject(By.desc("Emojis o letras")));

      // The password editor can pan the first field off-screen. Dismiss the
      // IME before targeting it again, rather than clicking stale/panned bounds.
      device.pressBack();
      device.waitForIdle();
      field = device.wait(Until.findObject(By.desc("external-probe-text")), 5000);
      assertNotNull(field);
      field.click();
      boolean reopened = device.wait(Until.hasObject(By.desc("Espacio")), 5000);
      if (!reopened) {
        java.io.ByteArrayOutputStream hierarchy = new java.io.ByteArrayOutputStream();
        device.dumpWindowHierarchy(hierarchy);
        fail("Text editor should reopen the keyboard after password input: " + hierarchy.toString("UTF-8"));
      }
      device.wait(Until.findObject(By.desc("external-probe-text")), 5000).setText("abcdef");
      device.findObject(By.desc("Herramientas de edición")).click();
      device.findObject(By.text("Fin")).click();
      device.findObject(By.text("ABC · Volver")).click();
      UiObject2 space = device.wait(Until.findObject(By.desc("Espacio")), 5000);
      android.graphics.Rect area = space.getVisibleBounds();
      device.swipe(
        area.centerX() + area.width() / 3,
        area.centerY(),
        area.centerX() - area.width() / 3,
        area.centerY(),
        20
      );
      assertEquals("abcdef", device.findObject(By.desc("external-probe-text")).getText());
      device.findObject(By.text("l")).click();
      assertEquals(7, device.findObject(By.desc("external-probe-text")).getText().length());
      assertNotEquals("abcdefl", device.findObject(By.desc("external-probe-text")).getText());
      device.findObject(By.desc("Herramientas de edición")).click();
      device.findObject(By.text("Fin")).click();
      device.findObject(By.text("ABC · Volver")).click();
      UiObject2 delete = device.findObject(By.desc("Borrar carácter"));
      android.graphics.Rect bounds = delete.getVisibleBounds();
      device.swipe(
        bounds.centerX(),
        bounds.centerY(),
        bounds.centerX(),
        bounds.centerY(),
        180
      );
      assertTrue(
        device.wait(
          Until.hasObject(
            By.text(java.util.regex.Pattern.compile("^(?:Texto)?$")).desc(
              "external-probe-text"
            )
          ),
          5000
        )
      );
    } finally {
      if (previous.contains("/")) shell("ime set " + previous);
      shell("ime disable " + component);
      device.pressHome();
    }
  }

  @Test(timeout = 15000)
  public void catalogueLoadsAllEntriesAndRespectsPrivateRecentPreferences()
    throws Exception {
    Context context =
      InstrumentationRegistry.getInstrumentation().getTargetContext();
    java.util.concurrent.CountDownLatch done =
      new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.atomic.AtomicReference<EmojiRepository.Catalog> result =
      new java.util.concurrent.atomic.AtomicReference<>();
    EmojiRepository.load(
      context,
      new EmojiRepository.Callback() {
        public void loaded(EmojiRepository.Catalog catalog) {
          result.set(catalog);
          done.countDown();
        }

        public void failed() {
          done.countDown();
        }
      }
    );
    assertTrue(done.await(10, java.util.concurrent.TimeUnit.SECONDS));
    assertNotNull(result.get());
    EmojiRepository.Catalog catalog = result.get();
    assertEquals(3979, catalog.entries.size());
    assertEquals(10, catalog.categories.size());
    assertEquals(3979, catalog.byId.size());
    for (EmojiRepository.Entry entry : catalog.entries)
      assertEquals(
        entry.id,
        entry.emoji.length(),
        KeyboardLogic.previousGraphemeLength(entry.emoji)
      );
    String original = KeyboardPreferences.get(context).getString(
      "emojiRecent",
      "[]"
    );
    boolean privateOriginal = KeyboardPreferences.flag(context, "incognito"),
      rememberOriginal = KeyboardPreferences.flag(context, "rememberEmojis");
    try {
      EmojiRepository.saveIds(
        context,
        "emojiRecent",
        new java.util.ArrayList<>()
      );
      KeyboardPreferences.get(context)
        .edit()
        .putBoolean("rememberEmojis", true)
        .putBoolean("incognito", true)
        .commit();
      EmojiRepository.recent(context, catalog.entries.get(0), false);
      assertTrue(EmojiRepository.ids(context, "emojiRecent").isEmpty());
      KeyboardPreferences.get(context)
        .edit()
        .putBoolean("incognito", false)
        .commit();
      EmojiRepository.recent(context, catalog.entries.get(0), true);
      assertTrue(EmojiRepository.ids(context, "emojiRecent").isEmpty());
      EmojiRepository.recent(context, catalog.entries.get(0), false);
      assertEquals(1, EmojiRepository.ids(context, "emojiRecent").size());
    } finally {
      KeyboardPreferences.get(context)
        .edit()
        .putString("emojiRecent", original)
        .putBoolean("incognito", privateOriginal)
        .putBoolean("rememberEmojis", rememberOriginal)
        .commit();
    }
  }

  @Test(timeout=90000)
  public void sendsRealStickersAndGifsToAnotherApp() throws Exception {
    Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    String previous=shell("settings get secure default_input_method"),component="app.velocity.keyboard/.VelocityInputMethodService";
    String testPackage=InstrumentationRegistry.getInstrumentation().getContext().getPackageName();boolean incognito=KeyboardPreferences.flag(context,"incognito");
    try{
      KeyboardPreferences.get(context).edit().putBoolean("incognito",false).commit();
      shell("settings put secure show_ime_with_hard_keyboard 1");shell("ime enable "+component);shell("ime set "+component);
      shell("am start -W -n "+testPackage+"/app.velocity.keyboard.InputProbeActivity -f 0x10008000 --ez rich true");
      device.wait(Until.findObject(By.desc("external-probe-text")),10000).click();
      device.wait(Until.findObject(By.desc("Emojis o letras")),5000).click();
      device.wait(Until.findObject(By.text("Stickers")),5000).click();
      device.wait(Until.findObject(By.desc("Enviar sticker: ¡Hola!")),10000).click();
      assertTrue(device.wait(Until.hasObject(By.text("recibido:image/png").desc("external-probe-text")),5000));
      if(!device.hasObject(By.text("GIFs")))device.findObject(By.desc("Emojis o letras")).click();
      device.wait(Until.findObject(By.text("GIFs")),5000).click();
      device.wait(Until.findObject(By.desc("Enviar gif: ¡Hola! animado")),10000).click();
      assertTrue(device.wait(Until.hasObject(By.text("recibido:image/gif").desc("external-probe-text")),5000));
      device.pressBack();
      device.wait(Until.findObject(By.desc("external-probe-password")),5000).click();
      assertFalse(device.hasObject(By.text("GIFs")));assertFalse(device.hasObject(By.text("Stickers")));
    }finally{
      KeyboardPreferences.get(context).edit().putBoolean("incognito",incognito).commit();
      if(previous.contains("/"))shell("ime set "+previous);shell("ime disable "+component);device.pressHome();
    }
  }

  @Test public void offlineMediaImportsAreBoundedAndThemeHasContrast() throws Exception {
    Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
    Context isolated=new android.content.ContextWrapper(target){
      @Override public java.io.File getFilesDir(){return new java.io.File(target.getCacheDir(),"media-test-files");}
      @Override public java.io.File getCacheDir(){return new java.io.File(target.getCacheDir(),"media-test-cache");}
    };
    PanelColors palette=new PanelColors(target);assertEquals(255,android.graphics.Color.alpha(palette.surface));assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(palette.ink,palette.surface)>=4.5);
    MediaRepository.clearImports(isolated);
    java.util.List<MediaRepository.Item> initial=MediaRepository.list(isolated);assertEquals(10,initial.size());
    try{
      try(java.io.InputStream in=target.getAssets().open("media/hola.png")){MediaRepository.importImage(isolated,in);}
      assertEquals(11,MediaRepository.list(isolated).size());
      try{MediaRepository.importImage(isolated,new java.io.ByteArrayInputStream(new byte[MediaRepository.MAX_BYTES+1]));fail("Oversized media accepted");}catch(java.io.IOException expected){}
      try{MediaRepository.importImage(isolated,new java.io.ByteArrayInputStream("not an image".getBytes("UTF-8")));fail("Invalid media accepted");}catch(java.io.IOException expected){}
      assertNotNull(MediaRepository.thumbnail(isolated,initial.get(0)));
      assertTrue(MediaRepository.stage(isolated,initial.get(0)).length()>100);
      android.view.inputmethod.EditorInfo editor=new android.view.inputmethod.EditorInfo();
      assertFalse(VelocityInputMethodService.supportsMedia(editor,"image/png"));
      androidx.core.view.inputmethod.EditorInfoCompat.setContentMimeTypes(editor,new String[]{"image/*"});
      assertTrue(VelocityInputMethodService.supportsMedia(editor,"image/gif"));assertFalse(VelocityInputMethodService.supportsMedia(editor,"video/mp4"));
      for(int color:new int[]{0xff163db8,0xfff3f6fc,0xffff00ff,0xff777777,0xff000000})assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(PanelColors.foreground(color),color)>=4.5);
    }finally{MediaRepository.clearImports(isolated);}
    assertEquals(10,MediaRepository.list(isolated).size());
  }

  private void restoreSecure(String key,String value)throws Exception{
    if(value.equals("null"))shell("settings delete secure "+key);
    else shell("settings put secure "+key+" '"+value.replace("'","'\\''")+"'");
  }

  private void tapWithoutAccessibility(android.app.UiAutomation automation,android.graphics.Point point){
    long time=android.os.SystemClock.uptimeMillis();
    android.view.MotionEvent down=android.view.MotionEvent.obtain(time,time,android.view.MotionEvent.ACTION_DOWN,point.x,point.y,0);
    android.view.MotionEvent up=android.view.MotionEvent.obtain(time,time+40,android.view.MotionEvent.ACTION_UP,point.x,point.y,0);
    down.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
    try{assertTrue(automation.injectInputEvent(down,true));assertTrue(automation.injectInputEvent(up,true));}finally{down.recycle();up.recycle();}
    android.os.SystemClock.sleep(250);
  }

  @Test(timeout=90000)
  public void typingAndReopeningWorkWithoutAnAccessibilityService() throws Exception {
    org.junit.Assume.assumeTrue("No-accessibility automation requires Android 12; exercised in the second emulator",android.os.Build.VERSION.SDK_INT>=31);
    android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    Context context=instrumentation.getTargetContext();
    UiDevice device=UiDevice.getInstance(instrumentation);
    String previous=shell("settings get secure default_input_method"),component="app.velocity.keyboard/.VelocityInputMethodService";
    String testPackage=instrumentation.getContext().getPackageName();
    boolean suggestions=KeyboardPreferences.flag(context,"suggestions"),incognito=KeyboardPreferences.flag(context,"incognito");
    String accessibilityServices=shell("settings get secure enabled_accessibility_services"),accessibilityEnabled=shell("settings get secure accessibility_enabled");
    try{
      KeyboardPreferences.get(context).edit().putBoolean("suggestions",true).putBoolean("incognito",false).commit();
      shell("settings put secure show_ime_with_hard_keyboard 1");shell("ime enable "+component);shell("ime set "+component);
      shell("am start -W -n "+testPackage+"/app.velocity.keyboard.InputProbeActivity -f 0x10008000 --ez suggestions true");
      UiObject2 field=device.wait(Until.findObject(By.desc("external-probe-text")),10000);field.click();
      android.graphics.Point fieldPoint=field.getVisibleCenter();
      android.graphics.Point[] points=new android.graphics.Point[4];String[] letters={"h","o","l","a"};
      for(int j=0;j<4;j++)points[j]=device.wait(Until.findObject(By.text(letters[j])),5000).getVisibleCenter();
      // Ordinary UiAutomator tests enable accessibility and masked this crash.
      // Retain the no-accessibility automation object: no UiDevice calls here.
      // Clear regular accessibility services before the framework switches to
      // the input-only connection. Restore the original settings in finally;
      // never skip the assertion that accessibility is actually disabled.
      // shell() obtains the default automation: finish all shell setup first.
      shell("settings put secure enabled_accessibility_services ''");
      shell("settings put secure accessibility_enabled 0");
      android.app.UiAutomation automation=instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_USE_ACCESSIBILITY);
      android.view.accessibility.AccessibilityManager manager=(android.view.accessibility.AccessibilityManager)context.getSystemService(Context.ACCESSIBILITY_SERVICE);
      long deadline=android.os.SystemClock.uptimeMillis()+5000;
      while(manager.isEnabled()&&android.os.SystemClock.uptimeMillis()<deadline)android.os.SystemClock.sleep(50);
      assertFalse("Regression must run with accessibility disabled",manager.isEnabled());
      for(android.graphics.Point point:points)tapWithoutAccessibility(automation,point);
      automation.injectInputEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_BACK),true);
      automation.injectInputEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_BACK),true);
      android.os.SystemClock.sleep(400);tapWithoutAccessibility(automation,fieldPoint);android.os.SystemClock.sleep(500);
      for(android.graphics.Point point:points)tapWithoutAccessibility(automation,point);
      instrumentation.getUiAutomation(0);
      assertTrue(device.wait(Until.hasObject(By.text("holahola").desc("external-probe-text")),5000));
      assertTrue(device.hasObject(By.text("q")));
    }finally{
      restoreSecure("enabled_accessibility_services",accessibilityServices);
      restoreSecure("accessibility_enabled",accessibilityEnabled);
      instrumentation.getUiAutomation(0);
      KeyboardPreferences.get(context).edit().putBoolean("suggestions",suggestions).putBoolean("incognito",incognito).commit();
      if(previous.contains("/"))shell("ime set "+previous);shell("ime disable "+component);device.pressHome();
    }
  }

  private UiObject2 waitForSuggestion(UiDevice device,String word) throws Exception {
    UiObject2 result=device.wait(Until.findObject(By.desc("Usar sugerencia: "+word).enabled(true)),10000);
    if(result==null){ByteArrayOutputStream tree=new ByteArrayOutputStream();device.dumpWindowHierarchy(tree);fail("Missing Spanish suggestion '"+word+"'; "+VelocityInputMethodService.suggestionDiagnostics+": "+tree.toString("UTF-8"));}
    return result;
  }

  @Test(timeout=90000)
  public void spanishSuggestionsCompleteCorrectUndoAndRespectProtectedFields() throws Exception {
    Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    boolean suggestions=KeyboardPreferences.flag(context,"suggestions"),autocorrect=KeyboardPreferences.flag(context,"autocorrect"),incognito=KeyboardPreferences.flag(context,"incognito");
    UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    String previous=shell("settings get secure default_input_method");String component="app.velocity.keyboard/.VelocityInputMethodService";
    String testPackage=InstrumentationRegistry.getInstrumentation().getContext().getPackageName();
    try{
      KeyboardPreferences.get(context).edit().putBoolean("suggestions",true).putBoolean("autocorrect",true).putBoolean("incognito",false).commit();
      shell("settings put secure show_ime_with_hard_keyboard 1");shell("ime enable "+component);shell("ime set "+component);
      shell("am start -W -n "+testPackage+"/app.velocity.keyboard.InputProbeActivity -f 0x10008000 --ez suggestions true");
      device.wait(Until.findObject(By.desc("external-probe-text")),10000).click();
      assertTrue(device.wait(Until.hasObject(By.desc("Sugerencias en español")),10000));
      // Exercise actual IME keystrokes, not only accessibility ACTION_SET_TEXT.
      assertEquals(12000,SpanishDictionary.get(context).size());
      for(String key:new String[]{"h","o","l","a","a"})device.wait(Until.findObject(By.text(key)),5000).click();
      waitForSuggestion(device,"hola").click();
      assertTrue(device.wait(Until.hasObject(By.text("hola ").desc("external-probe-text")),5000));
      device.findObject(By.desc("external-probe-text")).setText("grasias");
      assertTrue(device.wait(Until.hasObject(By.desc("Usar sugerencia: gracias")),5000));
      device.findObject(By.desc("Espacio")).click();
      assertTrue(device.wait(Until.hasObject(By.text("gracias ").desc("external-probe-text")),5000));
      device.findObject(By.desc("Borrar carácter")).click();
      assertTrue(device.wait(Until.hasObject(By.text("grasias").desc("external-probe-text")),5000));
      KeyboardPreferences.get(context).edit().putBoolean("autocorrect",false).commit();
      device.findObject(By.desc("external-probe-text")).setText("holaa");
      assertTrue(device.wait(Until.hasObject(By.desc("Usar sugerencia: hola")),5000));
      device.findObject(By.desc("Espacio")).click();
      assertTrue(device.wait(Until.hasObject(By.text("holaa ").desc("external-probe-text")),5000));
      device.findObject(By.desc("external-probe-number")).click();
      assertTrue(device.wait(Until.hasObject(By.text("1")),5000));assertFalse(device.hasObject(By.desc("Sugerencias en español")));
      device.findObject(By.desc("external-probe-password")).click();assertTrue(device.wait(Until.hasObject(By.text("Velocity · campo privado")),5000));assertFalse(device.hasObject(By.desc("Sugerencias en español")));
    }finally{
      KeyboardPreferences.get(context).edit().putBoolean("suggestions",suggestions).putBoolean("autocorrect",autocorrect).putBoolean("incognito",incognito).commit();
      if(previous.contains("/"))shell("ime set "+previous);shell("ime disable "+component);device.pressHome();
    }
  }

  private android.widget.Button findButton(
    android.view.View view,
    String text
  ) {
    if (
      view instanceof android.widget.Button &&
      text.contentEquals(((android.widget.Button) view).getText())
    ) return (android.widget.Button) view;
    if (view instanceof android.view.ViewGroup) {
      android.view.ViewGroup group = (android.view.ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        android.widget.Button result = findButton(group.getChildAt(i), text);
        if (result != null) return result;
      }
    }
    return null;
  }

  @Test
  public void typingAndCaseChangesReuseTheSameKeyTree() {
    Context context =
      InstrumentationRegistry.getInstrumentation().getTargetContext();
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      StringBuilder typed = new StringBuilder();
      VelocityKeyboardView view = new VelocityKeyboardView(
        new android.view.ContextThemeWrapper(
          context,
          android.R.style.Theme_Material_Light_NoActionBar
        ),
        new VelocityKeyboardView.Listener() {
          public void onText(String text) {
            typed.append(text);
          }

          public void onDelete() {}

          public void onEnter() {}

          public void onSwitchKeyboard() {}

          public void onSettings() {}

          public void onHide() {}
        }
      );
      android.widget.Button q = findButton(view, "q");
      assertNotNull(q);
      assertEquals(android.view.Gravity.CENTER,q.getGravity());
      // Resolve/layout the detached test view first. getTextAlignment() alone
      // reports inherited gravity until View resolves its parent direction.
      view.measure(android.view.View.MeasureSpec.makeMeasureSpec(1080,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED));
      view.layout(0,0,1080,view.getMeasuredHeight());
      assertEquals(android.text.Layout.Alignment.ALIGN_CENTER,q.getLayout().getParagraphAlignment(0));
      float glyphCenter=q.getCompoundPaddingLeft()+(q.getLayout().getLineLeft(0)+q.getLayout().getLineRight(0))/2f;
      assertEquals(q.getWidth()/2f,glyphCenter,1f);
      assertEquals(android.view.Gravity.CENTER,findButton(view,"1").getGravity());
      assertEquals(android.view.Gravity.CENTER,findButton(view,"⌫").getGravity());
      android.view.View holder = view.getChildAt(0);
      q.performClick();
      q.performClick();
      assertEquals("qq", typed.toString());
      assertSame(holder, view.getChildAt(0));
      assertSame(q, findButton(view, "q"));
      findButton(view, "⇧").performClick();
      assertSame(q, findButton(view, "Q"));
      q.performClick();
      assertSame(q, findButton(view, "q"));
      assertEquals("qqQ", typed.toString());
      view.setPredictionsAllowed(true);view.startEditor(false,false,false,"↵");
      android.widget.Button stableQ=findButton(view,"q");
      view.updateCandidates(java.util.Arrays.asList("hola","gracias","bien"));
      android.widget.Button pending=findButton(view,"gracias");assertNotNull(pending);assertTrue(pending.isEnabled());
      view.markCandidatesPending();assertFalse(pending.isEnabled());assertEquals("gracias",pending.getText().toString());assertEquals("Actualizando sugerencias",pending.getContentDescription().toString());
      assertSame(stableQ,findButton(view,"q"));
      view.updateCandidates(java.util.Arrays.asList("holaa","hola","holas"));
      assertTrue(findButton(view,"hola").isEnabled());assertEquals("Usar sugerencia: hola",findButton(view,"hola").getContentDescription().toString());
      assertNotNull(findButton(view,"holaa"));assertNotNull(findButton(view,"hola"));assertNotNull(findButton(view,"holas"));
      assertNull(findButton(view,"gracias"));assertSame(stableQ,findButton(view,"q"));
      int originalPadding = KeyboardPreferences.number(context, "bottomPadding");
      try {
        KeyboardPreferences.get(context).edit().putInt("bottomPadding", 22).commit();
        view.refresh();
        assertEquals(Math.round(22 * context.getResources().getDisplayMetrics().density),
          view.getPaddingBottom());
      } finally {
        KeyboardPreferences.get(context).edit().putInt("bottomPadding", originalPadding).commit();
        view.finishEditor();
      }
    });
  }
}
