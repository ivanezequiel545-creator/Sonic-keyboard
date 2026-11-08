import { test, expect } from "@playwright/test";
import JSZip from "jszip";

async function navigate(page, name) {
  if (
    await page
      .getByRole("button", { name: "Abrir menú", exact: true })
      .isVisible()
  ) {
    await page.getByRole("button", { name: "Abrir menú", exact: true }).click();
  }
  await page
    .locator(".sidebar")
    .getByRole("button", { name, exact: true })
    .click();
}

test.beforeEach(async ({ page }) => {
  await page.goto("/");
});

test("typing preserves focus; virtual keys, Unicode deletion and symbols work", async ({
  page,
}) => {
  const input = page.getByRole("textbox", { name: "Prueba el teclado" });
  await input.pressSequentially("Hola mundo", { delay: 30 });
  await expect(input).toHaveValue("Hola mundo");
  await expect(input).toBeFocused();
  await page.getByRole("button", { name: "Espacio", exact: true }).click();
  await page
    .locator(".key-row")
    .getByRole("button", { name: "Q", exact: true })
    .click();
  await expect(input).toHaveValue("Hola mundo Q");
  await page.getByRole("button", { name: "Abrir emojis", exact: true }).click();
  await page.getByRole("button", { name: "😊", exact: true }).click();
  await expect(input).toHaveValue("Hola mundo Q😊");
  await page
    .getByRole("button", { name: "Volver a letras", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Borrar carácter", exact: true })
    .click();
  await expect(input).toHaveValue("Hola mundo Q");
  await page.getByRole("button", { name: "?123", exact: true }).click();
  await page
    .locator(".key-row")
    .getByRole("button", { name: "1", exact: true })
    .click();
  await expect(input).toHaveValue("Hola mundo Q1");
});

test("offline translator is explicit about phrases it cannot translate", async ({
  page,
}) => {
  await page
    .getByRole("button", { name: "Abrir traductor", exact: true })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog
    .getByRole("textbox", { name: "Texto para traducir" })
    .fill("¡Hola!");
  await dialog
    .getByRole("button", { name: "Traducir frase", exact: true })
    .click();
  await expect(dialog.locator(".translation-result")).toContainText("hello");
  await dialog.getByRole("button", { name: "Usar texto", exact: true }).click();
  await expect(
    page.getByRole("textbox", { name: "Prueba el teclado" }),
  ).toHaveValue("hello");
  await page
    .getByRole("button", { name: "Abrir traductor", exact: true })
    .click();
  await dialog
    .getByRole("textbox", { name: "Texto para traducir" })
    .fill("Este texto no está en el diccionario");
  await dialog
    .getByRole("button", { name: "Traducir frase", exact: true })
    .click();
  await expect(dialog.locator(".translation-result")).toContainText(
    "no está en el diccionario local",
  );
  const external = dialog.getByRole("link", {
    name: "Google Translate",
    exact: true,
  });
  await expect(external).toHaveAttribute("href", /text=Este%20texto/);
  await page.keyboard.press("Escape");
  await expect(dialog).not.toBeVisible();
});

test("themes, favorites and hand preferences survive a reload", async ({
  page,
}) => {
  await navigate(page, "Temas 6");
  await page
    .getByRole("button", { name: "Añadir Midnight a favoritos" })
    .click();
  await page.getByRole("button", { name: "Favoritos", exact: true }).click();
  await expect(page.locator(".full-grid .theme-card")).toHaveCount(1);
  await page.getByRole("button", { name: "Usar tema", exact: true }).click();
  await navigate(page, "Inicio");
  await page
    .getByRole("button", { name: "Modo una mano", exact: true })
    .click();
  await expect(page.locator(".keyboard-stage")).toHaveClass(/hand-right/);
  await page.getByRole("button", { name: "Cambiar lado del teclado" }).click();
  await expect(page.locator(".keyboard-stage")).toHaveClass(/hand-left/);
  await page.reload();
  await expect(page.locator(".keyboard-stage")).toHaveClass(/hand-left/);
  await expect(page.locator(".virtual-keyboard")).toHaveClass(/theme-midnight/);
});

test("notes persist and can be inserted and removed", async ({ page }) => {
  await page.getByRole("button", { name: /Notas rápidas Tus frases/ }).click();
  await page
    .getByRole("textbox", { name: "Nueva nota" })
    .fill("Mi mensaje de prueba");
  await page.getByRole("button", { name: "Guardar nota", exact: true }).click();
  await expect(page.locator(".note")).toContainText("Mi mensaje de prueba");
  await page.getByRole("button", { name: "Usar", exact: true }).click();
  await expect(
    page.getByRole("textbox", { name: "Prueba el teclado" }),
  ).toHaveValue("Mi mensaje de prueba");
  await page.reload();
  await page.getByRole("button", { name: /Notas rápidas Tus frases/ }).click();
  await expect(page.locator(".note")).toContainText("Mi mensaje de prueba");
  await page.getByRole("button", { name: "Eliminar nota" }).click();
  await expect(page.locator(".note")).toHaveCount(0);
});

test("download creates a usable ZIP with the selected theme and explanatory readme", async ({
  page,
}) => {
  await navigate(page, "Descargas");
  await page.getByRole("button", { name: "Quitar todos", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "Descargar ZIP", exact: true }),
  ).toBeDisabled();
  await page.getByRole("checkbox").first().check();
  const downloadEvent = page.waitForEvent("download");
  await page
    .getByRole("button", { name: "Descargar ZIP", exact: true })
    .click();
  const download = await downloadEvent;
  expect(download.suggestedFilename()).toBe("velocity-velocity.zip");
  const stream = await download.createReadStream();
  const chunks = [];
  for await (const chunk of stream) chunks.push(chunk);
  const zip = await JSZip.loadAsync(Buffer.concat(chunks));
  expect(
    JSON.parse(await zip.file("velocity/theme.json").async("string")).format,
  ).toBe("velocity-theme");
  expect(await zip.file("velocity/fondo.svg").async("string")).toContain(
    "<svg",
  );
  expect(await zip.file("LEEME.txt").async("string")).toContain("No es un APK");
  await expect(page.locator(".history-row")).toHaveCount(1);
});

test("import validates a config; local reset removes saved preferences", async ({
  page,
}) => {
  await navigate(page, "Ajustes");
  await page.locator("input[type=file]").setInputFiles({
    name: "theme.json",
    mimeType: "application/json",
    buffer: Buffer.from(
      JSON.stringify({
        format: "velocity-theme",
        theme: "mint",
        settings: { hand: "right", sound: true, haptics: "invalid" },
      }),
    ),
  });
  await expect(
    page.getByRole("combobox", { name: "Modo de una mano" }),
  ).toHaveValue("right");
  await expect(
    page.getByRole("switch", { name: "Sonido al escribir" }),
  ).toHaveAttribute("aria-checked", "true");
  await page.locator("input[type=file]").setInputFiles({
    name: "bad.json",
    mimeType: "application/json",
    buffer: Buffer.from('{"format":"bad"}'),
  });
  await expect(page.getByRole("status")).toContainText(
    "archivo theme.json válido",
  );
  await page.getByRole("button", { name: "Restablecer", exact: true }).click();
  await page.getByRole("button", { name: "Sí, restablecer" }).click();
  await expect(
    page.getByRole("combobox", { name: "Modo de una mano" }),
  ).toHaveValue("full");
  await expect(
    page.getByRole("switch", { name: "Sonido al escribir" }),
  ).toHaveAttribute("aria-checked", "false");
});

test("mobile navigation and all screens fit a 390px viewport", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  for (const name of [
    "Temas 6",
    "Herramientas",
    "Descargas",
    "Ajustes",
    "Inicio",
  ]) {
    await navigate(page, name);
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    ).toBe(true);
  }
  await page.getByRole("button", { name: "Llévalo a tu celular" }).click();
  await expect(page.getByRole("dialog")).toContainText(
    "no reemplaza el teclado del sistema",
  );
  await page.getByRole("button", { name: "Cerrar ventana" }).click();
});

