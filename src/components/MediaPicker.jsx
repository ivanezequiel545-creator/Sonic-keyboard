import RemoteMediaGallery from "./RemoteMediaGallery.jsx";
import React, { useEffect, useState } from "react";

export function MediaTabs({ value, onChange }) {
  return <div className="media-tabs" role="tablist" aria-label="Contenido del teclado">
    {[["emoji", "Emojis"], ["gif", "GIFs"], ["sticker", "Stickers"]].map(([id, label]) =>
      <button type="button" role="tab" aria-selected={value === id} key={id} onClick={() => onChange(id)}>{label}</button>)}
  </div>;
}

export default function MediaPicker({ kind, incognito = false }) {
  const [animate, setAnimate] = useState(false);
  const [items, setItems] = useState([]), [selected, setSelected] = useState(null), [status, setStatus] = useState("");
  useEffect(() => { let alive = true; fetch("/media/catalog.json").then(r => { if (!r.ok) throw Error(); return r.json(); }).then(data => { if (alive) setItems(data); }).catch(() => { if (alive) setStatus("No se pudo abrir el pack local."); }); return () => { alive = false; }; }, []);
  useEffect(() => { setSelected(null); setStatus(""); }, [kind]);
  async function share() {
    if (!selected) return;
    try {
      const response = await fetch(`/media/${selected.file}`); if (!response.ok) throw Error();
      const file = new File([await response.blob()], selected.file, { type: selected.mime });
      if (!navigator.canShare?.({ files: [file] })) { setStatus("Este navegador no comparte archivos. Usa Descargar."); return; }
      await navigator.share({ files: [file], title: selected.label }); setStatus("Archivo compartido.");
    } catch (error) { setStatus(error.name === "AbortError" ? "No se compartió ningún archivo." : "No se pudo compartir. Usa Descargar."); }
  }
  return <div className="media-library">
    <RemoteMediaGallery kind={kind} incognito={incognito} />
    <h4>En mi dispositivo · sin conexión</h4>
    <p>Pack original y sin conexión. En Android, envía desde el teclado en apps compatibles; aquí puedes descargar o compartir el archivo. Importa tus propios archivos desde los ajustes nativos.</p>
    {kind === "gif" && <label className="media-animation"><input type="checkbox" checked={animate} onChange={event => setAnimate(event.target.checked)} /> Animar miniaturas GIF</label>}
    <div className="media-grid">{items.filter(item => item.kind === kind).map(item =>
      <button type="button" className={selected?.id === item.id ? "selected" : ""} key={item.id} aria-label={`Seleccionar ${item.label}`} aria-pressed={selected?.id === item.id} onClick={() => setSelected(item)}>
        <img src={`/media/${item.kind === "gif" && !animate ? item.file.replace(/\.gif$/, ".png") : item.file}`} alt="" loading="lazy" /><span>{item.label}</span>
      </button>)}</div>
    {selected && <div className="media-actions"><a download={selected.file} href={`/media/${selected.file}`}>Descargar {selected.kind === "gif" ? "GIF" : "sticker"}</a><button type="button" onClick={share}>Compartir archivo</button></div>}
    <p role="status">{status}</p>
    <small>La selección remota se carga solo al pedirlo; no se envían tus palabras. Los stickers se comparten como imágenes; cada app decide cómo mostrarlos.</small>
  </div>;
}
