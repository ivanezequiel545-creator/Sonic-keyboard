import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { readFileSync } from "node:fs";
const media = JSON.parse(readFileSync(new URL("./public/media/catalog.json", import.meta.url), "utf8"));

export default defineConfig({
  plugins: [
    react(),
    {
      name: "offline-shell",
      generateBundle(_, bundle) {
        const assets = [
          "/",
          "/index.html",
          "/manifest.webmanifest",
          "/icon.svg",
          "/icon-192.png",
          "/icon-512.png",
          "/data/emojis-es.json",
          "/data/es-words.json",
          "/licenses/spanish-words.txt",
          "/licenses/emojibase.txt",
          "/media/catalog.json",
          ...media.map(item => `/media/${item.file}`),
          ...Object.keys(bundle).map((name) => `/${name}`),
        ];
        this.emitFile({
          type: "asset",
          fileName: "sw.js",
          source: `
const CACHE = 'velocity-' + ${JSON.stringify(Date.now().toString())};
const ASSETS = ${JSON.stringify(assets)};
self.addEventListener('install', event => { event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(ASSETS))); self.skipWaiting(); });
self.addEventListener('activate', event => { event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(key => key.startsWith('velocity-') && key !== CACHE).map(key => caches.delete(key)))).then(() => self.clients.claim())); });
self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET' || new URL(event.request.url).origin !== self.location.origin) return;
  event.respondWith(caches.match(event.request, { ignoreVary: true }).then(cached => cached || fetch(event.request).catch(() => event.request.mode === 'navigate' ? caches.match('/index.html') : Response.error())));
});`,
        });
      },
    },
  ],
  server: { host: "0.0.0.0", allowedHosts: true },
  preview: { host: "0.0.0.0", allowedHosts: true },
});