test("no client-side exceptions across main sections", async ({ page }) => {
  const errors = [];
  page.on("pageerror", (err) => errors.push(err.message));
  for (const name of [
    "Temas 6",
    "Herramientas",
    "Descargas",
    "Ajustes",
    "Inicio",
  ])
    await navigate(page, name);
  expect(errors).toEqual([]);
});

test("production service worker supports offline keyboard, phrases and ZIP generation", async ({
  page,
  context,
}) => {
  test.skip(
    !process.env.TEST_OFFLINE,
    "Requires the compiled production preview, not the Vite dev server.",
  );
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready;
    if (!navigator.serviceWorker.controller)
      await new Promise((resolve) =>
        navigator.serviceWorker.addEventListener("controllerchange", resolve, {
          once: true,
        }),
      );
  });
  await context.setOffline(true);
  await page.reload();
  await expect(page.locator(".offline-banner")).toBeVisible();
  await page
    .getByRole("button", { name: "Abrir traductor", exact: true })
    .click();
  await page
    .getByRole("textbox", { name: "Texto para traducir" })
    .fill("gracias");
  await page
    .getByRole("button", { name: "Traducir frase", exact: true })
    .click();
  await expect(page.locator(".translation-result")).toContainText("thank you");
  await page.getByRole("button", { name: "Cerrar ventana" }).click();
  const downloadEvent = page.waitForEvent("download");
  await page
    .getByRole("button", { name: "Descargar pack gratis", exact: true })
    .click();
  const download = await downloadEvent;
  expect(download.suggestedFilename()).toBe("velocity-pack.zip");
  await context.setOffline(false);
});

test("Android download links are available with accurate limitations", async ({
  page,
}) => {
  const banner = page.getByRole("region", { name: "Aplicación Android" });
  await expect(banner).toContainText("no un teclado del sistema");
  await expect(banner).toContainText("Repositorio privado");
  await expect(
    banner.getByRole("link", { name: "Descargar APK", exact: true }),
  ).toHaveAttribute(
    "href",
    "https://github.com/ivanezequiel545-creator/Sonic-keyboard/releases/download/android-preview/velocity-android.apk",
  );
  await navigate(page, "Descargas");
  await expect(
    banner.getByRole("link", { name: "Versión e instrucciones" }),
  ).toHaveAttribute("href", /releases\/tag\/android-preview$/);
});
