import React, { useState, useEffect, useRef, useLayoutEffect } from "react";
import { createRoot } from "react-dom/client";
import {
  ArrowDownToLine,
  ArrowLeftRight,
  ArrowRight,
  ArrowUp,
  BookOpen,
  Check,
  CheckCheck,
  ChevronRight,
  Clipboard,
  Copy,
  Download,
  ExternalLink,
  Github,
  Globe2,
  Hand,
  Heart,
  Home,
  Info,
  Keyboard,
  Languages,
  LayoutGrid,
  Menu,
  Mic,
  Palette,
  Search,
  Settings2,
  ShieldCheck,
  Smile,
  Smartphone,
  Sparkles,
  Trash2,
  WifiOff,
  X,
  Zap,
} from "lucide-react";
import {
  themes,
  translateOffline,
  backgroundSVG,
  packageConfig,
} from "./data.js";
import {
  isNativeApp,
  saveDownload as saveFile,
  androidReleaseUrl,
  androidApkUrl,
  nativeKeyboard,
  configureNativeKeyboard,
} from "./platform.js";
import { androidPreviews } from "./android-previews.js";
import {
  DEFAULT_SETTINGS,
  sanitizeSettings,
  previousGraphemeLength,
  readableInk,
} from "./preferences.js";
import EmojiPicker from "./components/EmojiPicker.jsx";
import { currentSpanishWord, loadSpanishWords, spanishCompletions } from "./spanish-suggestions.js";
import CustomizationPanel from "./components/CustomizationPanel.jsx";
import "./styles.css";

function useStored(key, initial) {
  const [value, setValue] = useState(() => {
    try {
      const raw = localStorage.getItem(key);
      const value = raw ? JSON.parse(raw) : initial;
      return key === "velocity-settings" ? sanitizeSettings(value) : value;
    } catch {
      return initial;
    }
  });
  useEffect(() => {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      /* Storage may be disabled in private mode. */
    }
  }, [key, value]);
  return [value, setValue];
}
const navItems = [
  { id: "home", label: "Inicio", icon: Home },
  { id: "themes", label: "Temas", icon: Palette },
  { id: "customize", label: "Personalizar", icon: Settings2 },
  { id: "tools", label: "Herramientas", icon: LayoutGrid },
  { id: "downloads", label: "Descargas", icon: ArrowDownToLine },
];

