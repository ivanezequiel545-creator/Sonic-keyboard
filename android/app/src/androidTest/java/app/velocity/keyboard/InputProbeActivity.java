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
        // Isolate IME behavior from Android's separate spelling-correction popup.
        EditText text = new EditText(this){
          @Override public android.view.inputmethod.InputConnection onCreateInputConnection(android.view.inputmethod.EditorInfo info){
            android.view.inputmethod.InputConnection base=super.onCreateInputConnection(info);
            if(!getIntent().getBooleanExtra("rich",false)||base==null||android.os.Build.VERSION.SDK_INT<25)return base;
            info.contentMimeTypes=new String[]{"image/png","image/gif","image/webp"};
            return new android.view.inputmethod.InputConnectionWrapper(base,false){
              @Override public boolean commitContent(android.view.inputmethod.InputContentInfo content,int flags,android.os.Bundle options){
              try{
                if((flags&android.view.inputmethod.InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION)!=0)content.requestPermission();
                byte[] header=new byte[6];int count;
                try(java.io.InputStream in=getContentResolver().openInputStream(content.getContentUri())){if(in==null)return false;count=in.read(header);}
                String mime=content.getDescription().getMimeType(0);
                boolean valid=count>=4&&((mime.equals("image/png")&&header[0]==(byte)137&&header[1]=='P')||(mime.equals("image/gif")&&header[0]=='G'&&header[1]=='I'));
                if(!valid)return false;
                setText("recibido:"+mime);setSelection(length());content.releasePermission();return true;
              }catch(Exception e){return false;}
              }
            };
          }
        }; text.setContentDescription("external-probe-text"); text.setHint("Texto"); text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); root.addView(text,new LinearLayout.LayoutParams(-1,120));
        EditText number = new EditText(this); number.setContentDescription("external-probe-number"); number.setHint("Número"); number.setInputType(InputType.TYPE_CLASS_NUMBER); root.addView(number,new LinearLayout.LayoutParams(-1,120));
        EditText password = new EditText(this); password.setContentDescription("external-probe-password"); password.setHint("Privado"); password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); root.addView(password,new LinearLayout.LayoutParams(-1,120));
        if(getIntent().getBooleanExtra("suggestions",false))text.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        // This test host explicitly requests its IME, as a messaging app does.
        // Do not depend on Android's remembered soft-input visibility between
        // password, numeric and text editors or on accessibility click timing.
        for (EditText editor : new EditText[]{text, number, password}) {
            editor.setOnClickListener(view -> {
                view.requestFocus();
                view.post(() -> ((android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(view,
                    android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT));
              }
            };
        }
        setContentView(root);
    }
}
