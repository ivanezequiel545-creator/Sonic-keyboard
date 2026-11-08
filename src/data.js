export const themes = [
  {
    id: "velocity",
    name: "Blue Rush",
    tagline: "Tu dosis de velocidad",
    category: "Energía",
    color: "#1755e7",
    dark: "#0a298b",
    accent: "#d7fb83",
    label: "EL FAVORITO",
    free: true,
  },
  {
    id: "midnight",
    name: "Midnight",
    tagline: "Ideas que no duermen",
    category: "Oscuro",
    color: "#33324e",
    dark: "#161625",
    accent: "#c1b6fc",
    label: "MODO OSCURO",
    free: true,
  },
  {
    id: "mint",
    name: "Mint Flow",
    tagline: "Un respiro entre palabras",
    category: "Minimalista",
    color: "#b6d8c7",
    dark: "#709b88",
    accent: "#f4ffe6",
    label: "FRESH",
    free: true,
  },
  {
    id: "sunset",
    name: "Peach Club",
    tagline: "Buenas vibras, siempre",
    category: "Energía",
    color: "#f6b899",
    dark: "#da826c",
    accent: "#fff0bd",
    label: "NUEVO",
    free: true,
  },
  {
    id: "lilac",
    name: "Lilac Dream",
    tagline: "Un poco de imaginación",
    category: "Minimalista",
    color: "#b7a1da",
    dark: "#7d60b0",
    accent: "#efe8ff",
    label: "SUAVE",
    free: true,
  },
  {
    id: "graphite",
    name: "Graphite",
    tagline: "Menos ruido, más ideas",
    category: "Oscuro",
    color: "#52606c",
    dark: "#25303a",
    accent: "#e1edf4",
    label: "ESENCIAL",
    free: true,
  },
];

const dictionary = {
  hola: "hello",
  adiós: "goodbye",
  gracias: "thank you",
  "buenos días": "good morning",
  "buenas noches": "good night",
  "buenas tardes": "good afternoon",
  "cómo estás": "how are you",
  "estoy bien": "I am fine",
  "por favor": "please",
  "hasta luego": "see you later",
  "te quiero": "I love you",
  "me gusta": "I like it",
  "buen viaje": "have a good trip",
  "feliz cumpleaños": "happy birthday",
  "lo siento": "I am sorry",
  "de nada": "you are welcome",
  sí: "yes",
  no: "no",
  ayuda: "help",
  bienvenido: "welcome",
  mañana: "tomorrow",
  hoy: "today",
  agua: "water",
  "muchas gracias": "thank you very much",
  "qué hora es": "what time is it",
  "hasta mañana": "see you tomorrow",
  "no entiendo": "I do not understand",
  "dónde estás": "where are you",
  "nos vemos pronto": "see you soon",
  "escribe a tu ritmo": "write at your own pace",
};
export function translateOffline(text, direction = "es-en") {
  const normalize = (value) =>
    value
      .trim()
      .toLowerCase()
      .replace(/[¿?¡!.]+/g, "")
      .replace(/\s+/g, " ");
  const entries = Object.entries(dictionary);
  const match = entries.find(
    ([es, en]) =>
      normalize(direction === "es-en" ? es : en) === normalize(text),
  );
  return match ? (direction === "es-en" ? match[1] : match[0]) : null;
}
export function backgroundSVG(theme) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="1920" viewBox="0 0 1080 1920"><defs><linearGradient id="bg" x2="1" y2="1"><stop stop-color="${theme.color}"/><stop offset="1" stop-color="${theme.dark}"/></linearGradient></defs><rect width="1080" height="1920" fill="url(#bg)"/><g fill="none" stroke="${theme.accent}" opacity=".25" transform="translate(540 850) rotate(-35)"><ellipse rx="430" ry="180" stroke-width="8"/><ellipse rx="510" ry="240" stroke-width="3"/><ellipse rx="620" ry="310" stroke-width="2"/></g><path d="m620 560-300 440h235l-80 360 330-510H580z" fill="${theme.accent}"/><g stroke="white" opacity=".25" stroke-linecap="round" stroke-width="8"><path d="M90 700h190M50 760h160M790 1190h220M850 1250h180"/></g></svg>`;
}
export function packageConfig(theme, settings = {}) {
  return {
    format: "velocity-theme",
    version: 1,
    theme: theme.id,
    name: theme.name,
    colors: {
      primary: theme.color,
      background: theme.dark,
      accent: theme.accent,
    },
    layout: "es-qwerty",
    settings,
    license: "Unlicense",
    note: "Configuración de Velocity Web. No instala un teclado del sistema.",
  };
}
