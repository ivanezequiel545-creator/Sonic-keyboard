package app.velocity.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import java.util.Arrays;

/** Appearance preferences only; never editor text or clipboard history. */
public final class KeyboardPreferences {

  public static final String[] THEMES = {
    "velocity",
    "midnight",
    "mint",
    "sunset",
    "lilac",
    "graphite",
  };
  public static final String[] NAMES = {
    "Blue Rush",
    "Midnight",
    "Mint Flow",
    "Peach Club",
    "Lilac Dream",
    "Graphite",
  };
  public static final int[] COLORS = {
    0xff1755e7,
    0xff33324e,
    0xffb6d8c7,
    0xfff6b899,
    0xffb7a1da,
    0xff52606c,
  };
  public static final int[] DARKS = {
    0xff0a298b,
    0xff161625,
    0xff709b88,
    0xffda826c,
    0xff7d60b0,
    0xff25303a,
  };
  public static final int[] ACCENTS = {
    0xffd7fb83,
    0xffc1b6fc,
    0xfff4ffe6,
    0xfffff0bd,
    0xffefe8ff,
    0xffe1edf4,
  };
  public static final String[] INTEGER_KEYS = {
    "keyHeight",
    "keyGap",
    "keyRadius",
    "keyTextSize",
    "bottomPadding",
    "handWidth",
    "keyOpacity",
  };
  public static final int[] MIN = { 40, 1, 0, 14, 0, 65, 65 },
    MAX = { 68, 8, 20, 24, 36, 90, 100 },
    DEFAULT = { 56, 3, 9, 18, 10, 82, 96 };
  public static final String[] BOOLEAN_KEYS = {
    "sound",
    "haptics",
    "numberRow",
    "customColors",
    "rememberEmojis",
    "incognito",
    "doubleSpacePeriod",
  };

  private KeyboardPreferences() {}

  public static SharedPreferences get(Context c) {
    return c.getSharedPreferences("velocity_native", Context.MODE_PRIVATE);
  }

  public static int themeIndex(Context c) {
    return Math.max(
      0,
      Arrays.asList(THEMES).indexOf(get(c).getString("theme", "velocity"))
    );
  }

  public static boolean validTheme(String s) {
    return Arrays.asList(THEMES).contains(s);
  }

  public static boolean validHand(String s) {
    return "full".equals(s) || "left".equals(s) || "right".equals(s);
  }

  public static int clamp(String key, int value) {
    int i = Arrays.asList(INTEGER_KEYS).indexOf(key);
    return i < 0 ? value : Math.max(MIN[i], Math.min(MAX[i], value));
  }

  public static int number(Context c, String key) {
    int i = Arrays.asList(INTEGER_KEYS).indexOf(key);
    return i < 0 ? 0 : clamp(key, get(c).getInt(key, DEFAULT[i]));
  }

  public static boolean flag(Context c, String key) {
    return get(c).getBoolean(
      key,
      "haptics".equals(key) ||
        "numberRow".equals(key) ||
        "rememberEmojis".equals(key)
    );
  }

  public static boolean validColor(String s) {
    return s != null && s.matches("#[a-fA-F0-9]{6}");
  }

  public static int color(Context c, String key, int fallback) {
    String s = get(c).getString(key, "");
    return flag(c, "customColors") && validColor(s)
      ? Color.parseColor(s)
      : fallback;
  }

  public static int readable(int background) {
    double luminance =
      .2126 * channel(Color.red(background)) +
      .7152 * channel(Color.green(background)) +
      .0722 * channel(Color.blue(background));
    return luminance > .179 ? 0xff000000 : 0xffffffff;
  }

  private static double channel(int value) {
    double s = value / 255.0;
    return s <= .04045 ? s / 12.92 : Math.pow((s + .055) / 1.055, 2.4);
  }
}
