import React, { useEffect, useRef, useState } from 'react';
import { validRemoteItem, remoteFile } from '../remote-media.js';

export default function RemoteMediaGallery({ kind, incognito }) {
  const [items,setItems]=useState([]),[enabled,setEnabled]=useState(false),[selected,setSelected]=useState(null),[status,setStatus]=useState(''),[busy,setBusy]=useState(false);
  const [animate,setAnimate]=useState(()=>typeof matchMedia==='function'&&!matchMedia('(prefers-reduced-motion: reduce)').matches);
  const request=useRef(null),generation=useRef(0);
  useEffect(()=>{let live=true;fetch('/media/sources.json').then(r=>{if(!r.ok)throw Error();return r.json();}).then(data=>{if(live&&Array.isArray(data))setItems(data.filter(validRemoteItem));}).catch(()=>{if(live)setStatus('Selección no disponible.');});return()=>{live=false;generation.current++;request.current?.abort();};},[]);
  useEffect(()=>{generation.current++;request.current?.abort();setEnabled(false);setSelected(null);setBusy(false);setStatus('');},[kind,incognito]);
  const provider=kind==='gif'?'GIPHY':'Sticker.ly';
  const collection=kind==='gif'?'https://giphy.com/explore/famosos':'https://sticker.ly/s/G0VY8R';
  function hide(){generation.current++;request.current?.abort();setEnabled(false);setSelected(null);setBusy(false);setStatus('');}
  async function action(share){
    if(!selected||busy)return;const current=++generation.current,controller=new AbortController();request.current=controller;setBusy(true);setStatus('Descargando archivo…');
    const timer=setTimeout(()=>controller.abort(),25000);
    try{
      const file=await remoteFile(selected,controller.signal);if(generation.current!==current)return;
      if(share){if(!navigator.canShare?.({files:[file]}))throw Error('Este navegador no comparte archivos; prueba descargar.');await navigator.share({files:[file],title:`${selected.label} · ${selected.provider}`});}
      else {const url=URL.createObjectURL(file),link=document.createElement('a');link.href=url;link.download=file.name;document.body.append(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);}
      if(generation.current===current)setStatus(share?'Archivo compartido.':'Descarga iniciada.');
    }catch(error){if(generation.current===current)setStatus(error.name==='AbortError'?'Descarga cancelada o agotó el tiempo.':'No se pudo obtener el archivo (conexión, permisos del sitio o límite de 4 MB). Puedes abrir su fuente.');}
    finally{clearTimeout(timer);if(generation.current===current)setBusy(false);}
  }
  if(incognito)return <p className="remote-notice">Selección en línea desactivada en modo privado.</p>;
  return <section className="remote-media" aria-label={`Selección de ${provider}`}>
    <h4>{kind==='gif'?'GIPHY · famosos':'Sticker.ly · Los mejores Stickers'}</h4>
    <p>Contenido externo, no libre de derechos. Al cargar, el proveedor recibe tu IP y las peticiones de imagen, no lo que escribes. Necesita Internet.</p>
    <div className="media-actions"><button type="button" disabled={!items.length} onClick={()=>enabled?hide():setEnabled(true)}>{enabled?'Ocultar selección en línea':'Cargar selección en línea'}</button><a href={collection} target="_blank" rel="noopener noreferrer">Abrir página de {provider}</a></div>
    {enabled&&<>
      {kind==='gif'&&<label className="media-animation"><input type="checkbox" checked={animate} onChange={event=>setAnimate(event.target.checked)}/> Animar selección de GIPHY</label>}
      <div className="remote-media-grid">{items.filter(item=>item.kind===kind).map(item=><article key={item.id}>
        <button type="button" disabled={busy} aria-label={`Seleccionar ${item.label}`} aria-pressed={selected?.id===item.id} onClick={()=>{setSelected(item);setStatus('');}}>
          {(kind!=='gif'||animate)?<img src={item.url} alt="" loading="lazy" referrerPolicy="no-referrer" onError={event=>{event.currentTarget.hidden=true;}}/>:<div className="remote-placeholder">GIF</div>}
          <span>{item.label}</span>
        </button>
        <small>{item.author||item.provider}</small><a href={item.source} target="_blank" rel="noopener noreferrer">Ver fuente</a>
      </article>)}</div>
      {selected&&<div className="media-actions"><button disabled={busy} type="button" onClick={()=>action(false)}>Descargar selección</button><button disabled={busy} type="button" onClick={()=>action(true)}>Compartir selección</button></div>}
    </>}
    <p role="status">{status}</p>
  </section>;
}
