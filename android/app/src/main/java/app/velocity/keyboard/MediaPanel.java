package app.velocity.keyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.widget.*;
import java.util.*;
import java.util.concurrent.Future;

/** Local library plus an explicitly loaded selection; network never runs on the IME queue. */
final class MediaPanel extends LinearLayout {
  private volatile boolean detached,online;
  private volatile int epoch;
  private final List<Future<?>> downloads=new ArrayList<>();
  private LinearLayout creditDetails;
  private TextView creditText;
  interface Listener {void selected(MediaRepository.Item item,boolean share);}
  private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
  private void credits(Context context,String title,String text){creditText.setText(title+"\n"+text);creditDetails.setVisibility(VISIBLE);}
  MediaPanel(Context context,String kind,Listener listener){
    super(context);setOrientation(VERTICAL);PanelColors colors=new PanelColors(context);setBackgroundColor(colors.surface);
    TextView info=new TextView(context);info.setText("Toca para enviar · mantén para compartir · importa en ⚙");info.setTextSize(11);info.setTextColor(colors.muted);info.setPadding(dp(8),dp(6),dp(8),dp(6));addView(info);
    ScrollView scroll=new ScrollView(context);LinearLayout rows=new LinearLayout(context);rows.setOrientation(VERTICAL);scroll.addView(rows);addView(scroll,new LayoutParams(-1,dp(198)));
    String provider=kind.equals("gif")?"GIPHY · famosos":"Sticker.ly · G0VY8R";
    String collection=kind.equals("gif")?"https://giphy.com/explore/famosos":"https://sticker.ly/s/G0VY8R";
    TextView notice=new TextView(context);notice.setText(provider+" · contenido externo, no libre de derechos. Autoriza una vez para mostrar las miniaturas al abrir GIFs o Stickers. Usa Internet; el proveedor recibe tu IP, no lo que escribes.");notice.setTextColor(colors.muted);notice.setTextSize(11);rows.addView(notice);
    Button load=new Button(context);load.setAllCaps(false);load.setText("Cargar selección en línea");colors.chip(load,false);LinearLayout controls=new LinearLayout(context);controls.addView(load,new LayoutParams(0,dp(44),3));rows.addView(controls);
    Button source=new Button(context);source.setAllCaps(false);source.setText("Créditos");source.setTextSize(11);colors.chip(source,false);source.setOnClickListener(v->credits(context,provider,collection+"\n\nLas imágenes pertenecen a sus autores. Toca una miniatura para enviar el archivo, no para abrir una página. Puedes desactivar la carga en línea con el botón del panel."));controls.addView(source,new LayoutParams(0,dp(44),1));
    creditDetails=new LinearLayout(context);creditDetails.setOrientation(VERTICAL);creditDetails.setVisibility(GONE);
    Button closeCredits=new Button(context);closeCredits.setAllCaps(false);closeCredits.setText("Cerrar");colors.chip(closeCredits,false);closeCredits.setOnClickListener(v->creditDetails.setVisibility(GONE));creditDetails.addView(closeCredits,new LayoutParams(-1,dp(40)));
    creditText=new TextView(context);creditText.setTextColor(colors.muted);creditText.setTextSize(11);creditDetails.addView(creditText);rows.addView(creditDetails);
    LinearLayout remoteRows=new LinearLayout(context);remoteRows.setOrientation(VERTICAL);rows.addView(remoteRows);
    load.setOnClickListener(v->{
      cancelDownloads();remoteRows.removeAllViews();
      if(online){online=false;RemoteMedia.consent(context,false);load.setText("Cargar selección en línea");return;}
      online=true;RemoteMedia.consent(context,true);final int generation=epoch;final long cacheGeneration=RemoteMedia.generation();load.setText("Ocultar selección en línea");
      RemoteMedia.Active allowed=()->!detached&&online&&RemoteMedia.consented(context)&&epoch==generation&&cacheGeneration==RemoteMedia.generation()&&!KeyboardPreferences.flag(context,"incognito");
      downloads.add(RemoteMedia.IO.submit(()->{
        try{
          List<MediaRepository.Item> items=RemoteMedia.list(context);
          EmojiRepository.MAIN.post(()->{
            if(!allowed.allowed())return;LinearLayout row=null;int count=0;
            for(MediaRepository.Item item:items){if(!item.kind.equals(kind))continue;
              if(count++%3==0){row=new LinearLayout(context);remoteRows.addView(row,new LayoutParams(-1,dp(116)));}
              LinearLayout cell=cell(context,item,listener,row,colors,true);
              ImageView image=(ImageView)cell.getChildAt(0);TextView label=(TextView)cell.getChildAt(1);cell.setEnabled(false);
              downloads.add(RemoteMedia.IO.submit(()->{
                try{
                  java.io.File file=RemoteMedia.obtain(context,item,allowed);Bitmap bitmap=RemoteMedia.thumbnail(file);
                  if(bitmap==null)throw new java.io.IOException("Invalid preview");
                  EmojiRepository.MAIN.post(()->{if(allowed.allowed()){image.setImageBitmap(bitmap);cell.setEnabled(true);notifyContent();}else bitmap.recycle();});
                }catch(java.io.IOException|RuntimeException e){EmojiRepository.MAIN.post(()->{if(allowed.allowed()){label.setText("No disponible");notifyContent();}});}
              }));
            }
          });
        }catch(java.io.IOException e){EmojiRepository.MAIN.post(()->{if(allowed.allowed())Toast.makeText(context,"No se pudo abrir la selección",Toast.LENGTH_SHORT).show();});}
      }));
    });
    if(RemoteMedia.consented(context))load.performClick();
    TextView local=new TextView(context);local.setText("En mi dispositivo · sin conexión");local.setTextColor(colors.muted);local.setTextSize(11);rows.addView(local);
    LinearLayout localRows=new LinearLayout(context);localRows.setOrientation(VERTICAL);rows.addView(localRows);
    EmojiRepository.IO.execute(()->{
      try{
        List<MediaRepository.Item> items=MediaRepository.list(context);
        EmojiRepository.MAIN.post(()->{
          if(detached)return;LinearLayout row=null;int count=0;
          for(MediaRepository.Item item:items){if(!item.kind.equals(kind))continue;
            if(count++%3==0){row=new LinearLayout(context);localRows.addView(row,new LayoutParams(-1,dp(106)));}
            LinearLayout cell=cell(context,item,listener,row,colors,false);ImageView image=(ImageView)cell.getChildAt(0);
            EmojiRepository.IO.execute(()->{try{Bitmap bitmap=MediaRepository.thumbnail(context,item);EmojiRepository.MAIN.post(()->{if(!detached)image.setImageBitmap(bitmap);else if(bitmap!=null)bitmap.recycle();});}catch(java.io.IOException ignored){}});
          }
        });
      }catch(java.io.IOException e){EmojiRepository.MAIN.post(()->{if(!detached)info.setText("No se pudo abrir el pack local.");});}
    });
  }
  private LinearLayout cell(Context context,MediaRepository.Item item,Listener listener,LinearLayout row,PanelColors colors,boolean remote){
    LinearLayout cell=new LinearLayout(context);cell.setOrientation(VERTICAL);cell.setGravity(Gravity.CENTER);cell.setContentDescription("Enviar "+item.kind+": "+item.label);cell.setClickable(true);cell.setFocusable(true);
    ImageView image=new ImageView(context);image.setScaleType(ImageView.ScaleType.FIT_CENTER);cell.addView(image,new LayoutParams(-1,dp(70)));
    TextView label=new TextView(context);label.setText(item.label);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setGravity(Gravity.CENTER);label.setTextSize(11);colors.chip(label,false);cell.addView(label,new LayoutParams(-1,dp(24)));
    if(remote){TextView author=new TextView(context);author.setText(item.author.isEmpty()?item.provider:item.author);author.setTextColor(colors.muted);author.setTextSize(9);author.setSingleLine(true);author.setEllipsize(android.text.TextUtils.TruncateAt.END);author.setContentDescription("Créditos de "+item.label);author.setOnClickListener(v->credits(context,item.label,item.provider+"\n"+(item.author.isEmpty()?"Autor no identificado":item.author)+"\n"+item.source));cell.addView(author);}

    LayoutParams size=new LayoutParams(0,-1,1);size.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(cell,size);
    cell.setOnClickListener(v->{if(cell.isEnabled())listener.selected(item,false);});cell.setOnLongClickListener(v->{if(cell.isEnabled())listener.selected(item,true);return true;});return cell;
  }
  private void notifyContent(){if(isAttachedToWindow())sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);}
  @Override public void onInitializeAccessibilityEvent(android.view.accessibility.AccessibilityEvent event){super.onInitializeAccessibilityEvent(event);if(event.getEventType()==android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)event.setContentChangeTypes(android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE);}
  private void cancelDownloads(){epoch++;for(Future<?> task:downloads)task.cancel(true);downloads.clear();}
  boolean isClosed(){return detached;}
  void close(){detached=true;online=false;cancelDownloads();}
  @Override protected void onDetachedFromWindow(){close();super.onDetachedFromWindow();}
}
