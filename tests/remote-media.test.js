import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {safeMediaUrl,safeSourceUrl,validRemoteItem,matchesMediaHeader,remoteFile,MAX_REMOTE_BYTES} from '../src/remote-media.js';
const items=JSON.parse(readFileSync('public/media/sources.json','utf8'));
test('six GIPHY references and three stickers retain their sources, not redistributed media',()=>{
 assert.equal(items.length,9);assert.equal(items.filter(x=>x.kind==='gif').length,6);assert.equal(new Set(items.map(x=>x.id)).size,9);
 assert.deepEqual(items,JSON.parse(readFileSync('android/app/src/main/assets/media/sources.json','utf8')));
 for(const item of items){assert.ok(validRemoteItem(item),item.id);if(item.kind==='sticker'){assert.equal(item.author,'itoshiisagi7');assert.equal(item.collection,'https://sticker.ly/s/G0VY8R');}else assert.equal(item.collection,'https://giphy.com/explore/famosos');}
});
test('remote URLs reject credentials, lookalike hosts, redirects, alternate ports and unexpected paths',()=>{
 for(const url of ['http://media.giphy.com/media/id/200.gif','https://media.giphy.com.evil.test/media/id/200.gif','https://user:pass@media.giphy.com/media/id/200.gif','https://media.giphy.com:8080/media/id/200.gif','https://media.giphy.com/media/id/200.gif?redirect=localhost','https://127.0.0.1/a.gif','file:///a.gif','https://stickerly.pstatic.net/private/a.webp'])assert.equal(safeMediaUrl(url),false,url);
 assert.equal(safeSourceUrl('javascript:alert(1)'),false);assert.equal(safeSourceUrl('https://sticker.ly.evil.test/s/G0VY8R'),false);
 assert.equal(validRemoteItem({...items[0],mime:'image/webp'}),false);
});
test('remote downloads check headers and enforce limits even without Content-Length',async()=>{
 const original=global.fetch;
 try{
  global.fetch=async(url,opts)=>{assert.equal(url,items[0].url);assert.equal(opts.credentials,'omit');assert.equal(opts.redirect,'error');assert.equal(opts.referrerPolicy,'no-referrer');return new Response(readFileSync('public/media/hola.gif'));};
  assert.ok((await remoteFile(items[0])).size>100);
  global.fetch=async()=>new Response('<html>not a gif</html>');await assert.rejects(remoteFile(items[0]),/Formato/);
  global.fetch=async()=>new Response('x',{headers:{'Content-Length':String(MAX_REMOTE_BYTES+1)}});await assert.rejects(remoteFile(items[0]),/demasiado/);
  global.fetch=async()=>new Response(new Uint8Array(MAX_REMOTE_BYTES+1));await assert.rejects(remoteFile(items[0]),/4 MB/);
 }finally{global.fetch=original;}
 assert.equal(matchesMediaHeader(new TextEncoder().encode('GIF89a'), 'image/gif'),true);
 assert.equal(matchesMediaHeader(new TextEncoder().encode('RIFF1234WEBP'), 'image/webp'),true);
});
