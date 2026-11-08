package app.velocity.keyboard;
import static org.junit.Assert.*;
import org.junit.Test;
import java.io.*;
import java.util.*;
public class SpanishSuggestionsTest {
 @Test public void bundledDictionaryIsCompleteAndSuggestsSpanish()throws Exception{
  SpanishSuggestions engine=new SpanishSuggestions(new InputStreamReader(new FileInputStream("src/main/assets/data/es-words.tsv"),"UTF-8"));
  assertEquals(12000,engine.size());assertTrue(engine.suggest("hol").contains("hola"));assertTrue(engine.suggest("Hol").contains("Hola"));assertEquals("holaa",engine.suggest("holaa").get(0));assertTrue(engine.suggest("holaa").contains("hola"));assertTrue(engine.suggest("telefono").contains("teléfono"));assertEquals(Arrays.asList("hola","gracias","bien"),engine.suggest(""));
 }
 @Test public void correctionIsConservativeAndPreservesCase(){
  assertEquals("hola",SpanishSuggestions.automaticCorrection("holaa"));assertEquals("Gracias",SpanishSuggestions.automaticCorrection("Grasias"));assertEquals("TAMBIÉN",SpanishSuggestions.automaticCorrection("TAMBIEN"));
  for(String text:new String[]{"si","el","tu","mas","que","como","esta","anos","Ivanezequiel","https://holaa.com","hola123"})assertEquals(text,SpanishSuggestions.automaticCorrection(text));
  assertNotEquals(SpanishSuggestions.fold("año"),SpanishSuggestions.fold("ano"));
 }
 @Test public void extractsOnlyAnEligibleWordAtTheCursor(){
  assertEquals("hola",SpanishSuggestions.currentWord("¿hola"));assertEquals("mundo",SpanishSuggestions.currentWord("hola mundo"));assertEquals("",SpanishSuggestions.currentWord("hola "));for(String text:new String[]{"a@holaa","https://holaa","hola123","uno-dos","hola.com"})assertEquals("",SpanishSuggestions.currentWord(text));
  assertTrue(SpanishSuggestions.wordContinues("a"));assertFalse(SpanishSuggestions.wordContinues(" "));
 }
 @Test public void editDistanceIncludesOneInsertionDeletionSubstitutionOrSwap(){
  for(String[] pair:new String[][]{{"holaa","hola"},{"hola","holaa"},{"gracais","gracias"},{"holi","hola"}})assertTrue(SpanishSuggestions.oneEdit(pair[0],pair[1]));assertFalse(SpanishSuggestions.oneEdit("holaa","mundo"));
 }
 @Test public void protectedEditorsDoNotAllowSuggestions(){
  int text=android.text.InputType.TYPE_CLASS_TEXT;
  assertTrue(VelocityInputMethodService.acceptsSuggestions(text,0,false));
  for(int variation:new int[]{android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD,android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,android.text.InputType.TYPE_TEXT_VARIATION_URI})assertFalse(VelocityInputMethodService.acceptsSuggestions(text|variation,0,false));
  assertFalse(VelocityInputMethodService.acceptsSuggestions(text|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,0,false));assertFalse(VelocityInputMethodService.acceptsSuggestions(text,0x1000000,false));assertFalse(VelocityInputMethodService.acceptsSuggestions(text,0,true));assertFalse(VelocityInputMethodService.acceptsSuggestions(android.text.InputType.TYPE_CLASS_NUMBER,0,false));
 }
}
