import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./tests",
  testMatch: "**/*.spec.js",
  fullyParallel: false,
  workers: 1,
  timeout: 30000,
  reporter: "list",
  webServer: process.env.CI
    ? {
        command: "npm run preview -- --port 5173",
        url: "http://localhost:5173",
        reuseExistingServer: false,
      }
    : undefined,
  use: {
    baseURL: process.env.TEST_URL || "http://localhost:5173",
    headless: true,
    viewport: { width: 1440, height: 1000 },
    launchOptions: process.env.CHROMIUM_EXECUTABLE_PATH
      ? {
          executablePath: process.env.CHROMIUM_EXECUTABLE_PATH,
          args: [
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--no-zygote",
            "--disable-gpu",
          ],
        }
      : {},
  },
});
