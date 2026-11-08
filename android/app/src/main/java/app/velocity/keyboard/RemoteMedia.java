package app.velocity.keyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.HttpsURLConnection;
import org.json.*;

/** Fixed public references. No search queries, configured credentials or background sync. */
public final class RemoteMedia {
  static boolean consented(Context context){return context.getSharedPreferences("velocity-online-media",Context.MODE_PRIVATE).getBoolean("enabled",false);}
  static void consent(Context context,boolean enabled){context.getSharedPreferences("velocity-online-media",Context.MODE_PRIVATE).edit().putBoolean("enabled",enabled).apply();}
  public interface Active {boolean allowed();}
  public static final ExecutorService IO=Executors.newFixedThreadPool(2);
  private static volatile long cacheGeneration;
  static long generation(){return cacheGeneration;}
  interface Downloader {byte[] read(MediaRepository.Item item,Active active)throws IOException;}
  private static final long CACHE_LIMIT=16L*1024*1024;
  public static boolean safeUrl(String value){
    try{
      URI u=new URI(value);
      if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||u.getPort()!=-1||u.getQuery()!=null||u.getFragment()!=null)return false;
      return (u.getHost()!=null&&u.getHost().matches("media[0-4]\\.giphy\\.com")&&u.getPath().matches("/media/[A-Za-z0-9]+/200\\.gif"))||("stickerly.pstatic.net".equals(u.getHost())&&u.getPath().matches("/sticker_pack/[A-Za-z0-9]+/G0VY8R/15/-?[0-9]+\\.webp"));
    }catch(Exception e){return false;}
  }
  public static boolean safeSource(String value){
    try{URI u=new URI(value);return "https".equals(u.getScheme())&&u.getUserInfo()==null&&u.getPort()==-1&&u.getQuery()==null&&u.getFragment()==null&&(("giphy.com".equals(u.getHost())&&u.getPath().matches("/gifs/[A-Za-z0-9-]+"))||("sticker.ly".equals(u.getHost())&&"/s/G0VY8R".equals(u.getPath())));}catch(Exception e){return false;}
  }
  public static List<MediaRepository.Item> list(Context context)throws IOException{
    try(InputStream in=context.getAssets().open("media/sources.json")){
      JSONArray json=new JSONArray(new String(MediaRepository.readBounded(in),java.nio.charset.StandardCharsets.UTF_8));List<MediaRepository.Item> result=new ArrayList<>();
      if(json.length()>20)throw new IOException("Selection too large");
      for(int i=0;i<json.length();i++){
        JSONObject o=json.getJSONObject(i);String url=o.getString("url"),source=o.getString("source"),file=o.getString("file"),id=o.getString("id"),kind=o.getString("kind"),mime=o.getString("mime"),provider=o.getString("provider");
        boolean gif=provider.equals("GIPHY")&&kind.equals("gif")&&mime.equals("image/gif")&&java.net.URI.create(url).getHost().matches("media[0-4]\\.giphy\\.com")&&file.endsWith(".gif");
        boolean sticker=provider.equals("Sticker.ly")&&kind.equals("sticker")&&mime.equals("image/webp")&&url.startsWith("https://stickerly.pstatic.net/")&&file.endsWith(".webp");
        if(!safeUrl(url)||!safeSource(source)||!id.matches("[a-z0-9-]+")||!file.matches("[a-z0-9-]+\\.(gif|webp)")||(!gif&&!sticker))throw new IOException("Invalid reference");
        result.add(new MediaRepository.Item(id,o.getString("label"),kind,mime,file,false,url,source,provider,o.optString("author")));
      }return Collections.unmodifiableList(result);
    }catch(JSONException|RuntimeException e){throw new IOException("Invalid selection",e);}
  }
  private static File directory(Context c){return new File(c.getCacheDir(),"velocity-remote");}
  private static void active(Active active)throws IOException{if(Thread.currentThread().isInterrupted()||!active.allowed())throw new InterruptedIOException("Panel closed");}
  static void validate(byte[] bytes,String mime)throws IOException{
    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
    if(!mime.equals(bounds.outMimeType)||bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>4096||bounds.outHeight>4096||(long)bounds.outWidth*bounds.outHeight>16000000)throw new IOException("Unsupported media");
  }
  public static File obtain(Context context,MediaRepository.Item item,Active allowed)throws IOException{return obtain(context,item,allowed,RemoteMedia::download);}
  static File obtain(Context context,MediaRepository.Item item,Active panel,Downloader downloader)throws IOException{
    final long generation=cacheGeneration;Active allowed=()->cacheGeneration==generation&&panel.allowed();
    active(allowed);if(!safeUrl(item.url)||!item.file.matches("[a-z0-9-]+\\.(gif|webp)"))throw new IOException("Invalid media URL");
    File dir=directory(context);if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Storage unavailable");File cached=new File(dir,item.file);
    synchronized(RemoteMedia.class){if(cached.isFile()&&cached.length()>0&&cached.length()<=MediaRepository.MAX_BYTES&&System.currentTimeMillis()-cached.lastModified()<7L*24*60*60*1000)return cached;}
    byte[] bytes=downloader.read(item,allowed);active(allowed);
    if(bytes.length>MediaRepository.MAX_BYTES)throw new IOException("File too large");validate(bytes,item.mime);
    synchronized(RemoteMedia.class){
      active(allowed);prune(dir,bytes.length);
      File temp=File.createTempFile("download-",".tmp",dir);
      try{try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);}active(allowed);if(!temp.renameTo(cached))throw new IOException("Cache unavailable");}finally{temp.delete();}
    }return cached;
  }
  private static byte[] download(MediaRepository.Item item,Active allowed)throws IOException{
    HttpsURLConnection connection=(HttpsURLConnection)new java.net.URL(item.url).openConnection();
    connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(8000);connection.setReadTimeout(8000);connection.setRequestProperty("Accept",item.mime);
    byte[] bytes;long start=android.os.SystemClock.elapsedRealtime();
    try{
      if(connection.getResponseCode()!=200||connection.getContentLength()>MediaRepository.MAX_BYTES)throw new IOException("Provider unavailable");
      try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
        byte[] buffer=new byte[8192];int count;
        while((count=in.read(buffer))!=-1){active(allowed);if(out.size()+count>MediaRepository.MAX_BYTES||android.os.SystemClock.elapsedRealtime()-start>25000)throw new IOException("Download limit");out.write(buffer,0,count);}bytes=out.toByteArray();
      }
    }finally{connection.disconnect();}
    return bytes;
  }
  private static void prune(File dir,long incoming){File[] files=dir.listFiles();if(files==null)return;Arrays.sort(files,(a,b)->Long.compare(a.lastModified(),b.lastModified()));long total=0;for(File f:files)total+=f.length();for(File f:files){if(total+incoming<=CACHE_LIMIT)break;long size=f.length();if(f.delete())total-=size;}}
  public static Bitmap thumbnail(File file){BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),options);options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>240)options.inSampleSize*=2;options.inJustDecodeBounds=false;return BitmapFactory.decodeFile(file.getPath(),options);}
  public static synchronized void clear(Context context){cacheGeneration++;File[] files=directory(context).listFiles();if(files!=null)for(File f:files)f.delete();File[] staged=new File(context.getCacheDir(),"velocity-media").listFiles();if(staged!=null)for(File f:staged)if(f.getName().startsWith("giphy-")||f.getName().startsWith("stickerly-"))f.delete();}
}
