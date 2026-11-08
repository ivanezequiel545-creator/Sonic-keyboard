// Frequency data is CC BY-SA 4.0; see /licenses/spanish-words.txt.
// No user words are learned, stored or sent to the network.
export function currentSpanishWord(before='') {
 return String(before).match(/(?:^|\s|[¿¡("“])([a-záéíóúüñ]+)$/iu)?.[1] || '';
}
export function foldSpanish(word){return word.toLocaleLowerCase('es').replace(/[áéíóúü]/g,c=>({'á':'a','é':'e','í':'i','ó':'o','ú':'u','ü':'u'}[c]));}
export function matchSpanishCase(word,typed){if(typed.length>1&&typed===typed.toLocaleUpperCase('es'))return word.toLocaleUpperCase('es');return /^[A-ZÁÉÍÓÚÜÑ]/.test(typed)?word[0].toLocaleUpperCase('es')+word.slice(1):word;}
export function spanishCompletions(words,typed){
 if(!typed)return ['hola','gracias','bien'];const result=new Set([typed]);const prefix=foldSpanish(typed);
 for(const [word] of words)if(foldSpanish(word).startsWith(prefix)){result.add(matchSpanishCase(word,typed));if(result.size()===3)break;}
 return [...result];
}
let cached;
export function loadSpanishWords(){return cached ||= fetch('/data/es-words.json').then(r=>{if(!r.ok)throw Error('Diccionario no disponible');return r.json();}).then(data=>data.words).catch(e=>{cached=null;throw e;});}
