import { createRequire } from "node:module";
import { mkdir, readFile, writeFile } from "node:fs/promises";
const require = createRequire(import.meta.url);
const source = require("emojibase-data/es/data.json");
const categories = [
  ["0", "😀", "Caras y emociones"],
  ["1", "👋", "Personas y cuerpo"],
  ["3", "🐾", "Animales y naturaleza"],
  ["4", "🍕", "Comida y bebida"],
  ["5", "🌍", "Viajes y lugares"],
  ["6", "⚽", "Actividades"],
  ["7", "💡", "Objetos"],
  ["8", "❤️", "Símbolos"],
  ["9", "🏳️", "Banderas"],
  ["2", "🧩", "Componentes"],
].map(([id, icon, name]) => ({ id, icon, name }));
const entries = [];
function append(item, parent = null, tags = []) {
  entries.push({
    id: item.hexcode,
    emoji: item.emoji,
    label: item.label,
    group: String(item.group ?? 2),
    version: item.version,
    parent,
    tone: Array.isArray(item.tone) ? "mixed" : String(item.tone ?? 0),
    search: [item.emoji, item.hexcode, item.label, ...(item.tags || tags)]
      .join(" ")
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLowerCase(),
  });
  (item.skins || []).forEach((skin) =>
    append(skin, item.hexcode, item.tags || []),
  );
}
source
  .sort((a, b) => (a.order ?? 99999) - (b.order ?? 99999))
  .forEach((item) => append(item));
const catalog = {
  unicode: "17.0",
  source: "emojibase-data@17.0.0",
  locale: "es",
  count: entries.length,
  categories,
  entries,
};
if (
  entries.length !== 3979 ||
  new Set(entries.map((e) => e.id)).size !== entries.length
)
  throw Error("Unexpected emoji catalogue; review upstream changes.");
await mkdir("public/data", { recursive: true });
await writeFile("public/data/emojis-es.json", JSON.stringify(catalog));
await mkdir("android/app/src/main/assets/data", { recursive: true });
await writeFile(
  "android/app/src/main/assets/data/emojis-es.json",
  JSON.stringify(catalog),
);
const license = await readFile(
  require.resolve("emojibase-data/LICENSE"),
  "utf8",
);
await mkdir("public/licenses", { recursive: true });
await writeFile("public/licenses/emojibase.txt", license);
await writeFile(
  "android/app/src/main/assets/data/EMOJIBASE-LICENSE.txt",
  license,
);
console.log(
  `${entries.length} unique entries, Unicode ${catalog.unicode}, ${categories.length} categories.`,
);