function App() {
  const [page, setPage] = useState("home");
  const [themeId, setThemeId] = useStored("velocity-theme", "velocity");
  const theme = themes.find((t) => t.id === themeId) || themes[0];
  const [favorites, setFavorites] = useStored("velocity-favorites", []);
  const [settings, setSettings] = useStored(
    "velocity-settings",
    DEFAULT_SETTINGS,
  );
  const [background, setBackground] = useStored("velocity-background", "");
  const [spanishWords,setSpanishWords]=useState([]);
  const [textCursor,setTextCursor]=useState(0);
  useEffect(()=>{let alive=true;if(settings.suggestions&&!settings.incognito)loadSpanishWords().then(words=>{if(alive)setSpanishWords(words);}).catch(()=>{});return()=>{alive=false;};},[settings.suggestions,settings.incognito]);


  const [history, setHistory] = useStored("velocity-downloads", []);
  const [notes, setNotes] = useStored("velocity-notes", []);
  const [text, setText] = useState("");
  const [shift, setShift] = useState(true);
  const [symbols, setSymbols] = useState(false);
  const [symbolPage, setSymbolPage] = useState(0);
  const [emojiOpen, setEmojiOpen] = useState(false);
  const [modal, setModal] = useState(null);
  const [toast, setToast] = useState("");
  const [mobileMenu, setMobileMenu] = useState(false);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("Todos");
  const [installPrompt, setInstallPrompt] = useState(null);
  const [offline, setOffline] = useState(!navigator.onLine);
  const [downloading, setDownloading] = useState(false);
  const [downloadSelection, setDownloadSelection] = useState(
    themes.map((t) => t.id),
  );
  const [direction, setDirection] = useState("es-en");
  const [translationText, setTranslationText] = useState("");
  const [translationResult, setTranslationResult] = useState(null);
  const [translationAttempted, setTranslationAttempted] = useState(false);
  const [noteText, setNoteText] = useState("");
  const [listening, setListening] = useState(false);
  const [micAvailable] = useState(
    () => !!(window.SpeechRecognition || window.webkitSpeechRecognition),
  );
  const textarea = useRef(null);
  const pendingSelection = useRef(null);
  // Apply caret movement with the controlled DOM commit, never in an old RAF
  // that can race the user's next physical key or selection change.
  useLayoutEffect(() => {
    const pending=pendingSelection.current;
    if(!pending)return;
    pendingSelection.current=null;
    const el=textarea.current;
    if(el&&el.value===pending.value){el.focus();el.setSelectionRange(pending.position,pending.position);}
  },[text,textCursor]);
  function setDraft(value,position){
    pendingSelection.current={value,position};setText(value);setTextCursor(position);
    const el=textarea.current;
    if(el&&el.value===value){pendingSelection.current=null;el.focus();el.setSelectionRange(position,position);}
  }

  const recognition = useRef(null);
  const audioRef = useRef(null);
  const notify = (message) => setToast(message);
  useEffect(() => {
    if (!toast) return;
    const id = setTimeout(() => setToast(""), 4000);
    return () => clearTimeout(id);
  }, [toast]);
  useEffect(() => {
    const install = (e) => {
      e.preventDefault();
      setInstallPrompt(e);
    };
    const online = () => setOffline(!navigator.onLine);
    window.addEventListener("beforeinstallprompt", install);
    window.addEventListener("online", online);
    window.addEventListener("offline", online);
    return () => {
      window.removeEventListener("beforeinstallprompt", install);
      window.removeEventListener("online", online);
      window.removeEventListener("offline", online);
      recognition.current?.abort();
      audioRef.current?.close();
    };
  }, []);
  useEffect(() => {
    if (!modal) return;
    const previous = document.activeElement;
    const handler = (event) => {
      if (event.key === "Escape") setModal(null);
      if (event.key === "Tab") {
        const items = [
          ...document.querySelectorAll(
            ".modal button, .modal input, .modal textarea, .modal select, .modal a[href]",
          ),
        ].filter((el) => !el.disabled);
        if (!items.length) return;
        if (event.shiftKey && document.activeElement === items[0]) {
          event.preventDefault();
          items.at(-1).focus();
        } else if (!event.shiftKey && document.activeElement === items.at(-1)) {
          event.preventDefault();
          items[0].focus();
        }
      }
    };
    const id = requestAnimationFrame(() =>
      document.querySelector(".modal .close-button")?.focus(),
    );
    document.addEventListener("keydown", handler);
    const oldOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      cancelAnimationFrame(id);
      document.removeEventListener("keydown", handler);
      document.body.style.overflow = oldOverflow;
      previous?.focus();
    };
  }, [modal]);
  function navigate(id) {
    setPage(id);
    setMobileMenu(false);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  function updateSetting(key, value) {
    setSettings((prev) => ({ ...prev, [key]: value }));
  }
  function keyFeedback() {
    if (settings.haptics && navigator.vibrate) navigator.vibrate(8);
    if (settings.sound) {
      try {
        const AudioCtx = window.AudioContext || window.webkitAudioContext;
        if (!AudioCtx) return;
        const ctx = audioRef.current || (audioRef.current = new AudioCtx());
        ctx.resume();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = "sine";
        osc.frequency.value = 550;
        gain.gain.setValueAtTime(0.025, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.04);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + 0.04);
      } catch {
        /* Optional sound. */
      }
    }
  }
  function insert(value, remove = false) {
    keyFeedback();
    const el = textarea.current;
    const current=el?.value??text;
    const start=el?.selectionStart??current.length;
    const end=el?.selectionEnd??current.length;
    const before=remove&&start===end?Math.max(0,start-previousGraphemeLength(current.slice(0,start))):start;
    setDraft(current.slice(0,before)+value+current.slice(end),before+value.length);
    if (shift && value.trim()) setShift(false);
  }
  function suggest(word,sourceWord) {
    const el=textarea.current;const current=el?.value??text;
    const position=el?.selectionStart??textCursor;const end=el?.selectionEnd??position;
    const token=currentSpanishWord(current.slice(0,position));
    if(position!==end||token!==sourceWord)return;
    const start=position-token.length;const separator=current[end]===" "?"":" ";
    setDraft(current.slice(0,start)+word+separator+current.slice(end),start+word.length+1);
  }

  async function copy(value) {
    if (!value) return notify("Primero escribe algo para copiar.");
    try {
      await navigator.clipboard.writeText(value);
      notify("Texto copiado al portapapeles.");
    } catch {
      notify(
        "Tu navegador no permite copiar. Selecciona el texto y usa Copiar.",
      );
    }
  }
  async function downloadPack(ids) {
    if (!ids.length) return notify("Selecciona al menos un tema.");
    setDownloading(true);
    try {
      const { default: JSZip } = await import("jszip");
      const zip = new JSZip();
      const selected = themes.filter((t) => ids.includes(t.id));
      selected.forEach((t) => {
        const folder = zip.folder(t.id);
        folder.file(
          "theme.json",
          JSON.stringify(packageConfig(t, settings), null, 2),
        );
        folder.file("fondo.svg", backgroundSVG(t));
      });
      zip.file(
        "LEEME.txt",
        "VELOCITY — TEMAS ORIGINALES\n\nEste paquete incluye fondos SVG y configuraciones JSON para Velocity Web.\nNo es un APK ni instala un teclado del sistema.\n\nCómo usarlos:\n1. Descomprime el ZIP.\n2. Usa fondo.svg como imagen o ábrelo en un navegador.\n3. Para aplicar el tema aquí, abre la sección Temas y elige Usar tema.\n4. Para restaurar una configuración, importa theme.json en Ajustes.\n\nLicencia: Unlicense (dominio público en la medida permitida por la ley).\nDiseños originales. Sin personajes, imágenes ni logotipos de Sonic o SEGA.\nLas herramientas externas requieren conexión y tienen sus propios términos.\n",
      );
      const blob = await zip.generateAsync({ type: "blob" });
      const name =
        selected.length === 1
          ? `velocity-${selected[0].id}.zip`
          : "velocity-pack.zip";
      await saveFile(blob, name);
      setHistory((prev) =>
        [
          { name, date: new Date().toISOString(), count: selected.length },
          ...prev,
        ].slice(0, 20),
      );
      notify(
        isNativeApp
          ? "Paquete preparado para guardar o compartir."
          : "Paquete preparado. Revisa las descargas de tu navegador.",
      );
    } catch {
      notify("No pudimos preparar el paquete. Inténtalo otra vez.");
    } finally {
      setDownloading(false);
    }
  }
  async function applyNativePersonalization() {
    try {
      await configureNativeKeyboard({ theme: themeId, ...settings });
      await nativeKeyboard.setBackground({
        data: background ? background.split(",")[1] : null,
      });
      notify(
        "Diseño y comodidad aplicados al teclado Android. Vuelve a abrirlo para verlos.",
      );
    } catch {
      notify(
        "No se pudo aplicar la personalización. Revisa que usas la versión 1.0.0.",
      );
    }
  }
  async function installApp() {
    if (isNativeApp) {
      try {
        await nativeKeyboard.openSetup();
      } catch {
        notify("Instala la versión actual para configurar tu teclado.");
      }
      return;
    }
    if (!installPrompt) return setModal("install");
    try {
      await installPrompt.prompt();
      const choice = await installPrompt.userChoice;
      if (choice.outcome === "accepted")
        notify("¡Velocity ya tiene un lugar en tu celular!");
      setInstallPrompt(null);
    } catch {
      setModal("install");
    }
  }
  function startVoice() {
    if (listening) {
      recognition.current?.stop();
      return;
    }
    const SpeechRecognition =
      window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition)
      return notify(
        "Dictado no disponible en este navegador. Prueba Chrome para Android.",
      );
    const rec = new SpeechRecognition();
    recognition.current = rec;
    rec.lang = "es-ES";
    rec.interimResults = false;
    rec.onresult = (event) =>
      setText(
        (prev) =>
          prev +
          (prev && !prev.endsWith(" ") ? " " : "") +
          event.results[0][0].transcript,
      );
    rec.onerror = (event) => {
      setListening(false);
      notify(
        event.error === "not-allowed"
          ? "Permite el micrófono en tu navegador para usar el dictado."
          : "No se pudo reconocer la voz. Revisa tu conexión e inténtalo otra vez.",
      );
    };
    rec.onend = () => setListening(false);
    try {
      rec.start();
      setListening(true);
    } catch {
      setListening(false);
      notify("No pudimos iniciar el micrófono.");
    }
  }
  function applyTheme(id) {
    setThemeId(id);
    notify(`Tema ${themes.find((t) => t.id === id).name} aplicado.`);
  }
  function openTranslator() {
    setTranslationText(text);
    setTranslationAttempted(false);
    setTranslationResult(null);
    setModal("translate");
  }
  function toggleHand() {
    const next = settings.hand === "full" ? "right" : "full";
    updateSetting("hand", next);
    notify(
      next === "full"
        ? "Teclado completo activado."
        : "Modo una mano activado. Puedes cambiar de lado en el teclado.",
    );
  }
  const visibleThemes = themes.filter(
    (t) =>
      (filter === "Todos" ||
        (filter === "Favoritos"
          ? favorites.includes(t.id)
          : t.category === filter)) &&
      `${t.name} ${t.category}`.toLowerCase().includes(query.toLowerCase()),
  );
  const toolItems = [
    {
      icon: Languages,
      name: "Traductor",
      detail: "Que nada se pierda entre idiomas.",
      color: "blue",
      action: openTranslator,
      badge: "ES ⇄ EN",
    },
    {
      icon: Hand,
      name: "Una sola mano",
      detail: "Más cómodo. Igual de rápido.",
      color: "purple",
      action: toggleHand,
      badge: settings.hand !== "full" ? "ACTIVO" : null,
    },
    {
      icon: Clipboard,
      name: "Notas rápidas",
      detail: "Tus frases, siempre a mano.",
      color: "orange",
      action: () => setModal("notes"),
    },
    {
      icon: Smile,
      name: "Emojis",
      detail: "Hay un emoji para cada momento.",
      color: "green",
      action: () => {
        setEmojiOpen(true);
        navigate("home");
        setTimeout(
          () =>
            document
              .getElementById("playground")
              ?.scrollIntoView({ behavior: "smooth", block: "center" }),
          50,
        );
      },
    },
    {
      icon: Mic,
      name: "Dictado por voz",
      detail: "Dilo y deja que las palabras fluyan.",
      color: "pink",
      action: () => {
        navigate("home");
        startVoice();
      },
      badge: micAvailable ? "NAVEGADOR" : "NO DISPONIBLE",
    },
    {
      icon: Download,
      name: "Paquete completo",
      detail: "Todos tus temas en un solo ZIP.",
      color: "blue",
      action: () => downloadPack(themes.map((t) => t.id)),
      badge: "GRATIS",
    },
  ];

  function renderThemeCard({ item, compact = false }) {
    const active = item.id === themeId;
    return (
      <article
        key={item.id}
        className={`theme-card ${compact ? "compact" : ""}`}
      >
        <div
          className={`theme-art theme-${item.id}`}
          style={{
            "--theme-color": item.color,
            "--theme-dark": item.dark,
            "--theme-accent": item.accent,
          }}
        >
          <div className="theme-orbit orbit-one" />
          <div className="theme-orbit orbit-two" />
          <span className="theme-tag">{item.label}</span>
          <button
            className={`favorite ${favorites.includes(item.id) ? "is-favorite" : ""}`}
            aria-label={`${favorites.includes(item.id) ? "Quitar" : "Añadir"} ${item.name} ${favorites.includes(item.id) ? "de" : "a"} favoritos`}
            onClick={() =>
              setFavorites((prev) =>
                prev.includes(item.id)
                  ? prev.filter((id) => id !== item.id)
                  : [...prev, item.id],
              )
            }
          >
            <Heart
              size={15}
              fill={favorites.includes(item.id) ? "currentColor" : "none"}
            />
          </button>
          <div className="mini-keyboard">
            <div className="mini-suggestions">
              <span>hola</span>
              <span>¡Hola!</span>
              <span>gracias</span>
            </div>
            {["qwertyuiop", "asdfghjkl", "zxcvbnm"].map((row, i) => (
              <div className="mini-row" key={i}>
                {[...row].map((k) => (
                  <span key={k}>{k}</span>
                ))}
              </div>
            ))}
            <div className="mini-row bottom">
              <span>123</span>
              <span>☺</span>
              <span className="mini-space">español</span>
              <span>↵</span>
            </div>
          </div>
        </div>
        <div className="theme-info">
          <div>
            <h3>
              {item.name}
              {active && <span className="active-dot" title="Tema activo" />}
            </h3>
            <p>{item.tagline}</p>
          </div>
          <span className="free-label">Gratis</span>
        </div>
        {!compact && (
          <div className="theme-actions">
            <button
              className={`button small ${active ? "applied" : "secondary"}`}
              onClick={() => applyTheme(item.id)}
            >
              {active ? <Check size={15} /> : <Palette size={15} />}{" "}
              {active ? "Aplicado" : "Usar tema"}
            </button>
            <button
              className="icon-button"
              aria-label={`Descargar ${item.name}`}
              disabled={downloading}
              onClick={() => downloadPack([item.id])}
            >
              <Download size={17} />
            </button>
          </div>
        )}
        {compact && (
          <button
            className="theme-overlay-action"
            onClick={() => applyTheme(item.id)}
            aria-label={`Aplicar ${item.name}`}
          >
            {active ? <Check size={14} /> : <ArrowRight size={14} />}
          </button>
        )}
      </article>
    );
  }

  function renderKeyboardPreview() {
    const rows = symbols
      ? symbolPage
        ? [
            ["[", "]", "{", "}", "<", ">", "€", "£", "¥", "¢"],
            ["=", "*", "\\", "|", "~", "^", "°", "•", "©", "®"],
            ["á", "é", "í", "ó", "ú", "ü", "ñ"],
          ]
        : [
            ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"],
            ["@", "#", "$", "%", "&", "-", "+", "(", ")", "/"],
            ["_", '"', "'", ":", ";", "!", "?"],
          ]
      : [
          ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"],
          ["a", "s", "d", "f", "g", "h", "j", "k", "l", "ñ"],
          ["z", "x", "c", "v", "b", "n", "m"],
        ];
    const suggestionSource=currentSpanishWord(text.slice(0,textCursor));
    const suggestions=spanishCompletions(spanishWords,suggestionSource);
    return (
      <section className="playground card" id="playground">
        <div className="section-card-heading">
          <div className="heading-icon">
            <Keyboard size={18} />
            <h2>Pruébalo. Hazlo tuyo.</h2>
          </div>
          <span className="live-pill">
            <span /> En vivo
          </span>
        </div>
        <div className="message-box">
          <textarea
            ref={textarea}
            aria-label="Prueba el teclado"
            inputMode="none"
            placeholder="Escribe algo increíble…"
            value={text}
            onChange={(e) => {pendingSelection.current=null;setText(e.target.value);setTextCursor(e.target.selectionStart);}}
            onSelect={(e)=>{if(e.target.value===text)setTextCursor(e.target.selectionStart);}}
            style={{ fontSize: settings.fontSize === "large" ? 20 : 16 }}
          />
          <div className="message-actions">
            <span>
              {text.length > 0
                ? `${text.length} caracteres`
                : "Las buenas ideas empiezan aquí"}
            </span>
            <div>
              {text && (
                <button
                  className="icon-button tiny"
                  title="Borrar texto"
                  aria-label="Borrar texto"
                  onClick={() => setText("")}
                >
                  <Trash2 size={15} />
                </button>
              )}
              <button
                className="icon-button tiny"
                aria-label="Copiar texto"
                title="Copiar texto"
                onClick={() => copy(text)}
              >
                <Copy size={15} />
              </button>
            </div>
          </div>
        </div>
        <div
          className={`keyboard-stage hand-${settings.hand}`}
          style={{
            "--theme-color": settings.customColors
              ? settings.backgroundColor
              : theme.color,
            "--theme-dark": settings.customColors
              ? settings.backgroundColor
              : theme.dark,
            "--theme-accent": settings.customColors
              ? settings.accentColor
              : theme.accent,
            "--key-height": `${settings.keyHeight}px`,
            "--key-gap": `${settings.keyGap}px`,
            "--key-radius": `${settings.keyRadius}px`,
            "--key-font": `${settings.keyTextSize}px`,
            "--key-color":
              (settings.customColors ? settings.keyColor : "#f4f7ff") +
              Math.round(settings.keyOpacity * 2.55)
                .toString(16)
                .padStart(2, "0"),
            "--key-ink": settings.customColors
              ? readableInk(settings.keyColor)
              : "#173359",
            "--hand-width": `${settings.handWidth}%`,
            "--keyboard-bottom": `${settings.bottomPadding}px`,
            "--keyboard-photo": background ? `url(${background})` : "none",
          }}
        >
          {settings.hand !== "full" && (
            <button
              className="hand-switch"
              aria-label="Cambiar lado del teclado"
              onClick={() =>
                updateSetting(
                  "hand",
                  settings.hand === "right" ? "left" : "right",
                )
              }
            >
              <ArrowLeftRight size={18} />
              <small>
                Cambiar
                <br />
                de lado
              </small>
            </button>
          )}
          <div className={`virtual-keyboard theme-${theme.id}`}>
            <div className="keyboard-topbar">
              <button
                title="Traductor"
                aria-label="Abrir traductor"
                onClick={openTranslator}
              >
                <Languages size={16} />
              </button>
              <div className="suggestions">
                {settings.suggestions && !settings.incognito ? (
                  suggestions.map((s) => (
                    <button key={s} onPointerDown={e=>e.preventDefault()} onClick={() => suggest(s,suggestionSource)}>
                      {s}
                    </button>
                  ))
                ) : (
                  <span className="suggestions-off">Velocity · Español</span>
                )}
              </div>
              <button
                className={listening ? "recording" : ""}
                title="Dictado por voz"
                aria-label={listening ? "Detener dictado" : "Dictar texto"}
                onClick={startVoice}
              >
                <Mic size={16} />
              </button>
            </div>
            {!emojiOpen && !symbols && settings.numberRow && (
              <div className="key-row permanent-numbers">
                {"1234567890".split("").map((key) => (
                  <button className="key" key={key} onClick={() => insert(key)}>
                    {key}
                  </button>
                ))}
              </div>
            )}
            {emojiOpen ? (
              <EmojiPicker
                onInsert={insert}
                remember={settings.rememberEmojis}
                incognito={settings.incognito}
              />
            ) : (
              <div className="key-rows">
                {rows.map((row, i) => (
                  <div className={`key-row row-${i}`} key={i}>
                    {i === 2 && (
                      <button
                        className={`key function-key ${shift && !symbols ? "shift-on" : ""}`}
                        aria-label="Mayúsculas"
                        aria-pressed={shift}
                        onClick={() =>
                          symbols
                            ? setSymbolPage(1 - symbolPage)
                            : setShift(!shift)
                        }
                      >
                        {symbols ? (
                          `${symbolPage + 1}/2`
                        ) : (
                          <ArrowUp size={17} />
                        )}
                      </button>
                    )}
                    {row.map((k) => (
                      <button
                        className="key"
                        key={k}
                        onClick={() =>
                          insert(!symbols && shift ? k.toUpperCase() : k)
                        }
                      >
                        {!symbols && shift ? k.toUpperCase() : k}
                      </button>
                    ))}
                    {i === 2 && (
                      <button
                        className="key function-key"
                        aria-label="Borrar carácter"
                        onClick={() => insert("", true)}
                      >
                        <svg
                          width="19"
                          height="16"
                          viewBox="0 0 22 18"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="1.5"
                        >
                          <path d="M8 2h12v14H8L2 9z" />
                          <path d="m11 6 5 6m0-6-5 6" />
                        </svg>
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
            <div className="key-row keyboard-bottom">
              <button
                className="key function-key symbol-key"
                onClick={() => {
                  setSymbols(!symbols);
                  setEmojiOpen(false);
                }}
              >
                {symbols ? "ABC" : "?123"}
              </button>
              <button
                className="key function-key"
                aria-label={emojiOpen ? "Volver a letras" : "Abrir emojis"}
                onClick={() => setEmojiOpen(!emojiOpen)}
              >
                {emojiOpen ? <Keyboard size={17} /> : <Smile size={17} />}
              </button>
              <button
                className="key function-key punctuation"
                onClick={() => insert(",")}
              >
                ,
              </button>
              <button
                className="key space-key"
                aria-label="Espacio"
                onClick={() => insert(" ")}
              >
                <Globe2 size={12} /> español <span>·</span> Velocity
              </button>
              <button
                className="key function-key punctuation"
                onClick={() => insert(". ")}
              >
                .
              </button>
              <button
                className="key enter-key"
                aria-label="Nueva línea"
                onClick={() => insert("\n")}
              >
                <ArrowRight size={20} />
              </button>
            </div>
          </div>
        </div>
        <div className="keyboard-caption">
          <span>
            <span className="blue-dot" /> {theme.name}
          </span>
          <button onClick={toggleHand}>
            <Hand size={13} />
            {settings.hand === "full" ? "Modo una mano" : "Volver a completo"}
            <ChevronRight size={13} />
          </button>
        </div>
      </section>
    );
  }

  return (
    <div className="app-shell">
      {mobileMenu && (
        <button
          className="sidebar-backdrop"
          aria-label="Cerrar navegación"
          onClick={() => setMobileMenu(false)}
        />
      )}
      <aside className={`sidebar ${mobileMenu ? "mobile-open" : ""}`}>
        <a
          className="brand"
          href="#inicio"
          onClick={(e) => {
            e.preventDefault();
            navigate("home");
          }}
        >
          <span className="brand-symbol">
            <Zap size={23} fill="currentColor" strokeWidth={1.7} />
          </span>
          <span>
            velocity<span className="brand-period">.</span>
            <small>KEYBOARD, REIMAGINED.</small>
          </span>
        </a>
        <div className="nav-label">TU ESPACIO</div>
        <nav aria-label="Navegación principal">
          {navItems.map((item) => (
            <button
              key={item.id}
              onClick={() => navigate(item.id)}
              className={`nav-item ${page === item.id ? "active" : ""}`}
              aria-current={page === item.id ? "page" : undefined}
            >
              <item.icon size={19} />
              {item.label}
              {item.id === "themes" && <span className="nav-count">6</span>}
              {page === item.id && <span className="nav-indicator" />}
            </button>
          ))}
        </nav>
        <div className="sidebar-separator" />
        <button
          className={`nav-item ${page === "settings" ? "active" : ""}`}
          onClick={() => navigate("settings")}
        >
          <Settings2 size={19} />
          Ajustes
        </button>
        <div className="sidebar-bottom">
          <div className="sidebar-promo">
            <span className="promo-icon">
              <Zap size={19} />
            </span>
            <h3>
              Tu teclado.
              <br />
              Tus reglas.
            </h3>
            <p>
              Sin anuncios. Sin límites.
              <br />
              100% a tu ritmo.
            </p>
            <button onClick={installApp}>
              Instalar Velocity <ArrowUp size={15} className="diagonal" />
            </button>
            <div className="promo-orbit" />
          </div>
          <button className="help-link" onClick={() => setModal("help")}>
            <BookOpen size={16} />
            ¿Necesitas una mano?
            <ArrowUp size={13} className="diagonal" />
          </button>
          <div className="sidebar-footer">
            <span className="status-dot" /> Hecho para ser libre.
            <span>v1.0.1</span>
          </div>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <div className="breadcrumbs">
            <button
              className="mobile-toggle icon-button"
              aria-label="Abrir menú"
              onClick={() => setMobileMenu(true)}
            >
              <Menu size={22} />
            </button>
            <span>Tu espacio personal</span>
            <ChevronRight size={13} />
            <strong>
              {page === "settings"
                ? "Ajustes"
                : navItems.find((i) => i.id === page)?.label}
            </strong>
          </div>
          <div className="header-actions">
            <span className="open-source-label">
              <span /> Open source
            </span>
            <a
              className="icon-button"
              href="https://github.com/ivanezequiel545-creator/Sonic-keyboard"
              target="_blank"
              rel="noreferrer"
              aria-label="Ver repositorio en GitHub"
            >
              <Github size={19} />
            </a>
            <span className="header-divider" />
            <button
              className="avatar"
              onClick={() => setModal("welcome")}
              aria-label="Acerca de tu espacio"
            >
              V<span />
            </button>
          </div>
        </header>
        <main>
          {offline && (
            <div className="offline-banner">
              <WifiOff size={16} /> Estás sin conexión. El teclado, los temas y
              las frases de traducción siguen contigo.
            </div>
          )}
          {page === "home" && (
            <>
              <section className="hero">
                <div className="hero-copy">
                  <span className="eyebrow">
                    <span className="tiny-zap">
                      <Zap size={12} fill="currentColor" />
                    </span>{" "}
                    MENOS LÍMITES. MÁS TÚ.
                  </span>
                  <h1>
                    Escribe a<br />
                    tu{" "}
                    <span>
                      ritmo.
                      <svg viewBox="0 0 170 13" aria-hidden="true">
                        <path d="M3 9Q85-4 166 6" />
                      </svg>
                    </span>
                  </h1>
                  <p>
                    Un teclado con personalidad. Rápido, libre
                    <br className="desktop-break" /> y con todo lo que necesitas
                    a un toque.
                  </p>
                  <div className="hero-actions">
                    <button className="button primary" onClick={installApp}>
                      <Download size={17} />
                      Llévalo a tu celular
                      <ArrowRight size={16} />
                    </button>
                    <button
                      className="button text-button"
                      onClick={() => navigate("themes")}
                    >
                      Explorar temas
                      <ChevronRight size={16} />
                    </button>
                  </div>
                  <div className="hero-proof">
                    <span>
                      <ShieldCheck size={14} /> Sin anuncios
                    </span>
                    <i />
                    <span>100% gratis</span>
                    <i />
                    <span>Diseño original</span>
                  </div>
                </div>
                <div
                  className="hero-art"
                  role="img"
                  aria-label="Ilustración original de un rayo azul y órbitas que representan velocidad"
                >
                  <div className="art-grain" />
                  <span className="art-label">
                    <span /> MADE TO MOVE
                  </span>
                  <span className="art-plus plus-one">+</span>
                  <span className="art-plus plus-two">+</span>
                  <div className="speed-lines speed-left">
                    <i />
                    <i />
                    <i />
                  </div>
                  <div className="speed-lines speed-right">
                    <i />
                    <i />
                    <i />
                  </div>
                  <div className="orbital orbital-outer" />
                  <div className="orbital orbital-inner" />
                  <div className="orbital orbital-white" />
                  <div className="art-sphere" />
                  <div className="art-bolt">
                    <Zap fill="currentColor" strokeWidth={0.8} />
                  </div>
                  <span className="art-spark spark-one">✦</span>
                  <span className="art-spark spark-two">✦</span>
                  <div className="floating-key key-v">V</div>
                  <div className="floating-key key-zap">
                    <Zap size={22} fill="currentColor" />
                  </div>
                  <span className="art-bottom">
                    NOT JUST A KEYBOARD.
                    <br />
                    <strong>A LITTLE MORE YOU.</strong>
                  </span>
                  <span className="art-number">01 / ∞</span>
                </div>
              </section>
              <div className="comfort-announcement">
                <span>✦ NUEVO · VELOCITY 1.0.1</span>
                <div>
                  <strong>Un teclado que se siente mejor.</strong>
                  <p>
                    Más espacio para tus dedos. Todos tus emojis. Tu propio
                    estilo.
                  </p>
                </div>
                <button
                  className="button primary"
                  onClick={() => navigate("customize")}
                >
                  Personalizar mi teclado <ArrowRight size={16} />
                </button>
              </div>
              <div className="benefit-strip">
                <span>
                  <Palette size={17} />
                  <strong>Tu estilo, tu tema</strong>
                  <small>Hazlo único</small>
                </span>
                <span>
                  <Languages size={18} />
                  <strong>Conecta sin barreras</strong>
                  <small>Traduce al instante</small>
                </span>
                <span>
                  <Hand size={17} />
                  <strong>A tu medida</strong>
                  <small>Una mano, mil ideas</small>
                </span>
                <span>
                  <ShieldCheck size={18} />
                  <strong>Libre de verdad</strong>
                  <small>Tus palabras son tuyas</small>
                </span>
              </div>
              <AndroidDownloadBanner theme={themeId} settings={settings} />
              <div className="workspace-grid">
                {renderKeyboardPreview()}
                <section className="quick-tools card">
                  <div className="section-card-heading">
                    <div>
                      <h2>Un toque. Más posibilidades.</h2>
                      <p>Pequeñas herramientas, grandes ideas.</p>
                    </div>
                    <Sparkles size={18} className="muted" />
                  </div>
                  <div className="tool-list">
                    {toolItems.slice(0, 4).map((item) => (
                      <button
                        className="quick-tool"
                        key={item.name}
                        onClick={item.action}
                      >
                        <span className={`tool-icon ${item.color}`}>
                          <item.icon size={20} />
                        </span>
                        <span className="quick-tool-text">
                          <strong>
                            {item.name}
                            {item.badge && item.name === "Traductor" && (
                              <small>NUEVO</small>
                            )}
                          </strong>
                          <span>{item.detail}</span>
                        </span>
                        <ChevronRight size={16} />
                      </button>
                    ))}
                  </div>
                  <button
                    className="all-tools"
                    onClick={() => navigate("tools")}
                  >
                    Explorar todas las herramientas
                    <ArrowRight size={15} />
                  </button>
                </section>
              </div>
              <section className="themes-section">
                <div className="section-title">
                  <div>
                    <span className="overline">
                      UN TECLADO, MUCHAS VERSIONES DE TI
                    </span>
                    <h2>
                      Encuentra tu nueva vibra
                      <span className="lime-period">.</span>
                    </h2>
                  </div>
                  <button
                    className="button text-button"
                    onClick={() => navigate("themes")}
                  >
                    Ver todos los temas
                    <ArrowRight size={16} />
                  </button>
                </div>
                <div className="theme-grid compact-grid">
                  {themes
                    .slice(0, 4)
                    .map((t) => renderThemeCard({ item: t, compact: true }))}
                </div>
              </section>
              <section className="download-banner">
                <span className="banner-icon">
                  <Download size={23} />
                </span>
                <div>
                  <h3>Todo lo bueno, en un solo paquete.</h3>
                  <p>
                    Descarga los 6 temas originales y sus configuraciones. Tú
                    eliges cómo usarlos.
                  </p>
                </div>
                <button
                  className="button dark"
                  disabled={downloading}
                  onClick={() => downloadPack(themes.map((t) => t.id))}
                >
                  {downloading ? "Preparando…" : "Descargar pack gratis"}
                  <ArrowDownToLine size={16} />
                </button>
              </section>
            </>
          )}
          {page === "customize" && (
            <>
              <PageHeading
                eyebrow="VELOCITY 1.0.1 · HECHO PARA TUS MANOS"
                title="Cada detalle. A tu manera."
                text="Más comodidad, menos esfuerzo. Diseña tu teclado y pruébalo al instante."
              />
              <div className="personalization-layout">
                <CustomizationPanel
                  settings={settings}
                  onChange={(value) => setSettings(sanitizeSettings(value))}
                  background={background}
                  onBackground={setBackground}
                  native={isNativeApp}
                  onApply={applyNativePersonalization}
                />
                <div className="personalization-preview">
                  {renderKeyboardPreview()}
                  <div className="card comfort-information">
                    <h3>Sin salir de tu ritmo.</h3>
                    <p>
                      Prueba el catálogo de 3.979 entradas de Unicode 17.0 desde
                      ☺. Categorías, búsqueda en español, tonos, recientes y
                      favoritos, sin conexión.
                    </p>
                    <button
                      className="button secondary"
                      onClick={() => setEmojiOpen(true)}
                    >
                      Explorar emojis →
                    </button>
                    <p>
                      La web es un área de prueba. Para escribir en otras
                      aplicaciones de Android, utiliza el teclado nativo.
                    </p>
                  </div>
                </div>
              </div>
            </>
          )}
          {page === "themes" && (
            <>
              <PageHeading
                eyebrow="A TU MANERA"
                title="Un tema para cada versión de ti."
                text="Fondos originales, colores con personalidad. Elige el que va contigo."
              />
              <div className="filter-toolbar">
                <div className="filter-chips">
                  {[
                    "Todos",
                    "Energía",
                    "Oscuro",
                    "Minimalista",
                    "Favoritos",
                  ].map((f) => (
                    <button
                      className={filter === f ? "selected" : ""}
                      key={f}
                      onClick={() => setFilter(f)}
                    >
                      {f === "Favoritos" && <Heart size={13} />} {f}
                    </button>
                  ))}
                </div>
                <label className="search-field">
                  <Search size={17} />
                  <input
                    aria-label="Buscar temas"
                    placeholder="Encuentra tu tema…"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                  />
                </label>
              </div>
              <div className="theme-grid full-grid">
                {visibleThemes.map((t) => renderThemeCard({ item: t }))}
              </div>
              {!visibleThemes.length && (
                <div className="empty-state">
                  <Palette size={36} />
                  <h3>Ningún tema por aquí… todavía.</h3>
                  <p>
                    {filter === "Favoritos"
                      ? "Guarda tus favoritos tocando el corazón de cada tema."
                      : "Prueba con otro nombre o categoría."}
                  </p>
                  <button
                    className="button secondary"
                    onClick={() => {
                      setQuery("");
                      setFilter("Todos");
                    }}
                  >
                    Ver todos los temas
                  </button>
                </div>
              )}
              <div className="subtle-note">
                <ShieldCheck size={17} /> Arte original bajo Unlicense. Sin
                personajes ni marcas de terceros.
              </div>
            </>
          )}
          {page === "tools" && (
            <>
              <PageHeading
                eyebrow="MÁS QUE LETRAS"
                title="Pequeños superpoderes."
                text="Lo que necesitas para escribir mejor, a un toque de distancia."
              />
              <div className="tools-grid">
                {toolItems.map((item) => (
                  <button
                    className="tool-big card"
                    key={item.name}
                    onClick={item.action}
                    disabled={item.name === "Paquete completo" && downloading}
                  >
                    <span className={`tool-icon ${item.color}`}>
                      <item.icon size={25} />
                    </span>
                    {item.badge && (
                      <span className="tool-badge">{item.badge}</span>
                    )}
                    <h3>{item.name}</h3>
                    <p>{item.detail}</p>
                    <span className="tool-open">
                      {item.name === "Una sola mano"
                        ? settings.hand === "full"
                          ? "Activar modo"
                          : "Desactivar modo"
                        : "Abrir herramienta"}
                      <ArrowRight size={17} />
                    </span>
                  </button>
                ))}
              </div>
              <section className="external-section">
                <h2>Elige tu propio camino.</h2>
                <p>
                  Herramientas externas para ir un poco más lejos. Se abren
                  fuera de Velocity y requieren internet.
                </p>
                <div className="external-links">
                  <ExternalCard
                    name="Google Translate"
                    detail="Traducción de texto en más idiomas"
                    href="https://translate.google.com/"
                    icon={Languages}
                  />
                  <ExternalCard
                    name="DeepL"
                    detail="Otra forma de encontrar las palabras"
                    href="https://www.deepl.com/translator"
                    icon={Globe2}
                  />
                  <ExternalCard
                    name="HeliBoard"
                    detail="Teclado nativo libre para Android"
                    href="https://github.com/Helium314/HeliBoard"
                    icon={Keyboard}
                  />
                </div>
              </section>
              <div className="subtle-note">
                <Info size={17} /> Velocity no comparte tu texto
                automáticamente. El dictado puede usar servicios de tu
                navegador; los enlaces externos tienen sus propias políticas.
              </div>
            </>
          )}
          {page === "downloads" && (
            <>
              <PageHeading
                eyebrow="LLÉVATE TU ESTILO"
                title="Lo tuyo, listo para llevar."
                text="Descarga todo de una vez o crea tu propio paquete. Sin cuentas, sin rodeos."
              />
              <AndroidDownloadBanner theme={themeId} settings={settings} />
              <div className="downloads-layout">
                <section className="card pack-picker">
                  <div className="section-card-heading">
                    <div>
                      <h2>Arma tu paquete</h2>
                      <p>Incluye fondos SVG y configuraciones JSON.</p>
                    </div>
                    <button
                      className="text-link"
                      onClick={() =>
                        setDownloadSelection(
                          downloadSelection.length === themes.length
                            ? []
                            : themes.map((t) => t.id),
                        )
                      }
                    >
                      {downloadSelection.length === themes.length
                        ? "Quitar todos"
                        : "Elegir todos"}
                    </button>
                  </div>
                  {themes.map((t) => (
                    <label className="download-option" key={t.id}>
                      <input
                        type="checkbox"
                        checked={downloadSelection.includes(t.id)}
                        onChange={() =>
                          setDownloadSelection((prev) =>
                            prev.includes(t.id)
                              ? prev.filter((id) => id !== t.id)
                              : [...prev, t.id],
                          )
                        }
                      />
                      <span
                        className="download-swatch"
                        style={{
                          background: `linear-gradient(135deg,${t.color},${t.dark})`,
                        }}
                      >
                        <Zap size={20} color={t.accent} />
                      </span>
                      <span>
                        <strong>{t.name}</strong>
                        <small>Fondo + configuración · Unlicense</small>
                      </span>
                      <span className="file-format">SVG + JSON</span>
                    </label>
                  ))}
                  <div className="pack-footer">
                    <span>
                      {downloadSelection.length} de 6 temas seleccionados
                    </span>
                    <button
                      className="button primary"
                      disabled={downloading || !downloadSelection.length}
                      onClick={() => downloadPack(downloadSelection)}
                    >
                      <Download size={17} />
                      {downloading ? "Preparando…" : "Descargar ZIP"}
                    </button>
                  </div>
                </section>
                <aside className="download-info">
                  <div className="card info-card">
                    <span className="tool-icon green">
                      <Info size={22} />
                    </span>
                    <h3>Una descarga, cero misterios.</h3>
                    <p>
                      Este paquete no es una aplicación ni un APK. Incluye
                      diseños y ajustes para tu uso personal o para crear tu
                      propio proyecto.
                    </p>
                    <ol>
                      <li>Descarga y descomprime el ZIP.</li>
                      <li>Abre los fondos SVG en tu navegador.</li>
                      <li>Importa theme.json desde Ajustes.</li>
                    </ol>
                    <button className="text-link" onClick={installApp}>
                      ¿Quieres instalar esta web?
                      <ArrowRight size={14} />
                    </button>
                  </div>
                  <div className="card info-card">
                    <h3>¿Buscas un teclado del sistema?</h3>
                    <p>
                      Para escribir en otras apps de Android instala Velocity
                      Alpha 3 y activa su teclado nativo. Si buscas otra
                      alternativa libre, también puedes explorar HeliBoard.
                    </p>
                    <a
                      className="text-link"
                      href="https://github.com/Helium314/HeliBoard/releases"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Ver HeliBoard
                      <ExternalLink size={14} />
                    </a>
                  </div>
                </aside>
              </div>
              <section className="download-history">
                <div className="section-title">
                  <h2>Tus descargas recientes</h2>
                  {history.length > 0 && (
                    <button
                      className="text-link"
                      onClick={() => {
                        setHistory([]);
                        notify(
                          "Historial borrado. Los archivos descargados no se han eliminado.",
                        );
                      }}
                    >
                      Limpiar historial
                    </button>
                  )}
                </div>
                {history.length ? (
                  history.map((h, i) => (
                    <div className="history-row" key={`${h.date}-${i}`}>
                      <span className="tool-icon blue">
                        <Download size={18} />
                      </span>
                      <div>
                        <strong>{h.name}</strong>
                        <small>
                          {h.count} {h.count === 1 ? "tema" : "temas"} ·{" "}
                          {new Date(h.date).toLocaleDateString("es")}
                        </small>
                      </div>
                      <span className="downloaded-label">
                        <CheckCheck size={15} />
                        Preparado
                      </span>
                    </div>
                  ))
                ) : (
                  <div className="empty-state compact-empty">
                    <ArrowDownToLine size={28} />
                    <p>Tu próxima descarga aparecerá aquí.</p>
                  </div>
                )}
              </section>
            </>
          )}
          {page === "settings" && (
            <>
              <PageHeading
                eyebrow="LOS PEQUEÑOS DETALLES"
                title="Haz que se sienta tuyo."
                text="Personaliza tu experiencia. Los ajustes se guardan solo en este dispositivo."
              />
              <div className="settings-layout">
                <section className="card settings-card">
                  <h2>Tu teclado</h2>
                  <SettingRow
                    title="Modo de una mano"
                    text="Un teclado más compacto, en el lado que prefieras."
                  >
                    <select
                      aria-label="Modo de una mano"
                      value={settings.hand}
                      onChange={(e) => updateSetting("hand", e.target.value)}
                    >
                      <option value="full">Completo</option>
                      <option value="left">Izquierda</option>
                      <option value="right">Derecha</option>
                    </select>
                  </SettingRow>
                  <SettingRow
                    title="Sugerencias rápidas"
                    text="Completa palabras del diccionario español local, sin enviar tu texto."
                  >
                    <Toggle
                      label="Sugerencias rápidas"
                      checked={settings.suggestions}
                      onChange={() =>
                        updateSetting("suggestions", !settings.suggestions)
                      }
                    />
                  </SettingRow>
                  <SettingRow
                    title="Sonido al escribir"
                    text="Un pequeño clic para cada idea."
                  >
                    <Toggle
                      label="Sonido al escribir"
                      checked={settings.sound}
                      onChange={() => updateSetting("sound", !settings.sound)}
                    />
                  </SettingRow>
                  <SettingRow
                    title="Respuesta háptica"
                    text="Vibración suave en navegadores compatibles."
                  >
                    <Toggle
                      label="Respuesta háptica"
                      checked={settings.haptics}
                      onChange={() =>
                        updateSetting("haptics", !settings.haptics)
                      }
                    />
                  </SettingRow>
                  <SettingRow
                    title="Tamaño del texto"
                    text="Dale más espacio a tus palabras."
                  >
                    <select
                      aria-label="Tamaño del texto"
                      value={settings.fontSize}
                      onChange={(e) =>
                        updateSetting("fontSize", e.target.value)
                      }
                    >
                      <option value="normal">Normal</option>
                      <option value="large">Grande</option>
                    </select>
                  </SettingRow>
                  <button
                    className="button secondary"
                    onClick={() => {
                      navigate("home");
                      setTimeout(
                        () =>
                          document
                            .getElementById("playground")
                            ?.scrollIntoView({ behavior: "smooth" }),
                        80,
                      );
                    }}
                  >
                    Probar mis ajustes
                    <ArrowRight size={16} />
                  </button>
                </section>
                <section className="card settings-card">
                  <h2>Tus datos, bajo tu control.</h2>
                  <p className="settings-intro">
                    Sin cuentas ni analítica. Tus notas, temas y ajustes se
                    guardan en este navegador. El texto del área de prueba no se
                    guarda.
                  </p>
                  <div className="settings-data-action">
                    <div>
                      <strong>Importar configuración</strong>
                      <p>Restaura un archivo theme.json de Velocity.</p>
                    </div>
                    <label className="button secondary small import-button">
                      Importar
                      <input
                        type="file"
                        accept=".json,application/json"
                        onChange={async (e) => {
                          const file = e.target.files?.[0];
                          if (!file) return;
                          try {
                            if (file.size > 100000) throw new Error();
                            const config = JSON.parse(await file.text());
                            if (
                              config.format !== "velocity-theme" ||
                              !themes.some((t) => t.id === config.theme)
                            )
                              throw new Error();
                            setThemeId(config.theme);
                            setSettings(sanitizeSettings(config.settings));
                            notify("Tema y configuración importados.");
                          } catch {
                            notify(
                              "Elige un archivo theme.json válido de Velocity.",
                            );
                          }
                          e.target.value = "";
                        }}
                      />
                    </label>
                  </div>
                  <div className="settings-data-action">
                    <div>
                      <strong>Exportar mi tema</strong>
                      <p>Guarda tu configuración actual en JSON.</p>
                    </div>
                    <button
                      className="button secondary small"
                      onClick={async () => {
                        try {
                          await saveFile(
                            new Blob(
                              [
                                JSON.stringify(
                                  packageConfig(theme, settings),
                                  null,
                                  2,
                                ),
                              ],
                              { type: "application/json" },
                            ),
                            "theme.json",
                          );
                          notify("Configuración exportada.");
                        } catch {
                          notify(
                            "No se pudo guardar o compartir la configuración.",
                          );
                        }
                      }}
                    >
                      Exportar
                    </button>
                  </div>
                  <div className="settings-data-action">
                    <div>
                      <strong>Restablecer datos locales</strong>
                      <p>Elimina ajustes, favoritos, notas e historial.</p>
                    </div>
                    <button
                      className="button danger small"
                      onClick={() => setModal("reset")}
                    >
                      Restablecer
                    </button>
                  </div>
                </section>
              </div>
            </>
          )}
          <footer className="main-footer">
            <span>
              <Zap size={13} /> Diseñado para moverse contigo.
            </span>
            <div>
              <button onClick={() => setModal("privacy")}>Privacidad</button>
              <span>·</span>
              <button onClick={() => setModal("license")}>
                Libre y original
              </button>
              <span className="footer-version">
                Velocity © {new Date().getFullYear()}
              </span>
            </div>
          </footer>
        </main>
      </div>
      {toast && (
        <div className="toast" role="status">
          <Check size={17} />
          {toast}
          <button aria-label="Cerrar aviso" onClick={() => setToast("")}>
            <X size={15} />
          </button>
        </div>
      )}
      {modal && (
        <div
          className="modal-backdrop"
          onClick={(e) => {
            if (e.target === e.currentTarget) setModal(null);
          }}
        >
          <section
            className={`modal modal-${modal}`}
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-title"
          >
            <button
              className="close-button icon-button"
              onClick={() => setModal(null)}
              aria-label="Cerrar ventana"
            >
              <X size={21} />
            </button>
            {modal === "translate" && (
              <>
                <span className="tool-icon blue">
                  <Languages size={24} />
                </span>
                <h2 id="modal-title">Las palabras viajan.</h2>
                <p className="modal-subtitle">
                  Traduce frases básicas sin salir de tu espacio.
                </p>
                <div className="translation-direction">
                  <span>{direction === "es-en" ? "Español" : "Inglés"}</span>
                  <button
                    className="icon-button"
                    aria-label="Intercambiar idiomas"
                    onClick={() => {
                      setDirection((prev) =>
                        prev === "es-en" ? "en-es" : "es-en",
                      );
                      if (translationResult)
                        setTranslationText(translationResult);
                      setTranslationResult(null);
                      setTranslationAttempted(false);
                    }}
                  >
                    <ArrowLeftRight size={19} />
                  </button>
                  <span>{direction === "es-en" ? "Inglés" : "Español"}</span>
                </div>
                <textarea
                  className="translation-input"
                  aria-label="Texto para traducir"
                  value={translationText}
                  onChange={(e) => {
                    setTranslationText(e.target.value);
                    setTranslationAttempted(false);
                    setTranslationResult(null);
                  }}
                  placeholder={
                    direction === "es-en"
                      ? "Prueba «hola» o «buenos días»…"
                      : "Try “hello” or “good morning”…"
                  }
                />
                <button
                  className="button primary full-width"
                  disabled={!translationText.trim()}
                  onClick={() => {
                    setTranslationResult(
                      translateOffline(translationText, direction),
                    );
                    setTranslationAttempted(true);
                  }}
                >
                  Traducir frase
                  <Languages size={16} />
                </button>
                {translationAttempted && (
                  <div
                    className={`translation-result ${!translationResult ? "no-match" : ""}`}
                  >
                    {translationResult ? (
                      <>
                        <strong>{translationResult}</strong>
                        <div>
                          <button
                            className="icon-button"
                            aria-label="Copiar traducción"
                            onClick={() => copy(translationResult)}
                          >
                            <Copy size={17} />
                          </button>
                          <button
                            className="text-link"
                            onClick={() => {
                              setText(
                                (prev) =>
                                  prev + (prev ? " " : "") + translationResult,
                              );
                              setModal(null);
                              navigate("home");
                            }}
                          >
                            Usar texto
                            <ArrowRight size={15} />
                          </button>
                        </div>
                      </>
                    ) : (
                      <>
                        <Info size={19} />
                        <p>
                          Esta frase no está en el diccionario local. Prueba con
                          «gracias», «cómo estás» o usa un traductor externo
                          para textos libres.
                        </p>
                      </>
                    )}
                  </div>
                )}
                <div className="translator-info">
                  <WifiOff size={14} /> Diccionario básico de 30 frases · ES /
                  EN · Sin conexión
                </div>
                <div className="external-translate">
                  <span>Para cualquier texto y más idiomas:</span>
                  <a
                    href={`https://translate.google.com/?sl=${direction === "es-en" ? "es" : "en"}&tl=${direction === "es-en" ? "en" : "es"}&text=${encodeURIComponent(translationText)}&op=translate`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    Google Translate
                    <ExternalLink size={13} />
                  </a>
                  <a
                    href={`https://www.deepl.com/translator#${direction === "es-en" ? "es/en" : "en/es"}/${encodeURIComponent(translationText)}`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    DeepL
                    <ExternalLink size={13} />
                  </a>
                  <small>
                    Al abrir un enlace, el texto se envía al servicio elegido.
                    Requiere internet.
                  </small>
                </div>
              </>
            )}
            {modal === "notes" && (
              <>
                <span className="tool-icon orange">
                  <Clipboard size={24} />
                </span>
                <h2 id="modal-title">Siempre a mano.</h2>
                <p className="modal-subtitle">
                  Guarda frases que usas mucho. Solo en este dispositivo.
                </p>
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    if (!noteText.trim()) return;
                    if (notes.length >= 50)
                      return notify(
                        "Puedes guardar hasta 50 notas. Borra alguna para añadir otra.",
                      );
                    setNotes((prev) => [
                      { id: crypto.randomUUID(), text: noteText.trim() },
                      ...prev,
                    ]);
                    setNoteText("");
                  }}
                >
                  <textarea
                    className="translation-input"
                    aria-label="Nueva nota"
                    maxLength={1000}
                    placeholder="Una idea, una firma, ese mensaje de siempre…"
                    value={noteText}
                    onChange={(e) => setNoteText(e.target.value)}
                  />
                  <button
                    className="button primary full-width"
                    disabled={!noteText.trim()}
                    type="submit"
                  >
                    Guardar nota
                    <Clipboard size={16} />
                  </button>
                </form>
                <div className="notes-list">
                  {notes.length ? (
                    notes.map((note) => (
                      <div className="note" key={note.id}>
                        <p>{note.text}</p>
                        <div>
                          <button
                            className="text-link"
                            onClick={() => {
                              setText(
                                (prev) => prev + (prev ? " " : "") + note.text,
                              );
                              setModal(null);
                              navigate("home");
                            }}
                          >
                            Usar
                            <ArrowRight size={13} />
                          </button>
                          <button
                            className="icon-button tiny"
                            aria-label="Copiar nota"
                            onClick={() => copy(note.text)}
                          >
                            <Copy size={15} />
                          </button>
                          <button
                            className="icon-button tiny"
                            aria-label="Eliminar nota"
                            onClick={() =>
                              setNotes((prev) =>
                                prev.filter((n) => n.id !== note.id),
                              )
                            }
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="empty-notes">
                      <Clipboard size={27} />
                      <p>Tu primera nota empieza aquí.</p>
                    </div>
                  )}
                </div>
                <small className="privacy-note">
                  <ShieldCheck size={13} />
                  No guardes contraseñas o información sensible.
                </small>
              </>
            )}
            {modal === "install" && (
              <>
                <span className="tool-icon blue">
                  <Download size={24} />
                </span>
                <h2 id="modal-title">Un lugar en tu celular.</h2>
                <AndroidDownloadBanner theme={themeId} settings={settings} />
                <p className="modal-subtitle">
                  Instala Velocity como una app web, sin pasar por una tienda.
                </p>
                <div className="install-step">
                  <span>1</span>
                  <div>
                    <strong>En Android · Chrome</strong>
                    <p>
                      Abre el menú ⋮ del navegador y toca «Instalar aplicación»
                      o «Añadir a pantalla de inicio».
                    </p>
                  </div>
                </div>
                <div className="install-step">
                  <span>2</span>
                  <div>
                    <strong>En iPhone · Safari</strong>
                    <p>
                      Toca Compartir y elige «Añadir a pantalla de inicio».
                      Después toca «Añadir».
                    </p>
                  </div>
                </div>
                <div className="notice">
                  <Info size={19} />
                  <p>
                    La versión web{" "}
                    <strong>no reemplaza el teclado del sistema</strong> en
                    WhatsApp ni en otras apps. Para Android, instala el APK
                    Alpha 3 y activa su teclado nativo desde Ajustes. El modo
                    sin conexión de la web requiere una primera visita completa.
                  </p>
                </div>
                <button
                  className="button primary full-width"
                  onClick={() => {
                    setModal(null);
                    navigate("downloads");
                  }}
                >
                  Prefiero descargar los temas
                  <ArrowRight size={16} />
                </button>
              </>
            )}
            {modal === "help" && (
              <>
                <span className="tool-icon green">
                  <BookOpen size={24} />
                </span>
                <h2 id="modal-title">Vamos paso a paso.</h2>
                <p className="modal-subtitle">
                  Todo lo que necesitas saber para empezar.
                </p>
                <div className="faq">
                  <details open>
                    <summary>¿Cómo uso el teclado?</summary>
                    <p>
                      Escribe con las teclas de la vista previa o con tu teclado
                      habitual. Puedes copiar el resultado para usarlo en otra
                      app.
                    </p>
                  </details>
                  <details>
                    <summary>¿Funciona en WhatsApp o Telegram?</summary>
                    <p>
                      La web no escribe directamente en otras apps. En Android,
                      el APK Alpha 3 sí incluye un servicio de teclado: actívalo
                      y selecciónalo desde «Activar mi teclado». Funcionará en
                      campos de texto compatibles de otras apps, salvo
                      restricciones de estas o del sistema. No es un teclado
                      para iPhone.
                    </p>
                  </details>
                  <details>
                    <summary>¿Qué puedo descargar?</summary>
                    <p>
                      Fondos SVG originales y configuraciones JSON, juntos en un
                      ZIP o por tema. Los JSON se pueden restaurar desde
                      Ajustes.
                    </p>
                  </details>
                  <details>
                    <summary>¿Cómo funciona el traductor?</summary>
                    <p>
                      Incluye un diccionario pequeño de frases españolas e
                      inglesas para usar sin internet. Para traducción libre,
                      abre Google Translate o DeepL desde la herramienta.
                    </p>
                  </details>
                  <details>
                    <summary>¿Tiene imágenes de Sonic?</summary>
                    <p>
                      No. El arte abstracto de rayos y órbitas es original.
                      Velocity no está afiliado a Sonic ni a SEGA.
                    </p>
                  </details>
                </div>
              </>
            )}
            {modal === "privacy" && (
              <>
                <span className="tool-icon green">
                  <ShieldCheck size={24} />
                </span>
                <h2 id="modal-title">Tus palabras son tuyas.</h2>
                <div className="prose">
                  <p>
                    Velocity no incluye cuentas, publicidad, rastreadores ni un
                    servidor que almacene tus textos.
                  </p>
                  <p>
                    <strong>En tu dispositivo:</strong> los temas, ajustes,
                    favoritos, notas e historial de paquetes se conservan en
                    localStorage. Puedes eliminarlos desde Ajustes. El área de
                    escritura y las traducciones se pierden al recargar.
                  </p>
                  <p>
                    <strong>Servicios externos:</strong> al abrir un traductor
                    se comparte el texto incluido en el enlace con ese servicio.
                    El dictado depende del navegador y puede enviar audio a su
                    proveedor tras solicitar permiso. La función de copiar
                    utiliza el portapapeles del dispositivo.
                  </p>
                  <p>
                    El alojamiento recibe las solicitudes normales de la web. No
                    guardes datos sensibles en las notas: no están cifradas.
                  </p>
                </div>
              </>
            )}
            {modal === "license" && (
              <>
                <span className="tool-icon purple">
                  <Sparkles size={24} />
                </span>
                <h2 id="modal-title">Libre. Original. Tuyo.</h2>
                <div className="prose">
                  <p>
                    Los fondos y el código propios de Velocity se distribuyen
                    bajo la licencia <strong>Unlicense</strong>: dedicación al
                    dominio público en la medida permitida por la legislación
                    aplicable.
                  </p>
                  <p>
                    El arte usa rayos, colores y órbitas abstractas. No utiliza
                    personajes, logotipos, música ni ilustraciones de Sonic.
                    Este proyecto no está afiliado ni respaldado por SEGA.
                  </p>
                  <p>
                    Las dependencias, como React, Lucide y JSZip, conservan sus
                    respectivas licencias. Las herramientas externas tienen sus
                    propios términos.
                  </p>
                  <a
                    className="button secondary"
                    href="https://github.com/ivanezequiel545-creator/Sonic-keyboard"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Explorar el código
                    <Github size={17} />
                  </a>
                </div>
              </>
            )}
            {modal === "welcome" && (
              <>
                <span className="brand-symbol">
                  <Zap size={23} fill="currentColor" />
                </span>
                <h2 id="modal-title">Este espacio es tuyo.</h2>
                <p className="modal-subtitle">
                  Sin registro, sin contraseñas. Solo tú y tus ideas.
                </p>
                <div className="welcome-stats">
                  <div>
                    <strong>{themes.length}</strong>
                    <span>temas originales</span>
                  </div>
                  <div>
                    <strong>{favorites.length}</strong>
                    <span>favoritos</span>
                  </div>
                  <div>
                    <strong>{notes.length}</strong>
                    <span>notas guardadas</span>
                  </div>
                </div>
                <div className="notice">
                  <ShieldCheck size={20} />
                  <p>
                    Tus preferencias se guardan en este navegador, no en una
                    cuenta. Puedes exportar tu configuración desde Ajustes.
                  </p>
                </div>
                <button
                  className="button primary full-width"
                  onClick={() => {
                    setModal(null);
                    navigate("settings");
                  }}
                >
                  Personalizar mi espacio
                  <Settings2 size={16} />
                </button>
              </>
            )}
            {modal === "reset" && (
              <>
                <span className="tool-icon pink">
                  <Trash2 size={24} />
                </span>
                <h2 id="modal-title">¿Empezamos de nuevo?</h2>
                <p className="modal-subtitle">
                  Se borrarán tus notas, favoritos, ajustes e historial local.
                  Los archivos que ya descargaste no se eliminarán. Esta acción
                  no se puede deshacer.
                </p>
                <div className="modal-actions">
                  <button
                    className="button secondary"
                    onClick={() => setModal(null)}
                  >
                    Mejor no
                  </button>
                  <button
                    className="button danger"
                    onClick={() => {
                      setSettings({ ...DEFAULT_SETTINGS });
                      setBackground("");
                      try {
                        localStorage.removeItem("velocity-emoji-recent");
                        localStorage.removeItem("velocity-emoji-favorites");
                      } catch {}
                      setThemeId("velocity");
                      setFavorites([]);
                      setHistory([]);
                      setNotes([]);
                      setText("");
                      setModal(null);
                      notify("Todo listo para empezar de nuevo.");
                    }}
                  >
                    Sí, restablecer
                  </button>
                </div>
              </>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
function AndroidDownloadBanner({ theme, settings }) {
  const [nativeStatus, setNativeStatus] = useState(null);
  const [message, setMessage] = useState("");
  useEffect(() => {
    let alive = true;
    if (isNativeApp)
      nativeKeyboard
        .getStatus()
        .then((status) => {
          if (alive) setNativeStatus(status);
        })
        .catch(() => {
          if (alive)
            setMessage(
              "Este APK antiguo no incluye el módulo nativo. Instala Alpha 3 desde GitHub.",
            );
        });
    return () => {
      alive = false;
    };
  }, []);
  if (isNativeApp)
    return (
      <section
        className="android-banner card"
        aria-label="Teclado Android nativo"
      >
        <div className="android-banner-copy">
          <span className="overline">
            VELOCITY ANDROID · {nativeStatus?.version || "COMPROBANDO VERSIÓN"}
          </span>
          <h3>
            {nativeStatus?.imeAvailable
              ? "Actívalo. Escribe en tus apps."
              : "Tu próximo paso: el teclado nativo."}
          </h3>
          <p>
            {nativeStatus?.imeAvailable
              ? "Activa Velocity en Ajustes de Android y selecciónalo como método de entrada. No se activa sin tu permiso."
              : nativeStatus?.stage === 2
                ? "Alpha 2 incluye la vista previa nativa. Para escribir fuera de Velocity, instala Alpha 3."
                : "Esta versión contiene la web. Instala Alpha 3 para disponer del teclado del sistema."}
          </p>
        </div>
        <div className="android-banner-actions">
          {nativeStatus?.stage >= 2 && (
            <>
              <button
                className="button primary"
                onClick={async () => {
                  try {
                    await nativeKeyboard.openSetup();
                  } catch {
                    setMessage(
                      "No se pudo abrir la configuración del teclado.",
                    );
                  }
                }}
              >
                <Keyboard size={16} />
                {nativeStatus.imeAvailable
                  ? "Activar mi teclado"
                  : "Probar teclado nativo"}
              </button>
              <button
                className="text-link"
                onClick={async () => {
                  try {
                    await configureNativeKeyboard({ theme, ...settings });
                    setMessage(
                      "Tema y ajustes copiados al teclado nativo. Vuelve a abrirlo para verlos.",
                    );
                  } catch {
                    setMessage("No se pudieron copiar los ajustes.");
                  }
                }}
              >
                Aplicar estos ajustes al teclado
              </button>
            </>
          )}
          {!nativeStatus?.imeAvailable && (
            <a
              className="text-link"
              href={androidReleaseUrl}
              target="_blank"
              rel="noreferrer"
            >
              Descargar Alpha 3<ExternalLink size={13} />
            </a>
          )}
        </div>
        {message && (
          <p role="status" className="native-status-message">
            {message}
          </p>
        )}
      </section>
    );
  return (
    <section className="android-banner card" aria-label="Aplicación Android">
      <div className="android-banner-heading">
        <span className="tool-icon blue">
          <Smartphone size={25} />
        </span>
        <div className="android-banner-copy">
          <span className="overline">TRES PRELANZAMIENTOS · ANDROID 6+</span>
          <h3>De tu espacio a tus otras apps.</h3>
          <p>
            Elige una etapa. Alpha 3 incluye un teclado del sistema real; la web
            y Alpha 1 no sustituyen el teclado de Android.
          </p>
        </div>
      </div>
      <div className="android-preview-grid">
        {androidPreviews.map((preview) => (
          <a
            key={preview.stage}
            className={`android-preview-card ${preview.stage === 3 ? "recommended" : ""}`}
            href={preview.url}
            target="_blank"
            rel="noreferrer"
          >
            <span>
              ALPHA {preview.stage}
              {preview.stage === 3 ? " · IME" : ""}
            </span>
            <strong>{preview.title}</strong>
            <p>{preview.description}</p>
            <small>
              Ver APK e instrucciones <ExternalLink size={12} />
            </small>
          </a>
        ))}
      </div>
      <div className="android-banner-bottom">
        <small>
          Repositorio privado: inicia sesión en GitHub con acceso. Descarga el
          APK desde Assets. Versiones de prueba, no Play Store.
        </small>
        <a
          className="button primary"
          href={androidApkUrl}
          target="_blank"
          rel="noreferrer"
        >
          <Download size={16} />
          Descargar APK · Alpha 3
        </a>
      </div>
    </section>
  );
}

function PageHeading({ eyebrow, title, text }) {
  return (
    <div className="page-heading">
      <span className="overline">{eyebrow}</span>
      <h1>{title}</h1>
      <p>{text}</p>
    </div>
  );
}
function ExternalCard({ name, detail, href, icon: Icon }) {
  return (
    <a
      className="external-card card"
      href={href}
      target="_blank"
      rel="noreferrer"
    >
      <Icon size={21} />
      <div>
        <strong>{name}</strong>
        <p>{detail}</p>
      </div>
      <ExternalLink size={16} />
    </a>
  );
}
function Toggle({ label, checked, onChange }) {
  return (
    <button
      className={`toggle ${checked ? "on" : ""}`}
      role="switch"
      aria-label={label}
      aria-checked={checked}
      onClick={onChange}
    >
      <span />
    </button>
  );
}
function SettingRow({ title, text, children }) {
  return (
    <div className="setting-row">
      <div>
        <strong>{title}</strong>
        <p>{text}</p>
      </div>
      {children}
    </div>
  );
}

createRoot(document.getElementById("root")).render(<App />);
if (import.meta.env.PROD && !isNativeApp && "serviceWorker" in navigator)
  window.addEventListener("load", () => {
    navigator.serviceWorker
      .register("/sw.js")
      .catch((error) =>
        console.warn("No se pudo activar el modo sin conexión.", error),
      );
  });
