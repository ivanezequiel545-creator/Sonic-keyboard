package app.velocity.keyboard;

import static org.junit.Assert.*;

import org.junit.Test;

public class KeyboardLogicTest {

  @Test
  public void deletesUnicodeCodePointsNotHalfOfSurrogatePairs() {
    assertEquals(2, KeyboardLogic.codePointLengthAtEnd("a😊"));
    assertEquals(1, KeyboardLogic.codePointLengthAtEnd("año"));
    assertEquals(0, KeyboardLogic.codePointLengthAtEnd(""));
    assertEquals(0, KeyboardLogic.codePointLengthAtEnd(null));
  }

  @Test
  public void supportsSpanishLongPressAccents() {
    assertEquals("á", KeyboardLogic.accent("a"));
    assertEquals("ú", KeyboardLogic.accent("U"));
    assertEquals("ñ", KeyboardLogic.accent("n"));
    assertNull(KeyboardLogic.accent("z"));
  }

  @Test
  public void removesCompleteEmojiAndSpanishAccents() {
    for (String emoji : new String[] {
      "😊",
      "👩🏽‍💻",
      "👨‍👩‍👧‍👦",
      "🇻🇪",
      "1️⃣",
      "🏳️‍🌈",
      "👍🏿",
      "🫱🏽‍🫲🏻",
      "🏴󠁧󠁢󠁷󠁬󠁳󠁿",
    })
      assertEquals(
        emoji,
        emoji.length(),
        KeyboardLogic.previousGraphemeLength("hola" + emoji)
      );
    assertEquals(2, KeyboardLogic.previousGraphemeLength("cafe\u0301"));
    assertEquals(2, KeyboardLogic.previousGraphemeLength("🇻"));
    assertEquals(0, KeyboardLogic.previousGraphemeLength(null));
  }
}
