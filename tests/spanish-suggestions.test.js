import test from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs';
import {currentSpanishWord,foldSpanish,spanishCompletions} from '../src/spanish-suggestions.js';
const data=JSON.parse(fs.readFileSync(new URL('../public/data/es-words.json',import.meta.url)));
test('12,000 pinned Spanish words match the native dictionary and preserve attribution',()=>{
 assert.equal(data.count,12000);assert.equal(data.words.length,12000);assert.equal(new Set(data.words.map(([w])=>w)).size,12000);assert.equal(data.license,'CC-BY-SA-4.0');
 assert.equal(fs.readFileSync(new URL('../android/app/src/main/assets/data/es-words.tsv',import.meta.url),'utf8'),data.words.map(([w,n])=>`${w}\t${n}\n`).join(''));
 assert.match(fs.readFileSync(new URL('../public/licenses/spanish-words.txt',import.meta.url),'utf8'),/Changes by Velocity/);
});
test('Spanish prefix completion preserves case and ñ and does not parse URLs as words',()=>{
 assert.equal(currentSpanishWord('hola mundo'),'mundo');assert.equal(currentSpanishWord('¿hola'),'hola');for(const word of ['a@hola','https://hola','hola123','hola '])assert.equal(currentSpanishWord(word),'');
 assert.ok(spanishCompletions(data.words,'hol').includes('hola'));assert.ok(spanishCompletions(data.words,'Hol').includes('Hola'));assert.notEqual(foldSpanish('año'),foldSpanish('ano'));
});
