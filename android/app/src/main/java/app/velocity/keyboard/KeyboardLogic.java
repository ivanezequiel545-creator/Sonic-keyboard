package app.velocity.keyboard;

/** Pure functions shared by the IME and its tests. No keyboard content is retained. */
public final class KeyboardLogic {
    private KeyboardLogic() {}
    public static int codePointLengthAtEnd(CharSequence text) {
        return text == null || text.length() == 0 ? 0 : Character.charCount(Character.codePointBefore(text, text.length()));
    }
    public static String accent(String key) {
        switch (key.toLowerCase(java.util.Locale.ROOT)) {
            case "a": return "á";
            case "e": return "é";
            case "i": return "í";
            case "o": return "ó";
            case "u": return "ú";
            case "n": return "ñ";
            default: return null;
        }
    }
}
