// Integration health check only: never copies third-party media into the APK.
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { remoteFile } from '../src/remote-media.js';
const items=JSON.parse(await readFile('public/media/sources.json','utf8'));
const results=[];
let next=0;
async function worker(){
  while(next<items.length){
    const item=items[next++];
    try{
      const file=await remoteFile(item,AbortSignal.timeout(25000));
      results.push({id:item.id,ok:true,mime:file.type,bytes:file.size});
    }catch(error){results.push({id:item.id,ok:false,error:String(error.cause?.message||error.message).slice(0,240)});}
  }
}
await Promise.all([worker(),worker()]);
await mkdir('android/app/build/reports',{recursive:true});
await writeFile('android/app/build/reports/media-sources.json',JSON.stringify({checkedAt:new Date().toISOString(),scope:'GitHub runner network; not phone availability or browser CORS',results},null,2));
for(const result of results)console.log(`::notice title=Media source availability::${result.id}: ${result.ok?`OK ${result.mime}, ${result.bytes} bytes`:`UNAVAILABLE ${result.error}`}`);
