package app.velocity.keyboard;

import org.junit.Test;
import static org.junit.Assert.*;

public class KeyboardLogicTest {
    @Test public void deletesUnicodeCodePointsNotHalfOfSurrogatePairs() {
        assertEquals(2,KeyboardLogic.codePointLengthAtEnd("a😊"));
        assertEquals(1,KeyboardLogic.codePointLengthAtEnd("año"));
        assertEquals(0,KeyboardLogic.codePointLengthAtEnd(""));
        assertEquals(0,KeyboardLogic.codePointLengthAtEnd(null));
    }
    @Test public void supportsSpanishLongPressAccents() {
        assertEquals("á",KeyboardLogic.accent("a"));
        assertEquals("ú",KeyboardLogic.accent("U"));
        assertEquals("ñ",KeyboardLogic.accent("n"));
        assertNull(KeyboardLogic.accent("z"));
    }
}
