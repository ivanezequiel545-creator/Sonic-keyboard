package app.velocity.keyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.*;
import java.util.*;
import org.json.*;

/** Offline original pack plus bounded, explicitly imported private images. No network. */
public final class MediaRepository {
  public static final int MAX_BYTES=4*1024*1024,MAX_ITEMS=12;
  public static final class Item {
    public final String id,label,kind,mime,file;
    final boolean imported;
    Item(String id,String label,String kind,String mime,String file,boolean imported){this.id=id;this.label=label;this.kind=kind;this.mime=mime;this.file=file;this.imported=imported;}
  }
  private static File folder(Context c){return new File(c.getFilesDir(),"media");}
  public static List<Item> list(Context c)throws IOException{
    List<Item> result=new ArrayList<>();
    try(InputStream in=c.getAssets().open("media/catalog.json")){
      JSONArray data=new JSONArray(new String(readBounded(in),java.nio.charset.StandardCharsets.UTF_8));
      for(int i=0;i<data.length();i++){
        JSONObject v=data.getJSONObject(i);String file=v.getString("file");
        if(!file.matches("[a-z0-9-]+\\.(gif|png|webp)"))throw new IOException("Invalid asset");
        result.add(new Item(v.getString("id"),v.getString("label"),v.getString("kind"),v.getString("mime"),file,false));
      }
    }catch(JSONException e){throw new IOException("Invalid media catalog",e);}
    File[] files=folder(c).listFiles((dir,name)->name.matches("[a-f0-9-]+\\.(gif|png|webp)"));
    if(files!=null){Arrays.sort(files);int n=0;for(File file:files){if(n++>=MAX_ITEMS)break;String mime=mime(file.getName());result.add(new Item(file.getName(),"Mi "+(mime.equals("image/gif")?"GIF ":"sticker ")+n,mime.equals("image/gif")?"gif":"sticker",mime,file.getName(),true));}}
    return Collections.unmodifiableList(result);
  }
  private static String mime(String file){return file.endsWith(".gif")?"image/gif":file.endsWith(".webp")?"image/webp":"image/png";}
  static byte[] readBounded(InputStream in)throws IOException{
    ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
    while((n=in.read(buffer))!=-1){if(out.size()+n>MAX_BYTES)throw new IOException("Image too large");out.write(buffer,0,n);}return out.toByteArray();
  }
  public static void importImage(Context c,InputStream in)throws IOException{
    byte[] bytes=readBounded(in);BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
    if(options.outWidth<=0||options.outHeight<=0||options.outWidth>4096||options.outHeight>4096||(long)options.outWidth*options.outHeight>16000000)throw new IOException("Invalid dimensions");
    String extension="image/gif".equals(options.outMimeType)?".gif":"image/png".equals(options.outMimeType)?".png":"image/webp".equals(options.outMimeType)?".webp":null;
    if(extension==null)throw new IOException("Unsupported image");
    File directory=folder(c);if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Storage unavailable");
    File[] saved=directory.listFiles();if(saved!=null&&saved.length>=MAX_ITEMS)throw new IOException("Library full");
    File target=new File(directory,UUID.randomUUID()+extension);
    try(FileOutputStream output=new FileOutputStream(target)){output.write(bytes);}catch(IOException e){target.delete();throw e;}
  }
  static InputStream open(Context c,Item item)throws IOException{return item.imported?new FileInputStream(new File(folder(c),item.file)):c.getAssets().open("media/"+item.file);}
  public static Bitmap thumbnail(Context c,Item item)throws IOException{
    BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
    try(InputStream in=open(c,item)){BitmapFactory.decodeStream(in,null,options);}
    options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>240)options.inSampleSize*=2;
    options.inJustDecodeBounds=false;try(InputStream in=open(c,item)){return BitmapFactory.decodeStream(in,null,options);}
  }
  public static File stage(Context c,Item item)throws IOException{
    File dir=new File(c.getCacheDir(),"velocity-media");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Storage unavailable");
    File target=new File(dir,item.file);
    try(InputStream in=open(c,item);FileOutputStream out=new FileOutputStream(target)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}catch(IOException e){target.delete();throw e;}
    return target;
  }
  public static void clearImports(Context c){File[] files=folder(c).listFiles();if(files!=null)for(File file:files){file.delete();new File(new File(c.getCacheDir(),"velocity-media"),file.getName()).delete();}}
}
