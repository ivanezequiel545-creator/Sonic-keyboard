package app.velocity.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;

/** Only appearance preferences are persisted. Never stores typed text. */
public final class KeyboardPreferences {
    public static final String[] THEMES = {"velocity", "midnight", "mint", "sunset", "lilac", "graphite"};
    public static final String[] NAMES = {"Blue Rush", "Midnight", "Mint Flow", "Peach Club", "Lilac Dream", "Graphite"};
    public static final int[] COLORS = {0xff1755e7, 0xff33324e, 0xffb6d8c7, 0xfff6b899, 0xffb7a1da, 0xff52606c};
    public static final int[] DARKS = {0xff0a298b, 0xff161625, 0xff709b88, 0xffda826c, 0xff7d60b0, 0xff25303a};
    public static final int[] ACCENTS = {0xffd7fb83, 0xffc1b6fc, 0xfff4ffe6, 0xfffff0bd, 0xffefe8ff, 0xffe1edf4};
    private KeyboardPreferences() {}
    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences("velocity_native", Context.MODE_PRIVATE);
    }
    public static int themeIndex(Context context) {
        int index = Arrays.asList(THEMES).indexOf(get(context).getString("theme", "velocity"));
        return Math.max(0, index);
    }
    public static boolean validTheme(String theme) { return Arrays.asList(THEMES).contains(theme); }
    public static boolean validHand(String hand) { return "full".equals(hand) || "left".equals(hand) || "right".equals(hand); }
}
