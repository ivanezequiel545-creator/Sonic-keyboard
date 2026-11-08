import test from "node:test";
import assert from "node:assert/strict";
import JSZip from "jszip";
import {
  themes,
  translateOffline,
  backgroundSVG,
  packageConfig,
} from "../src/data.js";

test("six original themes have unique identifiers and valid colors", () => {
  assert.equal(themes.length, 6);
  assert.equal(new Set(themes.map((theme) => theme.id)).size, 6);
  themes.forEach((theme) =>
    ["color", "dark", "accent"].forEach((key) =>
      assert.match(theme[key], /^#[a-f\d]{6}$/i),
    ),
  );
});
test("offline translator handles whitespace, case and Spanish punctuation", () => {
  assert.equal(translateOffline("  ¡HOLA!  "), "hello");
  assert.equal(translateOffline("¿Cómo estás?"), "how are you");
  assert.equal(translateOffline("buenos   días"), "good morning");
  assert.equal(translateOffline("THANK YOU!", "en-es"), "gracias");
});
test("unsupported translations are not fabricated", () => {
  assert.equal(
    translateOffline("Un texto largo que no está en el diccionario"),
    null,
  );
  assert.equal(translateOffline(""), null);
});
test("theme exports contain original SVG, settings and license", () => {
  for (const theme of themes) {
    const svg = backgroundSVG(theme);
    assert.ok(svg.startsWith('<svg xmlns="http://www.w3.org/2000/svg"'));
    assert.ok(svg.endsWith("</svg>"));
    assert.ok(svg.includes(theme.color));
    const config = packageConfig(theme, { hand: "left" });
    assert.equal(config.format, "velocity-theme");
    assert.equal(config.theme, theme.id);
    assert.equal(config.settings.hand, "left");
    assert.equal(config.license, "Unlicense");
  }
});
test("generated assets survive a real ZIP round trip", async () => {
  const zip = new JSZip();
  themes.forEach((theme) => {
    zip.file(`${theme.id}/theme.json`, JSON.stringify(packageConfig(theme)));
    zip.file(`${theme.id}/fondo.svg`, backgroundSVG(theme));
  });
  const buffer = await zip.generateAsync({ type: "nodebuffer" });
  const extracted = await JSZip.loadAsync(buffer);
  assert.equal(
    JSON.parse(await extracted.file("velocity/theme.json").async("string"))
      .name,
    "Blue Rush",
  );
  assert.match(
    await extracted.file("mint/fondo.svg").async("string"),
    /<linearGradient/,
  );
});
