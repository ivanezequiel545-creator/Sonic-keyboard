package app.velocity.keyboard;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Immutable local word-frequency index. Never learns, stores or sends user text. */
public final class SpanishSuggestions {
    private static final Locale ES=new Locale("es");
    private static final Pattern WORD=Pattern.compile("(?:^|\\s|[¿¡(\"“])([a-záéíóúüñ]+)$",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    private static final Map<String,String> FIXES=new HashMap<>();
    static {
        String[][] pairs={{"holaa","hola"},{"grasias","gracias"},{"grasia","gracias"},{"gracais","gracias"},{"buenoz","buenos"},{"tambien","también"},{"despues","después"},{"adios","adiós"},{"dias","días"},{"dia","día"},{"estan","están"},{"estais","estáis"},{"asi","así"},{"perdon","perdón"},{"rapido","rápido"},{"facil","fácil"},{"dificil","difícil"},{"telefono","teléfono"},{"tecldo","teclado"},{"tecaldo","teclado"},{"porqe","porque"},{"porqeu","porque"},{"nesecito","necesito"},{"necesitp","necesito"},{"buenass","buenas"}};
        for(String[] pair:pairs)FIXES.put(pair[0],pair[1]);
    }
    private final List<String> words=new ArrayList<>();
    private final Set<String> known=new HashSet<>();
    private final Map<String,List<String>> prefixes=new HashMap<>();
    public SpanishSuggestions(Reader source)throws IOException {
        try(BufferedReader reader=new BufferedReader(source)){String line;while((line=reader.readLine())!=null){String word=line.split("\\s+")[0];if(!word.matches("[a-záéíóúüñ]{2,22}")||!known.add(word))continue;words.add(word);String folded=fold(word);for(int i=1;i<=Math.min(3,folded.length());i++){String prefix=folded.substring(0,i);List<String> bucket=prefixes.get(prefix);if(bucket==null){bucket=new ArrayList<>();prefixes.put(prefix,bucket);}bucket.add(word);}}}
    }
    public int size(){return words.size();}
    public static String currentWord(CharSequence before){if(before==null)return "";Matcher match=WORD.matcher(before);return match.find()?match.group(1):"";}
    public static boolean wordContinues(CharSequence after){return after!=null&&after.length()>0&&Character.isLetter(after.charAt(0));}
    /** Deliberately conservative: ambiguous si/sí, el/él, tu/tú, que/qué never auto-rewrite. */
    public static String automaticCorrection(String word){String replacement=FIXES.get(word.toLowerCase(ES));return replacement==null?word:matchCase(replacement,word);}
    public static String fold(String word){return word.toLowerCase(ES).replace('á','a').replace('é','e').replace('í','i').replace('ó','o').replace('ú','u').replace('ü','u');}
    public static String matchCase(String word,String typed){if(typed.isEmpty())return word;if(typed.length()>1&&typed.equals(typed.toUpperCase(ES)))return word.toUpperCase(ES);return Character.isUpperCase(typed.charAt(0))?word.substring(0,1).toUpperCase(ES)+word.substring(1):word;}
    public List<String> suggest(String typed){
        if(typed.isEmpty())return Arrays.asList("hola","gracias","bien");
        LinkedHashSet<String> result=new LinkedHashSet<>();result.add(typed);
        String folded=fold(typed);String fix=automaticCorrection(typed);if(!fix.equals(typed))result.add(fix);
        List<String> bucket=prefixes.get(folded.substring(0,Math.min(3,folded.length())));
        if(bucket!=null)for(String word:bucket)if(fold(word).startsWith(folded)){result.add(matchCase(word,typed));if(result.size()>=3)break;}
        if(result.size()<3&&typed.length()>=3)for(String word:words){if(oneEdit(folded,fold(word))){result.add(matchCase(word,typed));if(result.size()>=3)break;}}
        return new ArrayList<>(result);
    }
    public static boolean oneEdit(String a,String b){
        if(Math.abs(a.length()-b.length())>1)return false;int i=0,j=0,edits=0;
        while(i<a.length()&&j<b.length()){if(a.charAt(i)==b.charAt(j)){i++;j++;continue;}if(++edits>1)return false;if(a.length()==b.length()){if(i+1<a.length()&&j+1<b.length()&&a.charAt(i)==b.charAt(j+1)&&a.charAt(i+1)==b.charAt(j)){i+=2;j+=2;}else{i++;j++;}}else if(a.length()>b.length())i++;else j++;}
        return edits+(a.length()-i)+(b.length()-j)<=1;
    }
}
