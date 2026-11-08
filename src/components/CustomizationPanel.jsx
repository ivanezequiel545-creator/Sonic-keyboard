import React, { useState } from "react";
import { DEFAULT_SETTINGS, PRESETS } from "../preferences.js";
const sliders = [
  ["keyHeight", "Altura de teclas", 40, 68, "px"],
  ["keyGap", "Separación", 1, 8, "px"],
  ["keyRadius", "Bordes", 0, 20, "px"],
  ["keyTextSize", "Tamaño de letra", 14, 24, "px"],
  ["bottomPadding", "Margen inferior", 0, 36, "px"],
  ["handWidth", "Ancho a una mano", 65, 90, "%"],
  ["keyOpacity", "Opacidad de teclas", 65, 100, "%"],
];
export default function CustomizationPanel({
  settings,
  onChange,
  background,
  onBackground,
  onApply,
  native,
}) {
  const [error, setError] = useState("");
  async function upload(file) {
    if (!file) return;
    setError("");
    if (
      !["image/png", "image/jpeg", "image/webp"].includes(file.type) ||
      file.size > 8 * 1024 * 1024
    ) {
      setError("Elige una imagen PNG, JPEG o WebP de hasta 8 MB.");
      return;
    }
    let url;
    try {
      url = URL.createObjectURL(file);
      const image = new Image();
      image.src = url;
      await image.decode();
      const scale = Math.min(1, 1024 / Math.max(image.width, image.height));
      const canvas = document.createElement("canvas");
      canvas.width = Math.max(1, Math.round(image.width * scale));
      canvas.height = Math.max(1, Math.round(image.height * scale));
      canvas
        .getContext("2d")
        .drawImage(image, 0, 0, canvas.width, canvas.height);
      onBackground(canvas.toDataURL("image/jpeg", 0.82));
    } catch {
      setError("No se pudo procesar esta imagen.");
    } finally {
      if (url) URL.revokeObjectURL(url);
    }
  }
  return (
    <section
      className="customizer card"
      aria-label="Personalización del teclado"
    >
      <div className="customizer-heading">
        <span className="overline">DISEÑA TU FORMA DE ESCRIBIR</span>
        <h2>Tu zona de confort.</h2>
        <p>Un buen teclado se adapta a tus manos, no al revés.</p>
      </div>
      <div className="comfort-presets">
        {[
          ["comfortable", "☁", "Cómodo"],
          ["compact", "↘", "Compacto"],
          ["accessible", "Aa", "Accesible"],
        ].map(([id, icon, name]) => (
          <button
            key={id}
            onClick={() => onChange({ ...settings, ...PRESETS[id] })}
          >
            <span>{icon}</span>
            <strong>{name}</strong>
            <small>
              {id === "comfortable"
                ? "Más espacio, menos errores"
                : id === "compact"
                  ? "Recupera tu pantalla"
                  : "Teclas y letras más grandes"}
            </small>
          </button>
        ))}
      </div>
      <div className="customizer-sliders">
        {sliders.map(([key, label, min, max, unit]) => (
          <label key={key} className="range-control">
            <span>
              {label}
              <output>
                {settings[key]} {unit}
              </output>
            </span>
            <input
              aria-label={label}
              type="range"
              min={min}
              max={max}
              value={settings[key]}
              onChange={(e) =>
                onChange({ ...settings, [key]: Number(e.target.value) })
              }
            />
          </label>
        ))}
      </div>
      <div className="customizer-colors">
        <label className="checkbox-control">
          <input
            type="checkbox"
            checked={settings.customColors}
            onChange={(e) =>
              onChange({ ...settings, customColors: e.target.checked })
            }
          />{" "}
          Usar mis colores
        </label>
        <div className="color-controls">
          {[
            ["backgroundColor", "Fondo"],
            ["keyColor", "Teclas"],
            ["accentColor", "Acento"],
          ].map(([key, label]) => (
            <label key={key}>
              <input
                type="color"
                aria-label={`Color de ${label.toLowerCase()}`}
                value={settings[key]}
                onChange={(e) =>
                  onChange({
                    ...settings,
                    customColors: true,
                    [key]: e.target.value,
                  })
                }
              />
              {label}
            </label>
          ))}
        </div>
        <p>
          El color del texto de las teclas se calcula para mantener contraste.
          La opacidad puede afectar a la legibilidad sobre fotos.
        </p>
      </div>
      <div className="background-upload">
        <div>
          <h3>Un fondo que sea tuyo.</h3>
          <p>
            Se reduce a 1024 px y se guarda en este dispositivo. Nunca se sube.
          </p>
        </div>
        <label className="button secondary small import-button">
          Elegir imagen
          <input
            aria-label="Imagen de fondo"
            type="file"
            accept="image/png,image/jpeg,image/webp"
            onChange={(e) => {
              upload(e.target.files?.[0]);
              e.target.value = "";
            }}
          />
        </label>
        {background && (
          <button className="text-link" onClick={() => onBackground("")}>
            Quitar foto
          </button>
        )}
      </div>
      <div className="customizer-toggles">
        {[
          ["numberRow", "Fila de números permanente"],
          ["rememberEmojis", "Recordar emojis recientes"],
          ["incognito", "Modo privado · sin recientes"],
          ["doubleSpacePeriod", "Punto con doble espacio (nativo)"],
        ].map(([key, label]) => (
          <label className="checkbox-control" key={key}>
            <input
              type="checkbox"
              checked={settings[key]}
              onChange={(e) =>
                onChange({ ...settings, [key]: e.target.checked })
              }
            />
            {label}
          </label>
        ))}
      </div>
      {error && (
        <p className="customizer-error" role="alert">
          {error}
        </p>
      )}
      <div className="customizer-footer">
        <button
          className="button secondary"
          onClick={() => {
            onChange({ ...DEFAULT_SETTINGS, hand: settings.hand });
            onBackground("");
          }}
        >
          Restablecer diseño
        </button>
        {native && (
          <button className="button primary" onClick={onApply}>
            Aplicar al teclado Android →
          </button>
        )}
      </div>
      <div className="comfort-tips">
        <strong>Pequeños gestos, gran diferencia.</strong>
        <p>
          En Android: mantén ⌫ para borrar; desliza el espacio para mover el
          cursor; mantén una letra para acentos. ⋯ abre herramientas de edición.
          Nunca se guarda historial del portapapeles.
        </p>
      </div>
    </section>
  );
}
