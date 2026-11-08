import React, { useEffect, useMemo, useState } from "react";
import MediaPicker, { MediaTabs } from "./MediaPicker.jsx";
import { filterEmojis } from "../preferences.js";
let cached;
export function loadEmojiCatalog() {
  return (cached ||= fetch("/data/emojis-es.json")
    .then((r) => {
      if (!r.ok) throw Error("Catálogo no disponible");
      return r.json();
    })
    .catch((error) => {
      cached = null;
      throw error;
    }));
}
export default function EmojiPicker({
  onInsert,
  remember = true,
  incognito = false,
}) {
  const [mediaMode, setMediaMode] = useState("emoji");
  const [catalog, setCatalog] = useState(null),
    [error, setError] = useState(""),
    [query, setQuery] = useState(""),
    [category, setCategory] = useState("all"),
    [tone, setTone] = useState("all"),
    [page, setPage] = useState(0),
    [selected, setSelected] = useState(null);
  const stored = (key) => {
    try {
      const data = JSON.parse(localStorage.getItem(key) || "[]");
      return Array.isArray(data)
        ? data.filter((x) => typeof x === "string").slice(0, 200)
        : [];
    } catch {
      return [];
    }
  };
  const [recent, setRecent] = useState(() => stored("velocity-emoji-recent")),
    [favorites, setFavorites] = useState(() =>
      stored("velocity-emoji-favorites"),
    );
  useEffect(() => {
    let alive = true;
    loadEmojiCatalog()
      .then((d) => {
        if (alive) setCatalog(d);
      })
      .catch(() => {
        if (alive)
          setError(
            "No se pudo cargar el catálogo. Reabre el panel para reintentar.",
          );
      });
    return () => {
      alive = false;
    };
  }, []);
  useEffect(() => {
    setPage(0);
    setSelected(null);
  }, [query, category, tone]);
  const matches = useMemo(
    () =>
      filterEmojis(catalog?.entries || [], {
        query,
        category,
        tone,
        recent,
        favorites,
      }),
    [catalog, query, category, tone, recent, favorites],
  );
  const persist = (key, value) => {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      /* Private browsing can disable storage. */
    }
  };
  function insert(entry) {
    onInsert(entry.emoji);
    setSelected(entry);
    if (remember && !incognito) {
      const next = [entry.id, ...recent.filter((id) => id !== entry.id)].slice(
        0,
        40,
      );
      setRecent(next);
      persist("velocity-emoji-recent", next);
    }
  }
  function favorite() {
    if (!selected) return;
    const next = favorites.includes(selected.id)
      ? favorites.filter((id) => id !== selected.id)
      : [selected.id, ...favorites].slice(0, 200);
    setFavorites(next);
    persist("velocity-emoji-favorites", next);
  }
  const size = 72,
    pages = Math.max(1, Math.ceil(matches.length / size));
  if (mediaMode !== "emoji") return <section className="emoji-library" aria-label="Catálogo de emojis"><MediaTabs value={mediaMode} onChange={setMediaMode} /><MediaPicker kind={mediaMode} /></section>;
  return (
    <section className="emoji-library" aria-label="Catálogo de emojis">
      <MediaTabs value={mediaMode} onChange={setMediaMode} />
      <div className="emoji-search-row">
        <input
          aria-label="Buscar emojis en español"
          type="search"
          placeholder="Busca: corazón, bandera, gato…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <select
          aria-label="Tono de piel"
          value={tone}
          onChange={(e) => setTone(e.target.value)}
        >
          {[
            ["all", "Todos los tonos"],
            ["0", "Sin modificador"],
            ["1", "🏻 Claro"],
            ["2", "🏼 Claro medio"],
            ["3", "🏽 Medio"],
            ["4", "🏾 Oscuro medio"],
            ["5", "🏿 Oscuro"],
            ["mixed", "Tonos combinados"],
          ].map(([v, label]) => (
            <option key={v} value={v}>
              {label}
            </option>
          ))}
        </select>
      </div>
      <div className="emoji-categories" aria-label="Categorías de emojis">
        {[
          { id: "all", icon: "∞", name: "Todos" },
          { id: "recent", icon: "◷", name: "Recientes" },
          { id: "favorites", icon: "★", name: "Favoritos" },
          ...(catalog?.categories || []),
        ].map((c) => (
          <button
            key={c.id}
            className={category === c.id ? "selected" : ""}
            aria-pressed={category === c.id}
            onClick={() => setCategory(c.id)}
          >
            {c.icon} {c.name}
          </button>
        ))}
      </div>
      {!catalog ? (
        <p role="status">{error || "Cargando catálogo local…"}</p>
      ) : (
        <>
          <div className="emoji-result-count">
            {matches.length.toLocaleString("es")} resultados · Unicode{" "}
            {catalog.unicode} ·{" "}
            {incognito
              ? "Modo privado: sin recientes"
              : remember
                ? "Recientes solo en este dispositivo"
                : "Recientes desactivados"}
          </div>
          <div className="emoji-grid">
            {matches.slice(page * size, (page + 1) * size).map((entry) => (
              <button
                key={entry.id}
                title={entry.label}
                aria-label={entry.emoji}
                onClick={() => insert(entry)}
                onContextMenu={(e) => {
                  e.preventDefault();
                  setSelected(entry);
                }}
              >
                {entry.emoji}
              </button>
            ))}
          </div>
          {!matches.length && (
            <p className="emoji-empty">
              No hay emojis aquí. Prueba otra categoría, tono o búsqueda.
            </p>
          )}
          <div className="emoji-pagination">
            <button
              disabled={!page}
              onClick={() => setPage((p) => p - 1)}
              aria-label="Página anterior de emojis"
            >
              ←
            </button>
            <span>
              {page + 1} / {pages}
            </span>
            <button
              disabled={page + 1 >= pages}
              onClick={() => setPage((p) => p + 1)}
              aria-label="Página siguiente de emojis"
            >
              →
            </button>
            <button disabled={!selected} onClick={favorite}>
              {selected
                ? favorites.includes(selected.id)
                  ? "★ Quitar favorito"
                  : "☆ Guardar favorito"
                : "Selecciona un emoji"}
            </button>
            {category === "recent" && (
              <button
                onClick={() => {
                  setRecent([]);
                  persist("velocity-emoji-recent", []);
                }}
              >
                Borrar recientes
              </button>
            )}
          </div>
          {selected && (
            <div className="emoji-selection">
              {selected.emoji} {selected.label}
            </div>
          )}
          <small className="emoji-compatibility">
            Incluye variantes, banderas y componentes. La apariencia y
            compatibilidad dependen del sistema.{" "}
            <a href="/licenses/emojibase.txt" target="_blank" rel="noreferrer">
              Datos Emojibase · MIT
            </a>
          </small>
        </>
      )}
    </section>
  );
}
