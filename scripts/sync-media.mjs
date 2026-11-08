import { cpSync, mkdirSync } from 'node:fs';
mkdirSync('android/app/src/main/assets/media', { recursive: true });
cpSync('public/media', 'android/app/src/main/assets/media', { recursive: true });
