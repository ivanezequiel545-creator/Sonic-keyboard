export const DEFAULT_SETTINGS = Object.freeze({
  hand: "full",
  sound: false,
  haptics: true,
  suggestions: true,
  fontSize: "normal",
  keyHeight: 56,
  keyGap: 3,
  keyRadius: 9,
  keyTextSize: 18,
  bottomPadding: 10,
  handWidth: 82,
  numberRow: true,
  keyOpacity: 96,
  customColors: false,
  backgroundColor: "#163db8",
  keyColor: "#f4f7ff",
  accentColor: "#d7fb83",
  rememberEmojis: true,
  incognito: false,
  doubleSpacePeriod: false,
});
export const PRESETS = {
  comfortable: {
    keyOpacity: 96,
    keyHeight: 56,
    keyGap: 3,
    keyRadius: 9,
    keyTextSize: 18,
    bottomPadding: 10,
    handWidth: 82,
    numberRow: true,
  },
  compact: {
    keyOpacity: 96,
    keyHeight: 44,
    keyGap: 2,
    keyRadius: 5,
    keyTextSize: 16,
    bottomPadding: 4,
    handWidth: 88,
    numberRow: false,
  },
  accessible: {
    keyOpacity: 100,
    keyHeight: 64,
    keyGap: 4,
    keyRadius: 10,
    keyTextSize: 22,
    bottomPadding: 16,
    handWidth: 90,
    numberRow: true,
  },
};
export const RANGES = {
  keyHeight: [40, 68],
  keyGap: [1, 8],
  keyRadius: [0, 20],
  keyTextSize: [14, 24],
  bottomPadding: [0, 36],
  handWidth: [65, 90],
  keyOpacity: [65, 100],
};
export function sanitizeSettings(input = {}) {
  const result = { ...DEFAULT_SETTINGS };
  if (!input || typeof input !== "object") return result;
  for (const [key, value] of Object.entries(input)) {
    if (Object.hasOwn(RANGES, key) && Number.isFinite(value))
      result[key] = Math.round(
        Math.min(RANGES[key][1], Math.max(RANGES[key][0], value)),
      );
    else if (
      typeof DEFAULT_SETTINGS[key] === "boolean" &&
      typeof value === "boolean"
    )
      result[key] = value;
    else if (
      ["backgroundColor", "keyColor", "accentColor"].includes(key) &&
      typeof value === "string" &&
      /^#[a-f0-9]{6}$/i.test(value)
    )
      result[key] = value;
    else if (key === "hand" && ["full", "left", "right"].includes(value))
      result[key] = value;
    else if (key === "fontSize" && ["normal", "large"].includes(value))
      result[key] = value;
  }
  return result;
}
export function normalizeSearch(text) {
  return String(text)
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}
export function filterEmojis(
  entries,
  {
    query = "",
    category = "all",
    tone = "all",
    recent = [],
    favorites = [],
  } = {},
) {
  const words = normalizeSearch(query).split(/\s+/).filter(Boolean);
  const ids =
    category === "recent"
      ? recent
      : category === "favorites"
        ? favorites
        : null;
  let result = entries.filter(
    (e) =>
      (category === "all" || ids?.includes(e.id) || e.group === category) &&
      (tone === "all" || e.tone === tone) &&
      words.every((w) => e.search.includes(w)),
  );
  if (ids) result.sort((a, b) => ids.indexOf(a.id) - ids.indexOf(b.id));
  return result;
}
export function previousGraphemeLength(text) {
  if (!text) return 0;
  if (globalThis.Intl?.Segmenter) {
    const segments = [
      ...new Intl.Segmenter("es", { granularity: "grapheme" }).segment(text),
    ];
    return segments.at(-1)?.segment.length || 0;
  }
  const points = Array.from(text);
  let i = points.length - 1;
  const cp = (s) => s.codePointAt(0),
    ext = (s) => {
      const n = cp(s);
      return (
        /[\p{M}]/u.test(s) ||
        n === 0xfe0f ||
        n === 0xfe0e ||
        (n >= 0x1f3fb && n <= 0x1f3ff) ||
        (n >= 0xe0020 && n <= 0xe007f)
      );
    };
  const ri = (s) => {
    const n = cp(s);
    return n >= 0x1f1e6 && n <= 0x1f1ff;
  };
  if (ri(points[i])) {
    let n = 0;
    for (let j = i; j >= 0 && ri(points[j]); j--) n++;
    return points.slice(n % 2 === 0 ? i - 1 : i).join("").length;
  }
  while (i > 0 && ext(points[i])) i--;
  while (i > 0 && points[i - 1] === "\u200d") {
    i -= 2;
    while (i > 0 && ext(points[i])) i--;
  }
  return points.slice(Math.max(0, i)).join("").length;
}

export function readableInk(color) {
  const rgb = [1, 3, 5]
    .map((i) => parseInt(color.slice(i, i + 2), 16) / 255)
    .map((s) => (s <= 0.04045 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4));
  return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2] > 0.179
    ? "#000000"
    : "#ffffff";
}
