// Only the fixed selection is supported: no HTML scraping, API keys or typed queries.
export const MAX_REMOTE_BYTES = 4 * 1024 * 1024;
export function safeMediaUrl(value) {
  try {
    const u = new URL(value);
    return u.protocol === 'https:' && !u.username && !u.password && !u.port && !u.search && !u.hash &&
      (/^(media[0-4]\.giphy\.com)$/.test(u.hostname) || u.hostname === 'stickerly.pstatic.net') &&
      (u.hostname !== 'stickerly.pstatic.net' ? /^\/media\/[A-Za-z0-9]+\/200\.gif$/.test(u.pathname) : /^\/sticker_pack\/[A-Za-z0-9]+\/G0VY8R\/15\/-?\d+\.webp$/.test(u.pathname));
  } catch { return false; }
}
export function safeSourceUrl(value) {
  try { const u=new URL(value); return u.protocol==='https:'&&!u.username&&!u.password&&!u.port&&!u.search&&!u.hash&&((u.hostname==='giphy.com'&&/^\/gifs\/[a-zA-Z0-9-]+$/.test(u.pathname))||(u.hostname==='sticker.ly'&&u.pathname==='/s/G0VY8R')); } catch { return false; }
}
export function validRemoteItem(item) {
  return item && /^[a-z0-9-]+$/.test(item.id) && /^[a-z0-9-]+\.(gif|webp)$/.test(item.file) &&
    safeMediaUrl(item.url) && safeSourceUrl(item.source) &&
    ((item.provider==='GIPHY'&&item.kind==='gif'&&item.mime==='image/gif'&&item.file.endsWith('.gif')&&/^media[0-4]\.giphy\.com$/.test(new URL(item.url).hostname))||
     (item.provider==='Sticker.ly'&&item.kind==='sticker'&&item.mime==='image/webp'&&item.file.endsWith('.webp')&&new URL(item.url).hostname==='stickerly.pstatic.net'));
}
export function matchesMediaHeader(bytes, mime) {
  if(mime==='image/gif')return ['GIF87a','GIF89a'].includes(String.fromCharCode(...bytes.slice(0,6)));
  return mime==='image/webp'&&String.fromCharCode(...bytes.slice(0,4))==='RIFF'&&String.fromCharCode(...bytes.slice(8,12))==='WEBP';
}
export async function remoteFile(item, signal) {
  if(!validRemoteItem(item))throw Error('Fuente no permitida');
  const response=await fetch(item.url,{signal,credentials:'omit',referrerPolicy:'no-referrer',redirect:'error'});
  if(!response.ok||Number(response.headers.get('Content-Length'))>MAX_REMOTE_BYTES)throw Error('Archivo no disponible o demasiado grande');
  if(!response.body)throw Error('Descarga no disponible');
  const reader=response.body.getReader(),chunks=[];let total=0;
  try { while(true){const {value,done}=await reader.read();if(done)break;total+=value.length;if(total>MAX_REMOTE_BYTES)throw Error('Máximo 4 MB');chunks.push(value);} }
  catch(error){await reader.cancel().catch(()=>{});throw error;}
  finally{reader.releaseLock();}
  const blob=new Blob(chunks,{type:item.mime});
  if(!matchesMediaHeader(new Uint8Array(await blob.slice(0,12).arrayBuffer()),item.mime))throw Error('Formato inesperado');
  return new File([blob],item.file,{type:item.mime});
}
