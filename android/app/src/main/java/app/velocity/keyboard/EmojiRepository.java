package app.velocity.keyboard;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.*;

/** One shared indexed catalogue, decoded off the UI thread and bundled offline. */
public final class EmojiRepository {

  public static final ExecutorService IO = Executors.newSingleThreadExecutor();
  public static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static volatile Catalog cached;

  public interface Callback {
    void loaded(Catalog catalog);
    void failed();
  }

  public static final class Entry {

    public final String id, emoji, label, group, tone, search, parent;

    Entry(JSONObject o) {
      id = o.optString("id");
      emoji = o.optString("emoji");
      label = o.optString("label");
      group = o.optString("group");
      tone = o.optString("tone");
      search = o.optString("search");
      parent = o.optString("parent", "");
    }
  }

  public static final class Category {

    public final String id, icon, name;

    Category(JSONObject o) {
      id = o.optString("id");
      icon = o.optString("icon");
      name = o.optString("name");
    }
  }

  public static final class Catalog {

    public final List<Entry> entries = new ArrayList<>();
    public final List<Category> categories = new ArrayList<>();
    public final Map<String, Entry> byId = new HashMap<>();
  }

  public static void load(Context context, Callback callback) {
    Context app = context.getApplicationContext();
    if (cached != null) {
      MAIN.post(() -> callback.loaded(cached));
      return;
    }
    IO.execute(() -> {
      try {
        if (cached == null) {
          Catalog catalog = new Catalog();
          JSONObject json;
          try (
            InputStream stream = app.getAssets().open("data/emojis-es.json");
            ByteArrayOutputStream out = new ByteArrayOutputStream()
          ) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = stream.read(buffer)) != -1) out.write(buffer, 0, n);
            json = new JSONObject(out.toString("UTF-8"));
          }
          JSONArray data = json.getJSONArray("entries"),
            groups = json.getJSONArray("categories");
          for (int i = 0; i < data.length(); i++) {
            Entry entry = new Entry(data.getJSONObject(i));
            catalog.entries.add(entry);
            catalog.byId.put(entry.id, entry);
          }
          for (int i = 0; i < groups.length(); i++) catalog.categories.add(
            new Category(groups.getJSONObject(i))
          );
          cached = catalog;
        }
        MAIN.post(() -> callback.loaded(cached));
      } catch (Exception e) {
        MAIN.post(callback::failed);
      }
    });
  }

  public static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")
      .toLowerCase(Locale.ROOT)
      .trim();
  }

  public static List<String> ids(Context c, String key) {
    List<String> ids = new ArrayList<>();
    try {
      JSONArray a = new JSONArray(
        KeyboardPreferences.get(c).getString(key, "[]")
      );
      for (int i = 0; i < Math.min(a.length(), 200); i++) ids.add(
        a.getString(i)
      );
    } catch (JSONException ignored) {}
    return ids;
  }

  public static void saveIds(Context c, String key, List<String> ids) {
    KeyboardPreferences.get(c)
      .edit()
      .putString(key, new JSONArray(ids).toString())
      .apply();
  }

  public static void recent(Context c, Entry entry, boolean privateField) {
    if (
      privateField ||
      KeyboardPreferences.flag(c, "incognito") ||
      !KeyboardPreferences.flag(c, "rememberEmojis")
    ) return;
    List<String> ids = ids(c, "emojiRecent");
    ids.remove(entry.id);
    ids.add(0, entry.id);
    saveIds(c, "emojiRecent", ids.subList(0, Math.min(40, ids.size())));
  }

  public static boolean favorite(Context c, Entry entry) {
    List<String> ids = ids(c, "emojiFavorites");
    boolean remove = ids.remove(entry.id);
    if (!remove) ids.add(0, entry.id);
    saveIds(c, "emojiFavorites", ids.subList(0, Math.min(200, ids.size())));
    return !remove;
  }
}
