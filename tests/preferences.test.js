import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import {
  DEFAULT_SETTINGS,
  PRESETS,
  RANGES,
  sanitizeSettings,
  filterEmojis,
  previousGraphemeLength,
  readableInk,
} from "../src/preferences.js";
const catalog = JSON.parse(
  fs.readFileSync(new URL("../public/data/emojis-es.json", import.meta.url)),
);
test("all 3979 Unicode 17 entries are unique, categorized and bundled identically in native and web", () => {
  assert.equal(catalog.unicode, "17.0");
  assert.equal(catalog.count, 3979);
  assert.equal(catalog.entries.length, 3979);
  assert.equal(new Set(catalog.entries.map((e) => e.emoji)).size, 3979);
  assert.equal(new Set(catalog.entries.map((e) => e.id)).size, 3979);
  assert.equal(catalog.categories.length, 10);
  const groups = new Set(catalog.categories.map((c) => c.id));
  for (const entry of catalog.entries) {
    assert.ok(groups.has(entry.group));
    assert.ok(entry.label);
    assert.ok(entry.search);
    if (entry.parent)
      assert.ok(catalog.entries.some((e) => e.id === entry.parent));
  }
  assert.deepEqual(
    JSON.parse(
      fs.readFileSync(
        new URL(
          "../android/app/src/main/assets/data/emojis-es.json",
          import.meta.url,
        ),
      ),
    ),
    catalog,
  );
  for (const category of catalog.categories)
    assert.ok(filterEmojis(catalog.entries, { category: category.id }).length);
});
test("every catalogue entry deletes as one grapheme, including mixed tones, flags and ZWJ families", () => {
  for (const entry of catalog.entries)
    assert.equal(
      previousGraphemeLength(entry.emoji),
      entry.emoji.length,
      entry.id,
    );
  assert.equal(previousGraphemeLength("cafe\u0301"), 2);
  assert.equal(previousGraphemeLength(""), 0);
});
test("fallback deletion handles the entire catalogue on browsers without Intl.Segmenter", () => {
  const segmenter = Intl.Segmenter;
  try {
    Intl.Segmenter = undefined;
    for (const entry of catalog.entries)
      assert.equal(
        previousGraphemeLength(entry.emoji),
        entry.emoji.length,
        entry.id,
      );
  } finally {
    Intl.Segmenter = segmenter;
  }
});
test("Spanish search is accent-insensitive; tone, recent and favorites filters retain exact variants", () => {
  assert.ok(filterEmojis(catalog.entries, { query: "CORAZÓN" }).length > 10);
  assert.deepEqual(
    filterEmojis(catalog.entries, { query: "corazon" }),
    filterEmojis(catalog.entries, { query: "CORAZÓN" }),
  );
  const flags = filterEmojis(catalog.entries, { category: "9" });
  assert.ok(flags.some((e) => e.emoji === "🇻🇪"));
  assert.ok(filterEmojis(catalog.entries, { tone: "mixed" }).length > 0);
  const id = catalog.entries.find((e) => e.emoji === "😊").id;
  assert.equal(
    filterEmojis(catalog.entries, { category: "recent", recent: [id] })[0]
      .emoji,
    "😊",
  );
  assert.equal(
    filterEmojis(catalog.entries, { category: "favorites", favorites: [] })
      .length,
    0,
  );
});
test("old preferences migrate and malformed imported settings are constrained", () => {
  assert.deepEqual(sanitizeSettings(null), DEFAULT_SETTINGS);
  const value = sanitizeSettings(
    JSON.parse(
      '{"hand":"right","keyHeight":999,"keyGap":-5,"sound":"true","incognito":true,"backgroundColor":"url(evil)","keyColor":["#ffffff"],"constructor":5,"__proto__":20,"extra":"bad"}',
    ),
  );
  assert.equal(value.hand, "right");
  assert.equal(value.keyHeight, 68);
  assert.equal(value.keyGap, 1);
  assert.equal(value.sound, false);
  assert.equal(value.incognito, true);
  assert.equal(value.backgroundColor, DEFAULT_SETTINGS.backgroundColor);
  assert.equal(value.keyColor, DEFAULT_SETTINGS.keyColor);
  assert.equal(Object.hasOwn(value, "constructor"), false);
  assert.equal(Object.hasOwn(value, "extra"), false);
  assert.deepEqual(sanitizeSettings({ keyHeight: NaN }), DEFAULT_SETTINGS);
  for (const preset of Object.values(PRESETS))
    for (const key in RANGES)
      if (key in preset)
        assert.equal(sanitizeSettings(preset)[key], preset[key]);
});
test("custom key ink chooses contrasting text for white, black and saturated colors", () => {
  assert.equal(readableInk("#ffffff"), "#000000");
  assert.equal(readableInk("#000000"), "#ffffff");
  assert.equal(readableInk("#00ff00"), "#000000");
  assert.equal(readableInk("#0000ff"), "#ffffff");
});
