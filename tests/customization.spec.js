import { test, expect } from "@playwright/test";
async function customize(page) {
  await page.goto("/");
  if (
    await page
      .getByRole("button", { name: "Abrir menú", exact: true })
      .isVisible()
  )
    await page.getByRole("button", { name: "Abrir menú", exact: true }).click();
  await page
    .locator(".sidebar")
    .getByRole("button", { name: "Personalizar", exact: true })
    .click();
}
test("comfortable presets change real geometry and persist after reload", async ({
  page,
}) => {
  await customize(page);
  await page.getByRole("button", { name: /Accesible Teclas y letras/ }).click();
  await expect(
    page.getByRole("slider", { name: "Altura de teclas", exact: true }),
  ).toHaveValue("64");
  await expect(
    page.locator(".key-row").getByRole("button", { name: "Q", exact: true }),
  ).toHaveCSS("height", "64px");
  await page.reload();
  await customize(page);
  await expect(
    page.getByRole("slider", { name: "Altura de teclas", exact: true }),
  ).toHaveValue("64");
  await page.getByRole("button", { name: /Compacto Recupera/ }).click();
  await expect(page.locator(".permanent-numbers")).toHaveCount(0);
  await page
    .getByRole("checkbox", { name: "Fila de números permanente", exact: true })
    .check();
  await expect(page.locator(".permanent-numbers .key")).toHaveCount(10);
});
test("Spanish categories and tone variants are paginated without thousands of DOM buttons", async ({
  page,
}) => {
  await customize(page);
  await page.getByRole("button", { name: "Abrir emojis", exact: true }).click();
  const library = page.getByRole("region", { name: "Catálogo de emojis" });
  await expect(library.locator(".emoji-result-count")).toContainText("3979");
  await expect(library.locator(".emoji-grid button")).toHaveCount(72);
  await library
    .getByRole("searchbox", { name: "Buscar emojis en español" })
    .fill("corazón");
  await expect(library.locator(".emoji-grid button").first()).toBeVisible();
  await library.getByRole("searchbox").fill("");
  await library.getByRole("button", { name: /Banderas/ }).click();
  await library.getByRole("searchbox").fill("venezuela");
  await library.getByRole("button", { name: "🇻🇪", exact: true }).click();
  await expect(
    page.getByRole("textbox", { name: "Prueba el teclado" }),
  ).toHaveValue("🇻🇪");
  await library
    .getByRole("button", { name: "☆ Guardar favorito", exact: true })
    .click();
  await library.getByRole("button", { name: /★ Favoritos/ }).click();
  await library.getByRole("searchbox").fill("");
  await expect(library.locator(".emoji-grid button")).toHaveCount(1);
  await page
    .getByRole("button", { name: "Volver a letras", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Borrar carácter", exact: true })
    .click();
  await expect(
    page.getByRole("textbox", { name: "Prueba el teclado" }),
  ).toHaveValue("");
});
test("private mode does not record emoji choices; recoloring and photo removal work", async ({
  page,
}) => {
  await customize(page);
  await page
    .getByRole("checkbox", {
      name: "Modo privado · sin recientes",
      exact: true,
    })
    .check();
  await page.getByRole("button", { name: "Abrir emojis", exact: true }).click();
  await page.getByRole("button", { name: "😊", exact: true }).click();
  expect(
    await page.evaluate(() => localStorage.getItem("velocity-emoji-recent")),
  ).toBeNull();
  await page.getByLabel("Color de teclas", { exact: true }).fill("#00ff00");
  await page
    .getByRole("button", { name: "Volver a letras", exact: true })
    .click();
  await expect(
    page.locator(".key-row").getByRole("button", { name: "q", exact: true }),
  ).toHaveCSS("color", "rgb(0, 0, 0)");
  await page
    .getByLabel("Imagen de fondo", { exact: true })
    .setInputFiles({
      name: "pixel.png",
      mimeType: "image/png",
      buffer: Buffer.from(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j7yQAAAAASUVORK5CYII=",
        "base64",
      ),
    });
  await expect(
    page.getByRole("button", { name: "Quitar foto", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Quitar foto", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "Quitar foto", exact: true }),
  ).toHaveCount(0);
});
test("customization fits a narrow viewport at maximum comfort settings", async ({
  page,
}) => {
  await page.setViewportSize({ width: 360, height: 800 });
  await customize(page);
  await page.getByRole("button", { name: /Accesible Teclas y letras/ }).click();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.getByRole("button", { name: "Abrir emojis", exact: true }).click();
  await expect(
    page.getByRole("region", { name: "Catálogo de emojis" }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});
test("emoji catalogue is available offline even if never opened while online", async ({
  page,
  context,
}) => {
  test.skip(!process.env.TEST_OFFLINE, "Production service worker required");
  await page.goto("/");
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready;
    if (!navigator.serviceWorker.controller)
      await new Promise((r) =>
        navigator.serviceWorker.addEventListener("controllerchange", r, {
          once: true,
        }),
      );
  });
  await context.setOffline(true);
  await page.reload();
  await page.getByRole("button", { name: "Abrir emojis", exact: true }).click();
  await expect(page.locator(".emoji-grid button")).toHaveCount(72);
  await page
    .getByRole("searchbox", { name: "Buscar emojis en español" })
    .fill("venezuela");
  await expect(
    page.getByRole("button", { name: "🇻🇪", exact: true }),
  ).toBeVisible();
  await context.setOffline(false);
});

test("Spanish suggestion strip completes the word at the caret and respects private mode",async({page})=>{
 await customize(page);const input=page.getByRole('textbox',{name:'Prueba el teclado'});
 await input.fill('hol');await expect(page.locator('.suggestions').getByRole('button',{name:'hola',exact:true})).toBeVisible();
 await page.locator('.suggestions').getByRole('button',{name:'hola',exact:true}).click();await expect(input).toHaveValue('hola ');
 await input.fill('hol mundo');await input.press('Home');await input.press('ArrowRight');await input.press('ArrowRight');await input.press('ArrowRight');
 await expect(input).toHaveValue('hol mundo');await expect.poll(()=>input.evaluate(el=>el.selectionStart)).toBe(3);await expect(page.locator('.suggestions').getByRole('button',{name:'hol',exact:true})).toBeVisible();
 await page.locator('.suggestions').getByRole('button',{name:'hola',exact:true}).click();await expect(input).toHaveValue('hola mundo');
 await page.getByRole('checkbox',{name:'Modo privado · sin recientes',exact:true}).check();await expect(page.locator('.suggestions button')).toHaveCount(0);
});

test("offline GIF and sticker pack is available before first use and downloads the real file",async({page,context})=>{
 test.skip(!process.env.TEST_OFFLINE,"Production service worker required");
 await page.goto('/');await page.evaluate(async()=>{await navigator.serviceWorker.ready;if(!navigator.serviceWorker.controller)await new Promise(resolve=>navigator.serviceWorker.addEventListener('controllerchange',resolve,{once:true}));});
 await context.setOffline(true);await page.reload();
 await page.getByRole('button',{name:'Abrir emojis',exact:true}).click();
 await page.getByRole('tab',{name:'Stickers',exact:true}).click();
 await expect(page.locator('.media-grid button')).toHaveCount(6);
 await page.getByRole('button',{name:'Seleccionar ¡Hola!',exact:true}).click();
 const download=page.waitForEvent('download');await page.getByRole('link',{name:'Descargar sticker',exact:true}).click();expect((await download).suggestedFilename()).toBe('hola.png');
 await page.getByRole('tab',{name:'GIFs',exact:true}).click();await expect(page.locator('.media-grid button')).toHaveCount(4);
 await page.getByRole('checkbox',{name:'Animar miniaturas GIF'}).check();
 await expect(page.locator('.media-grid img').first()).toHaveAttribute('src','/media/hola.gif');
 await expect.poll(()=>page.locator('.media-grid img').first().evaluate(img=>img.complete&&img.naturalWidth===240)).toBe(true);
 await page.getByRole('button',{name:'Seleccionar ¡Hola! animado',exact:true}).click();
 const gif=page.waitForEvent('download');await page.getByRole('link',{name:'Descargar GIF',exact:true}).click();expect((await gif).suggestedFilename()).toBe('hola.gif');
 await page.getByRole('tab',{name:'Emojis',exact:true}).click();await expect(page.locator('.emoji-grid button')).toHaveCount(72);
 await context.setOffline(false);
});

test('external selection is opt-in, attributed, cancellable and hidden in private mode',async({page})=>{
 let requests=[];
 await page.route('https://media*.giphy.com/**',async route=>{requests.push(route.request().url());await route.fulfill({path:'public/media/hola.gif',contentType:'image/gif',headers:{'Access-Control-Allow-Origin':'*'}});});
 await customize(page);await page.getByRole('button',{name:'Abrir emojis',exact:true}).click();await page.getByRole('tab',{name:'GIFs',exact:true}).click();
 const remote=page.getByRole('region',{name:'Selección de GIPHY'});
 await expect(remote.getByRole('button',{name:'Cargar selección en línea',exact:true})).toBeEnabled();expect(requests).toHaveLength(0);
 await remote.getByRole('button',{name:'Cargar selección en línea',exact:true}).click();await expect(remote.locator('article')).toHaveCount(6);
 await expect(remote.getByRole('link',{name:'Ver fuente'})).toHaveCount(6);await expect.poll(()=>requests.length).toBeGreaterThan(0);
 await remote.getByRole('button',{name:'Seleccionar Wendy · impactada',exact:true}).click();
 const download=page.waitForEvent('download');await remote.getByRole('button',{name:'Descargar selección',exact:true}).click();expect((await download).suggestedFilename()).toBe('giphy-ugqysnkf2himbgt0cv.gif');
 await remote.getByRole('button',{name:'Ocultar selección en línea',exact:true}).click();await expect(remote.locator('article')).toHaveCount(0);
 await page.getByRole('checkbox',{name:'Modo privado · sin recientes',exact:true}).check();await expect(page.getByText('Selección en línea desactivada en modo privado.',{exact:true})).toBeVisible();await expect(page.locator('.remote-media img')).toHaveCount(0);
});
