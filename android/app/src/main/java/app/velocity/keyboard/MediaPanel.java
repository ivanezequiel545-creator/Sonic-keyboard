package app.velocity.keyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.widget.*;
import java.util.List;

/** Small offline gallery. GIF thumbnails are intentionally still to avoid constant motion. */
final class MediaPanel extends LinearLayout {
  private boolean detached;
  interface Listener {void selected(MediaRepository.Item item,boolean share);}
  private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
  MediaPanel(Context context,String kind,Listener listener){
    super(context);setOrientation(VERTICAL);PanelColors colors=new PanelColors(context);setBackgroundColor(colors.surface);
    TextView info=new TextView(context);info.setText("Toca para enviar · mantén para compartir · importa en ⚙");info.setTextSize(11);info.setTextColor(colors.muted);info.setPadding(dp(8),dp(6),dp(8),dp(6));addView(info);
    ScrollView scroll=new ScrollView(context);LinearLayout rows=new LinearLayout(context);rows.setOrientation(VERTICAL);scroll.addView(rows);addView(scroll,new LayoutParams(-1,dp(198)));
    EmojiRepository.IO.execute(()->{
      try{
        List<MediaRepository.Item> items=MediaRepository.list(context);
        EmojiRepository.MAIN.post(()->{
          if(detached)return;LinearLayout row=null;int count=0;
          for(MediaRepository.Item item:items){if(!item.kind.equals(kind))continue;
            if(count++%3==0){row=new LinearLayout(context);rows.addView(row,new LayoutParams(-1,dp(106)));}
            LinearLayout cell=new LinearLayout(context);cell.setOrientation(VERTICAL);cell.setGravity(Gravity.CENTER);cell.setContentDescription("Enviar "+kind+": "+item.label);cell.setClickable(true);cell.setFocusable(true);
            ImageView image=new ImageView(context);image.setScaleType(ImageView.ScaleType.FIT_CENTER);cell.addView(image,new LayoutParams(-1,dp(77)));
            TextView label=new TextView(context);label.setText(item.label);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setGravity(Gravity.CENTER);label.setTextSize(11);colors.chip(label,false);cell.addView(label,new LayoutParams(-1,dp(25)));
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-1,1);size.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(cell,size);
            cell.setOnClickListener(v->listener.selected(item,false));cell.setOnLongClickListener(v->{listener.selected(item,true);return true;});
            EmojiRepository.IO.execute(()->{try{Bitmap bitmap=MediaRepository.thumbnail(context,item);EmojiRepository.MAIN.post(()->{if(!detached)image.setImageBitmap(bitmap);else if(bitmap!=null)bitmap.recycle();});}catch(java.io.IOException ignored){}});
          }
          if(count==0)info.setText("Aún no hay archivos. Añade PNG, WebP o GIF desde ⚙.");
        });
      }catch(java.io.IOException e){EmojiRepository.MAIN.post(()->{if(!detached)info.setText("No se pudo abrir el pack local.");});}
    });
  }
  @Override protected void onDetachedFromWindow(){detached=true;super.onDetachedFromWindow();}
}
