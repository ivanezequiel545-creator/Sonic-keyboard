package app.velocity.keyboard;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RemoteMediaTest {
  private byte[] fixture(Context context,String mime)throws Exception{
    if(mime.equals("image/gif")){try(InputStream in=context.getAssets().open("media/hola.gif")){return MediaRepository.readBounded(in);}}
    Bitmap bitmap=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xff3366ff);ByteArrayOutputStream out=new ByteArrayOutputStream();assertTrue(bitmap.compress(Bitmap.CompressFormat.WEBP,90,out));bitmap.recycle();return out.toByteArray();
  }
  @Test public void referencesCacheCancellationAndLimitsAreEnforced()throws Exception{
    Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
    Context isolated=new android.content.ContextWrapper(target){@Override public File getCacheDir(){return new File(target.getCacheDir(),"remote-test");}};
    List<MediaRepository.Item> items=RemoteMedia.list(target);assertEquals(9,items.size());
    for(MediaRepository.Item item:items){assertTrue(RemoteMedia.safeUrl(item.url));assertTrue(RemoteMedia.safeSource(item.source));assertTrue(item.online());}
    for(String bad:new String[]{"http://media.giphy.com/media/id/200.gif","https://media.giphy.com.evil.test/media/id/200.gif","https://user:pass@media.giphy.com/media/id/200.gif","https://media.giphy.com:8080/media/id/200.gif","https://media.giphy.com/media/id/200.gif?secret=1","https://127.0.0.1/a.gif"})assertFalse(bad,RemoteMedia.safeUrl(bad));
    assertFalse(RemoteMedia.safeSource("javascript:alert(1)"));
    RemoteMedia.clear(isolated);MediaRepository.Item gif=items.get(0);byte[] data=fixture(target,gif.mime);
    try{
      File cached=RemoteMedia.obtain(isolated,gif,()->true,(item,active)->data);assertEquals(data.length,cached.length());
      assertEquals(cached,RemoteMedia.obtain(isolated,gif,()->true,(item,active)->{throw new IOException("Should use cache");}));
      try{RemoteMedia.obtain(isolated,gif,()->false,(item,active)->data);fail("Closed panel reused cache");}catch(InterruptedIOException expected){}
      RemoteMedia.clear(isolated);
      try{RemoteMedia.obtain(isolated,gif,()->true,(item,active)->new byte[MediaRepository.MAX_BYTES+1]);fail("Oversized file accepted");}catch(IOException expected){}
      try{RemoteMedia.obtain(isolated,gif,()->true,(item,active)->"<html>not GIF</html>".getBytes("UTF-8"));fail("Invalid image accepted");}catch(IOException expected){}
      try{RemoteMedia.obtain(isolated,gif,()->true,(item,active)->{RemoteMedia.clear(isolated);return data;});fail("Clear did not cancel pending write");}catch(InterruptedIOException expected){}
      assertFalse(cached.exists());
    }finally{RemoteMedia.clear(isolated);}
  }
  private MediaPanel mediaPanel(android.view.View view){
    if(view instanceof MediaPanel)return (MediaPanel)view;
    if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){MediaPanel found=mediaPanel(group.getChildAt(i));if(found!=null)return found;}}
    return null;
  }
  @Test public void finishingInputClosesMediaEvenWhileTheViewIsAttached(){
    Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
    boolean incognito=KeyboardPreferences.flag(target,"incognito");KeyboardPreferences.get(target).edit().putBoolean("incognito",false).commit();
    try{InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
      VelocityKeyboardView view=new VelocityKeyboardView(new android.view.ContextThemeWrapper(target,android.R.style.Theme_Material_Light_NoActionBar),new VelocityKeyboardView.Listener(){
        public void onText(String text){}public void onDelete(){}public void onEnter(){}public void onSwitchKeyboard(){}public void onSettings(){}public void onHide(){}
      });
      java.util.ArrayList<android.view.View> controls=new java.util.ArrayList<>();view.findViewsWithText(controls,"Emojis o letras",android.view.View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);assertFalse(controls.isEmpty());controls.get(0).performClick();
      controls.clear();view.findViewsWithText(controls,"GIFs",android.view.View.FIND_VIEWS_WITH_TEXT);assertFalse(controls.isEmpty());controls.get(0).performClick();
      MediaPanel panel=mediaPanel(view);assertNotNull(panel);assertFalse(panel.isClosed());
      // IME hide does not necessarily detach its input view from the window.
      view.finishEditor();assertTrue(panel.isClosed());
    });}finally{KeyboardPreferences.get(target).edit().putBoolean("incognito",incognito).commit();}
  }

  private String shell(String command)throws Exception{
    try(ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);InputStream in=new FileInputStream(fd.getFileDescriptor())){return new String(MediaRepository.readBounded(in),"UTF-8").trim();}
  }
  private UiObject2 wait(UiDevice device,BySelector selector)throws Exception{
    UiObject2 object=device.wait(Until.findObject(selector),10000);if(object==null){ByteArrayOutputStream tree=new ByteArrayOutputStream();device.dumpWindowHierarchy(tree);fail("Missing "+selector+": "+tree.toString("UTF-8"));}return object;
  }
  @Test(timeout=90000) public void optedInSelectionSendsGifAndWebpAndHidesInPrivateFields()throws Exception{
    Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    String component="app.velocity.keyboard/.VelocityInputMethodService",previous=shell("settings get secure default_input_method"),testPackage=InstrumentationRegistry.getInstrumentation().getContext().getPackageName();boolean incognito=KeyboardPreferences.flag(context,"incognito");
    RemoteMedia.clear(context);
    // Deterministic transport fixtures; this proves URI delivery, not live CDN availability.
    for(MediaRepository.Item item:RemoteMedia.list(context)){byte[] bytes=fixture(context,item.mime);RemoteMedia.obtain(context,item,()->true,(entry,active)->bytes);}
    try{
      KeyboardPreferences.get(context).edit().putBoolean("incognito",false).commit();shell("settings put secure show_ime_with_hard_keyboard 1");shell("ime enable "+component);shell("ime set "+component);
      shell("am start -W -n "+testPackage+"/app.velocity.keyboard.InputProbeActivity -f 0x10008000 --ez rich true");wait(device,By.desc("external-probe-text")).click();wait(device,By.desc("Emojis o letras")).click();wait(device,By.text("GIFs")).click();
      assertFalse(device.hasObject(By.desc("Enviar gif: Wendy · impactada")));
      wait(device,By.text("Cargar selección en línea")).click();
      UiObject2 gif=wait(device,By.desc("Enviar gif: Wendy · impactada"));assertTrue(device.wait(Until.hasObject(By.desc("Enviar gif: Wendy · impactada").enabled(true)),10000));gif.click();
      assertTrue(device.wait(Until.hasObject(By.text("recibido:image/gif").desc("external-probe-text")),5000));
      if(!device.hasObject(By.text("Stickers")))wait(device,By.desc("Emojis o letras")).click();wait(device,By.text("Stickers")).click();wait(device,By.text("Cargar selección en línea")).click();
      UiObject2 sticker=wait(device,By.desc("Enviar sticker: Los mejores Stickers · 1"));assertTrue(device.wait(Until.hasObject(By.desc("Enviar sticker: Los mejores Stickers · 1").enabled(true)),10000));sticker.click();
      assertTrue(device.wait(Until.hasObject(By.text("recibido:image/webp").desc("external-probe-text")),5000));
      device.pressBack();wait(device,By.desc("external-probe-password")).click();assertFalse(device.hasObject(By.text("Cargar selección en línea")));
    }finally{
      KeyboardPreferences.get(context).edit().putBoolean("incognito",incognito).commit();RemoteMedia.clear(context);if(previous.contains("/"))shell("ime set "+previous);shell("ime disable "+component);device.pressHome();
    }
  }
}
