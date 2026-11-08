package app.velocity.keyboard;
import android.content.Context;
import java.io.*;
/** One lazily decoded, immutable index, shared for the lifetime of this process. */
public final class SpanishDictionary {
    private static volatile SpanishSuggestions cached;
    private SpanishDictionary(){}
    public static SpanishSuggestions get(Context context)throws IOException {
        if(cached==null)synchronized(SpanishDictionary.class){if(cached==null)cached=new SpanishSuggestions(new InputStreamReader(context.getApplicationContext().getAssets().open("data/es-words.tsv"),"UTF-8"));}return cached;
    }
}
