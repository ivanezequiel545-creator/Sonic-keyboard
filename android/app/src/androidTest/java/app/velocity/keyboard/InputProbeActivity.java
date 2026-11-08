package app.velocity.keyboard;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputContentInfo;
import android.widget.EditText;
import android.widget.LinearLayout;

/** Separate test APK host. Uses only framework classes, not target-app libraries. */
public class InputProbeActivity extends Activity {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(20,20,20,20);
    EditText text = new EditText(this) {
      @Override public InputConnection onCreateInputConnection(EditorInfo info) {
        InputConnection base=super.onCreateInputConnection(info);
        if(!getIntent().getBooleanExtra("rich",false)||base==null||android.os.Build.VERSION.SDK_INT<25)return base;
        info.contentMimeTypes=new String[]{"image/png","image/gif","image/webp"};
        final EditText receiver=this;
        return new InputConnectionWrapper(base,false) {
          @Override public boolean commitContent(InputContentInfo content,int flags,Bundle options) {
            boolean requested=false;
            try {
              if((flags&InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION)!=0){content.requestPermission();requested=true;}
              byte[] header=new byte[12];int count;
              try(java.io.InputStream in=getContentResolver().openInputStream(content.getContentUri())) {
                if(in==null)return false;
                count=in.read(header);
              }
              String mime=content.getDescription().getMimeType(0);
              boolean valid=count>=4&&((mime.equals("image/png")&&header[0]==(byte)137&&header[1]=='P')||(mime.equals("image/gif")&&header[0]=='G'&&header[1]=='I')||(mime.equals("image/webp")&&count>=12&&header[0]=='R'&&header[1]=='I'&&header[8]=='W'&&header[9]=='E'));
              if(!valid)return false;
              receiver.setText("recibido:"+mime);receiver.setSelection(receiver.length());return true;
            } catch(Exception e) {
              return false;
            } finally {
              if(requested)content.releasePermission();
            }
          }
        };
      }
    };
    text.setContentDescription("external-probe-text");text.setHint("Texto");
    text.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    root.addView(text,new LinearLayout.LayoutParams(-1,120));
    EditText number=new EditText(this);number.setContentDescription("external-probe-number");number.setHint("Número");number.setInputType(InputType.TYPE_CLASS_NUMBER);root.addView(number,new LinearLayout.LayoutParams(-1,120));
    EditText password=new EditText(this);password.setContentDescription("external-probe-password");password.setHint("Privado");password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);root.addView(password,new LinearLayout.LayoutParams(-1,120));
    if(getIntent().getBooleanExtra("suggestions",false))text.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    // Explicitly request the IME instead of relying on remembered visibility.
    for(EditText editor:new EditText[]{text,number,password}) {
      editor.setOnClickListener(view->{
        view.requestFocus();
        view.post(()->((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(view,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT));
      });
    }
    setContentView(root);
  }
}
