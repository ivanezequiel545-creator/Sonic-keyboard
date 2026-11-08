import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
test('original media pack matches native assets and contains real animated GIFs and transparent PNGs',()=>{
 const catalog=JSON.parse(readFileSync('public/media/catalog.json','utf8'));
 assert.equal(catalog.length,10);assert.equal(catalog.filter(x=>x.kind==='gif').length,4);
 assert.equal(new Set(catalog.map(x=>x.id)).size,10);
 assert.deepEqual(catalog,JSON.parse(readFileSync('android/app/src/main/assets/media/catalog.json','utf8')));
 for(const item of catalog){
  assert.match(item.file,/^[a-z0-9-]+\.(png|gif)$/);
  const data=readFileSync('public/media/'+item.file);
  assert.deepEqual(data,readFileSync('android/app/src/main/assets/media/'+item.file));
  assert.ok(data.length<150000);
  if(item.kind==='gif'){assert.equal(item.mime,'image/gif');assert.equal(data.subarray(0,6).toString(),'GIF89a');assert.ok(data.includes(Buffer.from('NETSCAPE2.0')));}
  else{assert.equal(item.mime,'image/png');assert.equal(data.subarray(1,4).toString(),'PNG');assert.equal(data[25],6,'PNG must retain RGBA transparency');}
 }
});
