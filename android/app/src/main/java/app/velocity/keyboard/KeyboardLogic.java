package app.velocity.keyboard;

/** Pure Unicode/Spanish helpers; no editor contents are retained. */
public final class KeyboardLogic {

  private KeyboardLogic() {}

  public static int codePointLengthAtEnd(CharSequence text) {
    return text == null || text.length() == 0
      ? 0
      : Character.charCount(Character.codePointBefore(text, text.length()));
  }

  private static boolean extend(int cp) {
    int type = Character.getType(cp);
    return (
      type == Character.NON_SPACING_MARK ||
      type == Character.COMBINING_SPACING_MARK ||
      type == Character.ENCLOSING_MARK ||
      cp == 0xfe0f ||
      cp == 0xfe0e ||
      (cp >= 0x1f3fb && cp <= 0x1f3ff) ||
      (cp >= 0xe0020 && cp <= 0xe007f)
    );
  }

  private static boolean regional(int cp) {
    return cp >= 0x1f1e6 && cp <= 0x1f1ff;
  }

  public static int previousGraphemeLength(CharSequence text) {
    if (text == null || text.length() == 0) return 0;
    int end = text.length(),
      i = end;
    int cp = Character.codePointBefore(text, i);
    if (regional(cp)) {
      int n = 0,
        j = i;
      while (j > 0 && regional(Character.codePointBefore(text, j))) {
        j -= Character.charCount(Character.codePointBefore(text, j));
        n++;
      }
      return Math.min(n % 2 == 0 ? 4 : 2, end);
    }
    while (i > 0 && extend(Character.codePointBefore(text, i)))
      i -= Character.charCount(Character.codePointBefore(text, i));
    if (i > 0) i -= Character.charCount(Character.codePointBefore(text, i));
    while (i > 0 && Character.codePointBefore(text, i) == 0x200d) {
      i--;
      while (i > 0 && extend(Character.codePointBefore(text, i)))
        i -= Character.charCount(Character.codePointBefore(text, i));
      if (i > 0) i -= Character.charCount(Character.codePointBefore(text, i));
    }
    return end - i;
  }

  public static String accent(String key) {
    switch (key.toLowerCase(java.util.Locale.ROOT)) {
      case "a":
        return "á";
      case "e":
        return "é";
      case "i":
        return "í";
      case "o":
        return "ó";
      case "u":
        return "ú";
      case "n":
        return "ñ";
      default:
        return null;
    }
  }
}
