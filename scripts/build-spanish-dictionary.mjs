import {readFile,writeFile} from 'node:fs/promises';
// Run with the pinned FrequencyWords es_50k.txt source as argv[2].
// Source/license/provenance are recorded in public/licenses/spanish-words.txt.
// Normal builds use checked-in data and never contact an external dictionary.
const input=process.argv[2];if(!input)throw Error('Pass a local copy of the pinned es_50k.txt source');
const lines=(await readFile(input,'utf8')).split(/\r?\n/);const words=[],seen=new Set();
for(const line of lines){const [word,count]=line.trim().split(/\s+/);if(/^[a-záéíóúüñ]{2,22}$/.test(word)&&!seen.has(word)){seen.add(word);words.push([word,Number(count)]);}if(words.length===12000)break;}
if(words.length!==12000)throw Error('Expected 12000 words');
const existing=JSON.parse(await readFile('public/data/es-words.json','utf8'));existing.words=words;existing.count=words.length;
await writeFile('public/data/es-words.json',JSON.stringify(existing)+'\n');await writeFile('android/app/src/main/assets/data/es-words.tsv',words.map(([w,n])=>`${w}\t${n}\n`).join(''));
